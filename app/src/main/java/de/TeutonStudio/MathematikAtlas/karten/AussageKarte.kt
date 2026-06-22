package de.TeutonStudio.MathematikAtlas.karten

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Auswahl
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Kontext
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Zustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindete
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.veränderung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.wählte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisKartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisKnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisVerbindungFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KartenKonstruktor
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.VerbindungFabrik
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageKnotenFabrik

class AussageKarteDaten(
    override val id: String,
    override val name: String,
    initialKnoten: List<GraphDatenKnoten> = emptyList(),
    initialVerbindungen: List<GraphDatenVerbindung> = emptyList(),
): GraphDatenKarte{
    override val knoten = mutableStateListOf<GraphDatenKnoten>()
    override val verbindungen = mutableStateListOf<GraphDatenVerbindung>()
    init {
        knoten.addAll(initialKnoten)
        verbindungen.addAll(initialVerbindungen)
    }

    override var breite = 0f
    override var tiefe = 0f
    override var klasse: KartenArt? = AussageKarte.KARTEN_ART
}

class AussageKarte(
    override val graph: Graph,
    override val daten: AussageKarteDaten,
    veränderung: veränderung,
    verbindete: verbindete,
    wählte: wählte,
): GraphDatenObjektKarte<AussageKarteDaten> {
    override val knotenFabrik: KnotenFabrik = BasisKnotenFabrik + AussageKnotenFabrik
    override val verbindungFabrik: VerbindungFabrik = BasisVerbindungFabrik

    override val ctx = Kontext()
    override val auswahl = Auswahl()
    override val zustand: Zustand = Zustand()
    override val pseudoVerbindung = mutableStateOf<GraphDatenObjektVerbindung<*>?>(null)
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override fun beiKlick(klickPos: Offset) {
        auswahl.leereAuswahl()
    }

    override fun beiHalten(klickPos: Offset) {
//        super.beiHalten(klickPos)
//        TODO("Not yet implemented")
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
//        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntSize) {
        Column() {
            Text("KontextFenster der AussageKarte")
        }
    }

    @Composable
    override fun BoxScope.Inspektor() {}

    companion object {
        const val KARTEN_ART: KartenArt = "aussage-karte"
    }
}

@Suppress("UNCHECKED_CAST")
val MatheKartenFabrik: KartenFabrik = BasisKartenFabrik + mapOf(
        AussageKarte.KARTEN_ART to (::AussageKarte as KartenKonstruktor)
    )