package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TupelOperationKnotenTest {
    private val register = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereTupelOperationKnoten()
    }

    @Test
    fun `Tupel ergänzen ist im Katalog sichtbar`() {
        val arten = alleMathematikKnotenVorlagen().map { it.art }
        assertTrue(TUPEL_ERGÄNZEN_ART in arten)
        assertTrue(TUPEL_AUFLÖSEN_ART in arten)
    }

    @Test
    fun `Tupelmodus verkettet direkte Komponenten in Anschlussreihenfolge`() {
        val knoten = TupelOperationKnotenVorlagen.Ergänzen.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(TUPEL_ERGÄNZEN_ART)!!
        val verschachtelt = Tupel(listOf(RationaleZahl.von(7)))

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "tupel1" to BedingterWert(Tupel(listOf(RationaleZahl.von(1), verschachtelt))),
                    "tupel2" to BedingterWert(Tupel(listOf(RationaleZahl.von(2)))),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(
            listOf(RationaleZahl.von(1), verschachtelt, RationaleZahl.von(2)),
            assertIs<Tupel>(ergebnis.ausgaben.getValue("tupel").objekt).elemente,
        )
    }

    @Test
    fun `Elementmodus lässt ein Tupel als einzelnes verschachteltes Element stehen`() {
        val standard = TupelOperationKnotenVorlagen.Ergänzen.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereTupelErgänzen(standard, TupelErgänzenModus.Elemente)
        val auswerter = register.finde(TUPEL_ERGÄNZEN_ART)!!
        val element = Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(3)))

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "basistupel" to BedingterWert(Tupel(listOf(RationaleZahl.von(1)))),
                    "element1" to BedingterWert(element),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(
            Tupel(listOf(RationaleZahl.von(1), element)),
            ergebnis.ausgaben.getValue("tupel").objekt,
        )
    }

    @Test
    fun `Moduswechsel bewahrt nur ersten Tupelanschluss und Ausgang`() {
        val standard = TupelOperationKnotenVorlagen.Ergänzen.erzeuge(GraphPunkt.Zero)
        val erster = standard.anschlüsse.first { it.name == "tupel1" }
        val zweiter = standard.anschlüsse.first { it.name == "tupel2" }
        val ausgang = standard.anschlüsse.first { it.name == "tupel" }

        val elementModus = konfiguriereTupelErgänzen(standard, TupelErgänzenModus.Elemente)

        assertEquals(erster.id, elementModus.anschlüsse.first { it.name == "basistupel" }.id)
        assertEquals(ausgang.id, elementModus.anschlüsse.first { it.name == "tupel" }.id)
        assertNotEquals(zweiter.id, elementModus.anschlüsse.first { it.name == "element1" }.id)
        assertEquals(MathematikAnschlussArten.Objekt.id, elementModus.anschlüsse.first { it.name == "element1" }.art)
    }

    @Test
    fun `Tupelmodus weist Nichttupel am Tupelanschluss zurück`() {
        val knoten = TupelOperationKnotenVorlagen.Ergänzen.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(TUPEL_ERGÄNZEN_ART)!!

        val fehler = assertFailsWith<IllegalStateException> {
            auswerter.auswerten(
                KnotenAuswertungsKontext(
                    knoten = knoten,
                    eingänge = mapOf(
                        "tupel1" to BedingterWert(Tupel(listOf(RationaleZahl.von(1)))),
                        "tupel2" to BedingterWert(RationaleZahl.von(2)),
                    ),
                    rechenKontext = RechenKontext(),
                ),
            )
        }
        assertTrue(fehler.message.orEmpty().contains("Tupel"))
    }
}
