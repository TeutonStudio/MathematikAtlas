package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.math.roundToInt

/** Interaktive Ebene für persistierte visuelle Gruppen und Auswahlmarkierungen. */
@Composable
internal fun VisuelleGruppenEbene(editor: KartenEditorZustand) {
    var bearbeiteteGruppenId by remember(editor.karte.id) { mutableStateOf<VisuelleGruppenId?>(null) }
    val aktuelleGruppe = bearbeiteteGruppenId?.let { id ->
        editor.karte.visuelleGruppen.firstOrNull { it.id == id }
    }

    Box(Modifier.fillMaxSize()) {
        editor.karte.visuelleGruppen.forEach { gruppe ->
            key(gruppe.id) {
                VisuelleGruppeDarstellung(
                    editor = editor,
                    gruppe = gruppe,
                    bearbeiten = { bearbeiteteGruppenId = gruppe.id },
                )
            }
        }
        AuswahlMarkierungen(editor)
    }

    aktuelleGruppe?.let { gruppe ->
        VisuelleGruppeDialog(
            gruppe = gruppe,
            schließen = { bearbeiteteGruppenId = null },
            titelÜbernehmen = { titel ->
                editor.führeAus(KartenAktion.VisuelleGruppeTitelÄndern(gruppe.id, titel))
                bearbeiteteGruppenId = null
            },
            kinderZuordnen = {
                editor.führeAus(KartenAktion.VisuelleGruppenKinderZuordnen(gruppe.id))
            },
            gruppeLöschen = {
                editor.führeAus(KartenAktion.VisuelleGruppeLöschen(gruppe.id))
                bearbeiteteGruppenId = null
            },
        )
    }
}

@Composable
private fun BoxScope.VisuelleGruppeDarstellung(
    editor: KartenEditorZustand,
    gruppe: VisuelleKnotenGruppeDaten,
    bearbeiten: () -> Unit,
) {
    val dichte = LocalDensity.current
    val ansicht = editor.karte.ansicht
    val aktuelleGruppe by rememberUpdatedState(gruppe)
    val faktor = dichte.density * ansicht.zoom
    val start = Offset(
        ansicht.verschiebung.x + gruppe.position.x * faktor,
        ansicht.verschiebung.y + gruppe.position.y * faktor,
    )
    val breite = (gruppe.größe.breite * ansicht.zoom).coerceAtLeast(1f)
    val höhe = (gruppe.größe.höhe * ansicht.zoom).coerceAtLeast(1f)
    val kopfHöhe = (VISUELLE_GRUPPE_KOPFZEILE_HÖHE * ansicht.zoom).coerceAtLeast(18f)
    val gruppenFarbe = MaterialTheme.colorScheme.tertiary

    Box(
        Modifier
            .offset { IntOffset(start.x.roundToInt(), start.y.roundToInt()) }
            .size(breite.dp, höhe.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = gruppenFarbe.copy(alpha = .07f),
                cornerRadius = CornerRadius(14.dp.toPx()),
            )
            drawRoundRect(
                color = gruppenFarbe.copy(alpha = .85f),
                cornerRadius = CornerRadius(14.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawLine(
                color = gruppenFarbe.copy(alpha = .75f),
                start = Offset(0f, kopfHöhe.dp.toPx()),
                end = Offset(size.width, kopfHöhe.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }

        Surface(
            color = gruppenFarbe.copy(alpha = .18f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(kopfHöhe.dp)
                .semantics { contentDescription = "Visuelle Gruppe ${gruppe.titel} verschieben" }
                .pointerInput(gruppe.id, ansicht.zoom, dichte.density) {
                    detectDragGestures(
                        onDragStart = { editor.beginneInteraktion() },
                        onDragCancel = { editor.beendeInteraktion() },
                        onDragEnd = { editor.beendeInteraktion() },
                        onDrag = { änderung, verschiebung ->
                            änderung.consume()
                            val weltFaktor = (dichte.density * ansicht.zoom).coerceAtLeast(0.0001f)
                            editor.führeAus(
                                KartenAktion.VisuelleGruppeVerschieben(
                                    id = gruppe.id,
                                    delta = GraphPunkt(
                                        verschiebung.x / weltFaktor,
                                        verschiebung.y / weltFaktor,
                                    ),
                                ),
                                mitHistorie = false,
                            )
                        },
                    )
                },
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 10.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = gruppe.titel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = bearbeiten,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.semantics {
                        contentDescription = "Visuelle Gruppe ${gruppe.titel} bearbeiten"
                    },
                ) { Text("⋯") }
            }
        }

        Surface(
            color = gruppenFarbe,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(22.dp)
                .semantics { contentDescription = "Visuelle Gruppe ${gruppe.titel} skalieren" }
                .pointerInput(gruppe.id, ansicht.zoom, dichte.density) {
                    detectDragGestures(
                        onDragStart = { editor.beginneInteraktion() },
                        onDragCancel = { editor.beendeInteraktion() },
                        onDragEnd = { editor.beendeInteraktion() },
                        onDrag = { änderung, verschiebung ->
                            änderung.consume()
                            val weltFaktor = (dichte.density * ansicht.zoom).coerceAtLeast(0.0001f)
                            val aktuell = aktuelleGruppe
                            editor.führeAus(
                                KartenAktion.VisuelleGruppeGrößeÄndern(
                                    id = aktuell.id,
                                    größe = GraphGröße(
                                        breite = aktuell.größe.breite + verschiebung.x / weltFaktor,
                                        höhe = aktuell.größe.höhe + verschiebung.y / weltFaktor,
                                    ),
                                ),
                                mitHistorie = false,
                            )
                        },
                    )
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("⌟", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AuswahlMarkierungen(editor: KartenEditorZustand) {
    val auswahlFarbe = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        val karte = editor.karte
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

@Composable
private fun VisuelleGruppeDialog(
    gruppe: VisuelleKnotenGruppeDaten,
    schließen: () -> Unit,
    titelÜbernehmen: (String) -> Unit,
    kinderZuordnen: () -> Unit,
    gruppeLöschen: () -> Unit,
) {
    var titel by remember(gruppe.id, gruppe.titel) { mutableStateOf(gruppe.titel) }
    val bereinigterTitel = titel.trim()
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("Visuelle Gruppe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titel,
                    onValueChange = { titel = it },
                    label = { Text("Titel") },
                    singleLine = true,
                    isError = bereinigterTitel.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${gruppe.knotenIds.size} zugeordnete Knoten",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = kinderZuordnen,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Vollständig enthaltene Knoten zuordnen") }
                Button(
                    onClick = gruppeLöschen,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Nur Gruppe löschen") }
                Text(
                    "Knoten und Verbindungen bleiben beim Löschen erhalten.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { titelÜbernehmen(bereinigterTitel) },
                enabled = bereinigterTitel.isNotEmpty(),
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
    )
}
