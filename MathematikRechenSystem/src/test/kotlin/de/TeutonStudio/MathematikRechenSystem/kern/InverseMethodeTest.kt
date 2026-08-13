package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InverseMethodeTest {
    private val null = RationaleZahl.Null
    private val eins = RationaleZahl.Eins
    private val zwei = RationaleZahl.von(2)
    private val bin = EndlicheMenge(setOf(null, eins))

    @Test
    fun `endliche bijektive Abbildung erhaelt exakt vertauschte Signatur und inverse Auswertung`() {
        val x = Variable("x")
        val f = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = bin,
            werteVorräte = mapOf("x" to bin),
        )

        val inverse = assertIs<UmkehrMethodenErgebnis.Gueltig>(umkehrMethode(f)).methode

        assertEquals(bin, inverse.mathematischeSignatur.argumente.single().definitionsMenge)
        assertEquals(bin, inverse.mathematischeSignatur.ergebnisse.single().zielMenge)
        assertEquals(eins, inverse.wendeMathematischAn(mapOf("wert" to eins)))
        assertEquals("f^{\\langle-1\\rangle}", inverse.zuLatex())
    }

    @Test
    fun `Bijektion zwischen Tupelraeumen wird nicht auf skalare Funktionen reduziert`() {
        val x = Variable("x")
        val y = Variable("y")
        val f = MathematischeMethode(
            name = "swap",
            parameter = listOf(x, y),
            ausgaben = linkedMapOf("u" to y, "v" to x),
            zielMengen = linkedMapOf("u" to bin, "v" to bin),
            werteVorräte = linkedMapOf("x" to bin, "y" to bin),
        )

        val inverse = assertIs<UmkehrMethodenErgebnis.Gueltig>(umkehrMethode(f)).methode
        val wert = inverse.wendeMathematischAn(mapOf("u" to null, "v" to eins))

        assertEquals(Tupel(listOf(eins, null)), wert)
        assertEquals(2, inverse.mathematischeSignatur.argumente.size)
        assertEquals(2, inverse.mathematischeSignatur.ergebnisse.size)
    }

    @Test
    fun `nicht injektive Methode wird nachweislich abgelehnt`() {
        val x = Variable("x")
        val f = Methode(
            name = "c",
            parameter = listOf(x),
            vorschrift = null,
            zielMenge = bin,
            werteVorräte = mapOf("x" to bin),
        )

        val ergebnis = assertIs<UmkehrMethodenErgebnis.Ungueltig>(umkehrMethode(f))
        assertEquals(BijektivitaetsStatus.NICHT_INJEKTIV, ergebnis.status)
    }

    @Test
    fun `nicht surjektive Methode wird auf deklarierter Zielmenge abgelehnt`() {
        val x = Variable("x")
        val nurNull = EndlicheMenge(setOf(null))
        val f = Methode(
            name = "i",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = bin,
            werteVorräte = mapOf("x" to nurNull),
        )

        val ergebnis = assertIs<UmkehrMethodenErgebnis.Ungueltig>(umkehrMethode(f))
        assertEquals(BijektivitaetsStatus.NICHT_SURJEKTIV, ergebnis.status)
    }

    @Test
    fun `unendliche oder symbolische Raeume behalten Bijektivitaet als Voraussetzung`() {
        val x = Variable("x")
        val f = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = addition(listOf(x, eins)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        val bedingt = assertIs<UmkehrMethodenErgebnis.Bedingt>(umkehrMethode(f)).methode

        assertEquals("f^{\\langle-1\\rangle}", bedingt.zuLatex())
        assertIs<BijektivitaetsVoraussetzung>(bedingt.voraussetzungen.single())
    }
}
