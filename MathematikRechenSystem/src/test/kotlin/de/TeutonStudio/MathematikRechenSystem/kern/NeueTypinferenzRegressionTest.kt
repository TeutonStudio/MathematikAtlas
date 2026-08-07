package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeueTypinferenzRegressionTest {
    @Test
    fun `Inverses eines nachweislich reellen Nichtnullwerts bleibt reell`() {
        val x = Variable("x")
        val annahmen = setOf<Aussage>(Ungleichheit(x, RationaleZahl.Null))

        assertTrue(
            istNachweisbarReell(
                InversesElement(x),
                variableIstReell = { it.name == "x" },
                annahmen = annahmen,
            ),
        )
        assertEquals(
            ReelleZahlen,
            inferiereZahlenWertevorrat(
                InversesElement(x),
                werteVorräte = mapOf("x" to ReelleZahlen),
                annahmen = annahmen,
            ),
        )
    }

    @Test
    fun `Hyperreeller Wert wird nicht in Standardzahlbereiche einsortiert`() {
        val h = SymbolischerHyperReellerWert("h")

        assertFalse(istNachweisbarReell(h))
        assertFailsWith<IllegalStateException> {
            inferiereZahlenWertevorrat(h)
        }
    }

    @Test
    fun `Differenzierbarkeitsbereich erbt Elementmenge des Ursprungsbereichs`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val bereich = DifferenzierbarkeitsBereich(
            methode = methode,
            ursprungsBereich = ReelleZahlen,
            ordnung = DifferentialOrdnung.Konkret(1),
            operator = DifferentialOperator.Total,
        )

        assertEquals(ReelleZahlen, inferiereZielmenge(bereich))
    }

    @Test
    fun `Ableitungszielraum bleibt als strukturierter Raum erhalten`() {
        val zielraum = AbleitungsZielraum(
            argumentRaum = ReelleZahlen,
            ursprungsZiel = ReelleZahlen,
            ordnung = DifferentialOrdnung.Konkret(1),
            eindimensionalSkalarIdentifiziert = false,
        )

        assertEquals(zielraum, inferiereZielmenge(zielraum))
    }
}
