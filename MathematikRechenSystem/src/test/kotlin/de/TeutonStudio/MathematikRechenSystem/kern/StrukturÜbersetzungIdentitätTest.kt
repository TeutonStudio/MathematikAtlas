package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class StrukturÜbersetzungIdentitätTest {
    private fun z(n: Long) = RationaleZahl.von(n)

    @Test fun `bereits passende Zielstruktur bleibt gleich`() {
        val tupel = Tupel(listOf(z(1), Tupel(listOf(z(2), z(3)))))
        assertEquals(tupel, übersetzeStruktur(tupel, "tupel"))
        val zeile = ZeilenVektor(listOf(z(1), z(2)))
        val spalte = SpaltenVektor(listOf(z(1), z(2)))
        assertEquals(zeile, übersetzeStruktur(zeile, "zeile"))
        assertEquals(spalte, übersetzeStruktur(spalte, "spalte"))
    }

    @Test fun `orientierungswechsel laesst die Werte unveraendert`() {
        val zeile = ZeilenVektor(listOf(z(4), z(7)))
        val spalte = übersetzeStruktur(zeile, "spalte") as SpaltenVektor
        assertEquals(zeile.werte, spalte.werte)
        assertEquals(zeile, übersetzeStruktur(spalte, "zeile"))
    }

    @Test fun `verschachtelte Tupel werden nur explizit abgeflacht`() {
        val tupel = Tupel(listOf(z(1), Tupel(listOf(z(2)))))
        assertEquals(tupel, übersetzeStruktur(tupel, "tupel"))
        assertEquals(Tupel(listOf(z(1), z(2))), tupelAbflachen(tupel))
        assertFails { übersetzeStruktur(tupel, "spalte") }
    }
}
