package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import kotlin.test.Test
import kotlin.test.assertEquals

class MethodenArgumentanzahlTest {
    @Test
    fun `Argumentanzahl ist auch ohne Parameter Wertevorrat auslesbar`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
        )

        val ergebnis = MethodenArgumentanzahlAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = KnotenDaten(art = METHODEN_ARGUMENTANZAHL_ART, name = "Argumentanzahl"),
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(1), ergebnis.ausgaben.getValue("anzahl").objekt)
    }
}
