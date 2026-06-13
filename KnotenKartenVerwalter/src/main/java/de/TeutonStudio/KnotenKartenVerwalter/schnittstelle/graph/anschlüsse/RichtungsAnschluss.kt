package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

interface RichtungsAnschluss<D: RichtungsAnschlussDaten>: GraphAnschlussObjekt<D> {

//    override fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean = super.erlaubtVerbindung(anschluss)

    public companion object {
        public const val ANSCHLUSS_ART = "dir"
    }
    public override fun istEingang(): Boolean = this is BasisEingang || daten.richtung == AnschlussRichtung.Eingang
    public override fun istAusgang(): Boolean = this is BasisAusgang || daten.richtung == AnschlussRichtung.Ausgang
}