package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * Wertet ausführbare Definitionskarten mit symbolischen Vorschauwerten aus.
 * Generische Dokumentationskarten bleiben reine Dokumentation.
 */
internal fun KartenAuswerter.werteKonzeptKarteAus(karte: KartenDaten): KartenAuswertungsErgebnis {
    val enthältNichtAuswertbareDokumentation = karte.knoten.any { knoten ->
        knoten.art.startsWith("konzept.") && knoten.art != TestDefinitionsKarten.KONZEPT_EINGANG_ART
    }
    if (enthältNichtAuswertbareDokumentation) {
        return KartenAuswertungsErgebnis(emptyMap(), emptyList())
    }

    val vorgaben = karte.knoten
        .filter { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART }
        .associate { knoten ->
            val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
            knoten.id to mapOf(ausgang.name to knoten.vorschauWert(ausgang))
        }

    return auswerten(karte, vorgaben)
}

private fun KnotenDaten.vorschauWert(ausgang: AnschlussDaten): BedingterWert {
    val parameterName = name.trim().ifBlank { "eingang" }
    return when (ausgang.art) {
        MathematikAnschlussArten.Zahl.id -> {
            val variable = Variable(parameterName)
            BedingterWert(
                objekt = variable,
                werteVorrat = ReelleZahlen,
                reelleVariablen = mapOf(parameterName to ReelleZahlen),
                variablenQuellen = listOf(
                    VariablenQuelle(id, parameterName, ReelleZahlen, alsMethodenParameter = false),
                ),
            )
        }

        MathematikAnschlussArten.Aussage.id -> {
            val wahrheitsMenge = EndlicheMenge(
                setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)),
            )
            val aussage = parameter["vorschauWert"]
                ?.toBooleanStrictOrNull()
                ?.let(::WahrheitsKonstante)
                ?: UnentscheidbareAussage(parameterName, "Definitionskarte")
            BedingterWert(
                objekt = aussage,
                werteVorrat = wahrheitsMenge,
            )
        }

        else -> {
            val wert = AllgemeinerParameter(parameterName)
            val werteVorrat = BenannteMenge(
                "definitionswerte_$parameterName",
                "\\mathcal{W}_{${parameterName}}",
            )
            BedingterWert(
                objekt = wert,
                werteVorrat = werteVorrat,
                variablenQuellen = listOf(
                    VariablenQuelle(id, parameterName, werteVorrat, alsMethodenParameter = false),
                ),
            )
        }
    }
}
