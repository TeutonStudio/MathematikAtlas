package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IterierteAussagenOperatorenTest {
    private val c = Variable("c")
    private val methode = Funktion(
        name = "P",
        parameter = listOf(c),
        ausgaben = mapOf("aussage" to Vergleich(c, VergleichsArt.Kleiner, RationaleZahl.Eins)),
        zielMengen = mapOf(
            "aussage" to EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false))),
        ),
        werteVorräte = mapOf(c.name to ReelleZahlen),
    )

    @Test
    fun `unendliche Aussageniteration bleibt symbolisch statt rekursiv zu entscheiden`() {
        val aussage = iterierteDisjunktion(methode, ReelleZahlen)
        assertIs<IterierteDisjunktion>(aussage)
        assertEquals(EntscheidungsStatus.Unbekannt, aussage.entscheide().status)
    }

    @Test
    fun `iterierte Adjunktion wertet endliche Indexmengen binär aus`() {
        val index = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val aussage = iterierteAdjunktion(methode, index)
        assertEquals(Wahrheitswert.Lüge, aussage.entscheide().wahrheitswert)
    }
}
