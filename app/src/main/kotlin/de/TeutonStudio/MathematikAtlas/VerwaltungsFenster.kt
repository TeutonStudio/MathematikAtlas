package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
    var profilGeöffnet by remember { mutableStateOf(false) }
    var einstellungenGeöffnet by remember { mutableStateOf(false) }

    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Text(
                "Mathematik Atlas",
                Modifier.padding(18.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            PrimaryScrollableTabRow(selectedTabIndex = zustand.linkerBereich.ordinal, edgePadding = 8.dp) {
                VerwaltungsBereich.entries.forEach { bereich ->
                    Tab(
                        selected = zustand.linkerBereich == bereich,
                        onClick = { zustand.linkerBereich = bereich },
                        text = { Text(bereich.name) },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (zustand.linkerBereich) {
                    VerwaltungsBereich.Karten -> KartenListe(zustand)
                    VerwaltungsBereich.Konzepte -> KonzeptListe()
                    VerwaltungsBereich.Variablen -> VariablenListe(zustand)
                    VerwaltungsBereich.Auswertung -> AuswertungsListe(zustand)
                    VerwaltungsBereich.Fehler -> FehlerListe(zustand)
                }
            }
            HorizontalDivider()
            ProfilLeiste(onClick = { profilGeöffnet = true })
        }
    }

    if (profilGeöffnet) {
        ProfilDialog(
            schließen = { profilGeöffnet = false },
            einstellungenÖffnen = {
                profilGeöffnet = false
                einstellungenGeöffnet = true
            },
        )
    }
    if (einstellungenGeöffnet) {
        EinstellungenDialog(schließen = { einstellungenGeöffnet = false })
    }
}

@Composable
private fun ProfilLeiste(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Profil und Einstellungen öffnen" },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "P",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Lokales Profil", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Profilverwaltung folgt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfilDialog(schließen: () -> Unit, einstellungenÖffnen: () -> Unit) {
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("Profil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "P",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column {
                        Text("Lokales Profil", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Noch nicht mit einem Benutzerkonto verknüpft",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Die Profilleiste ist für die spätere Profilerstellung vorbereitet. " +
                        "Derzeit enthält sie den zentralen Zugang zu den Anwendungseinstellungen.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = einstellungenÖffnen,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Einstellungen") }
            }
        },
        confirmButton = { TextButton(onClick = schließen) { Text("Schließen") } },
    )
}

@Composable
private fun EinstellungenDialog(schließen: () -> Unit) {
    val darstellung = LocalDarstellungsSteuerung.current
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("Einstellungen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Darstellung", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Wähle, ob die App dem System folgt oder dauerhaft hell beziehungsweise dunkel dargestellt wird.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DarstellungsModus.entries.forEach { modus ->
                    val ausgewählt = darstellung.modus == modus
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { darstellung.ändereModus(modus) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (ausgewählt) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = ausgewählt,
                                onClick = { darstellung.ändereModus(modus) },
                            )
                            Column(Modifier.padding(vertical = 8.dp)) {
                                Text(modus.anzeigeName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    modus.beschreibung(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = schließen) { Text("Fertig") } },
    )
}

private fun DarstellungsModus.beschreibung(): String = when (this) {
    DarstellungsModus.System -> "Verwendet die Darstellung des Betriebssystems."
    DarstellungsModus.Hell -> "Verwendet immer den Lightmode."
    DarstellungsModus.Dunkel -> "Verwendet immer den Darkmode."
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
