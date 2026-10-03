package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TupelNullDimensionTest {
    private val auswerter = GesamterMathematikAuswerter.erzeugeRegister().finde("mathematik.tupel")!!

    @Test
    fun `Elementmodus erlaubt ein Tupel ohne Eingaenge`() {
        val vorlage = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero).copy(
            parameter = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero).parameter + ("festeEingänge" to "0"),
        )
        val knoten = konfiguriereTupel(vorlage, TUPEL_EINZEL_EINGABEN)

        assertEquals(listOf("tupel"), knoten.anschlüsse.map { it.name })
        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(knoten, emptyMap(), RechenKontext()))
        assertEquals(emptyList(), assertIs<Tupel>(ergebnis.ausgaben.getValue("tupel").objekt).elemente)
    }

    @Test
    fun `Methodenmodus erzeugt bei Dimension null ohne Methodenverbindung das 0 Tupel`() {
        val knoten = konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero), TUPEL_METHODE)

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("dimension" to BedingterWert(RationaleZahl.Null)),
                RechenKontext(),
            ),
        )

        assertEquals(emptyList(), assertIs<Tupel>(ergebnis.ausgaben.getValue("tupel").objekt).elemente)
    }
}
