package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikRechenSystem.kern.FormelAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.FormelAusdruckPruefer
import de.TeutonStudio.MathematikRechenSystem.kern.FormelPruefung
import de.TeutonStudio.MathematikRechenSystem.kern.FormelTyp
import de.TeutonStudio.MathematikRechenSystem.kern.StrukturFormelDarstellung
import de.TeutonStudio.MathematikRechenSystem.kern.StrukturFormelEditorZustand
import de.TeutonStudio.MathematikRechenSystem.kern.StrukturFormelFamilie
import de.TeutonStudio.MathematikRechenSystem.kern.StrukturFormelKategorie
import de.TeutonStudio.MathematikRechenSystem.kern.StrukturFormelTastatur
import de.TeutonStudio.MathematikRechenSystem.kern.strukturFormelPlatzhalter

@Composable
internal fun StrukturFormelBauerDialog(
    familie: StrukturFormelFamilie,
    start: FormelAusdruck?,
    quantorVariable: String,
    schließen: () -> Unit,
    übernehmen: (FormelAusdruck) -> Unit,
) {
    val editor = remember(familie, start) {
        StrukturFormelEditorZustand(
            start ?: strukturFormelPlatzhalter("wurzel", familie.wurzelErwartung, "Ausdruck"),
        )
    }
    var revision by remember { mutableIntStateOf(0) }
    var variablenName by remember { mutableStateOf("x") }
    var variablenTyp by remember(familie) { mutableStateOf(familie.erlaubteVariablenTypen.first()) }
    var typMenü by remember { mutableStateOf(false) }
    var zahlText by remember { mutableStateOf("") }

    val tasten = remember(familie) { StrukturFormelTastatur.fuer(familie) }
    val kategorien = remember(tasten) { tasten.map { it.kategorie }.distinct() }
    var kategorie by remember(familie) { mutableStateOf(kategorien.first()) }
    val vorschau = remember(revision, quantorVariable) {
        StrukturFormelDarstellung.latex(editor.wurzel, quantorVariable)
    }
    val prüfung = remember(revision) { FormelAusdruckPruefer.pruefe(editor.wurzel) }
    val gültig = prüfung == FormelPruefung.Gueltig

    fun geändert() {
        revision++
    }

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(.95f)
                .fillMaxHeight(.92f)
                .widthIn(max = 1180.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
        ) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Typisierter CAS-Formelbauer", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Operatoren erzeugen einen Ausdrucksbaum. Variablen werden später als typisierte Knotenanschlüsse sichtbar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }

                Surface(
                    Modifier.fillMaxWidth().heightIn(min = 96.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Formelvorschau", style = MaterialTheme.typography.labelLarge)
                        LatexText(vorschau, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            when (val status = prüfung) {
                                FormelPruefung.Gueltig -> "Vollständige Formel · Ergebnistyp ${editor.wurzel.typ.anzeigeName()}"
                                is FormelPruefung.Unvollstaendig -> "${status.platzhalterIds.size} offene Platzhalter"
                                is FormelPruefung.Ungueltig -> status.gruende.joinToString(" · ")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gültig) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "Auswahl: ${editor.auswahlTyp?.anzeigeName() ?: "keine"}",
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
                        onClick = { if (editor.naechsterPlatzhalter(-1) != null) geändert() },
                        enabled = editor.offenePlatzhalter.isNotEmpty(),
                    ) { Text("← Platzhalter") }
                    OutlinedButton(
                        onClick = { if (editor.naechsterPlatzhalter() != null) geändert() },
                        enabled = editor.offenePlatzhalter.isNotEmpty(),
                    ) { Text("Platzhalter →") }
                    TextButton(onClick = { if (editor.loescheAuswahl()) geändert() }) {
                        Text("Auswahl löschen")
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = variablenName,
                        onValueChange = { variablenName = it },
                        label = { Text("Variable") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        OutlinedButton(onClick = { typMenü = true }) {
                            Text(variablenTyp.anzeigeName())
                        }
                        DropdownMenu(expanded = typMenü, onDismissRequest = { typMenü = false }) {
                            familie.erlaubteVariablenTypen.forEach { typ ->
                                DropdownMenuItem(
                                    text = { Text(typ.anzeigeName()) },
                                    onClick = {
                                        variablenTyp = typ
                                        typMenü = false
                                    },
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { if (editor.setzeVariable(variablenName, variablenTyp)) geändert() },
                        enabled = variablenName.isNotBlank() && editor.kannEinsetzen(variablenTyp),
                    ) { Text("Variable einsetzen") }
                }

                if (FormelTyp.ZAHL in familie.erlaubteVariablenTypen) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = zahlText,
                            onValueChange = { zahlText = it },
                            label = { Text("Zahl") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                if (editor.setzeZahl(zahlText)) {
                                    zahlText = ""
                                    geändert()
                                }
                            },
                            enabled = zahlText.isNotBlank() && editor.kannEinsetzen(FormelTyp.ZAHL),
                        ) { Text("Zahl einsetzen") }
                    }
                }

                HorizontalDivider()
                PrimaryScrollableTabRow(
                    selectedTabIndex = kategorien.indexOf(kategorie),
                    edgePadding = 0.dp,
                ) {
                    kategorien.forEach { eintrag ->
                        Tab(
                            selected = kategorie == eintrag,
                            onClick = { kategorie = eintrag },
                            text = { Text(eintrag.anzeigeName()) },
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(116.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tasten.filter { it.kategorie == kategorie }, key = { it.id }) { taste ->
                        OutlinedButton(
                            onClick = { if (editor.druecke(taste)) geändert() },
                            enabled = editor.kannDruecken(taste),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(taste.beschriftung)
                                Text(
                                    taste.ergebnisTyp.anzeigeName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (familie == StrukturFormelFamilie.AUSSAGESATZ) {
                    Text(
                        "Quantoren bleiben eigenständige Operatoren des Aussagesatz-Knotens, weil gebundene Variablen keine freien Handles sind.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = schließen) { Text("Abbrechen") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { übernehmen(editor.wurzel) },
                        enabled = gültig,
                    ) { Text("Formel übernehmen") }
                }
            }
        }
    }
}

private fun StrukturFormelKategorie.anzeigeName(): String = when (this) {
    StrukturFormelKategorie.LOGIK -> "Logik"
    StrukturFormelKategorie.VEKTOR -> "Vektor"
    StrukturFormelKategorie.MATRIX -> "Matrix"
    StrukturFormelKategorie.TENSOR -> "Tensor"
    StrukturFormelKategorie.SKALAR -> "Skalar"
}

private fun FormelTyp.anzeigeName(): String = when (this) {
    FormelTyp.ZAHL -> "Zahl"
    FormelTyp.MENGE -> "Menge"
    FormelTyp.AUSSAGE -> "Aussage"
    FormelTyp.TUPEL -> "Tupel"
    FormelTyp.VEKTOR -> "Vektor"
    FormelTyp.MATRIX -> "Matrix"
    FormelTyp.TENSOR -> "Tensor"
    FormelTyp.METHODE -> "Methode"
    FormelTyp.OBJEKT -> "Objekt"
}
