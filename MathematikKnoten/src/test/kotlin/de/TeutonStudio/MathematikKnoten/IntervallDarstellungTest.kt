package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class IntervallDarstellungTest {
    @Test fun `Konjunktion wird als aufsteigende Intervallkette dargestellt`() {
        val x = Variable("x")
        val knoten = MathematikKnotenVorlagen.Konjunktion.erzeuge(GraphPunkt.Zero)
        val ergebnis = GesamterMathematikAuswerter.erzeugeRegister().finde("mathematik.konjunktion")!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "a" to BedingterWert(Vergleich(x, VergleichsArt.Kleiner, RationaleZahl.von(6))),
                    "b" to BedingterWert(Vergleich(RationaleZahl.von(2), VergleichsArt.Kleiner, x)),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals("2 < x < 6", ergebnis.ausgaben.getValue("aussage").latexDarstellung)
        assertIs<Konjunktion>(ergebnis.ausgaben.getValue("aussage").objekt)
    }
}
