package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals

class MethodenArgumentraumTest {
    @Test
    fun `einstellige Methode behaelt Einertupelraum als Wertevorrat`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertEquals(Tupelraum(listOf(ReelleZahlen)), methode.methodenSignatur().werteVorrat)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), methode.argumentRaum())
    }

    @Test
    fun `effektiver Wertevorrat bleibt kanonischer Argumentraum`() {
        val x = Variable("x")
        val y = Variable("y")
        val diagonale = DefinierteMenge(
            variablen = listOf(
                GebundeneMengenVariable(x, ReelleZahlen),
                GebundeneMengenVariable(y, ReelleZahlen),
            ),
            bedingung = Gleichheit(x, y),
        )
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
            effektiverWerteVorrat = diagonale,
        )

        assertEquals(diagonale, methode.argumentRaum())
    }
}
