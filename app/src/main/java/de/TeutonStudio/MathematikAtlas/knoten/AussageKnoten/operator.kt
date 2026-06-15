package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenEingabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullSystem

// lässt den Nutzer auswählen welcher Aussagen Operator (bei Assoziativ, kommutativ, automatisch Anschluss hinzufügen, wenn passende verbindung gezogen wird.
class operator(
    graph: Graph,
    daten: KnotenDaten,
    override val besitzer: Karte,
): Knoten(graph,daten), PullSystem<KnotenDaten>  {
}