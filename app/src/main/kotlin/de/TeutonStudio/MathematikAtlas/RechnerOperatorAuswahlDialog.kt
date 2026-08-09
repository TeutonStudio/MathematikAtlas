package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.logik.KnotenErsetzungsAuswirkung
import de.TeutonStudio.MathematikKnoten.LatexText

@Composable
internal fun RechnerOperatorAuswahlDialog(
    familienTitel: String,
    einträge: List<RechnerOperatorAuswahlEintrag>,
    aktuelleId: String?,
    auswirkungFür: (RechnerOperatorAuswahlEintrag) -> KnotenErsetzungsAuswirkung?,
    schließen: () -> Unit,
    operatorÜbernehmen: (RechnerOperatorAuswahlEintrag) -> Unit,
    formelÖffnen: (RechnerOperatorAuswahlEintrag) -> Unit,
) {
    var suchtext by remember(einträge) { mutableStateOf("") }
    var kategorie by remember(einträge) { mutableStateOf<String?>(null) }
    var auswahlId by remember(einträge, aktuelleId) {
        mutableStateOf(einträge.firstOrNull { it.id == aktuelleId }?.id ?: einträge.firstOrNull()?.id)
    }
    val kategorien = remember(einträge) { einträge.map { it.kategorie }.distinct() }
    val sichtbar = remember(einträge, suchtext, kategorie) {
        filtereRechnerOperatoren(einträge, suchtext, kategorie)
    }
    val ausgewählt = einträge.firstOrNull { it.id == auswahlId }
    val auswirkung = ausgewählt?.let(auswirkungFür)
    val sucheFokus = remember { FocusRequester() }
    val kannBestätigen = ausgewählt != null && ausgewählt.art != RechnerOperatorAuswahlArt.UNBEKANNT &&
        (ausgewählt.art == RechnerOperatorAuswahlArt.FORMEL || ausgewählt.id != aktuelleId)

    fun bestätige() {
        val eintrag = ausgewählt ?: return
        if (!kannBestätigen) return
        when (eintrag.art) {
            RechnerOperatorAuswahlArt.OPERATOR -> operatorÜbernehmen(eintrag)
            RechnerOperatorAuswahlArt.FORMEL -> formelÖffnen(eintrag)
            RechnerOperatorAuswahlArt.UNBEKANNT -> Unit
        }
    }

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
                .onPreviewKeyEvent { ereignis ->
                    when {
                        ereignis.type != KeyEventType.KeyDown -> false
                        ereignis.key == Key.Escape -> {
                            schließen()
                            true
                        }
                        ereignis.isCtrlPressed && ereignis.key == Key.F -> {
                            sucheFokus.requestFocus()
                            true
                        }
                        ereignis.isAltPressed && ereignis.key == Key.Enter -> {
                            bestätige()
                            true
                        }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val breit = maxWidth >= 980.dp
            Surface(
                modifier = Modifier
                    .fillMaxWidth(.96f)
                    .fillMaxHeight(.94f)
                    .widthIn(max = 1440.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
            ) {
                Column(Modifier.fillMaxSize()) {
                    DialogKopf(familienTitel, schließen)
                    HorizontalDivider()

                    if (breit) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            KategorienSpalte(kategorien, kategorie, { kategorie = it }, Modifier.width(210.dp))
                            AuswahlBereich(
                                suchtext = suchtext,
                                suchtextÄndern = { suchtext = it },
                                sucheFokus = sucheFokus,
                                einträge = sichtbar,
                                auswahlId = auswahlId,
                                auswählen = { auswahlId = it.id },
                                leeren = {
                                    suchtext = ""
                                    kategorie = null
                                },
                                modifier = Modifier.weight(1f),
                            )
                            OperatorDetails(
                                eintrag = ausgewählt,
                                auswirkung = auswirkung,
                                modifier = Modifier.widthIn(min = 300.dp, max = 390.dp).fillMaxHeight(),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            KategorienZeile(kategorien, kategorie, { kategorie = it })
                            AuswahlBereich(
                                suchtext = suchtext,
                                suchtextÄndern = { suchtext = it },
                                sucheFokus = sucheFokus,
                                einträge = sichtbar,
                                auswahlId = auswahlId,
                                auswählen = { auswahlId = it.id },
                                leeren = {
                                    suchtext = ""
                                    kategorie = null
                                },
                                modifier = Modifier.weight(1f),
                            )
                            OperatorDetails(
                                eintrag = ausgewählt,
                                auswirkung = auswirkung,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp),
                            )
                        }
                    }

                    HorizontalDivider()
                    DialogAktionen(
                        eintrag = ausgewählt,
                        aktuelleId = aktuelleId,
                        auswirkung = auswirkung,
                        kannBestätigen = kannBestätigen,
                        abbrechen = schließen,
                        bestätigen = ::bestätige,
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) { sucheFokus.requestFocus() }
}

@Composable
private fun DialogKopf(familienTitel: String, schließen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Operator auswählen", style = MaterialTheme.typography.headlineSmall)
            Text(
                familienTitel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = schließen) { Text("Schließen") }
    }
}

@Composable
private fun KategorienSpalte(
    kategorien: List<String>,
    ausgewählt: String?,
    auswählen: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            FilterChip(
                selected = ausgewählt == null,
                onClick = { auswählen(null) },
                label = { Text("Alle") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(kategorien, key = { it }) { kategorie ->
            FilterChip(
                selected = ausgewählt == kategorie,
                onClick = { auswählen(kategorie) },
                label = { Text(kategorie) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun KategorienZeile(
    kategorien: List<String>,
    ausgewählt: String?,
    auswählen: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = ausgewählt == null,
                onClick = { auswählen(null) },
                label = { Text("Alle") },
            )
        }
        items(kategorien, key = { it }) { kategorie ->
            FilterChip(
                selected = ausgewählt == kategorie,
                onClick = { auswählen(kategorie) },
                label = { Text(kategorie) },
            )
        }
    }
}

@Composable
private fun AuswahlBereich(
    suchtext: String,
    suchtextÄndern: (String) -> Unit,
    sucheFokus: FocusRequester,
    einträge: List<RechnerOperatorAuswahlEintrag>,
    auswahlId: String?,
    auswählen: (RechnerOperatorAuswahlEintrag) -> Unit,
    leeren: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = suchtext,
            onValueChange = suchtextÄndern,
            label = { Text("Operator suchen") },
            supportingText = { Text("Titel, Symbol, ID, Kategorie, Synonym oder Anschlussrolle") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(sucheFokus),
        )
        if (einträge.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Keine passenden Operatoren", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = leeren) { Text("Suche und Kategorie zurücksetzen") }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(210.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(einträge, key = { it.id }) { eintrag ->
                    OperatorKachel(
                        eintrag = eintrag,
                        ausgewählt = eintrag.id == auswahlId,
                        auswählen = { auswählen(eintrag) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OperatorKachel(
    eintrag: RechnerOperatorAuswahlEintrag,
    ausgewählt: Boolean,
    auswählen: () -> Unit,
) {
    val farbe = if (eintrag.art == RechnerOperatorAuswahlArt.FORMEL) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    OutlinedCard(
        onClick = auswählen,
        modifier = Modifier.fillMaxWidth().semantics { selected = ausgewählt },
        border = BorderStroke(
            if (ausgewählt) 2.dp else 1.dp,
            if (ausgewählt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.outlinedCardColors(containerColor = farbe),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    eintrag.titel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (ausgewählt) FontWeight.Bold else FontWeight.Medium,
                )
                if (ausgewählt) Text("Ausgewählt", style = MaterialTheme.typography.labelSmall)
            }
            LatexText(eintrag.symbolLatex, style = MaterialTheme.typography.titleMedium)
            Text(
                eintrag.kategorie,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            eintrag.status?.let { status ->
                Text(status, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun OperatorDetails(
    eintrag: RechnerOperatorAuswahlEintrag?,
    auswirkung: KnotenErsetzungsAuswirkung?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        if (eintrag == null) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Wähle einen Operator für die Detailansicht.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(eintrag.titel, style = MaterialTheme.typography.titleLarge)
                    LatexText(eintrag.symbolLatex, style = MaterialTheme.typography.titleMedium)
                    Text(eintrag.beschreibung, style = MaterialTheme.typography.bodyMedium)
                }
                if (eintrag.art == RechnerOperatorAuswahlArt.FORMEL) {
                    item {
                        Text(
                            "Der passende CAS-Formelbauer öffnet sich als nächster Schritt. Erst dessen Übernehmen verändert den Knoten.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else if (eintrag.art == RechnerOperatorAuswahlArt.UNBEKANNT) {
                    item {
                        Text(
                            "Dieser gespeicherte Zustand ist in der aktuellen Operatorquelle nicht registriert. Wähle einen gültigen Ersatz; das Öffnen des Dialogs verändert den Knoten nicht.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    item {
                        SignaturZeile("Eingänge", eintrag.eingänge)
                        SignaturZeile("Ausgänge", eintrag.ausgänge)
                    }
                    auswirkung?.let { wirkung ->
                        item {
                            HorizontalDivider()
                            Text("Auswirkungen", style = MaterialTheme.typography.titleSmall)
                            Text("Erhalten: ${wirkung.erhalteneAnschlüsse.namenOderKeine()}")
                            Text("Neu: ${wirkung.hinzugefügteAnschlüsse.namenOderKeine()}")
                            Text("Entfallen: ${wirkung.entfallendeAnschlüsse.namenOderKeine()}")
                            if (wirkung.trenntVerbindungen) {
                                Text(
                                    "${wirkung.entfallendeVerbindungen.size} bestehende Verbindung(en) werden getrennt.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Text("Keine bestehende Verbindung wird getrennt.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignaturZeile(titel: String, anschlüsse: List<de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten>) {
    Text(titel, style = MaterialTheme.typography.labelLarge)
    Text(
        anschlüsse.signaturText().ifBlank { "keine" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun List<de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten>.namenOderKeine(): String =
    joinToString { it.name }.ifBlank { "keine" }

@Composable
private fun DialogAktionen(
    eintrag: RechnerOperatorAuswahlEintrag?,
    aktuelleId: String?,
    auswirkung: KnotenErsetzungsAuswirkung?,
    kannBestätigen: Boolean,
    abbrechen: () -> Unit,
    bestätigen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = abbrechen) { Text("Abbrechen") }
        Button(onClick = bestätigen, enabled = kannBestätigen) {
            Text(
                when {
                    eintrag?.art == RechnerOperatorAuswahlArt.FORMEL && eintrag.id == aktuelleId -> "Formel bearbeiten"
                    eintrag?.art == RechnerOperatorAuswahlArt.FORMEL -> "Formel erstellen"
                    auswirkung?.trenntVerbindungen == true ->
                        "Übernehmen und ${auswirkung.entfallendeVerbindungen.size} Verbindung(en) trennen"
                    else -> "Übernehmen"
                },
            )
        }
    }
}

private fun List<de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten>.signaturText(): String =
    joinToString { anschluss -> "${anschluss.name}: ${anschluss.art.wert}" }
