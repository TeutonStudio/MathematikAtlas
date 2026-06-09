package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.GraphDaten

// Grundklasse für ein Objekt, dass auf einem KartenGraph erscheint
sealed interface GraphObjekt {
    public var graph: Graph
    public fun definiereGraph(graph: Graph) { graph.inhalt.add(this); this.graph = graph }

    public val daten: GraphDaten

    @Composable fun zuComposable(modifier: Modifier = Modifier.Companion)

    @Composable fun erhalteKontextFenster(pos: BildschirmPosition) = Unit

    fun öffneKontext() = derivedStateOf { graph.ctx && graph.ctxObjekt.daten.id == daten.id }



    // Der Inhalt des Inspectrs zu diesem Objekt
    fun erhalteInspectorFenster() = Unit

    // Auf dem Graph wird von einem Anschluss aus gezogen
    fun planeVerbindung(a: Anschluss) = Unit
    // Auf dem Graph wird eine gezogene Verbindung auf einem Anschluss dieses Knoten losgelassen
    // von ist dabei der Anschluss von dem gezogen wurde und nach der auf dem fallen gelassen wurde
    fun erstelleVerbindung(von: Anschluss, zu: Anschluss) = Unit

}
