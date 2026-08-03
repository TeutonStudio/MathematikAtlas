package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

@Composable
internal fun KnotenAuswahlDialog(zustand: AtlasZustand, position: GraphPunkt) {
    val context = LocalContext.current
    val modusSpeicher = remember(context) { KnotenWählerModusSpeicher(context) }
    var modus by remember(modusSpeicher) { mutableStateOf(modusSpeicher.lade()) }
    val setzeModus: (KnotenWählerModus) -> Unit = { neu ->
        modus = neu
        modusSpeicher.speichere(neu)
        if (neu == KnotenWählerModus.Konzeptbibliothek) zustand.setzeSuchText("")
    }

    when (modus) {
        KnotenWählerModus.Standard -> AlertDialog(
            onDismissRequest = zustand::schließeKnotenAuswahl,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Knoten einfügen")
                    Text(
                        modus.beschreibung,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 700.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KnotenWählerModusAuswahl(modus = modus, onModus = setzeModus)
                    HorizontalDivider()
                    StandardKnotenAuswahlInhalt(zustand, position)
                }
            },
            confirmButton = {
                TextButton(onClick = zustand::schließeKnotenAuswahl) { Text("Schließen") }
            },
        )

        KnotenWählerModus.Konzeptbibliothek -> KonzeptBibliothekDialog(
            zustand = zustand,
            position = position,
            vorlagen = zustand.sichtbareVorlagen(),
            onStandardWähler = { setzeModus(KnotenWählerModus.Standard) },
        )
    }
}

@Composable
internal fun KnotenWählerModusAuswahl(
    modus: KnotenWählerModus,
    onModus: (KnotenWählerModus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KnotenWählerModus.entries.forEach { eintrag ->
            FilterChip(
                selected = modus == eintrag,
                onClick = { onModus(eintrag) },
                label = { Text(eintrag.anzeigeName) },
            )
        }
    }
}

