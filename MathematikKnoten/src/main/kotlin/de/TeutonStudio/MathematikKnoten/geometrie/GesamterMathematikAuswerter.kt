package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister

/** Bestehender v2.1.18-Auswerter plus additive Geometriedomäne. */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereGeometrieGrundobjekte()
        registriereGeometrieRelationen()
        registriereGeometrieTransformationen()
    }
}
