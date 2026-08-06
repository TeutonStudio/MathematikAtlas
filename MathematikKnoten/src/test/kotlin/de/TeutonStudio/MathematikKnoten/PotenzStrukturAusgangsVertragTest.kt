package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PotenzStrukturAusgangsVertragTest {
    @Test
    fun `Multiplikation mit mehreren oeffentlichen Ausgaengen wird abgelehnt`() {
        val a = Variable("a")
        val b = Variable("b")
        val methode = Methode(
            name = "mehrfach",
            parameter = listOf(a, b),
            vorschrift = Tupel(listOf(a, b)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf("a" to ReelleZahlen, "b" to ReelleZahlen),
            ausgabeNamen = listOf("links", "rechts"),
        )
        val knoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val register = GesamterMathematikAuswerter.erzeugeRegister()
        val ergebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "traeger" to BedingterWert(ReelleZahlen),
                    "multiplikation" to BedingterWert(methode),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("genau einen öffentlichen Ausgang"))
    }
}
