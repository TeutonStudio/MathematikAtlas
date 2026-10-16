package de.TeutonStudio.MathematikKnoten.visualisierung.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenViewportInhalt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.*

/** Passive, begrenzte Vorschau. Der Editor besitzt sämtliche Knotengesten. */
class VisualisierungsKnotenRenderer(private val ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis?) : KnotenRenderer {
    override val interaktionsModus = KnotenInteraktionsModus.GanzeFlächeZiehbar
    override val misstNatürlichenInhalt = false
    @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        val mengen = visualisierungsMengen(knoten, ergebnisFür(knoten))
        val menge = mengen.firstOrNull()?.second
        val c = VisualisierungsKonfiguration.aus(knoten.eigenschaften).mitWirksamerDimension(menge)
        KnotenViewportInhalt(aktionen, kopf = {
            Text("Visualisierung · ${raumName(c.dimension)}", style = MaterialTheme.typography.titleMedium)
        }, ansicht = { modifier ->
            VisualisierungsSzene(mengen, c, false, {}, modifier)
        }, fuß = { Text("Vorschau · Halten für Details", style = MaterialTheme.typography.labelSmall) })
    }
}

private data class VisualisierungsBerechnungsStand(val ergebnis: VisualisierungsErgebnis? = null, val fertig: Boolean = false, val vorschau: VisualisierungsErgebnis? = null)

/** Ein neuer Schlüssel besitzt einen neuen Zustand; alte Resultate können keine Anfrage überspringen. */
@Composable
private fun erinnertesVisualisierungsErgebnis(menge: MengenAusdruck?, c: VisualisierungsKonfiguration, detail: Boolean): State<VisualisierungsBerechnungsStand> {
    val schlüssel = listOf(menge, c.samplingSignatur(), detail)
    val zustand = remember(schlüssel) {
        val vollständig = menge?.let { gemeinsameVisualisierungsBerechnung.gecacht(it, c) }
        mutableStateOf(VisualisierungsBerechnungsStand(vollständig, vollständig != null))
    }
    LaunchedEffect(schlüssel) {
        if (zustand.value.fertig) return@LaunchedEffect
        if (menge == null) { zustand.value = VisualisierungsBerechnungsStand(VisualisierungsErgebnis.NichtDarstellbar("Verbinde eine Menge mit dem Eingang."), true); return@LaunchedEffect }
        try {
            run {
                val vorschau = gemeinsameVisualisierungsBerechnung.berechne(menge, c.alsVorschau().copy(
                    sampling = c.alsVorschau().sampling.copy(zielFehlerPixel = 3.0 / if (c.dimension == RaumDimension.R3) c.kamera.zoom else 1.0)), detail)
                zustand.value = VisualisierungsBerechnungsStand(vorschau, false, vorschau)
            }
            delay(120)
            zustand.value = VisualisierungsBerechnungsStand(gemeinsameVisualisierungsBerechnung.berechne(menge, c, detail), true, zustand.value.vorschau)
        } catch (abbruch: kotlinx.coroutines.CancellationException) { throw abbruch }
        catch (fehler: Exception) { zustand.value = VisualisierungsBerechnungsStand(VisualisierungsErgebnis.NichtDarstellbar(fehler.message ?: "Darstellung konnte nicht berechnet werden."), true) }
    }
    return zustand
}

