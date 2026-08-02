package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister

/** Bestehender Standardauswerter plus additive lineare, Geometrie-, Mengenraum- und Aussagenlogikdomänen. */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereMatrixdiagonale()
        registriereSpurUndTupelsumme()
        registriereTransponieren()
        registriereGeometrieGrundobjekte()
        registriereGeometrieTeilobjekte()
        registriereGeometrieRelationen()
        registriereGeometrieTransformationen()
        registriereMengenraumKnoten()
        registriereAussagenLogikKnoten()
    }
}
