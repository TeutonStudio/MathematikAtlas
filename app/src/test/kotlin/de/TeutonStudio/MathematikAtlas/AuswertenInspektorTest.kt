package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals

class AuswertenInspektorTest {
    @Test
    fun `separater rechter Vektor wird als b-Spalte getrennt`() {
        val konfiguration = auswertungsTabellenKonfiguration(
            modus = "automatisch",
            variablenText = "u, v",
            eingabeSpalten = 2,
            verlaufsSpalten = 3,
            hatRechteSeiteEingang = true,
        )

        assertEquals(listOf("u", "v", "b"), konfiguration.spaltenNamen)
        assertEquals(1, konfiguration.rechteSeitenSpalten)
    }

    @Test
    fun `erweiterte Matrix im Systemmodus verwendet letzte Spalte als rechte Seite`() {
        val konfiguration = auswertungsTabellenKonfiguration(
            modus = "linearesSystem",
            variablenText = "x, y, z",
            eingabeSpalten = 4,
            verlaufsSpalten = 4,
            hatRechteSeiteEingang = false,
        )

        assertEquals(listOf("x", "y", "z", "b"), konfiguration.spaltenNamen)
        assertEquals(1, konfiguration.rechteSeitenSpalten)
    }

    @Test
    fun `inverse teilt erweiterte Matrix in Koeffizienten und Einheitsmatrix`() {
        val konfiguration = auswertungsTabellenKonfiguration(
            modus = "inverse",
            variablenText = "a, b, c",
            eingabeSpalten = 3,
            verlaufsSpalten = 6,
            hatRechteSeiteEingang = false,
        )

        assertEquals(listOf("a", "b", "c", "e_1", "e_2", "e_3"), konfiguration.spaltenNamen)
        assertEquals(3, konfiguration.rechteSeitenSpalten)
    }

    @Test
    fun `ungueltige Variablenliste faellt deterministisch auf x-Indizes zurueck`() {
        val konfiguration = auswertungsTabellenKonfiguration(
            modus = "zeilenstufenform",
            variablenText = "x, x",
            eingabeSpalten = 3,
            verlaufsSpalten = 3,
            hatRechteSeiteEingang = false,
        )

        assertEquals(listOf("x_1", "x_2", "x_3"), konfiguration.spaltenNamen)
        assertEquals(0, konfiguration.rechteSeitenSpalten)
    }
}
