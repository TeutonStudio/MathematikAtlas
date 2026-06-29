package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert.BasisDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung

open class BasisObjektAnschluss(
    override val graph: Graph,
    override val daten: GraphDatenAnschluss,
    override val besitzer: GraphDatenObjektKnoten<*>,
): GraphDatenObjektAnschluss<GraphDatenAnschluss> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override var dragPos = mutableStateOf<Offset>(Offset.Zero)
    override var dragZiel = mutableStateOf<GraphDatenObjektAnschluss<*>?>(null)

    override val inpsektorData: List<@Composable (() -> Unit)> = listOf()
    override val kontextData: List<@Composable (() -> Unit)> = listOf(
        { Text(daten.label,Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Duplizieren",Modifier.clickable { TODO("Kein duplizieren implementiert") }) },
        { Text("Vernichten",Modifier.clickable { TODO("Kein vernichten implementiert") }) },
    )


    override fun erhaltePseudoVerbindung(): GraphDatenObjektVerbindung<*> {
        return BasisObjektVerbindung(graph, BasisDatenVerbindung(
            "pseudo",
            GraphDatenVerbindung.IDEhe(
                besitzer.daten.id,
                besitzer.daten.id,
                daten.id,
                daten.id,
            ),
        ),
        derivedStateOf { pos },
        derivedStateOf { dragZiel.value?.pos ?: dragPos.value } ).apply {
            startKante = this@BasisObjektAnschluss.daten.kante
            endeKante = startKante.gegenüber()
        }
    }
    /*    override fun beiKlick(klickPos: Offset) {
            TODO("Not yet implemented")
        }*/
/*    override fun beiHalten(klickPos: Offset) {
        TODO("Not yet implemented")
    }*/
/*    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        TODO("Not yet implemented")
    }*/

    @Composable
    override fun BoxScope.Darstellung() {
        TODO("Not yet implemented")
    }

/*    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        StandardKontextFenster(pos)
    }*/

/*    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }*/

    public companion object {
        public const val ANSCHLUSS_ART = "default"
    }
}