@Composable
private fun VisualisierungsSzene(mengen: List<Pair<String, MengenAusdruck>>, konfiguration: VisualisierungsKonfiguration, interaktiv: Boolean,
    ändern: (VisualisierungsKonfiguration) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val dichte = androidx.compose.ui.platform.LocalDensity.current
        val breite = with(dichte) { maxWidth.toPx() }.coerceAtLeast(1f)
        val höhe = with(dichte) { maxHeight.toPx() }.coerceAtLeast(1f)
        val t = VisualisierungsTransformation(konfiguration, breite.toDouble(), höhe.toDouble())
        val basis = konfiguration
        // Gerundete Pixelmaße vermeiden neue Anfragen bei subpixeligen Layoutänderungen.
        val c = basis.copy(sampling = basis.sampling.copy(bildBreite = breite.toDouble(), bildHöhe = höhe.toDouble(),
            sichtBereiche = t.sichtBereiche(), zielFehlerPixel = 1.0 / if (basis.dimension == RaumDimension.R3) basis.kamera.zoom else 1.0))
        val aktiveMengen = mengen.filterIndexed { index, (name, _) -> konfiguration.ebenenStil(name, index).sichtbar }
        val anzahl = aktiveMengen.size.coerceAtLeast(1)
        val ebenenConfig = aktiveMengen.map { (name, _) ->
            val stil = konfiguration.ebenenStil(name, mengen.indexOfFirst { it.first == name })
            c.copy(sampling = c.sampling.copy(maximalesRasterBudget = (minOf(c.sampling.maximalesRasterBudget, c.detailBudget) / anzahl).coerceAtLeast(1)),
                aktiverStil = stil, farbe = if (c.farbe.modus == FarbModus.Spektrum) c.farbe else c.farbe.copy(festeFarbe = stil.farbe))
        }
        val stände = aktiveMengen.mapIndexed { index, (name, menge) -> key(name) {
            val empfohlen = konfiguration.copy(dimensionsModus = DimensionsModus.Automatisch).mitWirksamerDimension(menge).dimension
            if (empfohlen != konfiguration.dimension && konfiguration.ausgabeKoordinaten.isEmpty())
                VisualisierungsBerechnungsStand(VisualisierungsErgebnis.NichtDarstellbar("$name: Raum $empfohlen passt nicht zu ${konfiguration.dimension}."), true)
            else {
                val aktuell = erinnertesVisualisierungsErgebnis(menge, ebenenConfig[index], interaktiv).value
                var vorhanden by remember(menge, konfiguration.dimension, konfiguration.methodenModus, konfiguration.bereiche,
                    konfiguration.ausgabeKoordinaten, konfiguration.festeSchnitte) { mutableStateOf<VisualisierungsErgebnis?>(null) }
                LaunchedEffect(aktuell.ergebnis) { aktuell.ergebnis?.let { vorhanden = it } }
                aktuell.copy(ergebnis = aktuell.ergebnis ?: vorhanden)
            }
        } }
        val stand = VisualisierungsBerechnungsStand(vereinigeVisualisierungsErgebnisse(stände.mapNotNull { it.ergebnis }), stände.all { it.fertig }, vereinigeVisualisierungsErgebnisse(stände.mapNotNull { it.vorschau }))
        val ergebnis = stand.ergebnis ?: if (mengen.isEmpty()) VisualisierungsErgebnis.NichtDarstellbar("Verbinde eine Menge mit einem Eingang.") else null
        val menge = mengen
        val zeichenEbenen = stände.mapIndexed { i, e -> Triple(e.ergebnis, ebenenConfig[i], e.vorschau) }
        var letzteGeometrie by remember(menge, konfiguration.dimension, konfiguration.methodenModus,
            konfiguration.achsen, konfiguration.festeSchnitte, konfiguration.ausgabeKoordinaten, konfiguration.bereiche) {
            mutableStateOf<VisualisierungsErgebnis?>(null)
        }
        var letzteVorschau by remember(menge, konfiguration.dimension, konfiguration.methodenModus, konfiguration.achsen, konfiguration.festeSchnitte, konfiguration.ausgabeKoordinaten, konfiguration.bereiche) { mutableStateOf<VisualisierungsErgebnis?>(null) }
        LaunchedEffect(stand.vorschau) { if (stand.vorschau != null) letzteVorschau = stand.vorschau }
        LaunchedEffect(ergebnis) { if (ergebnis != null) letzteGeometrie = ergebnis }
        Plot(ergebnis ?: letzteGeometrie, konfiguration, { ändern(konfiguration.copy(kamera = it)) }, Modifier.fillMaxSize(), interaktiv, stand.vorschau ?: letzteVorschau, zeichenEbenen, aktiveMengen)
        if (!stand.fertig) Text(if (letzteGeometrie == null && ergebnis == null) "Berechne Darstellung …" else "Verfeinere Darstellung …",
            modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
        val status = when (val e = ergebnis) {
            is VisualisierungsErgebnis.NichtDarstellbar -> e.grund
            is VisualisierungsErgebnis.ProjektionErforderlich -> e.grund
            is VisualisierungsErgebnis.BedingtDarstellbar -> (listOf(e.grund) + e.bedingungen).joinToString(" ")
            is VisualisierungsErgebnis.Teilweise -> "Teilweise · " + e.hinweise.firstOrNull().orEmpty()
            is VisualisierungsErgebnis.Erfolgreich -> if (interaktiv) "${e.statistik.auswertungen} Auswertungen · " +
                (if (e.istApproximation) "Numerische Approximation" else "Exakt") + if (e.statistik.budgetErschöpft) " · Budget ausgeschöpft" else "" else ""
            null -> ""
        }
        if (interaktiv && aktiveMengen.isNotEmpty()) Surface(Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(6.dp), color=MaterialTheme.colorScheme.surface.copy(alpha=.92f)) {
            Column(Modifier.widthIn(max=maxWidth*.4f).heightIn(max=120.dp).verticalScroll(rememberScrollState()).padding(6.dp)) { aktiveMengen.forEachIndexed { index, (name, _) ->
                val stil=ebenenConfig[index].aktiverStil
                val e=stände[index].ergebnis
                val nachweis=when(e) { is VisualisierungsErgebnis.Erfolgreich -> if(e.istApproximation) "Näherung" else "exakt"
                    is VisualisierungsErgebnis.Teilweise -> "partiell / Näherung"; else -> "nicht dargestellt" }
                Text("● ${stil.name.ifBlank { "Menge ${mengen.indexOfFirst { it.first==name }+1}" }} · $nachweis", color=Color(stil.farbe ?: 0xFF2563EB), style=MaterialTheme.typography.labelSmall)
            } }
        }
        if (status.isNotBlank()) Surface(Modifier.align(androidx.compose.ui.Alignment.TopStart).widthIn(max=if(interaktiv) maxWidth*.55f else maxWidth).padding(6.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)) {
            Text(status, style = MaterialTheme.typography.labelSmall, maxLines = if (interaktiv) 4 else 2, modifier = Modifier.padding(4.dp))
        }
        if (interaktiv) {
            var menüOffen by remember { mutableStateOf(false) }
            val eingepassteKamera = remember(ergebnis, letzteGeometrie, konfiguration, breite, höhe) {
                kameraEinpassen(ergebnis ?: letzteGeometrie, konfiguration, breite.toDouble(), höhe.toDouble())
            }
            Row(Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { ändern(konfiguration.copy(kamera = t.navigiere(0.0, 0.0, 1.4, breite / 2.0, höhe / 2.0))) }) { Text("+") }
                TextButton(onClick = { ändern(konfiguration.copy(kamera = t.navigiere(0.0, 0.0, 1 / 1.4, breite / 2.0, höhe / 2.0))) }) { Text("−") }
                Box {
                    TextButton(onClick = { menüOffen = true }) { Text("Ansicht") }
                    DropdownMenu(expanded = menüOffen, onDismissRequest = { menüOffen = false }) {
                        DropdownMenuItem(text = { Text("Standardansicht") }, onClick = {
                            ändern(konfiguration.copy(kamera = standardKamera(konfiguration.dimension))); menüOffen = false
                        })
                        DropdownMenuItem(text = { Text(if (ergebnis is VisualisierungsErgebnis.Teilweise) "Teilbild einpassen" else "Dargestellten Inhalt einpassen") },
                            enabled = eingepassteKamera != null, onClick = { eingepassteKamera?.let { ändern(konfiguration.copy(kamera = it)) }; menüOffen = false })
                        if (konfiguration.dimension == RaumDimension.R3) listOf("XY" to (0.0 to 0.0), "XZ" to (90.0 to 0.0), "YZ" to (0.0 to 90.0)).forEach { (name, winkel) ->
                            DropdownMenuItem(text = { Text("$name-Ebene") }, onClick = {
                                ändern(konfiguration.copy(kamera = konfiguration.kamera.copy(rotationX = winkel.first, rotationY = winkel.second, rotationZ = 0.0))); menüOffen = false
                            })
                        }
                        fun grenzen(b: AchsenBereiche): String {
                            fun zahl(v: Double) = java.lang.String.format(java.util.Locale.ROOT, "%.5g", v)
                            fun bereich(name: String, r: ZahlenBereich) = "$name: [${zahl(r.minimum)}, ${zahl(r.maximum)}]"
                            return buildList {
                                add(bereich(konfiguration.achsen.x,b.x))
                                if(konfiguration.dimension != RaumDimension.R1) add(bereich(konfiguration.achsen.y,b.y))
                                if(konfiguration.dimension == RaumDimension.R3 && b.z != null) add(bereich(konfiguration.achsen.z ?: "z",b.z))
                            }.joinToString(" · ")
                        }
                        val s = (ergebnis as? VisualisierungsErgebnis.Erfolgreich)?.statistik ?: (ergebnis as? VisualisierungsErgebnis.Teilweise)?.statistik
                        val diagnose = listOf("Sichtbarer Weltbereich: " + grenzen(t.sichtBereiche(0.0)),
                            "Berechneter Weltbereich (${if(konfiguration.dimension == RaumDimension.R3) "konservative 3D-Hülle" else "20 % Reserve"}): " + grenzen(c.sampling.sichtBereiche ?: c.bereiche),
                            "Parameterfenster: " + grenzen(konfiguration.bereiche),
                            "Die Reserve verändert keine fachliche Definitionsmenge.") +
                            listOfNotNull(s?.let { "Arbeit ${it.auswertungen}/${minOf(c.sampling.maximalesRasterBudget,c.detailBudget)} · Zellen ${it.zellArbeit} · Punkte ${it.punktArbeit} · Rand ${it.randArbeit}" })
                        val hinweise = diagnose + when (val e = ergebnis) {
                            is VisualisierungsErgebnis.Erfolgreich -> e.hinweise
                            is VisualisierungsErgebnis.Teilweise -> e.hinweise
                            is VisualisierungsErgebnis.BedingtDarstellbar -> listOf(e.grund) + e.bedingungen
                            is VisualisierungsErgebnis.ProjektionErforderlich -> listOf(e.grund)
                            is VisualisierungsErgebnis.NichtDarstellbar -> listOf(e.grund)
                            null -> emptyList()
                        }
                        if (hinweise.isNotEmpty()) {
                            HorizontalDivider()
                            Column(Modifier.widthIn(max = 320.dp).heightIn(max = 220.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                                Text(hinweise.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisualisierungsDetailansicht(knoten: KnotenDaten, auswertung: KnotenAuswertungsErgebnis?,
    eigenschaftenErsetzen: (Map<String, KnotenEigenschaft>) -> Unit, modifier: Modifier = Modifier) {
    val mengen = visualisierungsMengen(knoten, auswertung)
    val menge = mengen.firstOrNull()?.second
    val c = VisualisierungsKonfiguration.aus(knoten.eigenschaften).mitWirksamerDimension(menge)
    VisualisierungsSzene(mengen, c, true, { eigenschaftenErsetzen(it.inEigenschaften(knoten.eigenschaften)) }, modifier)
}

internal fun standardKamera(dimension: RaumDimension) = KameraZustand(
    if (dimension == RaumDimension.R3) 24.0 else 0.0, if (dimension == RaumDimension.R3) -32.0 else 0.0,
    0.0, 0.0, 0.0, 0.0, 1.0)

internal fun kameraEinpassen(ergebnis: VisualisierungsErgebnis?, c: VisualisierungsKonfiguration, breite: Double, höhe: Double): KameraZustand? {
    val e = ergebnis as? VisualisierungsErgebnis.Erfolgreich; val t = ergebnis as? VisualisierungsErgebnis.Teilweise
    val punkte = (e?.punkte ?: t?.punkte.orEmpty()) + (e?.linien ?: t?.linien.orEmpty()).flatMap { it.punkte } +
        (e?.dreiecke ?: t?.dreiecke.orEmpty()).flatMap { listOf(it.a, it.b, it.c) } +
        (e?.intervalle ?: t?.intervalle.orEmpty()).flatMap { listOf(VisualisierungsPunkt(it.von, 0.0), VisualisierungsPunkt(it.bis, 0.0)) } +
        (e?.zellen ?: t?.zellen.orEmpty()).filter { it.status != ZellenStatus.Ausgeschlossen }.flatMap { z ->
            List(1 shl z.minimum.size.coerceAtMost(3)) { index ->
                fun koordinate(a: Int) = if (index and (1 shl a) == 0) z.minimum[a] else z.maximum[a]
                VisualisierungsPunkt(koordinate(0), if (z.minimum.size > 1) koordinate(1) else 0.0, if (z.minimum.size > 2) koordinate(2) else null)
            } }
    if (punkte.isEmpty()) return null
    val neutral = c.copy(kamera = c.kamera.copy(translationX = 0.0, translationY = 0.0, translationZ = 0.0, zoom = 1.0))
    val transformation = VisualisierungsTransformation(neutral, breite, höhe)
    val bild = punkte.map { transformation.bild(it.x, it.y, it.z ?: 0.0) }.filter { it.x.isFinite() && it.y.isFinite() }
    if (bild.isEmpty()) return null
    val minX = bild.minOf { it.x }; val maxX = bild.maxOf { it.x }; val minY = bild.minOf { it.y }; val maxY = bild.maxOf { it.y }
    val zoom = min(breite * .85 / (maxX - minX).coerceAtLeast(10.0), höhe * .85 / (maxY - minY).coerceAtLeast(10.0)).coerceIn(.001, 10_000.0)
    val mitte = transformation.welt((minX + maxX) / 2, (minY + maxY) / 2)
    val mittelBild = transformation.bild(mitte.x, mitte.y, mitte.tiefe)
    return neutral.kamera.copy(zoom = zoom,
        translationX = -(mittelBild.x - breite / 2) / transformation.xMaßstab,
        translationY = (mittelBild.y - höhe / 2) / transformation.maßstab)
}

private class DreieckSortierCache {
    private var kamera: KameraZustand? = null
    private var eingang: List<VisualisierungsDreieck>? = null
    private var sortiert: List<VisualisierungsDreieck> = emptyList()
    fun sortiere(dreiecke: List<VisualisierungsDreieck>, c: VisualisierungsKonfiguration): List<VisualisierungsDreieck> {
        if (c.dimension != RaumDimension.R3) return dreiecke
        if (eingang !== dreiecke || kamera != c.kamera) {
            eingang = dreiecke; kamera = c.kamera
            sortiert = dreiecke.sortedBy { dreieckTiefe(it, c) }
        }
        return sortiert
    }
}

/** Geometrie und Tiefe werden gemeinsam über alle Ebenen sortiert; Stile bleiben austauschbar. */
private class EbenenDreieckSortierCache {
    private var vorher: List<List<VisualisierungsDreieck>> = emptyList()
    private var kamera: KameraZustand? = null
    private var sortiert: List<Pair<Int, VisualisierungsDreieck>> = emptyList()
    fun sortiere(ebenen: List<List<VisualisierungsDreieck>>, c: VisualisierungsKonfiguration): List<Pair<Int, VisualisierungsDreieck>> {
        if (kamera != c.kamera || vorher.size != ebenen.size || vorher.indices.any { vorher[it] !== ebenen[it] }) {
            vorher = ebenen; kamera = c.kamera
            sortiert = ebenen.flatMapIndexed { index, dreiecke -> dreiecke.map { index to it } }.sortedBy { dreieckTiefe(it.second, c) }
        }
        return sortiert
    }
}

private data class VisualisierungsSegment(val von: VisualisierungsPunkt, val zu: VisualisierungsPunkt, val offen: Boolean = false)
private data class VisualisierungsZeichenDaten(
    val zellen: List<VisualisierungsZelle>,
    val punkte: List<VisualisierungsPunkt>,
    val segmente: List<VisualisierungsSegment>,
    val dreiecke: List<VisualisierungsDreieck>,
    val intervalle: List<VisualisierungsIntervall>,
)

internal fun <T> reduziereGleichmäßig(werte: List<T>, maximum: Int): List<T> {
    require(maximum > 0)
    if (werte.size <= maximum) return werte
    if (maximum == 1) return listOf(werte.first())
    return List(maximum) { index ->
        werte[(index.toLong() * werte.lastIndex / (maximum - 1)).toInt()]
    }
}

private fun bereiteZeichenDatenVor(
    ergebnis: VisualisierungsErgebnis?,
): VisualisierungsZeichenDaten {
    val erfolgreich = ergebnis as? VisualisierungsErgebnis.Erfolgreich
    val teilweise = ergebnis as? VisualisierungsErgebnis.Teilweise
    val dreiecke = erfolgreich?.dreiecke ?: teilweise?.dreiecke.orEmpty()
    val linien = erfolgreich?.linien ?: teilweise?.linien.orEmpty()
    val geometriePunkte = buildSet {
        dreiecke.forEach { add(Triple(it.a.x, it.a.y, it.a.z)); add(Triple(it.b.x, it.b.y, it.b.z)); add(Triple(it.c.x, it.c.y, it.c.z)) }
        linien.forEach { linie -> linie.punkte.forEach { add(Triple(it.x, it.y, it.z)) } }
    }
    val allePunkte = erfolgreich?.punkte ?: teilweise?.punkte.orEmpty()
    return VisualisierungsZeichenDaten(
        zellen = erfolgreich?.zellen ?: teilweise?.zellen.orEmpty(),
        punkte = allePunkte.filterNot { it.rasterProbe || Triple(it.x, it.y, it.z) in geometriePunkte },
        segmente = linien.flatMap { linie -> linie.punkte.zipWithNext { a,b -> VisualisierungsSegment(a,b,linie.offenerRand) } },
        dreiecke = dreiecke,
        intervalle = erfolgreich?.intervalle ?: teilweise?.intervalle.orEmpty(),
    )
}

@Composable private fun Plot(
    ergebnis: VisualisierungsErgebnis?,
    konfiguration: VisualisierungsKonfiguration,
    onKamera: (KameraZustand) -> Unit,
    modifier: Modifier,
    interaktiv: Boolean = true,
    interaktionsErgebnis: VisualisierungsErgebnis? = null,
    ebenen: List<Triple<VisualisierungsErgebnis?, VisualisierungsKonfiguration, VisualisierungsErgebnis?>> = emptyList(),
    eingangsIdentität: Any? = null,
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 128)
    var plotGröße by remember { mutableStateOf(IntSize(1, 1)) }
    val aktuelleKonfiguration by rememberUpdatedState(konfiguration)
    val fokus = remember { FocusRequester() }
    var inspektion by remember { mutableStateOf<Offset?>(null) }
    var kamera by remember { mutableStateOf(konfiguration.kamera) }
    var gesteAktiv by remember { mutableStateOf(false) }
    val persistierteKamera by rememberUpdatedState(konfiguration.kamera)
    val aktuelleKameraÜbernehmen by rememberUpdatedState(onKamera)
    LaunchedEffect(konfiguration.kamera, gesteAktiv) {
        if (!gesteAktiv) kamera = konfiguration.kamera
    }
    val datenZustand = remember(ergebnis) { mutableStateOf(VisualisierungsZeichenDaten(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())) }
    LaunchedEffect(ergebnis) { datenZustand.value = withContext(Dispatchers.Default) { bereiteZeichenDatenVor(ergebnis) } }
    val vollständigeZeichenDaten = datenZustand.value
    val interaktionsZustand = remember(interaktionsErgebnis) { mutableStateOf<VisualisierungsZeichenDaten?>(null) }
    LaunchedEffect(interaktionsErgebnis) { if (interaktionsErgebnis != null) interaktionsZustand.value = withContext(Dispatchers.Default) { bereiteZeichenDatenVor(interaktionsErgebnis) } }
    val unSortierteZeichenDaten = if (gesteAktiv) interaktionsZustand.value ?: vollständigeZeichenDaten else vollständigeZeichenDaten
    val ebenenErgebnisse = ebenen.map { it.first to it.third }
    val ebenenDaten = remember(eingangsIdentität) { mutableStateOf<List<Pair<VisualisierungsZeichenDaten, VisualisierungsZeichenDaten?>>>(emptyList()) }
    LaunchedEffect(eingangsIdentität, ebenenErgebnisse) {
        ebenenDaten.value = withContext(Dispatchers.Default) { ebenenErgebnisse.map { (voll, grob) ->
            bereiteZeichenDatenVor(voll) to grob?.let(::bereiteZeichenDatenVor)
        } }
    }
    val sortierCaches = remember(ebenen.size) { List(ebenen.size) { DreieckSortierCache() } }
    val globalerSortierCache = remember { EbenenDreieckSortierCache() }
    val hintergrund = MaterialTheme.colorScheme.surfaceVariant
    val raster = MaterialTheme.colorScheme.outlineVariant
    val rahmen = MaterialTheme.colorScheme.outline
    val beschriftung = MaterialTheme.colorScheme.onSurfaceVariant
    val xAchse = MaterialTheme.colorScheme.primary
    val yAchse = MaterialTheme.colorScheme.secondary
    val zAchse = MaterialTheme.colorScheme.tertiary
    val gesten = if (!interaktiv) Modifier else Modifier.focusRequester(fokus).onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) false else {
            val pan = when(event.key) { Key.DirectionLeft -> Offset(30f, 0f); Key.DirectionRight -> Offset(-30f, 0f)
                Key.DirectionUp -> Offset(0f, 30f); Key.DirectionDown -> Offset(0f, -30f); else -> Offset.Zero }
            val faktor = when(event.key) { Key.Plus, Key.Equals -> 1.25; Key.Minus -> .8; else -> 1.0 }
            if (pan == Offset.Zero && faktor == 1.0) false else {
                aktuelleKameraÜbernehmen(VisualisierungsTransformation(aktuelleKonfiguration.copy(kamera = kamera), plotGröße.width.toDouble(), plotGröße.height.toDouble()).navigiere(
                    pan.x.toDouble(), pan.y.toDouble(), faktor, plotGröße.width / 2.0, plotGröße.height / 2.0)); true
            }
        }
    }.focusable().pointerInput(konfiguration.dimension) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            fokus.requestFocus()
            gesteAktiv = true
            var gestenKamera = kamera
            var bewegt = false
            try {
                do {
                    val ereignis = awaitPointerEvent()
                    val pan = ereignis.calculatePan(); val zoom = ereignis.calculateZoom()
                    val centroid = ereignis.calculateCentroid(useCurrent = false)
                    if (pan != Offset.Zero || zoom != 1f) {
                        bewegt = true
                        inspektion = null
                        val transform = VisualisierungsTransformation(aktuelleKonfiguration.copy(kamera = gestenKamera), size.width.toDouble(), size.height.toDouble())
                        val rotieren = konfiguration.dimension == RaumDimension.R3 && ereignis.changes.count { it.pressed } < 2 && !ereignis.keyboardModifiers.isShiftPressed
                        gestenKamera = transform.navigiere(pan.x.toDouble(), pan.y.toDouble(), zoom.toDouble(), centroid.x.toDouble(), centroid.y.toDouble(), rotieren)
                        kamera = gestenKamera
                        ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (ereignis.changes.any { it.pressed })
                if (bewegt) aktuelleKameraÜbernehmen(gestenKamera) else inspektion = down.position
            } finally { gesteAktiv = false }
        }
    }.pointerInput(konfiguration.dimension) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val p = event.changes.first()
                    val transform = VisualisierungsTransformation(aktuelleKonfiguration.copy(kamera = kamera), size.width.toDouble(), size.height.toDouble())
                    kamera = transform.navigiere(0.0, 0.0, exp(-p.scrollDelta.y * .12), p.position.x.toDouble(), p.position.y.toDouble())
                    aktuelleKameraÜbernehmen(kamera); p.consume()
                }
            }
        }
    }
    Canvas(modifier.clipToBounds().onSizeChanged { plotGröße = IntSize(it.width.coerceAtLeast(1), it.height.coerceAtLeast(1)) }.testTag(if (interaktiv) "visualisierung-plot" else "visualisierung-vorschau").then(gesten)) {
        if (size.width <= 0 || size.height <= 0) return@Canvas
        val darstellungsKonfiguration = konfiguration.copy(kamera = kamera)
        clipRect {
        drawRect(hintergrund)
        when (darstellungsKonfiguration.dimension) {
            RaumDimension.R1 -> zeichneRaster(darstellungsKonfiguration, raster, beschriftung, textMeasurer)
            RaumDimension.R2, RaumDimension.C -> {
                zeichneRaster(darstellungsKonfiguration, raster, beschriftung, textMeasurer)
            }
            RaumDimension.R3 -> if (darstellungsKonfiguration.achsenSichtbar) zeichne3DAchsen(darstellungsKonfiguration, xAchse, yAchse, zAchse, textMeasurer)
        }
        ebenenDaten.value.forEachIndexed { index, (voll, grob) ->
        val unSortierteZeichenDaten = if (gesteAktiv) grob ?: voll else voll
        val darstellungsKonfiguration = ebenen.getOrNull(index)?.second?.copy(kamera = kamera) ?: darstellungsKonfiguration
        val sortierteDreiecke = if (darstellungsKonfiguration.dimension == RaumDimension.R3) emptyList() else sortierCaches[index].sortiere(unSortierteZeichenDaten.dreiecke, darstellungsKonfiguration)
        zeichneZellen(unSortierteZeichenDaten.zellen, darstellungsKonfiguration)
        if (darstellungsKonfiguration.dimension == RaumDimension.R1) {
            unSortierteZeichenDaten.intervalle.forEach { zeichneIntervall(it, darstellungsKonfiguration, farbeFür(null, darstellungsKonfiguration)) }
        }
        val zeichnetDreieckKanten = !gesteAktiv && sortierteDreiecke.size <= 1_500
        if (darstellungsKonfiguration.dimension != RaumDimension.R3)
            sortierteDreiecke.forEach { zeichneDreieck(it, darstellungsKonfiguration, zeichnetDreieckKanten) }
        if (darstellungsKonfiguration.farbe.modus != FarbModus.Spektrum) {
            listOf(false, true).forEach { offen ->
            val pfad = Path()
            unSortierteZeichenDaten.segmente.filter { it.offen == offen }.forEach { segment ->
                val a = projekt(segment.von, darstellungsKonfiguration, size.width, size.height)
                val b = projekt(segment.zu, darstellungsKonfiguration, size.width, size.height)
                if (sichtbar(a, b)) { pfad.moveTo(a.x,a.y); pfad.lineTo(b.x,b.y) }
            }
            drawPath(pfad,farbeFür(null,darstellungsKonfiguration),style=Stroke(2.2.dp.toPx(), pathEffect=if(offen) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),4.dp.toPx())) else null))
            }
        } else {
        unSortierteZeichenDaten.segmente.forEach { segment ->
            val a = projekt(segment.von, darstellungsKonfiguration, size.width, size.height)
            val b = projekt(segment.zu, darstellungsKonfiguration, size.width, size.height)
            if (!sichtbar(a, b)) return@forEach
            drawLine(
                farbeFür(segment.von.farbwert ?: segment.zu.farbwert, darstellungsKonfiguration),
                a, b,
                2.2.dp.toPx(),
            )
        }
        }
        unSortierteZeichenDaten.punkte.forEach { punkt ->
            val projektion = projekt(punkt, darstellungsKonfiguration, size.width, size.height)
            if (!projektion.x.isFinite() || !projektion.y.isFinite() || projektion.x < -10 || projektion.y < -10 || projektion.x > size.width + 10 || projektion.y > size.height + 10) return@forEach
            val radius = when (darstellungsKonfiguration.dimension) {
                RaumDimension.R1 -> 5.dp.toPx()
                RaumDimension.R2, RaumDimension.C -> 2.2.dp.toPx()
                RaumDimension.R3 -> 2.8.dp.toPx()
            }
            drawCircle(farbeFür(punkt.farbwert, darstellungsKonfiguration), radius, projektion)
            if (punkt.weitereFarbwerte.isNotEmpty()) {
                drawCircle(Color.Magenta.copy(alpha = 0.85f), radius + 2.5f, projektion, style = Stroke(1.5f))
            }
        }
        }
        if (darstellungsKonfiguration.dimension == RaumDimension.R3) {
            val dreiecksEbenen = ebenenDaten.value.map { (voll,grob) -> (if(gesteAktiv) grob ?: voll else voll).dreiecke }
            val sortiert = globalerSortierCache.sortiere(dreiecksEbenen, darstellungsKonfiguration)
            sortiert.forEach { (index,dreieck) ->
                val c = ebenen.getOrNull(index)?.second?.copy(kamera=kamera) ?: darstellungsKonfiguration
                zeichneDreieck(dreieck,c,!gesteAktiv && sortiert.size <= 1500)
            }
        }
        if (interaktiv && inspektion != null) zeichneInspektion(inspektion!!, unSortierteZeichenDaten, darstellungsKonfiguration, textMeasurer, beschriftung)
        }
        drawRect(rahmen, style = Stroke(1f))
    }
}

