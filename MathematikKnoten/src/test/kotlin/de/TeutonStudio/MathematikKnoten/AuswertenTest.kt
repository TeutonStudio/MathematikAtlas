package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuswertenTest {
    @Test
    fun `falsche Aussagen geben Lüge aus`() {
        val auswerten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val aussage = Gleichheit(RationaleZahl.von(2), EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2), RationaleZahl.von(3))))
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(auswerten.art)!!

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(auswerten, mapOf("objekt" to BedingterWert(aussage)), RechenKontext()),
        )

        val auswertung = assertIs<WahrheitsKonstante>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(false, auswertung.wert)
    }

    @Test
    fun `wahre Aussagen geben Wahr aus`() {
        val auswerten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val aussage = Gleichheit(RationaleZahl.von(2), RationaleZahl.von(2))
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(auswerten.art)!!

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(auswerten, mapOf("objekt" to BedingterWert(aussage)), RechenKontext()),
        )

        val auswertung = assertIs<WahrheitsKonstante>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(true, auswertung.wert)
    }
}
