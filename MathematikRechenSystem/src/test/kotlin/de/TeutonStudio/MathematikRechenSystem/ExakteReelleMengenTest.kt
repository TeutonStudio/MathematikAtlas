package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger
import kotlin.test.*

class ExakteReelleMengenTest {
    private fun z(n: Long, d: Long = 1) = RationaleZahl.von(n, d)
    private fun i(a: RationaleZahl, b: RationaleZahl, linksOffen: Boolean = false, rechtsOffen: Boolean = false) = ReellesIntervall(a, linksOffen, b, rechtsOffen)
    private fun norm(m: MengenAusdruck, a: RationaleZahl = z(-3), b: RationaleZahl = z(3)) =
        assertIs<ExakteMengenNormalisierung.Normalisiert>(normalisiereExakteReelleMenge(m, a, b, 1000)).menge
    private fun hat(m: ExakteReelleMenge, n: RationaleZahl) = m.segmente.any { it.enthält(ReelleGrenze.Endlich(n)) }

    @Test fun `kleine Intervalle und Luecken bleiben exakt erhalten`() {
        val schmal = norm(i(z(0), z(1, 100), true, true))
        assertTrue(hat(schmal, z(1, 200)))
        val getrennt = norm(Vereinigung(listOf(i(z(0), z(1)), i(z(101, 100), z(2)))))
        assertEquals(2, getrennt.segmente.size)
        assertFalse(hat(getrennt, z(201, 200)))
    }
    @Test fun `offene Subtraktion behaelt Endpunkte und Punktloch bleibt offen`() {
        val enden = norm(MengenDifferenz(i(z(0), z(1)), i(z(0), z(1), true, true)))
        assertEquals(2, enden.segmente.size)
        assertTrue(hat(enden, z(0))); assertTrue(hat(enden, z(1))); assertFalse(hat(enden, z(1, 2)))
        val loch = norm(MengenDifferenz(i(z(0), z(1)), EndlicheMenge(setOf(z(1, 2)))))
        assertEquals(2, loch.segmente.size); assertFalse(hat(loch, z(1, 2)))
    }
    @Test fun `Beruehrung wird nach exakter Randmitgliedschaft vereinigt`() {
        for (aOffen in listOf(false, true)) for (bOffen in listOf(false, true)) {
            val m = norm(Vereinigung(listOf(i(z(0), z(1), rechtsOffen = aOffen), i(z(1), z(2), linksOffen = bOffen))))
            assertEquals(if (aOffen && bOffen) 2 else 1, m.segmente.size)
        }
        val gefuellt = norm(Vereinigung(listOf(i(z(0), z(1), true, true), EndlicheMenge(setOf(z(1))), i(z(1), z(2), true, true))))
        assertEquals(1, gefuellt.segmente.size)
    }
    @Test fun `Fensterleerheit und global leer sind getrennt`() {
        val m = norm(Schnitt(listOf(GanzeZahlen, ReelleZahlen)), z(1, 10), z(9, 10))
        assertTrue(m.segmente.isEmpty()); assertFalse(m.vollständig)
        assertFalse(hat(norm(NatürlicheZahlen), z(0))); assertTrue(hat(norm(NatürlicheZahlen), z(1)))
    }
    @Test fun `leerer Schnitt braucht Grundmenge und Unendlichkeit bleibt offen`() {
        assertEquals(1, norm(Schnitt(emptyList(), ReelleZahlen)).segmente.size)
        assertIs<ExakteMengenNormalisierung.Offen>(normalisiereExakteReelleMenge(Schnitt(emptyList()), z(-1), z(1), 100))
        val rest = norm(MengenDifferenz(ReelleZahlen, i(z(0), z(1))))
        assertEquals(ReelleGrenze.MinusUnendlich, rest.segmente.first().von)
        assertFalse(rest.segmente.first().linksGeschlossen)
    }
    @Test fun `grosse rationale Grenzen werden vor Darstellung nicht verschmolzen`() {
        val a = RationaleZahl.von(BigInteger.ONE.shiftLeft(60))
        val b = a + z(1)
        assertEquals(2, norm(EndlicheMenge(setOf(a, b))).segmente.size)
        assertIs<ExakteMengenNormalisierung.Offen>(normalisiereExakteReelleMenge(GanzeZahlen, -a, a, 100))
    }
    @Test fun `nicht injektive Koordinatenabbildung darf keine Mengenalgebra ersetzen`() {
        val m = MengenDifferenz(EndlicheMenge(setOf(z(1))), EndlicheMenge(setOf(Tupel(listOf(z(1))))))
        assertIs<ExakteMengenNormalisierung.Offen>(normalisiereExakteReelleMenge(m, z(-2), z(2), 100))
    }
}
