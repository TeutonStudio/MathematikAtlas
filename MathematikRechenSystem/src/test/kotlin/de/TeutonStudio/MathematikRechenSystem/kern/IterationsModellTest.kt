package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
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
    fun `negative und nichtganze Iterationsordnungen werden abgelehnt`() {
        assertFailsWith<IllegalArgumentException> { IterationsOrdnung.Konkret(-1) }

        val negativ = pruefeIterationsOrdnung(RationaleZahl.von(-1))
        val nichtGanz = pruefeIterationsOrdnung(RationaleZahl.von(1, 2))
        val typfalsch = pruefeIterationsOrdnung(Tupel(listOf(RationaleZahl.Eins)))

        assertIs<IterationsOrdnungsPruefung.Ungueltig>(negativ)
        assertIs<IterationsOrdnungsPruefung.Ungueltig>(nichtGanz)
        assertIs<IterationsOrdnungsPruefung.Ungueltig>(typfalsch)
    }

    @Test
    fun `konkrete und symbolische Ordnung bleiben auch fuer grosse Werte strukturiert`() {
        val gross = BigInteger("100000000000000000000000000000000000")
        val konkret = assertIs<IterationsOrdnungsPruefung.Gueltig>(
            pruefeIterationsOrdnung(RationaleZahl(gross, BigInteger.ONE)),
        ).ordnung
        val symbolisch = assertIs<IterationsOrdnungsPruefung.Gueltig>(
            pruefeIterationsOrdnung(Variable("n")),
        ).ordnung

        assertEquals(gross, assertIs<IterationsOrdnung.Konkret>(konkret).wert)
        assertTrue(assertIs<IterationsOrdnung.Symbolisch>(symbolisch).annahmen.single().zuLatex().contains("mathbb N_0"))
    }

    @Test
    fun `drei Iterationsarten besitzen getrennte Operator IDs und Darstellung`() {
        val ordnung = IterationsOrdnung.Konkret(2)
        val multiplikation = IterierterAusdruck(x, IterationsArt.MULTIPLIKATION, ordnung)
        val differentiation = IterierterAusdruck(f, IterationsArt.DIFFERENTIATION, ordnung)
        val komposition = IterierterAusdruck(f, IterationsArt.SELBSTKOMPOSITION, ordnung)

        assertEquals("{x}^{2}", multiplikation.zuLatex())
        assertEquals("{f}^{\\mathrm{II}}", differentiation.zuLatex())
        assertEquals("{f}^{\\langle2\\rangle}", komposition.zuLatex())
        assertEquals(
            setOf("iteration.multiplikation", "iteration.differentiation", "iteration.selbstkomposition"),
            setOf(multiplikation.operatorId, differentiation.operatorId, komposition.operatorId),
        )
    }

    @Test
    fun `symbolische Ableitungsordnung behaelt runde Klammern`() {
        val n = Variable("n")
        val annahme = UnentscheidbareAussage("n\\in\\mathbb N_0", "Iterationsordnung")
        val ausdruck = IterierterAusdruck(
            f,
            IterationsArt.DIFFERENTIATION,
            IterationsOrdnung.Symbolisch(n, setOf(annahme)),
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
        assertFalse(komposition.identitaet.zuLatex().contains("operatorname{id}_"))
        assertEquals(ReelleZahlen, komposition.identitaet.alsMethode.zielMenge)
        assertEquals(ReelleZahlen, komposition.identitaet.alsMethode.werteVorräte.getValue("x"))
    }

    @Test
    fun `Methodeneinschraenkung setzt Wertevorrat exakt und behaelt Zielmenge`() {
        val intervall = ReellesIntervall(
            RationaleZahl.Null,
            false,
            RationaleZahl.Eins,
            false,
        )
        val eingeschraenkt = schraenkeMethodeEin(f, intervall)
        val methode = eingeschraenkt.eingeschraenkteMethode

        assertEquals("f\\vert_{${intervall.zuLatex()}}", eingeschraenkt.zuLatex())
        assertEquals(intervall, methode.werteVorräte.getValue("x"))
        assertEquals(f.zielMenge, methode.zielMenge)
        assertTrue(eingeschraenkt.voraussetzungen.single() is TeilmengenBeziehung)
    }

    @Test
    fun `nachgewiesenes engeres Bild aendert deklarierte Zielmenge nicht`() {
        val punkt = EndlicheMenge(setOf(RationaleZahl.Null))
        val eingeschraenkt = schraenkeMethodeEin(
            f,
            punkt,
            nachgewiesenesBild = punkt,
        )

        assertEquals(punkt, eingeschraenkt.nachgewiesenesBild)
        assertEquals(ReelleZahlen, eingeschraenkt.eingeschraenkteMethode.zielMenge)
    }

    @Test
    fun `falsche Teilmengenbeziehung wird abgelehnt`() {
        val endlicherBereich = EndlicheMenge(setOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val methode = f.copy(werteVorräte = mapOf("x" to endlicherBereich))
        val fremd = EndlicheMenge(setOf(RationaleZahl.von(3)))

        assertFailsWith<IllegalArgumentException> {
            schraenkeMethodeEin(methode, fremd)
        }
    }

    @Test
    fun `verschachtelte Einschraenkung normalisiert auf urspruengliche Methode und engere Menge`() {
        val intervall = ReellesIntervall(
            RationaleZahl.Null,
            false,
            RationaleZahl.von(2),
            false,
        )
        val erste = schraenkeMethodeEin(f, intervall)
        val punkt = EndlicheMenge(setOf(RationaleZahl.Eins))
        val zweite = schraenkeMethodeEin(erste, punkt)

        assertEquals(f, zweite.ursprungsMethode)
        assertEquals(punkt, zweite.menge)
        assertEquals("f\\vert_{${punkt.zuLatex()}}", zweite.zuLatex())
        assertEquals(punkt, zweite.eingeschraenkteMethode.werteVorräte.getValue("x"))
    }

    @Test
    fun `Restriktion bleibt auf einstellige Methoden begrenzt`() {
        val y = Variable("y")
        val mehrstellig = Methode(
            name = "g",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )

        assertFailsWith<IllegalArgumentException> {
            schraenkeMethodeEin(mehrstellig, ReelleZahlen)
        }
    }

    @Test
    fun `Snapshots persistieren Operatorart und Ordnung statt Rendererzeichen`() {
        val konkret = IterierterAusdruck(
            f,
            IterationsArt.DIFFERENTIATION,
            IterationsOrdnung.Konkret(4),
        ).alsSnapshot()
        val symbolisch = IterationsOrdnung.Symbolisch(
            Variable("n"),
            setOf(UnentscheidbareAussage("n\\in\\mathbb N_0", "Iteration")),
        ).alsSnapshot()

        assertEquals("iteration.differentiation", konkret.operatorId)
        assertEquals("konkret", konkret.ordnung.art)
        assertEquals("4", konkret.ordnung.wert)
        assertNotEquals("IV", konkret.ordnung.wert)
        assertEquals("symbolisch", symbolisch.art)
        assertIs<IterationsOrdnung.Symbolisch>(symbolisch.zuOrdnung())
    }
}
