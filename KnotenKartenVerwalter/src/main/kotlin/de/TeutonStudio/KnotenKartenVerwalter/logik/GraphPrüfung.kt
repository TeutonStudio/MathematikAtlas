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
        if (eingang.first.richtung == AnschlussRichtung.Eingang && karte.verbindungen.any { it.zu == eingang.second }) {
            return VerbindungsPrüfung.Abgelehnt("Ein Eingang darf nur eine eingehende Verbindung besitzen.")
        }
        if (erzeugtZyklus(karte, ausgang.second.knotenId, eingang.second.knotenId)) {
            return VerbindungsPrüfung.Abgelehnt("Zirkuläre Verbindungen sind nicht erlaubt.")
        }
        return VerbindungsPrüfung.Erlaubt
    }

    fun normalisiere(karte: KartenDaten, erster: AnschlussVerweis, zweiter: AnschlussVerweis): Pair<AnschlussVerweis, AnschlussVerweis>? {
        val a = karte.findeAnschluss(erster) ?: return null
        val b = karte.findeAnschluss(zweiter) ?: return null
        return richte(a, erster, b, zweiter)?.let { it.first.second to it.second.second }
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
