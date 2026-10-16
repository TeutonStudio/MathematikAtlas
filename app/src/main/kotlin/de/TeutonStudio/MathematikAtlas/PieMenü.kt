package de.TeutonStudio.MathematikAtlas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenVorschau
import kotlin.math.*

internal data class PieLayout(val plätze: List<PiePlatz>, val innen: Float, val außen: Float, val kopf: Float)

@Composable
internal fun erinnerePieLayout(atlas: AtlasZustand): PieLayout {
    val dichte = LocalDensity.current
    val pie = atlas.pie
    val zoom = atlas.editor.darstellungsKarte.ansicht.zoom
    // Mindestgrößen auch außerhalb des sichtbaren Ausschnitts bestimmen. Nur die
    // eigentlichen Vorschauinhalte werden anschließend per Viewport komponiert.
    val knoten = remember(pie.einträge) { pie.einträge.associate { it.id to it.vorlage.erzeuge(GraphPunkt.Zero) } }
    val größen = pie.einträge.associate { e ->
        val k = knoten.getValue(e.id)
        val min = atlas.rendererFür(k).Mindestgröße(k)
        val gemessen = pie.größen[e.id] ?: GraphGröße(0f, 0f)
        e.id to GraphGröße(maxOf(k.größe.breite, min.breite, gemessen.breite), maxOf(k.größe.höhe, min.höhe, gemessen.höhe))
    }
    val richtung = if (pie.kategorie?.id == "eigene-karten") 90f else -90f
    val basis = remember(pie.einträge, größen, zoom, dichte.density, richtung, pie.anschlussbezogen) {
        if (pie.anschlussbezogen) planePieKreise(pie.einträge, größen, zoom * dichte.density, 56f * dichte.density)
        else planePieFächer(pie.einträge, größen, zoom * dichte.density, richtung, 56f * dichte.density)
    }
    val plätze = remember(basis, pie.zoom) { skalierePiePlätze(basis, pie.zoom) }
    val schrift = dichte.fontScale.coerceAtLeast(1f)
    return PieLayout(plätze, 64f * dichte.density * schrift, 176f * dichte.density * schrift, maxOf(64f * dichte.density, pie.bedienHöhe))
}

