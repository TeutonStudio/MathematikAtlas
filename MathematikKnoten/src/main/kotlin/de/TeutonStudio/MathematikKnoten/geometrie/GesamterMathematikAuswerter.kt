package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister

/** Bestehender Standardauswerter plus additive Mathematikdomänen. */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereUniversellenZahlenRechner()
        registriereZahlenRechnerErweiterungen()
        registriereMatrixdiagonale()
        registriereSpurUndTupelsumme()
        registriereTransponieren()
        registriereGeometrieGrundobjekte()
        registriereGeometrieTeilobjekte()
        registriereGeometrieRelationen()
        registriereGeometrieTransformationen()
        registriereMengenraumKnoten()
        registriereAussagenLogikKnoten()
        registriereStrukturRechnerKnoten()
        registriereLineareStrukturErweiterungen()
        registriereLineareAlgebraGrundlagen()
    }
}
