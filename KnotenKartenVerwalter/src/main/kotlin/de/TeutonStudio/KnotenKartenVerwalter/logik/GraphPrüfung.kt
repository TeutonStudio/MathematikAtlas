package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

sealed interface VerbindungsPrüfung {
    data object Erlaubt : VerbindungsPrüfung
    data class Abgelehnt(val grund: String) : VerbindungsPrüfung
}

class GraphPrüfung(
    private val arten: AnschlussArtRegister,
    private val typen: TypSystem = StandardTypSystem(),
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

        when (val semantik = prüfeSemantik(probe, ausgang.second, eingang.second)) {
            TypPrüfung.Kompatibel, TypPrüfung.Unbestimmt -> Unit
            is TypPrüfung.Inkompatibel -> return VerbindungsPrüfung.Abgelehnt(semantik.grund)
        }
        fehlendeAnforderung(probe, ausgang.second, eingang.second)?.let { anforderung ->
            return VerbindungsPrüfung.Abgelehnt("Die Quelle erfüllt die Anforderung '${anforderung.id}' nicht.")
        }

        if (erzeugtZyklus(ohneAlteEingangsVerbindung, ausgang.second.knotenId, eingang.second.knotenId)) {
            return VerbindungsPrüfung.Abgelehnt("Zirkuläre Verbindungen sind nicht erlaubt.")
        }

        val ungültigeFolgeVerbindung = probe.verbindungen.firstOrNull { !istTypkompatibel(probe, it) }
        if (ungültigeFolgeVerbindung != null) {
            val von = effektiveArt(probe, ungültigeFolgeVerbindung.von)
            val zu = effektiveArt(probe, ungültigeFolgeVerbindung.zu)
            return VerbindungsPrüfung.Abgelehnt(
                "Die Verbindung würde einen abhängigen Ausgang von $von auf einen inkompatiblen Typ ändern.",
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

    /** Liefert den semantischen Typ einschließlich Typinferenz und Legacy-Fallback. */
    fun effektiverTyp(karte: KartenDaten, ref: AnschlussVerweis): TypAusdruck =
        typen.normalisiere(effektiverTyp(karte, ref, mutableSetOf()))

    fun effektiverVertrag(karte: KartenDaten, ref: AnschlussVerweis): AnschlussVertrag {
        val anschluss = karte.findeAnschluss(ref) ?: return AnschlussVertrag()
        return AnschlussVertrag(
            typ = effektiverTyp(karte, ref),
            anforderungen = effektiveAnforderungen(karte, ref, mutableSetOf()).distinct(),
        )
    }

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

    private fun effektiverTyp(
        karte: KartenDaten,
        ref: AnschlussVerweis,
        besucht: MutableSet<AnschlussVerweis>,
    ): TypAusdruck {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return TypAusdruck.Unbekannt
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return TypAusdruck.Unbekannt
        if (!besucht.add(ref)) return deklarierterOderFallbackTyp(anschluss)

        fun quellTyp(eingangsName: String): TypAusdruck? {
            val eingang = knoten.anschlüsse.firstOrNull {
                it.name == eingangsName && it.richtung == AnschlussRichtung.Eingang
            } ?: return null
            val eingangsRef = AnschlussVerweis(knoten.id, eingang.id)
            val quelle = karte.verbindungen.firstOrNull { it.zu == eingangsRef }?.von ?: return null
            return effektiverTyp(karte, quelle, besucht.toMutableSet())
        }

        return when (val regel = anschluss.typInferenz) {
            null -> deklarierterOderFallbackTyp(anschluss)
            is TypInferenzRegel.Fest -> regel.typ
            is TypInferenzRegel.FolgtEingang -> quellTyp(regel.eingang) ?: deklarierterOderFallbackTyp(anschluss)
            is TypInferenzRegel.GemeinsameOberart -> {
                typen.gemeinsameOberart(regel.eingänge.mapNotNull(::quellTyp)) ?: deklarierterOderFallbackTyp(anschluss)
            }
            is TypInferenzRegel.Priorisierung -> {
                val quellen = regel.eingänge.mapNotNull(::quellTyp)
                regel.prioritäten.firstOrNull { priorität ->
                    quellen.any { typen.prüfe(it, priorität) == TypPrüfung.Kompatibel }
                } ?: deklarierterOderFallbackTyp(anschluss)
            }
            is TypInferenzRegel.TupelAus -> {
                val komponenten = regel.eingänge.mapNotNull(::quellTyp)
                if (komponenten.size == regel.eingänge.size) {
                    TypAusdruck.Parameterisiert(TypKernIds.Tupel, komponenten)
                } else deklarierterOderFallbackTyp(anschluss)
            }
            is TypInferenzRegel.KomponenteVonTupel -> {
                val tupel = quellTyp(regel.eingang) as? TypAusdruck.Parameterisiert
                if (tupel?.konstruktor == TypKernIds.Tupel) {
                    tupel.argumente.getOrNull(regel.index) ?: deklarierterOderFallbackTyp(anschluss)
                } else deklarierterOderFallbackTyp(anschluss)
            }
        }
    }

    private fun deklarierterOderFallbackTyp(anschluss: AnschlussDaten): TypAusdruck =
        anschluss.vertrag.typ.takeUnless { it == TypAusdruck.Unbekannt }
            ?: typen.typFürAnschlussArt(anschluss.art)

    private fun effektiveAnforderungen(
        karte: KartenDaten,
        ref: AnschlussVerweis,
        besucht: MutableSet<AnschlussVerweis>,
    ): List<TypAnforderung> {
        val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return emptyList()
        val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return emptyList()
        if (!besucht.add(ref)) return anschluss.vertrag.anforderungen
        val folgt = anschluss.typInferenz as? TypInferenzRegel.FolgtEingang ?: return anschluss.vertrag.anforderungen
        val eingang = knoten.anschlüsse.firstOrNull {
            it.name == folgt.eingang && it.richtung == AnschlussRichtung.Eingang
        } ?: return anschluss.vertrag.anforderungen
        val quelle = karte.verbindungen.firstOrNull { it.zu == AnschlussVerweis(knoten.id, eingang.id) }?.von
            ?: return anschluss.vertrag.anforderungen
        return anschluss.vertrag.anforderungen + effektiveAnforderungen(karte, quelle, besucht)
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
        if (anschluss.vertrag.typ == normalisiert) return karte
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

    private fun prüfeSemantik(karte: KartenDaten, ausgang: AnschlussVerweis, eingang: AnschlussVerweis): TypPrüfung =
        typen.prüfe(effektiverTyp(karte, ausgang), effektiverTyp(karte, eingang))

    private fun fehlendeAnforderung(
        karte: KartenDaten,
        ausgang: AnschlussVerweis,
        eingang: AnschlussVerweis,
    ): TypAnforderung? {
        val vorhanden = effektiverVertrag(karte, ausgang).anforderungen.toSet()
        return effektiverVertrag(karte, eingang).anforderungen.firstOrNull { it !in vorhanden }
    }

    private fun istTypkompatibel(karte: KartenDaten, verbindung: VerbindungDaten): Boolean {
        val von = karte.findeAnschluss(verbindung.von) ?: return false
        val zu = karte.findeAnschluss(verbindung.zu) ?: return false
        val (ausgang, eingang) = richte(von, verbindung.von, zu, verbindung.zu) ?: return false
        val ausgangsArt = effektiveArt(karte, ausgang.second)
        if (eingang.first.zulässigeArten.isNotEmpty() && eingang.first.zulässigeArten.none { erlaubt -> arten.istUnterart(ausgangsArt, erlaubt) }) return false
        if (!arten.istUnterart(ausgangsArt, effektiveArt(karte, eingang.second))) return false
        if (prüfeSemantik(karte, ausgang.second, eingang.second) is TypPrüfung.Inkompatibel) return false
        return fehlendeAnforderung(karte, ausgang.second, eingang.second) == null
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
