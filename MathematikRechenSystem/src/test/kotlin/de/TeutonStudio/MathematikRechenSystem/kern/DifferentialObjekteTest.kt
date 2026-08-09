package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DifferentialObjekteTest {
    @Test
    fun `partielle Ableitung verwendet nur den Raum des gewaehlten Arguments`() {
        val x = Variable("x")
        val z = Variable("z")
        val f = Methode(
            name = "f",
            parameter = listOf(x, z),
            vorschrift = addition(x, z),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(
                "x" to ReelleZahlen,
                "z" to KomplexeZahlen,
            ),
        )

        val ergebnis = differenziereMethodeMitOperatorZielraum(
            methode = f,
            ordnung = DifferentialOrdnung.Konkret(1),
            operator = DifferentialOperator.Partiell(2),
            begriff = DifferentialBegriff.REELL_FRECHET,
        )
        val zielRaum = assertIs<AbleitungsZielraum>(ergebnis.zielRaum)

        assertEquals("\\partial_{2}f", ergebnis.methode.name)
        assertEquals(KomplexeZahlen, zielRaum.argumentRaum)
        assertEquals(ReelleZahlen, zielRaum.ursprungsZiel)
    }

    @Test
    fun `partielles Differential bleibt von partieller Ableitungsfunktion verschieden`() {
        val x = Variable("x")
        val y = Variable("y")
        val f = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = multiplikation(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val differential = MethodenDifferential(
            methode = f,
            operator = DifferentialOperator.Partiell(2),
        )

        assertEquals("d_{2}f", differential.zuLatex())
        assertTrue(differential.definitionsLatex().contains("\\iota_{2}"))
        assertEquals("\\partial_{2}f", differential.ableitungsFunktion.name)
    }
}
