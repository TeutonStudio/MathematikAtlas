package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.MathematikKnotenRenderer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun KonzeptBrowser(sitzung: KonzeptSitzung, modifier: Modifier = Modifier) {
    var suche by remember { mutableStateOf("") }
    val sichtbar = remember(suche) {
        TestDefinitionsKarten.alle.filter { konzept ->
            suche.isBlank() || sequenceOf(
                konzept.name,
                konzept.beschreibung,
                konzept.pfad.joinToString(" / "),
                konzept.tags.joinToString(" "),
            ).any { it.contains(suche, ignoreCase = true) }
        }
    }
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = suche,
            onValueChange = { suche = it },
            label = { Text("Konzepte durchsuchen") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(sichtbar, key = { it.id.wert }) { konzept ->
                ListItem(
                    headlineContent = { Text(konzept.name) },
                    supportingContent = {
                        Column {
                            Text(konzept.pfad.joinToString(" / "))
                            Text(konzept.beschreibung, maxLines = 2)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (sitzung.aktuellesKonzept?.id == konzept.id) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface,
                            MaterialTheme.shapes.medium,
                        )
                        .clickable { sitzung.öffne(konzept.id) },
                )
            }
            if (sichtbar.isEmpty()) item {
                Text("Keine passenden Konzepte.", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun KonzeptKopfLeiste(sitzung: KonzeptSitzung, modifier: Modifier = Modifier) {
    val konzept = sitzung.aktuellesKonzept ?: return
    Row(
        modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(konzept.name, style = MaterialTheme.typography.titleMedium)
            Text(konzept.pfad.joinToString(" / "), style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = sitzung::setzeAktuellenReiterZurück) { Text("Werte zurücksetzen") }
        Button(onClick = sitzung::schließe) { Text("Konzept schließen") }
    }
}

@Composable
internal fun KonzeptArbeitsbereich(sitzung: KonzeptSitzung, modifier: Modifier = Modifier) {
    val konzept = sitzung.aktuellesKonzept ?: return
    Column(modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = konzept.sortierteReiter.indexOfFirst { it.id == sitzung.aktuellerEintrag?.reiterId }.coerceAtLeast(0),
            edgePadding = 8.dp,
        ) {
            konzept.sortierteReiter.forEach { reiter ->
                Tab(
                    selected = sitzung.aktuellerEintrag?.reiterId == reiter.id,
                    onClick = { sitzung.wähleReiter(reiter.id) },
                    text = { Text(reiter.titel) },
                )
            }
        }
        KonzeptKartenFläche(
            sitzung = sitzung,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Composable
internal fun KonzeptInspektor(sitzung: KonzeptSitzung, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        val karte = sitzung.aktuelleKarte()
        val ausgewählt = sitzung.aktuellerEintrag?.ausgewählterKnoten
        val knoten = karte?.knoten?.firstOrNull { it.id == ausgewählt }
        val freigaben = sitzung.freigabenFürAuswahl()
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Konzept erkunden", style = MaterialTheme.typography.headlineSmall)
            if (knoten == null) {
                Text(
                    "Wähle einen Knoten aus. Halten öffnet ein verknüpftes Konzept im selben Pfad.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = sitzung::setzeAktuellenReiterZurück) { Text("Werte zurücksetzen") }
                return@Column
            }
            Text(knoten.name, style = MaterialTheme.typography.titleLarge)
            Text(knoten.art, style = MaterialTheme.typography.labelMedium)
            if (freigaben.isEmpty()) {
                Text(
                    "Dieser Knoten gehört zur unveränderlichen Struktur der Konzeptkarte.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                freigaben.forEach { freigabe ->
                    val wert = knoten.parameter[freigabe.parameter].orEmpty()
                    OutlinedTextField(
                        value = wert,
                        onValueChange = { sitzung.setzeParameter(knoten.id, freigabe.parameter, it) },
                        label = { Text(freigabe.beschriftung) },
                        supportingText = { Text("Nur diese Testkonstante wird verändert.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = sitzung::setzeAktuellenReiterZurück) { Text("Werte zurücksetzen") }
        }
    }
}

@Composable
internal fun KnotenKonzeptDialog(
    zustand: AtlasZustand,
    ursprungsKnoten: KnotenDaten,
    schließen: () -> Unit,
) {
    val wurzel = remember(ursprungsKnoten.id) { TestDefinitionsKarten.fürKnoten(ursprungsKnoten) }
    val sitzung = remember(ursprungsKnoten.id) {
        KonzeptSitzung().apply { wurzel?.let { öffne(it.id) } }
    }
    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.9f).widthIn(max = 1380.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ursprungsKnoten.name, style = MaterialTheme.typography.titleLarge)
                        Text(ursprungsKnoten.art, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }
                HorizontalDivider()
                if (wurzel == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Für diesen Knotentyp ist noch keine Test-Definitionskarte hinterlegt.")
                    }
                } else {
                    val konzept = sitzung.aktuellesKonzept
                    if (konzept != null) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = konzept.sortierteReiter.indexOfFirst { it.id == sitzung.aktuellerEintrag?.reiterId }.coerceAtLeast(0),
                            edgePadding = 8.dp,
                        ) {
                            konzept.sortierteReiter.forEach { reiter ->
                                Tab(
                                    selected = sitzung.aktuellerEintrag?.reiterId == reiter.id,
                                    onClick = { sitzung.wähleReiter(reiter.id) },
                                    text = { Text(reiter.titel) },
                                )
                            }
                        }
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        KonzeptKartenFläche(sitzung, Modifier.weight(1f).fillMaxHeight())
                        VerticalDivider()
                        KonzeptInspektor(sitzung, Modifier.width(310.dp).fillMaxHeight())
                    }
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        zustand.editor.wähleKnoten(ursprungsKnoten.id)
                        zustand.editor.dupliziereAuswahl()
                        schließen()
                    }) { Text("Duplizieren") }
                    TextButton(onClick = {
                        zustand.editor.wähleKnoten(ursprungsKnoten.id)
                        zustand.editor.isoliereAusgewähltenKnoten()
                        schließen()
                    }) { Text("Isolieren") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            zustand.editor.wähleKnoten(ursprungsKnoten.id)
                            zustand.editor.löscheAuswahl()
                            schließen()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Löschen") }
                }
            }
        }
    }
}

@Composable
private fun KonzeptKartenFläche(sitzung: KonzeptSitzung, modifier: Modifier = Modifier) {
    val karte = sitzung.aktuelleKarte() ?: return
    val auswertung = remember(karte) {
        KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(karte)
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        KonzeptKartenCanvas(
            karte = karte,
            auswertung = auswertung,
            ausgewählterKnoten = sitzung.aktuellerEintrag?.ausgewählterKnoten,
            onKnotenKlick = sitzung::wähleKnoten,
            onKnotenHalten = sitzung::navigiereÜber,
            modifier = Modifier.fillMaxSize(),
        )
        KonzeptBreadcrumb(
            sitzung = sitzung,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
    }
}

@Composable
private fun KonzeptBreadcrumb(sitzung: KonzeptSitzung, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sitzung.pfad.forEachIndexed { index, eintrag ->
                val name = TestDefinitionsKarten.finde(eintrag.konzeptId)?.name ?: eintrag.konzeptId.wert
                TextButton(
                    onClick = { sitzung.springeZu(index) },
                    enabled = index < sitzung.pfad.lastIndex,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) { Text(name) }
                if (index < sitzung.pfad.lastIndex) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KonzeptKartenCanvas(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
    ausgewählterKnoten: KnotenId?,
    onKnotenKlick: (KnotenId?) -> Unit,
    onKnotenHalten: (KnotenDaten) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (karte.knoten.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Leere Konzeptkarte") }
        return
    }
    val puffer = 48f
    val minX = karte.knoten.minOf { it.position.x } - puffer
    val minY = karte.knoten.minOf { it.position.y } - puffer
    val maxX = karte.knoten.maxOf { it.position.x + it.größe.breite } + puffer
    val maxY = karte.knoten.maxOf { it.position.y + it.größe.höhe } + puffer
    val weltBreite = max(1f, maxX - minX)
    val weltHöhe = max(1f, maxY - minY)
    val renderer = remember(auswertung) { MathematikKnotenRenderer { knoten -> auswertung.knoten[knoten.id] } }
    val verbindungsFarbe = MaterialTheme.colorScheme.outline
    val keineAktionen = remember {
        object : KnotenRendererAktionen {
            override fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
        }
    }

    BoxWithConstraints(modifier.clipToBounds().clickable { onKnotenKlick(null) }) {
        val verfügbareBreite = max(1f, maxWidth.value - 28f)
        val verfügbareHöhe = max(1f, maxHeight.value - 28f)
        val skalierung = min(verfügbareBreite / weltBreite, verfügbareHöhe / weltHöhe).coerceIn(.32f, 1.25f)
        val xVersatz = ((maxWidth.value - weltBreite * skalierung) / 2f).coerceAtLeast(0f)
        val yVersatz = ((maxHeight.value - weltHöhe * skalierung) / 2f).coerceAtLeast(0f)
        val dichte = LocalDensity.current

        Box(
            Modifier
                .offset(xVersatz.dp, yVersatz.dp)
                .width(weltBreite.dp)
                .height(weltHöhe.dp)
                .graphicsLayer {
                    scaleX = skalierung
                    scaleY = skalierung
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                fun px(wert: Float): Float = with(dichte) { wert.dp.toPx() }
                karte.verbindungen.forEach { verbindung ->
                    val vonKnoten = karte.knoten.firstOrNull { it.id == verbindung.von.knotenId } ?: return@forEach
                    val zuKnoten = karte.knoten.firstOrNull { it.id == verbindung.zu.knotenId } ?: return@forEach
                    val vonAnschluss = vonKnoten.anschlüsse.firstOrNull { it.id == verbindung.von.anschlussId } ?: return@forEach
                    val zuAnschluss = zuKnoten.anschlüsse.firstOrNull { it.id == verbindung.zu.anschlussId } ?: return@forEach
                    val start = anschlussWeltPosition(vonKnoten, vonAnschluss) - Offset(minX, minY)
                    val ende = anschlussWeltPosition(zuKnoten, zuAnschluss) - Offset(minX, minY)
                    val sx = px(start.x)
                    val sy = px(start.y)
                    val ex = px(ende.x)
                    val ey = px(ende.y)
                    val abstand = max(px(72f), abs(ex - sx) * .45f)
                    val pfad = Path().apply {
                        moveTo(sx, sy)
                        cubicTo(sx + abstand, sy, ex - abstand, ey, ex, ey)
                    }
                    drawPath(
                        pfad,
                        color = verbindungsFarbe,
                        style = Stroke(width = px(3f)),
                    )
                }
            }

            karte.knoten.forEach { knoten ->
                val ausgewählt = knoten.id == ausgewählterKnoten
                Box(
                    Modifier
                        .offset((knoten.position.x - minX).dp, (knoten.position.y - minY).dp)
                        .size(knoten.größe.breite.dp, knoten.größe.höhe.dp),
                ) {
                    Card(
                        Modifier
                            .fillMaxSize()
                            .border(
                                if (ausgewählt) 3.dp else 1.dp,
                                if (ausgewählt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.medium,
                            )
                            .combinedClickable(
                                onClick = { onKnotenKlick(knoten.id) },
                                onLongClick = { onKnotenHalten(knoten) },
                            ),
                        elevation = CardDefaults.cardElevation(if (ausgewählt) 8.dp else 2.dp),
                    ) {
                        renderer.Inhalt(knoten, ausgewählt, keineAktionen)
                    }
                    knoten.anschlüsse.forEach { anschluss ->
                        KonzeptAnschlussPunkt(knoten, anschluss)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.KonzeptAnschlussPunkt(knoten: KnotenDaten, anschluss: AnschlussDaten) {
    val gleicherKante = knoten.anschlüsse.filter { it.kante == anschluss.kante }.sortedBy { it.reihenfolge }
    val index = gleicherKante.indexOfFirst { it.id == anschluss.id }.coerceAtLeast(0)
    val anteil = (index + 1f) / (gleicherKante.size + 1f)
    val x = when (anschluss.kante) {
        AnschlussKante.Links -> (-7).dp
        AnschlussKante.Rechts -> (knoten.größe.breite - 7).dp
        AnschlussKante.Oben, AnschlussKante.Unten -> (knoten.größe.breite * anteil - 7).dp
    }
    val y = when (anschluss.kante) {
        AnschlussKante.Oben -> (-7).dp
        AnschlussKante.Unten -> (knoten.größe.höhe - 7).dp
        AnschlussKante.Links, AnschlussKante.Rechts -> (knoten.größe.höhe * anteil - 7).dp
    }
    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(x, y)
            .size(14.dp)
            .background(anschlussFarbe(anschluss.art.wert), CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
    )
}

private fun anschlussWeltPosition(knoten: KnotenDaten, anschluss: AnschlussDaten): Offset {
    val gleicherKante = knoten.anschlüsse.filter { it.kante == anschluss.kante }.sortedBy { it.reihenfolge }
    val index = gleicherKante.indexOfFirst { it.id == anschluss.id }.coerceAtLeast(0)
    val anteil = (index + 1f) / (gleicherKante.size + 1f)
    return when (anschluss.kante) {
        AnschlussKante.Links -> Offset(knoten.position.x, knoten.position.y + knoten.größe.höhe * anteil)
        AnschlussKante.Rechts -> Offset(knoten.position.x + knoten.größe.breite, knoten.position.y + knoten.größe.höhe * anteil)
        AnschlussKante.Oben -> Offset(knoten.position.x + knoten.größe.breite * anteil, knoten.position.y)
        AnschlussKante.Unten -> Offset(knoten.position.x + knoten.größe.breite * anteil, knoten.position.y + knoten.größe.höhe)
    }
}