/** Dieser Empfänger bleibt auch nach dem Entfernen aller Menükinder komponiert. */
@Composable
internal fun Modifier.pieEingaben(atlas: AtlasZustand, layout: PieLayout, erlaubt: Boolean): Modifier {
    val aktuellLayout by rememberUpdatedState(layout)
    val aktuellErlaubt by rememberUpdatedState(erlaubt)
    val dichte by rememberUpdatedState(LocalDensity.current.density)
    return pointerInput(atlas) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val pie = atlas.pie
            // Der allgemeine Seitentastenaufruf darf keinen Anschlussdrag übernehmen.
            val ansicht = atlas.editor.darstellungsKarte.ansicht
            val aufAnschluss = atlas.editor.darstellungsKarte.knoten.any { k -> k.anschlüsse.any { a ->
                val welt = k.position + k.anschlussPositionRelativ(a)
                val delta = down.position - Offset(ansicht.verschiebung.x + welt.x * ansicht.zoom * dichte,
                    ansicht.verschiebung.y + welt.y * ansicht.zoom * dichte)
                abs(delta.x) <= 14f * dichte && abs(delta.y) <= 14f * dichte
            } }
            val aufruf = !pie.aktiv && !aufAnschluss && aktuellErlaubt && down.type == PointerType.Stylus &&
                (currentEvent.buttons.isPrimaryPressed || currentEvent.buttons.isSecondaryPressed)
            if (!aufruf && !pie.aktiv) return@awaitEachGesture
            if (!aufruf && (pie.ebene in setOf(PieEbene.Definition, PieEbene.Anschluss, PieEbene.Weg) || down.position.y < aktuellLayout.kopf)) return@awaitEachGesture
            down.consume()
            val start = down.position
            val ebene = pie.ebene
            val platz = if (ebene == PieEbene.Fächer) aktuellLayout.plätze.firstOrNull {
                it.rahmen.contains(start - pie.position)
            } else null
            val art = when (down.type) {
                PointerType.Stylus -> KonzeptZeigerArt.Stift
                PointerType.Touch -> KonzeptZeigerArt.Touch
                PointerType.Mouse -> KonzeptZeigerArt.Maus
                else -> KonzeptZeigerArt.Unbekannt
            }
            val schwelle = KonzeptGestenSchwellen.für(art, viewConfiguration.touchSlop / dichte).bewegungDp * dichte
            var bewegt = false
            var letzte = start
            var eigenerDrag = false
            var transformation = false
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (event.changes.count { it.pressed } > 1) {
                        if (eigenerDrag) { pie.schließen(); break }
                        transformation = true; bewegt = true
                    }
                    if (transformation) {
                        // Nach dem ersten Pinch bis zum Loslassen im Menü bleiben:
                        // kein versehentlicher Knotendrag oder Definitionstap.
                        val pan = event.calculatePan()
                        val faktor = event.calculateZoom()
                        if (!aufruf && (pan != Offset.Zero || faktor != 1f))
                            pie.transformieren(event.calculateCentroid(useCurrent = false), pan, faktor)
                        event.changes.forEach { it.consume() }
                        if (event.changes.none { it.pressed }) break
                        continue
                    }
                    if (change == null) {
                        if (eigenerDrag) pie.schließen()
                        break
                    }
                    change.consume()
                    val überSchwelle = (change.position - start).getDistance() > schwelle
                    if (!bewegt && überSchwelle) {
                        bewegt = true
                        if (!aufruf && platz != null) {
                            val relativ = start - (pie.position + platz.rahmen.topLeft)
                            pie.beginneZiehen(platz.eintrag, change.position, relativ, dichte)
                            eigenerDrag = pie.zieht
                        } else if (!aufruf) pie.verschieben(change.position - start)
                    } else if (bewegt && !aufruf) {
                        if (eigenerDrag) pie.ziehe(change.position, dichte)
                        else pie.verschieben(change.position - letzte)
                    }
                    letzte = change.position
                    if (!change.pressed) {
                        when {
                            aufruf && !bewegt -> pie.öffnen(start)
                            eigenerDrag -> {
                                if (Rect(Offset.Zero, Size(size.width.toFloat(), size.height.toFloat())).contains(change.position)) pie.ablegen()
                                else pie.schließen()
                            }
                            !bewegt && platz != null -> pie.definitionÖffnen(platz.eintrag)
                            !bewegt && ebene == PieEbene.Kategorien -> {
                                val index = pieKategorieAmPunkt(start, pie.position, KonzeptBibliothekRegister.kategorien.size,
                                    aktuellLayout.innen, aktuellLayout.außen)
                                if (index != null) pie.wählen(index)
                                else pie.schließen()
                            }
                            !bewegt -> pie.schließen()
                        }
                        break
                    }
                }
            } finally {
                if (eigenerDrag && pie.zieht) pie.schließen()
            }
        }
    }.pointerInput(atlas) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pie = atlas.pie
                val change = event.changes.firstOrNull() ?: continue
                if (event.type == PointerEventType.Scroll && pie.ebene == PieEbene.Fächer && change.position.y >= aktuellLayout.kopf) {
                    // Ein gedrückter Vorschauzeiger besitzt bereits einen festen Griff.
                    if (event.changes.none { it.pressed })
                        pie.transformieren(change.position, Offset.Zero, exp(-change.scrollDelta.y * .12f))
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}

