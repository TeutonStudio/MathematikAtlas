package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MethodenRestriktionTest {
    private fun endlicheZahlen(vararg werte: Long): MengenAusdruck =
        if (werte.isEmpty()) LeereMenge else EndlicheMenge(werte.map { RationaleZahl.von(it) }.toSet())

    private fun konstanteMethode(
        name: String,
        werteVorrat: MengenAusdruck,
        wert: Long,
        ziel: MengenAusdruck,
    ): Methode {
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
    fun `echte Restriktion setzt den effektiven Gesamtwertebereich`() {
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

        assertEquals(AbdeckungsStatus.Vollständig, ergebnis.abdeckungsStatus)
        val restriktion = assertNotNull(ergebnis.methode)
        assertEquals(m, restriktion.effektiverWerteVorrat)
        assertEquals(m, restriktion.methodenSignatur().werteVorrat)
        assertEquals(RationaleZahl.von(2), restriktion.wendeAn(listOf(RationaleZahl.von(2))))
    }

    @Test
    fun `Ergaenzungen haben stabile Prioritaet und decken den Rest schrittweise`() {
        val ziel = endlicheZahlen(0, 1, 2)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val g1 = konstanteMethode("g_1", endlicheZahlen(1), 1, ziel)
        val g2 = konstanteMethode("g_2", endlicheZahlen(1, 2), 2, ziel)
        val m = endlicheZahlen(0, 1, 2)

        val ergebnis = restriktiereMethode(basis, m, listOf(g1, g2))

        assertEquals(AbdeckungsStatus.Vollständig, ergebnis.abdeckungsStatus)
        assertEquals(endlicheZahlen(1), ergebnis.ergänzungen[0].effektiverBereich)
        assertEquals(endlicheZahlen(2), ergebnis.ergänzungen[1].effektiverBereich)
        val methode = assertNotNull(ergebnis.methode)
        assertEquals(RationaleZahl.von(0), methode.wendeAn(listOf(RationaleZahl.von(0))))
        assertEquals(RationaleZahl.von(1), methode.wendeAn(listOf(RationaleZahl.von(1))))
        assertEquals(RationaleZahl.von(2), methode.wendeAn(listOf(RationaleZahl.von(2))))
    }

    @Test
    fun `unvollstaendige Abdeckung liefert Restmenge statt einer totalen Methode`() {
        val ziel = endlicheZahlen(0, 1, 2)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val g1 = konstanteMethode("g_1", endlicheZahlen(1), 1, ziel)

        val ergebnis = restriktiereMethode(basis, endlicheZahlen(0, 1, 2), listOf(g1))

        assertEquals(AbdeckungsStatus.Unvollständig, ergebnis.abdeckungsStatus)
        assertEquals(endlicheZahlen(2), ergebnis.restMenge)
        assertNull(ergebnis.methode)
    }

    @Test
    fun `Ergaenzung ausserhalb der Zielmenge wird nur auf ihrem effektiven Bereich verworfen`() {
        val ziel = endlicheZahlen(0, 1, 2)
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, ziel)
        val falsch = konstanteMethode("g", endlicheZahlen(1), 9, endlicheZahlen(9))

        val ergebnis = restriktiereMethode(basis, endlicheZahlen(0, 1), listOf(falsch))

        assertTrue(ergebnis.hatZielmengenVerletzung)
        assertNull(ergebnis.methode)
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
    fun `leere Zielmenge ist sofort vollstaendig abgedeckt`() {
        val basis = konstanteMethode("f", endlicheZahlen(0), 0, endlicheZahlen(0))
        val ergebnis = restriktiereMethode(basis, LeereMenge)
        assertEquals(AbdeckungsStatus.Vollständig, ergebnis.abdeckungsStatus)
        assertNotNull(ergebnis.methode)
        assertEquals(LeereMenge, ergebnis.methode?.effektiverWerteVorrat)
    }
}
