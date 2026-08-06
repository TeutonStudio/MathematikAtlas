package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IterationsFormelCodecTest {
    private val x = Variable("x")
    private val f = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = x,
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to ReelleZahlen),
    )

    @Test
    fun `drei Iterationsarten behalten getrennte Operator IDs im Formel DAG`() {
        val operatorIds = IterationsArt.entries.map { art ->
            val basis: MathematischesObjekt = if (art == IterationsArt.MULTIPLIKATION) x else f
            val ausdruck = IterierterAusdruck(basis, art, IterationsOrdnung.Konkret(2))
            val formel = IterationsFormelCodec.zuFormel(ausdruck)
            val graph = FormelGraph.ausFormel(formel)
            val rueckweg = IterationsFormelCodec.ausFormel(
                assertIs<FormelAusdruck.Operation>(graph.zuFormel()),
            )

            assertEquals(ausdruck.art, rueckweg.art)
            assertEquals(ausdruck.ordnung, rueckweg.ordnung)
            formel.operatorId
        }

        assertEquals(3, operatorIds.distinct().size)
        assertEquals(IterationsArt.entries.map { it.operatorId }, operatorIds)
    }

    @Test
    fun `symbolische Ordnung traegt Natuerlichkeitsannahme im Formelobjekt`() {
        val annahme = UnentscheidbareAussage("n\\in\\mathbb N_0", "Iteration")
        val ausdruck = IterierterAusdruck(
            f,
            IterationsArt.DIFFERENTIATION,
            IterationsOrdnung.Symbolisch(Variable("n"), setOf(annahme)),
        )

        val formel = IterationsFormelCodec.zuFormel(ausdruck)
        val rueckweg = IterationsFormelCodec.ausFormel(formel)
        val ordnung = assertIs<IterationsOrdnung.Symbolisch>(rueckweg.ordnung)

        assertEquals(setOf(annahme), ordnung.annahmen)
        assertEquals("n", ordnung.ausdruck.zuLatex())
    }

    @Test
    fun `Rendererprojektion aendert konkrete Ordnung im AST nicht`() {
        val ausdruck = IterierterAusdruck(
            f,
            IterationsArt.DIFFERENTIATION,
            IterationsOrdnung.Konkret(4),
        )
        val formel = IterationsFormelCodec.zuFormel(ausdruck)
        val ordnungsArgument = formel.argumente.single { it.rollenId == "ordnung" }
        val zahl = assertIs<RationaleZahl>(assertIs<FormelAusdruck.Literal>(ordnungsArgument.ausdruck).wert)

        assertEquals(RationaleZahl.von(4), zahl)
        assertTrue(ausdruck.zuLatex().contains("mathrm{IV}"))
        assertNotEquals(ausdruck.zuLatex(), zahl.zuLatex())
    }

    @Test
    fun `freie Variable darf nicht als Methodenbasis rekonstruiert werden`() {
        val formel = FormelAusdruck.Operation(
            id = "ungueltig",
            operatorId = IterationsArt.DIFFERENTIATION.operatorId,
            argumente = listOf(
                FormelArgument("basis", 0, FormelAusdruck.Variable("basis", "f", "f", FormelTyp.OBJEKT)),
                FormelArgument(
                    "ordnung",
                    1,
                    FormelAusdruck.Literal("ordnung", RationaleZahl.Eins, FormelTyp.ZAHL),
                ),
            ),
            typ = FormelTyp.METHODE,
        )

        assertFailsWith<IllegalArgumentException> {
            IterationsFormelCodec.ausFormel(formel)
        }
    }
}
