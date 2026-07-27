package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FallunterscheidungTest {
    private val auswerter = StandardMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Vorlage ordnet Aussage zwischen Wahr- und Lüge-Eingang an`() {
        val knoten = MathematikKnotenVorlagen.Fall.erzeuge(GraphPunkt.Zero)
        val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        val ausgänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals(listOf("wahr", "aussage", "lüge"), eingänge.map { it.name })
        assertEquals(listOf(MathematikAnschlussArten.Objekt.id, MathematikAnschlussArten.Aussage.id, MathematikAnschlussArten.Objekt.id), eingänge.map { it.art })
        assertEquals(listOf("wert"), ausgänge.map { it.name })
        assertEquals(MathematikAnschlussArten.Objekt.id, ausgänge.single().art)
    }

    @Test
    fun `wahre Aussage wählt ersten Eingang`() {
        val ergebnis = werteAus(
  wahr = BedingterWert(RationaleZahl.von(7)),
  aussage = BedingterWert(WahrheitsKonstante(true)),
  lüge = BedingterWert(EndlicheMenge(setOf(RationaleZahl.Eins))),
        )

        assertEquals(RationaleZahl.von(7), ergebnis.objekt)
    }

    @Test
    fun `falsche Aussage wählt zweiten Eingang`() {
        val falschWert = EndlicheMenge(setOf(RationaleZahl.von(2), RationaleZahl.von(3)))
        val ergebnis = werteAus(
  wahr = BedingterWert(RationaleZahl.von(7)),
  aussage = BedingterWert(WahrheitsKonstante(false)),
  lüge = BedingterWert(falschWert),
        )

        assertEquals(falschWert, ergebnis.objekt)
    }

    @Test
    fun `unentscheidbare Aussage bricht verständlich ab`() {
        assertFailsWith<IllegalStateException> {
  werteAus(
      wahr = BedingterWert(RationaleZahl.Eins),
      aussage = BedingterWert(Gleichheit(Variable("x"), RationaleZahl.Eins)),
      lüge = BedingterWert(RationaleZahl.Null),
  )
        }
    }

    private fun werteAus(wahr: BedingterWert, aussage: BedingterWert, lüge: BedingterWert): BedingterWert {
        val knoten = MathematikKnotenVorlagen.Fall.erzeuge(GraphPunkt.Zero)
        return auswerter.finde(knoten.art)!!.auswerten(
  KnotenAuswertungsKontext(knoten, mapOf("wahr" to wahr, "aussage" to aussage, "lüge" to lüge), RechenKontext()),
        ).ausgaben.getValue("wert")
    }
}
