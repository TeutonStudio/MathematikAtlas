package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussModifier
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KnotenKonstruktor
import com.TeutonStudio.KnotenKartenVerwalter.alignment
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten.Companion.zuAuswahl

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.erhalteSize
import com.TeutonStudio.KnotenKartenVerwalter.erhalteZoomfaktor
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeAnschluss
import com.TeutonStudio.KnotenKartenVerwalter.fillMaxKante
import com.TeutonStudio.KnotenKartenVerwalter.offsetKante
import com.TeutonStudio.KnotenKartenVerwalter.radius

// Composable
import com.TeutonStudio.KnotenKartenVerwalter.zuBild
import com.TeutonStudio.KnotenKartenVerwalter.zuLeiste
import kotlin.div
import kotlin.let

@Suppress("UNCHECKED_CAST")
val BasisKnotenFabrik: KnotenFabrik = mapOf(
    BasisKnoten.KNOTEN_ART to ::BasisKnoten as KnotenKonstruktor,
    EingabeKnoten.KNOTEN_ART to ::EingabeKnoten as KnotenKonstruktor,
    AusgabeKnoten.KNOTEN_ART to ::AusgabeKnoten as KnotenKonstruktor,
)

@Composable
public fun List<Knoten>.zuComposable(
    modifierKnoten: (KnotenDaten) -> Modifier,
    modifierAnschluss: (KnotenDaten) -> AnschlussModifier,
) = this.map { it.zuComposable(modifierKnoten(it.daten),modifierAnschluss(it.daten)) }

/**
 * Knoten Elternklasse
 */
abstract class Knoten(
    _graph: Graph
): GraphObjekt(_graph) {
    public abstract override val daten: KnotenDaten
    public abstract val besitzer: Karte
    public abstract val anschlussFabrik: AnschlussFabrik

    public val anschlüsse by lazy { daten.anschlüsse.entries.mapNotNull {
        anschlussFabrik.erzeugeAnschluss(graph,it.key, this)?.let { a -> a to it.value }
    }.toMap() }
    val bildPos get() = daten.position.zuBild(besitzer.zustand.ansicht)
    val boxModiRect get() = { d: Density -> Modifier.offset { bildPos }.size( size = with(d) { (daten.erhalteSize() * zoomFaktor()).toDpSize() }) }

    @Composable
    public fun zuComposable(
        modifierKnoten: Modifier = Modifier.fillMaxSize(),
        modifierAnschluss: AnschlussModifier = { daten, idx -> AnschlussModifierStandard },
        inhaltSkalierung: Float = 1f,
    ) {
        Box(modifier = boxModiRect(LocalDensity.current).draggable2D(
            enabled = daten.beweglich,
            state = rememberDraggable2DState {
                besitzer.aktualisierung(daten.id,daten.position + it / zoomFaktor())
                graph.wähle(daten.zuAuswahl())
                graph.keinKontext()
            },
        ).pointerInput(daten.id) {
            detectTapGestures(
                onLongPress = { graph.ctx = daten.id to it.round() },
                onTap = {
                    graph.wähle(daten.zuAuswahl())
                    graph.keinKontext()
                },
            )
        }) {
            Inhalt(modifierKnoten)
            AnschlussKante.entries.forEach { kante ->
                val modi = Modifier.fillMaxKante(kante).offsetKante(kante,radius(kante))
                Box(
                    modifier = modi.align(alignment(kante)), //.offset(x = (-5f * skalierung).dp),
                    contentAlignment = Alignment.Center,
                ) { anschlüsse.zuLeiste(kante) }
            }
            if (öffneKontext.value) erhalteKontextFenster(graph.ctx.second)
        }
    }

    @Composable public fun Inhalt(modifier: Modifier) = Card(modifier = modifier, border = if (istSelektiert) BorderStroke(5.dp,graph.selektiertFarbe) else null) {Column(Modifier.padding(15.dp)) { Kopfzeile(); Textzeile(); Fußzeile() }}

    @Composable public fun Kopfzeile() = Text(daten.name)
    @Composable public abstract fun Textzeile()
    @Composable public abstract fun Fußzeile()

    @Composable
    override fun erhalteKontextFenster(
        pos: BildschirmPosition
    ) {
        Box(
            modifier = Modifier
                .offset { pos }
//                .onSizeChanged { fensterGröße = it }
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
        ) {
            Card() {
                Column {
                    Text("Kontextfenster des Knoten")
                }
            }
        }

    }

    private fun zoomFaktor(): Float = besitzer.zustand.ansicht.erhalteZoomfaktor().coerceAtLeast(0.01f)

    @Composable
    override fun zuComposable(modifier: Modifier) = TODO("Falsche Methode aufgerufen")
}

/**
 * Standard Knoten
 */
open class BasisKnoten(
    _graph: Graph,
    override val daten: KnotenDaten,
    override val besitzer: Karte,
): Knoten(_graph) {
    override val anschlussFabrik: AnschlussFabrik = BasisAnschlussFabrik


    @Composable
    override fun Textzeile() {
        Text("Knoten Textzeile")
    }

    @Composable
    override fun Fußzeile() {
        Text("Knoten Fußzeile")
    }


    public companion object {
        public const val KNOTEN_ART: KnotenArt = "default"
    }
}

/**
 * Standard Knoten mit Ausgängen
 */
open class EingabeKnoten(_graph: Graph,daten: EingabeDaten, besitzer: Karte): BasisKnoten(_graph,daten,besitzer) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "eingabe"
    }

}

/**
 * Standard Knoten Eingängen
 */
open class AusgabeKnoten(_graph: Graph,daten: AusgabeDaten, besitzer: Karte): BasisKnoten(_graph,daten,besitzer) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "ausgabe"
    }

}



/**
 * Vorschau der Standard-Knotendarstellung.
 */
@Preview
@Composable
private fun KnotenPreview() { // TODO
/*    val daten = KnotenDaten(
        id = "knoten-1",
        name = "Ableitung",
    )*/
    // daten.zuComposable(modifierAnschluss = { _, _ -> AnschlussModifierStandard })
}
