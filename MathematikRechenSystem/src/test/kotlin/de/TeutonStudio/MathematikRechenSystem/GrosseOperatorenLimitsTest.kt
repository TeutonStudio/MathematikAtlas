package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrosseOperatorenLimitsTest {
    private val i = Variable("i")
    private val indexMenge = BenannteMenge("I")
    private val wahrheitsMenge = EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))
    private val zahlMethode = Funktion(
        "f", listOf(i), mapOf("wert" to i), mapOf("wert" to ReelleZahlen), mapOf(i.name to ReelleZahlen),
    )
    private val aussageMethode = Funktion(
        "P", listOf(i), mapOf("wert" to AussagenParameter("P_i")),
        mapOf("wert" to wahrheitsMenge), mapOf(i.name to ReelleZahlen),
    )
    private val mengenMethode = Funktion(
        "A", listOf(i), mapOf("wert" to MengenParameter("A_i")),
        mapOf("wert" to BenannteMenge("G")), mapOf(i.name to ReelleZahlen),
    )

    @Test
    fun `alle grossen Operatoren verwenden limits ohne Einermenge`() {
        val formeln = listOf(
            IterierteSumme(zahlMethode, indexMenge).zuLatex(),
            IteriertesProdukt(zahlMethode, indexMenge).zuLatex(),
            IterierteKonjunktion(aussageMethode, indexMenge).zuLatex(),
            IterierteDisjunktion(aussageMethode, indexMenge).zuLatex(),
            IterierteAdjunktion(aussageMethode, indexMenge).zuLatex(),
            IterierteVereinigung(mengenMethode, indexMenge).zuLatex(),
            IterierterSchnitt(mengenMethode, indexMenge).zuLatex(),
            IteriertesKartesischesProdukt(mengenMethode, indexMenge).zuLatex(),
        )

        formeln.forEach { formel ->
            assertTrue("\\limits_{i \\in I}" in formel, formel)
            assertFalse("\\Set{I}" in formel, formel)
        }
    }
}
