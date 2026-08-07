package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HyperModellTest {
    @Test
    fun `kanonisches Hypermodell beschreibt freien Filter nur axiomatisch`() {
        val modell = KanonischesHyperModell.modell

        assertEquals(NatürlicheZahlen, modell.indexMenge)
        assertEquals(HyperAxiomArt.FREIHEIT, modell.frei.art)
        assertTrue(modell.filterAxiome.any { it.latex.contains("\\varnothing") })
        assertTrue(modell.frei.latex.contains("endlich"))
        assertTrue(modell.alleAxiome.all { it.aussage.entscheide().wahrheitswert == null })
    }

    @Test
    fun `interne Aussage wird strukturiert uebertragen`() {
        val aussage = Gleichheit(Variable("x"), Variable("x"))
        val ergebnis = TransferUebersetzer.uebertrage(aussage)

        assertEquals(TransferStatus.UEBERTRAGEN, ergebnis.status)
        assertEquals("{}^*\\left(x = x\\right)", ergebnis.uebertragen?.zuLatex())
        assertTrue(ergebnis.externeBestandteile.isEmpty())
    }

    @Test
    fun `verschachtelte externe Praedikate werden automatisch erkannt`() {
        val extern = externesHyperPraedikat(
            ExternesHyperPraedikat.INFINITESIMAL,
            Variable("h"),
        )
        val aussage = Implikation(
            WahrheitsKonstante(true),
            Konjunktion(listOf(WahrheitsKonstante(true), extern)),
        )
        val ergebnis = TransferUebersetzer.uebertrage(aussage)

        assertEquals(TransferStatus.EXTERNE_BESTANDTEILE, ergebnis.status)
        assertNull(ergebnis.uebertragen)
        assertEquals(setOf(ExternesHyperPraedikat.INFINITESIMAL), ergebnis.externeBestandteile)
        assertEquals(setOf("analysis.hyper.infinitesimal"), ergebnis.externeReferenzen.map { it.id }.toSet())
    }

    @Test
    fun `nicht registrierte Symbole erzeugen eigene Transferdiagnose`() {
        val aussage = Disjunktion(
            listOf(
                WahrheitsKonstante(false),
                nichtRegistriertesHyperSymbol("operator.fremd"),
            ),
        )
        val ergebnis = TransferUebersetzer.uebertrage(aussage)

        assertEquals(TransferStatus.NICHT_REGISTRIERTE_SYMBOLE, ergebnis.status)
        assertEquals(setOf("operator.fremd"), ergebnis.nichtRegistrierteSymbole)
    }

    @Test
    fun `Hypererweiterung der Quaternionen bleibt ausgeschlossen`() {
        val quaternionen = BenannteMenge("H", "\\mathbb H")

        assertFailsWith<IllegalArgumentException> {
            SymbolischeHyperErweiterung(
                grundobjekt = quaternionen,
                art = HyperErweiterungsArt.MENGE,
            )
        }
    }

    @Test
    fun `hyperendliche Struktur materialisiert nur ihr Sichtfenster`() {
        val struktur = SymbolischeHyperendlicheStruktur(
            grundstruktur = Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
            hyperIndex = Variable("H"),
            sichtfenster = listOf(RationaleZahl.Eins, RationaleZahl.von(2)),
        )

        assertEquals(2, struktur.sichtfenster.size)
        assertIs<NachweisStatus.Unentscheidbar>(struktur.cauchyStatus)
        assertTrue(struktur.zuLatex().contains("H"))
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

        val endlicherWert = assertIs<EndlicherErweiterterReellerWert>(
            assertIs<HyperLimesErgebnis.Wert>(werteHyperLimes(endlich)).wert,
        )
        assertEquals(RationaleZahl.von(3), endlicherWert.wert)
        assertEquals(PositiveUnendlichkeit, assertIs<HyperLimesErgebnis.Wert>(werteHyperLimes(positiv)).wert)
        assertEquals(NegativeUnendlichkeit, assertIs<HyperLimesErgebnis.Wert>(werteHyperLimes(negativ)).wert)
    }

    @Test
    fun `unentscheidbarer Hyper Limes bleibt bedingt mit Aussagen`() {
        val wert = SymbolischerHyperReellerWert("h")
        val ergebnis = assertIs<HyperLimesErgebnis.Bedingt>(werteHyperLimes(wert))

        assertEquals("\\operatorname{limes}\\left(h\\right)", ergebnis.ausdruck.zuLatex())
        assertTrue(ergebnis.voraussetzungen.isNotEmpty())
        assertTrue(ergebnis.voraussetzungen.all { it.entscheide().wahrheitswert == null })
    }

    @Test
    fun `sichtbar gleicher Limes behaelt getrennte Operator IDs`() {
        val hyper = HyperLimes(SymbolischerHyperReellerWert("h"))
        val grenzwert = FolgenOderMethodenGrenzwert(Variable("h"))

        assertEquals(hyper.zuLatex(), grenzwert.zuLatex())
        assertNotEquals(hyper.operatorId, grenzwert.operatorId)
    }
}
