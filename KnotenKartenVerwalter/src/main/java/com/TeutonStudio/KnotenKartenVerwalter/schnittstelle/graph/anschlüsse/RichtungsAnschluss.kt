package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.ui.layout.LayoutCoordinates
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

sealed class RichtungsAnschluss<D: RichtungsAnschlussDaten>(
    graph: Graph,
    daten: D,
    override val besitzer: Knoten,
): Anschluss<D>(graph,daten) {
    override var layoutCoordinates: LayoutCoordinates? = null

    override fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean = super.erlaubtVerbindung(anschluss)

    public companion object {
        public const val ANSCHLUSS_ART = "dir"
    }
    public override fun istEingang(): Boolean = this is BasisEingang || daten.richtung == AnschlussRichtung.Eingang
    public override fun istAusgang(): Boolean = this is BasisAusgang || daten.richtung == AnschlussRichtung.Ausgang
}