package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StrukturFormelEditorTest {
    @Test
    fun `Codec behaelt typisierten Matrixausdruck und Variablen`() {
        val formel = FormelAusdruck.Operation(
            id = "produkt",
            operatorId = "matrix.produkt",
            argumente = listOf(
                FormelArgument("links", 0, FormelAusdruck.Variable("a", "A", "A", FormelTyp.MATRIX)),
                FormelArgument("rechts", 1, FormelAusdruck.Variable("b", "B", "B", FormelTyp.MATRIX)),
            ),
            typ = FormelTyp.MATRIX,
        )

        val geladen = StrukturFormelCodec.dekodieren(StrukturFormelCodec.kodieren(formel))

        assertEquals(formel, geladen)
        assertEquals(
            listOf(StrukturFormelVariable("A", FormelTyp.MATRIX), StrukturFormelVariable("B", FormelTyp.MATRIX)),
            strukturFormelVariablen(geladen),
        )
        assertEquals("A \\, B", StrukturFormelDarstellung.latex(geladen))
    }

    @Test
    fun `Editor setzt nur typkompatible Operatoren in Platzhalter`() {
        val editor = StrukturFormelEditorZustand(
            strukturFormelPlatzhalter("zahl", FormelTyp.ZAHL, "Skalar"),
        )
        val matrixTaste = StrukturFormelTastatur.fuer(StrukturFormelFamilie.MATRIX)
            .first { it.operatorId == "matrix.produkt" }
        val zahlTaste = StrukturFormelTastatur.fuer(StrukturFormelFamilie.MATRIX)
            .first { it.operatorId == "zahl.addition" }

        assertFalse(editor.kannDruecken(matrixTaste))
        assertFalse(editor.druecke(matrixTaste))
        assertTrue(editor.druecke(zahlTaste))
        assertIs<FormelAusdruck.Operation>(editor.wurzel)
        assertEquals(2, editor.offenePlatzhalter.size)
    }

    @Test
    fun `Matrix CAS kennt gemischte Ergebnis und Argumenttypen`() {
        val tasten = StrukturFormelTastatur.fuer(StrukturFormelFamilie.MATRIX)
        val skalar = tasten.first { it.operatorId == "matrix.skalarmultiplikation" }
        val matrixVektor = tasten.first { it.operatorId == "matrix.vektorProdukt" }

        assertEquals(FormelTyp.MATRIX, skalar.ergebnisTyp)
        assertEquals(listOf(FormelTyp.ZAHL, FormelTyp.MATRIX), skalar.argumente.map { it.typ })
        assertEquals(FormelTyp.VEKTOR, matrixVektor.ergebnisTyp)
        assertEquals(listOf(FormelTyp.MATRIX, FormelTyp.VEKTOR), matrixVektor.argumente.map { it.typ })
    }
}
