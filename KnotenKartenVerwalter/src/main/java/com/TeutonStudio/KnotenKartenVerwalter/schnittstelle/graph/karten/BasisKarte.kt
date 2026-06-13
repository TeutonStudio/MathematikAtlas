package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BasisVerbindungFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik

open class BasisKarte(
    graph: Graph,
    daten: KarteDaten,
    override val zustand: KarteZustand,
    override val aktualisierung: KartenAktualisierung,
    override val onVerbindungErstellen: VerbindungErstellen,
//    override val onKontextAktion: KontextAktionAusführen,
    override val onAuswahlÄndern: AuswahlÄndern,
): Karte(graph,daten) {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik

    override val pseudoVerbindung = mutableStateOf<Verbindung?>(null)

//    private fun pos(arg: Map.Entry<AnschlussDaten, KnotenDaten>): KartenPosition = (arg.value to arg.key).pos()

    public companion object {
        public const val KARTEN_ART: KnotenArt = "default"
    }
}