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

internal fun gleichmäßigeFlächenAuflösung(gewünschteAuflösung: Int, budget: Int): Int =
    minOf(gewünschteAuflösung.coerceAtLeast(2), sqrt(budget.toDouble()).toInt().coerceAtLeast(2))

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
        2 -> if (konfiguration.sampling.vorschau) {
            vorschauFläche(parameterBereiche[0], parameterBereiche[1], konfiguration, auswerten, abbruchPrüfen)
        } else fläche(parameterBereiche[0], parameterBereiche[1], konfiguration, auswerten, abbruchPrüfen)
        else -> AdaptiveMethodenErgebnis(emptyList(), emptyList(), emptyList(), emptyList(), VisualisierungsStatistik())
    }

    private fun kurve(
        bereich: ZahlenBereich,
        c: VisualisierungsKonfiguration,
        auswerten: (List<Double>) -> AdaptivePunktAuswertung,
        abbruchPrüfen: () -> Unit,
    ): AdaptiveMethodenErgebnis {
        val ziel = zielFehler(c, c.sampling.auflösung1D)
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
            val tm = t0 + (t1 - t0) / 2.0
            val m = wert(tm)
            val pixelZiel = c.sampling.bildBreite > 0 && c.sampling.bildHöhe > 0
            val gültig = a.punkt != null && b.punkt != null && m.punkt != null
            val krümmung = if (gültig) normierterAbstand(m.punkt!!, mittelpunkt(a.punkt!!, b.punkt!!), c) else Double.POSITIVE_INFINITY
            val kantenLänge = if (gültig) normierterAbstand(a.punkt!!, b.punkt!!, c) else Double.POSITIVE_INFINITY
            if (tiefe < maximaleTiefe && (krümmung > ziel * (if (pixelZiel) 1.0 else .35) || !pixelZiel && kantenLänge > ziel)) {
                val q1 = t0 + (tm - t0) / 2; val q3 = tm + (t1 - tm) / 2
                val benötigt = listOf(q1, q3).count { it !in cache }
                if (cache.size + benötigt <= c.sampling.maximalesRasterBudget) {
                    wert(q1); wert(q3)
                    teile(t0, tm, a, m, tiefe + 1)
                    teile(tm, t1, m, b, tiefe + 1)
                    return
                }
                budgetErschöpft = true
            }
            // Keine Brücke über undefinierte Stellen oder einen ungelösten großen Sprung.
            if (gültig && krümmung < 1.0) segmente += a.punkt!! to b.punkt!!
        }
        val startSegmente = minOf(if (c.sampling.bildBreite > 0) c.sampling.auflösung1D - 1 else 8,
            c.sampling.auflösung1D - 1, (c.sampling.maximalesRasterBudget - 1) / 2).coerceAtLeast(1)
        // Vor der ersten Verfeinerung alle Startsegmente inklusive Mittelpunkt sichern.
        val start = List(startSegmente + 1) { index ->
            bereich.minimum + (bereich.maximum - bereich.minimum) * index / startSegmente
        }
        start.forEach(::wert)
        start.zipWithNext { a, b -> wert(a + (b - a) / 2) }
        repeat(startSegmente) { index ->
            val t0 = start[index]; val t1 = start[index + 1]
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
        val ziel = zielFehler(c, c.sampling.auflösung2D)
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
            // Reserve all four child cells before descending: refinement of the first
            // child must never consume the samples needed to cover its siblings.
            fun kinderVorbereiten(): Boolean {
                val stellen = listOf(
                    Schlüssel(um, v0), Schlüssel(um, v1), Schlüssel(u0, vm), Schlüssel(u1, vm),
                    Schlüssel((u0 + (um - u0) / 2), (v0 + (vm - v0) / 2)),
                    Schlüssel((um + (u1 - um) / 2), (v0 + (vm - v0) / 2)),
                    Schlüssel((u0 + (um - u0) / 2), (vm + (v1 - vm) / 2)),
                    Schlüssel((um + (u1 - um) / 2), (vm + (v1 - vm) / 2)),
                )
                if (cache.size + stellen.count { it !in cache } > c.sampling.maximalesRasterBudget) {
                    budgetErschöpft = true
                    return false
                }
                stellen.forEach { wert(it.u, it.v) }
                return true
            }
            val gültig = listOf(a, b, d, e, m).all { it.punkt != null }
            if (!gültig) {
                if (!budgetErschöpft && tiefe < maximaleTiefe && kinderVorbereiten()) {
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
            if (!budgetErschöpft && tiefe < maximaleTiefe && (krümmung > ziel || c.sampling.bildBreite <= 0 && kantenFehler > ziel) && kinderVorbereiten()) {
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

    private fun vorschauFläche(
        uBereich: ZahlenBereich,
        vBereich: ZahlenBereich,
        c: VisualisierungsKonfiguration,
        auswerten: (List<Double>) -> AdaptivePunktAuswertung,
        abbruchPrüfen: () -> Unit,
    ): AdaptiveMethodenErgebnis {
        val gewünschteAuflösung = c.sampling.auflösung2D.coerceAtLeast(2)
        val auflösung = gleichmäßigeFlächenAuflösung(
            gewünschteAuflösung,
            c.sampling.maximalesRasterBudget,
        )
        val diagnosen = linkedSetOf<String>()
        val dreiecke = mutableListOf<VisualisierungsDreieck>()
        val raster = List(auflösung) { uIndex ->
            List(auflösung) { vIndex ->
                abbruchPrüfen()
                val u = uBereich.minimum + (uBereich.maximum - uBereich.minimum) * uIndex / (auflösung - 1)
                val v = vBereich.minimum + (vBereich.maximum - vBereich.minimum) * vIndex / (auflösung - 1)
                auswerten(listOf(u, v)).also { it.diagnose?.let(diagnosen::add) }
            }
        }
        // Eine grobe Vorschau darf unerkannte Definitionslücken nicht mit Flächen überbrücken.
        val punkte = raster.flatten().mapNotNull { it.punkt }.distinct()
        return AdaptiveMethodenErgebnis(
            punkte,
            emptyList(),
            dreiecke,
            diagnosen.toList(),
            VisualisierungsStatistik(
                auswertungen = auflösung * auflösung,
                stützpunkte = punkte.size,
                linien = 0,
                dreiecke = dreiecke.size,
                zellen = 0,
                maximaleTiefe = 0,
                budgetErschöpft = auflösung < gewünschteAuflösung,
            ),
        )
    }

    private fun zielFehler(c: VisualisierungsKonfiguration, auflösung: Int): Double =
        if (c.sampling.bildBreite > 0 && c.sampling.bildHöhe > 0)
            c.sampling.zielFehlerPixel / maxOf(c.sampling.bildBreite, c.sampling.bildHöhe)
        else 1.0 / (auflösung - 1).coerceAtLeast(1)

    private fun normierterAbstand(a: VisualisierungsPunkt, b: VisualisierungsPunkt, c: VisualisierungsKonfiguration): Double {
        val bereiche = if (c.dimension == de.TeutonStudio.MathematikKnoten.visualisierung.modell.RaumDimension.R3) c.bereiche else c.sampling.sichtBereiche ?: c.bereiche
        val dx = (a.x - b.x) / (bereiche.x.maximum - bereiche.x.minimum)
        val dy = (a.y - b.y) / (bereiche.y.maximum - bereiche.y.minimum)
        val dz = if (a.z != null && b.z != null && bereiche.z != null) {
            (a.z - b.z) / (bereiche.z.maximum - bereiche.z.minimum)
        } else 0.0
        if (c.sampling.bildBreite > 0 && c.sampling.bildHöhe > 0) {
            val maximum = maxOf(c.sampling.bildBreite, c.sampling.bildHöhe)
            val px = dx * c.sampling.bildBreite / maximum
            val py = dy * c.sampling.bildHöhe / maximum
            return sqrt(px * px + py * py + dz * dz)
        }
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun mittelpunkt(a: VisualisierungsPunkt, b: VisualisierungsPunkt) = VisualisierungsPunkt(
        x = (a.x + b.x) / 2.0,
        y = (a.y + b.y) / 2.0,
        z = if (a.z != null && b.z != null) (a.z + b.z) / 2.0 else null,
        farbwert = if (a.farbwert != null && b.farbwert != null) (a.farbwert + b.farbwert) / 2.0 else a.farbwert ?: b.farbwert,
    )
}