private fun DrawScope.sichtbar(a: Offset, b: Offset, c: Offset = b): Boolean =
    a.x.isFinite() && a.y.isFinite() && b.x.isFinite() && b.y.isFinite() && c.x.isFinite() && c.y.isFinite() &&
        maxOf(a.x,b.x,c.x) >= 0 && minOf(a.x,b.x,c.x) <= size.width && maxOf(a.y,b.y,c.y) >= 0 && minOf(a.y,b.y,c.y) <= size.height

private fun DrawScope.zeichneZellen(zellen: List<VisualisierungsZelle>, c: VisualisierungsKonfiguration) {
    if (c.dimension != RaumDimension.R2 && c.dimension != RaumDimension.C) { zellen.forEach { zeichneZelle(it, c) }; return }
    ZellenStatus.entries.filter { it != ZellenStatus.Ausgeschlossen }.forEach { status ->
        val pfad = Path()
        zellen.asSequence().filter { it.status == status && it.minimum.size == 2 }.forEach { z ->
            val a = projekt(VisualisierungsPunkt(z.minimum[0], z.minimum[1]), c, size.width, size.height)
            val b = projekt(VisualisierungsPunkt(z.maximum[0], z.maximum[1]), c, size.width, size.height)
            val links = min(a.x,b.x).coerceAtLeast(0f); val rechts = max(a.x,b.x).coerceAtMost(size.width)
            val oben = min(a.y,b.y).coerceAtLeast(0f); val unten = max(a.y,b.y).coerceAtMost(size.height)
            if (links < rechts && oben < unten) pfad.addRect(androidx.compose.ui.geometry.Rect(links,oben,rechts,unten))
        }
        val enthalten = status == ZellenStatus.Enthalten || status == ZellenStatus.NumerischEnthalten
        val farbe = if (enthalten) farbeFür(null,c) else if (status == ZellenStatus.Gemischt) Color(0xFFF59E0B) else Color(0xFF6B7280)
        val art = if (enthalten) c.aktiverStil.füllArt else FüllArt.Schraffur
        if (art == FüllArt.Ohne) return@forEach
        if (art == FüllArt.Vollfläche) drawPath(pfad,farbe.copy(alpha=c.aktiverStil.deckkraft.toFloat()))
        else clipPath(pfad) {
            val abstand = 12.dp.toPx()
            val muster = Path()
            if (art == FüllArt.Hexagonal) {
                val r = abstand; val h = sqrt(3f)*r
                var spalte = -1
                while (spalte*r*1.5f <= size.width+r) {
                    val x = spalte*r*1.5f; var y = if (spalte % 2 == 0) -h else -h/2
                    while (y <= size.height+h) {
                        repeat(6) { i -> val winkel=PI*i/3; val px=x+r*cos(winkel).toFloat(); val py=y+r*sin(winkel).toFloat()
                            if(i==0) muster.moveTo(px,py) else muster.lineTo(px,py) }; muster.close(); y+=h
                    }; spalte++
                }
            } else {
                var x = -size.height
                while(x <= size.width) { muster.moveTo(x,size.height);muster.lineTo(x+size.height,0f)
                    if(art==FüllArt.Kreuzschraffur) {muster.moveTo(x,0f);muster.lineTo(x+size.height,size.height)}; x+=abstand }
            }
            drawPath(muster,farbe.copy(alpha=if(enthalten) c.aktiverStil.deckkraft.toFloat() else .6f),style=Stroke(1.dp.toPx()))
        }

    }
}

