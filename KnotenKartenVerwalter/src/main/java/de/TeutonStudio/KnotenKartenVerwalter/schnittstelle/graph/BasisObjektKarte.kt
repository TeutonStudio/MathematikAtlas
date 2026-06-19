package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BasisVerbindungFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik

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
    override fun BoxScope.KontextFenster(pos: BildschirmPosition) {
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