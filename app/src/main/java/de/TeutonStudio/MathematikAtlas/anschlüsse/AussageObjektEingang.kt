package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten


class AussageObjektEingang(
    graph: Graph,
    daten: AussageEingang,
    besitzer: GraphDatenObjektKnoten<*>,
): AussageObjektAnschluss(graph,daten,besitzer) {
    class AussageEingang(
//    override val graph: Graph,
//    override val daten: GraphDatenAnschluss,
//    override val besitzer: GraphDatenObjektKnoten<*>,
        override val id: GraphDatenId,
        override val kante: Kante,
        override val richtung: Richtung,
    ): AussageAnschlussDaten(id,kante, Richtung.Eingang) {
    }

    public companion object {
        public const val ANSCHLUSS_ART = "inputAussage"
    }
}