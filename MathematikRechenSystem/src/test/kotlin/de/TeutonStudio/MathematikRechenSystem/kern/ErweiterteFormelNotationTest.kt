package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ErweiterteFormelNotationTest {
    private fun importiereOperation(latex: String): FormelAusdruck.Operation {
        val ergebnis = assertIs<FormelLatexImportErgebnis.Erfolg>(FormelLatexCodec.importiere(latex))
        return assertIs<FormelAusdruck.Operation>(ergebnis.ausdruck)
    }

    @Test
    fun `gewöhnliche Potenz wird als Multiplikationsiteration importiert`() {
        val operation = importiereOperation("a^{n}")

        assertEquals("iteration.multiplikation", operation.operatorId)
        assertEquals(listOf("basis", "ordnung"), operation.argumente.sortedBy { it.position }.map { it.rollenId })
        assertEquals("a^{n}", FormelLatexCodec.exportiere(operation))
    }

    @Test
    fun `symbolische Differentiationsordnung behaelt runde Klammern`() {
        val operation = importiereOperation("f^{(n)}")

        assertEquals("iteration.differentiation", operation.operatorId)
        assertEquals("f^{(n)}", FormelLatexCodec.exportiere(operation))
        assertEquals(FormelPruefung.Gueltig, FormelAusdruckPruefer.pruefe(operation))
    }

    @Test
    fun `roemische Differentiationsordnung wird numerisch gespeichert und roemisch exportiert`() {
        val operation = importiereOperation("f^{\\mathrm{IV}}")
        val ordnung = operation.argumente.sortedBy { it.position }[1].ausdruck
        val literal = assertIs<FormelAusdruck.Literal>(ordnung)

        assertEquals("4", literal.wert.zuLatex())
        assertEquals("f^{\\mathrm{IV}}", FormelLatexCodec.exportiere(operation))
    }

    @Test
    fun `Selbstkomposition verwendet Winkelklammern`() {
        val operation = importiereOperation("f^{\\langle n\\rangle}")

        assertEquals("iteration.selbstkomposition", operation.operatorId)
        assertEquals("f^{\\langle n\\rangle}", FormelLatexCodec.exportiere(operation))
    }

    @Test
    fun `Identitaet und Restriktion bleiben getrennte Semantik`() {
        val operation = importiereOperation("\\operatorname{id}\\vert_{W}")
        val basis = operation.argumente.sortedBy { it.position }.first().ausdruck

        assertEquals("methode.einschraenkung", operation.operatorId)
        assertEquals(FormelTyp.METHODE, basis.typ)
        assertEquals("\\operatorname{id}\\vert_{W}", FormelLatexCodec.exportiere(operation))
    }

    @Test
    fun `linke und rechte Division roundtrippen getrennt`() {
        val rechts = importiereOperation("a\\div_R b")
        val links = importiereOperation("a\\div_L b")

        assertEquals("algebra.division.rechts", rechts.operatorId)
        assertEquals("algebra.division.links", links.operatorId)
        assertEquals("a \\div_R b", FormelLatexCodec.exportiere(rechts))
        assertEquals("a \\div_L b", FormelLatexCodec.exportiere(links))
    }

    @Test
    fun `Bruch bleibt kommutativer Quotient`() {
        val bruch = importiereOperation("\\frac{a}{b}")

        assertEquals("zahl.division", bruch.operatorId)
        assertEquals("\\frac{a}{b}", FormelLatexCodec.exportiere(bruch))
    }

    @Test
    fun `plus minus und minus plus bleiben geordnet und auch verschachtelt strukturiert`() {
        val plusMinus = importiereOperation("\\pm\\,x")
        val summe = importiereOperation("a+\\mp\\,b")
        val rechterSummand = assertIs<FormelAusdruck.Operation>(
            summe.argumente.sortedBy { it.position }[1].ausdruck,
        )

        assertEquals("algebra.vorzeichen.plusMinus", plusMinus.operatorId)
        assertEquals("\\pm\\,x", FormelLatexCodec.exportiere(plusMinus))
        assertEquals("algebra.vorzeichen.minusPlus", rechterSummand.operatorId)
        assertEquals("a + \\mp\\,b", FormelLatexCodec.exportiere(summe))
    }

    @Test
    fun `Teilparser vergibt global eindeutige Ausdrucks IDs`() {
        val operation = importiereOperation("f^{(n+1)}")
        val ids = mutableListOf<String>()

        fun sammle(ausdruck: FormelAusdruck) {
            ids += ausdruck.id
            if (ausdruck is FormelAusdruck.Operation) {
                ausdruck.argumente.forEach { sammle(it.ausdruck) }
            }
        }
        sammle(operation)

        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.size >= 5)
    }
}
