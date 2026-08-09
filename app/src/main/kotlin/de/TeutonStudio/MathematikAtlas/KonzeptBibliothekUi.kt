package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen

private const val DIREKTE_KATEGORIE = "__direkt__"

internal enum class KonzeptRasterEbene { Hauptkategorien, Unterkategorien, Konzepte }

internal fun konzeptRasterSpalten(breiteDp: Float, ebene: KonzeptRasterEbene): Int = when (ebene) {
    KonzeptRasterEbene.Hauptkategorien -> when {
        breiteDp < 480f -> 2
        breiteDp < 720f -> 3
        breiteDp < 1000f -> 4
        else -> 5
    }

    KonzeptRasterEbene.Unterkategorien -> when {
        breiteDp < 560f -> 1
        breiteDp < 840f -> 2
        breiteDp < 1200f -> 3
        else -> 4
    }

    KonzeptRasterEbene.Konzepte -> when {
        breiteDp < 700f -> 1
        breiteDp < 1080f -> 2
        else -> 3
    }
}

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
    var hauptkategorieId by remember { mutableStateOf<String?>(null) }
    var unterkategorieId by remember { mutableStateOf<String?>(null) }
    var suchtext by remember { mutableStateOf("") }
    var eingangsArt by remember { mutableStateOf<AnschlussArtId?>(null) }
    var ausgangsArt by remember { mutableStateOf<AnschlussArtId?>(null) }
    var definitionsKnoten by remember { mutableStateOf<KnotenDaten?>(null) }

    val hauptkategorie = KonzeptBibliothekRegister.kategorien.firstOrNull { it.id == hauptkategorieId }
    val unterkategorie = hauptkategorie?.kinder?.firstOrNull { it.id == unterkategorieId }

    fun filterZurücksetzen() {
        suchtext = ""
        eingangsArt = null
        ausgangsArt = null
    }

    BoxWithConstraints(modifier) {
        when {
            hauptkategorie == null -> HauptkategorienEbene(
                einträge = einträge,
                anschlussArten = anschlussArten,
                suchtext = suchtext,
                onSuchtext = { suchtext = it },
                eingangsArt = eingangsArt,
                onEingangsArt = { eingangsArt = it },
                ausgangsArt = ausgangsArt,
                onAusgangsArt = { ausgangsArt = it },
                onFilterZurücksetzen = ::filterZurücksetzen,
                spalten = konzeptRasterSpalten(maxWidth.value, KonzeptRasterEbene.Hauptkategorien),
                onKategorie = { kategorie ->
                    hauptkategorieId = kategorie.id
                    unterkategorieId = if (kategorie.kinder.isEmpty()) DIREKTE_KATEGORIE else null
                },
            )

            unterkategorieId == null -> UnterkategorienEbene(
                hauptkategorie = hauptkategorie,
                einträge = einträge,
                anschlussArten = anschlussArten,
                suchtext = suchtext,
                onSuchtext = { suchtext = it },
                eingangsArt = eingangsArt,
                onEingangsArt = { eingangsArt = it },
                ausgangsArt = ausgangsArt,
                onAusgangsArt = { ausgangsArt = it },
                onFilterZurücksetzen = ::filterZurücksetzen,
                spalten = konzeptRasterSpalten(maxWidth.value, KonzeptRasterEbene.Unterkategorien),
                onZurück = { hauptkategorieId = null },
                onUnterkategorie = { unterkategorieId = it.id },
            )

            else -> KonzepteEbene(
                zustand = zustand,
                position = position,
                hauptkategorie = hauptkategorie,
                unterkategorie = unterkategorie,
                direkteKategorie = unterkategorieId == DIREKTE_KATEGORIE,
                einträge = einträge,
                anschlussArten = anschlussArten,
                suchtext = suchtext,
                onSuchtext = { suchtext = it },
                eingangsArt = eingangsArt,
                onEingangsArt = { eingangsArt = it },
                ausgangsArt = ausgangsArt,
                onAusgangsArt = { ausgangsArt = it },
                onFilterZurücksetzen = ::filterZurücksetzen,
                spalten = konzeptRasterSpalten(maxWidth.value, KonzeptRasterEbene.Konzepte),
                onZurück = {
                    if (unterkategorieId == DIREKTE_KATEGORIE) {
                        hauptkategorieId = null
                        unterkategorieId = null
                    } else {
                        unterkategorieId = null
                    }
                },
                definitionÖffnen = { vorlage -> definitionsKnoten = vorlage.erzeuge(GraphPunkt.Zero) },
            )
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

@Composable
private fun HauptkategorienEbene(
    einträge: List<KonzeptBibliothekEintrag>,
    anschlussArten: List<AnschlussArtId>,
    suchtext: String,
    onSuchtext: (String) -> Unit,
    eingangsArt: AnschlussArtId?,
    onEingangsArt: (AnschlussArtId?) -> Unit,
    ausgangsArt: AnschlussArtId?,
    onAusgangsArt: (AnschlussArtId?) -> Unit,
    onFilterZurücksetzen: () -> Unit,
    spalten: Int,
    onKategorie: (KonzeptKategorie) -> Unit,
) {
    val filter = KonzeptBibliothekFilter(
        suchtext = suchtext,
        erforderlicherEingang = eingangsArt,
        erforderlicherAusgang = ausgangsArt,
    )
    val ergebnisse = hauptkategorieTreffer(
        kategorien = KonzeptBibliothekRegister.kategorien,
        einträge = einträge,
        filter = filter,
    )
    val passendeAnzahl = ergebnisse.sumOf(KonzeptHauptkategorieTreffer::anzahl)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Fachgebiet wählen", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Suche und Anschlussfilter grenzen bereits die Fachgebiete anhand ihrer enthaltenen Konzepte ein.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BibliotheksSucheUndFilter(
            suchtext = suchtext,
            onSuchtext = onSuchtext,
            anschlussArten = anschlussArten,
            eingangsArt = eingangsArt,
            onEingangsArt = onEingangsArt,
            ausgangsArt = ausgangsArt,
            onAusgangsArt = onAusgangsArt,
            onZurücksetzen = onFilterZurücksetzen,
        )
        Text(
            "$passendeAnzahl passende Konzepte in ${ergebnisse.count { it.anzahl > 0 }} Fachgebieten",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (ergebnisse.isEmpty()) {
            LeererBibliothekszustand(
                text = "Kein Fachgebiet enthält passende Konzepte.",
                onZurücksetzen = onFilterZurücksetzen,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(spalten),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(ergebnisse, key = { it.kategorie.id }) { treffer ->
                    HauptkategorieKachel(treffer.kategorie, treffer.anzahl, onKategorie)
                }
            }
        }
    }
}

@Composable
private fun HauptkategorieKachel(
    kategorie: KonzeptKategorie,
    anzahl: Int,
    onKategorie: (KonzeptKategorie) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = anzahl > 0) { onKategorie(kategorie) },
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                kategorieSymbol(kategorie.id),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                kategorie.bezeichnung,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$anzahl Konzepte",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnterkategorienEbene(
    hauptkategorie: KonzeptKategorie,
    einträge: List<KonzeptBibliothekEintrag>,
    anschlussArten: List<AnschlussArtId>,
    suchtext: String,
    onSuchtext: (String) -> Unit,
    eingangsArt: AnschlussArtId?,
    onEingangsArt: (AnschlussArtId?) -> Unit,
    ausgangsArt: AnschlussArtId?,
    onAusgangsArt: (AnschlussArtId?) -> Unit,
    onFilterZurücksetzen: () -> Unit,
    spalten: Int,
    onZurück: () -> Unit,
    onUnterkategorie: (KonzeptKategorie) -> Unit,
) {
    val filter = KonzeptBibliothekFilter(
        suchtext = suchtext,
        erforderlicherEingang = eingangsArt,
        erforderlicherAusgang = ausgangsArt,
        kategoriePfad = listOf(hauptkategorie.id),
    )
    val passendeEinträge = einträge.filter { it.passt(filter) }
    val filterAktiv = suchtext.isNotBlank() || eingangsArt != null || ausgangsArt != null
    val ergebnisse = hauptkategorie.kinder.map { unterkategorie ->
        val treffer = passendeEinträge.filter { it.istInKategorie(hauptkategorie.id, unterkategorie.id) }
        unterkategorie to treffer
    }.filter { (_, treffer) -> !filterAktiv || treffer.isNotEmpty() }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BibliotheksNavigation(
            titel = hauptkategorie.bezeichnung,
            untertitel = "Unterkategorie auswählen oder Konzepte über Wort- und Anschlussfilter eingrenzen.",
            onZurück = onZurück,
        )
        BibliotheksSucheUndFilter(
            suchtext = suchtext,
            onSuchtext = onSuchtext,
            anschlussArten = anschlussArten,
            eingangsArt = eingangsArt,
            onEingangsArt = onEingangsArt,
            ausgangsArt = ausgangsArt,
            onAusgangsArt = onAusgangsArt,
            onZurücksetzen = onFilterZurücksetzen,
        )
        Text(
            "${passendeEinträge.size} passende Konzepte in ${ergebnisse.size} Unterkategorien",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (ergebnisse.isEmpty()) {
            LeererBibliothekszustand(
                text = "Keine Unterkategorie enthält passende Konzepte.",
                onZurücksetzen = onFilterZurücksetzen,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(spalten),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(ergebnisse, key = { it.first.id }) { (unterkategorie, treffer) ->
                    UnterkategorieKachel(
                        unterkategorie = unterkategorie,
                        treffer = treffer,
                        onClick = { onUnterkategorie(unterkategorie) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnterkategorieKachel(
    unterkategorie: KonzeptKategorie,
    treffer: List<KonzeptBibliothekEintrag>,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = treffer.isNotEmpty(), onClick = onClick),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    kategorieSymbol(unterkategorie.id),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.weight(1f)) {
                    Text(unterkategorie.bezeichnung, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${treffer.size} Treffer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                treffer.take(3).joinToString(" · ") { it.titel }.ifBlank { "Keine passenden Konzepte" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun KonzepteEbene(
    zustand: AtlasZustand,
    position: GraphPunkt,
    hauptkategorie: KonzeptKategorie,
    unterkategorie: KonzeptKategorie?,
    direkteKategorie: Boolean,
    einträge: List<KonzeptBibliothekEintrag>,
    anschlussArten: List<AnschlussArtId>,
    suchtext: String,
    onSuchtext: (String) -> Unit,
    eingangsArt: AnschlussArtId?,
    onEingangsArt: (AnschlussArtId?) -> Unit,
    ausgangsArt: AnschlussArtId?,
    onAusgangsArt: (AnschlussArtId?) -> Unit,
    onFilterZurücksetzen: () -> Unit,
    spalten: Int,
    onZurück: () -> Unit,
    definitionÖffnen: (KnotenVorlage) -> Unit,
) {
    val kategoriePfad = if (direkteKategorie) {
        listOf(hauptkategorie.id)
    } else {
        listOf(hauptkategorie.id, requireNotNull(unterkategorie).id)
    }
    val filter = KonzeptBibliothekFilter(
        suchtext = suchtext,
        erforderlicherEingang = eingangsArt,
        erforderlicherAusgang = ausgangsArt,
        kategoriePfad = kategoriePfad,
    )
    val sichtbareEinträge = einträge.filter { it.passt(filter) }
    val titel = unterkategorie?.bezeichnung ?: hauptkategorie.bezeichnung

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BibliotheksNavigation(
            titel = titel,
            untertitel = "Knotendarstellung antippen fügt ein. Ziehen legt den Knoten auf der Karte ab; Erklärung und Definition bleiben getrennt.",
            onZurück = onZurück,
        )
        BibliotheksSucheUndFilter(
            suchtext = suchtext,
            onSuchtext = onSuchtext,
            anschlussArten = anschlussArten,
            eingangsArt = eingangsArt,
            onEingangsArt = onEingangsArt,
            ausgangsArt = ausgangsArt,
            onAusgangsArt = onAusgangsArt,
            onZurücksetzen = onFilterZurücksetzen,
        )
        Text(
            "${sichtbareEinträge.size} Konzepte",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (sichtbareEinträge.isEmpty()) {
            LeererBibliothekszustand(
                text = "Keine Konzepte entsprechen den aktiven Filtern.",
                onZurücksetzen = onFilterZurücksetzen,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(spalten),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(sichtbareEinträge, key = KonzeptBibliothekEintrag::id) { eintrag ->
                    KonzeptBibliothekKarte(
                        zustand = zustand,
                        eintrag = eintrag,
                        position = position,
                        definitionÖffnen = definitionÖffnen,
                    )
                }
            }
        }
    }
}

@Composable
private fun BibliotheksNavigation(
    titel: String,
    untertitel: String,
    onZurück: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        TextButton(onClick = onZurück) { Text("‹ Zurück") }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titel, style = MaterialTheme.typography.headlineSmall)
            Text(
                untertitel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BibliotheksSucheUndFilter(
    suchtext: String,
    onSuchtext: (String) -> Unit,
    anschlussArten: List<AnschlussArtId>,
    eingangsArt: AnschlussArtId?,
    onEingangsArt: (AnschlussArtId?) -> Unit,
    ausgangsArt: AnschlussArtId?,
    onAusgangsArt: (AnschlussArtId?) -> Unit,
    onZurücksetzen: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = suchtext,
            onValueChange = onSuchtext,
            label = { Text("Konzepte durchsuchen") },
            supportingText = { Text("Wortsuche über Name, Erklärung, Synonyme und Fachbegriffe") },
            trailingIcon = {
                if (suchtext.isNotBlank()) {
                    IconButton(onClick = { onSuchtext("") }) { Text("×") }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 620.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnschlussArtAuswahl(
                        beschriftung = "Hat Eingang",
                        wert = eingangsArt,
                        optionen = anschlussArten,
                        onWert = onEingangsArt,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AnschlussArtAuswahl(
                        beschriftung = "Hat Ausgang",
                        wert = ausgangsArt,
                        optionen = anschlussArten,
                        onWert = onAusgangsArt,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnschlussArtAuswahl(
                        beschriftung = "Hat Eingang",
                        wert = eingangsArt,
                        optionen = anschlussArten,
                        onWert = onEingangsArt,
                        modifier = Modifier.weight(1f),
                    )
                    AnschlussArtAuswahl(
                        beschriftung = "Hat Ausgang",
                        wert = ausgangsArt,
                        optionen = anschlussArten,
                        onWert = onAusgangsArt,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (suchtext.isNotBlank() || eingangsArt != null || ausgangsArt != null) {
            TextButton(onClick = onZurücksetzen, modifier = Modifier.align(Alignment.End)) {
                Text("Suche und Filter zurücksetzen")
            }
        }
    }
}

@Composable
private fun KonzeptBibliothekKarte(
    zustand: AtlasZustand,
    eintrag: KonzeptBibliothekEintrag,
    position: GraphPunkt,
    definitionÖffnen: (KnotenVorlage) -> Unit,
) {
    val vorlage = eintrag.vorlage
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vorlage != null) {
                KnotenBibliothekVorschau(
                    zustand = zustand,
                    eintragId = eintrag.id,
                    vorlage = vorlage,
                    einfügbar = eintrag.istEinfügbar,
                    onEinfügen = { zustand.fügeKnotenEin(vorlage, position) },
                    onDefinition = { definitionÖffnen(vorlage) },
                )
            } else {
                Surface(
                    Modifier.fillMaxWidth().aspectRatio(5f / 3f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Geplantes Konzept", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    eintrag.titel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!eintrag.istEinfügbar) {
                    AssistChip(onClick = {}, enabled = false, label = { Text("Geplant") })
                }
            }
            Text(
                eintrag.beschreibung,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                eintrag.kategoriePfade.joinToString(" · ") { KonzeptBibliothekRegister.bezeichnungFür(it) },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            vorlage?.let { knotenVorlage ->
                val eingänge = knotenVorlage.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }
                val ausgänge = knotenVorlage.anschlüsse.count { it.richtung == AnschlussRichtung.Ausgang }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$eingänge Eingänge · $ausgänge Ausgänge",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    TextButton(onClick = { definitionÖffnen(knotenVorlage) }) { Text("Definition") }
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
    einfügbar: Boolean,
    onEinfügen: () -> Unit,
    onDefinition: () -> Unit,
) {
    val knoten = remember(eintragId) { vorlage.erzeuge(GraphPunkt.Zero) }
    val renderer = remember(knoten.art, knoten.parameter) { zustand.rendererFür(knoten) }
    val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    val ausgänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }

    Surface(
        Modifier.fillMaxWidth().aspectRatio(5f / 3f),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 8.dp)) {
                renderer.Inhalt(knoten, ausgewählt = false, aktionen = VorschauAktionen)
            }
            AnschlussPunkte(eingänge, Modifier.align(Alignment.CenterStart))
            AnschlussPunkte(ausgänge, Modifier.align(Alignment.CenterEnd))
            Box(
                Modifier.matchParentSize()
                    .then(
                        if (einfügbar) Modifier.konzeptVorlagenInteraktion(
                            zustand = zustand,
                            vorlage = vorlage,
                            onEinfügen = onEinfügen,
                            onDefinition = onDefinition,
                        ) else Modifier,
                    )
                    .onKeyEvent { event ->
                        if (einfügbar && event.type == KeyEventType.KeyUp &&
                            (event.key == Key.Enter || event.key == Key.Spacebar)
                        ) {
                            onEinfügen()
                            true
                        } else false
                    }
                    .focusable(enabled = einfügbar)
                    .semantics {
                        contentDescription = if (einfügbar) "${vorlage.name} einfügen" else "${vorlage.name} ist geplant"
                        if (einfügbar) {
                            onClick("Knoten einfügen") { onEinfügen(); true }
                            customActions = listOf(
                                CustomAccessibilityAction("Definition öffnen") { onDefinition(); true },
                            )
                        }
                    },
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
        OutlinedButton(onClick = { geöffnet = true }, modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun ColumnScope.LeererBibliothekszustand(
    text: String,
    onZurücksetzen: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().weight(1f).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        onZurücksetzen?.let { zurücksetzen ->
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = zurücksetzen) { Text("Suche und Filter zurücksetzen") }
        }
    }
}

private fun KonzeptBibliothekEintrag.istInKategorie(hauptkategorie: String, unterkategorie: String): Boolean =
    kategoriePfade.any { pfad -> pfad.firstOrNull() == hauptkategorie && pfad.getOrNull(1) == unterkategorie }

private fun kategorieSymbol(id: String): String = when (id) {
    "analysis", "differential-integral" -> "∫"
    "lineare-algebra", "vektoren" -> "𝐯"
    "matrizen" -> "▦"
    "tensoren" -> "⊗"
    "skalarprodukte" -> "⟨·,·⟩"
    "geometrie", "grundobjekte" -> "△"
    "konstruktionen" -> "⌖"
    "transformationen" -> "↻"
    "visualisierung" -> "⌗"
    "mengenlehre", "mengen" -> "∈"
    "mengenoperationen" -> "∪"
    "mengendefinitionen" -> "{x}"
    "logik", "aussagen" -> "⊢"
    "praedikate" -> "P(x)"
    "quantoren" -> "∀"
    "algebra", "operationen" -> "x²"
    "zahlen" -> "ℝ"
    "methoden", "funktionen" -> "f"
    "folgen-reihen" -> "Σ"
    "topologie", "grundbegriffe" -> "⊂"
    "stochastik" -> "P"
    "eigene-karten" -> "▣"
    "karteneingaenge" -> "→"
    "kartenausgaenge" -> "←"
    else -> "•"
}

private object VorschauAktionen : KnotenRendererAktionen {
    override fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
}
