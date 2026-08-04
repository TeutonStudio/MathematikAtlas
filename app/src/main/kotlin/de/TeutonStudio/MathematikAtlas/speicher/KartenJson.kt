package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.MathematikAtlas.migriereTranspositionsKnoten

/** App-Fassade: reiner Codec im Verwalter, App-spezifische Migration anschließend. */
object KartenJson {
    fun schreibe(karte: KartenDaten): String = KartenDatenJson.schreibe(karte)

    fun lese(text: String): KartenDaten =
        KartenDatenJson.lese(text).let(::migriereTranspositionsKnoten)
}
