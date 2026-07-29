package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ReelleMethodenSummeTest {
    private val auswerter = GesamterMathematikAuswerter.erzeugeRegister().finde("mathematik.reelleMethodenSumme")!!
    private val x = Variable("x")
    private val quadrat = Funktion(
        name = "f",
        parameter = listOf(x),
        ausgaben = mapOf("wert" to Potenz(x, RationaleZahl.von(2))),
        zielMengen = mapOf("wert" to ReelleZahlen),
        werteVorräte = mapOf("x" to ReelleZahlen),
    )

    @Test fun `Unter- und Obersumme von x Quadrat werden berechnet`() {
        val unten = werteAus("untersumme")
        val oben = werteAus("obersumme")

        assertEquals(RationaleZahl.von(7, 32), unten.ausgaben.getValue("wert").objekt)
        assertEquals(RationaleZahl.von(15, 32), oben.ausgaben.getValue("wert").objekt)
        assertTrue(unten.ausgaben.getValue("wert").latexDarstellung!!.contains("underline"))
        assertTrue(oben.ausgaben.getValue("wert").latexDarstellung!!.contains("overline"))
        assertEquals(4, (unten.ausgaben.getValue("visualisierung").objekt as Tupel).elemente.size)
    }

    @Test fun `Intervallmodus liest Grenzen aus dem Intervalleingang`() {
        val knoten = ErweiterteMathematikKnotenVorlagen.ReelleMethodenSumme.erzeuge(de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt.Zero).copy(
            parameter = mapOf("summenArt" to "untersumme", "bereichsArt" to "intervall"),
        )
        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "methode" to BedingterWert(quadrat),
                "partitionen" to BedingterWert(RationaleZahl.von(4)),
                "intervall" to BedingterWert(ReellesIntervall(RationaleZahl.Null, RationaleZahl.Eins)),
            ),
            RechenKontext(),
        ))

        assertEquals(RationaleZahl.von(7, 32), ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test fun `Null Partitionen werden abgelehnt`() {
        val knoten = ErweiterteMathematikKnotenVorlagen.ReelleMethodenSumme.erzeuge(de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt.Zero)
        val fehler = assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(quadrat),
                    "partitionen" to BedingterWert(RationaleZahl.Null),
                    "minimum" to BedingterWert(RationaleZahl.Null),
                    "maximum" to BedingterWert(RationaleZahl.Eins),
                ),
                RechenKontext(),
            ))
        }
        assertTrue(fehler.message!!.contains("0"))
    }

    private fun werteAus(art: String): KnotenAuswertungsErgebnis {
        val knoten = ErweiterteMathematikKnotenVorlagen.ReelleMethodenSumme.erzeuge(de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt.Zero).copy(
            parameter = mapOf("summenArt" to art, "bereichsArt" to "grenzen"),
        )
        return auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "methode" to BedingterWert(quadrat),
                "partitionen" to BedingterWert(RationaleZahl.von(4)),
                "minimum" to BedingterWert(RationaleZahl.Null),
                "maximum" to BedingterWert(RationaleZahl.Eins),
            ),
            RechenKontext(),
        ))
    }
}