private fun DrawScope.zeichneZelle(zelle: VisualisierungsZelle, c: VisualisierungsKonfiguration) {
    if (zelle.status == ZellenStatus.Ausgeschlossen || zelle.minimum.size != c.dimension.raumDimension) return
    val farbe = when (zelle.status) {
        ZellenStatus.Enthalten -> Color(0xFF2563EB)
        ZellenStatus.NumerischEnthalten -> Color(0xFF38BDF8)
        ZellenStatus.Gemischt -> Color(0xFFF59E0B)
        ZellenStatus.Unbekannt -> Color(0xFF6B7280)
        ZellenStatus.Ausgeschlossen -> return
    }
    if (c.dimension == RaumDimension.R3) {
        val mitte = zelle.minimum.zip(zelle.maximum) { a, b -> (a + b) / 2.0 }
        val ecken = List(8) { maske ->
            VisualisierungsPunkt(
                if (maske and 1 == 0) zelle.minimum[0] else zelle.maximum[0],
                if (maske and 2 == 0) zelle.minimum[1] else zelle.maximum[1],
                if (maske and 4 == 0) zelle.minimum[2] else zelle.maximum[2],
            )
        }
        if (zelle.status == ZellenStatus.Enthalten || zelle.status == ZellenStatus.NumerischEnthalten) {
            val flächen = listOf(
                intArrayOf(0, 2, 6, 4), intArrayOf(1, 5, 7, 3),
                intArrayOf(0, 4, 5, 1), intArrayOf(2, 3, 7, 6),
                intArrayOf(0, 1, 3, 2), intArrayOf(4, 6, 7, 5),
            )
            flächen
                .sortedBy { fläche -> fläche.map { kameraKoordinate(ecken[it], c).third }.average() }
                .forEach { fläche ->
                    val pfad = Path().apply {
                        val erster = projekt(ecken[fläche[0]], c, size.width, size.height)
                        moveTo(erster.x, erster.y)
                        fläche.drop(1).forEach { index ->
                            val punkt = projekt(ecken[index], c, size.width, size.height)
                            lineTo(punkt.x, punkt.y)
                        }
                        close()
                    }
                    drawPath(pfad, farbe.copy(alpha = if (zelle.status == ZellenStatus.Enthalten) 0.08f else 0.045f))
                }
        }
        val projizierteEcken = ecken.map { projekt(it, c, size.width, size.height) }
        repeat(8) { maske ->
            repeat(3) { achse ->
                val bit = 1 shl achse
                if (maske and bit == 0) {
                    drawLine(farbe.copy(alpha = 0.38f), projizierteEcken[maske], projizierteEcken[maske or bit], 1f)
                }
            }
        }
        val punkt = VisualisierungsPunkt(mitte[0], mitte[1], mitte[2])
        drawCircle(
            farbe.copy(alpha = if (zelle.status == ZellenStatus.Enthalten || zelle.status == ZellenStatus.NumerischEnthalten) 0.35f else 0.7f),
            if (zelle.status == ZellenStatus.Enthalten || zelle.status == ZellenStatus.NumerischEnthalten) 2.0f else 3.5f,
            projekt(punkt, c, size.width, size.height),
        )
        return
    }
    if (c.dimension == RaumDimension.R1) {
        val von = projekt(VisualisierungsPunkt(zelle.minimum[0], 0.0), c, size.width, size.height).x
        val bis = projekt(VisualisierungsPunkt(zelle.maximum[0], 0.0), c, size.width, size.height).x
        val links = min(von, bis).coerceAtLeast(0f)
        val rechts = max(von, bis).coerceAtMost(size.width)
        val y = size.height / 2f
        drawLine(
            farbe.copy(alpha = if (zelle.status == ZellenStatus.Enthalten || zelle.status == ZellenStatus.NumerischEnthalten) 0.55f else 0.4f),
            Offset(links, y),
            Offset(rechts, y),
            7.dp.toPx(),
        )
        if (zelle.status != ZellenStatus.Enthalten && zelle.status != ZellenStatus.NumerischEnthalten) {
            var x = links
            val halbeHöhe = 6.dp.toPx()
            val abstand = 8.dp.toPx()
            while (x <= rechts) {
                drawLine(farbe.copy(alpha = 0.85f), Offset(x, y - halbeHöhe), Offset(x, y + halbeHöhe), 1f)
                x += abstand
            }
        }
        return
    }
    val a = projekt(VisualisierungsPunkt(zelle.minimum[0], zelle.minimum[1]), c, size.width, size.height)
    val b = projekt(VisualisierungsPunkt(zelle.maximum[0], zelle.maximum[1]), c, size.width, size.height)
    val links = min(a.x, b.x).coerceAtLeast(0f)
    val rechts = max(a.x, b.x).coerceAtMost(size.width)
    val oben = min(a.y, b.y).coerceAtLeast(0f)
    val unten = max(a.y, b.y).coerceAtMost(size.height)
    if (rechts <= links || unten <= oben) return
    val deckkraft = when (zelle.status) {
        ZellenStatus.Enthalten -> 0.22f
        ZellenStatus.NumerischEnthalten -> 0.14f
        else -> 0.1f
    }
    drawRect(farbe.copy(alpha = deckkraft), Offset(links, oben), androidx.compose.ui.geometry.Size(rechts - links, unten - oben))
    if (zelle.status != ZellenStatus.Enthalten && zelle.status != ZellenStatus.NumerischEnthalten) {
        clipRect(links, oben, rechts, unten) {
            var x = links - (unten - oben)
            val abstand = 8.dp.toPx()
            while (x < rechts) {
                drawLine(farbe.copy(alpha = 0.75f), Offset(x, unten), Offset(x + (unten - oben), oben), 1f)
                x += abstand
            }
        }
    }
}

