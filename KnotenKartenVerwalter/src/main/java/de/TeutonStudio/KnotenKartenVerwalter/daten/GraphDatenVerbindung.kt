package de.TeutonStudio.KnotenKartenVerwalter.daten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungArt

interface GraphDatenVerbindung: GraphDaten {
    override var klasse: VerbindungArt?

    public val ids: IDEhe
    public val label: String?
    public val fehler: String?

    data class IDEhe(
        val knotenIdMann: String, val anschlussIdMann: String,
        val knotenIdWeib: String, val anschlussIdWeib: String,
    ) {
        constructor(
            anschlussMann: Anschluss<out GraphDatenAnschluss>,
            anschlussWeib: Anschluss<out GraphDatenAnschluss>,
        ): this(
            anschlussMann.besitzer.daten.id,anschlussMann.daten.id,
            anschlussWeib.besitzer.daten.id,anschlussWeib.daten.id,
        )
    }
}