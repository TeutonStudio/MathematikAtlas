package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.VisualisierungsKonfiguration
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.ZahlenBereich
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

internal data class AdaptivePunktAuswertung(
    val punkt: VisualisierungsPunkt?,
    val diagnose: String? = null,
)

internal data class AdaptiveMethodenErgebnis(
    val punkte: List<VisualisierungsPunkt>,
    val linien: List<VisualisierungsLinie>,
    val dreiecke: List<VisualisierungsDreieck>,
    val diagnosen: List<String>,
    val statistik: VisualisierungsStatistik,
)

/**
 * Adaptiver, bildraumbezogener Mesher für kontinuierliche ein- und zweiparametrige
 * Methoden. Die Kamera ist absichtlich kein Eingang: Genauigkeit bezieht sich auf
 * die persistierten Achsenspannen und bleibt beim Navigieren stabil.
 */
internal object AdaptiveMethodenGeometrie {
    fun sample(
        parameterBereiche: List<ZahlenBereich>,
        konfiguration: VisualisierungsKonfiguration,
        auswerten: (List<Double>) -> AdaptivePunktAuswertung,
        abbruchPrüfen: () -> Unit,
    ): AdaptiveMethodenErgebnis = when (parameterBereiche.size) {
        1 -> kurve(parameterBereiche.single(), konfiguration, auswerten, abbruchPrüfen)
        2 -> fläche(parameterBereiche[0], parameterBereiche[1], konfiguration, auswerten, abbruchPrüfen)
        else -> AdaptiveMethodenErgebnis(emptyList(), emptyList(), emptyList(), emptyList(), VisualisierungsStatistik())
    }

    private fun kurve(
        bereich: ZahlenBereich,
        c: VisualisierungsKonfiguration,
        auswerten: (List<Double>) -> AdaptivePunktAuswertung,
        abbruchPrüfen: () -> Unit,
    ): AdaptiveMethodenErgebnis {
        val ziel = 1.0 / (c.sampling.auflösung1D - 1).coerceAtLeast(1)
        val maximaleTiefe = (ceil(ln(c.sampling.auflösung1D.toDouble()) / ln(2.0)).toInt() + 4).coerceAtMost(24)
        val cache = linkedMapOf<Double, AdaptivePunktAuswertung>()
        val diagnosen = linkedSetOf<String>()
        var budgetErschöpft = false
        var erreichteTiefe = 0
        fun wert(t: Double): AdaptivePunktAuswertung {
            cache[t]?.let { return it }
            abbruchPrüfen()
            if (cache.size >= c.sampling.maximalesRasterBudget) {
                budgetErschöpft = true
                return AdaptivePunktAuswertung(null, "Auswertungsbudget ausgeschöpft.")
            }
            return auswerten(listOf(t)).also {
                cache[t] = it
                it.diagnose?.let(diagnosen::add)
            }
        }
        val segmente = mutableListOf<Pair<VisualisierungsPunkt, VisualisierungsPunkt>>()
        fun teile(t0: Double, t1: Double, a: AdaptivePunktAuswertung, b: AdaptivePunktAuswertung, tiefe: Int) {
            erreichteTiefe = max(erreichteTiefe, tiefe)
            if (a.punkt == null || b.punkt == null) return
            val tm = t0 + (t1 - t0) / 2.0
            val m = wert(tm)
            if (m.punkt == null) return
            val kantenLänge = normierterAbstand(a.punkt, b.punkt, c)
            val krümmung = normierterAbstand(m.punkt, mittelpunkt(a.punkt, b.punkt), c)
            if (!budgetErschöpft && tiefe < maximaleTiefe && (kantenLänge > ziel || krümmung > ziel * 0.35)) {
                teile(t0, tm, a, m, tiefe + 1)
                teile(tm, t1, m, b, tiefe + 1)
            } else {
                segmente += a.punkt to b.punkt
            }
        }
        val startSegmente = minOf(8, c.sampling.auflösung1D - 1).coerceAtLeast(1)
        repeat(startSegmente) { index ->
            val t0 = bereich.minimum + (bereich.maximum - bereich.minimum) * index / startSegmente
            val t1 = bereich.minimum + (bereich.maximum - bereich.minimum) * (index + 1) / startSegmente
            teile(t0, t1, wert(t0), wert(t1), 0)
        }
        val linien = segmente.map { (a, b) -> VisualisierungsLinie(listOf(a, b)) }
        val punkte = segmente.flatMap { listOf(it.first, it.second) }.distinct()
        return AdaptiveMethodenErgebnis(
            punkte,
            linien,
            emptyList(),
            diagnosen.toList(),
            VisualisierungsStatistik(cache.size, punkte.size, linien.size, 0, 0, erreichteTiefe, budgetErschöpft),
        )
    }

