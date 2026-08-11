package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.TeutonStudio.KnotenKartenVerwalter.daten.TypAusdruck
import de.TeutonStudio.KnotenKartenVerwalter.daten.TypId

/**
 * UI-neutrale Beschreibung einer kompakten Typgrafik. Domänen wie Mathematik
 * und später Godot bestimmen die fachlichen Kurzzeichen; der Grapheditor rendert
 * nur diesen Descriptor.
 */
data class TypVisualDescriptor(
    val kurzText: String,
    val tooltipText: String = kurzText,
    val alternativen: List<TypVisualDescriptor> = emptyList(),
) {
    init { require(kurzText.isNotBlank()) { "Eine Typdarstellung benötigt einen Kurztext." } }
}

fun interface TypVisualAuflöser {
    fun beschreibe(typ: TypAusdruck): TypVisualDescriptor
}

/** Konservativer Fallback für unbekannte Domänen. */
object StandardTypVisualAuflöser : TypVisualAuflöser {
    override fun beschreibe(typ: TypAusdruck): TypVisualDescriptor = when (typ) {
        TypAusdruck.Beliebig -> TypVisualDescriptor("*", "Beliebiger Typ")
        TypAusdruck.Unbekannt -> TypVisualDescriptor("?", "Unbekannter Typ")
        is TypAusdruck.Atom -> TypVisualDescriptor(kurzName(typ.id), typ.id.wert)
        is TypAusdruck.Variable -> TypVisualDescriptor("${'$'}${typ.id.wert}", "Typvariable ${typ.id.wert}")
        is TypAusdruck.Parameterisiert -> {
            val argumente = typ.argumente.map(::beschreibe)
            val text = "${kurzName(typ.konstruktor)}<${argumente.joinToString(",") { it.kurzText }}>"
            TypVisualDescriptor(text, text)
        }
        is TypAusdruck.Vereinigung -> {
            val alternativen = typ.alternativen.map(::beschreibe)
            TypVisualDescriptor(
                kurzText = alternativen.joinToString("∨") { it.kurzText },
                tooltipText = alternativen.joinToString(" oder ") { it.tooltipText },
                alternativen = alternativen,
            )
        }
    }

    private fun kurzName(id: TypId): String = id.wert.substringAfterLast('.').take(10).ifBlank { id.wert.take(10) }
}

/**
 * Kleine Orchestrator-artige Typmarke neben einem Handle. Bei Oder-Typen werden
 * die Alternativen getrennt dargestellt, anstatt die Vereinigung auf einen
 * unlesbaren langen String zu reduzieren.
 */
@Composable
fun TypMiniGrafik(
    descriptor: TypVisualDescriptor,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
) {
    val sichererZoom = zoom.coerceAtLeast(.0001f)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke((1f / sichererZoom).dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp,
    ) {
        if (descriptor.alternativen.size > 1) {
            Row(
                modifier = Modifier.padding(horizontal = (3f / sichererZoom).dp, vertical = (1f / sichererZoom).dp),
                horizontalArrangement = Arrangement.spacedBy((2f / sichererZoom).dp),
            ) {
                descriptor.alternativen.forEachIndexed { index, alternative ->
                    if (index > 0) {
                        Text(
                            text = "∨",
                            fontSize = (8f / sichererZoom).sp,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = alternative.kurzText,
                        fontSize = (8f / sichererZoom).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        } else {
            Text(
                text = descriptor.kurzText,
                modifier = Modifier.padding(horizontal = (4f / sichererZoom).dp, vertical = (1f / sichererZoom).dp),
                fontSize = (8f / sichererZoom).sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