private val RaumDimension.raumDimension: Int
    get() = when (this) {
        RaumDimension.R1 -> 1
        RaumDimension.R2, RaumDimension.C -> 2
        RaumDimension.R3 -> 3
    }

private fun DrawScope.text(text: String, x: Float, y: Float, farbe: Color, measurer: TextMeasurer) {
    val layout = measurer.measure(text, TextStyle(color = farbe, fontSize = 12.sp))
    drawText(layout, topLeft = Offset(x.coerceIn(2f, (size.width - layout.size.width - 2).coerceAtLeast(2f)),
        y.coerceIn(2f, (size.height - layout.size.height - 2).coerceAtLeast(2f))))
}

private fun DrawScope.zeichneRaster(c: VisualisierungsKonfiguration, raster: Color, farbe: Color, measurer: TextMeasurer) {
    val t = VisualisierungsTransformation(c, size.width.toDouble(), size.height.toDouble())
    val bereiche = t.sichtBereiche(0.0)
    val nullpunkt = projekt(VisualisierungsPunkt(0.0, 0.0), c, size.width, size.height)
    val schritt = achsenSchritt(1 / if (c.dimension == RaumDimension.R1) t.xMaßstab else t.maßstab, 80.dp.toPx().toDouble())
    val hauptRaster = Path(); val nebenRaster = Path()
    val labels = mutableListOf<Pair<String,Offset>>()
    fun achse(b: ZahlenBereich, horizontal: Boolean) {
        val fein = schritt / 5
        val anzahl = ((b.maximum - b.minimum) / fein).toInt().coerceIn(0, 500)
        val start = ceil(b.minimum / fein)
        repeat(anzahl + 1) { index ->
            val nummer = start + index; val wert = nummer * fein
            val p = if (horizontal) projekt(VisualisierungsPunkt(wert, 0.0), c, size.width, size.height) else projekt(VisualisierungsPunkt(0.0, wert), c, size.width, size.height)
            val haupt = abs(nummer % 5) < .001
            if (c.rasterSichtbar && c.dimension != RaumDimension.R1) {
                val pfad = if (haupt) hauptRaster else nebenRaster
                if (horizontal) { pfad.moveTo(p.x,0f);pfad.lineTo(p.x,size.height) } else { pfad.moveTo(0f,p.y);pfad.lineTo(size.width,p.y) }
            }
            if (haupt && c.achsenSichtbar) {
                val label = if (abs(wert) < schritt * 1e-6) "0" else if (abs(wert) >= 1e5 || abs(wert) < .001) "%.1e".format(java.util.Locale.ROOT, wert) else "%.6f".format(java.util.Locale.ROOT, wert).trimEnd('0').trimEnd('.')
                if (horizontal) labels += label to Offset(p.x + 3.dp.toPx(), nullpunkt.y + 3.dp.toPx())
                else if (abs(wert) > schritt * 1e-6) labels += label to Offset(nullpunkt.x + 3.dp.toPx(), p.y)
            }
        }
    }
    achse(bereiche.x, true)
    if (c.dimension != RaumDimension.R1) achse(bereiche.y, false)
    drawPath(nebenRaster,raster.copy(alpha=.24f),style=Stroke(1.dp.toPx()))
    drawPath(hauptRaster,raster.copy(alpha=.65f),style=Stroke(1.dp.toPx()))
    labels.forEach { (label,p) -> text(label,p.x,p.y,farbe,measurer) }
    if (c.achsenSichtbar) {
        drawLine(farbe, Offset(0f, nullpunkt.y), Offset(size.width, nullpunkt.y), 1.dp.toPx())
        if (c.dimension != RaumDimension.R1) drawLine(farbe, Offset(nullpunkt.x, 0f), Offset(nullpunkt.x, size.height), 1.dp.toPx())
        text(c.achsen.x, size.width - 24.dp.toPx(), nullpunkt.y - 22.dp.toPx(), farbe, measurer)
        if (c.dimension != RaumDimension.R1) text(c.achsen.y, nullpunkt.x + 6.dp.toPx(), 3.dp.toPx(), farbe, measurer)
    }
}

