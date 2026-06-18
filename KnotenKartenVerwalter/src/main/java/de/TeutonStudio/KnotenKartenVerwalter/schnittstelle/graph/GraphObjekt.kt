package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung

interface GraphObjekt {
    public val graph: Graph
    public fun registriere() = also { graph.inhalt.add(it) }

}