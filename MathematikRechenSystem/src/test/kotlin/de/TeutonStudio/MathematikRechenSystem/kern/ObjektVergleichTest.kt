package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class ObjektVergleichTest {
    private val eins = RationaleZahl.Eins
    private val zwei = RationaleZahl.von(2)
    private fun wahr(a: Aussage) = assertEquals(Wahrheitswert.Wahr, a.entscheide().wahrheitswert)
    private fun falsch(a: Aussage) = assertEquals(Wahrheitswert.Lüge, a.entscheide().wahrheitswert)

    @Test fun `geordnete Inhaltsfamilie ignoriert Orientierung nur bei Gleichheit`() {
        val werte = listOf(eins, zwei)
        val familie = listOf(Tupel(werte), SpaltenVektor(werte), ZeilenVektor(werte))
        familie.forEach { a -> familie.forEach { b ->
            wahr(Gleichheit(a, b))
            assertEquals(a::class == b::class, Selbigkeit(a, b).entscheide().wahrheitswert == Wahrheitswert.Wahr)
        } }
        falsch(Gleichheit(Tupel(werte), Tupel(werte.reversed())))
        falsch(Gleichheit(Tupel(werte), Tupel(listOf(eins))))
    }

    @Test fun `Selbigkeit prueft verschachtelte Strukturtypen`() {
        val a = Tupel(listOf(Tupel(listOf(eins, zwei))))
        val b = Tupel(listOf(SpaltenVektor(listOf(eins, zwei))))
        wahr(Gleichheit(a, b)); falsch(Selbigkeit(a, b))
        wahr(Selbigkeit(a, Tupel(listOf(Tupel(listOf(eins, zwei))))))
    }

    @Test fun `Zahlwert wird normalisiert aber komplexer Typ erhalten`() {
        val term = Division(RationaleZahl.von(4), zwei)
        wahr(Selbigkeit(term, zwei))
        val komplex = KomplexeZahl(zwei, RationaleZahl.Null)
        wahr(Gleichheit(zwei, komplex)); falsch(Selbigkeit(zwei, komplex))
        wahr(Selbigkeit(komplex, KomplexeZahl(term, RationaleZahl.Null)))
    }

    @Test fun `Mengen unterscheiden Elementidentitaet von Inhaltsueberdeckung`() {
        val tupel = Tupel(listOf(eins, zwei))
        val spalte = SpaltenVektor(listOf(eins, zwei))
        val eine = EndlicheMenge(setOf(tupel))
        val beide = EndlicheMenge(setOf(tupel, spalte))
        falsch(ElementBeziehung(spalte, eine))
        wahr(ElementBeziehung(spalte, beide))
        wahr(Gleichheit(eine, beide)); falsch(Selbigkeit(eine, beide))
        assertEquals(2, eindeutigeEndlicheElemente(beide)?.size)
        assertEquals(1, eindeutigeEndlicheElemente(EndlicheMenge(setOf(eins, Division(eins, eins))))?.size)
        wahr(Gleichheit(LeereMenge, EndlicheMenge(emptySet())))
        wahr(Selbigkeit(LeereMenge, EndlicheMenge(emptySet())))
        wahr(Selbigkeit(ReellesIntervall(eins, false, eins, true), LeereMenge))
        wahr(Gleichheit(ReellesIntervall(eins, false, eins, false), EndlicheMenge(setOf(eins))))
    }

    @Test fun `Unentscheidbare Elemente werden nicht zu verschiedenen Elementen erklaert`() {
        val x = Variable("x"); val y = Variable("y")
        assertNull(Gleichheit(x, y).entscheide().wahrheitswert)
        assertNull(Selbigkeit(x, y).entscheide().wahrheitswert)
        assertNull(eindeutigeEndlicheElemente(EndlicheMenge(setOf(x, y))))
        assertNull(Gleichheit(BenannteMenge("A"), Matrix(listOf(listOf(eins)))).entscheide().wahrheitswert)
    }

    @Test fun `Matrix und Tensor behalten Form und Inhaltsfamilie`() {
        val zeile = Matrix(listOf(listOf(eins, zwei)))
        val spalte = Matrix(listOf(listOf(eins), listOf(zwei)))
        falsch(Gleichheit(zeile, spalte))
        assertNull(Gleichheit(zeile, ZeilenVektor(listOf(eins, zwei))).entscheide().wahrheitswert)
        falsch(Gleichheit(Tensor(listOf(2, 1), listOf(eins, zwei)), Tensor(listOf(1, 2), listOf(eins, zwei))))
    }

    @Test fun `Selbigkeit impliziert Gleichheit in allen unterstuetzten Familien`() {
        val werte = listOf<MathematischesObjekt>(eins, KomplexeZahl(eins, eins), Tupel(emptyList()), Tupel(listOf(eins)),
            SpaltenVektor(listOf(eins)), ZeilenVektor(listOf(eins)), Matrix(listOf(listOf(eins))),
            Tensor(listOf(1), listOf(eins)), EndlicheMenge(setOf(eins)), LeereMenge, Tupelraum(listOf(ReelleZahlen)))
        werte.forEach { a -> werte.forEach { b ->
            if (Selbigkeit(a, b).entscheide().wahrheitswert == Wahrheitswert.Wahr) wahr(Gleichheit(a, b))
        } }
        val operator = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.selbigkeit"))
        assertIs<Selbigkeit>(operator.werteAus(mapOf("links" to eins, "rechts" to eins)))
        assertTrue(RelationsKlasse.AEQUIVALENZRELATION in operator.relationsStruktur!!.klassen())
        assertTrue("stackrel" in Selbigkeit(eins, eins).zuLatex())
    }
    @Test fun `symbolische und abstrakte Objekte sind reflexiv`() {
        val objekte = listOf<MathematischesObjekt>(Primzahlen, Potenzmenge(ReelleZahlen),
            Tensorraum(ReelleZahlen, listOf(zwei)), TypisiertesElement("v", "vektor"), AussagenParameter("A"))
        objekte.forEach { objekt ->
            wahr(Selbigkeit(objekt, objekt)); wahr(Gleichheit(objekt, objekt))
            wahr(ElementBeziehung(objekt, EndlicheMenge(setOf(objekt))))
        }
    }

    @Test fun `symbolische Intervalloffenheit beweist keine Ungleichheit`() {
        val x = Variable("x"); val y = Variable("y")
        val geschlossen = ReellesIntervall(x, false, y, false)
        val offen = ReellesIntervall(x, true, y, true)
        assertNull(Gleichheit(geschlossen, offen).entscheide().wahrheitswert)
        assertNull(Selbigkeit(geschlossen, offen).entscheide().wahrheitswert)
        wahr(Gleichheit(ReellesIntervall(zwei, false, eins, false), ReellesIntervall(zwei, true, eins, true)))
        falsch(Gleichheit(ReellesIntervall(eins, false, zwei, false), ReellesIntervall(eins, true, zwei, true)))
    }

    @Test fun `verschiedene Mengenausdruecke sind ohne Zeugen nicht widerlegt`() {
        val a = BenannteMenge("A")
        val paare = listOf(
            BenannteMenge("P") to Primzahlen,
            Potenzmenge(LeereMenge) to EndlicheMenge(setOf(LeereMenge)),
            Vereinigung(listOf(a, LeereMenge)) to a,
            MengenDifferenz(a, a) to LeereMenge,
            Tupelraum(listOf(ReelleZahlen)) to KartesischesProdukt(listOf(ReelleZahlen)),
        )
        paare.forEach { (links, rechts) ->
            assertTrue(Selbigkeit(links, rechts).entscheide().wahrheitswert != Wahrheitswert.Lüge)
        }
        assertNull(Selbigkeit(BenannteMenge("P"), Primzahlen).entscheide().wahrheitswert)
    }

}
