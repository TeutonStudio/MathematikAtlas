package com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl

import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt

data class EinzelAuswahl(
    val auswahlId: String
): AuswahlDaten {
    constructor(ausgwählt: GraphObjekt<out GraphDaten>): this(ausgwählt.daten.id)

    override fun enthält(gO: GraphObjekt<out GraphDaten>): Boolean = gO.daten.id == auswahlId

}