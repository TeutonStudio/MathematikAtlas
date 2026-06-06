package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsRegeln

// Kotlin
import kotlin.math.abs
import kotlin.math.max

/**
 * Erstellt eine Zeichenfunktion für eine einzelne Verbindung.
 *
 * Die eigentliche Geometrie wird erst im Canvas-DrawScope gezeichnet, damit alle
 * Verbindungen gemeinsam auf einer Canvas-Ebene hinter den Knoten liegen können.
 */
private fun VerbindungDaten.zuPfad(start: Offset, ende: Offset): DrawScope.() -> Unit = { VerbindungPfad(this@zuPfad, start, ende) }

/**
 * Rendert eine einzelne Verbindung zwischen zwei Bildschirmpositionen.
 */
@Composable
public fun VerbindungDaten.zuComposable(start: Offset, ende: Offset, modifier: Modifier = Modifier) =
    BasisVerbindung(this, start = start, ende = ende).zuComposable(modifier)

/**
 * Rendert eine Liste fachlicher Verbindungen.
 *
 * Die übergebenen Funktionen lösen die referenzierten Anschlusspositionen auf.
 * Verbindungen mit fehlenden Endpunkten werden übersprungen.
 */
@Composable
public fun List<VerbindungDaten>.zuComposable(
    start: (VerbindungDaten) -> Offset?,
    ende: (VerbindungDaten) -> Offset?,
    modifier: Modifier = Modifier,
    verbindungArten: VerbindungArten = VerbindungArten.Standard,
) = VerbindungUmgebung(
    modifier,
    this.mapNotNull {
        val s = start(it)
        val e = ende(it)
        if (s != null && e != null) verbindungArten.erstelle(it, s, e).zeichnung() else null
    },
)

/**
 * Rendert bereits aufgelöste Verbindungen.
 *
 * Diese Variante wird unter anderem für die temporäre Verbindung beim Ziehen
 * eines Anschlusses verwendet.
 */
@Composable
public fun List<Triple<VerbindungDaten, Offset, Offset>>.zuComposable(modifier: Modifier = Modifier) = VerbindungUmgebung(
    modifier,
    this.map { BasisVerbindung(it.first, start = it.second, ende = it.third).zeichnung() },
)

/**
 * Gemeinsame Canvas-Umgebung für eine oder mehrere Zeichenfunktionen.
 */
@Composable
private fun VerbindungUmgebung(
    modifier: Modifier = Modifier,
    vararg inhalt: DrawScope.() -> Unit,
) { Canvas(modifier = modifier) { inhalt.forEach { it() } } }

/**
 * Listenvariante der gemeinsamen Canvas-Umgebung.
 */
@Composable
private fun VerbindungUmgebung(
    modifier: Modifier = Modifier,
    inhalt: List<DrawScope.() -> Unit>,
) { Canvas(modifier = modifier) { inhalt.forEach { it() } } }


sealed interface Verbindung: GraphObjekt {
    public val daten: VerbindungDaten
    public val von: Anschluss?
    public val zu: Anschluss?
    public val start: Offset
    public val ende: Offset

}

open class BasisVerbindung(
    override val daten: VerbindungDaten,
    override val von: Anschluss? = null,
    override val zu: Anschluss? = null,
    override val start: Offset = Offset.Zero,
    override val ende: Offset = Offset.Zero,
): Verbindung {
    @Composable
    override fun zuComposable(modifier: Modifier) {
        VerbindungUmgebung(modifier, zeichnung())
    }

    internal open fun zeichnung(): DrawScope.() -> Unit = daten.zuPfad(start, ende)

    public companion object {
        public const val VERBINDUNG_ART: String = "default"
    }
}


/**
 * Zeichnet eine Bezier-Verbindung.
 *
 * Der Startpunkt läuft horizontal nach rechts aus, der Endpunkt horizontal von
 * links ein. Damit entspricht die Kurve der üblichen Darstellung von
 * Node-Graph-Verbindungen.
 */
private fun DrawScope.VerbindungPfad(
    daten: VerbindungDaten,
    start: Offset,
    ende: Offset,
) {
    val farbe = when {
        daten.fehler != null -> Color(0xFFDC2626)
        daten.ausgewaehlt -> Color(0xFF2563EB)
        else -> Color(0xFF475569)
    }

    val kontrollAbstand = max(48f, abs(ende.x - start.x) / 2f)
    val pfad = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x + kontrollAbstand,
            start.y,
            ende.x - kontrollAbstand,
            ende.y,
            ende.x,
            ende.y,
        )
    }
    drawPath(
        path = pfad,
        color = farbe,
        style = Stroke(width = 3f, cap = StrokeCap.Round),
    )
}

public fun interface VerbindungFabrik {
    public fun erstelle(daten: VerbindungDaten, start: Offset, ende: Offset): Verbindung
}

