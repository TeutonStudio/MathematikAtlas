package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.SignaturtragendeMethode

internal data class DesktopMethodenEintrag(val id: String, val titel: String, val kategorie: String, val kandidat: KnotenDaten?, val fehler: String? = null, val signatur: String = "")

@Composable
internal fun DesktopMethodenQuelle(knoten: KnotenDaten, atlas: DesktopAtlasZustand) {
    if (knoten.art != METHODEN_QUELLE_ART) return
    val quellen = remember { methodenKnotenQuellen() }
    val karten = remember(atlas.editor.karte.id, atlas.editor.karte.version) { atlas.speicher.gespeicherteKarten().filter { !it.archiviert && it.id != atlas.editor.karte.id } }
    val dekodiert = remember(knoten.eigenschaften) { runCatching { methodenQuellKnoten(knoten) } }
    val quelle = dekodiert.getOrNull()
    var geöffnet by remember(knoten.id) { mutableStateOf(false) }
    var fehler by remember(knoten.id) { mutableStateOf<String?>(null) }
    val aktuell = knoten.eigenschaften.text(METHODEN_QUELLE_ID, "identitaet")
    val einträge = remember(knoten, karten) {
        listOf(DesktopMethodenEintrag("identitaet", "Identität", "Spezielle Methoden", konfiguriereMethodenQuelle(knoten), signatur = "(x) → (x)")) +
            quellen.map { q -> DesktopMethodenEintrag(q.id, q.titel, "Knoten: ${q.kategorie}", if (q.fehler == null) konfiguriereMethodenQuelle(knoten, q.knoten, quelleId = q.id) else null, q.fehler, q.signatur) } +
            karten.map { karte ->
                fun referenziertAktuelle(k: KartenDaten, besucht: MutableSet<KartenVerweis>): Boolean = k.knoten.flatMap { it.alleKartenVerweise() }.any { ref ->
                    ref.kartenId == atlas.editor.karte.id || besucht.add(ref) && atlas.speicher.lade(ref)?.let { referenziertAktuelle(it, besucht) } == true
                }
                val probe = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister(), KartenQuelle(atlas.speicher::lade)).exportiereKarteAlsMethode(karte)
                val diagnose = if (referenziertAktuelle(karte, mutableSetOf())) "Rekursiver Kartenverweis." else
                    probe.fehler
                val ref = KartenVerweis(karte.id, karte.version)
                DesktopMethodenEintrag("karte:$ref", "${karte.name} · v${karte.version}", "Gespeicherte Karten", if (diagnose == null) konfiguriereMethodenQuelle(knoten, karte = ref) else null, diagnose, methodenQuellenSignatur(probe.ausgaben["methode"]?.objekt as? SignaturtragendeMethode))
            }
    }
    OutlinedButton(onClick = { geöffnet = true }, modifier = Modifier.fillMaxWidth()) { Text("Methodenquelle: ${einträge.firstOrNull { it.id == aktuell }?.titel ?: "Unbekannte Quelle"}") }
    if (aktuell == "identitaet") LatexText("x\\mapsto x")
    val methode = atlas.auswertung.knoten[knoten.id]?.ausgaben?.get("methode")?.objekt as? SignaturtragendeMethode
    methode?.let { Text("Argumente: ${it.signatur.argumente.joinToString { c -> c.name }} · Ergebnisse: ${it.signatur.ergebnisse.joinToString { c -> c.name }}") }
    if (geöffnet) DesktopMethodenAuswahlDialog(einträge, aktuell,
        { atlas.beantrageKonfiguration(it); geöffnet = false }, { geöffnet = false })
    if (quelle != null) {
        Text("Konfiguration: ${quelle.name}", style = MaterialTheme.typography.titleSmall)
        if (quelle.anschlüsse.any { it.kannSichErweitern }) {
            var anzahl by remember(quelle) { mutableStateOf(quelle.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }.toString()) }
            val gültig = anzahl.toIntOrNull()?.let { it in 2..128 } == true
            OutlinedTextField(anzahl, { anzahl = it }, label = { Text("Anzahl Argumente") },
                isError = !gültig, supportingText = { if (!gültig) Text("Ganze Zahl zwischen 2 und 128 eingeben.") })
            TextButton(enabled = gültig, onClick = {
                runCatching { konfiguriereMethodenQuellParameter(quelle, "festeEingänge", anzahl) }
                    .onSuccess { atlas.beantrageKonfiguration(konfiguriereMethodenQuelle(knoten, it)) }.onFailure { fehler = it.message }
            }) { Text("Argumentanzahl übernehmen") }
        }
        quelle.parameter.filterKeys { it !in setOf("operator", "festeEingänge", "operatorAnzeige") }.forEach { (name, wert) ->
            var entwurf by remember(quelle, name) { mutableStateOf(wert) }
            OutlinedTextField(entwurf, { entwurf = it }, label = { Text(name) }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = {
                runCatching {
                    val neu = konfiguriereMethodenQuellParameter(quelle, name, entwurf)
                    val probe = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).exportiereKarteAlsMethode(methodenQuellKarte(neu, knoten.id))
                    require(probe.fehler == null) { probe.fehler.orEmpty() }
                    neu
                }.onSuccess { fehler = null; atlas.beantrageKonfiguration(konfiguriereMethodenQuelle(knoten, it)) }.onFailure { fehler = it.message }
            }) { Text("Übernehmen") }
        }
    }
    (dekodiert.exceptionOrNull()?.message ?: fehler)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopMethodenAuswahlDialog(einträge: List<DesktopMethodenEintrag>, aktuell: String,
    übernehmen: (KnotenDaten) -> Unit, schließen: () -> Unit, modifier: Modifier = Modifier) {
    var suche by remember { mutableStateOf("") }
    var kategorie by remember { mutableStateOf<String?>(null) }
    var auswahl by remember { mutableStateOf(aktuell) }
    AtlasDialog(onDismissRequest = schließen,
        modifier = modifier.testTag("methoden-dialog"),
        titel = { Text("Methode auswählen") },
        inhaltScrolltSelbst = true,
        aktionen = {
            TextButton(onClick = schließen, modifier = Modifier.testTag("methoden-abbrechen")) { Text("Abbrechen") }
            Button(enabled = einträge.firstOrNull { it.id == auswahl }?.kandidat != null,
                modifier = Modifier.testTag("methoden-uebernehmen"), onClick = {
                    einträge.firstOrNull { it.id == auswahl }?.kandidat?.let(übernehmen)
                }) { Text("Methode übernehmen") }
        }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val steuerungsMaximum = maxHeight * .45f
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxWidth().heightIn(max = steuerungsMaximum).verticalScroll(rememberScrollState())) {
                    OutlinedTextField(suche, { suche = it }, label = { Text("Methoden suchen") }, modifier = Modifier.fillMaxWidth())
                    FlowRow {
                        TextButton(onClick = { kategorie = null }) { Text("Alle Kategorien") }
                        var kategorienOffen by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { kategorienOffen = true }) { Text(kategorie ?: "Kategorie wählen") }
                            DropdownMenu(kategorienOffen, { kategorienOffen = false }) {
                                einträge.map { it.kategorie }.distinct().forEach { kat -> DropdownMenuItem(text = { Text(kat) }, onClick = { kategorie = kat; kategorienOffen = false }) }
                            }
                        }
                    }
                    einträge.firstOrNull { it.id == auswahl }?.let { Text(it.signatur, modifier = Modifier.fillMaxWidth()) }
                }
                val sichtbar = einträge.filter { (kategorie == null || it.kategorie == kategorie) && (it.titel.contains(suche, true) || it.kategorie.contains(suche, true)) }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("methoden-quellenliste")) {
                    if (sichtbar.isEmpty()) item { Text("Keine passenden Methoden.") }
                    items(sichtbar, key = { it.id }) { eintrag ->
                        AtlasDialogListenEintrag(
                            headlineContent = { Text(eintrag.titel) },
                            supportingContent = { Text(eintrag.fehler ?: eintrag.kategorie) },
                            modifier = Modifier.testTag("methoden-quelle:${eintrag.id}").semantics { selected = eintrag.id == auswahl }.clickable { auswahl = eintrag.id },
                            leadingContent = { RadioButton(selected = eintrag.id == auswahl, onClick = { auswahl = eintrag.id }) },
                        )
                    }
                }
            }
        }
    }
}
