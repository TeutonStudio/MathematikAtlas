package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.BenannteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.SymmetrischeDifferenz
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SymmetrischeDifferenzKnotenTest {
    @Test
    fun `Vorlage ist erzeugbar und der Auswerter liefert eine Menge`() {
        val vorlage = MengenraumKnotenVorlagen.SymmetrischeDifferenz
        assertTrue(vorlage in MengenraumKnotenVorlagen.alle)
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)
        val auswerter = assertNotNull(GesamterMathematikAuswerter.erzeugeRegister().finde(knoten.art))

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "links" to BedingterWert(BenannteMenge("A")),
                    "rechts" to BedingterWert(BenannteMenge("B")),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val menge = assertIs<SymmetrischeDifferenz>(ergebnis.ausgaben.getValue("menge").objekt)
        assertEquals("A \\triangle B", menge.zuLatex())
    }

    @Test
    fun `Renderer erhält das Latex Dreiecksymbol`() {
        val quelltext = atlasLatexQuelltext("A \\triangle B", dunklesSchema = false)
        assertContains(quelltext, "A \\triangle B")
    }
}
