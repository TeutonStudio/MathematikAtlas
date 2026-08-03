package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.addition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class SpurKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Spurvorlage besitzt Matrixeingang und Zahlausgang`() {
        val vorlage = alleMathematikKnotenVorlagen().single { it.art == SPUR_ART }
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)

        assertEquals(
            MathematikAnschlussArten.Matrix.id,
            knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.art,
        )
        assertEquals(
            MathematikAnschlussArten.Zahl.id,
            knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.art,
        )
        assertNotNull(register.finde(SPUR_ART))
    }

    @Test
    fun `Spurknoten wertet quadratische Matrix aus und erhaelt Annahmen`() {
        val knoten = SpurKnotenVorlagen.Spur.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(
            listOf(
                listOf(zahl(1), zahl(2)),
                listOf(zahl(3), zahl(4)),
            ),
        )
        val annahme = de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit(Variable("x"), zahl(1))
        val ergebnis = register.finde(SPUR_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("matrix" to BedingterWert(matrix, setOf(annahme))),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(zahl(5), ergebnis.ausgaben.getValue("spur").objekt)
        assertEquals(setOf(annahme), ergebnis.ausgaben.getValue("spur").annahmen)
    }

    @Test
    fun `Tupelvariante der iterierten Summe faltet kartesisches Zahlentupel`() {
        val knoten = SpurKnotenVorlagen.IterierteSummeTupel.erzeuge(GraphPunkt.Zero)
        val a = Variable("a")
        val tupel = Tupel(listOf(a, zahl(2), zahl(3)))
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("tupel" to BedingterWert(tupel)),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(addition(a, zahl(5)), ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Bestehende Varianten der iterierten Summe bleiben intern registriert`() {
        val vorlagen = ZahlenRechnerKnotenVorlagen.alle.filter {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.ITERIERTE_SUMME.stabileId
        }

        assertEquals(2, vorlagen.size)
        assertNotNull(vorlagen.singleOrNull { vorlage ->
            vorlage.anschlüsse.any { it.name == "methode" } && vorlage.anschlüsse.any { it.name == "indexmenge" }
        })
        assertNotNull(vorlagen.singleOrNull { vorlage -> vorlage.anschlüsse.any { it.name == "tupel" } })
        assertEquals(1, alleMathematikKnotenVorlagen().count { it.art == ZAHLENRECHNER_ART })
    }

    @Test
    fun `Rechteckige Matrix wird auch am Knoten abgewiesen`() {
        val knoten = SpurKnotenVorlagen.Spur.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(
            listOf(
                listOf(zahl(1), zahl(2), zahl(3)),
                listOf(zahl(4), zahl(5), zahl(6)),
            ),
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            register.finde(SPUR_ART)!!.auswerten(
                KnotenAuswertungsKontext(
                    knoten = knoten,
                    eingänge = mapOf("matrix" to BedingterWert(matrix)),
                    rechenKontext = RechenKontext(),
                ),
            )
        }

        assertEquals(
            "Die Spur ist nur für quadratische Matrizen definiert; erhalten wurde 2×3.",
            fehler.message,
        )
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
