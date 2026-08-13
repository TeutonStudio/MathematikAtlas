package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class MethodenBereichsTypinferenzTest {
    @Test
    fun `Restriktion und Bereichsanpassung bleiben signaturtragende Methoden mit stabilem Zieltyp`() {
        val x = Variable("x")
        val basis = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val restriktion = assertNotNull(restriktiereMethode(basis, ReelleZahlen).methode)
        val anpassung = assertNotNull(passeMethodenBereichAn(basis, ReelleZahlen).methode)

        assertIs<SignaturtragendeMethode>(restriktion)
        assertIs<SignaturtragendeMethode>(anpassung)
        assertEquals(basis.typAusdruck, restriktion.typAusdruck)
        assertEquals(basis.typAusdruck, anpassung.typAusdruck)
        assertEquals(ReelleZahlen, restriktion.methodenSignatur().zielMenge)
        assertEquals(ReelleZahlen, anpassung.methodenSignatur().zielMenge)
        assertEquals(ReelleZahlen, restriktion.methodenSignatur().werteVorrat)
        assertEquals(ReelleZahlen, anpassung.methodenSignatur().werteVorrat)
    }
}
