package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.FormelArgument
import de.TeutonStudio.MathematikRechenSystem.kern.FormelAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.FormelTyp
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixRechner
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StrukturFormelRechnerTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `alle vier Strukturrechner sind als einheitliche Vorlagen sichtbar`() {
        val arten = alleMathematikKnotenVorlagen().map { it.art }.toSet()
        assertTrue(StrukturFormelRechnerVorlagen.alle.all { it.art in arten })
        StrukturRechnerKnotenFamilie.entries.forEach { familie ->
            assertNotNull(register.finde(familie.knotenArt))
        }
    }

    @Test
    fun `Formelmodus erzeugt typisierte Handles und bewahrt kompatible IDs`() {
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val matrixEingang = basis.anschlüsse.first { it.richtung.name == "Eingang" }
        val formel = FormelAusdruck.Operation(
            id = "transponiert",
            operatorId = "matrix.transponieren",
            argumente = listOf(
                FormelArgument(
                    "matrix",
                    0,
                    FormelAusdruck.Variable("a", matrixEingang.name, matrixEingang.name, FormelTyp.MATRIX),
                ),
            ),
            typ = FormelTyp.MATRIX,
        )

        val konfiguriert = konfiguriereStrukturRechnerFormel(
            basis,
            StrukturRechnerKnotenFamilie.MATRIX,
            formel,
        )

        assertEquals(MATRIX_FORMEL_ID, konfiguriert.parameter[RECHNER_OPERATOR_PARAMETER])
        assertEquals(matrixEingang.id, konfiguriert.anschlüsse.first { it.name == matrixEingang.name }.id)
        assertEquals(MathematikAnschlussArten.Matrix.id, konfiguriert.anschlüsse.last().art)
    }

    @Test
    fun `Matrixformel wird mit konkreten Eingaben ausgewertet`() {
        val formel = FormelAusdruck.Operation(
            id = "summe",
            operatorId = "matrix.addition",
            argumente = listOf(
                FormelArgument("a", 0, FormelAusdruck.Variable("a", "A", "A", FormelTyp.MATRIX)),
                FormelArgument("b", 1, FormelAusdruck.Variable("b", "B", "B", FormelTyp.MATRIX)),
            ),
            typ = FormelTyp.MATRIX,
        )
        val knoten = konfiguriereStrukturRechnerFormel(
            StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
            StrukturRechnerKnotenFamilie.MATRIX,
            formel,
        )
        val a = Matrix(listOf(listOf(zahl(1), zahl(2))))
        val b = Matrix(listOf(listOf(zahl(3), zahl(4))))

        val ergebnis = register.finde(MatrixRechner.KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("A" to BedingterWert(a), "B" to BedingterWert(b)),
                RechenKontext(),
            ),
        )

        assertEquals(Matrix(listOf(listOf(zahl(4), zahl(6)))), ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Vektorformel wechselt den Ausgang fuer Skalarprodukt atomar auf Zahl`() {
        val basis = StrukturFormelRechnerVorlagen.Vektor.erzeuge(GraphPunkt.Zero)
        val alterAusgang = basis.anschlüsse.last()
        val formel = FormelAusdruck.Operation(
            id = "skalarprodukt",
            operatorId = "vektor.skalarprodukt",
            argumente = listOf(
                FormelArgument("links", 0, FormelAusdruck.Variable("u", "u", "u", FormelTyp.VEKTOR)),
                FormelArgument("rechts", 1, FormelAusdruck.Variable("v", "v", "v", FormelTyp.VEKTOR)),
            ),
            typ = FormelTyp.ZAHL,
        )

        val konfiguriert = konfiguriereStrukturRechnerFormel(
            basis,
            StrukturRechnerKnotenFamilie.VEKTOR,
            formel,
        )

        val ausgang = konfiguriert.anschlüsse.last()
        assertEquals(MathematikAnschlussArten.Zahl.id, ausgang.art)
        assertNotEquals(alterAusgang.id, ausgang.id)
        assertEquals(VektorRechner.KNOTEN_ART, konfiguriert.art)
    }

    private fun zahl(wert: Long) = RationaleZahl.von(wert)
}
