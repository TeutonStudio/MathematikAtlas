package com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl

import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt

data class EinzelAuswahl(
    val auswahlId: String
): AuswahlDaten {
    constructor(ausgwählt: GraphObjekt): this(ausgwählt.daten.id)

    override fun enthält(gO: GraphObjekt): Boolean = gO.daten.id == auswahlId

}