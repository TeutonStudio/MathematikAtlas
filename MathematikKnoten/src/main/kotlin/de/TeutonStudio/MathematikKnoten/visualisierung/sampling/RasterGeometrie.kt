package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.ZahlenBereich
import kotlin.math.abs

internal data class RasterGeometrieErgebnis(
    val zellen: List<VisualisierungsZelle> = emptyList(),
    val linien: List<VisualisierungsLinie> = emptyList(),
    val dreiecke: List<VisualisierungsDreieck> = emptyList(),
)

/** Baut ausschließlich numerisch gekennzeichnete Geometrie aus einem regulären Raster. */
internal object RasterGeometrie {
    fun erzeuge(
        dimension: Int,
        auflösung: Int,
        bereiche: List<ZahlenBereich>,
        werte: Map<List<Int>, NumerischeMitgliedschaft>,
    ): RasterGeometrieErgebnis {
        if (auflösung < 2 || dimension !in 1..3) return RasterGeometrieErgebnis()
        val enthalten = werte.filterValues { it == NumerischeMitgliedschaft.Enthalten }.keys
        val zellen = if (dimension >= 2) enthalten.map { index -> rasterZelle(index, auflösung, bereiche) } else emptyList()
        val linien = if (dimension == 2) konturen2D(auflösung, bereiche, werte) else emptyList()
        val isoDreiecke = if (dimension == 3) isoFläche3D(auflösung, bereiche, werte) else emptyList()
        val volumenRand = if (dimension == 3) voxelRand3D(enthalten.toSet(), auflösung, bereiche) else emptyList()
        return RasterGeometrieErgebnis(zellen, linien, isoDreiecke + volumenRand)
    }

    private fun rasterZelle(index: List<Int>, n: Int, bereiche: List<ZahlenBereich>): VisualisierungsZelle {
        val minimum = index.mapIndexed { achse, i ->
            val b = bereiche[achse]
            val schritt = (b.maximum - b.minimum) / (n - 1)
            (b.minimum + i * schritt - schritt / 2.0).coerceAtLeast(b.minimum)
        }
        val maximum = index.mapIndexed { achse, i ->
            val b = bereiche[achse]
            val schritt = (b.maximum - b.minimum) / (n - 1)
            (b.minimum + i * schritt + schritt / 2.0).coerceAtMost(b.maximum)
        }
        return VisualisierungsZelle(minimum, maximum, ZellenStatus.NumerischEnthalten, "Numerische Rasterfüllung.")
    }

