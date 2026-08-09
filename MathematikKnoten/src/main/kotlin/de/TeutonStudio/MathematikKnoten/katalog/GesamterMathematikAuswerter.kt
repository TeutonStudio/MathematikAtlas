package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikKnoten.katalog.StandardMathematikAuswerterPakete

/**
 * Baut das vollständige Mathematikregister aus dem historischen Basisauswerter
 * und den explizit geordneten Registrierungsphasen auf.
 */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister =
        StandardMathematikAuswerter.erzeugeRegister().also(
            StandardMathematikAuswerterPakete::installiereIn,
        )
}
