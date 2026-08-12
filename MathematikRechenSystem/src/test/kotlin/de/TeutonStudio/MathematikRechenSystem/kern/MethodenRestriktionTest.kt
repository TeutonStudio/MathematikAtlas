package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MethodenRestriktionTest {
    private fun endlicheZahlen(vararg werte: Long): MengenAusdruck =
        if (werte.isEmpty()) LeereMenge else EndlicheMenge(werte.map { RationaleZahl.von(it) }.toSet())

    private fun konstanteMethode(
        name: String,
        werteVorrat: MengenAusdruck,
        wert: Long,
        ziel: MengenAusdruck,
    ): MathematischeMethode {
        val x = Variable("x")
        return Methode(
            name = name,
            parameter = listOf(x),
            vorschrift = RationaleZahl.von(wert),
            zielMenge = ziel,
            werteVorräte = mapOf(x.name to werteVorrat),
        )
    }

    @Test
    fun `gueltige Restriktion setzt exakt die Teilmenge als Definitionsbereich`() {
        val x = Variable("x")
        val basis = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to endlicheZahlen(0, 1, 2)),
        )
        val m = endlicheZahlen(0, 2)

        val ergebnis = restriktiereMethode(basis, m)

        val restriktion = assertNotNull(ergebnis.methode)
        assertEquals(m, restriktion.methodenSignatur().werteVorrat)
        assertEquals(ReelleZahlen, restriktion.methodenSignatur().zielMenge)
        assertEquals("f\\vert_{${m.zuLatex()}}", restriktion.zuLatex())
        assertEquals(RationaleZahl.von(2), restriktion.wendeAn(listOf(RationaleZahl.von(2))))
        assertNull(restriktion.bereichsanpassung)
    }

    @Test
    fun `Restriktion auf identische Definitionsmenge bleibt gueltig`() {
        val domain = endlicheZahlen(0, 1)
        val basis = konstanteMethode("f", domain, 1, endlicheZahlen(1))

        val ergebnis = restriktiereMethode(basis, domain)

        assertNotNull(ergebnis.methode)
        assertEquals(Wahrheitswert.Wahr, ergebnis.teilmengenPrüfung.wahrheitswert)
        assertEquals(domain, ergebnis.methode?.methodenSignatur()?.werteVorrat)
    }

    @Test
    fun `Restriktion ausserhalb der Definitionsmenge ist ungueltig und erweitert nicht`() {
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, endlicheZahlen(0))

        val ergebnis = restriktiereMethode(basis, endlicheZahlen(0, 1))

        assertEquals(Wahrheitswert.Lüge, ergebnis.teilmengenPrüfung.wahrheitswert)
        assertNull(ergebnis.methode)
        assertTrue(ergebnis.bedingungen.isEmpty())
    }

    @Test
    fun `leere Restriktionsmenge ist gueltig`() {
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, endlicheZahlen(0))

        val ergebnis = restriktiereMethode(basis, LeereMenge)

        val restriktion = assertNotNull(ergebnis.methode)
        assertEquals(LeereMenge, restriktion.methodenSignatur().werteVorrat)
        assertEquals(endlicheZahlen(0), restriktion.methodenSignatur().zielMenge)
    }

    @Test
    fun `mehrstellige Methode kann auf nichtkartesische Gesamtmenge restringiert werden`() {
        val x = Variable("x")
        val y = Variable("y")
        val basis = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = linkedMapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val diagonale = EndlicheMenge(
            setOf(
                Tupel(listOf(RationaleZahl.Null, RationaleZahl.Null)),
                Tupel(listOf(RationaleZahl.Eins, RationaleZahl.Eins)),
            ),
        )

        val ergebnis = restriktiereMethode(basis, diagonale)

        val restriktion = assertNotNull(ergebnis.methode)
        assertEquals(diagonale, restriktion.methodenSignatur().werteVorrat)
        assertEquals(
            RationaleZahl.von(2),
            restriktion.wendeAn(listOf(RationaleZahl.Eins, RationaleZahl.Eins)),
        )
    }

    @Test
    fun `Bereichsanpassung ohne Ergaenzung ist eigener Operator`() {
        val domain = endlicheZahlen(0, 1)
        val ziel = endlicheZahlen(7)
        val basis = konstanteMethode("f", domain, 7, ziel)

        val ergebnis = passeMethodenBereichAn(basis, domain)

        assertEquals(AbdeckungsStatus.Vollständig, ergebnis.abdeckungsStatus)
        val anpassung = assertNotNull(ergebnis.methode)
        assertSame(basis, anpassung.basis)
        assertTrue(anpassung.ergänzungen.isEmpty())
        assertEquals(domain, anpassung.methodenSignatur().werteVorrat)
        assertEquals(ziel, anpassung.methodenSignatur().zielMenge)
        assertTrue(anpassung.zuLatex().contains("Bereichsanpassung"))
        assertTrue(!anpassung.zuLatex().contains("\\vert_"))
    }

    @Test
    fun `Bereichsanpassung mit einer Ergaenzung deckt Restbereich`() {
        val ziel = endlicheZahlen(0, 1)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val g1 = konstanteMethode("g_1", endlicheZahlen(1), 1, ziel)

        val ergebnis = passeMethodenBereichAn(basis, endlicheZahlen(0, 1), listOf(g1))

        assertEquals(AbdeckungsStatus.Vollständig, ergebnis.abdeckungsStatus)
        assertEquals(endlicheZahlen(1), ergebnis.ergänzungen.single().effektiverBereich)
        val methode = assertNotNull(ergebnis.methode)
        assertEquals(RationaleZahl.von(0), methode.wendeAn(listOf(RationaleZahl.von(0))))
        assertEquals(RationaleZahl.von(1), methode.wendeAn(listOf(RationaleZahl.von(1))))
    }

    @Test
    fun `mehrere Ergaenzungen haben stabile erste passende Prioritaet`() {
        val ziel = endlicheZahlen(0, 1, 2)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val g1 = konstanteMethode("g_1", endlicheZahlen(1), 1, ziel)
        val g2 = konstanteMethode("g_2", endlicheZahlen(1, 2), 2, ziel)
        val m = endlicheZahlen(0, 1, 2)

        val ergebnis = passeMethodenBereichAn(basis, m, listOf(g1, g2))

        assertEquals(AbdeckungsStatus.Vollständig, ergebnis.abdeckungsStatus)
        assertEquals(endlicheZahlen(1), ergebnis.ergänzungen[0].effektiverBereich)
        assertEquals(endlicheZahlen(2), ergebnis.ergänzungen[1].effektiverBereich)
        val methode = assertNotNull(ergebnis.methode)
        assertEquals(RationaleZahl.von(1), methode.wendeAn(listOf(RationaleZahl.von(1))))
        assertEquals(RationaleZahl.von(2), methode.wendeAn(listOf(RationaleZahl.von(2))))
    }

    @Test
    fun `Umsortierung der Ergaenzungen aendert bei Ueberlappung deterministisch die Prioritaet`() {
        val ziel = endlicheZahlen(0, 1, 2)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val g1 = konstanteMethode("g_1", endlicheZahlen(1), 1, ziel)
        val g2 = konstanteMethode("g_2", endlicheZahlen(1), 2, ziel)
        val m = endlicheZahlen(0, 1)

        val zuerstG1 = assertNotNull(passeMethodenBereichAn(basis, m, listOf(g1, g2)).methode)
        val zuerstG2 = assertNotNull(passeMethodenBereichAn(basis, m, listOf(g2, g1)).methode)

        assertEquals(RationaleZahl.von(1), zuerstG1.wendeAn(listOf(RationaleZahl.von(1))))
        assertEquals(RationaleZahl.von(2), zuerstG2.wendeAn(listOf(RationaleZahl.von(1))))
    }

    @Test
    fun `unvollstaendige Bereichsanpassung liefert Restmenge statt totaler Methode`() {
        val ziel = endlicheZahlen(0, 1, 2)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val g1 = konstanteMethode("g_1", endlicheZahlen(1), 1, ziel)

        val ergebnis = passeMethodenBereichAn(basis, endlicheZahlen(0, 1, 2), listOf(g1))

        assertEquals(AbdeckungsStatus.Unvollständig, ergebnis.abdeckungsStatus)
        assertEquals(endlicheZahlen(2), ergebnis.restMenge)
        assertNull(ergebnis.methode)
    }

    @Test
    fun `Ergaenzung ausserhalb der Zielmenge wird auf effektivem Bereich verworfen`() {
        val ziel = endlicheZahlen(0, 1, 2)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val falsch = konstanteMethode("g", endlicheZahlen(1), 9, endlicheZahlen(9))

        val ergebnis = passeMethodenBereichAn(basis, endlicheZahlen(0, 1), listOf(falsch))

        assertTrue(ergebnis.hatZielmengenVerletzung)
        assertNull(ergebnis.methode)
    }

    @Test
    fun `groessere deklarierte Zielmenge darf auf effektivem Bereich trotzdem passen`() {
        val ziel = endlicheZahlen(0, 1)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val g = konstanteMethode("g", endlicheZahlen(1), 1, endlicheZahlen(1, 9))

        val ergebnis = passeMethodenBereichAn(basis, endlicheZahlen(0, 1), listOf(g))

        assertEquals(Wahrheitswert.Wahr, ergebnis.ergänzungen.single().zielPrüfung.wahrheitswert)
        val methode = assertNotNull(ergebnis.methode)
        assertEquals(ziel, methode.methodenSignatur().zielMenge)
    }
}
