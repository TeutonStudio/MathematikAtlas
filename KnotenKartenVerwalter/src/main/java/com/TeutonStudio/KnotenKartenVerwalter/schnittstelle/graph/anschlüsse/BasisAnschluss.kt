package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisAnschluss(
    graph: Graph,
    datenAnschluss: AnschlussDaten,
    override val besitzer: Knoten,
//    override var partner: Anschluss? = null,
): Anschluss<AnschlussDaten>(graph,datenAnschluss) {

    override fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean = !istSelbst(anschluss.besitzer)

    public companion object {
        public const val ANSCHLUSS_ART = "default"
    }
}