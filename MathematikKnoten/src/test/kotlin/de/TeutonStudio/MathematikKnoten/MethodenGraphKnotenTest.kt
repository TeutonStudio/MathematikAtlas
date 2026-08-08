package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MethodenGraphKnotenTest {
    @Test
    fun `vorlage besitzt genau methodeneingang und mengenausgang`() {
        val knoten = MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt.Zero)

        assertEquals(METHODEN_GRAPH_KNOTEN_ART, knoten.art)
        assertEquals(
            listOf("methode"),
            knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.name },
        )
        assertEquals(
            listOf("graph"),
            knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.map { it.name },
        )
        assertEquals(MathematikAnschlussArten.Methode.id, knoten.anschlüsse.first { it.name == "methode" }.art)
        assertEquals(MathematikAnschlussArten.Menge.id, knoten.anschlüsse.first { it.name == "graph" }.art)
    }

    @Test
    fun `auswerter erzeugt symbolischen graphen aus aktueller methodensignatur`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = multiplikation(listOf(x, x)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val knoten = MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt.Zero)
        val register = MathematikAuswerterRegister().apply { registriereMethodenGraphKnoten() }
        val auswerter = assertNotNull(register.finde(METHODEN_GRAPH_KNOTEN_ART))

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )

        val graph = assertIs<MethodenGraphMenge>(ergebnis.ausgaben.getValue("graph").objekt)
        assertEquals(methode, graph.methode)
        assertEquals(KartesischesProdukt(listOf(ReelleZahlen, ReelleZahlen)), graph.methode.graphRaum())
        assertEquals("\\operatorname{Graph}\\left(f\\right)", ergebnis.ausgaben.getValue("graph").latexDarstellung)
    }

    @Test
    fun `fehlender wertevorrat erzeugt konkrete signaturdiagnose`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
        )
        val knoten = MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt.Zero)
        val register = MathematikAuswerterRegister().apply { registriereMethodenGraphKnoten() }
        val auswerter = assertNotNull(register.finde(METHODEN_GRAPH_KNOTEN_ART))

        val fehler = assertFailsWith<IllegalStateException> {
            auswerter.auswerten(
                KnotenAuswertungsKontext(
                    knoten = knoten,
                    eingänge = mapOf("methode" to BedingterWert(methode)),
                    rechenKontext = RechenKontext(),
                ),
            )
        }

        assertTrue(fehler.message.orEmpty().contains("Graph von 'f'"))
        assertTrue(fehler.message.orEmpty().contains("Wertevorrat"))
    }

    @Test
    fun `graph erscheint genau einmal im sichtbaren knotenkatalog`() {
        val graphen = alleMathematikKnotenVorlagen().filter { it.art == METHODEN_GRAPH_KNOTEN_ART }

        assertEquals(1, graphen.size)
        assertEquals(MethodenGraphKnotenVorlagen.Graph, graphen.single())
    }

    @Test
    fun `karten json roundtrip erhaelt graphknoten handles und verbindung`() {
        val quelle = methodenQuelle()
        val graph = MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt(300f, 0f))
        val verbindung = methodenVerbindung(quelle, graph)
        val karte = KartenDaten(
            id = KartenId("methodengraph-roundtrip"),
            name = "Methodengraph Roundtrip",
            knoten = listOf(quelle, graph),
            verbindungen = listOf(verbindung),
        )

        val gelesen = KartenDatenJson.lese(KartenDatenJson.schreibe(karte))

        assertEquals(karte, gelesen)
        val gelesenerGraph = gelesen.knoten.single { it.art == METHODEN_GRAPH_KNOTEN_ART }
        assertEquals(
            listOf(MathematikAnschlussArten.Methode.id, MathematikAnschlussArten.Menge.id),
            gelesenerGraph.anschlüsse.map { it.art },
        )
        assertEquals(verbindung, gelesen.verbindungen.single())
    }

    @Test
    fun `undo redo erhaelt graphknoten und methodenverbindung`() {
        val anschlussArten = AnschlussArtRegister(MathematikAnschlussArten.alle)
        val editor = KartenEditorZustand(
            startKarte = KartenDaten(id = KartenId("methodengraph-history"), name = "Historie"),
            prüfung = GraphPrüfung(anschlussArten),
        )
        val quelle = methodenQuelle()
        val graph = MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt(300f, 0f))
        val verbindung = methodenVerbindung(quelle, graph)

        editor.führeAus(KartenAktion.KnotenEinfügen(quelle))
        editor.führeAus(KartenAktion.KnotenEinfügen(graph))
        editor.führeAus(KartenAktion.VerbindungEinfügen(verbindung))
        assertEquals(2, editor.karte.knoten.size)
        assertEquals(listOf(verbindung), editor.karte.verbindungen)

        editor.rückgängig()
        assertTrue(editor.karte.verbindungen.isEmpty())
        assertTrue(editor.karte.knoten.any { it.id == graph.id })

        editor.rückgängig()
        assertFalse(editor.karte.knoten.any { it.id == graph.id })

        editor.wiederholen()
        assertTrue(editor.karte.knoten.any { it.id == graph.id })
        assertTrue(editor.karte.verbindungen.isEmpty())

        editor.wiederholen()
        assertEquals(listOf(verbindung), editor.karte.verbindungen)
    }

    private fun methodenQuelle(): KnotenDaten = KnotenDaten(
        id = KnotenId("methoden-quelle"),
        art = "test.methode",
        name = "Methode",
        position = GraphPunkt.Zero,
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("methoden-quelle-ausgang"),
                name = "methode",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Methode.id,
            ),
        ),
    )

    private fun methodenVerbindung(quelle: KnotenDaten, graph: KnotenDaten): VerbindungDaten =
        VerbindungDaten(
            id = VerbindungsId("methodengraph-verbindung"),
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(
                graph.id,
                graph.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.id,
            ),
        )
}
