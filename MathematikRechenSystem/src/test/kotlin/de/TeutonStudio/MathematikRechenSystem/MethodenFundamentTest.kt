package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MethodenFundamentTest {
    @Test
    fun `wertevorrat wird als geordneter tupelraum abgeleitet`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to addition(listOf(x, y))),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = linkedMapOf(x.name to ReelleZahlen, y.name to GanzeZahlen),
        )

        val signatur = methode.methodenSignatur()
        assertEquals(listOf("x", "y"), signatur.argumente.map { it.parameter.name })
        assertEquals(Tupelraum(listOf(ReelleZahlen, GanzeZahlen)), signatur.werteVorrat)
        assertEquals(ReelleZahlen, signatur.zielMenge)
    }

    @Test
    fun `nullstellige methode verwendet leere menge`() {
        val methode = Methode(
            name = "c",
            parameter = emptyList(),
            ausgaben = mapOf("wert" to RationaleZahl.Eins),
            zielMengen = mapOf("wert" to ReelleZahlen),
        )

        assertEquals(LeereMenge, methode.methodenSignatur().werteVorrat)
        assertEquals(RationaleZahl.Eins, methode.wendeKanonischAn(emptyMap()))
    }

    @Test
    fun `historische mehrfachausgaben sind ein ergebnistupel`() {
        val methode = Methode(
            name = "paar",
            parameter = emptyList(),
            ausgaben = linkedMapOf("links" to RationaleZahl.Null, "rechts" to RationaleZahl.Eins),
            zielMengen = linkedMapOf("links" to ReelleZahlen, "rechts" to ReelleZahlen),
        )

        assertIs<Tupel>(methode.vorschrift)
        assertEquals(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), methode.zielMenge)
        assertEquals(Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins)), methode.wendeKanonischAn(emptyMap()))
    }

    @Test
    fun `aliase werden nur aus der methodensemantik berechnet`() {
        val x = Variable("x")
        val funktion = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val menge = MengenParameter("A")
        val abbildung = Methode(
            name = "P",
            parameter = listOf(menge),
            ausgaben = mapOf("wert" to Potenzmenge(menge)),
            zielMengen = mapOf("wert" to BenannteMenge("mengenfamilien", "\\mathfrak{M}")),
            werteVorräte = mapOf(menge.name to BenannteMenge("mengen", "\\mathfrak{M}")),
        )
        val aussage = AussagenParameter("A")
        val prädikat = Methode(
            name = "Q",
            parameter = listOf(aussage),
            ausgaben = mapOf("aussage" to aussage),
            zielMengen = mapOf("aussage" to WahrheitsMenge),
            werteVorräte = mapOf(aussage.name to WahrheitsMenge),
        )

        assertTrue(MethodenAlias.Funktion in funktion.aliase())
        assertFalse(MethodenAlias.Prädikat in funktion.aliase())
        assertTrue(MethodenAlias.Abbildung in abbildung.aliase())
        assertTrue(MethodenAlias.Prädikat in prädikat.aliase())
        assertEquals("Methode · Prädikat", prädikat.aliasAnzeige())
    }
    @Test
    fun `kanonische methode besitzt genau eine vorschrift und zielmenge`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertEquals(addition(RationaleZahl.von(2), RationaleZahl.Eins), methode.wendeAn(listOf(RationaleZahl.von(2))))
        assertEquals(ReelleZahlen, methode.zielMenge)
        assertEquals(listOf("wert"), methode.ausgabeNamen)
    }

    @Test
    fun `mehrere kartenausgaenge bleiben ein geordnetes ergebnistupel`() {
        val methode = Methode(
            name = "paar",
            parameter = emptyList(),
            vorschrift = Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins)),
            zielMenge = Tupelraum(listOf(GanzeZahlen, GanzeZahlen)),
            ausgabeNamen = listOf("links", "rechts"),
        )

        assertEquals(Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins)), methode.wendeAn(emptyList()))
        assertEquals(RationaleZahl.Null, methode.vorschriftFür("links"))
        assertEquals(GanzeZahlen, methode.zielMengeFür("rechts"))
    }

}
