package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussArt

open class AussageAnschlussDaten(
    override val id: GraphDatenId,
    override val kante: Kante,
    override val richtung: Richtung,
): GraphDatenAnschluss, GraphDatenAnschluss.gerichteteGDA, GraphDatenAnschluss.auswertbarerGDA {
    override var label = ""
    override var cache: GraphDatenAnschluss.auswertbarerGDA.PullDaten<*> = CacheDaten()
    override var klasse: AnschlussArt? = "" // TODO
    override fun baueCache(eingangCache: List<GraphDatenAnschluss.auswertbarerGDA.PullDaten<*>?>): GraphDatenAnschluss.auswertbarerGDA.PullDaten<*> {
        TODO("Not yet implemented")
    }

    class CacheDaten(): GraphDatenAnschluss.auswertbarerGDA.PullDaten<Any>("") {
        override fun ausSpeicher(wert: String): Any {
            TODO("Not yet implemented")
        }

        override fun zuSpeicher(wert: Any): String {
            TODO("Not yet implemented")
        }

    }

}