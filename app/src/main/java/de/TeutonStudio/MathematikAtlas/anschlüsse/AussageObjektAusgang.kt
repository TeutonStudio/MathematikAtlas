package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten


class AussageObjektAusgang(
    graph: Graph,
    daten: AussageAusgang,
    besitzer: GraphDatenObjektKnoten<*>,
): AussageObjektAnschluss(graph,daten,besitzer) {
    class AussageAusgang(
        override val id: GraphDatenId,
        override val kante: Kante,
        override val richtung: Richtung,
    ): AussageAnschlussDaten(id,kante, Richtung.Ausgang) {
    }

    public companion object {
        public const val ANSCHLUSS_ART = "outputAussage"
    }
}