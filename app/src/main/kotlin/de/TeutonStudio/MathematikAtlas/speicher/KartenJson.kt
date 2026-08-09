package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKnoten.MathematikKartenCodec
import org.json.JSONObject

/** App-Kompatibilitätsfassade für den plattformneutralen mathematischen Karten-Codec. */
object KartenJson {
    fun schreibe(karte: KartenDaten): String = MathematikKartenCodec.schreibe(karte)

    fun lese(text: String): KartenDaten = MathematikKartenCodec.lese(text)

    fun lese(json: JSONObject): KartenDaten = MathematikKartenCodec.lese(json)
}
