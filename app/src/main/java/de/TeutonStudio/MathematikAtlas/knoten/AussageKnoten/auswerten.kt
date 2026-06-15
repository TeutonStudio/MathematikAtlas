package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAusgabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Cache
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullSystem
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik

// einzelner Eingang, wertet aus ob Wahr, Lüge oder unentscheidbar
class auswerten(
    graph: Graph,
    daten: KnotenAusgabeDaten,
    besitzer: Karte,
): BasisKnoten(graph,daten,besitzer), PullSystem<KnotenDaten> {
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val cacheAnschlüsse: SnapshotStateMap<AnschlussDaten, Cache> = mutableStateMapOf()

    public companion object {
        public const val KNOTEN_ART: KnotenArt = "auswertenAussage"
    }
}