private fun DrawScope.zeichneIntervall(intervall: VisualisierungsIntervall, c: VisualisierungsKonfiguration, farbe: Color) {
    val y = size.height / 2f
    val links = projekt(VisualisierungsPunkt(intervall.von, 0.0), c, size.width, size.height)
    val rechts = projekt(VisualisierungsPunkt(intervall.bis, 0.0), c, size.width, size.height)
    drawLine(farbe, Offset(links.x, y), Offset(rechts.x, y), 5f)
    val radius = 5.dp.toPx()
    fun ende(position: Offset, geschlossen: Boolean, fensterrand: Boolean, richtung: Float) {
        val punkt = Offset(position.x, y)
        if (fensterrand) {
            drawLine(farbe, punkt, Offset(punkt.x - richtung * radius, punkt.y - radius), 2f)
            drawLine(farbe, punkt, Offset(punkt.x - richtung * radius, punkt.y + radius), 2f)
        } else if (geschlossen) {
            drawCircle(farbe, radius, punkt)
        } else {
            drawCircle(farbe, radius, punkt, style = Stroke(2f))
        }
    }
    ende(links, intervall.linksGeschlossen, intervall.linksAmFensterrand, -1f)
    ende(rechts, intervall.rechtsGeschlossen, intervall.rechtsAmFensterrand, 1f)
}

