package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class PrädikatsLösungTest {
    private val x = Variable("x")
    private val y = Variable("y")
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun solve(p: Aussage, m: MengenAusdruck = ReelleZahlen) = lösePrädikat(p, listOf(GebundeneMengenVariable(x,m)))
    private val linear get() = Gleichheit(addition(multiplikation(z(2),x),z(3)),z(7))

    @Test fun `linear solution has elementary steps and rejects excluded values`() {
        val r = solve(linear)
        assertEquals(EndlicheMenge(setOf(z(2))), r.menge)
        assertTrue(r.schritte.size >= 5)
        assertEquals(LeereMenge, solve(linear, EndlicheMenge(setOf(z(1),z(3)))).menge)
        assertEquals(LeereMenge, solve(Gleichheit(multiplikation(z(2),x),z(1)), GanzeZahlen).menge)
    }
    @Test fun `identity and contradiction differ in both solving paths`() {
        assertEquals(ReelleZahlen, solve(Gleichheit(addition(x,z(1)),addition(x,z(1)))).menge)
        assertEquals(LeereMenge, solve(Gleichheit(addition(x,z(1)),addition(x,z(2)))).menge)
        assertTrue(löseLinear(Gleichheit(x,x),x).alleWerte)
        assertFalse(löseLinear(Gleichheit(x,addition(x,z(1))),x).alleWerte)
    }
    @Test fun `negative inequality coefficient reverses order`() {
        val r = solve(Vergleich(multiplikation(z(-2),x), VergleichsArt.Kleiner,z(4)))
        val m = assertIs<BeschraenkteZahlmenge>(r.menge)
        assertEquals(z(-2), m.untereGrenze)
        assertFalse(m.untereGrenzeEnthalten)
    }
    @Test fun `quadratic roots double root and negative discriminant`() {
        assertEquals(EndlicheMenge(setOf(z(-2),z(2))), solve(Gleichheit(Potenz(x,z(2)),z(4))).menge)
        assertEquals(EndlicheMenge(setOf(z(0))), solve(Gleichheit(Potenz(x,z(2)),z(0))).menge)
        assertEquals(LeereMenge, solve(Gleichheit(Potenz(x,z(2)),z(-1))).menge)
    }
    @Test fun `systems use gauss with actual variable columns`() {
        val p = Konjunktion(listOf(Gleichheit(addition(x,y),z(3)),Gleichheit(subtraktion(x,y),z(1))))
        val r = lösePrädikat(p,listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)))
        assertEquals(EndlicheMenge(setOf(Tupel(listOf(z(2),z(1))))),r.menge)
        val table = r.schritte.first().tabelle
        assertEquals(listOf("x","y","b"), assertNotNull(table).spalten.map { it.titel })
    }
    @Test fun `inconsistent and underdetermined systems retain domain`() {
        val vars = listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen))
        assertEquals(LeereMenge,lösePrädikat(Konjunktion(listOf(Gleichheit(addition(x,y),z(1)),Gleichheit(addition(x,y),z(2)))),vars).menge)
        val r = lösePrädikat(Konjunktion(listOf(Gleichheit(addition(x,y),z(1)))),vars)
        assertEquals(vars,assertIs<DefinierteMenge>(r.menge).variablen)
        assertNotNull(r.schritte.first().tabelle)
    }
    @Test fun `finite cartesian enumeration and logical alternatives`() {
        val m = EndlicheMenge(setOf(z(1),z(2),z(3)))
        assertEquals(EndlicheMenge(setOf(z(1),z(3))),solve(Disjunktion(listOf(Gleichheit(x,z(1)),Gleichheit(x,z(3)))),m).menge)
        assertEquals(EndlicheMenge(setOf(z(2),z(3))),solve(Negation(Gleichheit(x,z(1))),m).menge)
    }
    @Test fun `unknown candidates and budget keep exact remainder`() {
        val p = Gleichheit(Sinus(x),Variable("a"))
        assertIs<DefinierteMenge>(solve(p).menge)
        val m = EndlicheMenge(setOf(z(1),z(2)))
        val r = lösePrädikat(p,listOf(GebundeneMengenVariable(x,m)),budget=1)
        assertIs<DefinierteMenge>(r.menge)
        assertTrue(r.warnungen.single().contains("vollständige"))
    }
    @Test fun `symbolic coefficient distinguishes zero case`() {
        val p = Gleichheit(multiplikation(Variable("a"),x),Variable("b"))
        val r = solve(p)
        val bed = assertIs<DefinierteMenge>(r.menge).bedingung
        assertIs<Disjunktion>(bed)
        assertEquals(Wahrheitswert.Wahr,ersetze(bed,mapOf("a" to z(0), "b" to z(0), "x" to z(8))).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge,ersetze(bed,mapOf("a" to z(0), "b" to z(1), "x" to z(8))).entscheide().wahrheitswert)
    }
    @Test fun `zero dimensional solution space`() {
        assertEquals(EndlicheMenge(setOf(Tupel(emptyList()))),lösePrädikat(WahrheitsKonstante(true),emptyList()).menge)
        assertEquals(LeereMenge,lösePrädikat(WahrheitsKonstante(false),emptyList()).menge)
    }
    @Test fun `definition holes survive identities negations and simplification`() {
        val quotient = Division(z(1),x)
        val identity = Gleichheit(quotient,quotient)
        val r = solve(identity)
        val bed = assertIs<DefinierteMenge>(r.menge).bedingung
        assertEquals(Wahrheitswert.Lüge,ersetze(bed,mapOf("x" to z(0))).entscheide().wahrheitswert)
        assertIs<DefinierteMenge>(solve(Negation(identity)).menge)
        assertNull(vereinfacheTerm(identity,RechenKontext()).entscheidung?.wahrheitswert)
        val undefined = Gleichheit(Division(z(1),z(0)),Division(z(1),z(0)))
        assertEquals(EntscheidungsStatus.NichtAuswertbar,vereinfacheTerm(undefined,RechenKontext()).entscheidung?.status)
    }
    @Test fun `huge exponent never overflows into a constant polynomial`() {
        val exponent = RationaleZahl.von(java.math.BigInteger("4294967296"))
        assertIs<DefinierteMenge>(solve(Gleichheit(Potenz(x,exponent),z(1))).menge)
    }
    @Test fun `predicate trace has visible progressive stages and nested arithmetic`() {
        val p = Gleichheit(Addition.roh(listOf(z(2),z(3))),z(5))
        val r = vereinfacheTerm(p,RechenKontext())
        assertEquals(WahrheitsKonstante(true),r.objekt)
        assertTrue(r.schritte.size >= 2)
        assertTrue(r.schritte.first().unterschritte.isNotEmpty())
        assertEquals(r.schritte.first().nachher, r.schritte.last().vorher)
    }

    @Test fun `finite and budget remainders preserve unknown parameter domain`() {
        val a=Variable("a")
        val q=Division(z(1),a)
        val p=Gleichheit(q,q)
        val vars=listOf(GebundeneMengenVariable(x,EndlicheMenge(setOf(z(1),z(2)))))
        for (budget in listOf(1,100)) {
            val r=lösePrädikat(p,vars,budget=budget)
            val gebunden=ersetze(r.menge,mapOf("a" to z(0))) as MengenAusdruck
            assertEquals(Wahrheitswert.Lüge,ElementBeziehung(z(1),gebunden).entscheide().wahrheitswert)
        }
        val r=lösePrädikat(p,emptyList())
        assertTrue(r.menge.zuLatex().contains("\\neq"))
    }

}
