package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MultinomVektorKnotenTest {
    private val auswerter = GesamterMathematikAuswerter.erzeugeRegister().finde(MULTINOMVEKTOR_ART)!!

    @Test
    fun `Standard ist ein Spaltenvektor mit x und dim Eingang`() {
        val knoten = MultinomVektorKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        assertEquals(listOf("x", "dim", "wert"), knoten.anschlüsse.map { it.name })
        assertEquals(MathematikAnschlussArten.SpaltenVektor.id, knoten.anschlüsse.last().art)
    }

    @Test
    fun `dim drei und x zwei erzeugt eins zwei vier acht`() {
        val knoten = MultinomVektorKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "x" to BedingterWert(RationaleZahl.von(2)),
                    "dim" to BedingterWert(RationaleZahl.von(3)),
                ),
                rechenKontext = RechenKontext(),
            ),
        )
        assertEquals(
            listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(4), RationaleZahl.von(8)),
            assertIs<SpaltenVektor>(ergebnis.ausgaben.getValue("wert").objekt).werte,
        )
    }

    @Test
    fun `Inspectorvertrag kann Tupel und Zeile typisieren`() {
        val basis = MultinomVektorKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val tupel = konfiguriereMultinomVektor(basis, ausgabeForm = MULTINOM_AUSGABE_TUPEL)
        assertEquals(MathematikAnschlussArten.Tupel.id, tupel.anschlüsse.last().art)

        val zeile = konfiguriereMultinomVektor(
            basis,
            ausgabeForm = MULTINOM_AUSGABE_VEKTOR,
            orientierung = VEKTOR_ORIENTIERUNG_ZEILE,
        )
        assertEquals(MathematikAnschlussArten.ZeilenVektor.id, zeile.anschlüsse.last().art)
    }
}
