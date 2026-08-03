package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun KonzeptBibliothekInhalt(
    zustand: AtlasZustand,
    position: GraphPunkt,
    vorlagen: List<KnotenVorlage>,
    modifier: Modifier = Modifier,
) {
    val einträge = remember(vorlagen) { KonzeptBibliothekRegister.erstelle(vorlagen) }
    val anschlussArten = remember(vorlagen) {
        vorlagen.flatMap { it.anschlüsse }.map { it.art }.distinctBy { it.wert }.sortedBy { it.wert }
    }
    var suchtext by remember { mutableStateOf("") }
    var hauptkategorie by remember { mutableStateOf<String?>(null) }
    var unterkategorie by remember { mutableStateOf<String?>(null) }
    var eingangsArt by remember { mutableStateOf<AnschlussArtId?>(null) }
    var ausgangsArt by remember { mutableStateOf<AnschlussArtId?>(null) }
    var definitionsKnoten by remember { mutableStateOf<KnotenDaten?>(null) }

    val kategoriePfad = when {
        hauptkategorie == null -> null
        unterkategorie == null -> listOf(hauptkategorie!!)
        else -> listOf(hauptkategorie!!, unterkategorie!!)
    }
    val filter = KonzeptBibliothekFilter(
        suchtext = suchtext,
        erforderlicherEingang = eingangsArt,
        erforderlicherAusgang = ausgangsArt,
        kategoriePfad = kategoriePfad,
    )
    val sichtbareEinträge = remember(einträge, filter) { einträge.filter { it.passt(filter) } }
    val unterkategorien = hauptkategorie?.let(KonzeptBibliothekRegister::unterkategorien).orEmpty()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = suchtext,
            onValueChange = { suchtext = it },
            label = { Text("Konzepte suchen") },
            supportingText = { Text("Name, Beschreibung, Synonym oder Fachgebiet") },
            trailingIcon = {
                if (suchtext.isNotBlank()) {
                    IconButton(onClick = { suchtext = "" }) { Text("×") }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = hauptkategorie == null,
                    onClick = {
                        hauptkategorie = null
                        unterkategorie = null
                    },
                    label = { Text("Alle") },
                )
            }
            items(KonzeptBibliothekRegister.kategorien, key = KonzeptKategorie::id) { kategorie ->
                FilterChip(
                    selected = hauptkategorie == kategorie.id,
                    onClick = {
                        hauptkategorie = kategorie.id
                        unterkategorie = null
                    },
                    label = { Text(kategorie.bezeichnung) },
                )
            }
        }

        if (unterkategorien.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = unterkategorie == null,
                        onClick = { unterkategorie = null },
                        label = { Text("Gesamter Bereich") },
                    )
                }
                items(unterkategorien, key = KonzeptKategorie::id) { kategorie ->
                    FilterChip(
                        selected = unterkategorie == kategorie.id,
                        onClick = { unterkategorie = kategorie.id },
                        label = { Text(kategorie.bezeichnung) },
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnschlussArtAuswahl(
                beschriftung = "Hat Eingang",
                wert = eingangsArt,
                optionen = anschlussArten,
                onWert = { eingangsArt = it },
                modifier = Modifier.weight(1f),
            )
            AnschlussArtAuswahl(
                beschriftung = "Hat Ausgang",
                wert = ausgangsArt,
                optionen = anschlussArten,
                onWert = { ausgangsArt = it },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            "${sichtbareEinträge.size} Konzepte · Knotendarstellung antippen fügt ein · Halten öffnet die Definition · Ziehen fügt ein",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 390.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(sichtbareEinträge, key = KonzeptBibliothekEintrag::id) { eintrag ->
                KonzeptBibliothekZeile(
                    zustand = zustand,
                    eintrag = eintrag,
                    position = position,
                    definitionÖffnen = { vorlage ->
                        definitionsKnoten = vorlage.erzeuge(GraphPunkt.Zero)
                    },
                )
            }
            if (sichtbareEinträge.isEmpty()) {
                item {
                    Text(
                        "Keine Konzepte entsprechen den aktiven Filtern.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    definitionsKnoten?.let { knoten ->
        KnotenKonzeptDialog(
            zustand = zustand,
            knoten = knoten,
            schließen = { definitionsKnoten = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KonzeptBibliothekZeile(
    zustand: AtlasZustand,
    eintrag: KonzeptBibliothekEintrag,
    position: GraphPunkt,
    definitionÖffnen: (KnotenVorlage) -> Unit,
) {
    val vorlage = eintrag.vorlage
    val dragModifier = if (eintrag.istEinfügbar && vorlage != null) {
        Modifier.knotenVorlagenDragQuelle(zustand, vorlage)
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier.fillMaxWidth()
            .then(dragModifier)
            .combinedClickable(
                enabled = true,
                onClick = {},
                onLongClick = { vorlage?.let(definitionÖffnen) },
            ),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vorlage != null) {
                KnotenBibliothekVorschau(
                    zustand = zustand,
                    eintragId = eintrag.id,
                    vorlage = vorlage,
                    onEinfügen = { zustand.fügeKnotenEin(vorlage, position) },
                )
            } else {
                Surface(
                    Modifier.width(180.dp).height(108.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Geplantes Konzept", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        eintrag.titel,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!eintrag.istEinfügbar) {
                        AssistChip(onClick = {}, enabled = false, label = { Text("Noch nicht verfügbar") })
                    }
                }
                Text(
                    eintrag.beschreibung,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    eintrag.kategoriePfade.joinToString(" · ") {
                        KonzeptBibliothekRegister.bezeichnungFür(it)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                vorlage?.let { knotenVorlage ->
                    val eingänge = knotenVorlage.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }
                    val ausgänge = knotenVorlage.anschlüsse.count { it.richtung == AnschlussRichtung.Ausgang }
                    Text(
                        "$eingänge Eingänge · $ausgänge Ausgänge",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    OutlinedButton(onClick = { definitionÖffnen(knotenVorlage) }) {
                        Text("Definition")
                    }
                }
            }
        }
    }
}

@Composable
private fun KnotenBibliothekVorschau(
    zustand: AtlasZustand,
    eintragId: String,
    vorlage: KnotenVorlage,
    onEinfügen: () -> Unit,
) {
    val knoten = remember(eintragId) { vorlage.erzeuge(GraphPunkt.Zero) }
    val renderer = remember(knoten.art, knoten.parameter) { zustand.rendererFür(knoten) }
    val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    val ausgänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }

    Surface(
        Modifier.width(180.dp).height(108.dp).clickable(onClick = onEinfügen),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 8.dp)) {
                renderer.Inhalt(knoten, ausgewählt = false, aktionen = VorschauAktionen)
            }
            AnschlussPunkte(
                anschlüsse = eingänge,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            AnschlussPunkte(
                anschlüsse = ausgänge,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun AnschlussPunkte(
    anschlüsse: List<AnschlussDaten>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        anschlüsse.forEach { anschluss ->
            Box(
                Modifier.size(9.dp).clip(CircleShape)
                    .background(anschlussFarbe(anschluss.art.wert)),
            )
        }
    }
}

@Composable
private fun AnschlussArtAuswahl(
    beschriftung: String,
    wert: AnschlussArtId?,
    optionen: List<AnschlussArtId>,
    onWert: (AnschlussArtId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var geöffnet by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { geöffnet = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "$beschriftung: ${wert?.wert ?: "beliebig"}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
            DropdownMenuItem(
                text = { Text("Beliebig") },
                onClick = {
                    onWert(null)
                    geöffnet = false
                },
            )
            optionen.forEach { art ->
                DropdownMenuItem(
                    text = { Text(art.wert) },
                    onClick = {
                        onWert(art)
                        geöffnet = false
                    },
                )
            }
        }
    }
}

private object VorschauAktionen : KnotenRendererAktionen {
    override fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
}
