package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.math.roundToInt

internal object GruppenKnotenRenderer : KnotenRenderer {
    @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(knoten.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(if (knoten.art == GRUPPEN_BLOCK_ART) "Gruppe · Doppelklick zum Öffnen" else "Schnittstelle der Hauptkarte",
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun BoxScope.GruppenRahmen(editor: KartenEditorZustand, dauerFür: (KnotenId) -> Long?) {
    val karte = editor.darstellungsKarte
    val ansicht = karte.ansicht
    val dichte = LocalDensity.current.density
    val farbe = MaterialTheme.colorScheme.tertiary
    val textMesser = rememberTextMeasurer()
    val größe = remember { mutableStateOf(IntSize.Zero) }
    val index = remember(karte.visuelleGruppen) { GruppenIndex(editor.karte) }
    Box(Modifier.fillMaxSize().onSizeChanged { größe.value = it }) {
        karte.visuelleGruppen.sortedBy { index.vorfahren(it.id).size }.forEach { g ->
            val dauer = index.knotenIn(g.id).mapNotNull(dauerFür).fold<Long,Long?>(null) { summe, wert ->
                val a = summe ?: 0L; val b = wert.coerceAtLeast(0L)
                if (Long.MAX_VALUE-a < b) Long.MAX_VALUE else a+b
            }
            val titelMaß = textMesser.measure(AnnotatedString(g.titel), style = MaterialTheme.typography.bodyLarge).size
            val zeitBreite = dauer?.let { textMesser.measure(AnnotatedString("Δzeit ${formatiereAuswertungsDauerNanos(it)}"), style = MaterialTheme.typography.labelSmall).size.width / dichte } ?: 0f
            SideEffect { editor.meldeGruppenKopfminimum(g.id, GraphGröße(titelMaß.width / dichte + zeitBreite + 80f,
                maxOf(VISUELLE_GRUPPE_KOPFZEILE_HÖHE, titelMaß.height / dichte + 16f))) }
            val faktor = dichte * ansicht.zoom
            val x = ansicht.verschiebung.x + g.position.x * faktor
            val y = ansicht.verschiebung.y + g.position.y * faktor
            val breite = g.größe.breite * faktor
            val höhe = g.größe.höhe * faktor
            if (x + breite < 0 || y + höhe < 0 || x > größe.value.width || y > größe.value.height) return@forEach
            key(g.id) {
                Box(Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .graphicsLayer { scaleX = ansicht.zoom; scaleY = ansicht.zoom; transformOrigin = TransformOrigin(0f, 0f) }
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(g.größe.breite.dp, g.größe.höhe.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(farbe.copy(alpha = .07f), cornerRadius = CornerRadius(14.dp.toPx()))
                        drawRoundRect(if (editor.ausgewählteGruppe == g.id) farbe else farbe.copy(alpha = .8f), cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke((if (editor.ausgewählteGruppe == g.id) 3.dp else 2.dp).toPx()))
                    }
                    Surface(color = farbe.copy(alpha = .18f), modifier = Modifier.fillMaxWidth()
                        .height(editor.gruppenKopfHöhe(g.id).dp)
                        .semantics { contentDescription = "Gruppe ${g.titel} auswählen" }
                        .pointerInput(g.id) { detectTapGestures(
                            onTap = { editor.wähleGruppe(g.id) },
                            onDoubleTap = { editor.öffneGruppe(g.id) },
                        ) }
                        .gruppenDrag(g.id, editor) { delta -> editor.führeAus(KartenAktion.VisuelleGruppeVerschieben(g.id, GraphPunkt(delta.x / dichte, delta.y / dichte)), false) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(g.titel, Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
                            dauer?.let { Text("Δzeit ${formatiereAuswertungsDauerNanos(it)}",style=MaterialTheme.typography.labelSmall) }
                            TextButton(onClick = { editor.wähleGruppe(g.id, true) }) { Text("⋯") }
                        }
                    }
                    Surface(color = farbe, modifier = Modifier.align(Alignment.BottomEnd).size(22.dp)
                        .semantics { contentDescription = "Gruppe ${g.titel} skalieren" }
                        .gruppenDrag(g.id, editor) { delta ->
                            val aktuell = editor.karte.visuelleGruppen.firstOrNull { it.id == g.id } ?: return@gruppenDrag
                            editor.führeAus(KartenAktion.VisuelleGruppeGrößeÄndern(g.id,
                                GraphGröße(aktuell.größe.breite + delta.x / dichte, aktuell.größe.höhe + delta.y / dichte)), false)
                        }) { Text("⌟") }
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.GruppenNavigation(editor: KartenEditorZustand) {
    val id = editor.geöffneteGruppe
    val index = remember(editor.karte.visuelleGruppen) { GruppenIndex(editor.karte) }
    var baumOffen by remember { mutableStateOf(false) }
    var zugeklappt by remember(editor.karte.id) { mutableStateOf<Set<VisuelleGruppenId>>(emptySet()) }
    Surface(modifier = Modifier.align(Alignment.TopStart).zIndex(3f), tonalElevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (id != null) {
                TextButton(onClick = { editor.öffneGruppe(null) }) { Text(editor.karte.name) }
                (index.vorfahren(id).reversed() + id).forEach { gid ->
                    Text("›")
                    TextButton(onClick = { editor.öffneGruppe(gid) }) { Text(index.gruppen[gid]?.titel.orEmpty()) }
                }
            }
            if (index.gruppen.isNotEmpty()) Box {
                TextButton(onClick = { baumOffen = true }) { Text("Gruppen ▾") }
                DropdownMenu(expanded = baumOffen, onDismissRequest = { baumOffen = false }) {
                    val einträge = mutableListOf<Pair<VisuelleKnotenGruppeDaten,Int>>()
                    fun ebene(eltern: VisuelleGruppenId?, tiefe: Int) {
                        index.kinder[eltern].orEmpty().sortedBy { it.titel }.forEach { g ->
                            einträge += g to tiefe
                            if (g.id !in zugeklappt) ebene(g.id,tiefe+1)
                        }
                    }
                    ebene(null,0)
                    einträge.forEach { (g,tiefe) ->
                        Row(Modifier.padding(start=(tiefe*12).dp), verticalAlignment=Alignment.CenterVertically) {
                            TextButton(onClick = { zugeklappt = if (g.id in zugeklappt) zugeklappt-g.id else zugeklappt+g.id }) {
                                Text(if (g.id in zugeklappt) "▸" else "▾")
                            }
                            TextButton(onClick = { editor.öffneGruppe(g.id); baumOffen=false }) { Text(g.titel) }
                            TextButton(onClick = { editor.wähleGruppe(g.id,true); baumOffen=false }) { Text("⋯") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GruppenInspektor(editor: KartenEditorZustand) {
    if (!editor.gruppenInspektorOffen) return
    val g = editor.karte.visuelleGruppen.firstOrNull { it.id == editor.ausgewählteGruppe } ?: return
    var titel by remember(g.id, g.titel) { mutableStateOf(g.titel) }
    val index = remember(editor.karte.visuelleGruppen) { GruppenIndex(editor.karte) }
    AtlasBestätigungsDialog(inhaltScrolltSelbst = true, onDismissRequest = { editor.gruppenInspektorOffen = false }, title = { Text("Gruppeninspektor") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.felder)) {
            OutlinedTextField(titel, { titel = it }, label = { Text("Titel") }, singleLine = true)
            Text("${index.knotenIn(g.id).size} Knoten · ${index.nachfahren(g.id).size - 1} Untergruppen")
            Button(onClick = {
                editor.brecheVerbindungsVorschauAb()
                editor.führeAus(KartenAktion.VisuelleGruppeFaltungÄndern(g.id, !g.eingeklappt))
            }, modifier = Modifier.fillMaxWidth()) { Text(if (g.eingeklappt) "Ausklappen" else "Einklappen") }
            OutlinedButton(onClick = { editor.öffneGruppe(g.id) }, modifier = Modifier.fillMaxWidth()) { Text("Gruppe öffnen") }
            OutlinedButton(onClick = {
                editor.führeAus(KartenAktion.VisuelleGruppeLöschen(g.id))
                editor.wähleGruppe(null)
            }, modifier = Modifier.fillMaxWidth()) { Text("Gruppe auflösen") }
            Text("Beim Auflösen bleiben Knoten, Untergruppen und Verbindungen erhalten.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(enabled = titel.isNotBlank(), onClick = {
            editor.führeAus(KartenAktion.VisuelleGruppeTitelÄndern(g.id, titel.trim()))
            editor.gruppenInspektorOffen = false
        }) { Text("Übernehmen") }
    }, dismissButton = { TextButton(onClick = { editor.gruppenInspektorOffen = false }) { Text("Schließen") } })
}

/** Eine Zweifingergeste verwirft den Gruppen-Drag und bleibt der Kamera überlassen. */
private fun Modifier.gruppenDrag(id: VisuelleGruppenId, editor: KartenEditorZustand, beiDelta: (Offset) -> Unit): Modifier =
    pointerInput(id, editor.darstellungsKarte.ansicht.zoom) {
        awaitEachGesture {
            val erster = awaitFirstDown(requireUnconsumed = false)
            var aktiv = false
            var bewegung = Offset.Zero
            var mehrfinger = false
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } > 1) {
                    if (aktiv) editor.verwerfeLaufendeInteraktion()
                    mehrfinger = true
                }
                val change = event.changes.firstOrNull { it.id == erster.id } ?: break
                if (!mehrfinger && change.pressed && change.position != change.previousPosition) {
                    bewegung += change.position - change.previousPosition
                    if (!aktiv && bewegung.getDistance() <= viewConfiguration.touchSlop) continue
                    val delta = if (aktiv) change.position - change.previousPosition else bewegung
                    if (!aktiv) { editor.wähleGruppe(id); editor.beginneInteraktion(); aktiv = true }
                    beiDelta(delta)
                    change.consume()
                }
            } while (event.changes.any { it.pressed })
            if (aktiv && !mehrfinger) editor.beendeInteraktion()
        }
    }
