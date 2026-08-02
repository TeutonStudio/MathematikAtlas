package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KompositionTest {
    private val x = Variable("x")
    private val y = Variable("y")

    private fun methode(
        name: String,
        parameter: List<FunktionsParameter>,
        ausdruck: MathematischesObjekt,
        definitionsmenge: MengenAusdruck = ReelleZahlen,
        zielmenge: MengenAusdruck = ReelleZahlen,
    ) = Funktion(
        name = name,
        parameter = parameter,
        ausgaben = mapOf("wert" to ausdruck),
        zielMengen = mapOf("wert" to zielmenge),
        werteVorräte = parameter.associate { it.name to definitionsmenge },
    )

    @Test
    fun `komponiert drei Methoden in sichtbarer Reihenfolge`() {
        val f = methode("f", listOf(x), addition(listOf(x, RationaleZahl.von(1))))
        val g = methode("g", listOf(x), multiplikation(listOf(x, RationaleZahl.von(2))))
        val h = methode("h", listOf(x), Potenz(x, RationaleZahl.von(2)))

        val ergebnis = komponiere(listOf(f, g, h))

        assertEquals("f\\circg\\circh", ergebnis.name)
        assertEquals(listOf(x), ergebnis.parameter)
        assertEquals(ReelleZahlen, ergebnis.einzigeZielMenge)
        assertEquals(
            addition(listOf(multiplikation(listOf(Potenz(x, RationaleZahl.von(2)), RationaleZahl.von(2))), RationaleZahl.von(1))),
            ergebnis.einzigeAusgabe().second,
        )
    }

    @Test
    fun `innerste Methode darf mehrere Parameter besitzen`() {
        val außen = methode("f", listOf(x), addition(listOf(x, RationaleZahl.von(1))))
        val innen = methode("g", listOf(x, y), addition(listOf(x, y)))

        val ergebnis = komponiere(listOf(außen, innen))

        assertEquals(listOf(x, y), ergebnis.parameter)
        assertEquals(setOf("x", "y"), ergebnis.werteVorräte.keys)
    }

    @Test
    fun `meldet die konkrete inkompatible Übergangsstelle`() {
        val außen = methode("f", listOf(x), x, definitionsmenge = ReelleZahlen)
        val innen = methode("g", listOf(x), x, zielmenge = KomplexeZahlen)

        val prüfung = prüfeKompositionsKette(listOf(außen, innen))

        assertTrue(!prüfung.istGültig)
        assertEquals(1, prüfung.fehler.single().äußerePosition)
        assertEquals(2, prüfung.fehler.single().innerePosition)
        assertTrue(prüfung.fehler.single().grund.contains("passt nicht"))
        assertFailsWith<IllegalArgumentException> { komponiere(listOf(außen, innen)) }
    }
}
