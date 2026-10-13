package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VariablenStrukturTest {
    private fun basis() = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)

    @Test fun `Winkeltupel traegt korrekte Winkelbereiche und symbolische Komponenten`() {
        val k = konfiguriereVariablenStruktur(basis(), "winkeltupel")
            .let { it.copy(parameter = it.parameter + (VARIABLE_DIMENSION to "2")) }
        val wert = variablenStrukturWert(k, ReelleZahlen)
        assertEquals(MathematikAnschlussArten.Tupel.id, k.anschlüsse.single { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang }.art)
        assertEquals(3, (wert.objekt as Tupel).elemente.size)
        assertEquals(listOf(winkelraum(2)), wert.variablenQuellen.map { it.werteVorrat })
        assertTrue((wert.objekt as Tupel).elemente.all { it is SymbolischeZahlKomponente })
    }

    @Test fun `Winkeltupel mit n null hat nur Vorzeichen`() {
        val k = konfiguriereVariablenStruktur(basis(), VARIABLE_STRUKTUR_WINKEL)
            .let { it.copy(parameter = it.parameter + (VARIABLE_DIMENSION to "0")) }
        val wert = variablenStrukturWert(k, ReelleZahlen)
        assertEquals(1, (wert.objekt as Tupel).elemente.size)
        assertEquals(winkelraum(0),
            wert.variablenQuellen.single().werteVorrat)
    }

    @Test fun `Ausgangs IDs bleiben beim Strukturwechsel erhalten`() {
        val k = basis()
        val v = konfiguriereVariablenStruktur(k, "spalte")
        assertEquals(k.anschlüsse.map { it.id }, v.anschlüsse.map { it.id })
        assertIs<SpaltenVektor>(variablenStrukturWert(v, ReelleZahlen).objekt)
        assertEquals("zahl", variablenStruktur(k))
    }

    @Test fun `ungueltige Dimension wird abgewiesen`() {
        val k = basis().copy(parameter = basis().parameter + (VARIABLE_DIMENSION to "0"))
        assertFailsWith<IllegalArgumentException> { variablenDimension(k) }
    }
}
