package de.TeutonStudio.MathematikKnoten.mathematikschrift.integral

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

private const val ATLAS_INTEGRAL_VIEWBOX_BREITE = 280f
private const val ATLAS_INTEGRAL_VIEWBOX_HOEHE = 350f
private const val ATLAS_INTEGRAL_STRICHBREITE = 12f

/**
 * Produktive Integralglyphe des Mathematik Atlas.
 *
 * Die Geometrie entspricht der vom Nutzer ausgewählten SVG-Variante 4. Der Pfad ist
 * bereits über die vertikale Achse gespiegelt, sodass zur Laufzeit keine zweite
 * Geometrie oder SVG-Transformation gepflegt werden muss.
 */
@Composable
internal fun AtlasIntegralGlyph(
    modifier: Modifier = Modifier,
    farbe: Color = LocalContentColor.current,
) {
    val pfad = remember { atlasIntegralVariante4Pfad() }
    Canvas(modifier = modifier) {
        val faktor = minOf(
            size.width / ATLAS_INTEGRAL_VIEWBOX_BREITE,
            size.height / ATLAS_INTEGRAL_VIEWBOX_HOEHE,
        )
        val breite = ATLAS_INTEGRAL_VIEWBOX_BREITE * faktor
        val hoehe = ATLAS_INTEGRAL_VIEWBOX_HOEHE * faktor
        val versatzX = (size.width - breite) / 2f
        val versatzY = (size.height - hoehe) / 2f

        withTransform({
            translate(left = versatzX, top = versatzY)
            scale(scaleX = faktor, scaleY = faktor, pivot = Offset.Zero)
        }) {
            drawPath(
                path = pfad,
                color = farbe,
                style = Stroke(
                    width = ATLAS_INTEGRAL_STRICHBREITE,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

private fun atlasIntegralVariante4Pfad(): Path = Path().apply {
    moveTo(98.5f, 291.5f)
    cubicTo(99.1f, 292.92f, 100.2f, 295.48f, 101.3f, 297.93f)
    cubicTo(102.41f, 300.39f, 103.69f, 302.81f, 105.22f, 305.08f)
    cubicTo(106.75f, 307.35f, 108.5f, 309.46f, 110.52f, 311.36f)
    cubicTo(112.55f, 313.25f, 114.84f, 314.95f, 117.35f, 316.35f)
    cubicTo(119.86f, 317.75f, 122.62f, 318.89f, 125.55f, 319.77f)
    cubicTo(128.49f, 320.65f, 131.61f, 321.24f, 134.85f, 321.56f)
    cubicTo(138.1f, 321.89f, 141.43f, 321.98f, 144.75f, 321.72f)
    cubicTo(148.07f, 321.46f, 151.34f, 320.88f, 154.46f, 319.93f)
    cubicTo(157.58f, 318.97f, 160.52f, 317.66f, 163.17f, 316.02f)
    cubicTo(165.82f, 314.38f, 168.19f, 312.41f, 170.18f, 310.16f)
    cubicTo(172.18f, 307.91f, 173.82f, 305.35f, 175.01f, 302.58f)
    cubicTo(176.2f, 299.81f, 176.96f, 296.82f, 177.17f, 293.66f)
    cubicTo(177.39f, 290.49f, 177.09f, 287.13f, 176.16f, 283.62f)
    cubicTo(175.22f, 280.1f, 173.69f, 276.42f, 171.55f, 272.63f)
    cubicTo(169.41f, 268.84f, 166.67f, 264.94f, 163.28f, 260.9f)
    cubicTo(159.89f, 256.87f, 155.87f, 252.79f, 151.28f, 248.6f)
    cubicTo(146.68f, 244.42f, 141.9f, 240.35f, 137.01f, 236.47f)
    cubicTo(132.13f, 232.59f, 127.01f, 228.66f, 122.03f, 224.76f)
    cubicTo(117.05f, 220.86f, 112.12f, 217.08f, 107.34f, 213.38f)
    cubicTo(102.55f, 209.67f, 97.86f, 206.15f, 93.34f, 202.73f)
    cubicTo(88.82f, 199.32f, 84.43f, 196.08f, 80.28f, 192.88f)
    cubicTo(76.13f, 189.68f, 72.16f, 186.57f, 68.42f, 183.46f)
    cubicTo(64.68f, 180.35f, 61.1f, 177.19f, 57.89f, 173.88f)
    cubicTo(54.67f, 170.58f, 51.69f, 167.12f, 49.09f, 163.42f)
    cubicTo(46.48f, 159.71f, 44.23f, 155.72f, 42.44f, 151.38f)
    cubicTo(40.65f, 147.05f, 39.23f, 142.45f, 38.33f, 137.46f)
    cubicTo(37.43f, 132.47f, 36.96f, 127.14f, 37.01f, 121.76f)
    cubicTo(37.05f, 116.38f, 37.59f, 110.91f, 38.59f, 105.58f)
    cubicTo(39.59f, 100.24f, 41.07f, 94.97f, 42.95f, 89.93f)
    cubicTo(44.83f, 84.89f, 47.13f, 80.03f, 49.78f, 75.44f)
    cubicTo(52.43f, 70.85f, 55.46f, 66.46f, 58.8f, 62.36f)
    cubicTo(62.14f, 58.27f, 65.78f, 54.38f, 69.65f, 50.83f)
    cubicTo(73.53f, 47.27f, 77.66f, 43.97f, 81.95f, 41.08f)
    cubicTo(86.25f, 38.18f, 90.73f, 35.63f, 95.33f, 33.54f)
    cubicTo(99.94f, 31.44f, 104.68f, 29.77f, 109.44f, 28.55f)
    cubicTo(114.2f, 27.33f, 118.95f, 26.54f, 123.61f, 26.16f)
    cubicTo(128.27f, 25.78f, 132.84f, 25.79f, 137.23f, 26.19f)
    cubicTo(141.62f, 26.59f, 145.85f, 27.36f, 149.8f, 28.47f)
    cubicTo(153.76f, 29.58f, 157.52f, 31.07f, 160.98f, 32.83f)
    cubicTo(164.44f, 34.58f, 167.65f, 36.6f, 170.57f, 38.87f)
    cubicTo(173.49f, 41.15f, 176.18f, 43.69f, 178.55f, 46.38f)
    cubicTo(180.92f, 49.08f, 183.05f, 51.99f, 184.87f, 55.01f)
    cubicTo(186.69f, 58.03f, 188.29f, 61.24f, 189.59f, 64.48f)
    cubicTo(190.89f, 67.72f, 191.98f, 71.07f, 192.79f, 74.41f)
    cubicTo(193.61f, 77.76f, 194.15f, 81.15f, 194.46f, 84.44f)
    cubicTo(194.77f, 87.74f, 194.92f, 91.02f, 194.91f, 94.12f)
}