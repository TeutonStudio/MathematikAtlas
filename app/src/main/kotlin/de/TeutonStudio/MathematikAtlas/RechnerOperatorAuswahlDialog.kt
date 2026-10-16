package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAktionen
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAbstände
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialog

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val eintragIds = einträge.map(RechnerOperatorAuswahlEintrag::id)
    var suchtext by remember(eintragIds) { mutableStateOf("") }
    var kategorie by remember(eintragIds) { mutableStateOf<String?>(null) }
    var auswahlId by remember(eintragIds, aktuelleId) {
        mutableStateOf(einträge.firstOrNull { it.id == aktuelleId }?.id ?: einträge.firstOrNull()?.id)
    }
    val kategorien = remember(einträge) { einträge.map { it.kategorie }.distinct() }
    val sichtbar = remember(einträge, suchtext, kategorie) {
        filtereRechnerOperatoren(einträge, suchtext, kategorie)
    }
    val ausgewählt = einträge.firstOrNull { it.id == auswahlId }
    val auswirkung = ausgewählt?.let(auswirkungFür)
    val sucheFokus = remember { FocusRequester() }
    val bestätigungsAktion = bestätigungsAktionFür(ausgewählt, aktuelleId)
    val kannBestätigen = bestätigungsAktion != RechnerOperatorBestätigungsAktion.KEINE

    fun bestätige() {
        val eintrag = ausgewählt ?: return
        when (bestätigungsAktion) {
            RechnerOperatorBestätigungsAktion.FORMEL_BAUEN -> formelÖffnen(eintrag)
            RechnerOperatorBestätigungsAktion.KNOTEN_ERSETZEN -> operatorÜbernehmen(eintrag)
            RechnerOperatorBestätigungsAktion.KEINE -> Unit
        }
    }

    AtlasDialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()


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

            Column(Modifier.fillMaxSize()) {
                DialogKopf(familienTitel, schließen)
                HorizontalDivider()

                if (breit) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(AtlasDialogAbstände.inhalt),
                        horizontalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.eintragHorizontal),
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
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(AtlasDialogAbstände.eintragHorizontal),
                        verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.felder),
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

    LaunchedEffect(Unit) { sucheFokus.requestFocus() }
}

@Composable
private fun DialogKopf(familienTitel: String, schließen: () -> Unit) {
    AtlasDialogKopf(
        titel = { Text("Operator auswählen") },
        untertitel = { Text(
                familienTitel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) },
        aktionen = { TextButton(onClick = schließen) { Text("Schließen") } },
    )
}

@Composable
private fun KategorienSpalte(
    kategorien: List<String>,
    ausgewählt: String?,
    auswählen: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AtlasDialogAbstände.eintragHorizontal, vertical = AtlasDialogAbstände.eintragVertikal),
        verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.aktionen),
    ) {
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
    LazyRow(
        contentPadding = PaddingValues(horizontal = AtlasDialogAbstände.eintragHorizontal, vertical = AtlasDialogAbstände.eintragVertikal),
        horizontalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung),
    ) {
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.felder)) {
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
                modifier = Modifier.fillMaxSize().testTag("rechner-operator-liste"),
                contentPadding = PaddingValues(horizontal = AtlasDialogAbstände.eintragHorizontal, vertical = AtlasDialogAbstände.eintragVertikal),
                horizontalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung),
                verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung),
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
        modifier = Modifier.fillMaxWidth().testTag("rechner-operator:${eintrag.id}").semantics { selected = ausgewählt },
        border = BorderStroke(
            if (ausgewählt) 2.dp else 1.dp,
            if (ausgewählt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.outlinedCardColors(containerColor = farbe),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AtlasDialogAbstände.eintragHorizontal, vertical = AtlasDialogAbstände.felder),
            verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.aktionen),
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
            Box(Modifier.fillMaxSize().padding(AtlasDialogAbstände.eintragHorizontal), contentAlignment = Alignment.Center) {
                Text("Wähle einen Operator für die Detailansicht.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(AtlasDialogAbstände.eintragHorizontal),
                verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung),
            ) {
                item {
                    Text(eintrag.titel, style = MaterialTheme.typography.titleLarge)
                    LatexText(eintrag.symbolLatex, style = MaterialTheme.typography.titleMedium)
                    Text(eintrag.beschreibung, style = MaterialTheme.typography.bodyMedium)
                    eintrag.definitionLatex?.let { LatexText(it, style = MaterialTheme.typography.bodyMedium) }
                }
                if (eintrag.art == RechnerOperatorAuswahlArt.FORMEL) {
                    item {
                        if (eintrag.kandidat == null) {
                            Text(
                                "Der passende CAS-Formelbauer öffnet sich als nächster Schritt. Erst die anschließende Bestätigung in diesem Dialog verändert den Knoten.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text(
                                "Die Formel ist vollständig. Prüfe vor dem Übernehmen ihre Signatur und Auswirkungen.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            SignaturZeile("Eingänge", eintrag.eingänge)
                            SignaturZeile("Ausgänge", eintrag.ausgänge)
                            auswirkung?.let { AuswirkungsDetails(it) }
                        }
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
                        item { AuswirkungsDetails(wirkung) }
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
private fun AuswirkungsDetails(wirkung: KnotenErsetzungsAuswirkung) {
    HorizontalDivider()
    Text("Auswirkungen", style = MaterialTheme.typography.titleSmall)
    Text("Erhalten: ${wirkung.erhalteneAnschlüsse.namenOderKeine()}")
    Text("Neu: ${wirkung.hinzugefügteAnschlüsse.namenOderKeine()}")
    Text("Entfallen: ${wirkung.entfallendeAnschlüsse.namenOderKeine()}")
    if (wirkung.trenntVerbindungen) {
        Text(
            "${wirkung.entfallendeVerbindungen.size} Verbindung(en) und ${wirkung.entfallendeLokaleWerte.size} lokale Operanden (${wirkung.entfallendeLokaleWerte.joinToString()}) entfallen.",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
    } else {
        Text("Keine bestehende Verbindung wird getrennt.")
    }
}

@Composable
private fun DialogAktionen(
    eintrag: RechnerOperatorAuswahlEintrag?,
    aktuelleId: String?,
    auswirkung: KnotenErsetzungsAuswirkung?,
    kannBestätigen: Boolean,
    abbrechen: () -> Unit,
    bestätigen: () -> Unit,
) {
    AtlasDialogAktionen {
        TextButton(onClick = abbrechen) { Text("Abbrechen") }
        Button(onClick = bestätigen, enabled = kannBestätigen) {
            Text(
                when {
                    eintrag?.art == RechnerOperatorAuswahlArt.FORMEL && eintrag.kandidat == null &&
                        eintrag.id == aktuelleId -> "Formel bearbeiten"
                    eintrag?.art == RechnerOperatorAuswahlArt.FORMEL && eintrag.kandidat == null -> "Formel erstellen"
                    auswirkung?.trenntVerbindungen == true ->
                        "Übernehmen und entfallende Operanden entfernen"
                    eintrag?.art == RechnerOperatorAuswahlArt.FORMEL -> "Formel übernehmen"
                    else -> "Übernehmen"
                },
            )
        }
    }
}

private fun List<de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten>.signaturText(): String =
    joinToString { anschluss -> "${anschluss.name}: ${anschluss.art.wert}" }
