package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.BasisAusgang
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

class AussageAusgang(
    graph: Graph,
    daten: AusgangDaten,
    besitzer: Knoten,
): BasisAusgang(graph,daten,besitzer) {
    override fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean {
        return super.erlaubtVerbindung(anschluss) && anschluss is AussageAusgang
    }

    public companion object {
        public const val ANSCHLUSS_ART = "outputAussage"
    }
}