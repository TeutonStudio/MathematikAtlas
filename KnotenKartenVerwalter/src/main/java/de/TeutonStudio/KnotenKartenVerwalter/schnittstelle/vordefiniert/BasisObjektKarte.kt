package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Kontext
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Zustand

class BasisObjektKarte(
    override val graph: Graph,
    override val daten: GraphDatenKarte,
) : GraphDatenObjektKarte<GraphDatenKarte> {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik
    override val layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)
    override fun beiKlick(klickPos: Offset) {
        TODO("Not yet implemented")
    }

    override fun beiHalten(klickPos: Offset) {
        TODO("Not yet implemented")
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntSize) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }

    override val pseudoVerbindung: MutableState<GraphDatenObjektVerbindung<*>?> = mutableStateOf(null)
    override val zustand: Zustand = Zustand()
    override val auswahl: Auswahl = Auswahl()
    override val ctx: Kontext = Kontext()

    public companion object {
        public const val KARTEN_ART = "default"
    }
}