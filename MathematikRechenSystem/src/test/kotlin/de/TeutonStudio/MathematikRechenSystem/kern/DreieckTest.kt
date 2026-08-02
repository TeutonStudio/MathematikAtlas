package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DreieckTest {
    @Test
    fun `Zwei Winkel bestimmen den dritten aber kein Geometrieobjekt`() {
        val lösung = assertIs<DreiecksLösung.Partiell>(
            löseDreieck(
                DreiecksWerte(
                    alpha = Division(Pi, RationaleZahl.von(3)),
                    beta = Division(Pi, RationaleZahl.von(4)),
                ),
            ),
        )

        assertNull(lösung.werte.a)
        assertNull(lösung.werte.b)
        assertNull(lösung.werte.c)
        val gamma = lösung.werte.gamma ?: error("Der dritte Winkel fehlt.")
        assertEquals(5.0 * PI / 12.0, gamma.numerisch(), 1e-8)
    }

    @Test
    fun `Drei vier fünf erzeugt ein vollständiges Dreieck`() {
        val lösung = assertIs<DreiecksLösung.Vollständig>(
            löseDreieck(
                DreiecksWerte(
                    a = RationaleZahl.von(3),
                    b = RationaleZahl.von(4),
                    c = RationaleZahl.von(5),
                ),
            ),
        )

        assertTrue(lösung.werte.istVollständig)
        assertEquals(RationaleZahl.von(5), lösung.dreieck.c)
        val struktur = strukturVon(lösung.dreieck)
        assertEquals(listOf(0, 1, 2), struktur.stufen.map { it.dimension })
        assertEquals(listOf(3, 3, 1), struktur.stufen.map { it.zellen.size })
        assertEquals(listOf("A", "B", "C"), struktur.stufen[0].zellen.map { it.id })
        assertEquals(listOf("a", "b", "c"), struktur.stufen[1].zellen.map { it.id })
        assertEquals("flaeche", struktur.stufen[2].zellen.single().id)
    }

    @Test
    fun `Verletzte Dreiecksungleichung ist ungültig`() {
        val lösung = assertIs<DreiecksLösung.Ungültig>(
            löseDreieck(
                DreiecksWerte(
                    a = RationaleZahl.von(1),
                    b = RationaleZahl.von(2),
                    c = RationaleZahl.von(3),
                ),
            ),
        )
        assertTrue(lösung.grund.contains("Dreiecksungleichung"))
    }

    @Test
    fun `SSA bewahrt beide gültigen Kandidaten`() {
        val lösung = assertIs<DreiecksLösung.Mehrdeutig>(
            löseDreieck(
                DreiecksWerte(
                    a = RationaleZahl.von(10),
                    b = RationaleZahl.von(12),
                    alpha = Division(Pi, RationaleZahl.von(6)),
                ),
            ),
        )
        assertEquals(2, lösung.kandidaten.size)
        assertEquals(RationaleZahl.von(10), lösung.werte.a)
        assertEquals(RationaleZahl.von(12), lösung.werte.b)
        assertNull(lösung.werte.beta)
    }

    private fun ZahlAusdruck.numerisch(): Double = when (val ergebnis = NumerischerAuswerter.wert(this)) {
        is NumerischesErgebnis.Wert -> ergebnis.wert
        is NumerischesErgebnis.Fehler -> error(ergebnis.beschreibung)
    }
}
