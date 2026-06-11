package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

open class BasisAnschluss(
    _graph: Graph,
    override val daten: AnschlussDaten,
    override val besitzer: Knoten,
//    override var partner: Anschluss? = null,
): Anschluss(_graph) {

    override fun erlaubtVerbindung(anschluss: Anschluss): Boolean = !istSelbst(anschluss.besitzer)

    public companion object {
        public const val ANSCHLUSS_ART = "default"
    }
}