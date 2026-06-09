package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScopeMarker
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition

//
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.VerbindungUmgebung
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungArt
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungFabrik
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungKonstruktor

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.istEingang
import com.TeutonStudio.KnotenKartenVerwalter.tangente
import com.TeutonStudio.KnotenKartenVerwalter.zuBild
import kotlin.invoke

// Kotlin
import kotlin.math.hypot
import kotlin.math.max

@Suppress("UNCHECKED_CAST")
val BasisVerbindungFabrik: VerbindungFabrik = mapOf(
    BasisVerbindung.VERBINDUNG_ART to ::BasisVerbindung as VerbindungKonstruktor,
)


abstract class Verbindung(
    _graph: Graph
): GraphObjekt(_graph) {
    public abstract override val daten: VerbindungDaten
//    public val von: Anschluss?
//    public val zu: Anschluss?
    public abstract var startKante: AnschlussKante // TODO herausfinden ob State oder var besser ist
    public abstract val start: State<KartenPosition>
    public abstract var endeKante: AnschlussKante // TODO herausfinden ob State oder var besser ist
    public abstract val ende: State<KartenPosition>

    @Composable
    override fun zuComposable(modifier: Modifier) = Canvas(modifier = modifier) { zeichnung() }

    public abstract val zeichnung: DrawScope.() -> Unit

    public fun c1(): Offset {
        val startRichtung = startKante.tangente()
        val dx = ende.value.x - start.value.x
        val dy = ende.value.y - start.value.y
        val distanz = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val kontrollAbstand = max(48f, distanz * 0.35f).coerceAtMost(240f)
        return start.value + startRichtung * kontrollAbstand
    }

    public fun c2(): Offset {
        val endeRichtung = endeKante.tangente()
        val dx = ende.value.x - start.value.x
        val dy = ende.value.y - start.value.y
        val distanz = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val kontrollAbstand = max(48f, distanz * 0.35f).coerceAtMost(240f)
        return ende.value + endeRichtung * kontrollAbstand
    }

    @Composable
    override fun erhalteKontextFenster(pos: BildschirmPosition) {
        TODO("Not yet implemented")
    }
}

open class BasisVerbindung(
    _graph: Graph,
    override val daten: VerbindungDaten,
    override val start: State<KartenPosition>,
    override val ende: State<KartenPosition>,
): Verbindung(_graph) {
    override var startKante: AnschlussKante = AnschlussKante.Links
    override var endeKante: AnschlussKante = AnschlussKante.Rechts


    override val zeichnung: DrawScope.() -> Unit
        get() = {
            drawPath(
                path = erhaltePfad(),
                color = when {
                    istSelektiert -> graph.selektiertFarbe
                    daten.fehler != null -> Color(0xFFDC2626)
                    else -> Color(0xFF475569)
                },
                style = Stroke(width = if (istSelektiert) 5f else 3f, cap = StrokeCap.Round),
            )
        }

    private fun erhaltePfad(): Path = Path().apply {
        val cubic = { o1: Offset, o2: Offset, o3:  Offset -> cubicTo(o1.x,o1.y,o2.x,o2.y,o3.x,o3.y) }
        moveTo(start.value.x, start.value.y)
        cubic(c1(),c2(),ende.value)
    }

    public companion object {
        public const val VERBINDUNG_ART: VerbindungArt = "default"
    }
}

/**
 * Temporärer Zustand waehrend eine neue Verbindung gezogen wird.
 *
 * Diese Datei kennt keine Karte und keine Compose-Oberflaeche.
 * Sie weiss nur: Startanschluss, aktuelle Pointerposition, Zielsuche,
 * Kompatibilitaet, Verbindung erzeugen.
 */
internal data class VerbindungsDrag(
    val start: AnschlussReferenz,
    val aktuellePosition: BildschirmPosition,
)

