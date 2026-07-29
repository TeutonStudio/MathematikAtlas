package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnung
import de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnungSpeicher
import de.TeutonStudio.MathematikAtlas.speicher.formatiereOrdnerPfad
import de.TeutonStudio.MathematikAtlas.speicher.parseOrdnerPfad
import de.TeutonStudio.MathematikKnoten.LatexText

@Composable
internal fun VerwaltungsFenster(zustand: AtlasZustand, modifier: Modifier) {
    val darstellung = LocalDarstellungsSteuerung.current
    var darstellungsMenüGeöffnet by remember { mutableStateOf(false) }
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Mathematik Atlas",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Box {
                    TextButton(
                        onClick = { darstellungsMenüGeöffnet = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Darstellung ändern, aktuell ${darstellung.modus.anzeigeName}"
                        },
                    ) { Text("◐", style = MaterialTheme.typography.titleLarge) }
                    DropdownMenu(
                        expanded = darstellungsMenüGeöffnet,
                        onDismissRequest = { darstellungsMenüGeöffnet = false },
                    ) {
                        DarstellungsModus.entries.forEach { modus ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = darstellung.modus == modus,
                                            onClick = null,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(modus.anzeigeName)
                                    }
                                },
                                onClick = {
                                    darstellungsMenüGeöffnet = false
                                    darstellung.ändereModus(modus)
                                },
                            )
                        }
                    }
                }
            }
            PrimaryScrollableTabRow(selectedTabIndex = zustand.linkerBereich.ordinal, edgePadding = 8.dp) {
                VerwaltungsBereich.entries.forEach { bereich ->
                    Tab(selected = zustand.linkerBereich == bereich, onClick = { zustand.linkerBereich = bereich }, text = { Text(bereich.name) })
                }
            }
            when (zustand.linkerBereich) {
                VerwaltungsBereich.Karten -> KartenListe(zustand)
                VerwaltungsBereich.Konzepte -> KonzeptListe()
                VerwaltungsBereich.Variablen -> VariablenListe(zustand)
                VerwaltungsBereich.Auswertung -> AuswertungsListe(zustand)
                VerwaltungsBereich.Fehler -> FehlerListe(zustand)
            }
        }
    }
}

