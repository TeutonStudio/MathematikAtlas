package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.MethodenArgument
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.Tupelraum
import de.TeutonStudio.MathematikRechenSystem.kern.VariablenQuelle
import de.TeutonStudio.MathematikRechenSystem.kern.argumentAnzahl
import de.TeutonStudio.MathematikRechenSystem.kern.methodenSignatur

const val METHODEN_WERTEVORRAT_ART = "mathematik.methodenWertevorrat"
const val METHODEN_ARGUMENTANZAHL_ART = "mathematik.methodenArgumentanzahl"
const val METHODEN_ARGUMENTE_ART = "mathematik.methodenArgumente"
const val METHODEN_ARGUMENTE_PROJEKTION = "methodenArgumente.projektion"
const val METHODEN_ZIELMENGE_ERGEBNISPROJEKTION = "methodenZielmenge.ergebnisprojektion"

fun methodenArgumentAusgangName(argument: MethodenArgument, index: Int): String {
    val name = argument.parameter.name.trim().ifBlank { "argument-${index + 1}" }
    return if (name == "dimension") "argument-${index + 1}" else name
}

fun MathematikAuswerterRegister.registriereMethodenArgumente() {
    registriere(METHODEN_ARGUMENTE_ART, MethodenArgumenteAuswerter)
}

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

/** Historischer, nur noch für bestehende Karten geladener Argumentanzahl-Knoten. */
internal object MethodenArgumentanzahlAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode
            ?: error("Die Methodensignatur ist noch unbekannt.")
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "anzahl" to BedingterWert(
                    objekt = RationaleZahl.von(methode.argumentAnzahl.toLong()),
                    variablenQuellen = methodenWert.variablenQuellen,
                ),
            ),
        )
    }
}

internal object MethodenArgumenteAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode
            ?: error("Die Methodensignatur ist noch unbekannt.")
        val argumente = methode.methodenSignatur().argumente
        val werte = argumente.map { argument ->
            val quelle = VariablenQuelle(
                kontext.knoten.id,
                argument.parameter.name,
                argument.werteVorrat,
                alsMethodenParameter = true,
            )
            BedingterWert(
                objekt = argument.parameter,
                werteVorrat = argument.werteVorrat,
                variablenQuellen = methodenWert.variablenQuellen + quelle,
            )
        }
        val projektion = kontext.knoten.parameter[METHODEN_ARGUMENTE_PROJEKTION]
            ?: METHODEN_ARGUMENTPROJEKTION_TUPEL

        val ausgaben = when (projektion) {
            METHODEN_ARGUMENTPROJEKTION_SEPARIERT -> buildMap {
                argumente.forEachIndexed { index, argument ->
                    put(methodenArgumentAusgangName(argument, index), werte[index])
                }
                put(
                    "dimension",
                    BedingterWert(
                        objekt = RationaleZahl.von(argumente.size.toLong()),
                        variablenQuellen = methodenWert.variablenQuellen,
                    ),
                )
            }
            else -> mapOf(
                "argumente" to BedingterWert(
                    objekt = Tupel(werte.map(BedingterWert::objekt)),
                    variablenQuellen = werte.flatMap(BedingterWert::variablenQuellen).distinct(),
                ),
            )
        }
        return KnotenAuswertungsErgebnis(ausgaben = ausgaben)
    }
}

/**
 * Liefert die Zielmenge derselben Ergebnisprojektion, die auch ein nachfolgender
 * Methodenaufruf verwendet. Ein bereits tupeliger Zielraum wird dabei niemals
 * nochmals als Einertupelraum verschachtelt.
 */
internal object MethodenZielmengeSignaturAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode ?: error("Eine konkrete Methode fehlt.")
        val roheZielMenge = methode.methodenSignatur().zielMenge
        val projektion = kontext.knoten.parameter[METHODEN_ZIELMENGE_ERGEBNISPROJEKTION]
            ?: METHODEN_ERGEBNISPROJEKTION_DIREKT
        val zielMenge = if (
            projektion == METHODEN_ERGEBNISPROJEKTION_TUPEL && roheZielMenge !is Tupelraum
        ) {
            Tupelraum(listOf(roheZielMenge))
        } else {
            roheZielMenge
        }
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
