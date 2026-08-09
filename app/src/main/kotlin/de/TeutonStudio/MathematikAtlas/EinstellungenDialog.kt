package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun EinstellungenDialogV2291(
    zustand: AtlasZustand,
    schließen: () -> Unit,
) {
    val navigation = remember { baueEinstellungsNavigation(standardEinstellungsSeiten) }
    var ausgewählteSeite by remember { mutableStateOf(EinstellungsSeiteId.Darstellung) }
    var ausgeklappteOrdner by remember { mutableStateOf(navigation.alleOrdnerPfade()) }
    var kompakterOrdnerStapel by remember {
        mutableStateOf<List<EinstellungsNavigationsElement.Ordner>>(emptyList())
    }
    var kompakteSeite by remember { mutableStateOf<EinstellungsSeiteId?>(null) }

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.96f)
                    .fillMaxHeight(0.94f)
                    .widthIn(max = 1180.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val breit = maxWidth >= 720.dp
                    Column(Modifier.fillMaxSize()) {
                        EinstellungenKopf(
                            kompakt = !breit,
                            kannZurück = !breit && (kompakteSeite != null || kompakterOrdnerStapel.isNotEmpty()),
                            zurück = {
                                if (kompakteSeite != null) {
                                    kompakteSeite = null
                                } else if (kompakterOrdnerStapel.isNotEmpty()) {
                                    kompakterOrdnerStapel = kompakterOrdnerStapel.dropLast(1)
                                }
                            },
                            schließen = schließen,
                        )
                        HorizontalDivider()

                        if (breit) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                EinstellungenNavigationBreit(
                                    navigation = navigation,
                                    ausgewählteSeite = ausgewählteSeite,
                                    ausgeklappteOrdner = ausgeklappteOrdner,
                                    seiteWählen = { ausgewählteSeite = it },
                                    ordnerUmschalten = { pfad ->
                                        ausgeklappteOrdner = if (pfad in ausgeklappteOrdner) {
                                            ausgeklappteOrdner - pfad
                                        } else {
                                            ausgeklappteOrdner + pfad
                                        }
                                    },
                                    modifier = Modifier.widthIn(min = 230.dp, max = 320.dp).fillMaxHeight(),
                                )
                                VerticalDivider()
                                EinstellungsSeiteInhalt(
                                    seite = ausgewählteSeite,
                                    zustand = zustand,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        } else {
                            val aktuelleEbene = kompakterOrdnerStapel.lastOrNull()?.inhalt ?: navigation
                            if (kompakteSeite != null) {
                                EinstellungsSeiteInhalt(
                                    seite = requireNotNull(kompakteSeite),
                                    zustand = zustand,
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                )
                            } else {
                                EinstellungenNavigationKompakt(
                                    ebene = aktuelleEbene,
                                    ordnerTitel = kompakterOrdnerStapel.lastOrNull()?.titel,
                                    seiteWählen = { seite ->
                                        ausgewählteSeite = seite
                                        kompakteSeite = seite
                                    },
                                    ordnerÖffnen = { ordner ->
                                        kompakterOrdnerStapel = kompakterOrdnerStapel + ordner
                                    },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                )
                            }
                        }

                        HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = schließen) { Text("Fertig") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EinstellungenKopf(
    kompakt: Boolean,
    kannZurück: Boolean,
    zurück: () -> Unit,
    schließen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (kompakt && kannZurück) {
            TextButton(onClick = zurück) { Text("‹ Zurück") }
        }
        Column(Modifier.weight(1f)) {
            Text("Einstellungen", style = MaterialTheme.typography.headlineSmall)
            if (!kompakt) {
                Text(
                    "Darstellung, Karten und Anwendungsinformationen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = schließen) { Text("Schließen") }
    }
}

@Composable
private fun EinstellungenNavigationBreit(
    navigation: EinstellungsNavigationsebene,
    ausgewählteSeite: EinstellungsSeiteId,
    ausgeklappteOrdner: Set<List<String>>,
    seiteWählen: (EinstellungsSeiteId) -> Unit,
    ordnerUmschalten: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        EinstellungenNavigationEbeneBreit(
            ebene = navigation,
            tiefe = 0,
            ausgewählteSeite = ausgewählteSeite,
            ausgeklappteOrdner = ausgeklappteOrdner,
            seiteWählen = seiteWählen,
            ordnerUmschalten = ordnerUmschalten,
        )
    }
}

@Composable
private fun EinstellungenNavigationEbeneBreit(
    ebene: EinstellungsNavigationsebene,
    tiefe: Int,
    ausgewählteSeite: EinstellungsSeiteId,
    ausgeklappteOrdner: Set<List<String>>,
    seiteWählen: (EinstellungsSeiteId) -> Unit,
    ordnerUmschalten: (List<String>) -> Unit,
) {
    ebene.elemente.forEach { element ->
        when (element) {
            is EinstellungsNavigationsElement.Seite -> {
                val ausgewählt = element.definition.id == ausgewählteSeite
                ListItem(
                    headlineContent = { Text(element.definition.titel) },
                    modifier = Modifier.padding(start = (tiefe * 12).dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { seiteWählen(element.definition.id) }
                        .background(
                            if (ausgewählt) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface,
                        ),
                )
            }
            is EinstellungsNavigationsElement.Ordner -> {
                val geöffnet = element.pfad in ausgeklappteOrdner
                ListItem(
                    headlineContent = { Text("${if (geöffnet) "▾" else "▸"} ${element.titel}") },
                    modifier = Modifier.padding(start = (tiefe * 12).dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { ordnerUmschalten(element.pfad) },
                )
                if (geöffnet) {
                    EinstellungenNavigationEbeneBreit(
                        ebene = element.inhalt,
                        tiefe = tiefe + 1,
                        ausgewählteSeite = ausgewählteSeite,
                        ausgeklappteOrdner = ausgeklappteOrdner,
                        seiteWählen = seiteWählen,
                        ordnerUmschalten = ordnerUmschalten,
                    )
                }
            }
        }
    }
}

@Composable
private fun EinstellungenNavigationKompakt(
    ebene: EinstellungsNavigationsebene,
    ordnerTitel: String?,
    seiteWählen: (EinstellungsSeiteId) -> Unit,
    ordnerÖffnen: (EinstellungsNavigationsElement.Ordner) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (ordnerTitel != null) {
            Text(ordnerTitel, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
        }
        ebene.elemente.forEach { element ->
            when (element) {
                is EinstellungsNavigationsElement.Seite -> ListItem(
                    headlineContent = { Text(element.definition.titel) },
                    trailingContent = { Text("›", style = MaterialTheme.typography.titleLarge) },
                    modifier = Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { seiteWählen(element.definition.id) },
                )
                is EinstellungsNavigationsElement.Ordner -> ListItem(
                    headlineContent = { Text(element.titel) },
                    supportingContent = { Text("Kategorie") },
                    trailingContent = { Text("›", style = MaterialTheme.typography.titleLarge) },
                    modifier = Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { ordnerÖffnen(element) },
                )
            }
        }
    }
}

@Composable
private fun EinstellungsSeiteInhalt(
    seite: EinstellungsSeiteId,
    zustand: AtlasZustand,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (seite) {
            EinstellungsSeiteId.Darstellung -> DarstellungEinstellungsSeite()
            EinstellungsSeiteId.Beispielkarten -> BeispielkartenEinstellungsSeite(zustand)
            EinstellungsSeiteId.Ueber -> UeberEinstellungsSeite()
        }
    }
}

@Composable
private fun DarstellungEinstellungsSeite() {
    val darstellung = LocalDarstellungsSteuerung.current
    Text("Darstellung", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Wähle, ob die App dem System folgt oder dauerhaft hell beziehungsweise dunkel dargestellt wird.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    DarstellungsModus.entries.forEach { modus ->
        val ausgewählt = darstellung.modus == modus
        Surface(
            modifier = Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { darstellung.ändereModus(modus) },
            shape = MaterialTheme.shapes.medium,
            color = if (ausgewählt) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
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
                        modus.einstellungsBeschreibung(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BeispielkartenEinstellungsSeite(zustand: AtlasZustand) {
    val context = LocalContext.current
    val verwaltung = remember(context, zustand.speicher) {
        erstelleBeispielKartenVerwaltung(context, zustand.speicher)
    }
    val scope = rememberCoroutineScope()
    var läuft by remember { mutableStateOf(false) }
    var meldung by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    Text("Beispielkarten", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Erstellt einen neuen Satz der mitgelieferten Beispielkarten. Vorhandene Karten und frühere Beispielsätze bleiben unverändert.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Neue Sätze werden unter „Beispiel Karten“, „Beispiel Karten 2“, … gespeichert.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                enabled = !läuft,
                onClick = {
                    läuft = true
                    meldung = null
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { verwaltung.erstelleNeu() }
                        }.fold(
                            onSuccess = { ergebnis ->
                                zustand.ladeKartenNeu()
                                KartenAenderungsSignal.markiereAenderung()
                                val pfad = ergebnis.ordnerPfad.joinToString("/")
                                meldung = "${ergebnis.anzahl} Beispielkarten wurden in „$pfad“ erstellt." to false
                            },
                            onFailure = { fehler ->
                                zustand.ladeKartenNeu()
                                val grund = fehler.message ?: fehler::class.simpleName ?: "Unbekannter Fehler"
                                meldung = (
                                    "Die Beispielkarten konnten nicht vollständig erstellt werden. " +
                                        "Es wurden keine unvollständigen Beispielkarten beibehalten. $grund"
                                    ) to true
                            },
                        )
                        läuft = false
                    }
                },
            ) {
                Text(if (läuft) "Beispielkarten werden erstellt …" else "Beispielkarten neu erstellen")
            }
        }
    }

    meldung?.let { (text, istFehler) ->
        Text(
            text = text,
            color = if (istFehler) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun UeberEinstellungsSeite() {
    val buildInformation = remember { aktuelleAppBuildInformation() }
    Text("Über", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Informationen zur installierten Anwendung und zum zugrunde liegenden Build.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider()
    Text(buildInformation.versionsZeile, style = MaterialTheme.typography.bodyLarge)
    Text(
        buildInformation.buildZeile,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun DarstellungsModus.einstellungsBeschreibung(): String = when (this) {
    DarstellungsModus.System -> "Verwendet die Darstellung des Betriebssystems."
    DarstellungsModus.Hell -> "Verwendet immer den Lightmode."
    DarstellungsModus.Dunkel -> "Verwendet immer den Darkmode."
}
