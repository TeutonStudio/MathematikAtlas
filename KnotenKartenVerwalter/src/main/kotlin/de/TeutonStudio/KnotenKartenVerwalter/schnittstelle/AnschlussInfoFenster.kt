package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.findeAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal data class AnschlussInfoZiel(val ref: AnschlussVerweis, val beitrag: VerbindungsId?)
internal data class AnschlussHover(val ziel: AnschlussInfoZiel, val anker: Offset)

@Stable
internal class AnschlussInfoInteraktion {
    var hover by mutableStateOf<AnschlussHover?>(null)
    var dialog by mutableStateOf<AnschlussInfoZiel?>(null)
    fun schließenHover() { hover = null }
    fun öffnen(ziel: AnschlussInfoZiel) { hover = null; dialog = ziel }
}

internal fun KartenEditorZustand.infoZiel(ref: AnschlussVerweis) =
    AnschlussInfoZiel(projektion.original(ref), darstellungsKarte.findeAnschluss(ref)?.beitragsVerbindung)

@Composable
internal fun AnschlussInfoFenster(
    zustand: KartenEditorZustand,
    interaktion: AnschlussInfoInteraktion,
    gesperrt: Boolean,
    provider: (KartenDaten, AnschlussVerweis) -> AnschlussInfo?,
) {
    val hover = interaktion.hover
    var sichtbar by remember(hover) { mutableStateOf(false) }
    val hoverErlaubt = !gesperrt && zustand.verbindungsStart == null && interaktion.dialog == null
    LaunchedEffect(hover, hoverErlaubt, zustand.darstellungsKarte.ansicht) {
        sichtbar = false
        if (hover != null && hoverErlaubt) { delay(500); sichtbar = true }
    }
    LaunchedEffect(gesperrt, zustand.verbindungsStart, zustand.darstellungsKarte.ansicht) {
        interaktion.schließenHover()
        if (gesperrt) interaktion.dialog = null
    }
    fun info(ziel: AnschlussInfoZiel): AnschlussInfo? {
        if (zustand.karte.findeAnschluss(ziel.ref) == null) return null
        if (ziel.beitrag != null && zustand.karte.verbindungen.none { it.id == ziel.beitrag }) return null
        return provider(zustand.karte, ziel.ref)
    }
    val hoverDaten = hover?.let { info(it.ziel) }
    LaunchedEffect(hover, hoverDaten == null) {
        if (hover != null && hoverDaten == null) interaktion.schließenHover()
    }
    if (sichtbar && hoverErlaubt && hover != null && hoverDaten != null) hoverDaten.let { daten ->
        val dichte = androidx.compose.ui.platform.LocalDensity.current
        val abstand = with(dichte) { 12.dp.roundToPx() }
        val position = remember(hover.anker, abstand) {
            object : PopupPositionProvider {
                override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
                    layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                    val x = anchorBounds.left + hover.anker.x.roundToInt() + abstand
                    val unten = anchorBounds.top + hover.anker.y.roundToInt() + abstand
                    val y = if (unten + popupContentSize.height <= windowSize.height) unten
                        else anchorBounds.top + hover.anker.y.roundToInt() - popupContentSize.height - abstand
                    return IntOffset(x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                        y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)))
                }
            }
        }
        Popup(popupPositionProvider = position, properties = PopupProperties(focusable = false)) {
            Surface(Modifier.widthIn(max = 320.dp), shape = MaterialTheme.shapes.small,
                tonalElevation = 6.dp, shadowElevation = 4.dp) {
                Column(Modifier.padding(AtlasDialogAbstände.beschreibung)) {
                    Text(daten.name, style = MaterialTheme.typography.titleSmall)
                    Text("${daten.richtung} · ${daten.arten.joinToString(" oder ") { it.name }}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    val ziel = interaktion.dialog
    val daten = ziel?.let(::info)
    LaunchedEffect(ziel, daten == null) { if (ziel != null && daten == null) interaktion.dialog = null }
    if (!gesperrt && ziel != null && daten != null) AnschlussInfoDialog(daten, ziel.beitrag) { interaktion.dialog = null }
}

@Composable
internal fun AnschlussInfoDialog(info: AnschlussInfo, beitrag: VerbindungsId?, schließen: () -> Unit) {
    AtlasBestätigungsDialog(
        onDismissRequest = schließen,
        title = { Text("Anschluss: ${info.name}") },
        untertitel = { Text(info.knotenName) },
        confirmButton = { TextButton(onClick = schließen) { Text("Schließen") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.abschnitt)) {
                AtlasDialogAbschnitt("Vertrag") {
                    Text("Richtung: ${info.richtung}")
                    Text("Belegung: ${if (info.belegung == AnschlussBelegung.Mehrfach) "Mehrfach" else "Einzeln"}")
                    Text("Semantischer Typ: ${info.typ}")
                    info.eingabeHinweis?.let { Text(it) }
                    info.anforderungen.forEach { Text(it) }
                }
                AtlasDialogAbschnitt("Zulässige Typen") {
                    info.arten.forEach { art -> AtlasDialogEintrag {
                        Text(art.name, style = MaterialTheme.typography.labelLarge)
                        Text(art.beschreibung.ifBlank { "Keine Beschreibung vorhanden." })
                    } }
                }
                AtlasDialogAbschnitt("Verbindungen") {
                    if (info.verbindungen.isEmpty()) Text("Keine Verbindungen vorhanden.")
                    info.verbindungen.forEach { v ->
                        val ausgewählt = v.id == beitrag
                        Surface(shape = MaterialTheme.shapes.small,
                            border = if (ausgewählt) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
                            AtlasDialogEintrag {
                                if (ausgewählt) Text("Getroffener Beitrag", style = MaterialTheme.typography.labelLarge)
                                Text(v.beschreibung)
                            }
                        }
                    }
                }
            }
        },
    )
}
