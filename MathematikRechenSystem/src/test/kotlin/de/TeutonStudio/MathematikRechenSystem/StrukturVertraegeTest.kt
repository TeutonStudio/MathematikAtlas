package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StrukturVertraegeTest {
    @Test
    fun `kartesisches Tupel bleibt Tupel und vereinigt Zahlbereiche`() {
        val tupel = Tupel(listOf(RationaleZahl.von(1), Division(RationaleZahl.Eins, RationaleZahl.von(2))))
        val vertrag = assertIs<StrukturPruefung.Gueltig<KartesischerTupelVertrag>>(tupel.kartesischerTupelVertrag()).wert
        assertEquals(2, vertrag.laenge)
        assertEquals(RationaleZahlen, vertrag.zahlBereich)
        assertIs<Tupel>(tupel)
    }

    @Test
    fun `heterogenes Tupel nennt erste nichtnumerische Komponente`() {
        val ergebnis = Tupel(
            listOf<MathematischesObjekt>(RationaleZahl.Eins, WahrheitsKonstante(true)),
        ).kartesischerTupelVertrag()
        val fehler = assertIs<StrukturPruefung.Ungueltig>(ergebnis)
        assertEquals("Die 2. Tupelkomponente ist keine Zahl; das Tupel ist daher nicht kartesisch.", fehler.grund)
    }

    @Test
    fun `beide Vektororientierungen besitzen dieselbe Tensorform`() {
        val zeile = ZeilenVektor(listOf(RationaleZahl.Eins, RationaleZahl.Null))
        val spalte = zeile.transponiert()
        assertEquals(listOf(2), zeile.tensorForm)
        assertEquals(listOf(2), spalte.tensorForm)
        assertEquals(1, zeile.tensorStufe)
        assertEquals(VektorOrientierung.Zeile, assertIs<StrukturPruefung.Gueltig<NumerischeKomponentenAnsicht>>(zeile.numerischeKomponentenAnsicht()).wert.orientierung)
        assertEquals(VektorOrientierung.Spalte, assertIs<StrukturPruefung.Gueltig<NumerischeKomponentenAnsicht>>(spalte.numerischeKomponentenAnsicht()).wert.orientierung)
    }

    @Test
    fun `Zahl ist nur in tensoriellem Kontext Singleton erster Stufe`() {
        val zahl = RationaleZahl.von(7)
        assertIs<StrukturPruefung.Ungueltig>(zahl.numerischeKomponentenAnsicht())
        val ansicht = assertIs<StrukturPruefung.Gueltig<TensorielleAnsicht>>(zahl.tensorielleAnsicht()).wert
        assertEquals(listOf(1), ansicht.form)
        assertEquals(1, ansicht.stufe)
        assertEquals(zahl, ansicht.komponente(listOf(0)))
    }

    @Test
    fun `Matrix und Tensor implementieren denselben Vertrag`() {
        val matrix = Matrix(listOf(listOf(RationaleZahl.Eins, RationaleZahl.Null)))
        val tensor = Tensor(listOf(1, 2, 1), listOf(RationaleZahl.Eins, RationaleZahl.Null))
        assertEquals(listOf(1, 2), matrix.tensorForm)
        assertEquals(2, matrix.tensorStufe)
        assertEquals(listOf(1, 2, 1), tensor.tensorForm)
        assertEquals(3, tensor.tensorStufe)
        assertEquals(RationaleZahl.Null, tensor.tensorKomponente(listOf(0, 1, 0)))
    }
}
