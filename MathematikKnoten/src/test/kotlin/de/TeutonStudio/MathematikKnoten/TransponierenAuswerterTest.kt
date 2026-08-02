package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class TransponierenAuswerterTest {
    private val eins = RationaleZahl.von(1)
    private val zwei = RationaleZahl.von(2)
    private val drei = RationaleZahl.von(3)
    private val vier = RationaleZahl.von(4)
    private val auswerter = GesamterMathematikAuswerter.erzeugeRegister().finde("mathematik.transponieren")!!

    @Test fun `Spaltenvektor wird zur Zeile und Metadaten bleiben erhalten`() {
        val knoten = MathematikKnotenVorlagen.Transponieren.erzeuge(GraphPunkt.Zero)
        val annahme = Gleichheit(eins, eins)
        val eingang = BedingterWert(SpaltenVektor(listOf(eins, zwei)), annahmen = setOf(annahme))

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(knoten, mapOf("wert" to eingang), RechenKontext()))
        val ausgang = ergebnis.ausgaben.getValue("wert")

        assertEquals(ZeilenVektor(listOf(eins, zwei)), ausgang.objekt)
        assertEquals(eingang.annahmen, ausgang.annahmen)
    }

    @Test fun `rechteckige Matrix wird korrekt transponiert`() {
        val knoten = MathematikKnotenVorlagen.Transponieren.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(listOf(listOf(eins, zwei, drei), listOf(vier, eins, zwei)))

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(knoten, mapOf("wert" to BedingterWert(matrix)), RechenKontext()))

        assertEquals(
            Matrix(listOf(listOf(eins, vier), listOf(zwei, eins), listOf(drei, zwei))),
            ergebnis.ausgaben.getValue("wert").objekt,
        )
    }

    @Test fun `Tensor verwendet konfigurierte Achsenpermutation`() {
        val knoten = MathematikKnotenVorlagen.Transponieren.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("achsenPermutation" to "1,0,2"),
        )
        val tensor = Tensor(listOf(2, 2, 2), List(8) { RationaleZahl.von(it.toLong()) })

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(knoten, mapOf("wert" to BedingterWert(tensor)), RechenKontext()))
        val ausgang = assertIs<Tensor>(ergebnis.ausgaben.getValue("wert").objekt)

        assertEquals(listOf(2, 2, 2), ausgang.dimensionen)
        assertEquals(tensor.wertAn(listOf(1, 0, 1)), ausgang.wertAn(listOf(0, 1, 1)))
    }
}
