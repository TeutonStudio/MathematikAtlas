package de.TeutonStudio.MathematikAtlas.anschlüsse

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussArt

open class AussageObjektAnschluss(
    override val graph: Graph,
    override val daten: AussageObjektAnschluss.AussageAnschlussDaten,
    override val besitzer: GraphDatenObjektKnoten<*>,
) : GraphDatenObjektAnschluss<AussageObjektAnschluss.AussageAnschlussDaten> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)

    open class AussageAnschlussDaten(
        override val id: GraphDatenId,
        override val kante: Kante,
        override val richtung: Richtung,
    ): GraphDatenAnschluss, GraphDatenAnschluss.gerichteteGDA, GraphDatenAnschluss.auswertbarerGDA {
        override var label = ""
        override var cache: GraphDatenAnschluss.auswertbarerGDA.PullDaten<*> = CacheDaten()
        override var klasse: AnschlussArt? = "" // TODO
        override fun baueCache(eingangCache: List<GraphDatenAnschluss.auswertbarerGDA.PullDaten<*>?>): GraphDatenAnschluss.auswertbarerGDA.PullDaten<*> {
            return CacheDaten()
        }

        class CacheDaten(): GraphDatenAnschluss.auswertbarerGDA.PullDaten<Any>("") {
            override fun ausSpeicher(wert: String): Any {
                return this
            }

            override fun zuSpeicher(wert: Any): String {
                return ""
            }

        }

    }
    override fun beiVerbindungZiehenStart(
        start: PointerInputChange,
        change: PointerInputChange,
        klickPos: Offset
    ) {
        TODO("Not yet implemented")
    }

    override fun beiVerbindungZiehenDelta(
        change: PointerInputChange,
        dragAmount: Offset
    ) {
        TODO("Not yet implemented")
    }

    override fun beiVerbindungZiehenEnde(change: PointerInputChange) {
        TODO("Not yet implemented")
    }

    override fun beiVerbindungZiehenAbbruch() {
        TODO("Not yet implemented")
    }

    override fun beiKlick(klickPos: Offset) {
        TODO("Not yet implemented")
    }

    override fun beiHalten(klickPos: Offset) {
        TODO("Not yet implemented")
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Darstellung() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntSize) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }

    public companion object {
        public const val ANSCHLUSS_ART = "outputAussage"
    }
}