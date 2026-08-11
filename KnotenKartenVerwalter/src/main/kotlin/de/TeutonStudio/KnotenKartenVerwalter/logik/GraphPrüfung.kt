package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

sealed interface VerbindungsPrüfung {
    data object Erlaubt : VerbindungsPrüfung
    data class Abgelehnt(val grund: String) : VerbindungsPrüfung
}

class GraphPrüfung(
    private val arten: AnschlussArtRegister,
    private val typen: TypSystem = StandardTypSystem.ausAnschlussArten(arten),
    private val anforderungen: TypAnforderungsPrüfer = KeineTypAnforderungsPrüfung,
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

        val quellTyp = effektiverTyp(probe, ausgang.second)
        val zielTyp = effektiverTyp(probe, eingang.second)
        when (val typPrüfung = typen.prüfe(quellTyp, zielTyp)) {
            TypPrüfung.Kompatibel, TypPrüfung.Unbestimmt -> Unit
            is TypPrüfung.Inkompatibel -> return VerbindungsPrüfung.Abgelehnt(typPrüfung.grund)
        }
        when (val anforderungsPrüfung = anforderungen.prüfe(quellTyp, eingang.first.vertrag.anforderungen)) {
            TypPrüfung.Kompatibel, TypPrüfung.Unbestimmt -> Unit
            is TypPrüfung.Inkompatibel -> return VerbindungsPrüfung.Abgelehnt(anforderungsPrüfung.grund)
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
            val quelle = quelleFürEingang(karte, knoten, regel.eingang) ?: return anschluss.art
            val quellArt = effektiveArt(karte, quelle, besucht)
            return regel.abbildung[quellArt]
                ?: regel.abbildung.entries.firstOrNull { (von, _) -> arten.istUnterart(quellArt, von) }?.value
                ?: anschluss.art
        }

        anschluss.artFolgtEingang?.let { eingangsName ->
            if (!besucht.add(ref)) return anschluss.art
            val quelle = quelleFürEingang(karte, knoten, eingangsName) ?: return anschluss.art
            return effektiveArt(karte, quelle, besucht)
        }

        anschluss.artPriorisiertEingänge?.let { regel ->
            if (!besucht.add(ref)) return anschluss.art
            val quellArten = regel.eingänge.mapNotNull { eingangsName ->
                val quelle = quelleFürEingang(karte, knoten, eingangsName) ?: return@mapNotNull null
                effektiveArt(karte, quelle, besucht.toMutableSet())
            }
            return regel.prioritäten.firstOrNull { priorität ->
                quellArten.any { quellArt -> arten.istUnterart(quellArt, priorität) }
            } ?: anschluss.art
        }

        if (anschluss.artVereinigtEingänge.isEmpty()) return anschluss.art
        if (!besucht.add(ref)) return anschluss.art
        val quellArten = anschluss.artVereinigtEingänge.mapNotNull { eingangsName ->
            val quelle = quelleFürEingang(karte, knoten, eingangsName) ?: return@mapNotNull null
            effektiveArt(karte, quelle, besucht.toMutableSet())
        }
        return arten.gemeinsameOberart(quellArten) ?: anschluss.art
    }

    /**
     * Liefert den effektiven semantischen Typ. Fehlt ein expliziter G0.2-Vertrag,
     * wird die effektive AnschlussArt konservativ als atomarer Typ gespiegelt.
     */
    fun effektiverTyp(karte: KartenDaten, ref: AnschlussVerweis): TypAusdruck =
        effektiverTyp(karte, ref, mutableSetOf())

    private fun effektiverTyp(
        karte: KartenDaten,
        ref: AnschlussVerweis,
        besucht: MutableSet<AnschlussVerweis>,
    ): TypAusdruck {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId }
            ?: return TypAusdruck.Unbekannt
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId }
            ?: return TypAusdruck.Unbekannt
        if (!besucht.add(ref)) return typFallback(karte, ref, anschluss)

        val inferiert = when (val regel = anschluss.typInferenz) {
            null -> null
            is TypInferenzRegel.FolgtEingang -> {
                quelleFürEingang(karte, knoten, regel.eingang)?.let { quelle ->
                    effektiverTyp(karte, quelle, besucht.toMutableSet())
                }
            }
            is TypInferenzRegel.GemeinsameOberart -> {
                val quellTypen = quellTypen(karte, knoten, regel.eingänge, besucht)
                typen.gemeinsameOberart(quellTypen)
            }
            is TypInferenzRegel.VereinigungAusEingängen -> {
                val quellTypen = quellTypen(karte, knoten, regel.eingänge, besucht)
                quellTypen.takeIf { it.isNotEmpty() }?.let { typen.normalisiere(TypAusdruck.Vereinigung(it)) }
            }
            is TypInferenzRegel.AbbildungVonEingang -> {
                val quelle = quelleFürEingang(karte, knoten, regel.eingang)
                val quellTyp = quelle?.let { effektiverTyp(karte, it, besucht.toMutableSet()) }
                quellTyp?.let { typ ->
                    regel.fälle.firstOrNull { fall ->
                        typen.normalisiere(typ) == typen.normalisiere(fall.von) ||
                            typen.prüfe(typ, fall.von) == TypPrüfung.Kompatibel
                    }?.zu
                }
            }
            is TypInferenzRegel.TupelAusEingängen -> {
                val quellTypen = quellTypen(karte, knoten, regel.eingänge, besucht)
                quellTypen.takeIf { it.isNotEmpty() }?.let {
                    TypAusdruck.Parameterisiert(regel.konstruktor, it)
                }
            }
            is TypInferenzRegel.KomponenteAusEingang -> {
                val quelle = quelleFürEingang(karte, knoten, regel.eingang)
                val quellTyp = quelle?.let { effektiverTyp(karte, it, besucht.toMutableSet()) }
                quellTyp?.let { komponentenTyp(it, regel.index, regel.konstruktor) }
            }
        }

        return typen.normalisiere(inferiert ?: typFallback(karte, ref, anschluss))
    }

    private fun typFallback(karte: KartenDaten, ref: AnschlussVerweis, anschluss: AnschlussDaten): TypAusdruck =
        anschluss.vertrag.typ.takeUnless { it == TypAusdruck.Unbekannt }
            ?: TypAusdruck.Atom(TypId(effektiveArt(karte, ref).wert))

    private fun quellTypen(
        karte: KartenDaten,
        knoten: KnotenDaten,
        eingänge: List<String>,
        besucht: Set<AnschlussVerweis>,
    ): List<TypAusdruck> = eingänge.mapNotNull { name ->
        quelleFürEingang(karte, knoten, name)?.let { quelle ->
            effektiverTyp(karte, quelle, besucht.toMutableSet())
        }
    }

    private fun komponentenTyp(typ: TypAusdruck, index: Int, konstruktor: TypId?): TypAusdruck? = when (val norm = typen.normalisiere(typ)) {
        is TypAusdruck.Parameterisiert -> if (konstruktor == null || norm.konstruktor == konstruktor) {
            norm.argumente.getOrNull(index)
        } else null
        is TypAusdruck.Vereinigung -> {
            val komponenten = norm.alternativen.mapNotNull { komponentenTyp(it, index, konstruktor) }
            komponenten.takeIf { it.isNotEmpty() }?.let { typen.normalisiere(TypAusdruck.Vereinigung(it)) }
        }
        else -> null
    }

    private fun quelleFürEingang(karte: KartenDaten, knoten: KnotenDaten, eingangsName: String): AnschlussVerweis? {
        val eingang = knoten.anschlüsse.firstOrNull {
            it.name == eingangsName && it.richtung == AnschlussRichtung.Eingang
        } ?: return null
        return karte.verbindungen.firstOrNull { it.zu == AnschlussVerweis(knoten.id, eingang.id) }?.von
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

    fun ändereAnschlussTyp(karte: KartenDaten, ref: AnschlussVerweis, typ: TypAusdruck): KartenDaten {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return karte
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return karte
        val normalisiert = typen.normalisiere(typ)
        if (typen.normalisiere(anschluss.vertrag.typ) == normalisiert) return karte
        val mitNeuemTyp = karte.copy(knoten = karte.knoten.map {
            if (it.id == knoten.id) it.copy(anschlüsse = it.anschlüsse.map { a ->
                if (a.id == anschluss.id) a.copy(vertrag = a.vertrag.copy(typ = normalisiert)) else a
            }) else it
        })
        return mitNeuemTyp.copy(verbindungen = mitNeuemTyp.verbindungen.filter { istTypkompatibel(mitNeuemTyp, it) })
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
        val (ausgang, eingang) = richte(von, verbindung.von, zu, verbindung.zu) ?: return false
        val ausgangsArt = effektiveArt(karte, ausgang.second)
        if (eingang.first.zulässigeArten.isNotEmpty() && eingang.first.zulässigeArten.none { erlaubt -> arten.istUnterart(ausgangsArt, erlaubt) }) return false
        if (!arten.istUnterart(ausgangsArt, effektiveArt(karte, eingang.second))) return false
        val quellTyp = effektiverTyp(karte, ausgang.second)
        val zielTyp = effektiverTyp(karte, eingang.second)
        if (typen.prüfe(quellTyp, zielTyp) is TypPrüfung.Inkompatibel) return false
        if (anforderungen.prüfe(quellTyp, eingang.first.vertrag.anforderungen) is TypPrüfung.Inkompatibel) return false
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

fun KartenDaten.findeAnschluss(ref: AnschlussVerweis): AnschlussDaten? =
    knoten.firstOrNull { it.id == ref.knotenId }?.anschlüsse?.firstOrNull { it.id == ref.anschlussId }