private fun DrawScope.zeichne3DAchsen(c: VisualisierungsKonfiguration, xFarbe: Color, yFarbe: Color, zFarbe: Color, measurer: TextMeasurer) {
    val zBereich = c.bereiche.z ?: return
    val xStart = projekt(VisualisierungsPunkt(c.bereiche.x.minimum, 0.0, 0.0), c, size.width, size.height)
    val xEnde = projekt(VisualisierungsPunkt(c.bereiche.x.maximum, 0.0, 0.0), c, size.width, size.height)
    val yStart = projekt(VisualisierungsPunkt(0.0, c.bereiche.y.minimum, 0.0), c, size.width, size.height)
    val yEnde = projekt(VisualisierungsPunkt(0.0, c.bereiche.y.maximum, 0.0), c, size.width, size.height)
    val zStart = projekt(VisualisierungsPunkt(0.0, 0.0, zBereich.minimum), c, size.width, size.height)
    val zEnde = projekt(VisualisierungsPunkt(0.0, 0.0, zBereich.maximum), c, size.width, size.height)

    drawLine(xFarbe.copy(alpha = 0.78f), xStart, xEnde, 2f)
    drawLine(yFarbe.copy(alpha = 0.78f), yStart, yEnde, 2f)
    drawLine(zFarbe.copy(alpha = 0.78f), zStart, zEnde, 2f)

    text(c.achsen.x.ifBlank { "x" }, xEnde.x, xEnde.y, xFarbe, measurer)
    text(c.achsen.y.ifBlank { "y" }, yEnde.x, yEnde.y, yFarbe, measurer)
    text(c.achsen.z?.ifBlank { "z" } ?: "z", zEnde.x, zEnde.y, zFarbe, measurer)
}

private fun DrawScope.zeichneDreieck(
    dreieck: VisualisierungsDreieck,
    c: VisualisierungsKonfiguration,
    kanteZeichnen: Boolean,
) {
    if (c.farbe.modus == FarbModus.Keine) return
    val pa = projekt(dreieck.a, c, size.width, size.height)
    val pb = projekt(dreieck.b, c, size.width, size.height)
    val pc = projekt(dreieck.c, c, size.width, size.height)
    if (!sichtbar(pa, pb, pc)) return
    val pfad = Path().apply {
        moveTo(pa.x, pa.y)
        lineTo(pb.x, pb.y)
        lineTo(pc.x, pc.y)
        close()
    }
    val a = kameraKoordinate(dreieck.a, c)
    val b = kameraKoordinate(dreieck.b, c)
    val d = kameraKoordinate(dreieck.c, c)
    val ux = b.first - a.first; val uy = b.second - a.second; val uz = b.third - a.third
    val vx = d.first - a.first; val vy = d.second - a.second; val vz = d.third - a.third
    val nx = uy * vz - uz * vy; val ny = uz * vx - ux * vz; val nz = ux * vy - uy * vx
    val länge = sqrt(nx * nx + ny * ny + nz * nz)
    val licht = if (länge <= 1e-12) 0.7 else (0.35 + 0.65 * abs((nx * 0.35 - ny * 0.45 + nz * 0.82) / länge)).coerceIn(0.28, 1.0)
    val farbwert = listOfNotNull(dreieck.a.farbwert, dreieck.b.farbwert, dreieck.c.farbwert).takeIf { it.isNotEmpty() }?.average()
    val basis = farbeFür(farbwert, c)
    val alpha = when (dreieck.nachweis) {
        DarstellungsNachweis.Exakt -> 0.92f
        DarstellungsNachweis.Bewiesen -> 0.84f
        DarstellungsNachweis.Numerisch -> 0.68f
        DarstellungsNachweis.Gemischt -> 0.46f
        DarstellungsNachweis.Unbekannt -> 0.28f
    }
    val schattiert = Color(
        red = (basis.red * licht.toFloat()).coerceIn(0f, 1f),
        green = (basis.green * licht.toFloat()).coerceIn(0f, 1f),
        blue = (basis.blue * licht.toFloat()).coerceIn(0f, 1f),
        alpha = alpha,
    )
    drawPath(pfad, schattiert)
    if (kanteZeichnen) {
        drawPath(pfad, schattiert.copy(alpha = min(0.72f, alpha + 0.1f)), style = Stroke(0.65f))
    }
}

