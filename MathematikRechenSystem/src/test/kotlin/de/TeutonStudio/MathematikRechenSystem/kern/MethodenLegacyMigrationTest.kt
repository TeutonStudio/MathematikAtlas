package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MethodenLegacyMigrationTest {
    @Test
    fun `historische nullstellige LeereMenge wird als leeres kartesisches Produkt gelesen`() {
        val methode = Methode(
            name = "legacy-konstante",
            parameter = emptyList(),
            vorschrift = RationaleZahl.Eins,
            zielMenge = ReelleZahlen,
            effektiverWerteVorrat = LeereMenge,
        )

        val signatur = methode.mathematischeMethodenSignatur()

        assertEquals(Tupelraum(emptyList()), signatur.kanonischerArgumentRaum)
        assertEquals(Tupelraum(emptyList()), signatur.definitionsRaum)
        assertNotEquals(LeereMenge, signatur.definitionsRaum)
    }

    @Test
    fun `historische Einzelausgabe wird nur extern skalar aber intern als Einertupel gelesen`() {
        val methode = Methode(
            name = "legacy-identitaet",
            parameter = listOf(Variable("x")),
            vorschrift = Variable("x"),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        assertEquals(Tupelraum(listOf(ReelleZahlen)), methode.mathematischeMethodenSignatur().zielRaum)
        assertEquals(Tupel(listOf(RationaleZahl.Eins)), methode.wendeKanonischAn(Tupel(listOf(RationaleZahl.Eins))))
        assertEquals(RationaleZahl.Eins, methode.wendeAn(listOf(RationaleZahl.Eins)))
    }
}
