package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PotenzZielmengenTest {
    private val struktur = StandardPotenzStrukturen.zahlbereich(FundamentalerZahlbereich.REELL)

    @Test
    fun `algebraische Potenz bleibt im Traeger ihrer Struktur`() {
        val potenz = AlgebraischePotenz(
            basis = RationaleZahl.von(2),
            ordnung = IterationsOrdnung.Konkret(3),
            struktur = struktur,
        )

        assertEquals(ReelleZahlen, inferiereZielmenge(potenz))
    }

    @Test
    fun `Strukturzeugnis ist kein gewoehnlicher Wert mit erfundener Zielmenge`() {
        assertFailsWith<IllegalStateException> {
            inferiereZielmenge(struktur)
        }
    }

    @Test
    fun `nicht angewandte punktweise Methodenpotenz verlangt Methodenkontext`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val potenz = PunktweiseMethodenPotenz(
            methode = methode,
            ordnung = IterationsOrdnung.Konkret(2),
            struktur = struktur,
        )

        assertFailsWith<IllegalStateException> {
            inferiereZielmenge(potenz)
        }
    }
}
