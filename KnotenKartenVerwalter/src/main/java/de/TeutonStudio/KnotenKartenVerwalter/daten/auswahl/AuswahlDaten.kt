package de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl

//import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKnoten
//import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
//import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt


/**
 * Beschreibt die aktuell ausgewählten Graphobjekte.
 *
 * Die Auswahl ist kontrollierter Zustand der Karte; die UI schlägt Änderungen vor,
 * der aufrufende Code entscheidet über den gespeicherten Wert.
 */
/*
interface AuswahlDaten {

    */
/** Prüft, ob das Graphobjekt in dieser Auswahl enthalten ist. *//*

    public fun enthält(gO: GraphObjekt): Boolean


    public companion object {
        public val LEER: AuswahlDaten = MultiAuswahl()

        public fun VerbindungDaten.zuAuswahl() = MultiAuswahl(verbindungIds = setOf(id))
        public fun GraphDatenKnoten.zuAuswahl() = MultiAuswahl(knotenIds = setOf(id))
    }

}
*/
