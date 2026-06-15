package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenEingabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Cache
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.EingabeKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullSystem
import de.TeutonStudio.MathematikAtlas.anschlüsse.MatheAnschlussFabrik

class definition(
    graph: Graph,
    daten: KnotenEingabeDaten,
    besitzer: Karte,
): EingabeKnoten(graph,daten,besitzer), PullSystem<KnotenEingabeDaten> {
    override val anschlussFabrik: AnschlussFabrik get() = MatheAnschlussFabrik
    override val cacheAnschlüsse: SnapshotStateMap<AnschlussDaten, Cache> = mutableStateMapOf()


    @Composable
    override fun Textzeile() {
        // Schalter Wahr, Lüge
    }

    public companion object {
        public const val KNOTEN_ART: KnotenArt = "eingabeAussage"
    }
}