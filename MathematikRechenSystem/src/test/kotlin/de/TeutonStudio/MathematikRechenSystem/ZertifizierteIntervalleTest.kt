package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ZertifizierteIntervalleTest {
    private val x = Variable("x")
    private fun z(n: Long, d: Long = 1) = RationaleZahl.von(n, d)
    private val umgebung = mapOf("x" to RationalesIntervall(z(-2), z(3)))

    @Test fun `gerade Potenz schliesst Null ein und negative Potenz beachtet Polstelle`() {
        val wert = assertIs<IntervallNachweis.Schranke>(ZertifizierterIntervallAuswerter.wert(Potenz(x, z(2)), umgebung))
        assertEquals(RationalesIntervall(z(0), z(9)), wert.intervall)
        assertIs<IntervallNachweis.Offen>(ZertifizierterIntervallAuswerter.wert(Potenz(x, z(-1)), umgebung))
        assertIs<IntervallNachweis.Offen>(ZertifizierterIntervallAuswerter.wert(Potenz(x, z(0)), umgebung))
    }
    @Test fun `Division ueber Polstelle liefert keinen falschen Nullstellennachweis`() {
        val bedingung = Gleichheit(Division(z(1), x), z(0))
        assertNull(ZertifizierterIntervallAuswerter.aussage(bedingung, umgebung).wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ZertifizierterIntervallAuswerter.aussage(bedingung, mapOf("x" to RationalesIntervall(z(1), z(2)))).wahrheitswert)
    }
    @Test fun `kleine positive Divisoren werden nicht auf null gerundet`() {
        val wert = assertIs<IntervallNachweis.Schranke>(ZertifizierterIntervallAuswerter.wert(Division(z(1), z(1, 1000000000000)), emptyMap()))
        assertEquals(z(1000000000000), wert.intervall.minimum)
    }
    @Test fun `Konjunktion zweier Gleichungen zertifiziert keine ganze Achse`() {
        val aussage = Konjunktion(listOf(Gleichheit(x, z(0)), Gleichheit(Variable("y"), z(0))))
        assertNull(ZertifizierterIntervallAuswerter.aussage(aussage, umgebung + ("y" to RationalesIntervall(z(-1), z(1)))).wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ZertifizierterIntervallAuswerter.aussage(aussage, umgebung + ("y" to RationalesIntervall(z(1), z(2)))).wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ZertifizierterIntervallAuswerter.aussage(aussage, mapOf("x" to RationalesIntervall(z(0)), "y" to RationalesIntervall(z(0)))).wahrheitswert)
    }
    @Test fun `rationale Stichproben liegen in zertifizierter Polynomhulle`() {
        val term = addition(Potenz(x, z(2)), multiplikation(z(-3), x), z(1))
        val h = assertIs<IntervallNachweis.Schranke>(ZertifizierterIntervallAuswerter.wert(term, umgebung)).intervall
        for (n in -20L..30L) {
            val t = z(n, 10)
            val erwartung = t * t - z(3) * t + z(1)
            assertTrue(erwartung >= h.minimum && erwartung <= h.maximum)
        }
    }
    @Test fun `undefinierte Fallbedingung erzeugt auch bei gleichem Ausdruck keinen Beweis`() {
        val fall = ZahlFallAusdruck(z(1), Vergleich(Division(z(1), z(0)), VergleichsArt.Kleiner, z(0)), z(2))
        assertIs<IntervallNachweis.Offen>(ZertifizierterIntervallAuswerter.wert(fall, emptyMap()))
        assertNull(ZertifizierterIntervallAuswerter.aussage(Gleichheit(fall, fall), emptyMap()).wahrheitswert)
    }
    @Test fun `Budget und fehlende Bindung werden nicht als falsch klassifiziert`() {
        val a = Vergleich(x, VergleichsArt.Kleiner, z(4))
        assertNull(ZertifizierterIntervallAuswerter.aussage(a, emptyMap()).wahrheitswert)
        assertNull(ZertifizierterIntervallAuswerter.aussage(a, umgebung, AuswertungsBudget(1)).wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ZertifizierterIntervallAuswerter.aussage(a, umgebung).wahrheitswert)
    }
}
