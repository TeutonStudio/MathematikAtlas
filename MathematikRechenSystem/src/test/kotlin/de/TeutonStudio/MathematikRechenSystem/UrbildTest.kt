package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class UrbildTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private val x = Variable("x")
    private fun menge(vararg n: Long) = EndlicheMenge(n.map(::z).toSet())
    private fun quadrat(domäne: MengenAusdruck = ReelleZahlen) = Methode(
        "q", listOf(x), Potenz(x, z(2)), ReelleZahlen, mapOf("x" to domäne),
    )

    @Test fun `quadratic inverse contains every preimage and unifies target sets`() {
        assertEquals(menge(-2, 2), bildeUrbild(quadrat(), menge(4)).menge)
        assertEquals(menge(-2, 2, -3, 3), bildeUrbild(quadrat(), menge(4, 9)).menge)
        assertEquals(LeereMenge, bildeUrbild(quadrat(), menge(-1)).menge)
        assertEquals(menge(2), bildeUrbild(quadrat(NatürlicheZahlen), menge(4)).menge)
    }

    @Test fun `finite domain is enumerated with stable signature identities`() {
        val parameter = Variable("visible", bindungsId = "stored:parameter")
        val f = MathematischeMethode("f", listOf(parameter), Potenz(parameter, z(2)), ReelleZahlen,
            mapOf("visible" to menge(-2, 0, 2, 3)), argumentIdentitäten = mapOf("visible" to "public:argument"))
        assertEquals(menge(-2, 2), bildeUrbild(f, menge(4)).menge)
        assertEquals(menge(-2, 2), bildeUrbild(f.copy(werteVorräte = mapOf("visible" to ReelleZahlen)), menge(4)).menge)
    }

    @Test fun `effective joint domain filters finite candidates`() {
        val y = Variable("y")
        val f = Methode("sum", listOf(x, y), addition(x, y), ReelleZahlen,
            mapOf("x" to menge(1, 2), "y" to menge(1, 2)),
            effektiverWerteVorrat = EndlicheMenge(setOf(Tupel(listOf(z(1), z(2))))))
        assertEquals(EndlicheMenge(setOf(Tupel(listOf(z(1), z(2))))), bildeUrbild(f, menge(3)).menge)
    }

    @Test fun `empty targets and domains yield empty sets`() {
        assertEquals(LeereMenge, bildeUrbild(quadrat(), LeereMenge).menge)
        assertEquals(LeereMenge, bildeUrbild(quadrat(LeereMenge), menge(4)).menge)
    }

    @Test fun `one structured output is not confused with multiple outputs`() {
        val input = TypisiertesElement("v", "mathematik.tupel")
        val pair = Tupel(listOf(z(1), z(2)))
        val source = EndlicheMenge(setOf(pair, Tupel(listOf(z(3), z(4)))))
        val f = Methode("identity", listOf(input), input, Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), mapOf("v" to source))
        assertEquals(EndlicheMenge(setOf(pair)), bildeUrbild(f, EndlicheMenge(setOf(pair))).menge)
    }

    @Test fun `multiple numeric outputs form simultaneous equations`() {
        val y = Variable("y")
        val f = Methode("pair", listOf(x, y), Tupel(listOf(addition(x, y), subtraktion(x, y))),
            Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
            ausgabeNamen = listOf("sum", "difference"))
        val erwartet = EndlicheMenge(setOf(Tupel(listOf(z(2), z(1)))))
        assertEquals(erwartet, bildeUrbild(f, EndlicheMenge(setOf(Tupel(listOf(z(3), z(1)))))).menge)
    }

    @Test fun `zero arguments retain the empty tuple as the preimage element`() {
        val f = Methode("constant", emptyList(), z(7), ReelleZahlen)
        assertEquals(EndlicheMenge(setOf(Tupel(emptyList()))), bildeUrbild(f, menge(7)).menge)
        assertEquals(LeereMenge, bildeUrbild(f, menge(8)).menge)
    }

    @Test fun `open finite conditions preserve the entire predicate`() {
        val f = quadrat(menge(-2, 2)).copy(voraussetzungen = setOf(Gleichheit(Variable("a"), z(1))))
        val r = bildeUrbild(f, menge(4))
        assertIs<PrädikatsMenge>(r.menge)
        assertTrue(r.warnungen.isNotEmpty())
        assertTrue(r.menge.zuLatex().contains("a"))
    }

    @Test fun `original denominator exclusions survive simplification`() {
        val f = Methode("ratio", listOf(x), Division(x, x), ReelleZahlen, mapOf("x" to ReelleZahlen))
        val r = bildeUrbild(f, menge(1))
        assertFalse(r.menge == ReelleZahlen)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(z(0), r.menge).entscheide().wahrheitswert)
    }

    @Test fun `infinite structured domains have an exact symbolic representation`() {
        val input = TypisiertesElement("v", "mathematik.vektor.spalte", strukturForm = listOf(z(2)))
        val space = Vektorraum(VektorOrientierung.Spalte, 2, ReelleZahlen)
        val f = Methode("identity", listOf(input), input, space, mapOf("v" to space))
        val r = bildeUrbild(f, EndlicheMenge(setOf(SpaltenVektor(listOf(z(1), z(2))))))
        assertIs<PrädikatsMenge>(r.menge)
        assertTrue(r.warnungen.isNotEmpty())
    }

    @Test fun `incompatible target structures are rejected`() {
        assertFailsWith<IllegalArgumentException> { bildeUrbild(quadrat(), WahrheitsMenge) }
    }

    @Test fun `effective domain may enlarge finite component metadata`() {
        val f = quadrat(menge(0)).copy(effektiverWerteVorrat = Tupelraum(listOf(ReelleZahlen)))
        assertEquals(menge(-2, 2), bildeUrbild(f, menge(4)).menge)
    }

    @Test fun `finite vector targets cannot be supplied to numeric methods`() {
        assertFailsWith<IllegalArgumentException> {
            bildeUrbild(quadrat(), EndlicheMenge(setOf(SpaltenVektor(listOf(z(4))))))
        }
    }

    @Test fun `constant methods return whole domains and general targets remain exact`() {
        val f = Methode("constant", listOf(x), z(7), ReelleZahlen, mapOf("x" to menge(-1, 0, 1)))
        assertEquals(menge(-1, 0, 1), bildeUrbild(f, menge(7)).menge)
        assertEquals(LeereMenge, bildeUrbild(f, menge(8)).menge)
        val r = bildeUrbild(quadrat(), ReellesIntervall(z(0), false, z(4), false))
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(z(1), r.menge).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(z(3), r.menge).entscheide().wahrheitswert)
    }

    @Test fun `symbolic inverse sets roundtrip through the value codec`() {
        val input = TypisiertesElement("v", "mathematik.vektor.spalte", strukturForm = listOf(z(2)))
        val space = Vektorraum(VektorOrientierung.Spalte, 2, ReelleZahlen)
        val f = Methode("identity", listOf(input), input, space, mapOf("v" to space))
        val r = bildeUrbild(f, EndlicheMenge(setOf(SpaltenVektor(listOf(z(1), z(2)))))).menge
        assertEquals(r, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(r)))
    }

    @Test fun `symbolic set arguments preserve prerequisites without casts`() {
        val a=MengenParameter("A"); val b=MengenParameter("B")
        val f=Methode("sets",listOf(a,b),z(7),ReelleZahlen,
            mapOf("A" to Potenzmenge(ReelleZahlen),"B" to Potenzmenge(ReelleZahlen)))
            .copy(voraussetzungen=setOf(TeilmengenBeziehung(a,b)))
        val r=bildeUrbild(f,menge(7)).menge
        assertIs<PrädikatsMenge>(r)
        // Die allgemeine Potenzmengenmitgliedschaft bleibt im vorhandenen Kern offen.
        assertNull(ElementBeziehung(Tupel(listOf(menge(1),menge(1,2))),r).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge,ElementBeziehung(Tupel(listOf(menge(1),menge(2))),r).entscheide().wahrheitswert)
    }

    @Test fun `symbolic fallback does not erase open method conditions`() {
        val input=TypisiertesElement("v","mathematik.vektor.spalte",strukturForm=listOf(z(2)))
        val space=Vektorraum(VektorOrientierung.Spalte,2,ReelleZahlen)
        val f=Methode("constant",listOf(input),z(7),ReelleZahlen,mapOf("v" to space))
            .copy(voraussetzungen=setOf(Gleichheit(Variable("a"),z(1))))
        val r=bildeUrbild(f,menge(7)).menge
        assertNull(ElementBeziehung(SpaltenVektor(listOf(z(1),z(2))),r).entscheide().wahrheitswert)
    }

    @Test fun `methods without mathematical capabilities are diagnosed`() {
        val neutral=object : Methode { override val name="Script" }
        assertFailsWith<MethodenSignaturFehler> { bildeUrbild(neutral,menge(4)) }
    }

    @Test fun `infinite constant methods yield the complete domain`() {
        val f=Methode("constant",listOf(x),z(7),ReelleZahlen,mapOf("x" to ReelleZahlen))
        val r=bildeUrbild(f,menge(7)).menge
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(z(-100),r).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(z(100),r).entscheide().wahrheitswert)
        assertEquals(LeereMenge,bildeUrbild(f,menge(8)).menge)
    }
}
