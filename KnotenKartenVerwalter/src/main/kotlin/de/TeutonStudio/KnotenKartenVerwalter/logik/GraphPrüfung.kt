package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

sealed interface VerbindungsPrüfung {
    data object Erlaubt : VerbindungsPrüfung
    data class Abgelehnt(val grund: String) : VerbindungsPrüfung
}

class GraphPrüfung(private val arten: AnschlussArtRegister) {
    fun prüfe(karte: KartenDaten, erster: AnschlussVerweis, zweiter: AnschlussVerweis): VerbindungsPrüfung {
        if (erster == zweiter) return VerbindungsPrüfung.Abgelehnt("Ein Anschluss kann nicht mit sich selbst verbunden werden.")
        val a = karte.findeAnschluss(erster) ?: return VerbindungsPrüfung.Abgelehnt("Erster Anschluss fehlt.")
        val b = karte.findeAnschluss(zweiter) ?: return VerbindungsPrüfung.Abgelehnt("Zweiter Anschluss fehlt.")
        val (ausgang, eingang) = richte(a, erster, b, zweiter)
            ?: return VerbindungsPrüfung.Abgelehnt("Die Anschlussrichtungen sind nicht kompatibel.")

        // Ein belegter Eingang wird beim Verbinden atomar ersetzt. Die Typ- und
        // Zyklusprüfung betrachtet deshalb bereits den Graphen nach dieser Ersetzung.
        val ohneAlteEingangsVerbindung = if (eingang.first.richtung == AnschlussRichtung.Eingang) {
            karte.copy(verbindungen = karte.verbindungen.filterNot { it.zu == eingang.second })
        } else karte
        val neueVerbindung = VerbindungDaten(von = ausgang.second, zu = eingang.second)
        val probe = ohneAlteEingangsVerbindung.copy(
            verbindungen = ohneAlteEingangsVerbindung.verbindungen.filterNot {
                it.von == neueVerbindung.von && it.zu == neueVerbindung.zu
            } + neueVerbindung,
        )

        val ausgangsArt = effektiveArt(probe, ausgang.second)
        val eingangsArt = effektiveArt(probe, eingang.second)
        if (!arten.istUnterart(ausgangsArt, eingangsArt)) {
            return VerbindungsPrüfung.Abgelehnt("$ausgangsArt kann nicht an $eingangsArt angeschlossen werden.")
        }
        if (erzeugtZyklus(ohneAlteEingangsVerbindung, ausgang.second.knotenId, eingang.second.knotenId)) {
            return VerbindungsPrüfung.Abgelehnt("Zirkuläre Verbindungen sind nicht erlaubt.")
        }

        val ungültigeFolgeVerbindung = probe.verbindungen.firstOrNull { !istTypkompatibel(probe, it) }
        if (ungültigeFolgeVerbindung != null) {
            val von = effektiveArt(probe, ungültigeFolgeVerbindung.von)
            val zu = effektiveArt(probe, ungültigeFolgeVerbindung.zu)
            return VerbindungsPrüfung.Abgelehnt(
                "Die Verbindung würde einen abhängigen Ausgang von $von auf einen mit $zu inkompatiblen Typ ändern.",
            )
        }
        return VerbindungsPrüfung.Erlaubt
    }

    fun normalisiere(karte: KartenDaten, erster: AnschlussVerweis, zweiter: AnschlussVerweis): Pair<AnschlussVerweis, AnschlussVerweis>? {
        val a = karte.findeAnschluss(erster) ?: return null
        val b = karte.findeAnschluss(zweiter) ?: return null
        return richte(a, erster, b, zweiter)?.let { it.first.second to it.second.second }
    }

    /**
     * Liefert die deklarierte Art, die Art eines einzelnen referenzierten Eingangs oder
     * die kleinste gemeinsame Oberart aller verbundenen Eingänge aus [AnschlussDaten.artVereinigtEingänge].
     */
    fun effektiveArt(karte: KartenDaten, ref: AnschlussVerweis): AnschlussArtId =
        effektiveArt(karte, ref, mutableSetOf())

