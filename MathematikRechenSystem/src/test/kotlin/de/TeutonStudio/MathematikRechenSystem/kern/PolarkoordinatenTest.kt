package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.*

class PolarkoordinatenTest {
    private fun t(vararg x: Double) = Tupel(x.map(::rationaleKoordinate))
    private fun werte(t: Tupel) = t.elemente.flatMap { if (it is Tupel) it.elemente else listOf(it) }.map { zahl ->
        ((DomaenenAuswerter.wert(if (zahl is GradWinkel) zahl.grad else zahl as ZahlAusdruck) as DomaenenErgebnis.Wert).wert as DomaenenWert.Reell).wert.toDouble()
    }
    private fun nahe(erwartet: List<Double>, t: Tupel, toleranz: Double = 1e-12) {
        val ist = werte(t)
        assertEquals(erwartet.size, ist.size)
        erwartet.zip(ist).forEach { (e, i) -> assertTrue(abs(e - i) <= toleranz, "$e statt $i") }
    }

    @Test fun `Quadranten und Achsen verwenden Grad und kanonischen Azimut`() {
        listOf(0.0 to t(1.0, 0.0), 90.0 to t(0.0, 1.0), 180.0 to t(-1.0, 0.0),
            270.0 to t(0.0, -1.0), 45.0 to t(1.0, 1.0), 135.0 to t(-1.0, 1.0),
            225.0 to t(-1.0, -1.0), 315.0 to t(1.0, -1.0)).forEach { (winkel, x) ->
            assertEquals(winkel, werte(Polarkoordinaten.polar(x))[1], 1e-12)
        }
        nahe(listOf(0.0, 0.0), Polarkoordinaten.polar(t(0.0, 0.0)))
        nahe(listOf(-2.0, 0.0), Polarkoordinaten.kartesisch(t(2.0, 180.0)))
    }

    @Test fun `Kugelkoordinaten enthalten beide Pole und nicht bestimmte Teilwinkel`() {
        nahe(listOf(2.0, 0.0, 0.0), Polarkoordinaten.polar(t(0.0, 0.0, 2.0)))
        nahe(listOf(2.0, 0.0, 180.0), Polarkoordinaten.polar(t(0.0, 0.0, -2.0)))
        nahe(listOf(0.0, 0.0, -2.0), Polarkoordinaten.kartesisch(t(2.0, 123.0, 180.0)), 0.0)
        nahe(listOf(1.0, 0.0, 180.0, 90.0), Polarkoordinaten.polar(t(0.0, 0.0, -1.0, 0.0)))
    }

    @Test fun `R1 liefert skalares Vorzeichen und R2 ein Einertupel`() {
        for (x in listOf(-3.0, 0.0, 3.0)) {
            val p = Polarkoordinaten.polar(t(x))
            val s = Polarkoordinaten.winkelOderVorzeichen(t(x))
            assertEquals(1, assertIs<Tupel>(s).elemente.size)
            assertIs<ZahlAusdruck>(s.elemente.single())
            assertEquals(2, p.elemente.size)
            nahe(listOf(x), Polarkoordinaten.kartesisch(p), 0.0)
            nahe(listOf(x), Polarkoordinaten.kartesisch(p, eindimensional = true), 0.0)
            nahe(listOf(x), Polarkoordinaten.kartesisch(Polarkoordinaten.radius(t(x)), s), 0.0)
        }
        assertIs<Tupel>(Polarkoordinaten.winkelOderVorzeichen(t(1.0, 0.0)))
        assertEquals(2, Polarkoordinaten.kartesisch(rationaleKoordinate(2.0), t(0.0)).elemente.size)
        assertEquals(1, Polarkoordinaten.kartesisch(rationaleKoordinate(2.0), rationaleKoordinate(1.0)).elemente.size)
    }

    @Test fun `Rundreisen in Dimension 2 bis 32 sind numerisch stabil`() {
        val zufall = Random(42)
        for (n in 2..32) repeat(8) {
            val x = List(n) { zufall.nextDouble(-100.0, 100.0) }
            val p = Polarkoordinaten.polar(t(*x.toDoubleArray()))
            assertEquals(2, p.elemente.size)
            assertEquals(n - 1, assertIs<Tupel>(p.elemente[1]).elemente.size)
            nahe(x, Polarkoordinaten.kartesisch(p), 1e-10)
        }
        for (skala in listOf(1e-200, 1e200)) {
            val x = t(3 * skala, 4 * skala)
            val p = Polarkoordinaten.polar(x)
            assertEquals(5.0, werte(p)[0] / skala, 1e-12)
            val r = werte(Polarkoordinaten.kartesisch(p))
            assertEquals(3.0, r[0] / skala, 1e-12)
            assertEquals(4.0, r[1] / skala, 1e-12)
        }
        assertTrue(werte(Polarkoordinaten.polar(t(1.0, -1e-20)))[1] < 360.0)
    }

    @Test fun `Ungueltige und unbestimmte Eingaben werden diagnostiziert`() {
        listOf(Tupel(emptyList()), Tupel(listOf(Variable("x"))), Tupel(listOf(KomplexeZahl(RationaleZahl.Eins, RationaleZahl.Eins))),
            Tupel(listOf(t(1.0))), t(Double.MAX_VALUE, Double.MAX_VALUE)).forEach { assertFails { Polarkoordinaten.polar(it) } }
        listOf(t(-1.0, 0.0), t(1.0, -1.0), t(1.0, 360.0), t(1.0, 0.0, 181.0), t(1.0, 0.0, -1.0),
            t(1.0)).forEach { assertFails { Polarkoordinaten.kartesisch(it) } }
        listOf(t(1.0, 0.0), t(1.0, 2.0), t(1.0, 1.0, 1.0)).forEach {
            assertFails { Polarkoordinaten.kartesisch(it, eindimensional = true) }
        }
        assertFails { Polarkoordinaten.kartesisch(RationaleZahl.Eins, Tupel(emptyList())) }
    }

    @Test fun `Rechnerdispatcher liefert passende Ergebnisse und Fehler`() {
        val anfrage = VektorRechnerAnfrage(VektorRechnerOperator.RADIUS, objekte = listOf(t(3.0, 4.0)))
        val r = assertIs<VektorRechnerErgebnis.ZahlWert>(VektorRechner.erzeuge(anfrage)).wert
        nahe(listOf(5.0), Tupel(listOf(r)))
        assertIs<VektorRechnerErgebnis.VektorWert>(VektorRechner.erzeuge(anfrage.copy(operator = VektorRechnerOperator.POLARWINKEL)))
        assertIs<VektorRechnerErgebnis.ZahlWert>(VektorRechner.erzeuge(anfrage.copy(operator = VektorRechnerOperator.POLARWINKEL, objekte = listOf(t(-3.0)))))
        assertIs<VektorRechnerErgebnis.Ungueltig>(VektorRechner.erzeuge(anfrage.copy(objekte = emptyList())))
        val symbolisch = anfrage.copy(objekte = listOf(Tupel(listOf(Variable("x")))))
        assertIs<VektorRechnerErgebnis.Ungueltig>(VektorRechner.erzeuge(symbolisch))
    }
}
