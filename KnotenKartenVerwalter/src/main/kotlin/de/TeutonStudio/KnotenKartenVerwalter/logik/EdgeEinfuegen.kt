package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Unveränderlicher Dialogkontext; schützt vor inzwischen veränderten Edges. */
data class EdgeEinfuegeKontext(
    val edgeId: VerbindungsId,
    val quelle: AnschlussVerweis,
    val ziel: AnschlussVerweis,
) {
    companion object {
        fun aus(verbindung: VerbindungDaten) = EdgeEinfuegeKontext(
            verbindung.id,
            verbindung.von,
            verbindung.zu,
        )
    }
}

data class KnotenVariante(
    val varianteId: String?,
    val vorlage: KnotenVorlage,
)

data class KompatiblesAnschlussPaar(
    val eingangId: AnschlussId,
    val ausgangId: AnschlussId,
    val eingangsArt: AnschlussArtId,
    val ausgangsArt: AnschlussArtId,
    val bedingungen: List<String> = emptyList(),
)

data class EinfuegeKandidat(
    val knotenVorlageId: KnotenArtId,
    val varianteId: String?,
    /** Die IDs dieses Probeobjekts werden bei Bestätigung stabil übernommen. */
    val probeknoten: KnotenDaten,
    val anschlussPaare: List<KompatiblesAnschlussPaar>,
)

sealed interface KandidatenSucheErgebnis {
    data class Erfolg(val kandidaten: List<EinfuegeKandidat>) : KandidatenSucheErgebnis
    data class Konflikt(val code: String, val nachricht: String) : KandidatenSucheErgebnis
}

/**
 * UI-unabhängiger Kandidatenfilter. Die zweite Verbindung wird gegen einen
 * Probegraphen geprüft, der die erste Teilverbindung bereits enthält. Dadurch
 * funktionieren auch eingangsabhängige Ausgangsarten korrekt.
 */
class EdgeEinfuegeKandidatenSuche(
    private val pruefung: GraphPrüfung,
) {
    fun suche(
        karte: KartenDaten,
        kontext: EdgeEinfuegeKontext,
        varianten: Iterable<KnotenVariante>,
    ): KandidatenSucheErgebnis {
        val original = karte.verbindungen.singleOrNull { it.id == kontext.edgeId }
            ?: return KandidatenSucheErgebnis.Konflikt(
                "edge_fehlt",
                "Die ursprüngliche Verbindung existiert nicht mehr.",
            )
        if (original.von != kontext.quelle || original.zu != kontext.ziel) {
            return KandidatenSucheErgebnis.Konflikt(
                "edge_veraendert",
                "Die ursprüngliche Verbindung wurde während des Dialogs verändert.",
            )
        }
        if (karte.findeAnschluss(kontext.quelle) == null || karte.findeAnschluss(kontext.ziel) == null) {
            return KandidatenSucheErgebnis.Konflikt(
                "anschluss_fehlt",
                "Quell- oder Zielanschluss existiert nicht mehr.",
            )
        }

        val basis = karte.copy(verbindungen = karte.verbindungen.filterNot { it.id == original.id })
        val kandidaten = varianten.mapNotNull { variante ->
            val probeKnoten = variante.vorlage.erzeuge(GraphPunkt.Zero)
            val probeKarte = basis.copy(knoten = basis.knoten + probeKnoten)
            val eingänge = probeKnoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
            val ausgänge = probeKnoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }
            val paare = buildList {
                for (eingang in eingänge) {
                    val eingangsRef = AnschlussVerweis(probeKnoten.id, eingang.id)
                    if (pruefung.prüfe(probeKarte, kontext.quelle, eingangsRef) !is VerbindungsPrüfung.Erlaubt) continue
                    val normalEingang = pruefung.normalisiere(probeKarte, kontext.quelle, eingangsRef) ?: continue
                    val ersteEdge = VerbindungDaten(von = normalEingang.first, zu = normalEingang.second)
                    val mitEingang = probeKarte.copy(verbindungen = probeKarte.verbindungen + ersteEdge)
                    for (ausgang in ausgänge) {
                        val ausgangsRef = AnschlussVerweis(probeKnoten.id, ausgang.id)
                        if (pruefung.prüfe(mitEingang, ausgangsRef, kontext.ziel) !is VerbindungsPrüfung.Erlaubt) continue
                        add(
                            KompatiblesAnschlussPaar(
                                eingangId = eingang.id,
                                ausgangId = ausgang.id,
                                eingangsArt = pruefung.effektiveArt(mitEingang, eingangsRef),
                                ausgangsArt = pruefung.effektiveArt(mitEingang, ausgangsRef),
                            ),
                        )
                    }
                }
            }.distinctBy { it.eingangId to it.ausgangId }
            paare.takeIf { it.isNotEmpty() }?.let {
                EinfuegeKandidat(
                    knotenVorlageId = variante.vorlage.art,
                    varianteId = variante.varianteId,
                    probeknoten = probeKnoten,
                    anschlussPaare = it,
                )
            }
        }.sortedWith(compareBy({ it.probeknoten.name }, { it.varianteId.orEmpty() }))
        return KandidatenSucheErgebnis.Erfolg(kandidaten)
    }
}

