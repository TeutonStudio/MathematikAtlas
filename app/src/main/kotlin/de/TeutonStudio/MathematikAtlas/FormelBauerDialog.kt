package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.MathematikRechenSystem.kern.*

@Composable
internal fun FormelBauerDialog(
    startLatex: String,
    schließen: () -> Unit,
    übernehmen: (String) -> Unit,
) {
    val editor = remember(startLatex) {
        FormelEditorZustand().also { zustand ->
            if (startLatex.isNotBlank()) zustand.importiere(startLatex)
        }
    }
    var revision by remember { mutableIntStateOf(0) }
    var latexEingabe by remember(startLatex) {
        mutableStateOf(startLatex.ifBlank { editor.exportiere() })
    }
    var importFehler by remember { mutableStateOf<String?>(null) }
    var zahlText by remember { mutableStateOf("") }
    var variablenText by remember { mutableStateOf("x") }
    var kategorie by remember { mutableStateOf(FormelTastenKategorie.GRUNDRECHNUNG) }

    val prüfung = remember(revision) { FormelAusdruckPruefer.pruefe(editor.wurzel) }
    val gültig = importFehler == null && prüfung == FormelPruefung.Gueltig

    fun geändert() {
        revision++
        latexEingabe = editor.exportiere()
        importFehler = null
    }

    fun latexGeändert(neu: String) {
        latexEingabe = neu
        when (val ergebnis = editor.importiere(neu)) {
            is FormelLatexImportErgebnis.Erfolg -> {
                revision++
                importFehler = null
            }
            is FormelLatexImportErgebnis.Fehler -> {
                importFehler = "${ergebnis.nachricht} (Position ${ergebnis.position + 1})"
            }
        }
    }

    fun cursorBewegen(richtung: FormelCursorRichtung) {
        if (editor.bewegeCursor(richtung)) revision++
    }

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.92f).widthIn(max = 1180.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
        ) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("CAS-Formelbauer", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "LaTeX und strukturierte Formel werden live synchronisiert; die Struktur bleibt die interne Wahrheit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }

                Surface(
                    Modifier.fillMaxWidth().heightIn(min = 128.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Formelvorschau", style = MaterialTheme.typography.labelLarge)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StrukturelleFormelVorschau(
                                wurzel = editor.wurzel,
                                cursor = editor.cursor,
                                cursorSetzen = { id, position ->
                                    if (editor.setzeCursorAufAusdruck(id, position)) revision++
                                },
                                navigieren = ::cursorBewegen,
                                rücklöschen = { if (editor.loescheRueckwaerts()) geändert() },
                                vorwärtsLöschen = { if (editor.loescheVorwaerts()) geändert() },
                                modifier = Modifier.weight(1f),
                            )
                            FormelCursorTasten(
                                kannBewegen = editor::kannCursorBewegen,
                                bewegen = ::cursorBewegen,
                            )
                        }
                        Text(
                            when (val status = prüfung) {
                                FormelPruefung.Gueltig -> "Vollständige Formel"
                                is FormelPruefung.Unvollstaendig -> "${status.platzhalterIds.size} offene Platzhalter"
                                is FormelPruefung.Ungueltig -> status.gruende.joinToString(" · ")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gültig) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "Cursor: ${editor.cursor.position}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (editor.rueckgaengig()) geändert() },
                        enabled = editor.kannRueckgaengig,
                    ) { Text("Rückgängig") }
                    OutlinedButton(
                        onClick = { if (editor.wiederholen()) geändert() },
                        enabled = editor.kannWiederholen,
                    ) { Text("Wiederholen") }
                    OutlinedButton(
                        onClick = { if (editor.naechsterPlatzhalter(-1) != null) revision++ },
                        enabled = editor.offenePlatzhalter.isNotEmpty(),
                    ) { Text("← Platzhalter") }
                    OutlinedButton(
                        onClick = { if (editor.naechsterPlatzhalter() != null) revision++ },
                        enabled = editor.offenePlatzhalter.isNotEmpty(),
                    ) { Text("Platzhalter →") }
                    TextButton(onClick = { if (editor.loescheAuswahl()) geändert() }) { Text("Auswahl löschen") }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = zahlText,
                        onValueChange = { zahlText = it },
                        label = { Text("Zahl") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { if (editor.setzeZahl(zahlText)) { zahlText = ""; geändert() } },
                        enabled = zahlText.isNotBlank(),
                    ) { Text("Zahl einsetzen") }
                    OutlinedTextField(
                        value = variablenText,
                        onValueChange = { variablenText = it },
                        label = { Text("Variable") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { if (editor.setzeVariable(variablenText)) geändert() },
                        enabled = variablenText.isNotBlank(),
                    ) { Text("Variable einsetzen") }
                }

                PrimaryScrollableTabRow(
                    selectedTabIndex = FormelTastenKategorie.entries.indexOf(kategorie),
                    edgePadding = 0.dp,
                ) {
                    FormelTastenKategorie.entries.forEach { eintrag ->
                        Tab(
                            selected = kategorie == eintrag,
                            onClick = { kategorie = eintrag },
                            text = { Text(eintrag.anzeigeName()) },
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        FormelTastatur.standard.filter { it.kategorie == kategorie },
                        key = FormelTastaturTaste::id,
                    ) { taste ->
                        OutlinedButton(
                            onClick = { if (editor.druecke(taste)) geändert() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(taste.beschriftung) }
                    }
                }

                OutlinedTextField(
                    value = latexEingabe,
                    onValueChange = ::latexGeändert,
                    label = { Text("LaTeX") },
                    supportingText = {
                        Text(importFehler ?: "Änderungen werden sofort in die strukturierte Formel übernommen.")
                    },
                    isError = importFehler != null,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { übernehmen(editor.exportiere()) },
                        enabled = gültig,
                    ) { Text("Formel übernehmen") }
                }
            }
        }
    }
}

private fun FormelTastenKategorie.anzeigeName(): String = when (this) {
    FormelTastenKategorie.GRUNDRECHNUNG -> "Grundrechnung"
    FormelTastenKategorie.POTENZEN -> "Potenzen"
    FormelTastenKategorie.FUNKTIONEN -> "Funktionen"
    FormelTastenKategorie.TRIGONOMETRIE -> "Trigonometrie"
    FormelTastenKategorie.HYPERBOLISCH -> "Hyperbolisch"
    FormelTastenKategorie.KONSTANTEN -> "Konstanten"
}