    private fun effektiveArt(
        karte: KartenDaten,
        ref: AnschlussVerweis,
        besucht: MutableSet<AnschlussVerweis>,
    ): AnschlussArtId {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return AnschlussArtId("unbekannt")
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return AnschlussArtId("unbekannt")

        anschluss.artFolgtEingang?.let { eingangsName ->
            if (!besucht.add(ref)) return anschluss.art
            val eingang = knoten.anschlüsse.firstOrNull {
                it.name == eingangsName && it.richtung == AnschlussRichtung.Eingang
            } ?: return anschluss.art
            val eingangsRef = AnschlussVerweis(knoten.id, eingang.id)
            val quelle = karte.verbindungen.firstOrNull { it.zu == eingangsRef }?.von ?: return anschluss.art
            return effektiveArt(karte, quelle, besucht)
        }

        if (anschluss.artVereinigtEingänge.isEmpty()) return anschluss.art
        if (!besucht.add(ref)) return anschluss.art
        val quellArten = anschluss.artVereinigtEingänge.mapNotNull { eingangsName ->
            val eingang = knoten.anschlüsse.firstOrNull {
                it.name == eingangsName && it.richtung == AnschlussRichtung.Eingang
            } ?: return@mapNotNull null
            val eingangsRef = AnschlussVerweis(knoten.id, eingang.id)
            val quelle = karte.verbindungen.firstOrNull { it.zu == eingangsRef }?.von ?: return@mapNotNull null
            effektiveArt(karte, quelle, besucht.toMutableSet())
        }
        return arten.gemeinsameOberart(quellArten) ?: anschluss.art
    }

    /**
     * Ändert die Art eines vorhandenen Anschlusses und entfernt dadurch ungültig gewordene Kanten.
     * Anschluss-ID, Richtung und alle unveränderten Kanten bleiben erhalten.
     */
    fun ändereAnschlussArt(karte: KartenDaten, ref: AnschlussVerweis, art: AnschlussArtId): KartenDaten {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return karte
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return karte
        if (anschluss.art == art) return karte
        val mitNeuerArt = karte.copy(knoten = karte.knoten.map {
            if (it.id == knoten.id) it.copy(anschlüsse = it.anschlüsse.map { a ->
                if (a.id == anschluss.id) a.copy(art = art) else a
            }) else it
        })
        return mitNeuerArt.copy(verbindungen = mitNeuerArt.verbindungen.filter { istTypkompatibel(mitNeuerArt, it) })
    }

    private fun richte(
        a: AnschlussDaten, aRef: AnschlussVerweis,
        b: AnschlussDaten, bRef: AnschlussVerweis,
    ): Pair<Pair<AnschlussDaten, AnschlussVerweis>, Pair<AnschlussDaten, AnschlussVerweis>>? = when {
        a.richtung == AnschlussRichtung.Ausgang && b.richtung == AnschlussRichtung.Eingang -> (a to aRef) to (b to bRef)
        b.richtung == AnschlussRichtung.Ausgang && a.richtung == AnschlussRichtung.Eingang -> (b to bRef) to (a to aRef)
        a.richtung == AnschlussRichtung.Neutral && b.richtung == AnschlussRichtung.Neutral -> (a to aRef) to (b to bRef)
        a.richtung == AnschlussRichtung.Ausgang && b.richtung == AnschlussRichtung.Neutral -> (a to aRef) to (b to bRef)
        b.richtung == AnschlussRichtung.Ausgang && a.richtung == AnschlussRichtung.Neutral -> (b to bRef) to (a to aRef)
        else -> null
    }

    private fun istTypkompatibel(karte: KartenDaten, verbindung: VerbindungDaten): Boolean {
        val von = karte.findeAnschluss(verbindung.von) ?: return false
        val zu = karte.findeAnschluss(verbindung.zu) ?: return false
        richte(von, verbindung.von, zu, verbindung.zu) ?: return false
        return arten.istUnterart(effektiveArt(karte, verbindung.von), effektiveArt(karte, verbindung.zu))
    }

    private fun erzeugtZyklus(karte: KartenDaten, von: KnotenId, zu: KnotenId): Boolean {
        if (von == zu) return true
        val nachfolger = karte.verbindungen.groupBy { it.von.knotenId }.mapValues { e -> e.value.map { it.zu.knotenId } }
        val offen = ArrayDeque<KnotenId>().apply { add(zu) }
        val besucht = mutableSetOf<KnotenId>()
        while (offen.isNotEmpty()) {
            val aktuell = offen.removeFirst()
            if (!besucht.add(aktuell)) continue
            if (aktuell == von) return true
            nachfolger[aktuell].orEmpty().forEach { offen.add(it) }
        }
        return false
    }
}

fun KartenDaten.findeAnschluss(ref: AnschlussVerweis): AnschlussDaten? =
    knoten.firstOrNull { it.id == ref.knotenId }?.anschlüsse?.firstOrNull { it.id == ref.anschlussId }
