package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.GraphDaten

// Grundklasse für ein Objekt, dass auf einem KartenGraph erscheint
abstract class GraphObjekt(
    _graph: Graph
) {
    public lateinit var graph: Graph
    init { definiereGraph(_graph) }
    public fun definiereGraph(graph: Graph) { graph.inhalt.add(this); this.graph = graph }

    public abstract val daten: GraphDaten

    @Composable abstract fun zuComposable(modifier: Modifier = Modifier.Companion)

    @Composable
    open fun erhalteKontextFenster(pos: BildschirmPosition) = Unit

    public val öffneKontext = derivedStateOf { graph.ctx.first == daten.id }

    val istSelektiert by derivedStateOf { graph.selektiert.enthält(this) }

    // Der Inhalt des Inspectrs zu diesem Objekt
    fun erhalteInspectorFenster() = Unit

    // Auf dem Graph wird von einem Anschluss aus gezogen
    fun planeVerbindung(a: Anschluss) = Unit
    // Auf dem Graph wird eine gezogene Verbindung auf einem Anschluss dieses Knoten losgelassen
    // von ist dabei der Anschluss von dem gezogen wurde und nach der auf dem fallen gelassen wurde
    fun erstelleVerbindung(von: Anschluss, zu: Anschluss) = Unit

}
