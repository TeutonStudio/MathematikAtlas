package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister

/** Bestehender Standardauswerter plus additive Geometriedomäne und versionierte Knotenerweiterungen. */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereDivisionUndKehrwert()
        registriereGeometrieGrundobjekte()
        registriereGeometrieRelationen()
        registriereGeometrieTransformationen()
    }
}
