package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals

class MethodenArgumentraumTest {
    @Test
    fun `einstellige Methode behaelt Einertupelraum als Definitionsraum`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertEquals(
            Tupelraum(listOf(ReelleZahlen)),
            methode.mathematischeMethodenSignatur().definitionsRaum,
        )
        assertEquals(Tupelraum(listOf(ReelleZahlen)), methode.argumentRaum())
    }

    @Test
    fun `effektiver Definitionsraum bleibt echte Teilmenge des kanonischen Argumentraums`() {
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
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
            effektiverWerteVorrat = diagonale,
        )

        val signatur = methode.mathematischeMethodenSignatur()
        assertEquals(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), signatur.kanonischerArgumentRaum)
        assertEquals(diagonale, signatur.definitionsRaum)
        assertEquals(diagonale, methode.argumentRaum())
    }
}
