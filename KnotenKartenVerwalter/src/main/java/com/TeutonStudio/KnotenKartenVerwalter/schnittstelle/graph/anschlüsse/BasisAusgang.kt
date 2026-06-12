package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.ui.layout.LayoutCoordinates
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisAusgang(
    graph: Graph,
    daten: AusgangDaten,
    override val besitzer: Knoten,
): RichtungsAnschluss<AusgangDaten>(graph,daten, besitzer) {

    override fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean = super.erlaubtVerbindung(anschluss) && !anschluss.istAusgang()

    public companion object {
        public const val ANSCHLUSS_ART = "output"
    }
}