private fun dreieckTiefe(dreieck: VisualisierungsDreieck, c: VisualisierungsKonfiguration): Double =
    listOf(dreieck.a, dreieck.b, dreieck.c).map { kameraKoordinate(it, c).third }.average()

private fun kameraKoordinate(p: VisualisierungsPunkt, c: VisualisierungsKonfiguration): Triple<Double, Double, Double> {
    var x = p.x + c.kamera.translationX
    var y = p.y + c.kamera.translationY
    var z = (p.z ?: 0.0) + c.kamera.translationZ
    if (c.dimension == RaumDimension.R3) {
        val ry = c.kamera.rotationY * Math.PI / 180
        val rx = c.kamera.rotationX * Math.PI / 180
        val rz = c.kamera.rotationZ * Math.PI / 180

        val xNachY = x * cos(ry) + z * sin(ry)
        val zNachY = -x * sin(ry) + z * cos(ry)
        x = xNachY
        z = zNachY

        val yNachX = y * cos(rx) - z * sin(rx)
        val zNachX = y * sin(rx) + z * cos(rx)
        y = yNachX
        z = zNachX

        val xNachZ = x * cos(rz) - y * sin(rz)
        val yNachZ = x * sin(rz) + y * cos(rz)
        x = xNachZ
        y = yNachZ
    }
    return Triple(x, y, z)
}

private val projektionsCache = ThreadLocal<VisualisierungsTransformation>()

private fun projekt(p: VisualisierungsPunkt, c: VisualisierungsKonfiguration, breite: Float, höhe: Float): Offset {
    var t = projektionsCache.get()
    if (t == null || t.konfiguration != c || t.breite != breite.toDouble() || t.höhe != höhe.toDouble()) {
        t = VisualisierungsTransformation(c, breite.toDouble(), höhe.toDouble()); projektionsCache.set(t)
    }
    val bild = t.bild(p.x, p.y, p.z ?: 0.0)
    return Offset(bild.x.toFloat(), bild.y.toFloat())
}
private fun farbeFür(wert: Double?, c: VisualisierungsKonfiguration): Color {
    if (c.farbe.modus == FarbModus.Keine) return Color.Transparent
    if (c.farbe.modus != FarbModus.Spektrum || wert == null) return Color(c.farbe.festeFarbe ?: 0xFF2563EB)
    val b = c.farbe.bereich ?: ZahlenBereich(-1.0, 1.0); val t = ((wert - b.minimum) / (b.maximum - b.minimum)).coerceIn(0.0, 1.0).toFloat()
    return when (c.farbe.palette) {
        "Sonnenuntergang" -> Color(0.95f, 0.18f + .6f * t, 0.12f + .55f * t)
        "Wald" -> Color(0.08f + .35f * t, 0.25f + .6f * t, 0.16f + .2f * t)
        else -> Color(0.1f + .8f * t, 0.25f, 0.9f - .7f * t)
    }
}

private fun raumName(dimension: RaumDimension): String = when (dimension) {
    RaumDimension.R1 -> "R¹"
    RaumDimension.R2 -> "R²"
    RaumDimension.R3 -> "R³"
    RaumDimension.C -> "ℂ"
}

private fun DrawScope.zeichneInspektion(cursor: Offset, daten: VisualisierungsZeichenDaten, c: VisualisierungsKonfiguration, measurer: TextMeasurer, farbe: Color) {
    val t = VisualisierungsTransformation(c, size.width.toDouble(), size.height.toDouble())
    var treffer: VisualisierungsPunkt? = null
    var abstand = 18.dp.toPx().toDouble()
    fun prüfe(p: VisualisierungsPunkt) {
        val b = t.bild(p.x, p.y, p.z ?: 0.0)
        val distanz = hypot(b.x - cursor.x, b.y - cursor.y)
        if (distanz < abstand) { abstand = distanz; treffer = p }
    }
    daten.punkte.forEach(::prüfe)
    daten.segmente.forEach { s ->
        val a = t.bild(s.von.x, s.von.y, s.von.z ?: 0.0); val b = t.bild(s.zu.x, s.zu.y, s.zu.z ?: 0.0)
        val dx = b.x - a.x; val dy = b.y - a.y
        val f = (((cursor.x - a.x) * dx + (cursor.y - a.y) * dy) / (dx * dx + dy * dy).coerceAtLeast(1e-12)).coerceIn(0.0, 1.0)
        prüfe(VisualisierungsPunkt(s.von.x + (s.zu.x - s.von.x) * f, s.von.y + (s.zu.y - s.von.y) * f,
            s.von.z?.let { z -> z + ((s.zu.z ?: z) - z) * f }))
    }
    var besteTiefe = Double.NEGATIVE_INFINITY
    daten.dreiecke.forEach { d ->
        val a = t.bild(d.a.x, d.a.y, d.a.z ?: 0.0); val b = t.bild(d.b.x, d.b.y, d.b.z ?: 0.0); val p = t.bild(d.c.x, d.c.y, d.c.z ?: 0.0)
        val det = (b.y - p.y) * (a.x - p.x) + (p.x - b.x) * (a.y - p.y)
        if (abs(det) > 1e-12) {
            val u = ((b.y - p.y) * (cursor.x - p.x) + (p.x - b.x) * (cursor.y - p.y)) / det
            val v = ((p.y - a.y) * (cursor.x - p.x) + (a.x - p.x) * (cursor.y - p.y)) / det
            val w = 1 - u - v; val tiefe = u * a.tiefe + v * b.tiefe + w * p.tiefe
            if (u >= 0 && v >= 0 && w >= 0 && tiefe > besteTiefe) {
                besteTiefe = tiefe
                treffer = VisualisierungsPunkt(u * d.a.x + v * d.b.x + w * d.c.x, u * d.a.y + v * d.b.y + w * d.c.y,
                    if (c.dimension == RaumDimension.R3) u * (d.a.z ?: 0.0) + v * (d.b.z ?: 0.0) + w * (d.c.z ?: 0.0) else null)
            }
        }
    }
    val welt = treffer?.let { BildPunkt(it.x, it.y, it.z ?: 0.0) } ?: t.welt(cursor.x.toDouble(), cursor.y.toDouble())
    drawLine(farbe.copy(alpha = .5f), Offset(cursor.x, 0f), Offset(cursor.x, size.height), 1.dp.toPx())
    drawLine(farbe.copy(alpha = .5f), Offset(0f, cursor.y), Offset(size.width, cursor.y), 1.dp.toPx())
    fun zahl(v: Double) = "%.5g".format(java.util.Locale.ROOT, v)
    val label = if (c.dimension == RaumDimension.R3 && treffer == null) "Kein Geometrietreffer" else
        (if (treffer != null) "Darstellung ≈ " else "Cursor: ") + listOfNotNull(zahl(welt.x),
            if (c.dimension == RaumDimension.R1) null else zahl(welt.y), if (c.dimension == RaumDimension.R3) zahl(welt.tiefe) else null).joinToString("; ")
    text(label, 8.dp.toPx(), 8.dp.toPx(), farbe, measurer)
}

internal fun visualisierungsMengen(knoten: KnotenDaten, auswertung: KnotenAuswertungsErgebnis?): List<Pair<String, MengenAusdruck>> =
    knoten.anschlüsse.filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        .mapNotNull { a -> (auswertung?.eingänge?.get(a.name)?.objekt as? MengenAusdruck)?.let { a.name to it } }
