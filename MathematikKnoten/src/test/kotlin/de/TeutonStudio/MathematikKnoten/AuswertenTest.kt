package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Funktion
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

    @Test
    fun `Term zu Methode übernimmt Argumente Wertevorrat und Zielmenge`() {
        val knoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "term" to BedingterWert(Variable("x")),
                "argument1" to BedingterWert(Variable("x"), werteVorrat = ReelleZahlen),
                "zielmenge" to BedingterWert(ReelleZahlen),
            ),
            RechenKontext(),
        ))

        val methode = assertIs<Funktion>(ergebnis.ausgaben.getValue("methode").objekt)
        assertEquals(listOf(Variable("x")), methode.parameter)
        assertEquals(ReelleZahlen, methode.werteVorräte.getValue("x"))
        assertEquals(ReelleZahlen, methode.einzigeZielMenge)
    }
}
