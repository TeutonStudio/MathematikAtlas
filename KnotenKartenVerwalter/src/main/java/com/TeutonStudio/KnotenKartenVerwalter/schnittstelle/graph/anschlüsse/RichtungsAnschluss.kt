package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

sealed class RichtungsAnschluss(
    _graph: Graph,
    override val daten: RichtungsAnschlussDaten,
    override val besitzer: Knoten,
): Anschluss(_graph) {

    override fun erlaubtVerbindung(daten: Anschluss): Boolean = daten.istEingang()

    public companion object {
        public const val ANSCHLUSS_ART = "output"
    }
    public fun Anschluss.istEingang(): Boolean = if (this is RichtungsAnschluss) this is BasisEingang || daten.richtung == AnschlussRichtung.Eingang else false
    public fun Anschluss.istAusgang(): Boolean = if (this is RichtungsAnschluss) this is BasisAusgang || daten.richtung == AnschlussRichtung.Ausgang else false
}