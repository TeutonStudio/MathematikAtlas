package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.Potenz
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuswertenUndStandardwertDarstellungTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val kartenAuswerter = KartenAuswerter(register)

    @Test
    fun `Auswerten besitzt ausschließlich Zahlterm Ein und Ausgang`() {
        val vorlage = alleMathematikKnotenVorlagen().single { it.art == "mathematik.auswerten" }
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)
        val eingang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals("term", eingang.name)
        assertEquals("term", ausgang.name)
        assertEquals(MathematikAnschlussArten.Zahl.id, eingang.art)
        assertEquals(MathematikAnschlussArten.Zahl.id, ausgang.art)
        assertTrue(AussagenLogikKnotenVorlagen.alle.none { it.art == "mathematik.auswerten" })
    }

    @Test
    fun `Auswerten liefert seinen vereinfachten Zahlterm am Termausgang`() {
        val knoten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val ergebnis = requireNotNull(register.finde(knoten.art)).auswerten(
  KnotenAuswertungsKontext(
      knoten = knoten,
      eingänge = mapOf(
          "term" to BedingterWert(Potenz(RationaleZahl.von(2), RationaleZahl.von(-1))),
      ),
      rechenKontext = RechenKontext(),
  ),
        )

        assertEquals(setOf("term"), ergebnis.ausgaben.keys)
    }

    @Test
    fun `Auswerten lehnt Gleichheiten und andere Aussagen ab`() {
        val knoten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val fehler = assertFailsWith<IllegalStateException> {
  requireNotNull(register.finde(knoten.art)).auswerten(
      KnotenAuswertungsKontext(
          knoten = knoten,
          eingänge = mapOf(
              "term" to BedingterWert(Gleichheit(RationaleZahl.von(1), RationaleZahl.von(1))),
          ),
          rechenKontext = RechenKontext(),
      ),
  )
        }

        assertEquals("Auswerten benötigt einen Zahlterm.", fehler.message)
    }

    @Test
    fun `historische Anschlussnamen des Auswerten Knotens bleiben lesbar`() {
        val basis = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val historisch = basis.copy(
  anschlüsse = basis.anschlüsse.map { anschluss ->
      when (anschluss.richtung) {
          AnschlussRichtung.Eingang -> anschluss.copy(name = "objekt", art = MathematikAnschlussArten.Objekt.id)
          AnschlussRichtung.Ausgang -> anschluss.copy(name = "wert", art = MathematikAnschlussArten.Objekt.id)
      }
  },
        )
        val ergebnis = requireNotNull(register.finde(historisch.art)).auswerten(
  KnotenAuswertungsKontext(
      knoten = historisch,
      eingänge = mapOf("objekt" to BedingterWert(RationaleZahl.von(2))),
      rechenKontext = RechenKontext(),
  ),
        )

        assertEquals(setOf("wert"), ergebnis.ausgaben.keys)
    }

    @Test
    fun `Standardwerte erzeugen eine operative LaTeX Darstellung`() {
        val potenz = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero).copy(
  parameter = mapOf(
      "standardwert.basis" to "2",
      "standardwert.exponent" to "-1",
  ),
        )
        val ergebnis = kartenAuswerter.auswerten(
  KartenDaten(name = "Standardwert-LaTeX", knoten = listOf(potenz)),
        )
        val wert = ergebnis.knoten.getValue(potenz.id).ausgaben.getValue("wert")

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals("\\left(2\\right)^{-1}", wert.anzeigeLatex())
    }
}