@Composable
internal fun PieMenü(atlas: AtlasZustand, layout: PieLayout) {
    val pie = atlas.pie
    BackHandler(pie.aktiv) { pie.zurück() }
    LaunchedEffect(pie.verbindungsKontext) { if (pie.anschlussbezogen) pie.ladeVerbindungsEinträge() }
    LaunchedEffect(atlas.editor.karte, pie.ebene, pie.verbindungsKontext) {
        val kontext = pie.verbindungsKontext
        if (kontext != null && pie.ebene in setOf(PieEbene.Fächer, PieEbene.Definition) && atlas.editor.karte != kontext.basis)
            pie.schließen()
    }
    PieVerbindungsDialoge(atlas)
    if (!pie.sichtbar) return
    val dichte = LocalDensity.current.density
    val zoom = atlas.editor.darstellungsKarte.ansicht.zoom * pie.zoom
    BoxWithConstraints(Modifier.fillMaxSize().zIndex(110f).clipToBounds()) {
        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = .76f)))
        val viewport = Rect(0f, layout.kopf, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        if (pie.ebene == PieEbene.Kategorien) {
            PieRing(atlas, layout)
        } else {
            if (pie.lädt || pie.filterFehler != null || pie.anschlussbezogen && pie.einträge.isEmpty()) {
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (pie.lädt) CircularProgressIndicator()
                    Text(if (pie.lädt) "Passende Knoten werden gesucht …" else pie.filterFehler ?: "Keine passenden Knoten gefunden")
                }
            }
            val erste = layout.plätze.groupBy { it.eintrag.gruppe }.mapValues { it.value.first().eintrag.id }
            layout.plätze.forEach { p ->
                val rect = p.rahmen.translate(pie.position)
                if (rect.inflate(64f * dichte).overlaps(viewport)) key(p.eintrag.id) {
                    val knoten = remember(p.eintrag.id) { p.eintrag.vorlage.erzeuge(GraphPunkt.Zero) }
                    if (erste[p.eintrag.gruppe] == p.eintrag.id || p.eintrag.mitÜbersetzer) Surface(
                        Modifier.offset { IntOffset(rect.left.roundToInt(), (rect.top - 44f * dichte * pie.zoom).roundToInt()) }
                            .graphicsLayer { scaleX = pie.zoom; scaleY = pie.zoom; transformOrigin = TransformOrigin(0f, 0f) }
                            .widthIn(max = 300.dp), shape = MaterialTheme.shapes.small,
                    ) { Text(if (p.eintrag.mitÜbersetzer) "${p.eintrag.gruppe} · Mit Übersetzer" else p.eintrag.gruppe, Modifier.padding(8.dp), style = MaterialTheme.typography.labelLarge) }
                    KnotenVorschau(knoten, atlas.rendererFür(knoten), zoom,
                        farbeFürAnschluss = { anschlussFarbe(it.art.wert) },
                        modifier = Modifier.offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${p.eintrag.vorlage.name}: Definition öffnen oder auf die Karte ziehen"
                                onClick("Definition öffnen") { pie.definitionÖffnen(p.eintrag); true }
                            },
                        beiGröße = { pie.meldeGröße(p.eintrag.id, it) },
                    )
                }
            }
        }
        // Außerhalb der verschiebbaren Ebene; bleibt auch bei starkem Zoom erreichbar.
        Surface(Modifier.align(Alignment.TopStart).fillMaxWidth().heightIn(min = 64.dp)
            .onSizeChanged { pie.meldeBedienHöhe(it.height.toFloat()) }, tonalElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (pie.ebene != PieEbene.Kategorien) TextButton(onClick = pie::zurück) { Text("Zurück") }
                Text(if (pie.ebene == PieEbene.Kategorien) "Knotenkategorien" else if (pie.anschlussbezogen) "Passende Knoten" else pie.kategorie?.let(::pieKategorieName).orEmpty(),
                    Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2)
                TextButton(onClick = pie::schließen) { Text("Schließen") }
            }
        }
    }
    pie.definition?.let { knoten ->
        KnotenKonzeptDialog(atlas, knoten, schließen = pie::zurück, nurDefinition = true, vorschau = true)
    }
}

private fun pieKategorieName(k: KonzeptKategorie) = if (k.id == "eigene-karten") "Definierte Karten" else k.bezeichnung

@Composable
private fun PieRing(atlas: AtlasZustand, layout: PieLayout) {
    val pie = atlas.pie
    val kategorien = KonzeptBibliothekRegister.kategorien
    val farbe = MaterialTheme.colorScheme.primaryContainer
    val inaktiv = MaterialTheme.colorScheme.surfaceVariant
    val rand = MaterialTheme.colorScheme.outline
    val dichte = LocalDensity.current.density
    val mitte = pie.position
    val schritt = 360f / kategorien.size
    Canvas(Modifier.fillMaxSize()) {
        kategorien.forEachIndexed { i, k ->
            val start = -90f + i * schritt + 1f
            val außen = Rect(mitte - Offset(layout.außen, layout.außen), Size(layout.außen * 2, layout.außen * 2))
            val innen = Rect(mitte - Offset(layout.innen, layout.innen), Size(layout.innen * 2, layout.innen * 2))
            val path = Path().apply {
                arcTo(außen, start, schritt - 2f, true)
                arcTo(innen, start + schritt - 2f, -(schritt - 2f), false)
                close()
            }
            drawPath(path, if (pie.für(k).isEmpty()) inaktiv else farbe)
            drawPath(path, rand, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
    }
    kategorien.forEachIndexed { i, k ->
        val rad = (-90f + (i + .5f) * schritt) * PI.toFloat() / 180f
        val position = mitte + Offset(cos(rad), sin(rad)) * ((layout.innen + layout.außen) / 2f)
        val verfügbar = pie.für(k).isNotEmpty()
        Box(Modifier.offset { IntOffset((position.x - 44f * dichte).roundToInt(), (position.y - 28f * dichte).roundToInt()) }
            .width(88.dp).heightIn(min = 56.dp).semantics(mergeDescendants = true) {
                contentDescription = pieKategorieName(k)
                if (!verfügbar) disabled()
                else onClick { pie.wählen(i); true }
            }, contentAlignment = Alignment.Center) {
            Text(pieKategorieName(k), style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (verfügbar) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
