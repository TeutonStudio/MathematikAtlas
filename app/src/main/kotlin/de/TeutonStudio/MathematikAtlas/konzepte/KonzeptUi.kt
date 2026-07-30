package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KnotenKonzeptDialog(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    schließen: () -> Unit,
) {
    val konzept = remember(knoten.art) { TestDefinitionsKarten.fürKnoten(knoten) }
    var reiterIndex by remember(knoten.art) { mutableIntStateOf(0) }
    var komplexDarstellung by remember(knoten.id) { mutableStateOf(KomplexDarstellung.Kartesisch) }

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.92f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(konzept?.name ?: knoten.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            konzept?.beschreibung ?: "Für diese Knotenart ist keine Definitionskarte registriert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }
                HorizontalDivider()

                if (konzept == null) {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp)) {
                        Text("Fehlende Definition für ${knoten.art}", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    if (konzept.reiter.size > 1) {
                        PrimaryTabRow(selectedTabIndex = reiterIndex) {
                            konzept.reiter.forEachIndexed { index, reiter ->
                                Tab(
                                    selected = reiterIndex == index,
                                    onClick = { reiterIndex = index },
                                    text = { Text(reiter.titel) },
                                )
                            }
                        }
                    }
                    if (konzept.reiter.any(KonzeptReiter::besitztDarstellungsVarianten)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = komplexDarstellung == KomplexDarstellung.Kartesisch,
                                onClick = { komplexDarstellung = KomplexDarstellung.Kartesisch },
                                label = { Text("Kartesisch") },
                            )
                            FilterChip(
                                selected = komplexDarstellung == KomplexDarstellung.Polar,
                                onClick = { komplexDarstellung = KomplexDarstellung.Polar },
                                label = { Text("Polar") },
                            )
                        }
                    }
                    val reiter = konzept.reiter[reiterIndex.coerceIn(0, konzept.reiter.lastIndex)]
                    UnveränderlicheKonzeptKarte(
                        zustand = zustand,
                        karte = reiter.karteFür(komplexDarstellung),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }

                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.dupliziereAuswahl()
                            schließen()
                        },
                    ) { Text("Duplizieren") }
                    OutlinedButton(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.isoliereAusgewähltenKnoten()
                            schließen()
                        },
                    ) { Text("Isolieren") }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            zustand.editor.wähleKnoten(knoten.id)
                            zustand.editor.löscheAuswahl()
                            schließen()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Löschen") }
                }
            }
        }
    }
}

@Composable
private fun UnveränderlicheKonzeptKarte(
    zustand: AtlasZustand,
    karte: KartenDaten,
    modifier: Modifier = Modifier,
) {
    val editor = remember(karte.id) {
        KartenEditorZustand(karte, GraphPrüfung(zustand.anschlussArten))
    }

    LaunchedEffect(editor.karte) {
        val ohneAnsicht = editor.karte.copy(ansicht = karte.ansicht)
        if (ohneAnsicht != karte) {
            editor.ersetzeKarte(karte.copy(ansicht = editor.karte.ansicht))
        }
    }

    KnotenKartenEditor(
        zustand = editor,
        modifier = modifier,
        rendererFür = { dokumentationsKnoten ->
            if (dokumentationsKnoten.art.startsWith("konzept.")) KonzeptDokumentationsRenderer
            else zustand.rendererFür(dokumentationsKnoten)
        },
        farbeFürAnschluss = { anschluss -> anschlussFarbe(anschluss.art.wert) },
    )
}

private object KonzeptDokumentationsRenderer : KnotenRenderer {
    @Composable
    override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (knoten.art) {
                TestDefinitionsKarten.KONZEPT_REGEL_ART -> {
                    Text(knoten.name, style = MaterialTheme.typography.titleMedium)
                    Text(knoten.parameter["regel"].orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        knoten.parameter["knotenArt"].orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TestDefinitionsKarten.KONZEPT_EINGANG_ART -> {
                    Text("Eingang", style = MaterialTheme.typography.labelLarge)
                    Text(knoten.name, style = MaterialTheme.typography.titleMedium)
                    Text(knoten.parameter["typ"].orEmpty(), style = MaterialTheme.typography.bodySmall)
                    if (knoten.parameter["variabel"] == "true") {
                        Text("erweiterbar", style = MaterialTheme.typography.labelSmall)
                    }
                }
                else -> {
                    Text("Ausgang", style = MaterialTheme.typography.labelLarge)
                    Text(knoten.name, style = MaterialTheme.typography.titleMedium)
                    Text(knoten.parameter["typ"].orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
