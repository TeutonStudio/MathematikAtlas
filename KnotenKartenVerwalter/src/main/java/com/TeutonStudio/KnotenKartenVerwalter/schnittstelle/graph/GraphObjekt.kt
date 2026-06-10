package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

// Grundklasse für ein Objekt, dass auf einem KartenGraph erscheint
abstract class GraphObjekt(
    _graph: Graph
) {
    public abstract val daten: GraphDaten
    public lateinit var graph: Graph
    init { definiereGraph(_graph) }
    public fun definiereGraph(graph: Graph) { graph.inhalt.add(this); this.graph = graph }


    @Composable abstract fun zuComposable(modifier: Modifier = Modifier.Companion)

    @Composable
    open fun erhalteKontextFenster(pos: BildschirmPosition) = Unit
    public val öffneKontext = derivedStateOf { graph.ctx.first == daten.id }

    public val istSelektiert by derivedStateOf { graph.selektiert.enthält(this) }

    public fun KartenPosition.zuBild(zustand: KarteZustand = graph.karte.zustand): BildschirmPosition = (this + zustand.pos * zustand.zoom).round()
    public fun KartenPosition.zuBildAusKnoten(zustand: KarteZustand = graph.karte.zustand): BildschirmPosition = round()
    public fun BildschirmPosition.zuKarte(zustand: KarteZustand = graph.karte.zustand): KartenPosition = (this.toOffset() - zustand.pos * zustand.zoom)
    public fun BildschirmPosition.zuKnoten(
        knoten: Knoten,
        zustand: KarteZustand = graph.karte.zustand,
    ): KnotenPosition = this.toOffset() - knoten.daten.position

    // Der Inhalt des Inspectrs zu diesem Objekt
    fun erhalteInspectorFenster() = Unit

    // Auf dem Graph wird von einem Anschluss aus gezogen
    fun planeVerbindung(a: Anschluss) = Unit
    // Auf dem Graph wird eine gezogene Verbindung auf einem Anschluss dieses Knoten losgelassen
    // von ist dabei der Anschluss von dem gezogen wurde und nach der auf dem fallen gelassen wurde
    fun erstelleVerbindung(von: Anschluss, zu: Anschluss) = Unit

}
