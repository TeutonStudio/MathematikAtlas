package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
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
