package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisEingang(
    graph: Graph,
    daten: EingangDaten,
    override val besitzer: Knoten,
): RichtungsAnschluss<EingangDaten>(graph,daten, besitzer) {

    override fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean = super.erlaubtVerbindung(anschluss) && !anschluss.istEingang()

    public companion object {
        public const val ANSCHLUSS_ART = "input"
    }
}