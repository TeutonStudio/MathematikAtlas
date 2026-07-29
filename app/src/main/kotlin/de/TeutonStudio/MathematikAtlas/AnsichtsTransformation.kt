package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt

private const val MINIMALER_ZOOM = 0.25f
private const val MAXIMALER_ZOOM = 3.5f

/**
 * Wendet die kombinierte Pan-/Zoom-Geste des Verschieben-Werkzeugs auf die Kartenansicht an.
 * Der Gestenmittelpunkt bleibt beim Skalieren unter demselben Bildschirmpunkt.
 */
internal fun AnsichtsFenster.transformiereAnsicht(
    zentrum: Offset,
    pan: Offset,
    zoomFaktor: Float,
): AnsichtsFenster {
    val gültigerFaktor = zoomFaktor.takeIf { it.isFinite() && it > 0f } ?: 1f
    val alterZoom = zoom.coerceAtLeast(0.0001f)
    val neuerZoom = (alterZoom * gültigerFaktor).coerceIn(MINIMALER_ZOOM, MAXIMALER_ZOOM)
    val effektiverFaktor = neuerZoom / alterZoom

    return copy(
        verschiebung = GraphPunkt(
            zentrum.x + pan.x - (zentrum.x - verschiebung.x) * effektiverFaktor,
            zentrum.y + pan.y - (zentrum.y - verschiebung.y) * effektiverFaktor,
        ),
        zoom = neuerZoom,
    )
}
