package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.BenannteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TupelKnotenTest {
    private val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde("mathematik.tupel")!!

    @Test
    fun `Vorlage bleibt standardmaessig beim dynamischen Zahlentupel`() {
        val knoten = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)

        assertEquals(TUPEL_EINZEL_EINGABEN, tupelKonfiguration(knoten).erzeugungsArt)
        assertEquals(listOf("a", "b", "tupel"), knoten.anschlüsse.map { it.name })
        assertTrue(knoten.anschlüsse.take(2).all { it.kannSichErweitern })
    }

    @Test
    fun `Indexmethode wird fuer die mathematischen Indizes eins bis n ausgewertet`() {
        val knoten = konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero), TUPEL_METHODE)
        val index = Variable("i")
        val methode = Methode(
            name = "f",
            parameter = listOf(index),
            vorschrift = index,
            zielMenge = BenannteMenge("Z", "\\mathbb{Z}"),
        )

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "dimension" to BedingterWert(RationaleZahl.von(3)),
                    "methode" to BedingterWert(methode),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(
            listOf(RationaleZahl.von(1), RationaleZahl.von(2), RationaleZahl.von(3)),
            assertIs<Tupel>(ergebnis.ausgaben.getValue("tupel").objekt).elemente,
        )
    }

    @Test
    fun `Indexmethode darf allgemeine mathematische Objekte als Komponenten liefern`() {
        val knoten = konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero), TUPEL_METHODE)
        val methode = Methode(
            name = "wahr",
            parameter = listOf(Variable("i")),
            vorschrift = WahrheitsKonstante(true),
            zielMenge = BenannteMenge("A", "\\mathbb{A}"),
        )

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "dimension" to BedingterWert(RationaleZahl.von(2)),
                    "methode" to BedingterWert(methode),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(
            listOf(WahrheitsKonstante(true), WahrheitsKonstante(true)),
            assertIs<Tupel>(ergebnis.ausgaben.getValue("tupel").objekt).elemente,
        )
    }

    @Test
    fun `Dimension muss konkret ganzzahlig und nichtnegativ sein`() {
        val knoten = konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero), TUPEL_METHODE)
        val methode = Methode(
            name = "f",
            parameter = listOf(Variable("i")),
            vorschrift = RationaleZahl.Eins,
            zielMenge = BenannteMenge("Z", "\\mathbb{Z}"),
        )

        val nullErgebnis = GesamterMathematikAuswerter.erzeugeRegister().finde("mathematik.tupel")!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("dimension" to BedingterWert(RationaleZahl.Null), "methode" to BedingterWert(methode)),
                RechenKontext(),
            ),
        )
        assertEquals(emptyList(), assertIs<Tupel>(nullErgebnis.ausgaben.getValue("tupel").objekt).elemente)

        val fehler = assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(
                KnotenAuswertungsKontext(
                    knoten,
                    mapOf("dimension" to BedingterWert(RationaleZahl.von(3, 2)), "methode" to BedingterWert(methode)),
                    RechenKontext(),
                ),
            )
        }
        assertContains(fehler.message.orEmpty(), "konkrete positive ganze Zahl")
    }

    @Test
    fun `Moduswechsel ersetzt Anschluesse atomar und Undo stellt Kante wieder her`() {
        val tupel = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)
        val quelle = KnotenDaten(
            art = "test.zahl",
            name = "Quelle",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Zahl.id,
                ),
            ),
        )
        val a = tupel.anschlüsse.first { it.name == "a" }
        val zustand = KartenEditorZustand(
            KartenDaten(
                name = "Test",
                knoten = listOf(quelle, tupel),
                verbindungen = listOf(
                    VerbindungDaten(
                        von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                        zu = AnschlussVerweis(tupel.id, a.id),
                    ),
                ),
            ),
            GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)),
        )

        zustand.setzeTupelKonfiguration(tupel.id, TUPEL_METHODE)

        assertTrue(zustand.karte.verbindungen.isEmpty())
        assertEquals(
            listOf("dimension", "methode", "tupel"),
            zustand.karte.knoten.first { it.id == tupel.id }.anschlüsse.map { it.name },
        )
        zustand.rückgängig()
        assertEquals(1, zustand.karte.verbindungen.size)
        assertEquals(a.id, zustand.karte.knoten.first { it.id == tupel.id }.anschlüsse.first { it.name == "a" }.id)
    }
}
