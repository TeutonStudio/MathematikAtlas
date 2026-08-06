package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IterationsModellTest {
    private val x = Variable("x")
    private val f = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = x,
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to ReelleZahlen),
    )

    @Test
    fun `negative Iterationsordnungen werden abgelehnt`() {
        assertFailsWith<IllegalArgumentException> { IterationsOrdnung.Konkret(-1) }
    }

    @Test
    fun `drei Iterationsarten besitzen getrennte Operator IDs und Darstellung`() {
        val ordnung = IterationsOrdnung.Konkret(2)

        assertEquals("{x}^{2}", IterierterAusdruck(x, IterationsArt.MULTIPLIKATION, ordnung).zuLatex())
        assertEquals("{f}^{\\mathrm{II}}", IterierterAusdruck(f, IterationsArt.DIFFERENTIATION, ordnung).zuLatex())
        assertEquals("{f}^{\\langle 2\\rangle}", IterierterAusdruck(f, IterationsArt.SELBSTKOMPOSITION, ordnung).zuLatex())
    }

    @Test
    fun `symbolische Ableitungsordnung behaelt runde Klammern`() {
        val n = Variable("n")
        val ausdruck = IterierterAusdruck(
            f,
            IterationsArt.DIFFERENTIATION,
            IterationsOrdnung.Symbolisch(n),
        )

        assertEquals("{f}^{(n)}", ausdruck.zuLatex())
    }

    @Test
    fun `konkrete Ableitungsordnungen werden aufrecht roemisch dargestellt`() {
        assertEquals("IV", roemischeZahlOderNull(BigInteger.valueOf(4)))
        assertEquals("IX", roemischeZahlOderNull(BigInteger.valueOf(9)))
        assertEquals("MMXXVI", roemischeZahlOderNull(BigInteger.valueOf(2026)))
        assertEquals(null, roemischeZahlOderNull(BigInteger.valueOf(4000)))
        assertEquals(
            "{f}^{(4000)}",
            IterierterAusdruck(f, IterationsArt.DIFFERENTIATION, IterationsOrdnung.Konkret(4000)).zuLatex(),
        )
    }

    @Test
    fun `Nullfaelle bleiben fachlich verschieden`() {
        val potenz = assertIs<IterationsNullfall.MultiplikativNeutral>(
            bestimmeIterationsNullfall(
                IterationsArt.MULTIPLIKATION,
                x,
                neutralesElement = RationaleZahl.Eins,
            ),
        )
        val ableitung = assertIs<IterationsNullfall.UrspruenglicherAusdruck>(
            bestimmeIterationsNullfall(IterationsArt.DIFFERENTIATION, f),
        )
        val komposition = assertIs<IterationsNullfall.Identitaet>(
            bestimmeIterationsNullfall(
                IterationsArt.SELBSTKOMPOSITION,
                f,
                werteVorrat = ReelleZahlen,
            ),
        )

        assertEquals(RationaleZahl.Eins, potenz.element)
        assertEquals(f, ableitung.ausdruck)
        assertEquals("\\operatorname{id}\\vert_{\\mathbb R}", komposition.identitaet.zuLatex())
    }

    @Test
    fun `Methodeneinschraenkung behaelt unbekannte Teilmengenbedingung sichtbar`() {
        val eingeschraenkt = MethodenEinschraenkung(f, ReelleZahlen)

        assertEquals("f\\vert_{\\mathbb R}", eingeschraenkt.zuLatex())
        assertTrue(eingeschraenkt.voraussetzungen.isNotEmpty())
        assertFailsWith<IllegalArgumentException> {
            MethodenEinschraenkung(f, ReelleZahlen, teilMengenStatus = false)
        }
    }
}