data class KubischeBezierKurve(
    val start: GraphPunkt,
    val kontrollpunkt1: GraphPunkt,
    val kontrollpunkt2: GraphPunkt,
    val ende: GraphPunkt,
) {
    fun punkt(t: Float): GraphPunkt {
        require(t in 0f..1f)
        val u = 1f - t
        return start * (u * u * u) +
            kontrollpunkt1 * (3f * u * u * t) +
            kontrollpunkt2 * (3f * u * t * t) +
            ende * (t * t * t)
    }

    companion object {
        fun gerade(start: GraphPunkt, ende: GraphPunkt) = KubischeBezierKurve(
            start,
            start + (ende - start) * (1f / 3f),
            start + (ende - start) * (2f / 3f),
            ende,
        )
    }
}

data class KnotenEinfuegeBefehl(
    val vorher: KartenDaten,
    val nachher: KartenDaten,
    val knotenId: KnotenId,
    val eingangsEdgeId: VerbindungsId,
    val ausgangsEdgeId: VerbindungsId,
) {
    val beschreibung: String = "Knoten zwischen Verbindung einfügen"

    fun anwenden(aktuell: KartenDaten): KartenDaten {
        require(aktuell == vorher) { "Der Graph wurde seit dem Planen der Einfügung verändert." }
        return nachher
    }

    fun rueckgaengig(aktuell: KartenDaten): KartenDaten {
        require(aktuell == nachher) { "Der Graph entspricht nicht dem Ergebnis dieser Einfügung." }
        return vorher
    }
}

sealed interface KnotenEinfuegeErgebnis {
    data class Erfolg(
        val karte: KartenDaten,
        val befehl: KnotenEinfuegeBefehl,
    ) : KnotenEinfuegeErgebnis

    data class Fehlgeschlagen(
        val code: String,
        val nachricht: String,
    ) : KnotenEinfuegeErgebnis
}

