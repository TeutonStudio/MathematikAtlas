package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PotenzRechnerAdapterTest {
    @Test
    fun `Zahlenadapter uebernimmt konkrete natuerliche Potenz`() {
        val ergebnis = assertIs<ZahlenRechnerErgebnis.Wert>(
            ZahlenPotenzAdapter.versuche(
                basis = ZahlenRechnerEingabe(
                    "basis",
                    RationaleZahl.von(2),
                    FundamentalerZahlbereich.NATUERLICH_POSITIV,
                ),
                exponent = ZahlenRechnerEingabe(
                    "exponent",
                    RationaleZahl.von(20),
                    FundamentalerZahlbereich.NATUERLICH_POSITIV,
                ),
            ),
        )

        assertEquals(RationaleZahl.von(1_048_576), ergebnis.ausdruck)
        assertEquals(FundamentalerZahlbereich.NATUERLICH_POSITIV, ergebnis.bereich)
        assertEquals("potenz|N", ergebnis.definitionsId)
    }

    @Test
    fun `Zahlenadapter laesst negative und rationale Exponenten beim Altvertrag`() {
        val basis = ZahlenRechnerEingabe(
            "basis",
            RationaleZahl.von(2),
            FundamentalerZahlbereich.NATUERLICH_POSITIV,
        )

        assertNull(
            ZahlenPotenzAdapter.versuche(
                basis,
                ZahlenRechnerEingabe("exponent", RationaleZahl.von(-1), FundamentalerZahlbereich.GANZ),
            ),
        )
        assertNull(
            ZahlenPotenzAdapter.versuche(
                basis,
                ZahlenRechnerEingabe("exponent", RationaleZahl.von(1, 2), FundamentalerZahlbereich.RATIONAL),
            ),
        )
    }

    @Test
    fun `Matrixadapter liefert typisierte Einheitsmatrix fuer Ordnung null`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(2), RationaleZahl.Null),
                listOf(RationaleZahl.Null, RationaleZahl.von(3)),
            ),
        )
        val ergebnis = assertIs<MatrixRechnerErgebnis.MatrixWert>(
            MatrixPotenzAdapter.werteNatuerlichAus(
                MatrixOperand("basis", matrix, FundamentalerZahlbereich.NATUERLICH_MIT_NULL),
                BigInteger.ZERO,
            ),
        )

        assertEquals(einheitsMatrixFuerPotenz(2), ergebnis.wert)
        assertEquals(FundamentalerZahlbereich.NATUERLICH_MIT_NULL, ergebnis.zahlbereich)
    }

    @Test
    fun `Matrixadapter lehnt rechteckige zweite Potenz ab`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3)),
                listOf(RationaleZahl.von(4), RationaleZahl.von(5), RationaleZahl.von(6)),
            ),
        )
        val ergebnis = assertIs<MatrixRechnerErgebnis.Ungueltig>(
            MatrixPotenzAdapter.werteNatuerlichAus(
                MatrixOperand("basis", matrix, FundamentalerZahlbereich.NATUERLICH_MIT_NULL),
                BigInteger.valueOf(2),
            ),
        )

        assertEquals("abgeschlossenheit_widerlegt", ergebnis.code)
    }
}
