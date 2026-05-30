package com.TeutonStudio.MathematikAtlas.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.UnbekannteKnoten

// Latex-Anzeige, einzelner Ausgang, Zahlenraum-Auswahl im Inspektor.
object UnbekannteAtlasKnoten {
    const val ART: String = UnbekannteKnoten.KNOTEN_ART

    fun daten(variable: String): Map<String, Any> = matheDaten(
        art = ART,
        name = variable,
        daten = mapOf(
            "variable" to variable,
            "zahlenTyp" to ZahlenTyp(Zahlenraum.Reell, anzeigename = variable),
        ),
    )
}
