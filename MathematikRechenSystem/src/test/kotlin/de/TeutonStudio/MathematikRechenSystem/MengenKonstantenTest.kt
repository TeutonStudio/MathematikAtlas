package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger
import kotlin.test.*

class MengenKonstantenTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun c(a: Long, b: Long) = KomplexeZahl(z(a), z(b))
    private fun mitglied(wert: MathematischesObjekt, menge: MengenKonstante) = ElementBeziehung(wert, menge.menge).entscheide().wahrheitswert

    @Test fun `alle Quellen haben stabile nullstellige Signaturen`() {
        assertEquals(setOf("leer", "N", "N0", "Z", "Q", "R", "C", "P", "Zi", "PZi", "Grad"), RechnerOperatorDefinitionen.mengenQuellen.map { it.stabileId }.toSet())
        MengenKonstante.entries.forEach {
            assertTrue(it.signatur.eingänge.isEmpty())
            assertEquals(FormelTyp.MENGE, it.signatur.ausgänge.single().typ)
            assertEquals(it.menge, it.auswerten())
            assertSame(it, MengenKonstante.vonId(it.stabileId))
        }
        assertNull(MengenKonstante.vonId("fremd"))
    }

    @Test fun `alle hundert Inklusionen und Gleichheiten sind konsistent und durch Gegenbeispiele abgesichert`() {
        // Spalten und Zeilen: leer,N,N0,Z,Q,R,C,P,Zi,PZi.
        val matrix = listOf(
            "1111111111", "0111111010", "0011111010", "0001111010", "0000111000",
            "0000011000", "0000001000", "0111111110", "0000001010", "0000001011",
        )
        val zeugen = listOf(z(0), z(1), z(-1), z(2), z(3), RationaleZahl.von(1, 2), c(0, 1), c(1, 1), MathematischeKonstante("pi", "\\pi"))
        MengenKonstante.entries.filter { it != MengenKonstante.GRAD }.forEachIndexed { i, a ->
            MengenKonstante.entries.filter { it != MengenKonstante.GRAD }.forEachIndexed { j, b ->
                val erwartet = matrix[i][j] == '1'
                assertEquals(if (erwartet) Wahrheitswert.Wahr else Wahrheitswert.Lüge, TeilmengenBeziehung(a.menge, b.menge).entscheide().wahrheitswert, "${a.stabileId} ⊆ ${b.stabileId}")
                assertEquals(if (i == j) Wahrheitswert.Wahr else Wahrheitswert.Lüge, Gleichheit(a.menge, b.menge).entscheide().wahrheitswert)
                assertEquals(if (erwartet && i != j) Wahrheitswert.Wahr else Wahrheitswert.Lüge, EchteTeilmengeBeziehung(a.menge, b.menge).entscheide().wahrheitswert)
                assertEquals(if (a == MengenKonstante.LEER || b == MengenKonstante.LEER) Wahrheitswert.Wahr else Wahrheitswert.Lüge, Disjunktheit(a.menge, b.menge).entscheide().wahrheitswert)
                // Für konkrete Zeugen darf ein bewiesenes Teilmengenverhältnis nie verletzt werden.
                if (erwartet) zeugen.forEach { wert ->
                    if (mitglied(wert, a) == Wahrheitswert.Wahr) assertNotEquals(Wahrheitswert.Lüge, mitglied(wert, b))
                }
            }
        }
    }

    @Test fun `Null negative Werte Brueche und komplexe Komponenten werden exakt behandelt`() {
        assertEquals(Wahrheitswert.Lüge, mitglied(z(0), MengenKonstante.N))
        assertEquals(Wahrheitswert.Wahr, mitglied(z(0), MengenKonstante.N0))
        assertEquals(Wahrheitswert.Lüge, mitglied(z(-1), MengenKonstante.N0))
        assertEquals(Wahrheitswert.Wahr, mitglied(z(-1), MengenKonstante.Z))
        assertEquals(Wahrheitswert.Lüge, mitglied(RationaleZahl.von(1, 2), MengenKonstante.ZI))
        assertEquals(Wahrheitswert.Wahr, mitglied(c(2, 0), MengenKonstante.N))
        assertEquals(Wahrheitswert.Lüge, mitglied(c(2, 1), MengenKonstante.R))
        assertEquals(Wahrheitswert.Wahr, mitglied(c(-2, 1), MengenKonstante.ZI))
        assertEquals(Wahrheitswert.Wahr, mitglied(c(-2, 1), MengenKonstante.C))
        assertNull(mitglied(Variable("x"), MengenKonstante.P))
        assertNull(TeilmengenBeziehung(BenannteMenge("X"), GanzeZahlen).entscheide().wahrheitswert)
    }

    @Test fun `rationale und gausssche Primzahlen haben verschiedene Mitgliedschaft`() {
        listOf(2L, 3L, 5L, 7L).forEach { assertEquals(Wahrheitswert.Wahr, mitglied(z(it), MengenKonstante.P)) }
        listOf(-3L, 0L, 1L, 4L, 9L).forEach { assertEquals(Wahrheitswert.Lüge, mitglied(z(it), MengenKonstante.P)) }
        listOf(c(1, 1), c(-1, -1), c(2, 1), c(3, 0), c(-3, 0), c(0, -3)).forEach { assertEquals(Wahrheitswert.Wahr, mitglied(it, MengenKonstante.PZI)) }
        listOf(c(2, 0), c(5, 0), c(1, 0), c(-1, 0), c(0, 1), c(0, -1), c(0, 0), c(2, 2), c(9, 0)).forEach { assertEquals(Wahrheitswert.Lüge, mitglied(it, MengenKonstante.PZI)) }
        val grossePrimzahl = RationaleZahl.von(BigInteger("32416190071"))
        assertNull(mitglied(grossePrimzahl, MengenKonstante.P))
    }

    @Test fun `Mengenalgebra absorbiert nur belegte Beziehungen`() {
        assertEquals(GanzeZahlen, vereinige(listOf(NatürlicheZahlen, GanzeZahlen)))
        assertEquals(NatürlicheZahlen, schneide(listOf(NatürlicheZahlen, GanzeZahlen)))
        assertEquals(LeereMenge, mengenDifferenz(Primzahlen, GaußscheGanzeZahlen))
        assertEquals(MengenDifferenz(GanzeZahlen, NatürlicheZahlen), symmetrischeDifferenz(NatürlicheZahlen, GanzeZahlen))
        assertIs<Schnitt>(schneide(listOf(RationaleZahlen, GaußscheGanzeZahlen)))
        assertIs<Vereinigung>(vereinige(listOf(Primzahlen, GaußschePrimzahlen)))
        assertIs<MengenDifferenz>(mengenDifferenz(Primzahlen, GaußschePrimzahlen))
        val ergebnis = MengenRechner.erzeuge(MengenRechnerOperator.SCHNITT, listOf(MengenRechnerEingabe("a", NatürlicheZahlen), MengenRechnerEingabe("b", GanzeZahlen)))
        assertEquals(NatürlicheZahlen, assertIs<MengenRechnerErgebnis.Wert>(ergebnis).menge)
    }

    @Test fun `N0 und alle Konstanten besitzen konsistente Kardinalitaet und reelle Eigenschaften`() {
        MengenKonstante.entries.forEach { quelle ->
            val vertrag = kardinalitaetsVertrag(quelle.menge)
            assertNotEquals(EndlichkeitsStatus.UNENTSCHEIDBAR, vertrag.endlichkeit)
            assertNotEquals(AbzaehlbarkeitsStatus.UNENTSCHEIDBAR, vertrag.abzaehlbarkeit)
            mächtigkeit(quelle.menge)
        }
        assertEquals(AussageStatus.BEWIESEN, konstanteReelleMengeneigenschaft(MengenKonstante.N0.menge, "abgeschlossen"))
        assertEquals(AussageStatus.WIDERLEGT, konstanteReelleMengeneigenschaft(RationaleZahlen, "konvex"))
        assertEquals(AussageStatus.BEWIESEN, konstanteReelleMengeneigenschaft(ReelleZahlen, "offen"))
        assertEquals(AussageStatus.UNENTSCHEIDBAR, konstanteReelleMengeneigenschaft(GaußscheGanzeZahlen, "offen"))
    }
    @Test fun `echte Topologieraume verwenden die Mengenbeziehungen und behalten ihren Traeger`() {
        val reell = TopologischerRaum(ReelleZahlen, StandardTopologieRegister.fuer(ReelleZahlen)!!)
        assertEquals(AussageStatus.BEWIESEN, reell.abgeschlossenheitsStatus(MengenKonstante.N0.menge))
        assertEquals(AussageStatus.WIDERLEGT, reell.offenheitsStatus(MengenKonstante.N0.menge))
        assertEquals(AussageStatus.WIDERLEGT, reell.abgeschlossenheitsStatus(RationaleZahlen))
        assertEquals(AussageStatus.WIDERLEGT, reell.offenheitsStatus(GaußscheGanzeZahlen))
        val komplex = TopologischerRaum(KomplexeZahlen, StandardTopologieRegister.fuer(KomplexeZahlen)!!)
        assertEquals(AussageStatus.UNENTSCHEIDBAR, komplex.offenheitsStatus(ReelleZahlen))
        val diskret = TopologischerRaum(GaußscheGanzeZahlen, DiskreteTopologie(GaußscheGanzeZahlen))
        assertEquals(AussageStatus.BEWIESEN, diskret.offenheitsStatus(Primzahlen))
        assertEquals(AussageStatus.WIDERLEGT, diskret.offenheitsStatus(ReelleZahlen))
    }

}
