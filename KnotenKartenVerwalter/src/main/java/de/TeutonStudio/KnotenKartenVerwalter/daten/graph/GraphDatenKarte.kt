package de.TeutonStudio.KnotenKartenVerwalter.daten.graph

import androidx.compose.runtime.snapshots.SnapshotStateList
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenArt

interface GraphDatenKarte: GraphDaten, GraphDaten.benanntesGD, GraphDaten.orthogoneGD {
    override var klasse: KartenArt?

//    public val ctx
    public val knoten: SnapshotStateList<GraphDatenKnoten>
    public val verbindungen: SnapshotStateList<GraphDatenVerbindung>

    interface auswertbareKGD: GraphDatenKarte {

    }
}