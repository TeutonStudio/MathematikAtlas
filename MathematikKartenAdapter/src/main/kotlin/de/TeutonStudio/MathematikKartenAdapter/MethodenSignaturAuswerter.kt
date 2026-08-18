package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeArgumentKomponente
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeMethode
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.SignaturtragendeMethode
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.Tupelraum
import de.TeutonStudio.MathematikRechenSystem.kern.mathematischeMethodenSignatur

const val METHODEN_WERTEVORRAT_ART = "mathematik.methodenWertevorrat"
const val METHODEN_WERTEGRUNDRAUM_ART = "mathematik.methodenWertegrundraum"
const val METHODEN_ARGUMENTANZAHL_ART = "mathematik.methodenArgumentanzahl"
const val METHODEN_ARGUMENTE_ART = "mathematik.methodenArgumente"
const val METHODEN_ARGUMENTE_PROJEKTION = "methodenArgumente.projektion"
const val METHODEN_ZIELMENGE_ERGEBNISPROJEKTION = "methodenZielmenge.ergebnisprojektion"

fun methodenArgumentAusgangName(argument: MathematischeArgumentKomponente, index: Int): String {
    val name = argument.name.trim().ifBlank { "argument-${index + 1}" }
    return if (name == "dimension") "argument-${index + 1}" else name
}

fun MathematikAuswerterRegister.registriereMethodenArgumente() {
    registriere(METHODEN_ARGUMENTE_ART, MethodenArgumenteAuswerter)
    registriere(METHODEN_WERTEGRUNDRAUM_ART, MethodenWertegrundraumAuswerter)
}

/** Mathematischer Gesamtdefinitionsraum einer Methode. */
internal object MethodenWertevorratAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode ?: error("Eine konkrete Methode fehlt.")
        val definitionsRaum = methode.mathematischeMethodenSignatur().definitionsRaum
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "menge" to BedingterWert(
                    objekt = definitionsRaum,
                    variablenQuellen = methodenWert.variablenQuellen,
                ),
            ),
        )
    }
}

/** Gemeinsame Komponenten-Definitionsmenge, falls alle Argumente dieselbe besitzen. */
internal object MethodenWertegrundraumAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode ?: error("Eine konkrete Methode fehlt.")
        val argumente = methode.mathematischeMethodenSignatur().argumente
        check(argumente.isNotEmpty()) {
            "Eine nullstellige Methode besitzt keinen Wertegrundraum."
        }
        val grundraum = argumente.first().definitionsMenge
        check(argumente.all { it.definitionsMenge == grundraum }) {
            "Die Methodenargumente besitzen keinen gemeinsamen Wertegrundraum."
        }
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "menge" to BedingterWert(
                    objekt = grundraum,
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
        val argumentAnzahl = when (methode) {
            is MathematischeMethode -> methode.parameter.size
            is SignaturtragendeMethode -> methode.signatur.argumente.size
            else -> error("Die Methode '${methode.name}' stellt keine semantische Argumentstruktur bereit.")
        }
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

/** Mathematische Argumentreflexion; generische Methodenanschlüsse verwenden die neutrale Signatur direkt. */
internal object MethodenArgumenteAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode
            ?: error("Die Methodensignatur ist noch unbekannt.")
        val argumente = methode.mathematischeMethodenSignatur().argumente
        val werte = argumente.map { argument ->
            val quelle = VariablenQuelle(
                kontext.knoten.id,
                argument.name,
                argument.definitionsMenge,
                alsMethodenParameter = true,
            )
            BedingterWert(
                objekt = argument.parameter,
                werteVorrat = argument.definitionsMenge,
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
                    objekt = Tupel(argumente.map { it.parameter }),
                    variablenQuellen = werte.flatMap(BedingterWert::variablenQuellen).distinct(),
                ),
            )
        }
        return KnotenAuswertungsErgebnis(ausgaben = ausgaben)
    }
}

/**
 * Liefert die mathematische Zielmenge derselben Ergebnisprojektion, die auch ein
 * nachfolgender Methodenaufruf verwendet. Die kanonische Signatur bleibt intern
 * immer ein Tupelraum; die direkte Einzelkomponentenprojektion ist explizit Legacy-UI.
 */
internal object MethodenZielmengeSignaturAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? Methode ?: error("Eine konkrete Methode fehlt.")
        val signatur = methode.mathematischeMethodenSignatur()
        val projektion = kontext.knoten.parameter[METHODEN_ZIELMENGE_ERGEBNISPROJEKTION]
            ?: METHODEN_ERGEBNISPROJEKTION_DIREKT
        val zielMenge = when {
            signatur.ergebnisse.size != 1 -> signatur.zielRaum
            projektion == METHODEN_ERGEBNISPROJEKTION_DIREKT -> signatur.ergebnisse.single().zielMenge
            signatur.ergebnisse.single().zielMenge is Tupelraum -> signatur.ergebnisse.single().zielMenge
            else -> Tupelraum(listOf(signatur.ergebnisse.single().zielMenge))
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