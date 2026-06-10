package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisEingang(
    _graph: Graph,
    override val daten: EingangDaten,
    override val besitzer: Knoten,
): RichtungsAnschluss(_graph,daten, besitzer) {

    override fun erlaubtVerbindung(daten: Anschluss): Boolean = super.erlaubtVerbindung(daten) && daten.istAusgang()

    public companion object {
        public const val ANSCHLUSS_ART = "input"
    }
}