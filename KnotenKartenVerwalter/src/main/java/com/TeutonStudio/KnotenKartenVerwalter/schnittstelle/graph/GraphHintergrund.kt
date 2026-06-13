package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

interface GraphHintergrund {

    enum class RasterArt {
        Punkte,
        Linien,
    }

    enum class RasterTesselation {
        Trigon,
        Quadgon,
        Hexagon,
    }

    @Composable
    fun Hintergrund(
        zustand: KarteZustand,
        rasterGröße: Float,
        modifier: Modifier = Modifier,
        vordergrund: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = modifier.fillMaxSize().clipToBounds(),
        ) {
            Raster(
                zustand = zustand,
                rasterGröße = rasterGröße,
                modifier = Modifier.matchParentSize(),
                rasterArt = zustand.rasterEinstellung.first,
                rasterTesselation = zustand.rasterEinstellung.second,
            )

            vordergrund()
        }
    }

    @Composable
    private fun Raster(
        zustand: KarteZustand,
        rasterGröße: Float,
        modifier: Modifier = Modifier,
        rasterArt: RasterArt,
        rasterTesselation: RasterTesselation,
    ) {
        val rasterFarbe =
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)

        Canvas(
            modifier = modifier.fillMaxSize(),
        ) {
            val zoom = zustand.zoom //.coerceAtLeast(0.0001f)
            val grundGröße = rasterGröße.coerceAtLeast(0.0001f)

            /*
             * Beim Herauszoomen werden nicht einfach die Pixelabstände
             * vergrößert. Stattdessen werden ganze Rasterstufen übersprungen.
             *
             * Dadurch bleibt das Raster weiterhin auf Vielfachen der
             * ursprünglichen Welt-Rastergröße ausgerichtet.
             */
            val mindestAbstand = when (rasterTesselation) {
                RasterTesselation.Quadgon -> 10f
                RasterTesselation.Trigon -> 12f
                RasterTesselation.Hexagon -> 18f
            }

            val rasterStufe = ceil(
                mindestAbstand / (grundGröße * zoom),
            ).toInt().coerceAtLeast(1)

            val weltRasterGröße = grundGröße * rasterStufe

            val sichtbareWelt = Rect(
                left = -zustand.pos.x / zoom,
                top = -zustand.pos.y / zoom,
                right = (size.width - zustand.pos.x) / zoom,
                bottom = (size.height - zustand.pos.y) / zoom,
            )

            val punktRadius = 1.4.dp.toPx()
            val linienBreite = 1.dp.toPx()

            when (rasterTesselation) {
                RasterTesselation.Quadgon -> zeichneQuadRaster(
                    sichtbareWelt = sichtbareWelt,
                    rasterGröße = weltRasterGröße,
                    verschiebung = zustand.pos,
                    zoom = zoom,
                    rasterArt = rasterArt,
                    farbe = rasterFarbe,
                    punktRadius = punktRadius,
                    linienBreite = linienBreite,
                )

                RasterTesselation.Trigon -> zeichneTrigonRaster(
                    sichtbareWelt = sichtbareWelt,
                    rasterGröße = weltRasterGröße,
                    verschiebung = zustand.pos,
                    zoom = zoom,
                    rasterArt = rasterArt,
                    farbe = rasterFarbe,
                    punktRadius = punktRadius,
                    linienBreite = linienBreite,
                )

                RasterTesselation.Hexagon -> zeichneHexagonRaster(
                    sichtbareWelt = sichtbareWelt,
                    rasterGröße = weltRasterGröße,
                    verschiebung = zustand.pos,
                    zoom = zoom,
                    rasterArt = rasterArt,
                    farbe = rasterFarbe,
                    punktRadius = punktRadius,
                    linienBreite = linienBreite,
                )
            }
        }
    }

    private fun DrawScope.zeichneQuadRaster(
        sichtbareWelt: Rect,
        rasterGröße: Float,
        verschiebung: Offset,
        zoom: Float,
        rasterArt: RasterArt,
        farbe: Color,
        punktRadius: Float,
        linienBreite: Float,
    ) {
        val ersteSpalte = floor(sichtbareWelt.left / rasterGröße).toInt() - 1
        val letzteSpalte = ceil(sichtbareWelt.right / rasterGröße).toInt() + 1
        val ersteZeile = floor(sichtbareWelt.top / rasterGröße).toInt() - 1
        val letzteZeile = ceil(sichtbareWelt.bottom / rasterGröße).toInt() + 1

        when (rasterArt) {
            RasterArt.Punkte -> {
                for (zeile in ersteZeile..letzteZeile) {
                    val weltY = zeile * rasterGröße
                    val bildY = weltY * zoom + verschiebung.y

                    for (spalte in ersteSpalte..letzteSpalte) {
                        val weltX = spalte * rasterGröße
                        val bildX = weltX * zoom + verschiebung.x

                        drawCircle(
                            color = farbe,
                            radius = punktRadius,
                            center = Offset(bildX, bildY),
                        )
                    }
                }
            }

            RasterArt.Linien -> {
                for (spalte in ersteSpalte..letzteSpalte) {
                    val weltX = spalte * rasterGröße
                    val bildX = weltX * zoom + verschiebung.x

                    drawLine(
                        color = farbe,
                        start = Offset(bildX, 0f),
                        end = Offset(bildX, size.height),
                        strokeWidth = linienBreite,
                    )
                }

                for (zeile in ersteZeile..letzteZeile) {
                    val weltY = zeile * rasterGröße
                    val bildY = weltY * zoom + verschiebung.y

                    drawLine(
                        color = farbe,
                        start = Offset(0f, bildY),
                        end = Offset(size.width, bildY),
                        strokeWidth = linienBreite,
                    )
                }
            }
        }
    }

    private fun DrawScope.zeichneTrigonRaster(
        sichtbareWelt: Rect,
        rasterGröße: Float,
        verschiebung: Offset,
        zoom: Float,
        rasterArt: RasterArt,
        farbe: Color,
        punktRadius: Float,
        linienBreite: Float,
    ) {
        val dreieckHöhe = rasterGröße * SQRT_3 / 2f

        val ersteZeile =
            floor(sichtbareWelt.top / dreieckHöhe).toInt() - 2

        val letzteZeile =
            ceil(sichtbareWelt.bottom / dreieckHöhe).toInt() + 2

        for (zeile in ersteZeile..letzteZeile) {
            val versatzX =
                if ((zeile and 1) != 0) rasterGröße / 2f else 0f

            val weltY = zeile * dreieckHöhe

            val ersteSpalte = floor(
                (sichtbareWelt.left - versatzX) / rasterGröße,
            ).toInt() - 2

            val letzteSpalte = ceil(
                (sichtbareWelt.right - versatzX) / rasterGröße,
            ).toInt() + 2

            for (spalte in ersteSpalte..letzteSpalte) {
                val weltPunkt = Offset(
                    x = spalte * rasterGröße + versatzX,
                    y = weltY,
                )

                val bildPunkt = weltPunkt.zuBildschirm(
                    verschiebung = verschiebung,
                    zoom = zoom,
                )

                when (rasterArt) {
                    RasterArt.Punkte -> {
                        drawCircle(
                            color = farbe,
                            radius = punktRadius,
                            center = bildPunkt,
                        )
                    }

                    RasterArt.Linien -> {
                        val rechts = Offset(
                            x = weltPunkt.x + rasterGröße,
                            y = weltPunkt.y,
                        ).zuBildschirm(verschiebung, zoom)

                        val untenLinks = Offset(
                            x = weltPunkt.x - rasterGröße / 2f,
                            y = weltPunkt.y + dreieckHöhe,
                        ).zuBildschirm(verschiebung, zoom)

                        val untenRechts = Offset(
                            x = weltPunkt.x + rasterGröße / 2f,
                            y = weltPunkt.y + dreieckHöhe,
                        ).zuBildschirm(verschiebung, zoom)

                        drawLine(
                            color = farbe,
                            start = bildPunkt,
                            end = rechts,
                            strokeWidth = linienBreite,
                        )

                        drawLine(
                            color = farbe,
                            start = bildPunkt,
                            end = untenLinks,
                            strokeWidth = linienBreite,
                        )

                        drawLine(
                            color = farbe,
                            start = bildPunkt,
                            end = untenRechts,
                            strokeWidth = linienBreite,
                        )
                    }
                }
            }
        }
    }

    private fun DrawScope.zeichneHexagonRaster(
        sichtbareWelt: Rect,
        rasterGröße: Float,
        verschiebung: Offset,
        zoom: Float,
        rasterArt: RasterArt,
        farbe: Color,
        punktRadius: Float,
        linienBreite: Float,
    ) {
        /*
         * rasterGröße ist hier die Seitenlänge eines Hexagons.
         *
         * Verwendet wird ein flach liegendes Hexagon:
         *
         *     ______
         *    /      \
         *    \______/
         */
        val seitenLänge = rasterGröße
        val hexagonHöhe = SQRT_3 * seitenLänge
        val spaltenAbstand = seitenLänge * 1.5f

        val ersteSpalte =
            floor(sichtbareWelt.left / spaltenAbstand).toInt() - 2

        val letzteSpalte =
            ceil(sichtbareWelt.right / spaltenAbstand).toInt() + 2

        for (spalte in ersteSpalte..letzteSpalte) {
            val weltX = spalte * spaltenAbstand

            val zeilenVersatz =
                if ((spalte and 1) != 0) hexagonHöhe / 2f else 0f

            val ersteZeile = floor(
                (sichtbareWelt.top - zeilenVersatz) / hexagonHöhe,
            ).toInt() - 2

            val letzteZeile = ceil(
                (sichtbareWelt.bottom - zeilenVersatz) / hexagonHöhe,
            ).toInt() + 2

            for (zeile in ersteZeile..letzteZeile) {
                val weltMitte = Offset(
                    x = weltX,
                    y = zeile * hexagonHöhe + zeilenVersatz,
                )

                val bildMitte = weltMitte.zuBildschirm(
                    verschiebung = verschiebung,
                    zoom = zoom,
                )

                when (rasterArt) {
                    /*
                     * Beim Punktraster werden die Mittelpunkte der
                     * Hexagonfelder gezeichnet.
                     */
                    RasterArt.Punkte -> {
                        drawCircle(
                            color = farbe,
                            radius = punktRadius,
                            center = bildMitte,
                        )
                    }

                    RasterArt.Linien -> {
                        zeichneHexagonLinien(
                            mitte = bildMitte,
                            seitenLänge = seitenLänge * zoom,
                            farbe = farbe,
                            linienBreite = linienBreite,
                        )
                    }
                }
            }
        }
    }

    private fun DrawScope.zeichneHexagonLinien(
        mitte: Offset,
        seitenLänge: Float,
        farbe: Color,
        linienBreite: Float,
    ) {
        val punkte = Array(6) { index ->
            val winkel = index * PI / 3.0

            Offset(
                x = mitte.x + cos(winkel).toFloat() * seitenLänge,
                y = mitte.y + sin(winkel).toFloat() * seitenLänge,
            )
        }

        /*
         * Nur die obere Hälfte wird gezeichnet.
         *
         * Die untere Hälfte wird jeweils vom benachbarten Hexagon
         * übernommen. Dadurch werden gemeinsame Kanten nicht doppelt
         * gezeichnet und erscheinen nicht dunkler.
         */
        val pfad = Path().apply {
            moveTo(punkte[0].x, punkte[0].y)
            lineTo(punkte[1].x, punkte[1].y)
            lineTo(punkte[2].x, punkte[2].y)
            lineTo(punkte[3].x, punkte[3].y)
        }

        drawPath(
            path = pfad,
            color = farbe,
            style = Stroke(width = linienBreite),
        )
    }

    private fun Offset.zuBildschirm(
        verschiebung: Offset,
        zoom: Float,
    ): Offset = Offset(
        x = x * zoom + verschiebung.x,
        y = y * zoom + verschiebung.y,
    )

    private companion object {
        /**
         * Primzahlen für die Aufstiegslogik.
         *
         * Ebene 1 ist fest "jeder 3.".
         * Für Ebene 2..15 werden daraus die Faktoren (p + 1):
         * 3 -> 4, 5 -> 6, 7 -> 8, 11 -> 12, ...
         */
        val AUFSTIEGS_PRIMZAHLEN = listOf(
            3L, 5L, 7L, 11L, 13L, 17L, 19L,
            23L, 29L, 31L, 37L, 41L, 43L, 47L
        )

        /**
         * Gesamtperioden pro Ebene.
         *
         * Beispiel:
         * Ebene 1 = 3
         * Ebene 2 = 3 * 4 = 12
         * Ebene 3 = 3 * 4 * 6 = 72
         * Ebene 4 = 3 * 4 * 6 * 8 = 576
         * ...
         *
         * Insgesamt 15 Ebenen.
         */
        val PUNKT_EBENEN_PERIODEN: List<Long> = buildList {
            var periode = 3L
            add(periode) // Ebene 1

            for (prime in AUFSTIEGS_PRIMZAHLEN) {
                if (size >= 15) break
                periode *= (prime + 1L)
                add(periode)
            }
        }
        val SQRT_3: Float = sqrt(3f)
    }

    /**
     * Ermittelt, auf welcher Hierarchie-Ebene ein Index liegt.
     *
     * 0 = normal
     * 1 = Ebene 1
     * 2 = Ebene 2
     * ...
     * 15 = Ebene 15
     */
    private fun punktEbene(index: Int): Int {
        val wert = abs(index.toLong())

        // Die Achse / der Ursprung soll maximal betont werden.
        if (wert == 0L) return PUNKT_EBENEN_PERIODEN.size

        var ebene = 0

        for ((i, periode) in PUNKT_EBENEN_PERIODEN.withIndex()) {
            if (wert % periode == 0L) {
                ebene = i + 1
            } else {
                // Da jede höhere Ebene ein Vielfaches der vorherigen ist,
                // können wir hier abbrechen.
                break
            }
        }

        return ebene
    }

    /**
     * Radius abhängig von der Hierarchie-Ebene.
     *
     * Kannst du natürlich nach Geschmack aggressiver oder sanfter machen.
     */
    private fun punktRadiusFürEbene(basisRadius: Float, ebene: Int): Float {
        if (ebene <= 0) return basisRadius

        val faktor = when (ebene.coerceAtMost(15)) {
            1 -> 1.35f
            2 -> 1.65f
            3 -> 2.0f
            4 -> 2.35f
            5 -> 2.75f
            6 -> 3.2f
            7 -> 3.7f
            8 -> 4.2f
            9 -> 4.8f
            10 -> 5.4f
            11 -> 6.1f
            12 -> 6.9f
            13 -> 7.8f
            14 -> 8.8f
            15 -> 10.0f
            else -> 1f
        }

        return basisRadius * faktor
    }
}