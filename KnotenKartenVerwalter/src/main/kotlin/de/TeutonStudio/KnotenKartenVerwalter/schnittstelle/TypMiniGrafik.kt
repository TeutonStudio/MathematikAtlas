package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypVisualDescriptor
import de.TeutonStudio.TypSystem.TypVisualMuster
import de.TeutonStudio.TypSystem.TypVisualSegment
import de.TeutonStudio.TypSystem.zuVisualDescriptor

/**
 * Kleine, Orchestrator-artige Typgrafik. Die Komponente kennt keine mathematischen
 * Farben; Domänen liefern diese über [farbeFürSegment]. Vereinigungstypen werden
 * diagonal gestreift, zusammengesetzte Typen erhalten mehrere kompakte Segmente.
 */
@Composable
fun TypMiniGrafik(
    descriptor: TypVisualDescriptor,
    modifier: Modifier = Modifier,
    farbeFürSegment: @Composable (TypVisualSegment) -> Color = { MaterialTheme.colorScheme.primary },
) {
    val fallback = MaterialTheme.colorScheme.primary
    val rahmen = MaterialTheme.colorScheme.surface
    val segmente = descriptor.segmente.ifEmpty {
        listOf(TypVisualSegment("typ", descriptor.kurzLabel))
    }
    val farben = mutableListOf<Color>()
    for (segment in segmente) {
        farben += farbeFürSegment(segment)
    }
    if (farben.isEmpty()) farben += fallback

    Box(
        modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(fallback)
            .border(2.dp, rahmen, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (descriptor.muster) {
                TypVisualMuster.Einfach -> drawRect(farben.first())
                TypVisualMuster.Gestreift -> {
                    val streifenBreite = (size.minDimension / farben.size.coerceAtLeast(2)).coerceAtLeast(2f)
                    rotate(-45f) {
                        var x = -size.width * 1.5f
                        var index = 0
                        while (x < size.width * 2.5f) {
                            drawRect(
                                color = farben[index % farben.size],
                                topLeft = Offset(x, -size.height),
                                size = Size(streifenBreite, size.height * 3f),
                            )
                            x += streifenBreite
                            index += 1
                        }
                    }
                }
                TypVisualMuster.Zusammengesetzt -> {
                    val breite = size.width / farben.size.coerceAtLeast(1)
                    farben.forEachIndexed { index, farbe ->
                        drawRect(
                            color = farbe,
                            topLeft = Offset(index * breite, 0f),
                            size = Size(breite + 1f, size.height),
                        )
                    }
                }
            }
        }
    }
}

/** Semantische Standarddarstellung eines Anschlusses ohne Domänenwissen. */
fun AnschlussDaten.typVisualDescriptor(): TypVisualDescriptor =
    vertrag.typ.zuVisualDescriptor()

/** Hilfsfunktion für Renderpfade, die nur einen Typausdruck besitzen. */
fun TypAusdruck.standardTypVisualDescriptor(): TypVisualDescriptor = zuVisualDescriptor()
