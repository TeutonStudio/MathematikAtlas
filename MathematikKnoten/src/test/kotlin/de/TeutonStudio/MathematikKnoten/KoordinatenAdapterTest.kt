package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.KoordinatenAdapter
import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.KoordinatenErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KoordinatenAdapterTest {
    @Test
    fun `zweidimensionales Tupel wird als reelle Koordinate erkannt`() {
        val ergebnis = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(
                Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
                2,
            ),
        )
        assertEquals(listOf(1.0, 2.0), ergebnis.werte)
    }

    @Test
    fun `zu grosse Dimension verlangt explizite Projektion`() {
        val ergebnis = assertIs<KoordinatenErgebnis.ProjektionErforderlich>(
            KoordinatenAdapter.extrahiere(
                Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3), RationaleZahl.von(4))),
                3,
            ),
        )
        assertEquals(4, ergebnis.vorhandeneDimension)
        assertEquals(3, ergebnis.erwarteteDimension)
    }

    @Test
    fun `Variablen werden mit numerischer Umgebung ausgewertet`() {
        val ergebnis = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(
                Tupel(listOf(Variable("x"), Variable("y"))),
                2,
                mapOf("x" to 1.25, "y" to -2.5),
            ),
        )
        assertEquals(listOf(1.25, -2.5), ergebnis.werte)
    }

    @Test
    fun `fehlende Variablen bleiben bedingt darstellbar`() {
        assertIs<KoordinatenErgebnis.BedingtDarstellbar>(
            KoordinatenAdapter.extrahiere(
                Tupel(listOf(Variable("x"), Variable("y"))),
                2,
            ),
        )
    }

    @Test
    fun `eindimensionale Zahl wird direkt visualisiert`() {
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
        val komplex = KomplexeZahl(RationaleZahl.von(3), RationaleZahl.Eins)
        val ergebnis = assertIs<KoordinatenErgebnis.Darstellbar>(
            KoordinatenAdapter.extrahiere(komplex, 2),
        )
        assertEquals(listOf(3.0, 1.0), ergebnis.werte)
        val latex = komplex.zuLatex()
        assertEquals(listOf("Re($latex)", "Im($latex)"), ergebnis.komponenten.map { it.semantik })
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
        assertTrue(ergebnis.komponenten[0].semantik?.startsWith("Re(") == true)
        assertTrue(ergebnis.komponenten[1].semantik?.startsWith("Im(") == true)
        assertEquals("Komponente 2", ergebnis.komponenten[2].semantik)
    }

    @Test
    fun `R mal C behaelt Faktorordnung und wird dreidimensional`() {
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
        assertEquals("Komponente 1", ergebnis.komponenten[0].semantik)
        assertTrue(ergebnis.komponenten[1].semantik?.startsWith("Re(") == true)
        assertTrue(ergebnis.komponenten[2].semantik?.startsWith("Im(") == true)
    }

    @Test
    fun `komplexe zwei komponenten passen nicht implizit in R1`() {
        val ergebnis = assertIs<KoordinatenErgebnis.ProjektionErforderlich>(
            KoordinatenAdapter.extrahiere(
                KomplexeZahl(RationaleZahl.Eins, RationaleZahl.von(2)),
                1,
            ),
        )
        assertEquals(2, ergebnis.vorhandeneDimension)
        assertEquals(1, ergebnis.erwarteteDimension)
    }
}
