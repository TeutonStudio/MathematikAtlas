package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StrukturRechnerKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Neue Strukturknoten sind im Katalog und Auswerter registriert`() {
        val arten = alleMathematikKnotenVorlagen().map { it.art }.toSet()
        val erwartet = setOf(
            SKALARPRODUKT_ART,
            TENSORPRODUKT_ART,
            DIMENSIONEN_ART,
            TensorRechner.KNOTEN_ART,
            AussagenSatzRechner.KNOTEN_ART,
            CAUCHY_ART,
        )

        assertTrue(erwartet.all(arten::contains))
        erwartet.forEach { assertNotNull(register.finde(it), "Auswerter für $it fehlt") }
    }

    @Test
    fun `Skalarproduktknoten akzeptiert Zeile und Spalte gemeinsam`() {
        val knoten = StrukturRechnerKnotenVorlagen.Skalarprodukt.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(SKALARPRODUKT_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "links" to BedingterWert(ZeilenVektor(listOf(zahl(1), zahl(2)))),
                    "rechts" to BedingterWert(SpaltenVektor(listOf(zahl(3), zahl(4)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(zahl(11), ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Dimensionenknoten gibt Form und Stufe getrennt aus`() {
        val knoten = StrukturRechnerKnotenVorlagen.Dimensionen.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(
            listOf(
                listOf(zahl(1), zahl(2), zahl(3)),
                listOf(zahl(4), zahl(5), zahl(6)),
            ),
        )
        val ergebnis = register.finde(DIMENSIONEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("objekt" to BedingterWert(matrix)),
                RechenKontext(),
            ),
        )

        assertEquals(Tupel(listOf(zahl(2), zahl(3))), ergebnis.ausgaben.getValue("dimensionen").objekt)
        assertEquals(zahl(2), ergebnis.ausgaben.getValue("stufe").objekt)
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
