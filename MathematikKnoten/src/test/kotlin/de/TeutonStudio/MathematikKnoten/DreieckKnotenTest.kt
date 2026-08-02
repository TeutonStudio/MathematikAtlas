package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DreieckKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Dreiecksvorlage besitzt sechs optionale Maße und einen Geometrieausgang`() {
        val vorlage = GeometrieKnotenVorlagen.Dreieck
        assertNotNull(register.finde(vorlage.art))
        assertEquals(
            listOf("a", "b", "c", "α", "β", "γ"),
            vorlage.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.name },
        )
        assertEquals(
            listOf("a", "b", "c", "α", "β", "γ", "dreieck"),
            vorlage.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.map { it.name },
        )
        assertEquals(GeometrieAnschlussArten.Dreieck.id, vorlage.anschlüsse.last().art)
    }

    @Test
    fun `Zwei Winkel liefern den dritten als partielles Ergebnis`() {
        val ergebnis = auswerten(
            mapOf(
                "α" to Division(Pi, RationaleZahl.von(3)),
                "β" to Division(Pi, RationaleZahl.von(4)),
            ),
        )
        assertNotNull(ergebnis.ausgaben["γ"])
        assertFalse("dreieck" in ergebnis.ausgaben)
        assertNull(ergebnis.fehler)
        assertTrue(ergebnis.warnungen.single().contains("Ähnlichkeitsklasse"))
    }

    @Test
    fun `SSS liefert Maße und GeometrieDreieck`() {
        val ergebnis = auswerten(
            mapOf(
                "a" to RationaleZahl.von(3),
                "b" to RationaleZahl.von(4),
                "c" to RationaleZahl.von(5),
            ),
        )
        assertEquals(setOf("a", "b", "c", "α", "β", "γ", "dreieck"), ergebnis.ausgaben.keys)
        val dreieck = assertIs<GeometrieDreieck>(ergebnis.ausgaben.getValue("dreieck").objekt)
        assertEquals(GeometrieAnschlussArten.Dreieck.id, geometrieAnschlussArt(dreieck))
        assertEquals(listOf(3, 3, 1), strukturVon(dreieck).stufen.map { it.zellen.size })
    }

    @Test
    fun `Mehrdeutiger SSA Fall erzeugt kein willkürliches Dreieck`() {
        val ergebnis = auswerten(
            mapOf(
                "a" to RationaleZahl.von(10),
                "b" to RationaleZahl.von(12),
                "α" to Division(Pi, RationaleZahl.von(6)),
            ),
        )
        assertFalse("dreieck" in ergebnis.ausgaben)
        assertTrue(ergebnis.warnungen.single().contains("zwei mögliche"))
    }

    private fun auswerten(eingänge: Map<String, ZahlAusdruck>) = register
        .finde(GeometrieKnotenVorlagen.Dreieck.art)!!
        .auswerten(
            KnotenAuswertungsKontext(
                knoten = GeometrieKnotenVorlagen.Dreieck.erzeuge(GraphPunkt.Zero),
                eingänge = eingänge.mapValues { BedingterWert(it.value) },
                rechenKontext = RechenKontext(),
            ),
        )
}
