package de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektInspektor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik
import de.TeutonStudio.MathematikAtlas.anschlüsse.ZahlenObjektAnschluss
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.LaTeXFormelText

typealias ZahlenUnbekanntDaten = definition.ZahlenDefinitionDaten

class unbekannt(
    override val graph: Graph,
    override val daten: ZahlenUnbekanntDaten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<ZahlenUnbekanntDaten>, GraphDatenObjektInspektor<ZahlenUnbekanntDaten> {
    private val basis = definition(graph, daten.apply {
        klasse = KNOTEN_ART
        setzeWert(ZahlenObjektAnschluss.UNBEKANNT)
    }, besitzer)

    override val layoutCoordinates = basis.layoutCoordinates
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val minimaleBreite = 160f
    override val minimaleTiefe = 70f

    @Composable
    override fun BoxScope.Darstellung() {
        Card(Modifier.matchParentSize()) {
            Column {
                Text(daten.name)
                LaTeXFormelText("?", besitzer.daten)
            }
        }
    }

    @Composable override fun BoxScope.KontextFenster(pos: IntOffset) = StandardKontextFenster(pos)
    @Composable override fun BoxScope.Inspektor() = Composable()

    @Composable
    override fun Inhalt() {
        basis.Inhalt()
    }

    companion object {
        const val KNOTEN_ART: KnotenArt = "unbekanntZahl"
    }
}
