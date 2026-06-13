package de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl

import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.MultiAuswahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt


/**
 * Beschreibt die aktuell ausgewaehlten Graph-Elemente.
 *
 * Die Auswahl ist als kontrollierter Zustand gedacht. Die UI kann eine neue Auswahl vorschlagen,
 * aber der aufrufende Code entscheidet, welche Elemente tatsaechlich als ausgewaehlt gespeichert
 * werden.
 */
interface AuswahlDaten {

    public fun enthält(gO: GraphObjekt): Boolean


    public companion object {
        public val LEER: AuswahlDaten = MultiAuswahl()

        public fun VerbindungDaten.zuAuswahl() = MultiAuswahl(verbindungIds = setOf(id))
        public fun <D: AnschlussDaten> KnotenDaten<D>.zuAuswahl() = MultiAuswahl(knotenIds = setOf(id))
    }

}