package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister

/** Bestehender Standardauswerter plus additive Geometrie- und Mengenraumdomänen. */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereGeometrieGrundobjekte()
        registriereGeometrieRelationen()
        registriereGeometrieTransformationen()
        registriereMengenraumKnoten()
    }
}
