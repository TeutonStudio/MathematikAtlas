package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val VARIABLE_BEREICH_MODUS = "variable.bereichModus"
const val VARIABLE_BEREICH_GRUNDMENGE = "grundmenge"
const val VARIABLE_BEREICH_INTERVALL = "intervall"
const val VARIABLE_INTERVALL_LINKS = "variable.intervallLinks"
const val VARIABLE_INTERVALL_RECHTS = "variable.intervallRechts"
const val VARIABLE_INTERVALL_LINKS_OFFEN = "variable.intervallLinksOffen"
const val VARIABLE_INTERVALL_RECHTS_OFFEN = "variable.intervallRechtsOffen"

/** Intervallgrenzen sind reelle Grad-Repräsentanten, keine Elemente der Quotienten-Winkelmenge. */
fun variablenWertevorrat(knoten: KnotenDaten, normaleMenge: MengenAusdruck): MengenAusdruck {
    if (knoten.parameter[VARIABLE_BEREICH_MODUS] != VARIABLE_BEREICH_INTERVALL) return normaleMenge
    require(knoten.parameter["werteVorrat"].orEmpty().ifBlank { "R" } in setOf("R", "ℝ")) {
        "Ein reelles Intervall erfordert die Grundmenge R."
    }
    val links = RationaleZahl.parse(knoten.parameter[VARIABLE_INTERVALL_LINKS] ?: "0")
    val rechts = RationaleZahl.parse(knoten.parameter[VARIABLE_INTERVALL_RECHTS] ?: "360")
    require(links < rechts) { "Die rechte Intervallgrenze muss größer als die linke sein." }
    return reellesIntervall(
        links, knoten.parameter[VARIABLE_INTERVALL_LINKS_OFFEN] == "true",
        rechts, knoten.parameter[VARIABLE_INTERVALL_RECHTS_OFFEN] != "false",
    )
}
