package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße

/** Misst Text/Bedienung frei und den interaktiven Viewport genau einmal mit endlichen Maßen. */
@Composable
fun KnotenViewportInhalt(
    aktionen: KnotenRendererAktionen,
    kopf: @Composable () -> Unit,
    fuß: @Composable () -> Unit = {},
    steuerung: @Composable () -> Unit = {},
    ansicht: @Composable (Modifier) -> Unit,
) {
    Layout(modifier = Modifier.fillMaxSize().padding(8.dp), content = {
        Box { kopf() }
        Box { fuß() }
        Box { steuerung() }
        Box(Modifier.fillMaxSize()) { ansicht(Modifier.fillMaxSize()) }
    }) { messbare, constraints ->
        val kopfMaß = messbare[0].measure(Constraints())
        val fußMaß = messbare[1].measure(Constraints())
        val steuerMaß = messbare[2].measure(Constraints())
        val abstand = 6.dp.roundToPx()
        val steuerAbstand = if (steuerMaß.width > 0) abstand else 0
        val textHöhe = kopfMaß.height + fußMaß.height + abstand * 2
        val viewportMinimum = maxOf(240.dp.roundToPx(), steuerMaß.height)
        val minimumBreite = maxOf(kopfMaß.width, fußMaß.width, 240.dp.roundToPx() + steuerMaß.width + steuerAbstand)
        aktionen.inhaltsminimumMelden(GraphGröße((minimumBreite + 16.dp.roundToPx()) / density,
            (textHöhe + viewportMinimum + 16.dp.roundToPx()) / density))
        val viewportHöhe = maxOf(viewportMinimum, constraints.maxHeight - textHöhe)
        val viewportBreite = (constraints.maxWidth - steuerMaß.width - steuerAbstand).coerceAtLeast(1)
        val ansichtMaß = messbare[3].measure(Constraints.fixed(viewportBreite, viewportHöhe))
        layout(constraints.maxWidth, constraints.maxHeight) {
            kopfMaß.placeRelative(0, 0)
            val oben = kopfMaß.height + abstand
            ansichtMaß.placeRelative(0, oben)
            steuerMaß.placeRelative(viewportBreite + steuerAbstand, oben)
            fußMaß.placeRelative(0, oben + viewportHöhe + abstand)
        }
    }
}
