package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.BegriffsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.EindeutigeLineareLoesung
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.NachweisStatus
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.addition
import de.TeutonStudio.MathematikRechenSystem.kern.multiplikation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LineareAlgebraGrundlagenKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `neue Lina Grundlagenknoten stehen im Erstellen Dialog bereit`() {
        val arten = alleMathematikKnotenVorlagen().map { it.art }.toSet()

        assertTrue(BEGRIFF_VEKTORRAUM_KNOTEN_ART in arten)
        assertTrue(BEGRIFF_LINEARE_ABBILDUNG_KNOTEN_ART in arten)
        assertTrue(arten.count { it == "mathematik.auswerten" } == 1)
        LineareAlgebraGrundlagenKnotenVorlagen.alle.forEach { vorlage ->
            assertNotNull(register.finde(vorlage.art), "Auswerter für ${vorlage.art} fehlt.")
        }
    }

    @Test
    fun `Vektorraum Begriffsknoten gibt eine auswertbare Begriffsaussage aus`() {
        val x = Variable("x")
        val y = Variable("y")
        val a = Variable("a")
        val plus = Methode(
            "plus",
            listOf(x, y),
            addition(x, y),
            RationaleZahlen,
            mapOf(x.name to RationaleZahlen, y.name to RationaleZahlen),
        )
        val skalar = Methode(
            "skalar",
            listOf(a, x),
            multiplikation(a, x),
            RationaleZahlen,
            mapOf(a.name to RationaleZahlen, x.name to RationaleZahlen),
        )
        val knoten = LineareAlgebraGrundlagenKnotenVorlagen.Vektorraum.erzeuge(GraphPunkt.Zero)

        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "menge" to BedingterWert(RationaleZahlen),
                    "addition" to BedingterWert(plus),
                    "skalareMultiplikation" to BedingterWert(skalar),
                ),
                RechenKontext(),
            ),
        )

        val aussage = assertIs<BegriffsAussage>(ergebnis.ausgaben.getValue("aussage").objekt)
        assertEquals(NachweisStatus.Nachgewiesen, aussage.pruefung.status)
    }

    @Test
    fun `Gauss Knoten liefert Matrix und strukturiertes Protokoll`() {
        val knoten = AussagenLogikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
                listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
            ),
        )

        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("objekt" to BedingterWert(matrix)),
                RechenKontext(),
            ),
        )

        assertIs<Matrix>(ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.schritte.isNotEmpty())
        assertTrue(ergebnis.schritte.all { it.strukturOperation != null })
        assertTrue(ergebnis.schritte.all { it.strukturOperation!!.betroffeneZeilen.isNotEmpty() })
    }

    @Test
    fun `Gauss Knoten loest Ax gleich b wenn rechte Seite angeschlossen ist`() {
        val knoten = AussagenLogikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(1), RationaleZahl.von(1)),
                listOf(RationaleZahl.von(1), RationaleZahl.von(-1)),
            ),
        )

        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "objekt" to BedingterWert(matrix),
                    "rechteSeite" to BedingterWert(
                        SpaltenVektor(listOf(RationaleZahl.von(3), RationaleZahl.von(1))),
                    ),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(
            EindeutigeLineareLoesung(
                SpaltenVektor(listOf(RationaleZahl.von(2), RationaleZahl.von(1))),
            ),
            ergebnis.ausgaben.getValue("wert").objekt,
        )
        assertTrue(ergebnis.warnungen.first().startsWith("Rang(A)"))
    }
}
