package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

@JvmInline
value class KonzeptId(val wert: String) {
    override fun toString(): String = wert
}

enum class KonzeptReiterRolle { Definition, Spezialfall, Beispiel, Äquivalenz }

enum class KomplexDarstellung { Kartesisch, Polar }

data class KonzeptReiter(
    val id: String,
    val titel: String,
    val rolle: KonzeptReiterRolle,
    val karte: KartenDaten,
    val darstellungsVarianten: Map<KomplexDarstellung, KartenDaten> = emptyMap(),
) {
    fun karteFür(darstellung: KomplexDarstellung): KartenDaten =
        darstellungsVarianten[darstellung] ?: karte

    val besitztDarstellungsVarianten: Boolean
        get() = darstellungsVarianten.isNotEmpty()
}

data class KonzeptDefinition(
    val id: KonzeptId,
    val name: String,
    val beschreibung: String,
    val pfad: List<String>,
    val tags: Set<String>,
    val knotenArten: Set<KnotenArtId>,
    val knotenParameter: Map<String, String> = emptyMap(),
    val reiter: List<KonzeptReiter>,
) {
    init {
        require(reiter.count { it.rolle == KonzeptReiterRolle.Definition } == 1) {
            "$name benötigt genau einen Definitionsreiter."
        }
    }

    fun erklärt(knoten: KnotenDaten): Boolean =
        knoten.art in knotenArten && knotenParameter.all { (schlüssel, wert) ->
            knoten.parameter[schlüssel] == wert
        }
}


internal object KonzeptKnotenArten {
    const val REGEL = "konzept.regel"
    const val EINGANG = "konzept.eingang"
    const val AUSGANG = "konzept.ausgang"
}
