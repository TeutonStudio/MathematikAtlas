package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

sealed class RichtungsAnschluss(
    _graph: Graph,
    override val daten: RichtungsAnschlussDaten,
    override val besitzer: Knoten,
): Anschluss(_graph) {
    override fun erlaubtVerbindung(anschluss: Anschluss): Boolean = super.erlaubtVerbindung(anschluss)

    public companion object {
        public const val ANSCHLUSS_ART = "dir"
    }
    public override fun istEingang(): Boolean = this is BasisEingang || daten.richtung == AnschlussRichtung.Eingang
    public override fun istAusgang(): Boolean = this is BasisAusgang || daten.richtung == AnschlussRichtung.Ausgang
}