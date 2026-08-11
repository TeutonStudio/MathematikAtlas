package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.TypSystem.*

sealed interface VerbindungsPrüfung {
    data object Erlaubt : VerbindungsPrüfung
    data class Abgelehnt(val grund: String) : VerbindungsPrüfung
}

class GraphPrüfung(
    private val arten: AnschlussArtRegister,
    private val typSystem: TypSystem = legacyTypSystem(arten),
) {
    fun prüfe(karte: KartenDaten, erster: AnschlussVerweis, zweiter: AnschlussVerweis): VerbindungsPrüfung {
        if (erster == zweiter) return VerbindungsPrüfung.Abgelehnt("Ein Anschluss kann nicht mit sich selbst verbunden werden.")
        val a = karte.findeAnschluss(erster) ?: return VerbindungsPrüfung.Abgelehnt("Erster Anschluss fehlt.")
        val b = karte.findeAnschluss(zweiter) ?: return VerbindungsPrüfung.Abgelehnt("Zweiter Anschluss fehlt.")
        val (ausgang, eingang) = richte(a, erster, b, zweiter)
            ?: return VerbindungsPrüfung.Abgelehnt("Die Anschlussrichtungen sind nicht kompatibel.")

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
        if (eingang.first.zulässigeArten.isNotEmpty() && eingang.first.zulässigeArten.none { erlaubt -> arten.istUnterart(ausgangsArt, erlaubt) }) {
            return VerbindungsPrüfung.Abgelehnt("$ausgangsArt ist für diesen Eingang nicht zulässig.")
        }
        if (!arten.istUnterart(ausgangsArt, eingangsArt)) {
            return VerbindungsPrüfung.Abgelehnt("$ausgangsArt kann nicht an $eingangsArt angeschlossen werden.")
        }

        val ausgangsTyp = effektiverTyp(probe, ausgang.second)
        val eingangsTyp = effektiverTyp(probe, eingang.second)
        when (val typPrüfung = typSystem.prüfe(ausgangsTyp, eingangsTyp)) {
            TypPrüfung.Kompatibel, is TypPrüfung.Unbestimmt -> Unit
            is TypPrüfung.Inkompatibel -> return VerbindungsPrüfung.Abgelehnt(typPrüfung.grund)
        }
        when (val anforderung = typSystem.prüfeAnforderungen(ausgangsTyp, eingang.first.vertrag.anforderungen)) {
            TypPrüfung.Kompatibel, is TypPrüfung.Unbestimmt -> Unit
            is TypPrüfung.Inkompatibel -> return VerbindungsPrüfung.Abgelehnt(anforderung.grund)
        }

        if (erzeugtZyklus(ohneAlteEingangsVerbindung, ausgang.second.knotenId, eingang.second.knotenId)) {
            return VerbindungsPrüfung.Abgelehnt("Zirkuläre Verbindungen sind nicht erlaubt.")
        }

        val ungültigeFolgeVerbindung = probe.verbindungen.firstOrNull { !istTypkompatibel(probe, it) }
        if (ungültigeFolgeVerbindung != null) {
            val von = effektiverTyp(probe, ungültigeFolgeVerbindung.von)
            val zu = effektiverTyp(probe, ungültigeFolgeVerbindung.zu)
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

    /** Liefert die deklarierte oder aus verbundenen Eingängen zentral abgeleitete Anschlussart. */
    fun effektiveArt(karte: KartenDaten, ref: AnschlussVerweis): AnschlussArtId =
        effektiveArt(karte, ref, mutableSetOf())

    private fun effektiveArt(
        karte: KartenDaten,
        ref: AnschlussVerweis,
        besucht: MutableSet<AnschlussVerweis>,
    ): AnschlussArtId {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return AnschlussArtId("unbekannt")
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return AnschlussArtId("unbekannt")

        anschluss.artAbbildungVonEingang?.let { regel ->
            if (!besucht.add(ref)) return anschluss.art
            val eingang = knoten.anschlüsse.firstOrNull {
                it.name == regel.eingang && it.richtung == AnschlussRichtung.Eingang
            } ?: return anschluss.art
            val eingangsRef = AnschlussVerweis(knoten.id, eingang.id)
            val quelle = karte.verbindungen.firstOrNull { it.zu == eingangsRef }?.von ?: return anschluss.art
            val quellArt = effektiveArt(karte, quelle, besucht)
            return regel.abbildung[quellArt]
                ?: regel.abbildung.entries.firstOrNull { (von, _) -> arten.istUnterart(quellArt, von) }?.value
                ?: anschluss.art
        }

        anschluss.artFolgtEingang?.let { eingangsName ->
            if (!besucht.add(ref)) return anschluss.art
            val eingang = knoten.anschlüsse.firstOrNull {
                it.name == eingangsName && it.richtung == AnschlussRichtung.Eingang
            } ?: return anschluss.art
            val eingangsRef = AnschlussVerweis(knoten.id, eingang.id)
            val quelle = karte.verbindungen.firstOrNull { it.zu == eingangsRef }?.von ?: return anschluss.art
            return effektiveArt(karte, quelle, besucht)
        }

        anschluss.artPriorisiertEingänge?.let { regel ->
            if (!besucht.add(ref)) return anschluss.art
            val quellArten = regel.eingänge.mapNotNull { eingangsName ->
                val eingang = knoten.anschlüsse.firstOrNull {
                    it.name == eingangsName && it.richtung == AnschlussRichtung.Eingang
                } ?: return@mapNotNull null
                val eingangsRef = AnschlussVerweis(knoten.id, eingang.id)
                val quelle = karte.verbindungen.firstOrNull { it.zu == eingangsRef }?.von
                    ?: return@mapNotNull null
                effektiveArt(karte, quelle, besucht.toMutableSet())
            }
            return regel.prioritäten.firstOrNull { priorität ->
                quellArten.any { quellArt -> arten.istUnterart(quellArt, priorität) }
            } ?: anschluss.art
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
     * Liefert den G0.2-Typ eines Anschlusses. Noch nicht migrierte Anschlüsse werden
     * verlustfrei als Atom ihrer effektiven Anschlussart behandelt.
     */
    fun effektiverTyp(karte: KartenDaten, ref: AnschlussVerweis): TypAusdruck =
        effektiverTyp(karte, ref, mutableSetOf())

    private fun effektiverTyp(
        karte: KartenDaten,
        ref: AnschlussVerweis,
        besucht: MutableSet<AnschlussVerweis>,
    ): TypAusdruck {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return TypAusdruck.Unbekannt
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return TypAusdruck.Unbekannt
        if (!besucht.add(ref)) return deklarierterTyp(karte, ref, anschluss)

        val inferiert = anschluss.typInferenz?.let { regel ->
            inferiereTyp(karte, knoten, anschluss, regel, besucht)
        }
        return typSystem.normalisiere(inferiert ?: deklarierterTyp(karte, ref, anschluss))
    }

    private fun deklarierterTyp(
        karte: KartenDaten,
        ref: AnschlussVerweis,
        anschluss: AnschlussDaten,
    ): TypAusdruck = if (anschluss.vertrag.typ != TypAusdruck.Unbekannt) {
        anschluss.vertrag.typ
    } else {
        TypAusdruck.Atom(TypId(effektiveArt(karte, ref).wert))
    }

    private fun inferiereTyp(
        karte: KartenDaten,
        knoten: KnotenDaten,
        anschluss: AnschlussDaten,
        regel: TypInferenzRegel,
        besucht: MutableSet<AnschlussVerweis>,
    ): TypAusdruck? {
        fun quellTyp(eingangsName: String): TypAusdruck? {
            val eingang = knoten.anschlüsse.firstOrNull {
                it.name == eingangsName && it.richtung == AnschlussRichtung.Eingang
            } ?: return null
            val eingangsRef = AnschlussVerweis(knoten.id, eingang.id)
            val quelle = karte.verbindungen.firstOrNull { it.zu == eingangsRef }?.von ?: return null
            return effektiverTyp(karte, quelle, besucht.toMutableSet())
        }

        fun quellTypen(eingänge: List<String>): List<TypAusdruck> = eingänge.mapNotNull(::quellTyp)

        return when (regel) {
            is TypInferenzRegel.FolgtEingang -> quellTyp(regel.eingang)
            is TypInferenzRegel.GemeinsameOberart -> {
                val typen = quellTypen(regel.eingänge)
                typSystem.gemeinsameOberart(typen)
            }
            is TypInferenzRegel.Vereinigung -> {
                val typen = quellTypen(regel.eingänge)
                if (typen.isEmpty()) null else typSystem.normalisiere(TypAusdruck.Vereinigung(typen))
            }
            is TypInferenzRegel.TupelAus -> {
                val typen = quellTypen(regel.eingänge)
                if (typen.size != regel.eingänge.size) null
                else TypAusdruck.Parameterisiert(TypId("typ.tupel"), typen)
            }
            is TypInferenzRegel.KomponenteVonTupel -> {
                val typ = quellTyp(regel.eingang) as? TypAusdruck.Parameterisiert
                if (typ?.konstruktor == TypId("typ.tupel")) typ.argumente.getOrNull(regel.index) else null
            }
            is TypInferenzRegel.AbbildungVonEingang -> {
                val quellTyp = quellTyp(regel.eingang) ?: return null
                regel.abbildung[quellTyp]
                    ?: regel.abbildung.entries.firstOrNull { (von, _) ->
                        typSystem.prüfe(quellTyp, von) is TypPrüfung.Kompatibel
                    }?.value
            }
            is TypInferenzRegel.Priorisierung -> {
                val typen = quellTypen(regel.eingänge)
                regel.prioritäten.firstOrNull { priorität ->
                    typen.any { typ -> typSystem.prüfe(typ, priorität) is TypPrüfung.Kompatibel }
                }
            }
        } ?: if (anschluss.vertrag.typ != TypAusdruck.Unbekannt) anschluss.vertrag.typ else null
    }

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

    fun ändereAnschlussVertrag(
        karte: KartenDaten,
        ref: AnschlussVerweis,
        vertrag: AnschlussVertrag,
    ): KartenDaten {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return karte
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return karte
        if (anschluss.vertrag == vertrag) return karte
        val geändert = karte.copy(knoten = karte.knoten.map {
            if (it.id == knoten.id) it.copy(anschlüsse = it.anschlüsse.map { a ->
                if (a.id == anschluss.id) a.copy(vertrag = vertrag) else a
            }) else it
        })
        return geändert.copy(verbindungen = geändert.verbindungen.filter { istTypkompatibel(geändert, it) })
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
        val (_, eingang) = richte(von, verbindung.von, zu, verbindung.zu) ?: return false
        val ausgangsArt = effektiveArt(karte, verbindung.von)
        if (eingang.first.zulässigeArten.isNotEmpty() && eingang.first.zulässigeArten.none { erlaubt -> arten.istUnterart(ausgangsArt, erlaubt) }) return false
        if (!arten.istUnterart(ausgangsArt, effektiveArt(karte, verbindung.zu))) return false

        val ausgangsTyp = effektiverTyp(karte, verbindung.von)
        val eingangsTyp = effektiverTyp(karte, verbindung.zu)
        if (typSystem.prüfe(ausgangsTyp, eingangsTyp) is TypPrüfung.Inkompatibel) return false
        if (typSystem.prüfeAnforderungen(ausgangsTyp, eingang.first.vertrag.anforderungen) is TypPrüfung.Inkompatibel) return false
        return true
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

private fun legacyTypSystem(arten: AnschlussArtRegister): TypSystem = StandardTypSystem(
    istAtomUntertyp = { von, erwartet ->
        arten.istUnterart(AnschlussArtId(von.wert), AnschlussArtId(erwartet.wert))
    },
    konstruktoren = listOf(
        TypKonstruktorDefinition(TypId("typ.tupel"), emptyList()),
    ),
)

fun KartenDaten.findeAnschluss(ref: AnschlussVerweis): AnschlussDaten? =
    knoten.firstOrNull { it.id == ref.knotenId }?.anschlüsse?.firstOrNull { it.id == ref.anschlussId }