    private fun fläche(
        uBereich: ZahlenBereich,
        vBereich: ZahlenBereich,
        c: VisualisierungsKonfiguration,
        auswerten: (List<Double>) -> AdaptivePunktAuswertung,
        abbruchPrüfen: () -> Unit,
    ): AdaptiveMethodenErgebnis {
        data class Schlüssel(val u: Double, val v: Double)
        val ziel = 1.0 / (c.sampling.auflösung2D - 1).coerceAtLeast(1)
        val maximaleTiefe = (ceil(ln(c.sampling.auflösung2D.toDouble()) / ln(2.0)).toInt() + 3).coerceAtMost(18)
        val cache = linkedMapOf<Schlüssel, AdaptivePunktAuswertung>()
        val diagnosen = linkedSetOf<String>()
        val dreiecke = mutableListOf<VisualisierungsDreieck>()
        var budgetErschöpft = false
        var erreichteTiefe = 0
        fun wert(u: Double, v: Double): AdaptivePunktAuswertung {
            val schlüssel = Schlüssel(u, v)
            cache[schlüssel]?.let { return it }
            abbruchPrüfen()
            if (cache.size >= c.sampling.maximalesRasterBudget) {
                budgetErschöpft = true
                return AdaptivePunktAuswertung(null, "Auswertungsbudget ausgeschöpft.")
            }
            return auswerten(listOf(u, v)).also {
                cache[schlüssel] = it
                it.diagnose?.let(diagnosen::add)
            }
        }
        fun teile(u0: Double, u1: Double, v0: Double, v1: Double, tiefe: Int) {
            erreichteTiefe = max(erreichteTiefe, tiefe)
            val um = u0 + (u1 - u0) / 2.0
            val vm = v0 + (v1 - v0) / 2.0
            val a = wert(u0, v0)
            val b = wert(u1, v0)
            val d = wert(u0, v1)
            val e = wert(u1, v1)
            val m = wert(um, vm)
            val gültig = listOf(a, b, d, e, m).all { it.punkt != null }
            if (!gültig) {
                if (!budgetErschöpft && tiefe < maximaleTiefe) {
                    teile(u0, um, v0, vm, tiefe + 1)
                    teile(um, u1, v0, vm, tiefe + 1)
                    teile(u0, um, vm, v1, tiefe + 1)
                    teile(um, u1, vm, v1, tiefe + 1)
                }
                return
            }
            val pa = a.punkt!!; val pb = b.punkt!!; val pd = d.punkt!!; val pe = e.punkt!!; val pm = m.punkt!!
            val kantenFehler = maxOf(
                normierterAbstand(pa, pb, c), normierterAbstand(pb, pe, c),
                normierterAbstand(pe, pd, c), normierterAbstand(pd, pa, c),
            )
            val bilinear = mittelpunkt(mittelpunkt(pa, pe), mittelpunkt(pb, pd))
            val krümmung = normierterAbstand(pm, bilinear, c)
            if (!budgetErschöpft && tiefe < maximaleTiefe && (kantenFehler > ziel || krümmung > ziel * 0.35)) {
                teile(u0, um, v0, vm, tiefe + 1)
                teile(um, u1, v0, vm, tiefe + 1)
                teile(u0, um, vm, v1, tiefe + 1)
                teile(um, u1, vm, v1, tiefe + 1)
            } else {
                // Mittelpunktfächer vermeiden eine willkürliche Diagonale und halten gekrümmte Zellen stabil.
                dreiecke += VisualisierungsDreieck(pa, pb, pm)
                dreiecke += VisualisierungsDreieck(pb, pe, pm)
                dreiecke += VisualisierungsDreieck(pe, pd, pm)
                dreiecke += VisualisierungsDreieck(pd, pa, pm)
            }
        }
        teile(uBereich.minimum, uBereich.maximum, vBereich.minimum, vBereich.maximum, 0)
        val punkte = dreiecke.flatMap { listOf(it.a, it.b, it.c) }.distinct()
        return AdaptiveMethodenErgebnis(
            punkte,
            emptyList(),
            dreiecke,
            diagnosen.toList(),
            VisualisierungsStatistik(cache.size, punkte.size, 0, dreiecke.size, 0, erreichteTiefe, budgetErschöpft),
        )
    }

    private fun normierterAbstand(a: VisualisierungsPunkt, b: VisualisierungsPunkt, c: VisualisierungsKonfiguration): Double {
        val dx = (a.x - b.x) / (c.bereiche.x.maximum - c.bereiche.x.minimum)
        val dy = (a.y - b.y) / (c.bereiche.y.maximum - c.bereiche.y.minimum)
        val dz = if (a.z != null && b.z != null && c.bereiche.z != null) {
            (a.z - b.z) / (c.bereiche.z.maximum - c.bereiche.z.minimum)
        } else 0.0
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun mittelpunkt(a: VisualisierungsPunkt, b: VisualisierungsPunkt) = VisualisierungsPunkt(
        x = (a.x + b.x) / 2.0,
        y = (a.y + b.y) / 2.0,
        z = if (a.z != null && b.z != null) (a.z + b.z) / 2.0 else null,
        farbwert = if (a.farbwert != null && b.farbwert != null) (a.farbwert + b.farbwert) / 2.0 else a.farbwert ?: b.farbwert,
    )
}
