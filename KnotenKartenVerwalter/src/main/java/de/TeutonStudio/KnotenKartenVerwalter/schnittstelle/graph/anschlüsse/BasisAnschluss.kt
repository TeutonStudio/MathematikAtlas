package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisAnschluss(
    graph: Graph,
    datenAnschluss: AnschlussDaten,
    override val besitzer: Knoten,
): Anschluss<AnschlussDaten>(graph,datenAnschluss) {
    override var layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)

    public override fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean = !istSelbst(anschluss.besitzer)

    public companion object {
        public const val ANSCHLUSS_ART = "default"
    }
}
