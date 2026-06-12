package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

interface GraphAnschlussObjekt<D: AnschlussDaten>: GraphDatenObjekt<D> {
    public val besitzer: Knoten
    public val karte get() = besitzer.besitzer
}