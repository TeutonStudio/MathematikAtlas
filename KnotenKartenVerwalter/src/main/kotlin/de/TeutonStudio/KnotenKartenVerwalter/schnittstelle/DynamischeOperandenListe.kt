package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.math.roundToInt

/** Gemeinsamer Android-/Desktop-Inspectorbaustein ohne mathematische Parameterkonventionen. */
@Composable
fun DynamischeOperandenListe(
    karte: KartenDaten,
    knoten: KnotenDaten,
    zusätzlichBelegteEingänge: Set<AnschlussId> = emptySet(),
    übernehmen: (KartenAktion.KnotenAnschlüsseÄndern) -> Unit,
) {
    val gruppen = dynamischeOperandengruppen(karte, knoten, zusätzlichBelegteEingänge)
    if (gruppen.isEmpty()) return
    val zeilenHoehe = with(LocalDensity.current) { 64.dp.toPx() }
    Column(Modifier.fillMaxWidth()) {
        Text("Operandenreihenfolge", style = MaterialTheme.typography.titleSmall)
        Text("Griff gedrückt halten und ziehen oder Pfeile verwenden.", style = MaterialTheme.typography.bodySmall)
        gruppen.forEach { gruppe ->
            val ids = gruppe.map { it.id }
            fun verschiebe(von: Int, nach: Int) {
                if (von == nach) return
                val neu = ids.toMutableList().apply { add(nach, removeAt(von)) }
                ordneDynamischeOperanden(karte, knoten.id, neu, zusätzlichBelegteEingänge)?.let(übernehmen)
            }
            gruppe.forEachIndexed { index, port ->
                key(port.id) {
                    var delta by remember(ids) { mutableStateOf(0f) }
                    var zieht by remember(ids) { mutableStateOf(false) }
                    var griffFokussiert by remember { mutableStateOf(false) }
                    val griffFokus = remember { FocusRequester() }
                    val ziel = (index + (delta / zeilenHoehe).roundToInt()).coerceIn(0, ids.lastIndex)
                    val quelle = karte.verbindungen.singleOrNull { it.zu == AnschlussVerweis(knoten.id, port.id) }?.von
                    val quellenKnoten = karte.knoten.firstOrNull { it.id == quelle?.knotenId }
                    val quellenPort = quellenKnoten?.anschlüsse?.firstOrNull { it.id == quelle?.anschlussId }
                    Surface(color = if (zieht || griffFokussiert) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).semantics {
                                contentDescription = "Operand ${index + 1}: ${port.name}. Mit Pfeiltasten nach oben oder unten verschieben."
                            }.focusRequester(griffFokus).onFocusChanged { griffFokussiert = it.isFocused }
                                .onPreviewKeyEvent { ereignis ->
                                    val nach = when (ereignis.key) {
                                        Key.DirectionUp -> index - 1
                                        Key.DirectionDown -> index + 1
                                        else -> return@onPreviewKeyEvent false
                                    }
                                    if (ereignis.type == KeyEventType.KeyUp && nach in ids.indices) verschiebe(index, nach)
                                    true
                                }.focusable()
                                .pointerInput(karte, ids) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { delta = 0f; zieht = true; griffFokus.requestFocus() },
                                        onDragCancel = { delta = 0f; zieht = false },
                                        onDragEnd = {
                                            val nach = (index + (delta / zeilenHoehe).roundToInt()).coerceIn(0, ids.lastIndex)
                                            delta = 0f; zieht = false; verschiebe(index, nach)
                                        },
                                        onDrag = { change, amount -> change.consume(); delta += amount.y },
                                    )
                                }, contentAlignment = Alignment.Center) { Text("≡") }
                            Column(Modifier.weight(1f)) {
                                Text("${index + 1}. ${port.name}" + if (zieht) " → ${ziel + 1}" else "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(quellenKnoten?.let { "${it.name} · ${quellenPort?.name.orEmpty()}" } ?: "Nicht verbunden",
                                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { verschiebe(index, index - 1) }, enabled = index > 0,
                                modifier = Modifier.width(48.dp).semantics { contentDescription = "${port.name} nach oben" }) { Text("↑") }
                            TextButton(onClick = { verschiebe(index, index + 1) }, enabled = index < ids.lastIndex,
                                modifier = Modifier.width(48.dp).semantics { contentDescription = "${port.name} nach unten" }) { Text("↓") }
                        }
                    }
                }
            }
        }
    }
}
