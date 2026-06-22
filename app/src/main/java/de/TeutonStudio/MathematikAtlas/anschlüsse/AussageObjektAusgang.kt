package de.TeutonStudio.MathematikAtlas.anschlüsse

import androidx.compose.runtime.derivedStateOf
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert.BasisDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BezierObjektVerbindung


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