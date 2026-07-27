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
        if (!arten.istUnterart(ausgang.first.art, eingang.first.art)) {
            return VerbindungsPrüfung.Abgelehnt("${ausgang.first.art} kann nicht an ${eingang.first.art} angeschlossen werden.")
        }
        // Ein belegter Eingang wird beim Verbinden atomar ersetzt. Die Zyklusprüfung
        // muss daher bereits den Graphen nach dieser Ersetzung untersuchen, nicht den
        // vorübergehend noch doppelt gedachten Zwischenstand.
        val prüfKarte = if (eingang.first.richtung == AnschlussRichtung.Eingang) {
            karte.copy(verbindungen = karte.verbindungen.filterNot { it.zu == eingang.second })
        } else karte
        if (erzeugtZyklus(prüfKarte, ausgang.second.knotenId, eingang.second.knotenId)) {
            return VerbindungsPrüfung.Abgelehnt("Zirkuläre Verbindungen sind nicht erlaubt.")
        }
        return VerbindungsPrüfung.Erlaubt
    }

    fun normalisiere(karte: KartenDaten, erster: AnschlussVerweis, zweiter: AnschlussVerweis): Pair<AnschlussVerweis, AnschlussVerweis>? {
        val a = karte.findeAnschluss(erster) ?: return null
        val b = karte.findeAnschluss(zweiter) ?: return null
        return richte(a, erster, b, zweiter)?.let { it.first.second to it.second.second }
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
        val (ausgang, eingang) = richte(von, verbindung.von, zu, verbindung.zu) ?: return false
        return arten.istUnterart(ausgang.first.art, eingang.first.art)
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
