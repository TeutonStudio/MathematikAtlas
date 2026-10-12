package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand

/** Interaktive Ebene für persistierte visuelle Gruppen und Auswahlmarkierungen. */
@Composable
internal fun VisuelleGruppenEbene(
    editor: KartenEditorZustand,
    auswertungsDauerFür: (KnotenId) -> Long?,
) {
    Box(Modifier.fillMaxSize()) { AuswahlMarkierungen(editor) }
}

internal fun kollektiveAuswertungsDauerNanos(
    knotenIds: Set<KnotenId>,
    dauerFür: (KnotenId) -> Long?,
): Long? {
    var summe = 0L
    var dauerVorhanden = false
    knotenIds.forEach { id ->
        dauerFür(id)?.let { dauer ->
            dauerVorhanden = true
            val nichtNegativ = dauer.coerceAtLeast(0L)
            summe = if (Long.MAX_VALUE - summe < nichtNegativ) Long.MAX_VALUE else summe + nichtNegativ
        }
    }
    return summe.takeIf { dauerVorhanden }
}

@Composable
private fun AuswahlMarkierungen(editor: KartenEditorZustand) {
    val auswahlFarbe = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        val karte = editor.darstellungsKarte
        val zoom = karte.ansicht.zoom
        editor.ausgewählteKnoten.forEach { id ->
            val knoten = karte.knoten.firstOrNull { it.id == id } ?: return@forEach
            val links = knoten.position.x - 4f
            val oben = knoten.position.y - 4f
            val rechts = knoten.position.x + knoten.größe.breite + 4f
            val unten = knoten.position.y + knoten.größe.höhe + 4f
            val start = Offset(
                karte.ansicht.verschiebung.x + links * density * zoom,
                karte.ansicht.verschiebung.y + oben * density * zoom,
            )
            drawRoundRect(
                color = auswahlFarbe,
                topLeft = start,
                size = Size((rechts - links) * density * zoom, (unten - oben) * density * zoom),
                cornerRadius = CornerRadius(10.dp.toPx() * zoom),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
