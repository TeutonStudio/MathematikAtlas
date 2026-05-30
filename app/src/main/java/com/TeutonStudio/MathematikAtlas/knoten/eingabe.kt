package com.TeutonStudio.MathematikAtlas.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.MathematikEingabeKnoten

// Latex-Anzeige, Definition im Inspektor.
object EingabeAtlasKnoten {
    const val ART: String = MathematikEingabeKnoten.KNOTEN_ART

    fun daten(wert: String = ""): Map<String, Any> = matheDaten(
        art = ART,
        name = wert.ifBlank { "Eingabe" },
        daten = mapOf(
            "wert" to wert,
            "zahlenTyp" to ZahlenTyp(Zahlenraum.Reell, wert = wert.ifBlank { null }),
        ),
    )
}
