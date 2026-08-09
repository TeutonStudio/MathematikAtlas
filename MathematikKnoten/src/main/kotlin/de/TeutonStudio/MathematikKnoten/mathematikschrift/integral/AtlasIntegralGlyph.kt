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
 * Die Geometrie entspricht `atlas-integral-v2.svg`. ViewBox und Strichparameter
 * bleiben mit der bisherigen Glyphe identisch, damit die LaTeX-Anordnung unverändert
 * weiterverwendet werden kann.
 */
@Composable
internal fun AtlasIntegralGlyph(
    modifier: Modifier = Modifier,
    farbe: Color = LocalContentColor.current,
) {
    val pfad = remember { atlasIntegralV2Pfad() }
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

private fun atlasIntegralV2Pfad(): Path = Path().apply {
    moveTo(98.5f, 291.5f)
    cubicTo(100.99974f, 309.2541f, 118.19254f, 320.85295f, 135.26467f, 321.78275f)
    cubicTo(152.0269f, 324.54848f, 173.26514f, 316.86574f, 176.4575f, 298.26625f)
    cubicTo(182.58052f, 280.55361f, 175.75971f, 261.19625f, 163.34501f, 247.98128f)
    cubicTo(139.29723f, 220.1421f, 101.65056f, 210.82844f, 73.524798f, 188.2103f)
    cubicTo(56.822509f, 175.18996f, 41.491627f, 158.23742f, 38.112148f, 136.54706f)
    cubicTo(28.360697f, 81.225008f, 77.33798f, 23.40349f, 133.90516f, 25.963125f)
    cubicTo(162.8652f, 25.405986f, 192.75657f, 37.570113f, 209.55007f, 61.825917f)
    cubicTo(216.49993f, 71.289333f, 218.89265f, 82.797438f, 220.89617f, 94.12f)
}
