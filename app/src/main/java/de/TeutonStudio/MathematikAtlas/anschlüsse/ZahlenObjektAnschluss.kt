package de.TeutonStudio.MathematikAtlas.anschlüsse

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussArt

class ZahlenObjektAnschluss(
    graph: Graph,
    daten: ZahlenAnschlussDaten,
    besitzer: GraphDatenObjektKnoten<*>,
) : AussageObjektAnschluss(graph, daten, besitzer) {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override var dragPos = mutableStateOf(Offset.Zero)

    class ZahlenAnschlussDaten(
        override val id: GraphDatenId,
        override val kante: Kante,
        override val richtung: Richtung,
    ) : AussageObjektAnschluss.AussageAnschlussDaten(id, kante, richtung) {
        override var label = ""
        override var klasse: AnschlussArt? = ANSCHLUSS_ART
        override var cache: GraphDatenAnschluss.auswertbarerGDA.PullDaten<*> by mutableStateOf(
            CacheDaten()
        )

        override fun erlaubeVerbindung(mit: GraphDatenAnschluss): Boolean =
            super.erlaubeVerbindung(mit) && mit is ZahlenAnschlussDaten

        override fun baueCache(
            eingangCache: List<GraphDatenAnschluss.auswertbarerGDA.PullDaten<*>?>,
        ): GraphDatenAnschluss.auswertbarerGDA.PullDaten<*> =
            eingangCache.firstNotNullOfOrNull { it as? CacheDaten } ?: CacheDaten()

        class CacheDaten(
            val latex: String = UNBEKANNT,
        ) : GraphDatenAnschluss.auswertbarerGDA.PullDaten<String>(latex) {
            override fun ausSpeicher(wert: String): String = wert
            override fun zuSpeicher(wert: String): String = wert
        }
    }

    companion object {
        const val ANSCHLUSS_ART: AnschlussArt = "zahl"
        const val EINGANG_ART: AnschlussArt = "zahl-eingang"
        const val AUSGANG_ART: AnschlussArt = "zahl-ausgang"
        const val UNBEKANNT = "?"
    }
}