/**
 * Baut die Vorschau-Verbindung fuer den Canvas-Layer.
 *
 * Wenn der Drag an einem Eingang startet, wird die Vorschau visuell gedreht,
 * damit die Bezier-Tangenten weiterhin passend aussehen.
 */
internal fun VerbindungsDrag.zuVorschau(): Triple<VerbindungDaten, BildschirmPosition, BildschirmPosition> {
    val startPosition = if (start.richtung.istEingang()) { aktuellePosition } else { start.position }
    val endePosition = if (start.richtung == AnschlussRichtung.Eingang) { start.position } else { aktuellePosition }

    return Triple(
        VerbindungDaten(
            id = "temporaer",
            ids = ("" to "") to (start.anschlussId to start.anschlussId),
        ),
        startPosition,
        endePosition,
    )
}

/**
 * Pointer-Interaktion fuer das Ziehen einer Verbindung ab einem Anschluss.
 *
 * Der Graph wird als Lambda uebergeben, damit der Pointer-Handler auch nach
 * Recompositionen aktuelle Knoten, Anschluesse und Verbindungen sieht.
 */
/*internal fun Modifier.verbindungsZiehen(
    start: AnschlussReferenz,
    graph: () -> Karte,
    onDragAendern: (VerbindungsDrag?) -> Unit,
    onZiehtAnschlussAendern: (Boolean) -> Unit,
    onBlockiereHintergrundGestenAendern: (Boolean) -> Unit,
    onVerbindungErstellen: (VerbindungDaten) -> Unit,
//    regeln: VerbindungsRegeln = VerbindungsRegeln(),
    maxZielAbstand: Float = 28f,
): Modifier =
    pointerInput(start.knotenId, start.anschlussId, start.position) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var lokalerDrag = VerbindungsDrag(
                start = start,
                aktuellePosition = start.position,
            )

            try {
                onZiehtAnschlussAendern(true)
                onBlockiereHintergrundGestenAendern(true)
                onDragAendern(lokalerDrag)

                down.consume()

                drag(down.id) { change ->
                    change.consume()
                    change.scaleFactor // TODO für zoom verwenden
                    change.panOffset // TODO vlt besser als selbst auszurechnen
                    val rel = change.position - change.previousPosition // TODO vlt. panOffset
                    lokalerDrag = lokalerDrag.copy(
                        aktuellePosition = lokalerDrag.aktuellePosition + IntOffset(rel.x.toInt(),rel.y.toInt())
                    )

                    onDragAendern(lokalerDrag)
                }

                val aktuellerGraph = graph()
                val ziel = lokalerDrag.aktuellePosition.naechsterAnschluss(
                    anschluesse = aktuellerGraph.daten, //.erhalteAnschlüsse(referenz),
                    maxAbstand = maxZielAbstand,
                )

                if (ziel != null) {
                    val verbindung = lokalerDrag.start.zuVerbindungOderNull(
                        ziel = ziel,
                        vorhandeneVerbindungen = aktuellerGraph.daten.verbindungen,
//                        regeln = regeln,
                    )

                    if (verbindung != null) {
                        onVerbindungErstellen(verbindung)
                    }
                }
            } finally {
                onDragAendern(null)
                onZiehtAnschlussAendern(false)
                onBlockiereHintergrundGestenAendern(false)
            }
        }
    }*/

/**
 * Erstellt eine Verbindung, falls Start und Ziel fachlich kompatibel sind.
 */