    private fun konturen2D(
        n: Int,
        bereiche: List<ZahlenBereich>,
        werte: Map<List<Int>, NumerischeMitgliedschaft>,
    ): List<VisualisierungsLinie> {
        val ergebnis = mutableListOf<VisualisierungsLinie>()
        for (x in 0 until n - 1) for (y in 0 until n - 1) {
            val ecken = listOf(listOf(x, y), listOf(x + 1, y), listOf(x + 1, y + 1), listOf(x, y + 1))
            val residuen = ecken.map { (werte[it] as? NumerischeMitgliedschaft.Grenze)?.residuum }
            if (residuen.any { it == null }) continue
            val schnitte = mutableListOf<VisualisierungsPunkt>()
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0).forEach { (a, b) ->
                schnittpunkt(ecken[a], residuen[a]!!, ecken[b], residuen[b]!!, n, bereiche)?.let(schnitte::add)
            }
            when (schnitte.size) {
                2 -> ergebnis += VisualisierungsLinie(schnitte, DarstellungsNachweis.Numerisch)
                4 -> {
                    ergebnis += VisualisierungsLinie(schnitte.take(2), DarstellungsNachweis.Numerisch)
                    ergebnis += VisualisierungsLinie(schnitte.drop(2), DarstellungsNachweis.Numerisch)
                }
            }
        }
        return ergebnis
    }

    private fun isoFläche3D(
        n: Int,
        bereiche: List<ZahlenBereich>,
        werte: Map<List<Int>, NumerischeMitgliedschaft>,
    ): List<VisualisierungsDreieck> {
        val ergebnis = mutableListOf<VisualisierungsDreieck>()
        val tetraeder = listOf(
            intArrayOf(0, 1, 3, 7), intArrayOf(0, 3, 2, 7), intArrayOf(0, 2, 6, 7),
            intArrayOf(0, 6, 4, 7), intArrayOf(0, 4, 5, 7), intArrayOf(0, 5, 1, 7),
        )
        val kanten = listOf(0 to 1, 0 to 2, 0 to 3, 1 to 2, 1 to 3, 2 to 3)
        for (x in 0 until n - 1) for (y in 0 until n - 1) for (z in 0 until n - 1) {
            val ecken = List(8) { maske -> listOf(x + (maske and 1), y + ((maske shr 1) and 1), z + ((maske shr 2) and 1)) }
            val residuen = ecken.map { (werte[it] as? NumerischeMitgliedschaft.Grenze)?.residuum }
            if (residuen.any { it == null }) continue
            tetraeder.forEach { tetra ->
                val schnitte = kanten.mapNotNull { (ia, ib) ->
                    val a = tetra[ia]; val b = tetra[ib]
                    schnittpunkt(ecken[a], residuen[a]!!, ecken[b], residuen[b]!!, n, bereiche)
                }.distinctBy { Triple(it.x, it.y, it.z) }
                if (schnitte.size == 3) {
                    ergebnis += VisualisierungsDreieck(schnitte[0], schnitte[1], schnitte[2])
                } else if (schnitte.size == 4) {
                    ergebnis += VisualisierungsDreieck(schnitte[0], schnitte[1], schnitte[2])
                    ergebnis += VisualisierungsDreieck(schnitte[0], schnitte[2], schnitte[3])
                }
            }
        }
        return ergebnis
    }

    private fun voxelRand3D(
        enthalten: Set<List<Int>>,
        n: Int,
        bereiche: List<ZahlenBereich>,
    ): List<VisualisierungsDreieck> {
        val ergebnis = mutableListOf<VisualisierungsDreieck>()
        val richtungen = listOf(
            listOf(-1, 0, 0), listOf(1, 0, 0), listOf(0, -1, 0),
            listOf(0, 1, 0), listOf(0, 0, -1), listOf(0, 0, 1),
        )
        enthalten.forEach { index ->
            val zelle = rasterZelle(index, n, bereiche)
            richtungen.forEachIndexed { fläche, delta ->
                val nachbar = index.indices.map { index[it] + delta[it] }
                if (nachbar in enthalten) return@forEachIndexed
                val x = doubleArrayOf(zelle.minimum[0], zelle.maximum[0])
                val y = doubleArrayOf(zelle.minimum[1], zelle.maximum[1])
                val z = doubleArrayOf(zelle.minimum[2], zelle.maximum[2])
                val vier = when (fläche) {
                    0 -> listOf(p(x[0], y[0], z[0]), p(x[0], y[1], z[0]), p(x[0], y[1], z[1]), p(x[0], y[0], z[1]))
                    1 -> listOf(p(x[1], y[0], z[0]), p(x[1], y[0], z[1]), p(x[1], y[1], z[1]), p(x[1], y[1], z[0]))
                    2 -> listOf(p(x[0], y[0], z[0]), p(x[0], y[0], z[1]), p(x[1], y[0], z[1]), p(x[1], y[0], z[0]))
                    3 -> listOf(p(x[0], y[1], z[0]), p(x[1], y[1], z[0]), p(x[1], y[1], z[1]), p(x[0], y[1], z[1]))
                    4 -> listOf(p(x[0], y[0], z[0]), p(x[1], y[0], z[0]), p(x[1], y[1], z[0]), p(x[0], y[1], z[0]))
                    else -> listOf(p(x[0], y[0], z[1]), p(x[0], y[1], z[1]), p(x[1], y[1], z[1]), p(x[1], y[0], z[1]))
                }
                ergebnis += VisualisierungsDreieck(vier[0], vier[1], vier[2], DarstellungsNachweis.Numerisch)
                ergebnis += VisualisierungsDreieck(vier[0], vier[2], vier[3], DarstellungsNachweis.Numerisch)
            }
        }
        return ergebnis
    }

    private fun schnittpunkt(
        a: List<Int>,
        ra: Double,
        b: List<Int>,
        rb: Double,
        n: Int,
        bereiche: List<ZahlenBereich>,
    ): VisualisierungsPunkt? {
        if (!ra.isFinite() || !rb.isFinite()) return null
        if (ra != 0.0 && rb != 0.0 && (ra > 0.0) == (rb > 0.0)) return null
        val anteil = if (abs(ra) + abs(rb) == 0.0) 0.5 else abs(ra) / (abs(ra) + abs(rb))
        val koordinaten = a.indices.map { achse ->
            val raster = a[achse] + (b[achse] - a[achse]) * anteil
            val bereich = bereiche[achse]
            bereich.minimum + raster / (n - 1) * (bereich.maximum - bereich.minimum)
        }
        return VisualisierungsPunkt(koordinaten[0], koordinaten.getOrElse(1) { 0.0 }, koordinaten.getOrNull(2))
    }

    private fun p(x: Double, y: Double, z: Double) = VisualisierungsPunkt(x, y, z)
}
