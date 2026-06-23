package de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektKontext
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektInspektor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.anschlüsse.ZahlenObjektAnschluss
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.LaTeXFormelText
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.latexInfo

typealias ZahlenAuswertenDaten = auswerten.ZahlenAuswertenDaten

class auswerten(
    override val graph: Graph,
    override val daten: ZahlenAuswertenDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<ZahlenAuswertenDaten>, GraphDatenObjektInspektor<ZahlenAuswertenDaten> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val minimaleBreite = 200f
    override val minimaleTiefe = 80f

    @Composable
    override fun BoxScope.Darstellung() {
        Card(Modifier.matchParentSize()) {
            Column {
                Text(daten.name)
                LaTeXFormelText(daten.formel(), besitzer.daten)
            }
        }
    }

    @Composable override fun BoxScope.KontextFenster(pos: IntOffset) = StandardKontextFenster(pos)
    @Composable override fun BoxScope.Inspektor() = Composable()

    @Composable
    override fun Inhalt() {
        BasisObjektKontext(latexInfo("Formel", besitzer.daten, daten.formel())).Inhalt()
    }

    class ZahlenAuswertenDaten(
        override val id: GraphDatenId,
        override val name: String = "Zahl auswerten",
    ) : GraphDatenKnoten {
        override var klasse: KnotenArt? = KNOTEN_ART
        override var beweglich = true
        override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
        override val anschlussIdx = mutableStateMapOf<String, Int>()
        override val data = mutableMapOf<String, Any>()
        override var position by mutableStateOf(GraphPosition.Zero)
        override var breite by mutableFloatStateOf(0f)
        override var tiefe by mutableFloatStateOf(0f)

        init {
            val eingang = ZahlenObjektAnschluss.ZahlenAnschlussDaten("$id-eingang-0", Kante.Links, Richtung.Eingang).apply {
                label = "Zahl"
                klasse = ZahlenObjektAnschluss.EINGANG_ART
            }
            anschlüsse.add(eingang)
            anschlussIdx[eingang.id] = 0
        }

        fun formel(): String =
            (anschlüsse.firstOrNull() as? ZahlenObjektAnschluss.ZahlenAnschlussDaten)
                ?.let { it.cache as? ZahlenObjektAnschluss.ZahlenAnschlussDaten.CacheDaten }
                ?.latex
                ?.takeIf { it != ZahlenObjektAnschluss.UNBEKANNT }
                ?: "?"
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "auswertenZahl"
    }
}
