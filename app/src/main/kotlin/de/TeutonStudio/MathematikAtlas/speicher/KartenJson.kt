package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.MathematikAtlas.migriereTranspositionsKnoten
import de.TeutonStudio.MathematikKnoten.migriereIterierteSelbstkompositionKnoten
import org.json.JSONObject

/** App-Fassade: reiner Codec im Verwalter, App-spezifische Migration anschließend. */
object KartenJson {
    fun schreibe(karte: KartenDaten): String =
        KartenDatenJson.schreibe(karte.migriereIterierteSelbstkompositionKnoten())

    fun lese(text: String): KartenDaten = lese(JSONObject(text))

    fun lese(json: JSONObject): KartenDaten = KartenDatenJson.lese(json)
        .let(::migriereTranspositionsKnoten)
        .migriereIterierteSelbstkompositionKnoten()
}