/**
 * Registry wie ReactFlows `nodeTypes`: `KnotenDaten.knotenArt` entscheidet,
 * welche Knotenklasse und damit welche Anschlüsse verwendet werden.
 */
data class VerbindungArten(
    private val fabriken: Map<String, VerbindungFabrik> = standardFabriken,
) {
    public fun erstelle(daten: VerbindungDaten): Verbindung =
        erstelle(daten, Offset.Zero, Offset.Zero)

    internal fun erstelle(daten: VerbindungDaten, start: Offset, ende: Offset): BasisVerbindung =
        (fabriken[daten.art] ?: fabriken.getValue(BasisVerbindung.VERBINDUNG_ART))
            .erstelle(daten, start, ende) as? BasisVerbindung
            ?: BasisVerbindung(daten, start = start, ende = ende)

    public fun mit(art: String, fabrik: VerbindungFabrik): VerbindungArten =
        copy(fabriken = fabriken + (art to fabrik))

    public companion object {
        private val standardFabriken = mapOf(
            BasisVerbindung.VERBINDUNG_ART to VerbindungFabrik { daten, start, ende ->
                BasisVerbindung(daten, start = start, ende = ende)
            },
        )

        public val Standard: VerbindungArten = VerbindungArten()
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
    val aktuellePosition: Offset,
)

/**
 * Baut die Vorschau-Verbindung fuer den Canvas-Layer.
 *
 * Wenn der Drag an einem Eingang startet, wird die Vorschau visuell gedreht,
 * damit die Bezier-Tangenten weiterhin passend aussehen.
 */
internal fun VerbindungsDrag.zuVorschau(): Triple<VerbindungDaten, Offset, Offset> {
    val startPosition = if (start.richtung == AnschlussRichtung.Eingang) {
        aktuellePosition
    } else {
        start.position
    }

    val endePosition = if (start.richtung == AnschlussRichtung.Eingang) {
        start.position
    } else {
        aktuellePosition
    }

    return Triple(
        VerbindungDaten(
            id = "temporaer",
            quellKnotenId = "",
            quellAnschlussId = start.anschlussId,
            zielKnotenId = "",
            zielAnschlussId = start.anschlussId,
            ausgewaehlt = true,
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
internal fun Modifier.verbindungsZiehen(
    start: AnschlussReferenz,
    graph: () -> KartenGraph,
    onDragAendern: (VerbindungsDrag?) -> Unit,
    onZiehtAnschlussAendern: (Boolean) -> Unit,
    onBlockiereHintergrundGestenAendern: (Boolean) -> Unit,
    onVerbindungErstellen: (VerbindungDaten) -> Unit,
    regeln: VerbindungsRegeln = VerbindungsRegeln(),
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

                    lokalerDrag = lokalerDrag.copy(
                        aktuellePosition = lokalerDrag.aktuellePosition +
                                (change.position - change.previousPosition),
                    )

                    onDragAendern(lokalerDrag)
                }

                val aktuellerGraph = graph()
                val ziel = aktuellerGraph.naechsterAnschluss(
                    position = lokalerDrag.aktuellePosition,
                    maxAbstand = maxZielAbstand,
                    ausgenommen = lokalerDrag.start,
                )

                if (ziel != null) {
                    val verbindung = lokalerDrag.start.zuVerbindungOderNull(
                        ziel = ziel,
                        vorhandeneVerbindungen = aktuellerGraph.verbindungen,
                        regeln = regeln,
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
    }

/**
 * Erstellt eine Verbindung, falls Start und Ziel fachlich kompatibel sind.
 */
internal fun AnschlussReferenz.zuVerbindungOderNull(
    ziel: AnschlussReferenz,
    vorhandeneVerbindungen: List<VerbindungDaten>,
    regeln: VerbindungsRegeln = VerbindungsRegeln(),
): VerbindungDaten? {
    if (knotenId == ziel.knotenId) return null
    if (richtung == ziel.richtung) return null

    val quelle = if (richtung == AnschlussRichtung.Ausgang) this else ziel
    val ende = if (richtung == AnschlussRichtung.Eingang) this else ziel

    val verbindung = VerbindungDaten(
        id = "verbindung-${quelle.knotenId}-${quelle.anschlussId}-${ende.knotenId}-${ende.anschlussId}",
        quellKnotenId = quelle.knotenId,
        quellAnschlussId = quelle.anschlussId,
        zielKnotenId = ende.knotenId,
        zielAnschlussId = ende.anschlussId,
    ) /*.mitTypPruefung(
        quellTyp = quelle.zahlenTyp,
        zielTyp = ende.zahlenTyp,
    )*/

    val erlaubt = regeln.darfErstellen(
        vorhandeneVerbindungen = vorhandeneVerbindungen,
        neueVerbindung = verbindung,
        quellRichtung = quelle.richtung,
        zielRichtung = ende.richtung,
        quellTyp = quelle.zahlenTyp,
        zielTyp = ende.zahlenTyp,
    )

    return if (erlaubt) verbindung else null
}