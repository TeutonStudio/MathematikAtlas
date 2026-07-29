package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister

/** Standardauswerter plus additive Geometrie- und Versionsknoten. */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereDivisionUndKehrwert()
        registriereSubtraktion()
        registriereOptimierteKonjunktion()
        registriereReelleMethodenSumme()
        registriereDreieckRechner()
        registriereGeometrieGrundobjekte()
        registriereGeometrieRelationen()
        registriereGeometrieTransformationen()
    }
}
