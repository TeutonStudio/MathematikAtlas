package de.TeutonStudio.MathematikKnoten

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
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikRechenSystem.kern.*

@Composable
fun GemeinsamerFormelBauerDialog(
    startLatex: String,
    startZeichnung: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft? = null,
    übernehmenMitZeichnung: ((FormelAusdruck, String, de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft) -> Unit)? = null,
    schließen: () -> Unit,
    übernehmen: (String) -> Unit,
    startAusdruck: FormelAusdruck? = null,
    startKnoten: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten? = null,
    startKontext: GebundeneFormel? = null,
    übernehmenGebunden: ((GebundeneFormel, de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft) -> Unit)? = null,
) {
    if(übernehmenGebunden != null) {
        val geladen=remember(startKnoten) {runCatching {startKnoten?.let(::ladeGebundeneKnotenFormel)}}
        if(geladen.isFailure) {FormelKontextFehlerDialog(geladen.exceptionOrNull()?.message.orEmpty(),schließen);return}
        val initial = remember(startKontext, startAusdruck,startLatex) { startKontext ?: geladen.getOrNull() ?: FormelBearbeitungsEntwurf.aus(startAusdruck ?: (FormelLatexCodec.importiere(startLatex) as? FormelLatexImportErgebnis.Erfolg)?.ausdruck ?: neuerFormelPlatzhalter("wurzel","Ausdruck")).formel }
        GebundenerFormelBauerDialog(initial,startZeichnung,schließen,übernehmenGebunden)
        return
    }
    val zeichnung = remember(startZeichnung) { FormelZeichnungsEntwurf(startZeichnung) }
    val editor = remember(startLatex, startAusdruck) {
        (startAusdruck?.let(::FormelEditorZustand) ?: FormelEditorZustand()).also { zustand ->
            if (startAusdruck == null && startLatex.isNotBlank()) zustand.importiere(startLatex)
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
                latexEingabe = editor.exportiere()
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

    FormelEditorDialogRahmen(
        titel = "CAS-Formelbauer", beschreibung = "LaTeX und strukturierte Formel werden live synchronisiert; die Struktur bleibt die interne Wahrheit.", zeichnung = zeichnung,
        gültig = gültig, schließen = schließen,
        übernehmen = { if (übernehmenMitZeichnung != null) übernehmenMitZeichnung(editor.wurzel, editor.exportiere(), zeichnung.übernehmen()) else übernehmen(editor.exportiere()) },
        vorschau = {
                Surface(
                    Modifier.fillMaxWidth().heightIn(min = 128.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(LocalFormelEditorAbstände.current.inhalt),
                        verticalArrangement = Arrangement.spacedBy(LocalFormelEditorAbstände.current.standard),
                    ) {
                        Text("Formelvorschau", style = MaterialTheme.typography.labelLarge)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LocalFormelEditorAbstände.current.bereich),
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
        },
    ) {


                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LocalFormelEditorAbstände.current.standard),
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
                    OutlinedButton(onClick = { if (editor.gruppiereAuswahl()) geändert() }) {
                        Text("Klammern setzen")
                    }
                    OutlinedButton(
                        onClick = { if (editor.entgruppiereAuswahl()) geändert() },
                        enabled = editor.kannEntgruppieren(),
                    ) { Text("Klammern entfernen") }
                    TextButton(onClick = { if (editor.loescheAuswahl()) geändert() }) { Text("Auswahl löschen") }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalFormelEditorAbstände.current.standard)) {
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 280.dp),
                    contentPadding = PaddingValues(vertical = LocalFormelEditorAbstände.current.winzig),
                    horizontalArrangement = Arrangement.spacedBy(LocalFormelEditorAbstände.current.standard),
                    verticalArrangement = Arrangement.spacedBy(LocalFormelEditorAbstände.current.standard),
                ) {
                    items(
                        FormelTastatur.standard.filter { it.kategorie == kategorie },
                        key = FormelTastaturTaste::id,
                    ) { taste ->
                        OutlinedButton(
                            onClick = { if (editor.druecke(taste)) geändert() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            taste.beschriftungLatex?.let { LatexText(it, style = MaterialTheme.typography.labelLarge) }
                                ?: Text(taste.beschriftung)
                        }
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
