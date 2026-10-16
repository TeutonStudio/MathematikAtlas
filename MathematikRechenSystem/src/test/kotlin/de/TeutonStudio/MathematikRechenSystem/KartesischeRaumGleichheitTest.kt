package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KartesischeRaumGleichheitTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    @Test fun `alle drei Darstellungen sind in beiden Richtungen gleich inklusive null und eins`() {
        for (n in 0..3) {
            val räume = listOf<MengenAusdruck>(Tupelraum(List(n) { ReelleZahlen }),
                KartesischesProdukt(List(n) { ReelleZahlen }), KartesischerPotenzraum(ReelleZahlen, z(n.toLong())))
            for (a in räume) for (b in räume) assertEquals(Wahrheitswert.Wahr, entscheideGleichheit(a, b).wahrheitswert)
        }
        assertEquals(Wahrheitswert.Lüge, entscheideGleichheit(Tupelraum(listOf(ReelleZahlen)), Tupelraum(emptyList())).wahrheitswert)
        assertNotEquals(Wahrheitswert.Wahr, entscheideGleichheit(Tupelraum(listOf(ReelleZahlen)), ReelleZahlen).wahrheitswert)
    }
    @Test fun `heterogene Faktoren werden geordnet verglichen ohne Verschachtelung zu verlieren`() {
        val heterogen = Tupelraum(listOf(ReelleZahlen, GanzeZahlen))
        assertEquals(Wahrheitswert.Wahr, entscheideGleichheit(heterogen, KartesischesProdukt(listOf(ReelleZahlen, GanzeZahlen))).wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, entscheideGleichheit(heterogen, KartesischesProdukt(listOf(GanzeZahlen, ReelleZahlen))).wahrheitswert)
        val verschachtelt = Tupelraum(listOf(heterogen, ReelleZahlen))
        assertEquals(Wahrheitswert.Wahr, entscheideGleichheit(verschachtelt, KartesischesProdukt(listOf(KartesischesProdukt(listOf(ReelleZahlen, GanzeZahlen)), ReelleZahlen))).wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, entscheideGleichheit(verschachtelt, KartesischerPotenzraum(ReelleZahlen, z(3))).wahrheitswert)
    }
    @Test fun `leere Faktoren beweisen gleiche leere Produkte statt ungleicher Faktoren`() {
        val a = KartesischesProdukt(listOf(ReelleZahlen, LeereMenge))
        val b = Tupelraum(listOf(GanzeZahlen, EndlicheMenge(emptySet())))
        assertEquals(Wahrheitswert.Wahr, entscheideGleichheit(a, b).wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, entscheideGleichheit(a, LeereMenge).wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, entscheideGleichheit(a, Tupelraum(listOf(ReelleZahlen))).wahrheitswert)
        val unbekannt = BenannteMenge("A")
        assertNull(entscheideGleichheit(Tupelraum(listOf(unbekannt, ReelleZahlen)), KartesischesProdukt(listOf(unbekannt, GanzeZahlen))).wahrheitswert)
    }

    @Test fun `symbolische Dimensionen werden weder geraten noch expandiert`() {
        val n = Variable("n")
        assertEquals(Wahrheitswert.Wahr, entscheideGleichheit(KartesischerPotenzraum(ReelleZahlen, n), KartesischerPotenzraum(ReelleZahlen, addition(n, z(0)))).wahrheitswert)
        assertNull(entscheideGleichheit(KartesischerPotenzraum(ReelleZahlen, n), Tupelraum(listOf(ReelleZahlen))).wahrheitswert)
        assertNull(entscheideGleichheit(KartesischerPotenzraum(ReelleZahlen, n), KartesischerPotenzraum(GanzeZahlen, n)).wahrheitswert)
        assertNull(KartesischerPotenzraum(ReelleZahlen, RationaleZahl.parse("100000000000000000000")).kartesischeFaktorenOderNull())
    }
}
