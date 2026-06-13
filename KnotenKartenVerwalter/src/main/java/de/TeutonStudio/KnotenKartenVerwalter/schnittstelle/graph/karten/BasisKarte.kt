package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import de.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import de.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import de.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BasisVerbindungFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik

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