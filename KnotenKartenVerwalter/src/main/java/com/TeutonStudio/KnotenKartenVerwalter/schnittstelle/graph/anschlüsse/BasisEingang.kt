package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisEingang(
    _graph: Graph,
    override val daten: EingangDaten,
    override val besitzer: Knoten,
): RichtungsAnschluss(_graph,daten, besitzer) {

    override fun erlaubtVerbindung(anschluss: Anschluss): Boolean = super.erlaubtVerbindung(anschluss) && !anschluss.istEingang()

    public companion object {
        public const val ANSCHLUSS_ART = "input"
    }
}