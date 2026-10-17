package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.BenannteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.SymmetrischeDifferenz
import de.TeutonStudio.MathematikRechenSystem.kern.vereinige
import de.TeutonStudio.MathematikRechenSystem.kern.mengenDifferenz
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SymmetrischeDifferenzKnotenTest {
    @Test
    fun `historische Vorlage bleibt auswertbar aber ist nicht mehr separat erzeugbar`() {
        val vorlage = MengenraumKnotenVorlagen.SymmetrischeDifferenz
        assertFalse(vorlage in MengenraumKnotenVorlagen.alle)
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)
        val auswerter = assertNotNull(GesamterMathematikAuswerter.erzeugeRegister().findeNachMigration(knoten.art))

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

        val a=BenannteMenge("A");val b=BenannteMenge("B")
        assertEquals(vereinige(listOf(mengenDifferenz(a,b),mengenDifferenz(b,a))),ergebnis.ausgaben.getValue("menge").objekt)
    }

    @Test
    fun `Renderer erhält das Latex Dreiecksymbol`() {
        val quelltext = atlasLatexQuelltext("A \\triangle B", dunklesSchema = false)
        assertContains(quelltext, "A \\triangle B")
    }
}