@Composable
private fun KartenListe(zustand: AtlasZustand) {
    val context = LocalContext.current
    val ordnungsSpeicher = remember(context) { KartenOrdnungSpeicher(context) }
    var ordnung by remember(ordnungsSpeicher) { mutableStateOf(ordnungsSpeicher.lade()) }
    var dialog by remember { mutableStateOf<KartenOrdnerDialog?>(null) }
    var ordnerMenü by remember { mutableStateOf<List<String>?>(null) }

    fun speichere(neu: KartenOrdnung) {
        val normalisiert = neu.normalisiert()
        ordnungsSpeicher.speichere(normalisiert)
        ordnung = normalisiert
    }

    val einträge = remember(zustand.karten, ordnung) { kartenListenEinträge(zustand.karten, ordnung) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = zustand::neueKarte, modifier = Modifier.weight(1f)) { Text("Neue Karte") }
            OutlinedButton(onClick = { dialog = KartenOrdnerDialog.OrdnerAnlegen }, modifier = Modifier.weight(1f)) { Text("Ordner +") }
        }
        OutlinedButton(
            onClick = zustand::archiviereAktuell,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        ) { Text("Aktuelle Karte archivieren") }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(einträge, key = KartenListenEintrag::schlüssel) { eintrag ->
                when (eintrag) {
                    is KartenListenEintrag.Ordner -> ListItem(
                        headlineContent = { Text("▾ ${eintrag.pfad.last()}") },
                        supportingContent = {
                            if (eintrag.pfad.size > 1) Text(formatiereOrdnerPfad(eintrag.pfad.dropLast(1)))
                        },
                        trailingContent = {
                            Box {
                                TextButton(onClick = { ordnerMenü = eintrag.pfad }) { Text("⋮") }
                                DropdownMenu(
                                    expanded = ordnerMenü == eintrag.pfad,
                                    onDismissRequest = { ordnerMenü = null },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Pfad bearbeiten") },
                                        onClick = {
                                            ordnerMenü = null
                                            dialog = KartenOrdnerDialog.OrdnerVerschieben(eintrag.pfad)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Leeren Ordner löschen") },
                                        enabled = ordnung.kannOrdnerLöschen(eintrag.pfad),
                                        onClick = {
                                            ordnerMenü = null
                                            speichere(ordnung.ohneOrdner(eintrag.pfad))
                                        },
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(start = (eintrag.tiefe * 12).dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)),
                    )
                    is KartenListenEintrag.Karte -> ListItem(
                        headlineContent = { Text(eintrag.karte.name) },
                        supportingContent = { Text("Version ${eintrag.karte.version}") },
                        trailingContent = {
                            TextButton(onClick = { dialog = KartenOrdnerDialog.KarteVerschieben(eintrag.karte) }) { Text("Ordner") }
                        },
                        modifier = Modifier.padding(start = (eintrag.tiefe * 12).dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { zustand.öffne(eintrag.karte) }
                            .background(
                                if (eintrag.karte.id == zustand.editor.karte.id) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface,
                            ),
                    )
                }
            }
            if (einträge.isEmpty()) item {
                Text("Keine Karten vorhanden.", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    when (val aktuell = dialog) {
        KartenOrdnerDialog.OrdnerAnlegen -> OrdnerPfadDialog(
            titel = "Ordner anlegen",
            aktuellerPfad = emptyList(),
            stammErlaubt = false,
            schließen = { dialog = null },
            übernehmen = { pfad ->
                speichere(ordnung.mitOrdner(pfad))
                null
            },
        )
        is KartenOrdnerDialog.KarteVerschieben -> OrdnerPfadDialog(
            titel = "Karte verschieben",
            aktuellerPfad = ordnung.ordnerFür(aktuell.karte.id),
            stammErlaubt = true,
            schließen = { dialog = null },
            übernehmen = { pfad ->
                speichere(ordnung.mitKarteInOrdner(aktuell.karte.id, pfad))
                null
            },
        )
        is KartenOrdnerDialog.OrdnerVerschieben -> OrdnerPfadDialog(
            titel = "Ordnerpfad bearbeiten",
            aktuellerPfad = aktuell.pfad,
            stammErlaubt = false,
            schließen = { dialog = null },
            übernehmen = { pfad ->
                runCatching { ordnung.verschiebeOrdner(aktuell.pfad, pfad) }
                    .fold(
                        onSuccess = { neu -> speichere(neu); null },
                        onFailure = { it.message ?: "Der Ordnerpfad ist ungültig." },
                    )
            },
        )
        null -> Unit
    }
}

@Composable
private fun OrdnerPfadDialog(
    titel: String,
    aktuellerPfad: List<String>,
    stammErlaubt: Boolean,
    schließen: () -> Unit,
    übernehmen: (List<String>) -> String?,
) {
    var text by remember(aktuellerPfad) { mutableStateOf(formatiereOrdnerPfad(aktuellerPfad)) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val pfad = parseOrdnerPfad(text)
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text(titel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; fehler = null },
                    label = { Text("Ordnerpfad") },
                    placeholder = { Text("Algebra/Lineare Algebra") },
                    supportingText = {
                        Text(fehler ?: if (stammErlaubt) "Leer lassen für die oberste Ebene." else "Unterordner mit / trennen.")
                    },
                    isError = fehler != null || (!stammErlaubt && pfad.isEmpty()),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!stammErlaubt && pfad.isEmpty()) {
                        fehler = "Der Ordnerpfad darf nicht leer sein."
                    } else {
                        fehler = übernehmen(pfad)
                        if (fehler == null) schließen()
                    }
                },
                enabled = stammErlaubt || pfad.isNotEmpty(),
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
    )
}

private sealed interface KartenOrdnerDialog {
    data object OrdnerAnlegen : KartenOrdnerDialog
    data class KarteVerschieben(val karte: KartenDaten) : KartenOrdnerDialog
    data class OrdnerVerschieben(val pfad: List<String>) : KartenOrdnerDialog
}

private sealed interface KartenListenEintrag {
    val tiefe: Int
    val schlüssel: String

    data class Ordner(val pfad: List<String>, override val tiefe: Int) : KartenListenEintrag {
        override val schlüssel = "ordner:${formatiereOrdnerPfad(pfad)}"
    }

    data class Karte(val karte: KartenDaten, override val tiefe: Int) : KartenListenEintrag {
        override val schlüssel = "karte:${karte.id.wert}"
    }
}

private fun kartenListenEinträge(karten: List<KartenDaten>, ordnung: KartenOrdnung): List<KartenListenEintrag> = buildList {
    fun fügeEbeneHinzu(eltern: List<String>, tiefe: Int) {
        ordnung.ordner.asSequence()
            .filter { it.size == eltern.size + 1 && it.take(eltern.size) == eltern }
            .sortedBy { it.last().lowercase() }
            .forEach { pfad ->
                add(KartenListenEintrag.Ordner(pfad, tiefe))
                fügeEbeneHinzu(pfad, tiefe + 1)
            }
        karten.asSequence()
            .filter { ordnung.ordnerFür(it.id) == eltern }
            .sortedBy { it.name.lowercase() }
            .forEach { add(KartenListenEintrag.Karte(it, tiefe)) }
    }
    fügeEbeneHinzu(emptyList(), 0)
}

@Composable private fun KonzeptListe() {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Mathematische Systeme", style = MaterialTheme.typography.titleMedium) }
        items(listOf("Standardanalysis", "Nichtstandardanalysis", "Aussagenlogik", "Mengenlehre", "Lineare Algebra")) { name ->
            ListItem(headlineContent = { Text(name) }, supportingContent = { Text("Definitionen, Kurzverfahren und Beispiele können als Karten hinterlegt werden.") })
        }
    }
}

@Composable private fun VariablenListe(zustand: AtlasZustand) {
    val variablen = zustand.auswertung.knoten.values.flatMap { it.ausgaben.values }.mapNotNull { it.objekt as? de.TeutonStudio.MathematikRechenSystem.kern.Variable }.distinctBy { it.name }
    LazyColumn(contentPadding = PaddingValues(12.dp)) {
        if (variablen.isEmpty()) item { Text("Keine freien Variablen in der aktuellen Karte.") }
        items(variablen) { ListItem(headlineContent = { Text(it.name) }) }
    }
}

@Composable private fun AuswertungsListe(zustand: AtlasZustand) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(zustand.editor.karte.knoten) { knoten ->
            val e = zustand.auswertung.knoten[knoten.id]
            ListItem(
                headlineContent = { Text(knoten.name) },
                supportingContent = {
                    val latex = e?.ausgaben?.values?.joinToString { it.objekt.zuLatex() }
                    if (latex != null) LatexText(latex, style = MaterialTheme.typography.bodyMedium)
                    else Text(e?.fehler ?: "Noch kein Ergebnis")
                },
            )
        }
    }
}

@Composable private fun FehlerListe(zustand: AtlasZustand) {
    LazyColumn(contentPadding = PaddingValues(12.dp)) {
        if (zustand.auswertung.fehler.isEmpty()) item { Text("Keine Auswertungsfehler.") }
        items(zustand.auswertung.fehler) { fehler -> ListItem(headlineContent = { Text(fehler, color = MaterialTheme.colorScheme.error) }) }
    }
}
