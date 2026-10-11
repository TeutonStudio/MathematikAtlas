package de.TeutonStudio.MathematikKnoten.visualisierung.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import kotlin.math.*
import kotlin.coroutines.coroutineContext

private enum class VisualisierungsCacheQualität { Vorschau, Detail }

private data class VisualisierungsCacheSchlüssel(
    val knotenId: String,
    val mengenWert: Any?,
    val samplingSignatur: List<Any?>,
    val qualität: VisualisierungsCacheQualität,
)

/** Kleiner prozessweiter LRU-Cache; verhindert Resampling beim Scrollen und erneuten Öffnen. */
private object VisualisierungsErgebnisCache {
    private const val MAXIMALE_EINTRÄGE = 8
    private val ergebnisse = object : LinkedHashMap<VisualisierungsCacheSchlüssel, VisualisierungsErgebnis>(MAXIMALE_EINTRÄGE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<VisualisierungsCacheSchlüssel, VisualisierungsErgebnis>?): Boolean =
            size > MAXIMALE_EINTRÄGE
    }

    @Synchronized fun lese(schlüssel: VisualisierungsCacheSchlüssel): VisualisierungsErgebnis? = ergebnisse[schlüssel]
    @Synchronized fun speichere(schlüssel: VisualisierungsCacheSchlüssel, ergebnis: VisualisierungsErgebnis) {
        ergebnisse[schlüssel] = ergebnis
    }
}

