package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MethodenWertegrundraumTest {
    @Test
    fun `homogener neunstelliger Wertevorrat liefert gemeinsamen Grundraum`() {
        val parameter = (1..9).map { index -> Variable("x$index") }
        val methode = Methode(
            name = "f",
            parameter = parameter,
            ausgaben = mapOf("wert" to parameter.first()),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = parameter.associate { it.name to ReelleZahlen },
        )

        val ergebnis = MethodenWertegrundraumAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = KnotenDaten(
                    art = METHODEN_WERTEGRUNDRAUM_ART,
                    name = "Methoden-Wertegrundraum",
                ),
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(ReelleZahlen, ergebnis.ausgaben.getValue("menge").objekt)
    }

    @Test
    fun `heterogene Methodenargumente besitzen keinen gemeinsamen Wertegrundraum`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to RationaleZahl.Null),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(
                x.name to ReelleZahlen,
                y.name to KomplexeZahlen,
            ),
        )

        assertFailsWith<IllegalStateException> {
            MethodenWertegrundraumAuswerter.auswerten(
                KnotenAuswertungsKontext(
                    knoten = KnotenDaten(
                        art = METHODEN_WERTEGRUNDRAUM_ART,
                        name = "Methoden-Wertegrundraum",
                    ),
                    eingänge = mapOf("methode" to BedingterWert(methode)),
                    rechenKontext = RechenKontext(),
                ),
            )
        }
    }
}
