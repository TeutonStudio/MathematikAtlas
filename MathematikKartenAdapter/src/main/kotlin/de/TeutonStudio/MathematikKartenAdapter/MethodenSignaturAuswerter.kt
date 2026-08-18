package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*

const val METHODEN_WERTEVORRAT_ART = "mathematik.methodenWertevorrat"
const val METHODEN_WERTEGRUNDRAUM_ART = "mathematik.methodenWertegrundraum"
const val METHODEN_ARGUMENTANZAHL_ART = "mathematik.methodenArgumentanzahl"
const val METHODEN_ARGUMENTE_ART = "mathematik.methodenArgumente"
const val METHODEN_ARGUMENTE_PROJEKTION = "methodenArgumente.projektion"
const val METHODEN_ZIELMENGE_ERGEBNISPROJEKTION = "methodenZielmenge.ergebnisprojektion"

/** Sichtbarer Komponentenname ohne Rückgriff auf mathematische Parameterobjekte. */
fun methodenArgumentAusgangName(argument: MethodenKomponente, index: Int): String {
    val name = argument.name.trim().ifBlank { "argument-${index + 1}" }
    return if (name == "dimension") "argument-${index + 1}" else name
}

fun MathematikAuswerterRegister.registriereMethodenArgumente() {
    registriere(METHODEN_ARGUMENTE_ART, MethodenArgumenteAuswerter)
    registriere(METHODEN_WERTEGRUNDRAUM_ART, MethodenWertegrundraumAuswerter)
}

/**
 * Historischer Knotenname. Fachlich liefert der Knoten nun den tatsächlichen
 * mathematischen Definitionsraum und damit bei 0/1/n Argumenten immer die kanonische
 * Tupelraum-Semantik aus #431.
 */
internal object MethodenWertevorratAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? MathematischeSignaturtragendeMethode
            ?: error("Der Methoden-Wertevorrat benötigt eine mathematische Raum-/Mengensignatur.")
        val definitionsRaum = methode.mathematischeSignatur.definitionsRaum
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

internal object MethodenWertegrundraumAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? MathematischeSignaturtragendeMethode
            ?: error("Der Methoden-Wertegrundraum benötigt eine mathematische Raum-/Mengensignatur.")
        val argumente = methode.mathematischeSignatur.argumente
        check(argumente.isNotEmpty()) {
            "Eine nullstellige Methode besitzt keinen Komponenten-Wertegrundraum."
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
        val methode = methodenWert.objekt as? SignaturtragendeMethode
            ?: error("Die neutrale Methodensignatur ist noch unbekannt.")
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "anzahl" to BedingterWert(
                    objekt = RationaleZahl.von(methode.signatur.argumente.size.toLong()),
                    variablenQuellen = methodenWert.variablenQuellen,
                ),
            ),
        )
    }
}

/**
 * Gibt konkrete symbolische Argumentobjekte aus und ist deshalb eine mathematische
 * Introspektion. Die UI-Anschlussprojektion selbst arbeitet dagegen nur mit der
 * neutralen [MethodenSignatur].
 */
internal object MethodenArgumenteAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? MathematischeSignaturtragendeMethode
            ?: error("Konkrete Methodenargumente benötigen eine mathematische Raum-/Mengensignatur.")
        val argumente = methode.mathematischeSignatur.argumente
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
                methode.signatur.argumente.forEachIndexed { index, argument ->
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
                    objekt = Tupel(werte.map { it.objekt as MathematischesObjekt }),
                    variablenQuellen = werte.flatMap(BedingterWert::variablenQuellen).distinct(),
                ),
            )
        }
        return KnotenAuswertungsErgebnis(ausgaben = ausgaben)
    }
}

/**
 * Liefert die mathematische Zielmenge bzw. den kanonischen Zielraum derselben
 * Ergebnisprojektion, die auch ein nachfolgender Methodenaufruf verwendet.
 */
internal object MethodenZielmengeSignaturAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val methodenWert = kontext.eingänge["methode"] ?: error("Eine konkrete Methode fehlt.")
        val methode = methodenWert.objekt as? MathematischeSignaturtragendeMethode
            ?: error("Die Zielmenge benötigt eine mathematische Raum-/Mengensignatur.")
        val signatur = methode.mathematischeSignatur
        val projektion = kontext.knoten.parameter[METHODEN_ZIELMENGE_ERGEBNISPROJEKTION]
            ?: METHODEN_ERGEBNISPROJEKTION_DIREKT
        val zielMenge = when {
            projektion == METHODEN_ERGEBNISPROJEKTION_TUPEL -> signatur.zielRaum
            signatur.ergebnisse.size == 1 -> signatur.ergebnisse.single().zielMenge
            else -> signatur.zielRaum
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
