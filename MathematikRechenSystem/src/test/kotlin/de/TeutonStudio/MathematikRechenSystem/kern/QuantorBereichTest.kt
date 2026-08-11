package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuantorBereichTest {
    @Test
    fun `bestehender Mengenquantor bleibt quellkompatibel und endlich auswertbar`() {
        val x = LogischeVariable(
            "x",
            "x",
            EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins)),
        )
        val aussage = QuantifizierteAussage(
            AussagenSatzOperator.ALLQUANTOR,
            x,
            Gleichheit(Variable("x"), Variable("x")),
        )

        assertEquals(Wahrheitswert.Wahr, aussage.entscheide().wahrheitswert)
        assertTrue(aussage.zuLatex().contains("x\\in"))
    }

    @Test
    fun `Typquantor ueber Mengen konstruiert keine Universalmenge`() {
        val x = LogischeVariable(
            id = "menge-x",
            name = "x",
            quantorBereich = QuantorBereich.Typ(LogischerTyp.MENGE),
        )
        val aussage = QuantifizierteAussage(
            AussagenSatzOperator.ALLQUANTOR,
            x,
            WahrheitsKonstante(true),
        )
        val ergebnis = aussage.entscheide()

        assertNull(x.bereich)
        assertNull(ergebnis.wahrheitswert)
        assertEquals(EntscheidungsStatus.Unbekannt, ergebnis.status)
        assertTrue(ergebnis.begründung.contains("keine Universalmenge"))
        assertEquals("\\forall x:\\mathsf{Menge}:\\;\\mathcal{Wahr}", aussage.zuLatex())
    }
}
