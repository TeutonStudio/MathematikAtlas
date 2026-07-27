package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import kotlin.test.Test
import kotlin.test.assertEquals

class AssoziativeOperatorenTest {
    @Test
    fun `unverbundene Additionsanschluesse werden zu eindeutigen Unbekannten`() {
        val addition = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(addition.art)!!

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(addition, emptyMap(), RechenKontext()))

        assertEquals(
            "\\mathrm{Addition}_{\\mathrm{eingabe}_{1}} + \\mathrm{Addition}_{\\mathrm{eingabe}_{2}}",
            ergebnis.ausgaben.getValue("wert").objekt.zuLatex(),
        )
    }

    @Test
    fun `gleiche Anschlusspositionen unterschiedlicher Knoten bleiben verschiedene Unbekannte`() {
        val erster = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val zweiter = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(erster.art)!!

        val erstesErgebnis = auswerter.auswerten(KnotenAuswertungsKontext(erster, emptyMap(), RechenKontext()))
        val zweitesErgebnis = auswerter.auswerten(KnotenAuswertungsKontext(zweiter, emptyMap(), RechenKontext()))

        check(erstesErgebnis.ausgaben.getValue("wert").objekt != zweitesErgebnis.ausgaben.getValue("wert").objekt)
    }
}
