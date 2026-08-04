package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SkalarproduktFalkAblaufTest {
    @Test
    fun `rechtslineares quaternionisches Schema bewahrt konjugiert links die Faktorenreihenfolge`() {
        val ablauf = SkalarproduktFalkAblauf(
            linkeKomponenten = listOf("q", "s"),
            rechteKomponenten = listOf("r", "t"),
            linearitaet = SkalarproduktLinearitaet.RECHTSLINEAR,
            konjugiert = true,
        )

        assertEquals("\\overline{q}\\,r", ablauf.produktLatex(0))
        assertEquals("\\overline{q}\\,r+\\overline{s}\\,t", ablauf.vollständigeSummeLatex())
    }

    @Test
    fun `linkslineares quaternionisches Schema bewahrt konjugiert rechts die Faktorenreihenfolge`() {
        val ablauf = SkalarproduktFalkAblauf(
            linkeKomponenten = listOf("q", "s"),
            rechteKomponenten = listOf("r", "t"),
            linearitaet = SkalarproduktLinearitaet.LINKSLINEAR,
            konjugiert = true,
        )

        assertEquals("q\\,\\overline{r}", ablauf.produktLatex(0))
        assertEquals("q\\,\\overline{r}+s\\,\\overline{t}", ablauf.vollständigeSummeLatex())
    }

    @Test
    fun `Standardauswertung ordnet nichtkommutative symbolische Faktoren entsprechend der Konvention`() {
        val q = Variable("q")
        val r = Variable("r")
        val werteVorräte = mapOf(
            q.name to FundamentalerZahlbereich.QUATERNION.alsMenge(),
            r.name to FundamentalerZahlbereich.QUATERNION.alsMenge(),
        )

        val rechtslinear = assertIs<StrukturPruefung.Gueltig<ZahlAusdruck>>(
            standardSkalarprodukt(
                SpaltenVektor(listOf(q)),
                SpaltenVektor(listOf(r)),
                SkalarproduktSpezifikation(
                    linearitaet = SkalarproduktLinearitaet.RECHTSLINEAR,
                    konjugiert = true,
                ),
                werteVorräte,
            ),
        ).wert.zuLatex()
        val linkslinear = assertIs<StrukturPruefung.Gueltig<ZahlAusdruck>>(
            standardSkalarprodukt(
                SpaltenVektor(listOf(q)),
                SpaltenVektor(listOf(r)),
                SkalarproduktSpezifikation(
                    linearitaet = SkalarproduktLinearitaet.LINKSLINEAR,
                    konjugiert = true,
                ),
                werteVorräte,
            ),
        ).wert.zuLatex()

        assertTrue(rechtslinear.indexOf("\\overline{q}") < rechtslinear.indexOf("r"))
        assertTrue(linkslinear.indexOf("q") < linkslinear.indexOf("\\overline{r}"))
    }

    @Test
    fun `Matrixprodukt bleibt von der Skalarproduktkonvention unberührt`() {
        val links = Matrix(
            listOf(
                listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
                listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
            ),
        )
        val rechts = Matrix(
            listOf(
                listOf(RationaleZahl.von(5), RationaleZahl.von(6)),
                listOf(RationaleZahl.von(7), RationaleZahl.von(8)),
            ),
        )

        val ergebnis = assertIs<MatrixRechnerErgebnis.MatrixWert>(
            MatrixRechner.erzeuge(
                MatrixRechnerAnfrage(
                    operator = MatrixRechnerOperator.MATRIXPRODUKT,
                    matrizen = listOf(
                        MatrixOperand("links", links, FundamentalerZahlbereich.RATIONAL),
                        MatrixOperand("rechts", rechts, FundamentalerZahlbereich.RATIONAL),
                    ),
                ),
            ),
        )

        assertEquals(
            Matrix(
                listOf(
                    listOf(RationaleZahl.von(19), RationaleZahl.von(22)),
                    listOf(RationaleZahl.von(43), RationaleZahl.von(50)),
                ),
            ),
            ergebnis.wert,
        )
    }
}
