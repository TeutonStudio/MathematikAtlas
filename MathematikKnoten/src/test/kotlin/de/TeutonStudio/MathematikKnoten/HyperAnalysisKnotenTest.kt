package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HyperAnalysisKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    private fun kontext(
        knoten: KnotenDaten,
        eingaben: Map<String, BedingterWert> = emptyMap(),
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingaben,
        rechenKontext = RechenKontext(),
    )

    @Test
    fun `sichtbarer Vorlagenkatalog enthaelt alle Hyperanalyse Knoten`() {
        val arten = alleMathematikKnotenVorlagen().map { it.art }.toSet()

        assertTrue(HyperAnalysisKnotenVorlagen.alle.all { it.art in arten })
        assertEquals(6, HyperAnalysisKnotenVorlagen.alle.size)
    }

    @Test
    fun `endlicher Hyperwert und Hyper Limes verwenden erweiterten reellen Wert`() {
        val hyperKnoten = HyperAnalysisKnotenVorlagen.HyperWert.erzeuge(GraphPunkt.Zero).copy(
            parameter = HyperAnalysisKnotenVorlagen.HyperWert.standardParameter + mapOf(
                HYPER_WERT_NAME_PARAMETER to "h",
                HYPER_GROESSENKLASSE_PARAMETER to HyperGroessenKlasse.ENDLICH.name,
                HYPER_STANDARDTEIL_PARAMETER to "3/2",
            ),
        )
        val hyperErgebnis = assertNotNull(register.finde(HYPER_WERT_KNOTEN_ART)).auswerten(
            kontext(hyperKnoten),
        )
        val hyperwert = assertIs<SymbolischerHyperReellerWert>(
            hyperErgebnis.ausgaben.getValue("wert").objekt,
        )
        val limesKnoten = HyperAnalysisKnotenVorlagen.HyperLimes.erzeuge(GraphPunkt.Zero)
        val limesErgebnis = assertNotNull(register.finde(HYPER_LIMES_KNOTEN_ART)).auswerten(
            kontext(limesKnoten, mapOf("hyperwert" to BedingterWert(hyperwert))),
        )
        val wert = assertIs<EndlicherErweiterterReellerWert>(
            limesErgebnis.ausgaben.getValue("wert").objekt,
        )

        assertEquals(RationaleZahl.von(3, 2), wert.wert)
        assertEquals("\\overline{\\mathbb R}", limesErgebnis.ausgaben.getValue("wert").zielMenge?.zuLatex())
    }

    @Test
    fun `Transfer diagnostiziert externes Praedikat statt es zu uebertragen`() {
        val transfer = HyperAnalysisKnotenVorlagen.Transfer.erzeuge(GraphPunkt.Zero)
        val extern = externesHyperPraedikat(
            ExternesHyperPraedikat.STANDARD,
            Variable("h"),
        )
        val ergebnis = assertNotNull(register.finde(HYPER_TRANSFER_KNOTEN_ART)).auswerten(
            kontext(transfer, mapOf("aussage" to BedingterWert(extern))),
        )

        assertTrue(ergebnis.warnungen.single().contains("standard"))
        assertIs<UnentscheidbareAussage>(ergebnis.ausgaben.getValue("aussage").objekt)
    }

    @Test
    fun `Hypererweiterung von Quaternionen liefert Knotenfehler`() {
        val knoten = HyperAnalysisKnotenVorlagen.HyperErweiterung.erzeuge(GraphPunkt.Zero).copy(
            parameter = HyperAnalysisKnotenVorlagen.HyperErweiterung.standardParameter +
                (HYPER_ERWEITERUNGSART_PARAMETER to HyperErweiterungsArt.MENGE.name),
        )
        val ergebnis = assertNotNull(register.finde(HYPER_ERWEITERUNG_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf("grundobjekt" to BedingterWert(BenannteMenge("H", "\\mathbb H"))),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("Quaternionen"))
    }

    @Test
    fun `hyperendliches Sichtfenster bleibt ausdruecklich nur Vorschau`() {
        val knoten = HyperAnalysisKnotenVorlagen.HyperendlicheStruktur.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(HYPER_ENDLICH_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "struktur" to BedingterWert(Tupel(listOf(RationaleZahl.Eins))),
                    "hyperIndex" to BedingterWert(Variable("H")),
                    "sichtfenster" to BedingterWert(
                        Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
                    ),
                ),
            ),
        )
        val struktur = assertIs<SymbolischeHyperendlicheStruktur>(
            ergebnis.ausgaben.getValue("struktur").objekt,
        )

        assertEquals(2, struktur.sichtfenster.size)
        assertTrue(ergebnis.warnungen.single().contains("kein"))
    }

    @Test
    fun `historischer Hyperwert migriert idempotent auf kanonisches Modell`() {
        val alt = KnotenDaten(
            art = "mathematik.hyperzahl",
            name = "Alter Hyperwert",
            position = GraphPunkt.Zero,
            parameter = mapOf("name" to "H"),
        )
        val karte = KartenDaten(name = "Hyper", knoten = listOf(alt))

        val migriert = karte.migriereHyperAnalysisKnoten()
        val knoten = migriert.knoten.single()

        assertEquals(HYPER_WERT_KNOTEN_ART, knoten.art)
        assertEquals("H", knoten.parameter[HYPER_WERT_NAME_PARAMETER])
        assertEquals(KanonischesHyperModell.modell.id.wert, knoten.parameter[HYPER_MODELL_ID_PARAMETER])
        assertEquals(migriert, migriert.migriereHyperAnalysisKnoten())
    }
}
