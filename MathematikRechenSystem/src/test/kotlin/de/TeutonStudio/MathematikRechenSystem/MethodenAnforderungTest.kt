package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MethodenAnforderungTest {
    @Test
    fun `ergebnisanforderung unterscheidet konkrete methoden semantisch`() {
        val x = Variable("x")
        val zahlMethode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val mengenMethode = Methode(
            name = "A",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to MengenParameter("A_x")),
            zielMengen = mapOf("wert" to BenannteMenge("M", "\\mathfrak{M}")),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertNull(MethodenAnforderung.ErgebnisArt("mathematik.zahl").prüfe(zahlMethode))
        assertNull(MethodenAnforderung.ErgebnisArt("mathematik.menge").prüfe(mengenMethode))
        assertNotNull(MethodenAnforderung.ErgebnisArt("mathematik.zahl").prüfe(mengenMethode))
    }

    @Test
    fun `zahlenfunktion akzeptiert mehrere numerische Argumentraeume und effektiven Bereich`() {
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
            vorschrift = addition(x, y),
            zielMenge = FundamentalerZahlbereich.QUATERNION.alsMenge(),
            werteVorräte = mapOf(
                x.name to reellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false),
                y.name to ReelleZahlen,
            ),
            effektiverWerteVorrat = diagonale,
        )

        assertNull(MethodenAnforderung.Zahlenfunktion.prüfe(methode))
        assertTrue(methode.methodenSignatur().effektiverWerteVorrat === diagonale)
    }

    @Test
    fun `zahlenfunktion lehnt nichtnumerischen Argumentraum positionsgenau ab`() {
        val v = AllgemeinerParameter("v")
        val methode = Methode(
            name = "f",
            parameter = listOf(v),
            vorschrift = RationaleZahl.Null,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(
                v.name to Vektorraum(VektorOrientierung.Spalte, 2, ReelleZahlen),
            ),
        )

        val diagnose = assertNotNull(MethodenAnforderung.Zahlenfunktion.prüfe(methode))
        assertTrue(diagnose.contains("1. Argument 'v'"))
        assertTrue(diagnose.contains("\\mathbb H"))
    }

    @Test
    fun `zahlenfunktion lehnt nichtnumerische Zielmenge ab`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = BenannteMenge("Farben", "\\mathcal F"),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val diagnose = assertNotNull(MethodenAnforderung.Zahlenfunktion.prüfe(methode))
        assertTrue(diagnose.contains("Zielmenge"))
        assertTrue(diagnose.contains("\\mathcal F"))
    }
}
