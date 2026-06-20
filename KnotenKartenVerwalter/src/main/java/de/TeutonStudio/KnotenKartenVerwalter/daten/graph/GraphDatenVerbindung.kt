package de.TeutonStudio.KnotenKartenVerwalter.daten.graph

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.VerbindungArt

interface GraphDatenVerbindung: GraphDaten {
    override var klasse: VerbindungArt?

    public val ids: IDEhe
    public val label: String?
    public val fehler: String?

    data class IDEhe(
        val knotenIdMann: String, val anschlussIdMann: String,
        val knotenIdWeib: String, val anschlussIdWeib: String,
    ) {
        public fun enthält(anschluss: GraphDatenAnschluss): Boolean {
            if (anschlussIdMann == anschluss.id) return true
            if (anschlussIdWeib == anschluss.id) return true
            return false
        }

        constructor(
            anschlussMann: GraphDatenObjektAnschluss<*>,
            anschlussWeib: GraphDatenObjektAnschluss<*>,
        ): this(
            anschlussMann.besitzer.daten.id,anschlussMann.daten.id,
            anschlussWeib.besitzer.daten.id,anschlussWeib.daten.id,
        )
    }
}