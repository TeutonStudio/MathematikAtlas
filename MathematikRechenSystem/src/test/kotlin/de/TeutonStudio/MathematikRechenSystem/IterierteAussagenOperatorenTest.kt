package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IterierteAussagenOperatorenTest {
    private val c = Variable("c")
    private val wahrheitsMenge = EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))
    private val methode = Funktion(
        name = "P",
        parameter = listOf(c),
        ausgaben = mapOf("aussage" to Vergleich(c, VergleichsArt.Kleiner, RationaleZahl.Eins)),
        zielMengen = mapOf("aussage" to wahrheitsMenge),
        werteVorräte = mapOf(c.name to ReelleZahlen),
    )

    @Test
    fun `unendliche Aussageniteration bleibt symbolisch statt rekursiv zu entscheiden`() {
        val aussage = iterierteDisjunktion(methode, ReelleZahlen)
        assertIs<IterierteDisjunktion>(aussage)
        assertEquals(EntscheidungsStatus.Unbekannt, aussage.entscheide().status)
    }

    @Test
    fun `iterierte Adjunktion wertet endliche Indexmengen als Parität aus`() {
        val index = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val aussage = iterierteAdjunktion(methode, index)

        assertEquals(Wahrheitswert.Wahr, aussage.entscheide().wahrheitswert)
    }

    @Test
    fun `iterierte Adjunktion verwendet stackrel bigvee limits und echte Indexmenge`() {
        val aussage = assertIs<IterierteAdjunktion>(iterierteAdjunktion(methode, BenannteMenge("A")))

        assertEquals(
            "\\mathop{\\stackrel{\\bullet}{\\bigvee}}\\limits_{c \\in A} P(c)",
            aussage.zuLatex(),
        )
    }

    @Test
    fun `Aussageniteration akzeptiert allgemeine Mengenelemente`() {
        val element = AllgemeinerParameter("element")
        val objektMethode = Funktion(
            name = "Q",
            parameter = listOf(element),
            ausgaben = mapOf("aussage" to Gleichheit(element, element)),
            zielMengen = mapOf("aussage" to wahrheitsMenge),
            werteVorräte = mapOf(element.name to BenannteMenge("A")),
        )
        val index = EndlicheMenge(setOf(Tupel(listOf(RationaleZahl.Eins)), BenannteMenge("B")))

        assertEquals(Wahrheitswert.Wahr, iterierteKonjunktion(objektMethode, index).entscheide().wahrheitswert)
    }

    @Test
    fun `leere Aussageniterationen verwenden ihre neutralen Elemente`() {
        assertEquals(Wahrheitswert.Wahr, iterierteKonjunktion(methode, LeereMenge).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, iterierteDisjunktion(methode, LeereMenge).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, iterierteAdjunktion(methode, LeereMenge).entscheide().wahrheitswert)
    }
}
