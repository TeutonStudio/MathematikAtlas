package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HyperModellTest {
    @Test
    fun `kanonisches Hypermodell beschreibt freien Filter nur axiomatisch`() {
        val modell = KanonischesHyperModell.modell

        assertTrue(modell.frei)
        assertEquals("\\mathbb N", modell.indexMengeLatex)
        assertTrue(modell.filterAxiome.any { it.contains("\\varnothing") })
        assertTrue(modell.filterAxiome.any { it.contains("endlich") })
    }

    @Test
    fun `interne Aussage wird strukturiert uebertragen`() {
        val aussage = Gleichheit(Variable("x"), Variable("x"))
        val ergebnis = TransferUebersetzer.uebertrage(aussage)

        assertEquals(TransferStatus.UEBERTRAGEN, ergebnis.status)
        assertEquals("{}^*\\left(x = x\\right)", ergebnis.uebertragen?.zuLatex())
    }

    @Test
    fun `externe Praedikate werden nicht blind uebertragen`() {
        val aussage = WahrheitsKonstante(true)
        val ergebnis = TransferUebersetzer.uebertrage(
            aussage,
            externeBestandteile = setOf(ExternesHyperPraedikat.STANDARDTEIL),
        )

        assertEquals(TransferStatus.EXTERNE_BESTANDTEILE, ergebnis.status)
        assertNull(ergebnis.uebertragen)
        assertEquals(setOf(ExternesHyperPraedikat.STANDARDTEIL), ergebnis.externeBestandteile)
    }

    @Test
    fun `Hyper Limes bildet endliche und unendliche Werte auf erweiterte Reelle ab`() {
        val endlich = SymbolischerHyperReellerWert(
            name = "h",
            groessenKlasse = HyperGroessenKlasse.ENDLICH,
            standardteil = RationaleZahl.von(3),
        )
        val positiv = SymbolischerHyperReellerWert("H", HyperGroessenKlasse.POSITIV_UNENDLICH)
        val negativ = SymbolischerHyperReellerWert("-H", HyperGroessenKlasse.NEGATIV_UNENDLICH)

        assertEquals(RationaleZahl.von(3), assertIs<HyperLimesErgebnis.Wert>(werteHyperLimes(endlich)).wert)
        assertEquals(PositiveUnendlichkeit, assertIs<HyperLimesErgebnis.Wert>(werteHyperLimes(positiv)).wert)
        assertEquals(NegativeUnendlichkeit, assertIs<HyperLimesErgebnis.Wert>(werteHyperLimes(negativ)).wert)
    }

    @Test
    fun `unentscheidbarer Hyper Limes bleibt bedingt`() {
        val wert = SymbolischerHyperReellerWert("h")
        val ergebnis = assertIs<HyperLimesErgebnis.Bedingt>(werteHyperLimes(wert))

        assertEquals("\\operatorname{limes}\\left(h\\right)", ergebnis.ausdruck.zuLatex())
        assertTrue(ergebnis.voraussetzungen.isNotEmpty())
    }

    @Test
    fun `sichtbar gleicher Limes behaelt getrennte Operator IDs`() {
        val hyper = HyperLimes(SymbolischerHyperReellerWert("h"))
        val grenzwert = FolgenOderMethodenGrenzwert(Variable("f"))

        assertEquals(hyper.zuLatex(), grenzwert.zuLatex())
        assertNotEquals(hyper.operatorId, grenzwert.operatorId)
    }
}
