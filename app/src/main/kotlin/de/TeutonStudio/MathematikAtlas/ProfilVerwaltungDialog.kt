package de.TeutonStudio.MathematikAtlas

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikAtlas.speicher.*
import java.text.DateFormat
import java.util.Date

private enum class ProfilReiter(val titel: String) {
    Profil("Profil"),
    Löschen("Löschverwaltung"),
    Papierkorb("Papierkorb"),
}

@Composable
internal fun ProfilVerwaltungDialog(
    zustand: AtlasZustand,
    schließen: () -> Unit,
    einstellungenÖffnen: () -> Unit,
    profilGeändert: (LokalesProfil) -> Unit,
) {
    val context = LocalContext.current
    val profilSpeicher = remember(context) { LokalesProfilSpeicher(context) }
    val ordnungsSpeicher = remember(context) { KartenOrdnungSpeicher(context) }
    var profil by remember { mutableStateOf(profilSpeicher.lade()) }
    var pseudonym by remember(profil.id) { mutableStateOf(profil.pseudonym) }
    var lieblingsFarbe by remember(profil.id) { mutableStateOf(profil.lieblingsFarbe) }
    var reiter by remember { mutableStateOf(ProfilReiter.Profil) }
    var ordnung by remember { mutableStateOf(ordnungsSpeicher.lade()) }
    var papierkorb by remember { mutableStateOf(zustand.speicher.papierkorbEinträge()) }
    var inPapierkorbVerschieben by remember { mutableStateOf<PapierkorbEintrag?>(null) }
    var wiederherstellen by remember { mutableStateOf<PapierkorbEintrag?>(null) }
    var endgültigLöschen by remember { mutableStateOf<PapierkorbEintrag?>(null) }
    var meldung by remember { mutableStateOf<String?>(null) }

    val papierkorbIds = remember(papierkorb) { papierkorb.flatMapTo(mutableSetOf()) { it.kartenIds } }
    val alleKarten = remember(zustand.karten, papierkorb) {
        zustand.speicher.liste(archivierteEinschließen = true).filter { it.id !in papierkorbIds }
    }

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.9f).fillMaxHeight(.9f).widthIn(max = 980.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Profil und Verwaltung", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Lokale Identität, exakte Profilfarbe, Papierkorb und endgültige Löschung",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }
                TabRow(selectedTabIndex = reiter.ordinal) {
                    ProfilReiter.entries.forEach { eintrag ->
                        Tab(
                            selected = reiter == eintrag,
                            onClick = { reiter = eintrag },
                            text = { Text(eintrag.titel) },
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp)) {
                    when (reiter) {
                        ProfilReiter.Profil -> ProfilInhalt(
                            profil = profil,
                            pseudonym = pseudonym,
                            pseudonymÄndern = { pseudonym = it },
                            lieblingsFarbe = lieblingsFarbe,
                            lieblingsFarbeÄndern = { lieblingsFarbe = it },
                            speichern = {
                                profil = profilSpeicher.speichere(
                                    profil.copy(pseudonym = pseudonym.trim(), lieblingsFarbe = lieblingsFarbe),
                                )
                                pseudonym = profil.pseudonym
                                lieblingsFarbe = profil.lieblingsFarbe
                                profilGeändert(profil)
                                meldung = "Pseudonym und Profilfarbe wurden gespeichert."
                            },
                            einstellungenÖffnen = einstellungenÖffnen,
                        )
                        ProfilReiter.Löschen -> LöschverwaltungInhalt(
                            karten = alleKarten,
                            ordnung = ordnung,
                            karteInPapierkorb = { karte ->
                                val pfad = ordnung.ordnerFür(karte.id)
                                inPapierkorbVerschieben = PapierkorbEintrag(
                                    art = PapierkorbArt.Karte,
                                    name = karte.name,
                                    ursprünglicherPfad = pfad,
                                    kartenPfade = mapOf(karte.id to pfad),
                                )
                            },
                            ordnerInPapierkorb = { pfad ->
                                val kartenPfade = ordnung.kartenUnter(pfad).associateWith(ordnung::ordnerFür)
                                inPapierkorbVerschieben = PapierkorbEintrag(
                                    art = PapierkorbArt.Ordner,
                                    name = pfad.lastOrNull() ?: "Sammlung",
                                    ursprünglicherPfad = pfad,
                                    kartenPfade = kartenPfade,
                                    ordnerPfade = ordnung.ordnerUnter(pfad),
                                )
                            },
                        )
                        ProfilReiter.Papierkorb -> PapierkorbInhalt(
                            einträge = papierkorb,
                            wiederherstellen = { wiederherstellen = it },
                            endgültigLöschen = { endgültigLöschen = it },
                        )
                    }
                }
                meldung?.let { text ->
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { meldung = null }) { Text("Ausblenden") }
                    }
                }
            }
        }
    }

    inPapierkorbVerschieben?.let { eintrag ->
        AlertDialog(
            onDismissRequest = { inPapierkorbVerschieben = null },
            title = { Text("In den Papierkorb verschieben?") },
            text = {
                Text(
                    if (eintrag.art == PapierkorbArt.Ordner) {
                        "Die Sammlung „${eintrag.name}“ mit ${eintrag.kartenIds.size} Karten wird aus der Bibliothek entfernt. Bestehende Kartenverweise bleiben bis zur endgültigen Löschung funktionsfähig."
                    } else {
                        "Die Karte „${eintrag.name}“ wird aus der Bibliothek entfernt. Bestehende Kartenverweise bleiben bis zur endgültigen Löschung funktionsfähig."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val aktiveIds = zustand.karten.mapTo(mutableSetOf()) { it.id }
                    if (aktiveIds.isNotEmpty() && aktiveIds.all { it in eintrag.kartenIds }) zustand.neueKarte()
                    zustand.speicher.legeInPapierkorb(eintrag)
                    ordnung = if (eintrag.art == PapierkorbArt.Ordner) {
                        ordnung.ohneOrdnerBaum(eintrag.ursprünglicherPfad)
                    } else {
                        ordnung.ohneKarten(eintrag.kartenIds)
                    }
                    ordnungsSpeicher.speichere(ordnung)
                    papierkorb = zustand.speicher.papierkorbEinträge()
                    inPapierkorbVerschieben = null
                    context.findActivity()?.recreate()
                }) { Text("In Papierkorb") }
            },
            dismissButton = { TextButton(onClick = { inPapierkorbVerschieben = null }) { Text("Abbrechen") } },
        )
    }

    wiederherstellen?.let { eintrag ->
        AlertDialog(
            onDismissRequest = { wiederherstellen = null },
            title = { Text("Wiederherstellen?") },
            text = { Text("„${eintrag.name}“ wird an seinem ursprünglichen Bibliothekspfad wiederhergestellt.") },
            confirmButton = {
                TextButton(onClick = {
                    ordnung = ordnung.mitOrdnern(eintrag.ordnerPfade).mitKartenInOrdnern(eintrag.kartenPfade)
                    ordnungsSpeicher.speichere(ordnung)
                    zustand.speicher.entfernePapierkorbEintrag(eintrag.id)
                    papierkorb = zustand.speicher.papierkorbEinträge()
                    wiederherstellen = null
                    context.findActivity()?.recreate()
                }) { Text("Wiederherstellen") }
            },
            dismissButton = { TextButton(onClick = { wiederherstellen = null }) { Text("Abbrechen") } },
        )
    }

    endgültigLöschen?.let { eintrag ->
        val blockierend = remember(eintrag) { zustand.speicher.blockierendeVerwendungen(eintrag.kartenIds) }
        AlertDialog(
            onDismissRequest = { endgültigLöschen = null },
            title = { Text("Endgültig löschen?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("„${eintrag.name}“ kann danach nicht wiederhergestellt werden.")
                    if (blockierend.isNotEmpty()) {
                        Text(
                            "Die Löschung ist blockiert, weil ${blockierend.size} gespeicherte Kartenversionen Inhalte aus dieser Auswahl verwenden.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        blockierend.take(5).forEach { verwendung ->
                            Text(
                                "• ${verwendung.verwendendeKarte.name} v${verwendung.verwendendeKarte.version} → " +
                                    "${verwendung.verwendeterVerweis.kartenId} v${verwendung.verwendeterVerweis.version}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = blockierend.isEmpty(),
                    onClick = {
                        val fehler = zustand.speicher.löscheEndgültig(eintrag.kartenIds)
                        if (fehler.isEmpty()) {
                            zustand.speicher.entfernePapierkorbEintrag(eintrag.id)
                            papierkorb = zustand.speicher.papierkorbEinträge()
                            endgültigLöschen = null
                            context.findActivity()?.recreate()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Endgültig löschen") }
            },
            dismissButton = { TextButton(onClick = { endgültigLöschen = null }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun ProfilInhalt(
    profil: LokalesProfil,
    pseudonym: String,
    pseudonymÄndern: (String) -> Unit,
    lieblingsFarbe: ProfilFarbe,
    lieblingsFarbeÄndern: (ProfilFarbe) -> Unit,
    speichern: () -> Unit,
    einstellungenÖffnen: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().widthIn(max = 720.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Lokale Identität", style = MaterialTheme.typography.titleLarge)
        Text(
            "Pseudonym und Lieblingsfarbe werden lokal gespeichert. Die Farbe bestimmt dekorative Material-Farbrollen; semantische Fehlerfarben bleiben davon unabhängig.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = pseudonym,
            onValueChange = { pseudonymÄndern(it.take(40)) },
            label = { Text("Pseudonym") },
            supportingText = { Text("Maximal 40 Zeichen") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ProfilFarbAuswahl(
            startFarbe = lieblingsFarbe,
            farbeGeaendert = lieblingsFarbeÄndern,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Lokale Profil-ID: ${profil.id.wert}", style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = speichern,
            enabled = pseudonym.isNotBlank(),
        ) { Text("Profil speichern") }
        HorizontalDivider()
        Text("Anwendung", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = einstellungenÖffnen) { Text("Einstellungen öffnen") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LöschverwaltungInhalt(
    karten: List<KartenDaten>,
    ordnung: KartenOrdnung,
    karteInPapierkorb: (KartenDaten) -> Unit,
    ordnerInPapierkorb: (List<String>) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item { Text("Sammlungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (ordnung.ordner.isEmpty()) {
            item { Text("Keine Sammlungen vorhanden.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(
            ordnung.ordner.sortedWith(compareBy({ it.size }, { formatiereOrdnerPfad(it).lowercase() })),
            key = { formatiereOrdnerPfad(it) },
        ) { pfad ->
            ListItem(
                headlineContent = { Text(formatiereOrdnerPfad(pfad)) },
                supportingContent = { Text("${ordnung.kartenUnter(pfad).size} Karten einschließlich Unterordnern") },
                trailingContent = { TextButton(onClick = { ordnerInPapierkorb(pfad) }) { Text("Löschen") } },
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
            Text("Karten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(karten, key = { it.id.wert }) { karte ->
            ListItem(
                headlineContent = { Text(karte.name) },
                supportingContent = {
                    Text(buildString {
                        append("Version ${karte.version}")
                        if (karte.archiviert) append(" · archiviert")
                        val pfad = ordnung.ordnerFür(karte.id)
                        if (pfad.isNotEmpty()) append(" · ${formatiereOrdnerPfad(pfad)}")
                    })
                },
                trailingContent = { TextButton(onClick = { karteInPapierkorb(karte) }) { Text("Löschen") } },
            )
        }
    }
}

@Composable
private fun PapierkorbInhalt(
    einträge: List<PapierkorbEintrag>,
    wiederherstellen: (PapierkorbEintrag) -> Unit,
    endgültigLöschen: (PapierkorbEintrag) -> Unit,
) {
    if (einträge.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Der Papierkorb ist leer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(einträge, key = PapierkorbEintrag::id) { eintrag ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(eintrag.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append(if (eintrag.art == PapierkorbArt.Ordner) "Sammlung" else "Karte")
                            append(" · ${eintrag.kartenIds.size} Karten")
                            append(" · ${DateFormat.getDateTimeInstance().format(Date(eintrag.gelöschtAm))}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { wiederherstellen(eintrag) }) { Text("Wiederherstellen") }
                        TextButton(
                            onClick = { endgültigLöschen(eintrag) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("Endgültig löschen") }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
