package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten

class BasisObjektKnoten(
    override val graph: Graph,
    override val daten: GraphDatenKnoten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<GraphDatenKnoten>/*, GraphDatenObjektInspektor<GraphDatenKnoten>*/ {
    override val layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)
    override val kontextData: List<@Composable (() -> Unit)> = listOf(
        { Text(daten.name,Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Duplizieren",Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Vernichten",Modifier.clickable { TODO("Kein vernichten implementiert") }) },
    )
    override val inpsektorData: List<@Composable (() -> Unit)> = listOf(
        { Text(daten.name,Modifier.clickable { TODO("Kein duplizieren implementiert") }) },

    )

    @Composable
    override fun BoxScope.Darstellung() {
        Card {
            Column {
                Text(daten.name)
            }
        }
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        StandardKontextFenster(pos)
    }

/*
    @Composable
    override fun BoxScope.Inspektor() {
        basisKontext().CardColumn(this@BasisObjektKnoten)
    }
*/

/*    @Composable
    override fun Inhalt() {
        basisKontext().Inhalt()
    }*/

    override val anschlussFabrik: AnschlussFabrik = BasisAnschlussFabrik
    override fun definiereVerbindung() {
        TODO("Not yet implemented")
    }

/*    private fun basisKontext() = BasisObjektKontext(
        info("Klasse", daten.klasse ?: "-"),
        info("Position", "${daten.position.x.toInt()}, ${daten.position.y.toInt()}"),
        info("Anschlüsse", daten.anschlüsse.size),
    )*/

    public companion object {
        public const val KNOTEN_ART = "default"
    }
}
