package de.TeutonStudio.MathematikAtlas.anschlüsse

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert.BasisDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BezierObjektVerbindung
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageWert

open class AussageObjektAnschluss(
    override val graph: Graph,
    override val daten: AussageObjektAnschluss.AussageAnschlussDaten,
    override val besitzer: GraphDatenObjektKnoten<*>,
) : GraphDatenObjektAnschluss<AussageObjektAnschluss.AussageAnschlussDaten> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override var dragPos = mutableStateOf(Offset.Zero)
    override var dragZiel = mutableStateOf<GraphDatenObjektAnschluss<*>?>(null)

    open class AussageAnschlussDaten(
        override val id: GraphDatenId,
        override val kante: Kante,
        override val richtung: Richtung,
    ): GraphDatenAnschluss, GraphDatenAnschluss.gerichteteGDA, GraphDatenAnschluss.auswertbarerGDA {
        override var label = ""
        override var cache: GraphDatenAnschluss.auswertbarerGDA.PullDaten<*> =
            CacheDaten(AussageWert.UNENTSCHEIDBAR)
        override var klasse: AnschlussArt? = "" // TODO
        override fun baueCache(eingangCache: List<GraphDatenAnschluss.auswertbarerGDA.PullDaten<*>?>): GraphDatenAnschluss.auswertbarerGDA.PullDaten<*> {
            val ersterEingang = eingangCache.firstNotNullOfOrNull { it as? CacheDaten }?.wert ?: AussageWert.UNENTSCHEIDBAR

            return CacheDaten(ersterEingang)
        }

        class CacheDaten(
            val wert: AussageWert = AussageWert.UNENTSCHEIDBAR,
        ): GraphDatenAnschluss.auswertbarerGDA.PullDaten<AussageWert>(wert.name) {
            override fun ausSpeicher(wert: String): AussageWert =
                AussageWert.entries.firstOrNull { it.name == wert }
                    ?: AussageWert.UNENTSCHEIDBAR

            override fun zuSpeicher(wert: AussageWert): String = wert.name

        }

    }
    override fun erhaltePseudoVerbindung(): GraphDatenObjektVerbindung<*> {
        return BezierObjektVerbindung(graph, BasisDatenVerbindung(
            "pseudo",GraphDatenVerbindung.IDEhe(this,this)),
            derivedStateOf { pos },
            derivedStateOf { dragZiel.value?.pos ?: dragPos.value },
        ).apply {
            startKante = this@AussageObjektAnschluss.daten.kante
            endeKante = startKante.gegenüber()
        }
    }
    @Composable
    override fun BoxScope.Darstellung() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Inspektor() {
        Text("Inpektor des Aussage Anschluss")
    }

    public companion object {
        public const val ANSCHLUSS_ART = "outputAussage"
    }
}
