package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PraedikatsWahrheitstabellenTest {
    @Test
    fun `tabellenpraedikat verwendet die kanonische wahrheitsmenge`() {
        val methode = erzeugeTabellenPrädikat(
            name = "P",
            definitionsMengen = listOf(ReelleZahlen),
            argumente = listOf(RationaleZahl.Null),
            wert = true,
        )

        assertSame(WahrheitsMenge, methode.zielMenge)
        assertEquals("P:\\mathbb{R}", kartenTabellenPrädikatSignatur(methode))
    }

    @Test
    fun `unentscheidbares tabellenergebnis bleibt unbekannt statt fehler`() {
        val methode = erzeugeTabellenPrädikat(
            name = "P",
            definitionsMengen = listOf(ReelleZahlen),
            argumente = listOf(RationaleZahl.Null),
            wert = true,
        )
        val parameter = methode.parameter.single()
        val aussage = methode.wendeKanonischAn(mapOf(parameter.name to RationaleZahl.Eins)) as Aussage

        assertNull(aussage.entscheide().wahrheitswert)
    }
}
