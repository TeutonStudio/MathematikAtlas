package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisAusgang(
    _graph: Graph,
    override val daten: AusgangDaten,
    override val besitzer: Knoten,
): RichtungsAnschluss(_graph,daten, besitzer) {

    override fun erlaubtVerbindung(anschluss: Anschluss): Boolean = super.erlaubtVerbindung(anschluss) && !anschluss.istAusgang()

    public companion object {
        public const val ANSCHLUSS_ART = "output"
    }
}