/** Atomarer splitEdge-/insertNodeIntoEdge-Dienst. */
class KnotenInEdgeEinfueger(
    private val pruefung: GraphPrüfung,
) {
    fun einfuegen(
        karte: KartenDaten,
        kontext: EdgeEinfuegeKontext,
        kandidat: EinfuegeKandidat,
        paar: KompatiblesAnschlussPaar,
        kurve: KubischeBezierKurve? = null,
    ): KnotenEinfuegeErgebnis {
        val original = karte.verbindungen.singleOrNull { it.id == kontext.edgeId }
            ?: return fehler("edge_fehlt", "Die ursprüngliche Verbindung existiert nicht mehr.")
        if (original.von != kontext.quelle || original.zu != kontext.ziel) {
            return fehler("edge_veraendert", "Die ursprüngliche Verbindung wurde inzwischen verändert.")
        }
        if (paar !in kandidat.anschlussPaare) {
            return fehler("anschlusspaar_unbekannt", "Das gewählte Anschlusspaar gehört nicht zum Kandidaten.")
        }
        if (karte.knoten.any { it.id == kandidat.probeknoten.id }) {
            return fehler("knoten_id_belegt", "Die vorbereitete Knoten-ID ist bereits belegt.")
        }

        val quellKnoten = karte.knoten.firstOrNull { it.id == kontext.quelle.knotenId }
            ?: return fehler("quelle_fehlt", "Der Quellknoten existiert nicht mehr.")
        val zielKnoten = karte.knoten.firstOrNull { it.id == kontext.ziel.knotenId }
            ?: return fehler("ziel_fehlt", "Der Zielknoten existiert nicht mehr.")
        val quellMitte = quellKnoten.position + GraphPunkt(quellKnoten.größe.breite / 2f, quellKnoten.größe.höhe / 2f)
        val zielMitte = zielKnoten.position + GraphPunkt(zielKnoten.größe.breite / 2f, zielKnoten.größe.höhe / 2f)
        val mittelpunkt = (kurve ?: KubischeBezierKurve.gerade(quellMitte, zielMitte)).punkt(0.5f)
        val neuerKnoten = kandidat.probeknoten.copy(
            position = mittelpunkt - GraphPunkt(
                kandidat.probeknoten.größe.breite / 2f,
                kandidat.probeknoten.größe.höhe / 2f,
            ),
        )

        val eingang = AnschlussVerweis(neuerKnoten.id, paar.eingangId)
        val ausgang = AnschlussVerweis(neuerKnoten.id, paar.ausgangId)
        if (neuerKnoten.anschlüsse.none { it.id == paar.eingangId && it.richtung == AnschlussRichtung.Eingang } ||
            neuerKnoten.anschlüsse.none { it.id == paar.ausgangId && it.richtung == AnschlussRichtung.Ausgang }
        ) {
            return fehler("anschluss_geaendert", "Die Knotenvariante besitzt das gewählte Anschlusspaar nicht mehr.")
        }

        val ohneOriginal = karte.copy(
            knoten = karte.knoten + neuerKnoten,
            verbindungen = karte.verbindungen.filterNot { it.id == original.id },
        )
        val erstePruefung = pruefung.prüfe(ohneOriginal, kontext.quelle, eingang)
        if (erstePruefung is VerbindungsPrüfung.Abgelehnt) {
            return fehler("eingangsverbindung_ungueltig", erstePruefung.grund)
        }
        val normalEingang = pruefung.normalisiere(ohneOriginal, kontext.quelle, eingang)
            ?: return fehler("eingangsverbindung_nicht_normalisierbar", "Die erste Teilverbindung konnte nicht normalisiert werden.")
        val eingangsEdge = VerbindungDaten(von = normalEingang.first, zu = normalEingang.second)
        val mitEingang = ohneOriginal.copy(verbindungen = ohneOriginal.verbindungen + eingangsEdge)

        val zweitePruefung = pruefung.prüfe(mitEingang, ausgang, kontext.ziel)
        if (zweitePruefung is VerbindungsPrüfung.Abgelehnt) {
            return fehler("ausgangsverbindung_ungueltig", zweitePruefung.grund)
        }
        val normalAusgang = pruefung.normalisiere(mitEingang, ausgang, kontext.ziel)
            ?: return fehler("ausgangsverbindung_nicht_normalisierbar", "Die zweite Teilverbindung konnte nicht normalisiert werden.")
        val ausgangsEdge = VerbindungDaten(von = normalAusgang.first, zu = normalAusgang.second)
        val nachher = mitEingang.copy(verbindungen = mitEingang.verbindungen + ausgangsEdge)

        return KnotenEinfuegeErgebnis.Erfolg(
            karte = nachher,
            befehl = KnotenEinfuegeBefehl(
                vorher = karte,
                nachher = nachher,
                knotenId = neuerKnoten.id,
                eingangsEdgeId = eingangsEdge.id,
                ausgangsEdgeId = ausgangsEdge.id,
            ),
        )
    }

    private fun fehler(code: String, nachricht: String) =
        KnotenEinfuegeErgebnis.Fehlgeschlagen(code, nachricht)
}
