package de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl

import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt

data class EinzelAuswahl(
    val auswahlId: String
): AuswahlDaten {
    constructor(ausgwählt: GraphObjekt): this(ausgwählt.daten.id)

    override fun enthält(gO: GraphObjekt): Boolean = gO.daten.id == auswahlId

}