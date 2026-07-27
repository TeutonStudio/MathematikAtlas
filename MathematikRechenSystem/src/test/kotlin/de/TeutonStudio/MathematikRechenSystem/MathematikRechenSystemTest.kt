package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MathematikRechenSystemTest {
    @Test fun rationaleZahlenSindExakt() {
        assertEquals(RationaleZahl.von(5, 6), RationaleZahl.von(1, 2) + RationaleZahl.von(1, 3))
    }

    @Test fun additionWirdAbgeflacht() {
        val x = Variable("x")
        val term = addition(addition(x, RationaleZahl.von(2)), RationaleZahl.von(3))
        assertEquals(addition(x, RationaleZahl.von(5)), term)
        assertTrue(term is Addition)
        assertEquals(2, term.summanden.size)
    }

    @Test fun funktionKannTeilweiseGebundenWerden() {
        val x = Variable("x"); val y = Variable("y")
        val f = Funktion("f", listOf(x, y), mapOf("wert" to addition(x, y)))
        val g = f.binde(mapOf("x" to RationaleZahl.von(4)))
        assertEquals(listOf(y), g.freieParameter)
        assertEquals(RationaleZahl.von(7), g.binde(mapOf("y" to RationaleZahl.von(3))).auswerten().getValue("wert"))
    }

    @Test fun matrixWirdExaktInvertiert() {
        val m = Matrix(listOf(listOf(RationaleZahl.von(2), RationaleZahl.von(0)), listOf(RationaleZahl.von(0), RationaleZahl.von(4))))
        val inv = m.inverseRational()
        assertEquals(RationaleZahl.von(1, 2), inv.zeilen[0][0])
        assertEquals(RationaleZahl.von(1, 4), inv.zeilen[1][1])
    }

    @Test fun lineareGleichungWirdGelöst() {
        val x = Variable("x")
        val gleichung = Gleichheit(addition(multiplikation(RationaleZahl.von(2), x), RationaleZahl.von(4)), RationaleZahl.von(10))
        assertEquals(listOf(RationaleZahl.von(3)), löseLinear(gleichung, x).lösungen)
    }

    @Test fun unentscheidbareAussageBleibtOhneWahrheitswert() {
        val e = UnentscheidbareAussage("G", "Axiomensystem S").entscheide()
        assertNull(e.wahrheitswert)
        assertIs<EntscheidungsStatus.Unentscheidbar>(e.status)
    }
}