/** Compose-Renderer mit abbrechbarem Hintergrundsampling und lokaler Kamerasteuerung. */
class VisualisierungsKnotenRenderer(
    private val ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis?,
) : KnotenRenderer {
    override val interaktionsModus = KnotenInteraktionsModus.NurKopfzeileZiehbar

    @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        val mengenWert = ergebnisFür(knoten)?.eingänge?.get("menge")
        val menge = mengenWert?.objekt as? MengenAusdruck
        var konfiguration by remember(knoten.id, knoten.eigenschaften) { mutableStateOf(VisualisierungsKonfiguration.aus(knoten.eigenschaften)) }
        fun ändern(neu: VisualisierungsKonfiguration) {
            konfiguration = neu
            aktionen.eigenschaftenErsetzen(neu.zuEigenschaften())
        }
        val wirksameKonfiguration = konfiguration.mitWirksamerDimension(menge)
        val vorschauKonfiguration = wirksameKonfiguration.alsVorschau()
        val vorschauSchlüssel = VisualisierungsCacheSchlüssel(
            knotenId = knoten.id.wert,
            mengenWert = mengenWert,
            samplingSignatur = vorschauKonfiguration.samplingSignatur(),
            qualität = VisualisierungsCacheQualität.Vorschau,
        )
        val ergebnis by erinnertesVisualisierungsErgebnis(vorschauSchlüssel, menge, vorschauKonfiguration, verzögerungMillis = 140)
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth().height(34.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Visualisierung", style = MaterialTheme.typography.titleMedium)
                Text(raumName(wirksameKonfiguration.dimension), style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Plot(
                    ergebnis = ergebnis,
                    konfiguration = vorschauKonfiguration,
                    onKamera = { ändern(konfiguration.copy(kamera = it)) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Steuerung(wirksameKonfiguration, { neu -> ändern(konfiguration.copy(kamera = neu.kamera)) }, Modifier.width(128.dp).fillMaxHeight())
            }
            Text(
                "Vorschau · Knoten halten für Details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LatexText(legende(wirksameKonfiguration), style = MaterialTheme.typography.bodySmall)
            val statistik = when (val wert = ergebnis) {
                is VisualisierungsErgebnis.Erfolgreich -> wert.statistik
                is VisualisierungsErgebnis.Teilweise -> wert.statistik
                else -> null
            }
            if (statistik != null && statistik.auswertungen > 0) {
                Text(
                    buildString {
                        append("Auswertungen ${statistik.auswertungen}/${vorschauKonfiguration.sampling.maximalesRasterBudget}")
                        append(" · ${statistik.stützpunkte} Stützpunkte")
                        if (statistik.linien > 0) append(" · ${statistik.linien} Segmente")
                        if (statistik.dreiecke > 0) append(" · ${statistik.dreiecke} Dreiecke")
                        if (statistik.zellen > 0) append(" · ${statistik.zellen} Zellen")
                        if (statistik.budgetErschöpft) append(" · Budget ausgeschöpft")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (statistik.budgetErschöpft) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val annahmen = mengenWert?.annahmen.orEmpty()
            if (annahmen.isNotEmpty()) {
                LatexText(
                    "Annahmen: " + annahmen.joinToString(", ") { it.zuLatex() },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            val hatZellen = when (val wert = ergebnis) {
                is VisualisierungsErgebnis.Erfolgreich -> wert.zellen.isNotEmpty()
                is VisualisierungsErgebnis.Teilweise -> wert.zellen.isNotEmpty()
                else -> false
            }
            if (hatZellen) {
                Text(
                    "Zellen: Blau = bewiesen enthalten, Hellblau = numerisch enthalten, Orange = gemischt, Grau schraffiert beziehungsweise als Drahtzelle = unbestimmt.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (val wert = ergebnis) {
                is VisualisierungsErgebnis.NichtDarstellbar -> Text(wert.grund, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                is VisualisierungsErgebnis.BedingtDarstellbar -> Text(
                    listOf(wert.grund, wert.bedingungen.joinToString()).filter(String::isNotBlank).joinToString(" "),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
                is VisualisierungsErgebnis.ProjektionErforderlich -> Text(
                    wert.grund,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
                is VisualisierungsErgebnis.Teilweise -> Text(wert.hinweise.joinToString(" "), style = MaterialTheme.typography.labelSmall)
                is VisualisierungsErgebnis.Erfolgreich -> if (wert.hinweise.isNotEmpty()) {
                    Text(wert.hinweise.joinToString(" "), style = MaterialTheme.typography.labelSmall)
                } else if (wert.istApproximation) {
                    Text("Numerische Approximation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                null -> Text("Berechne Darstellung …", style = MaterialTheme.typography.labelSmall)
            }
        }

    }
}

@Composable
private fun erinnertesVisualisierungsErgebnis(
    schlüssel: VisualisierungsCacheSchlüssel,
    menge: MengenAusdruck?,
    konfiguration: VisualisierungsKonfiguration,
    verzögerungMillis: Long = 0,
): State<VisualisierungsErgebnis?> {
    val gecacht = remember(schlüssel) { VisualisierungsErgebnisCache.lese(schlüssel) }
    return produceState(gecacht, schlüssel) {
        if (value != null) return@produceState
        val berechnet = if (menge == null) {
            VisualisierungsErgebnis.NichtDarstellbar("Verbinde eine Menge mit dem Eingang.")
        } else {
            if (verzögerungMillis > 0) delay(verzögerungMillis)
            withContext(Dispatchers.Default) {
                VisualisierungsSampler.sample(menge, konfiguration) { coroutineContext.ensureActive() }
            }
        }
        VisualisierungsErgebnisCache.speichere(schlüssel, berechnet)
        value = berechnet
    }
}

@Composable
fun VisualisierungsDetailansicht(
    knoten: KnotenDaten,
    auswertung: KnotenAuswertungsErgebnis?,
    eigenschaftenErsetzen: (Map<String, KnotenEigenschaft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mengenWert = auswertung?.eingänge?.get("menge")
    val menge = mengenWert?.objekt as? MengenAusdruck
    val konfiguration = remember(knoten.id, knoten.eigenschaften, menge) {
        VisualisierungsKonfiguration.aus(knoten.eigenschaften).mitWirksamerDimension(menge)
    }
    val detailSchlüssel = VisualisierungsCacheSchlüssel(
        knotenId = knoten.id.wert,
        mengenWert = mengenWert,
        samplingSignatur = konfiguration.samplingSignatur(),
        qualität = VisualisierungsCacheQualität.Detail,
    )
    val detailErgebnis by erinnertesVisualisierungsErgebnis(detailSchlüssel, menge, konfiguration)
    var budgetEntwurf by remember(knoten.id) {
        mutableFloatStateOf(konfiguration.sampling.maximalesRasterBudget.toFloat())
    }
    LaunchedEffect(konfiguration.sampling.maximalesRasterBudget) {
        budgetEntwurf = konfiguration.sampling.maximalesRasterBudget.toFloat()
    }
    VisualisierungsDialogInhalt(
        ergebnis = detailErgebnis,
        konfiguration = konfiguration,
        kameraÄndern = { kamera -> eigenschaftenErsetzen(konfiguration.copy(kamera = kamera).zuEigenschaften()) },
        budgetEntwurf = budgetEntwurf,
        budgetEntwurfÄndern = { budgetEntwurf = it },
        budgetÜbernehmen = {
            val budget = ((budgetEntwurf / 500f).roundToInt() * 500)
                .coerceIn(1_000, MAXIMALES_VISUALISIERUNGS_DETAIL_BUDGET)
            budgetEntwurf = budget.toFloat()
            if (budget != konfiguration.sampling.maximalesRasterBudget) {
                eigenschaftenErsetzen(
                    konfiguration.copy(
                        sampling = konfiguration.sampling.copy(maximalesRasterBudget = budget),
                    ).zuEigenschaften(),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun VisualisierungsDialogInhalt(
    ergebnis: VisualisierungsErgebnis?,
    konfiguration: VisualisierungsKonfiguration,
    kameraÄndern: (KameraZustand) -> Unit,
    budgetEntwurf: Float,
    budgetEntwurfÄndern: (Float) -> Unit,
    budgetÜbernehmen: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Plot(
                ergebnis = ergebnis,
                konfiguration = konfiguration,
                onKamera = kameraÄndern,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Steuerung(
                konfiguration,
                { neu -> kameraÄndern(neu.kamera) },
                Modifier.width(144.dp).fillMaxHeight(),
            )
        }
        val budget = ((budgetEntwurf / 500f).roundToInt() * 500)
            .coerceIn(1_000, MAXIMALES_VISUALISIERUNGS_DETAIL_BUDGET)
        val flächenAuflösung = sqrt(budget.toDouble()).toInt()
        val volumenAuflösung = cbrt(budget.toDouble()).toInt()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Detailbudget", style = MaterialTheme.typography.labelMedium)
                Text("$budget Auswertungen", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = budgetEntwurf,
                onValueChange = budgetEntwurfÄndern,
                onValueChangeFinished = budgetÜbernehmen,
                valueRange = 1_000f..MAXIMALES_VISUALISIERUNGS_DETAIL_BUDGET.toFloat(),
                steps = 13,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Automatisch: Flächenraster bis ${flächenAuflösung}×$flächenAuflösung, " +
                    "Volumenraster bis ${volumenAuflösung}×$volumenAuflösung×$volumenAuflösung.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LatexText(legende(konfiguration), style = MaterialTheme.typography.bodySmall)
        VisualisierungsStatus(ergebnis, konfiguration.sampling.maximalesRasterBudget)
    }
}

@Composable
private fun VisualisierungsStatus(ergebnis: VisualisierungsErgebnis?, budget: Int) {
    val statistik = when (ergebnis) {
        is VisualisierungsErgebnis.Erfolgreich -> ergebnis.statistik
        is VisualisierungsErgebnis.Teilweise -> ergebnis.statistik
        else -> null
    }
    if (statistik != null && statistik.auswertungen > 0) {
        Text(
            buildString {
                append("Auswertungen ${statistik.auswertungen}/$budget")
                append(" · ${statistik.stützpunkte} Stützpunkte")
                if (statistik.linien > 0) append(" · ${statistik.linien} Segmente")
                if (statistik.dreiecke > 0) append(" · ${statistik.dreiecke} Dreiecke")
                if (statistik.zellen > 0) append(" · ${statistik.zellen} Zellen")
                if (statistik.budgetErschöpft) append(" · Budget ausgeschöpft")
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (statistik.budgetErschöpft) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    when (ergebnis) {
        null -> Text("Berechne hohe Auflösung …", style = MaterialTheme.typography.labelSmall)
        is VisualisierungsErgebnis.NichtDarstellbar -> Text(ergebnis.grund, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        is VisualisierungsErgebnis.BedingtDarstellbar -> Text(ergebnis.grund, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
        is VisualisierungsErgebnis.ProjektionErforderlich -> Text(ergebnis.grund, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
        is VisualisierungsErgebnis.Teilweise -> if (ergebnis.hinweise.isNotEmpty()) Text(ergebnis.hinweise.joinToString(" "), style = MaterialTheme.typography.labelSmall)
        is VisualisierungsErgebnis.Erfolgreich -> if (ergebnis.hinweise.isNotEmpty()) Text(ergebnis.hinweise.joinToString(" "), style = MaterialTheme.typography.labelSmall)
    }
}

private data class VisualisierungsSegment(val von: VisualisierungsPunkt, val zu: VisualisierungsPunkt)
private data class VisualisierungsZeichenDaten(
    val zellen: List<VisualisierungsZelle>,
    val punkte: List<VisualisierungsPunkt>,
    val segmente: List<VisualisierungsSegment>,
    val dreiecke: List<VisualisierungsDreieck>,
    val intervalle: List<VisualisierungsIntervall>,
)

private const val INTERAKTIVE_MAXIMALE_ZELLEN = 160
private const val INTERAKTIVE_MAXIMALE_PUNKTE = 900
private const val INTERAKTIVE_MAXIMALE_SEGMENTE = 700
private const val INTERAKTIVE_MAXIMALE_DREIECKE = 700

internal fun <T> reduziereGleichmäßig(werte: List<T>, maximum: Int): List<T> {
    require(maximum > 0)
    if (werte.size <= maximum) return werte
    if (maximum == 1) return listOf(werte.first())
    return List(maximum) { index ->
        werte[(index.toLong() * werte.lastIndex / (maximum - 1)).toInt()]
    }
}

private fun VisualisierungsZeichenDaten.fürInteraktion(): VisualisierungsZeichenDaten = copy(
    zellen = reduziereGleichmäßig(zellen, INTERAKTIVE_MAXIMALE_ZELLEN),
    punkte = reduziereGleichmäßig(punkte, INTERAKTIVE_MAXIMALE_PUNKTE),
    segmente = reduziereGleichmäßig(segmente, INTERAKTIVE_MAXIMALE_SEGMENTE),
    dreiecke = reduziereGleichmäßig(dreiecke, INTERAKTIVE_MAXIMALE_DREIECKE),
)

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
        punkte = allePunkte.filterNot { Triple(it.x, it.y, it.z) in geometriePunkte },
        segmente = linien.flatMap { linie -> linie.punkte.zipWithNext(::VisualisierungsSegment) },
        dreiecke = dreiecke,
        intervalle = erfolgreich?.intervalle ?: teilweise?.intervalle.orEmpty(),
    )
}

@Composable private fun Plot(
    ergebnis: VisualisierungsErgebnis?,
    konfiguration: VisualisierungsKonfiguration,
    onKamera: (KameraZustand) -> Unit,
    modifier: Modifier,
) {
    var kamera by remember { mutableStateOf(konfiguration.kamera) }
    var gesteAktiv by remember { mutableStateOf(false) }
    val persistierteKamera by rememberUpdatedState(konfiguration.kamera)
    val aktuelleKameraÜbernehmen by rememberUpdatedState(onKamera)
    LaunchedEffect(konfiguration.kamera, gesteAktiv) {
        if (!gesteAktiv) kamera = konfiguration.kamera
    }
    val darstellungsKonfiguration = konfiguration.copy(kamera = kamera)
    val vollständigeZeichenDaten = remember(ergebnis) { bereiteZeichenDatenVor(ergebnis) }
    val interaktiveZeichenDaten = remember(vollständigeZeichenDaten) { vollständigeZeichenDaten.fürInteraktion() }
    val unSortierteZeichenDaten = if (gesteAktiv) interaktiveZeichenDaten else vollständigeZeichenDaten
    val sortierteDreiecke = remember(unSortierteZeichenDaten.dreiecke, darstellungsKonfiguration.kamera, konfiguration.dimension) {
        if (konfiguration.dimension == RaumDimension.R3) {
            unSortierteZeichenDaten.dreiecke.sortedBy { dreieckTiefe(it, darstellungsKonfiguration) }
        } else {
            unSortierteZeichenDaten.dreiecke
        }
    }
    val hintergrund = MaterialTheme.colorScheme.surfaceVariant
    val raster = MaterialTheme.colorScheme.outlineVariant
    val rahmen = MaterialTheme.colorScheme.outline
    val beschriftung = MaterialTheme.colorScheme.onSurfaceVariant
    val xAchse = MaterialTheme.colorScheme.primary
    val yAchse = MaterialTheme.colorScheme.secondary
    val zAchse = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.pointerInput(konfiguration.dimension) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            gesteAktiv = true
            var gestenKamera = persistierteKamera
            do {
                val ereignis = awaitPointerEvent()
                val pan = ereignis.calculatePan()
                val zoom = ereignis.calculateZoom()
                if (pan != Offset.Zero || zoom != 1f) {
                    gestenKamera = transformiereVisualisierungsKamera(gestenKamera, konfiguration.dimension, pan, zoom)
                    kamera = gestenKamera
                    ereignis.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (ereignis.changes.any { it.pressed })
            gesteAktiv = false
            aktuelleKameraÜbernehmen(gestenKamera)
        }
    }) {
        drawRect(hintergrund)
        when (darstellungsKonfiguration.dimension) {
            RaumDimension.R1 -> zeichneZahlengerade(darstellungsKonfiguration, raster, beschriftung)
            RaumDimension.R2, RaumDimension.C -> {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawLine(raster, Offset(0f, center.y), Offset(size.width, center.y), 1f)
                drawLine(raster, Offset(center.x, 0f), Offset(center.x, size.height), 1f)
                zeichne2DAchsenBeschriftungen(darstellungsKonfiguration, beschriftung)
            }
            RaumDimension.R3 -> zeichne3DAchsen(darstellungsKonfiguration, xAchse, yAchse, zAchse)
        }
        unSortierteZeichenDaten.zellen.forEach { zeichneZelle(it, darstellungsKonfiguration) }
        if (darstellungsKonfiguration.dimension == RaumDimension.R1) {
            unSortierteZeichenDaten.intervalle.forEach { zeichneIntervall(it, darstellungsKonfiguration, farbeFür(null, darstellungsKonfiguration)) }
        }
        val zeichnetDreieckKanten = sortierteDreiecke.size <= 1_500
        sortierteDreiecke.forEach { zeichneDreieck(it, darstellungsKonfiguration, zeichnetDreieckKanten) }
        unSortierteZeichenDaten.segmente.forEach { segment ->
            drawLine(
                farbeFür(segment.von.farbwert ?: segment.zu.farbwert, darstellungsKonfiguration),
                projekt(segment.von, darstellungsKonfiguration, size.width, size.height),
                projekt(segment.zu, darstellungsKonfiguration, size.width, size.height),
                2.2f,
            )
        }
        unSortierteZeichenDaten.punkte.forEach { punkt ->
            val projektion = projekt(punkt, darstellungsKonfiguration, size.width, size.height)
            val radius = when (darstellungsKonfiguration.dimension) {
                RaumDimension.R1 -> 5.0f
                RaumDimension.R2, RaumDimension.C -> 2.2f
                RaumDimension.R3 -> 2.8f
            }
            drawCircle(farbeFür(punkt.farbwert, darstellungsKonfiguration), radius, projektion)
            if (punkt.weitereFarbwerte.isNotEmpty()) {
                drawCircle(Color.Magenta.copy(alpha = 0.85f), radius + 2.5f, projektion, style = Stroke(1.5f))
            }
        }
        drawRect(rahmen, style = Stroke(1f))
    }
}

internal fun transformiereVisualisierungsKamera(
    kamera: KameraZustand,
    dimension: RaumDimension,
    pan: Offset,
    zoomFaktor: Float,
): KameraZustand = when (dimension) {
    RaumDimension.R3 -> kamera.copy(
        rotationY = kamera.rotationY + pan.x * 0.5,
        rotationX = kamera.rotationX + pan.y * 0.5,
        zoom = (kamera.zoom * zoomFaktor).coerceIn(0.1, 20.0),
    )
    RaumDimension.R2, RaumDimension.C -> kamera.copy(
        translationX = kamera.translationX - pan.x / 20.0,
        translationY = kamera.translationY + pan.y / 20.0,
        zoom = (kamera.zoom * zoomFaktor).coerceIn(0.1, 20.0),
    )
    RaumDimension.R1 -> kamera.copy(
        translationX = kamera.translationX - pan.x / 20.0,
        zoom = (kamera.zoom * zoomFaktor).coerceIn(0.1, 20.0),
    )
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
        val links = min(von, bis)
        val rechts = max(von, bis)
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
    val links = min(a.x, b.x)
    val rechts = max(a.x, b.x)
    val oben = min(a.y, b.y)
    val unten = max(a.y, b.y)
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

private fun DrawScope.achsenPaint(farbe: Color) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = farbe.toArgb()
    textSize = 12.dp.toPx()
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

private fun DrawScope.zeichneZahlengerade(c: VisualisierungsKonfiguration, achsenFarbe: Color, textFarbe: Color) {
    val mitte = size.height / 2f
    drawLine(achsenFarbe, Offset(0f, mitte), Offset(size.width, mitte), 1.5f)
    val paint = achsenPaint(textFarbe)
    val text = c.achsen.x.ifBlank { "x" }
    drawContext.canvas.nativeCanvas.drawText(text, size.width - paint.measureText(text) - 7.dp.toPx(), mitte - 7.dp.toPx(), paint)
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

private fun DrawScope.zeichne2DAchsenBeschriftungen(c: VisualisierungsKonfiguration, farbe: Color) {
    val abstand = 7.dp.toPx()
    val paint = achsenPaint(farbe)
    val canvas = drawContext.canvas.nativeCanvas
    val x = c.achsen.x.ifBlank { "x" }
    val y = c.achsen.y.ifBlank { "y" }
    canvas.drawText(x, size.width - paint.measureText(x) - abstand, size.height / 2f - abstand, paint)
    canvas.drawText(y, size.width / 2f + abstand, paint.textSize + abstand, paint)
}

private fun DrawScope.zeichne3DAchsen(c: VisualisierungsKonfiguration, xFarbe: Color, yFarbe: Color, zFarbe: Color) {
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

    zeichneAchsenText(c.achsen.x.ifBlank { "x" }, xEnde, achsenPaint(xFarbe))
    zeichneAchsenText(c.achsen.y.ifBlank { "y" }, yEnde, achsenPaint(yFarbe))
    zeichneAchsenText(c.achsen.z?.ifBlank { "z" } ?: "z", zEnde, achsenPaint(zFarbe))
}

private fun DrawScope.zeichneAchsenText(text: String, position: Offset, paint: Paint) {
    val rand = 5.dp.toPx()
    val x = (position.x + rand).coerceIn(rand, (size.width - paint.measureText(text) - rand).coerceAtLeast(rand))
    val y = (position.y - rand).coerceIn(paint.textSize + rand, size.height - rand)
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
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

private fun projekt(p: VisualisierungsPunkt, c: VisualisierungsKonfiguration, breite: Float, höhe: Float): Offset {
    val kameraPunkt = kameraKoordinate(p, c)
    val x = kameraPunkt.first
    if (c.dimension == RaumDimension.R1) {
        val bereich = c.bereiche.x
        return Offset(
            ((x - bereich.minimum) / (bereich.maximum - bereich.minimum) * breite * c.kamera.zoom).toFloat(),
            höhe / 2f,
        )
    }
    val y = kameraPunkt.second
    val bx = c.bereiche.x
    val by = c.bereiche.y
    return Offset(
        ((x - bx.minimum) / (bx.maximum - bx.minimum) * breite * c.kamera.zoom).toFloat(),
        (höhe - (y - by.minimum) / (by.maximum - by.minimum) * höhe * c.kamera.zoom).toFloat(),
    )
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

@Composable private fun Steuerung(c: VisualisierungsKonfiguration, ändern: (VisualisierungsKonfiguration) -> Unit, modifier: Modifier) {
    val standard = KameraZustand(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (c.dimension == RaumDimension.R3) {
            Text("Rotation", style = MaterialTheme.typography.labelMedium)
            listOf(
                "X" to { d: Double -> c.kamera.copy(rotationX = c.kamera.rotationX + d) },
                "Y" to { d: Double -> c.kamera.copy(rotationY = c.kamera.rotationY + d) },
                "Z" to { d: Double -> c.kamera.copy(rotationZ = c.kamera.rotationZ + d) },
            ).forEach { (name, funktion) -> AchsenTaste(name, true) { ändern(c.copy(kamera = funktion(it))) } }
            OutlinedButton(
                onClick = { ändern(c.copy(kamera = c.kamera.copy(rotationX = 0.0, rotationY = 0.0, rotationZ = 0.0))) },
                enabled = abs(c.kamera.rotationX) + abs(c.kamera.rotationY) + abs(c.kamera.rotationZ) > 1e-6,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset") }
            Spacer(Modifier.height(5.dp))
        }
        Text("Verschiebung", style = MaterialTheme.typography.labelMedium)
        val schritt = (c.bereiche.x.maximum - c.bereiche.x.minimum) * .1
        val verschiebungen = when (c.dimension) {
            RaumDimension.R1 -> listOf("X" to { d: Double -> c.kamera.copy(translationX = c.kamera.translationX + d) })
            RaumDimension.R2, RaumDimension.C -> listOf(
                "X" to { d: Double -> c.kamera.copy(translationX = c.kamera.translationX + d) },
                "Y" to { d: Double -> c.kamera.copy(translationY = c.kamera.translationY + d) },
            )
            RaumDimension.R3 -> listOf(
                "X" to { d: Double -> c.kamera.copy(translationX = c.kamera.translationX + d) },
                "Y" to { d: Double -> c.kamera.copy(translationY = c.kamera.translationY + d) },
                "Z" to { d: Double -> c.kamera.copy(translationZ = c.kamera.translationZ + d) },
            )
        }
        verschiebungen.forEach { (name, funktion) -> AchsenTaste(name, true) { ändern(c.copy(kamera = funktion(it * schritt))) } }
        OutlinedButton(
            onClick = { ändern(c.copy(kamera = c.kamera.copy(translationX = 0.0, translationY = 0.0, translationZ = 0.0))) },
            enabled = abs(c.kamera.translationX) + abs(c.kamera.translationY) + abs(c.kamera.translationZ) > 1e-6,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset") }
        OutlinedButton(onClick = { ändern(c.copy(kamera = standard)) }, enabled = !c.kamera.istStandard(c.dimension), modifier = Modifier.fillMaxWidth()) { Text("Ansicht zurück") }
    }
}
@Composable private fun AchsenTaste(name: String, enabled: Boolean, ändern: (Double) -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = { ändern(-15.0) }, enabled = enabled) { Text("−") }; Text(name, modifier = Modifier.padding(top = 12.dp)); TextButton(onClick = { ändern(15.0) }, enabled = enabled) { Text("+") } }
private fun legende(c: VisualisierungsKonfiguration): String {
    val farbBereich = c.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)
    val achsen = when (c.dimension) {
        RaumDimension.R1 -> "${c.achsen.x}\\text{-Achse}: ${c.achsen.x}"
        RaumDimension.R2 -> "${c.achsen.x}\\text{-Achse}: ${c.achsen.x},\\quad ${c.achsen.y}\\text{-Achse}: ${c.achsen.y}"
        RaumDimension.C -> "\\operatorname{Re}(z)\\text{-Achse}: ${c.achsen.x},\\quad \\operatorname{Im}(z)\\text{-Achse}: ${c.achsen.y}"
        RaumDimension.R3 -> "${c.achsen.x}\\text{-Achse}: ${c.achsen.x},\\quad ${c.achsen.y}\\text{-Achse}: ${c.achsen.y},\\quad ${c.achsen.z}\\text{-Achse}: ${c.achsen.z}"
    }
    return achsen + if (c.farbe.modus == FarbModus.Spektrum) ",\\quad \\operatorname{Farbe}: ${c.farbe.variable ?: "t"},\\quad ${farbBereich.minimum}\\le ${c.farbe.variable ?: "t"}\\le ${farbBereich.maximum}" else ""
}

private fun raumName(dimension: RaumDimension): String = when (dimension) {
    RaumDimension.R1 -> "R¹"
    RaumDimension.R2 -> "R²"
    RaumDimension.R3 -> "R³"
    RaumDimension.C -> "ℂ"
}
