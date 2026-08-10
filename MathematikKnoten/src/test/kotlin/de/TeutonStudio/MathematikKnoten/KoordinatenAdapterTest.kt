package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KoordinatenAdapterTest {
    @Test
    fun `Tupel Zeile und Spalte verwenden denselben Strukturvertrag`() {
        val objekte = listOf<MathematischesObjekt>(
            Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
            ZeilenVektor(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
            SpaltenVektor(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
        )
        val ergebnisse = objekte.map {
            assertIs<KoordinatenErgebnis.Darstellbar>(KoordinatenAdapter.extrahiere(it, 2)).werte
        }
        assertEquals(listOf(1.0, 2.0), ergebnisse[0])
        assertEquals(ergebnisse[0], ergebnisse[1])
        assertEquals(ergebnisse[0], ergebnisse[2])
    }

    @Test
    fun `heterogenes Tupel wird nicht als Koordinate fehlinterpretiert`() {
        val objekt = Tupel(listOf(RationaleZahl.Eins, WahrheitsKonstante(true)))
        val ergebnis = assertIs<KoordinatenErgebnis.NichtDarstellbar>(
            KoordinatenAdapter.extrahiere(objekt, 2),
        )
        assertTrue("keine Zahl" in ergebnis.grund)
    }

    @Test
    fun `zu viele reelle Komponenten verlangen explizite Projektion`() {
        val objekt = Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3)))
        val ergebnis = assertIs<KoordinatenErgebnis.ProjektionErforderlich>(
            KoordinatenAdapter.extrahiere(objekt, 2),
        )
        assertEquals(3, ergebnis.vorhandeneDimension)
        assertEquals(2, ergebnis.erwarteteDimension)
        assertTrue("Projektion" in ergebnis.grund)
    }

    @Test
    fun `zu wenige Komponenten bleiben Dimensionsfehler`() {
        val objekt = Tupel(listOf(RationaleZahl.Eins))
        val ergebnis = assertIs<KoordinatenErgebnis.NichtDarstellbar>(
            KoordinatenAdapter.extrahiere(objekt, 2),
        )
        assertTrue("Koordinatendimension 1 statt erwartet 2" in ergebnis.grund)
    }

    @Test
    fun `reeller skalar bleibt eindimensional`() {
        val eindimensional = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(RationaleZahl.von(7), 1),
        )
        assertEquals(listOf(7.0), eindimensional.werte)
    }

    @Test
    fun `Fallausdruck nutzt denselben Adapter nach Auswahl des Zweigs`() {
        val objekt = FallAusdruck(
            Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
            WahrheitsKonstante(true),
            Tupel(listOf(RationaleZahl.von(3), RationaleZahl.von(4))),
        )
        val ergebnis = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(objekt, 2),
        )
        assertEquals(listOf(1.0, 2.0), ergebnis.werte)
    }

    @Test
    fun `komplexer skalar wird kanonisch in real und imaginaerteil entfaltet`() {
        val ergebnis = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(
                KomplexeZahl(RationaleZahl.von(3), RationaleZahl.Eins),
                2,
            ),
        )
        assertEquals(listOf(3.0, 1.0), ergebnis.werte)
        assertEquals(listOf("Re(3+i)", "Im(3+i)"), ergebnis.komponenten.map { it.semantik })
    }

    @Test
    fun `reell eingebettete komplexe zahl behaelt zwei komplexe komponenten`() {
        val ergebnis = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(
                KomplexeZahl(RationaleZahl.von(3), RationaleZahl.Null),
                2,
            ),
        )
        assertEquals(listOf(3.0, 0.0), ergebnis.werte)
    }

    @Test
    fun `C mal R behaelt Faktorordnung und wird dreidimensional`() {
        val objekt = Tupel(
            listOf(
                KomplexeZahl(RationaleZahl.Eins, RationaleZahl.von(2)),
                RationaleZahl.von(3),
            ),
        )
        val ergebnis = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(objekt, 3),
        )
        assertEquals(listOf(1.0, 2.0, 3.0), ergebnis.werte)
    }

    @Test
    fun `R mal C behaelt die umgekehrte Faktorordnung`() {
        val objekt = Tupel(
            listOf(
                RationaleZahl.von(3),
                KomplexeZahl(RationaleZahl.Eins, RationaleZahl.von(2)),
            ),
        )
        val ergebnis = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(objekt, 3),
        )
        assertEquals(listOf(3.0, 1.0, 2.0), ergebnis.werte)
    }

    @Test
    fun `komplexer skalar in R1 verlangt Projektion`() {
        val ergebnis = assertIs<KoordinatenErgebnis.ProjektionErforderlich>(
            KoordinatenAdapter.extrahiere(
                KomplexeZahl(RationaleZahl.von(3), RationaleZahl.Eins),
                1,
            ),
        )
        assertEquals(2, ergebnis.vorhandeneDimension)
    }

    @Test
    fun `Quaternion verlangt R4 oder eine ausdrueckliche Projektion`() {
        val ergebnis = assertIs<KoordinatenErgebnis.ProjektionErforderlich>(
            KoordinatenAdapter.extrahiere(
                Variable("q"),
                1,
                domänenUmgebung = mapOf(
                    "q" to DomaenenWert.Quaternion(
                        java.math.BigDecimal.ONE,
                        java.math.BigDecimal.ONE,
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO,
                    ),
                ),
            ),
        )
        assertEquals(4, ergebnis.vorhandeneDimension)
        assertTrue("R⁴" in ergebnis.grund)
        assertTrue("Projektion" in ergebnis.grund)
    }

    @Test
    fun `Koordinatenbild verwendet die Dimension seines Systems`() {
        val raum = EuklidischerRaum("E", 2)
        val system = GeometrischesKoordinatensystem(raum, "K")
        val bild = KoordinatenBild(
            GeometriePunkt("A", raum, Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2)))),
            system,
        )
        assertEquals(
            listOf(1.0, 2.0),
            assertIs<KoordinatenErgebnis.Darstellbar>(KoordinatenAdapter.extrahiere(bild, 2)).werte,
        )
        assertIs<KoordinatenErgebnis.ProjektionErforderlich>(KoordinatenAdapter.extrahiere(bild, 1))
    }
}
