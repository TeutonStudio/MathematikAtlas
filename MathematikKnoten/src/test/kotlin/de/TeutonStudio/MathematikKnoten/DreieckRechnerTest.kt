package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.abs
import kotlin.test.*

class DreieckRechnerTest {
    @Test fun `SSS Dreieck wird eindeutig vollständig berechnet`() {
        val ergebnis = löseDreieck(DreieckEingabe(a = 3.0, b = 4.0, c = 5.0))
        val lösung = ergebnis.lösungen.single()

        assertEquals(DreieckStatus.Eindeutig, ergebnis.status)
        assertTrue(abs(lösung.gamma - 90.0) < 1e-6)
        assertTrue(abs(lösung.alpha - 36.86989765) < 1e-5)
        assertTrue(abs(lösung.beta - 53.13010235) < 1e-5)
    }

    @Test fun `zwei Winkel und eine Seite bestimmen das Dreieck`() {
        val ergebnis = löseDreieck(DreieckEingabe(a = 5.0, alpha = 30.0, beta = 60.0))
        val lösung = ergebnis.lösungen.single()

        assertEquals(DreieckStatus.Eindeutig, ergebnis.status)
        assertTrue(abs(lösung.gamma - 90.0) < 1e-6)
        assertTrue(abs(lösung.c - 10.0) < 1e-5)
    }

    @Test fun `SSW kann zwei legitime Dreiecke liefern`() {
        val ergebnis = löseDreieck(DreieckEingabe(a = 10.0, b = 12.0, alpha = 30.0))

        assertEquals(DreieckStatus.Mehrdeutig, ergebnis.status)
        assertEquals(2, ergebnis.lösungen.size)
    }

    @Test fun `zusätzlicher widersprüchlicher Wert macht Kombination ungültig`() {
        val ergebnis = löseDreieck(DreieckEingabe(a = 3.0, b = 4.0, c = 5.0, gamma = 80.0))

        assertEquals(DreieckStatus.Ungültig, ergebnis.status)
        assertTrue(ergebnis.lösungen.isEmpty())
    }

    @Test fun `zu wenige Werte bleiben unzureichend`() {
        val ergebnis = löseDreieck(DreieckEingabe(a = 3.0, b = 4.0))

        assertEquals(DreieckStatus.Unzureichend, ergebnis.status)
    }

    @Test fun `Knotenauswerter veröffentlicht Werte erst bei eindeutiger Lösung`() {
        val knoten = ErweiterteMathematikKnotenVorlagen.DreieckRechner.erzeuge(GraphPunkt.Zero)
        val auswerter = GesamterMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val eindeutig = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "a" to BedingterWert(RationaleZahl.von(3)),
                "b" to BedingterWert(RationaleZahl.von(4)),
                "c" to BedingterWert(RationaleZahl.von(5)),
            ),
            RechenKontext(),
        ))
        assertTrue("gammaWert" in eindeutig.ausgaben)
        assertEquals(WahrheitsKonstante(true), eindeutig.ausgaben.getValue("bestimmt").objekt)

        val mehrdeutig = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "a" to BedingterWert(RationaleZahl.von(10)),
                "b" to BedingterWert(RationaleZahl.von(12)),
                "alpha" to BedingterWert(RationaleZahl.von(30)),
            ),
            RechenKontext(),
        ))
        assertFalse("gammaWert" in mehrdeutig.ausgaben)
        assertEquals(WahrheitsKonstante(false), mehrdeutig.ausgaben.getValue("bestimmt").objekt)
        assertEquals(WahrheitsKonstante(true), mehrdeutig.ausgaben.getValue("gültig").objekt)
    }
}
