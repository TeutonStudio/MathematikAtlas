package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisEingang(
    graph: Graph,
    daten: EingangDaten,
    override val besitzer: Knoten,
): Anschluss<EingangDaten>(graph,daten), RichtungsAnschluss<EingangDaten> {
    override var layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)

    override fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean = super.erlaubtVerbindung(anschluss) && !anschluss.istEingang()

    public companion object {
        public const val ANSCHLUSS_ART = "input"
    }
}