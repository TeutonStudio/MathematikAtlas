package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.methodenSignatur

const val METHODEN_WERTEVORRAT_ART = "mathematik.methodenWertevorrat"
const val METHODEN_ARGUMENTANZAHL_ART = "mathematik.methodenArgumentanzahl"

internal object MethodenWertevorratAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode ?: error("Eine konkrete Methode fehlt.")
        val werteVorrat = methode.methodenSignatur().werteVorrat
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "menge" to BedingterWert(
                    objekt = werteVorrat,
                    variablenQuellen = methodenWert.variablenQuellen,
                ),
            ),
        )
    }
}

internal object MethodenArgumentanzahlAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode ?: error("Eine konkrete Methode fehlt.")
        val argumentAnzahl = methode.methodenSignatur().argumente.size
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "anzahl" to BedingterWert(
                    objekt = RationaleZahl.von(argumentAnzahl.toLong()),
                    variablenQuellen = methodenWert.variablenQuellen,
                ),
            ),
        )
    }
}

/**
 * Verwendet ausschließlich die kanonische Methodensignatur. Historische öffentliche
 * Mehrfachausgaben sind im Methodenmodell bereits zu einem Tupelergebnis normalisiert.
 */
internal object MethodenZielmengeSignaturAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode ?: error("Eine konkrete Methode fehlt.")
        val zielMenge = methode.methodenSignatur().zielMenge
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "menge" to BedingterWert(
                    objekt = zielMenge,
                    variablenQuellen = methodenWert.variablenQuellen,
                ),
            ),
        )
    }
}