internal fun AnschlussReferenz.zuVerbindungOderNull(
    ziel: AnschlussReferenz,
    vorhandeneVerbindungen: List<VerbindungDaten>,
//    regeln: VerbindungsRegeln = VerbindungsRegeln(),
): VerbindungDaten? {
    if (knotenId == ziel.knotenId) return null
    if (richtung == ziel.richtung) return null

    val quelle = if (richtung == AnschlussRichtung.Ausgang) this else ziel
    val ende = if (richtung == AnschlussRichtung.Eingang) this else ziel

    val verbindung = VerbindungDaten(
        id = "verbindung-${quelle.knotenId}-${quelle.anschlussId}-${ende.knotenId}-${ende.anschlussId}",
        ids = (quelle.knotenId to ende.knotenId) to (quelle.anschlussId to ende.anschlussId),
    ) /*.mitTypPruefung(
        quellTyp = quelle.zahlenTyp,
        zielTyp = ende.zahlenTyp,
    )*/

/*    val erlaubt = regeln.darfErstellen( TODO
        vorhandeneVerbindungen = vorhandeneVerbindungen,
        neueVerbindung = verbindung,
        quellRichtung = quelle.richtung,
        zielRichtung = ende.richtung,
    )*/

    return if (true) verbindung else null
}

/**
 * Loest alle gerichteten Anschluesse eines Knotens in Bildschirmpositionen auf.
 */
internal fun Knoten.anschlussReferenzen(zustand: KarteZustand): List<Pair<AnschlussDaten,KnotenDaten>> =
    daten.anschlüsse.entries
        .sortedWith(compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id })
        .map { (anschluss, idx) -> Pair(anschluss, daten) }

/**
 * Berechnet die Bildschirmposition eines einzelnen Anschlusses.
 */
internal fun Knoten.anschlussReferenz(
    anschluss: AnschlussDaten,
    zustand: KarteZustand,
): AnschlussReferenz? {
    val richtung = if (anschluss is RichtungsAnschlussDaten) anschluss.richtung else null
    val anschluesseAnKante = daten.anschlüsse.entries
        .filter { (daten, _) -> daten.kante == anschluss.kante }
        .sortedWith(compareBy<Map.Entry<AnschlussDaten, Int>> { it.value }.thenBy { it.key.id })

    val indexAnKante = anschluesseAnKante
        .indexOfFirst { (daten, _) -> daten.id == anschluss.id }
        .coerceAtLeast(0)

    val anzahlAnKante = anschluesseAnKante.size.coerceAtLeast(1)
    val anteil = (indexAnKante + 1f) / (anzahlAnKante + 1f)

    val kartePos = Offset(
        x = when (anschluss.kante) {
            AnschlussKante.Links -> daten.position.x
            AnschlussKante.Rechts -> daten.position.x + daten.dimension.width
            AnschlussKante.Oben,
            AnschlussKante.Unten -> daten.position.x + daten.dimension.width * anteil
        },
        y = when (anschluss.kante) {
            AnschlussKante.Links,
            AnschlussKante.Rechts -> daten.position.y + daten.dimension.height * anteil
            AnschlussKante.Oben -> daten.position.y
            AnschlussKante.Unten -> daten.position.y + daten.dimension.height
        },
    ) as KartenPosition

    return AnschlussReferenz(
        knotenId = daten.id,
        anschlussId = anschluss.id,
        richtung = richtung,
        kante = anschluss.kante,
        position = kartePos.zuBild(zustand.ansicht),
    )
}

/**
 * Sucht den naechsten Anschluss zu einer Bildschirmposition.
 */
internal fun BildschirmPosition.naechsterAnschluss(
    anschluesse: List<AnschlussReferenz>,
    maxAbstand: Float,
): AnschlussReferenz? =
    anschluesse
        .map { it to hypot((x - it.position.x).toDouble(), (y - it.position.y).toDouble()) }
        .filter { it.second <= maxAbstand }
        .minByOrNull { it.second }
        ?.first

internal fun KartenTreffer.zuAuswahl(): AuswahlDaten = when (this) {
    KartenTreffer.Hintergrund -> AuswahlDaten()
    is KartenTreffer.Knoten -> AuswahlDaten(knotenIds = setOf(knotenId))
    is KartenTreffer.Anschluss -> AuswahlDaten(knotenIds = setOf(knotenId))
    is KartenTreffer.Verbindung -> AuswahlDaten(verbindungIds = setOf(verbindungId))
}
