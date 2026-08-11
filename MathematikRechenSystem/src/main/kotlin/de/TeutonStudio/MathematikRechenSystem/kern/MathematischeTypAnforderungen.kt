package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAnforderung

/**
 * Kanonische IDs für strukturabhängige Typanforderungen.
 *
 * G0.2 definiert nur die Sprache. G0.3 entscheidet anhand konkreter Träger und
 * Strukturknoten, ob die Anforderungen tatsächlich erfüllt sind.
 */
object MathematischeTypAnforderungen {
    fun topologie() = TypAnforderung("struktur.topologie")
    fun metrik() = TypAnforderung("struktur.metrik")
    fun norm() = TypAnforderung("struktur.norm")
    fun skalarProdukt() = TypAnforderung("struktur.skalarprodukt")
    fun mass() = TypAnforderung("struktur.mass")

    fun gruppe() = TypAnforderung("axiom.gruppe")
    fun kommutativeGruppe() = TypAnforderung("axiom.gruppe.kommutativ")
    fun ring() = TypAnforderung("axiom.ring")
    fun koerper() = TypAnforderung("axiom.koerper")
    fun schiefKoerper() = TypAnforderung("axiom.schiefkoerper")
    fun vektorraum() = TypAnforderung("axiom.vektorraum")

    fun benannt(id: String, parameter: Map<String, String> = emptyMap()): TypAnforderung =
        TypAnforderung(id, parameter)
}