@Composable
private fun StandardKnotenAuswahlInhalt(zustand: AtlasZustand, position: GraphPunkt) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = zustand.suchText,
            onValueChange = zustand::setzeSuchText,
            label = { Text("Knoten suchen") },
            supportingText = { Text("Name, Kategorie oder Beschreibung") },
            trailingIcon = {
                if (zustand.suchText.isNotEmpty()) {
                    IconButton(
                        onClick = { zustand.setzeSuchText("") },
                        modifier = Modifier.semantics { contentDescription = "Suche zurücksetzen" },
                    ) {
                        Text("×", style = MaterialTheme.typography.titleLarge)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        val sichtbareVorlagen = zustand.sichtbareVorlagen()
        var ausgewählterTab by remember { mutableStateOf("Alle") }
        val zeigeTupelVektorAktionen = zustand.suchText.isBlank() || listOf(
            "Spaltenvektor erzeugen",
            "Zeilenvektor erzeugen",
            "Erzeugt ein Tupel und verbindet es mit Tupel zu Spalte oder Tupel zu Zeile",
        ).any { it.contains(zustand.suchText, ignoreCase = true) }
        val zeigeMengendefinition = zustand.suchText.isBlank() || listOf(
            "Mengendefinition",
            "Mengenkonstruktor",
            "Mengendefinator",
            "Erzeugt ein gekoppeltes Paar für eine symbolische Menge mit Prädikat",
        ).any { it.contains(zustand.suchText, ignoreCase = true) }
        val zeigeFaltung = zustand.suchText.isBlank() || listOf(
            "Endliche Faltung",
            "Faltungskonstruktor",
            "Faltungsdefinator",
            "Bindet Index und Akkumulator für iterierte Operatoren",
        ).any { it.contains(zustand.suchText, ignoreCase = true) }
        val rechnen = sichtbareVorlagen.filter(::istRechenVorlage)
        val mengen = sichtbareVorlagen.filter { it.kategorie == "Mengen" && it.art !in mengenrechnungsArten }
        val zahlen = sichtbareVorlagen.filter { it.art in setOf("mathematik.zahl", "mathematik.variable") }
        val tupel = sichtbareVorlagen.filter { it.art == "mathematik.tupel" }
        val matrizen = sichtbareVorlagen.filter { it.kategorie == "Matrizen" }
        val geometrie = sichtbareVorlagen.filter { it.kategorie.startsWith("Geometrie:") }
        val mengenZusatz = if (zeigeMengendefinition) 1 else 0
        val faltungsZusatz = if (zeigeFaltung) 1 else 0
        val tabs = listOf(
            KnotenAuswahlTab("Alle", sichtbareVorlagen.filterNot { it.kategorie in kartenKategorien }, zusätzlicheEinträge = mengenZusatz + faltungsZusatz),
            KnotenAuswahlTab("Rechnen", rechnen, zusätzlicheEinträge = faltungsZusatz),
            KnotenAuswahlTab("Zahlen", zahlen),
            KnotenAuswahlTab("Mengen", mengen, zusätzlicheEinträge = mengenZusatz),
            KnotenAuswahlTab("Tupel", tupel, zusätzlicheEinträge = if (zeigeTupelVektorAktionen) 2 else 0),
            KnotenAuswahlTab("Abbildungen", sichtbareVorlagen.filter { it.kategorie in abbildungsKategorien }),
            KnotenAuswahlTab("Vektoren", sichtbareVorlagen.filter { it.kategorie == "Vektoren" }),
            KnotenAuswahlTab("Matrizen", matrizen),
            KnotenAuswahlTab("Geometrie", geometrie),
            KnotenAuswahlTab("Aussagen", sichtbareVorlagen.filter { it.kategorie == "Aussage" || it.kategorie.startsWith("Aussagen:") }),
            KnotenAuswahlTab("Karten", sichtbareVorlagen.filter { it.kategorie in kartenKategorien }),
        )
        val aktiverTab = tabs.firstOrNull { it.name == ausgewählterTab && it.anzahl > 0 }
            ?: tabs.firstOrNull { it.anzahl > 0 }
            ?: tabs.first()
        PrimaryScrollableTabRow(selectedTabIndex = tabs.indexOf(aktiverTab), edgePadding = 0.dp) {
            tabs.forEach { tab ->
                Tab(
                    selected = aktiverTab == tab,
                    onClick = { ausgewählterTab = tab.name },
                    enabled = tab.anzahl > 0,
                    text = { Text("${tab.name} (${tab.anzahl})") },
                )
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 450.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (aktiverTab.name in setOf("Alle", "Rechnen") && zeigeFaltung) {
                item {
                    Text(
                        "Iterierte Operatoren",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                item {
                    ZusammengesetzterEintrag(
                        name = "Endliche Faltung",
                        beschreibung = "Erzeugt einen gekoppelten Faltungskonstruktor und Faltungsdefinator für Index, neutrales Element und Akkumulator.",
                        enabled = zustand.kannFaltungEinfügen(),
                        onClick = { zustand.fügeFaltungEin(position) },
                    )
                }
            }
            if (aktiverTab.name in setOf("Alle", "Mengen") && zeigeMengendefinition) {
                item {
                    Text(
                        "Mengendefinition",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                item {
                    ZusammengesetzterEintrag(
                        name = "Mengendefinition",
                        beschreibung = "Erzeugt einen gekoppelten Mengenkonstruktor und Mengendefinator ohne ungültige Direktverbindung.",
                        enabled = zustand.kannMengendefinitionEinfügen(),
                        onClick = { zustand.fügeMengendefinitionEin(position) },
                    )
                }
            }
            if (aktiverTab.name == "Tupel" && zeigeTupelVektorAktionen) {
                item {
                    Text(
                        "Vektor aus Tupel",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                item {
                    ZusammengesetzterEintrag(
                        name = "Spaltenvektor erzeugen",
                        beschreibung = "Erzeugt ein Tupel und verbindet es mit „Tupel zu Spalte“.",
                        enabled = zustand.kannTupelVektorEinfügen(),
                        onClick = { zustand.fügeTupelVektorEin(spalte = true, position = position) },
                    )
                }
                item {
                    ZusammengesetzterEintrag(
                        name = "Zeilenvektor erzeugen",
                        beschreibung = "Erzeugt ein Tupel und verbindet es mit „Tupel zu Zeile“.",
                        enabled = zustand.kannTupelVektorEinfügen(),
                        onClick = { zustand.fügeTupelVektorEin(spalte = false, position = position) },
                    )
                }
            }
            val gruppen = if (aktiverTab.name == "Alle") {
                aktiverTab.vorlagen.groupBy(::kategorieAnzeige)
            } else {
                emptyMap<String, List<KnotenVorlage>>()
            }
            if (gruppen.isNotEmpty()) {
                gruppen.keys.sortedWith(
                    compareBy(
                        { kategorienReihenfolge.indexOf(it).let { index -> if (index < 0) Int.MAX_VALUE else index } },
                        { it },
                    ),
                ).forEach { gruppe ->
                    val einträge = gruppen.getValue(gruppe)
                    item {
                        Text(
                            gruppe,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }
                    items(einträge.sortedBy { it.name }) { vorlage ->
                        ListItem(
                            headlineContent = { Text(vorlage.name) },
                            supportingContent = { Text(vorlage.beschreibung) },
                            modifier = Modifier.clip(MaterialTheme.shapes.small)
                                .clickable { zustand.fügeKnotenEin(vorlage, position) },
                        )
                    }
                }
            }
            if (aktiverTab.name != "Alle") {
                items(aktiverTab.vorlagen.sortedBy { it.name }) { vorlage ->
                    ListItem(
                        headlineContent = { Text(vorlage.name) },
                        supportingContent = { Text(vorlage.beschreibung) },
                        modifier = Modifier.clip(MaterialTheme.shapes.small)
                            .clickable { zustand.fügeKnotenEin(vorlage, position) },
                    )
                }
            }
            if (aktiverTab.anzahl == 0) {
                item {
                    Text(
                        "Keine passenden Knoten",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZusammengesetzterEintrag(
    name: String,
    beschreibung: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(beschreibung) },
        modifier = Modifier.clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick),
    )
}

private data class KnotenAuswahlTab(
    val name: String,
    val vorlagen: List<KnotenVorlage>,
    val zusätzlicheEinträge: Int = 0,
) {
    val anzahl get() = vorlagen.size + zusätzlicheEinträge
}

private fun istRechenVorlage(vorlage: KnotenVorlage): Boolean =
    vorlage.kategorie in rechnenKategorien || vorlage.art in mengenrechnungsArten

private fun kategorieAnzeige(vorlage: KnotenVorlage): String = when {
    istRechenVorlage(vorlage) -> "Rechnen"
    vorlage.kategorie.startsWith("Aussagen:") -> vorlage.kategorie.substringAfter(": ")
    vorlage.kategorie in abbildungsKategorien -> "Abbildungen"
    vorlage.kategorie in kartenKategorien -> "Karten"
    else -> vorlage.kategorie
}

private val mengenrechnungsArten = setOf(
    "mathematik.vereinigung",
    "mathematik.schnitt",
    "mathematik.differenz",
    "mathematik.kartesischesProdukt",
    "mathematik.mächtigkeit",
    "mathematik.iterierteVereinigung",
    "mathematik.iterierterSchnitt",
    "mathematik.iteriertesKartesischesProdukt",
    "mathematik.abbild",
)
private val rechnenKategorien = setOf("Rechnen", "Analysis", "Algebra", "Zahlen", "Operatoren", "Steuerung")
private val abbildungsKategorien = setOf("Methoden", "Abbildungen")
private val kartenKategorien = setOf("Gruppen", "Gespeicherte Karten")
private val kategorienReihenfolge = listOf(
    "Rechnen", "Mengen", "Abbildungen", "Vektoren", "Matrizen",
    "Geometrie: Räume", "Geometrie: Grundobjekte", "Geometrie: Konstruktionen", "Geometrie: Relationen",
    "Geometrie: Struktur", "Geometrie: Mengen", "Geometrie: Transformationen", "Geometrie: Darstellung",
    "Aussagenlogik", "Mengenprädikate", "Zahlenprädikate", "Aussagenprädikate", "Aussage", "Karten",
)
