package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class AtlasBefehlTest {
    private val art = AnschlussArtId("test")
    private val register = AnschlussArtRegister(listOf(AnschlussArt(art, "Test")))

    private fun knoten(name: String, x: Float, ausgang: Boolean = false): KnotenDaten = KnotenDaten(
        art = KnotenArtId("test.$name"),
        name = name,
        position = GraphPunkt(x, 20f),
        anschlüsse = listOf(AnschlussDaten(
            name = if (ausgang) "aus" else "ein",
            art = art,
            richtung = if (ausgang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
            kante = if (ausgang) AnschlussKante.Rechts else AnschlussKante.Links,
        )),
    )

    @Test fun `Modifierauswahl ersetzt erweitert und schaltet um`() {
        val a = knoten("a", 0f)
        val b = knoten("b", 200f)
        val editor = KartenEditorZustand(KartenDaten(name = "T", knoten = listOf(a, b)), GraphPrüfung(register))
        editor.wähleKnoten(a.id)
        editor.wähleKnoten(b.id, AuswahlÄnderung.Hinzufügen)
        assertEquals(setOf(a.id, b.id), editor.ausgewählteKnoten)
        editor.wähleKnoten(a.id, AuswahlÄnderung.Umschalten)
        assertEquals(setOf(b.id), editor.ausgewählteKnoten)
        editor.wähleKnoten(a.id)
        assertEquals(setOf(a.id), editor.ausgewählteKnoten)
    }

    @Test fun `Clipboard kopiert nur interne Kante und vergibt alle IDs neu`() {
        val a = knoten("a", 0f, ausgang = true)
        val b = knoten("b", 200f)
        val c = knoten("c", 400f)
        val intern = VerbindungDaten(AnschlussVerweis(a.id, a.anschlüsse.single().id), AnschlussVerweis(b.id, b.anschlüsse.single().id))
        val extern = VerbindungDaten(AnschlussVerweis(a.id, a.anschlüsse.single().id), AnschlussVerweis(c.id, c.anschlüsse.single().id))
        val gruppe = VisuelleKnotenGruppeDaten(knotenIds = setOf(a.id, b.id), position = GraphPunkt.Zero)
        val karte = KartenDaten(name = "T", knoten = listOf(a, b, c), verbindungen = listOf(intern, extern), visuelleGruppen = listOf(gruppe))
        val clipboard = AtlasZwischenablage()
        assertTrue(clipboard.kopiere(karte, setOf(a.id, b.id)))
        val neu = assertNotNull(clipboard.erzeugeEinfügung(GraphPunkt(100f, 100f)))
        assertEquals(2, neu.knoten.size)
        assertEquals(1, neu.verbindungen.size)
        assertEquals(1, neu.gruppen.size)
        assertTrue(neu.knoten.none { it.id in setOf(a.id, b.id) })
        assertTrue(neu.knoten.flatMap { it.anschlüsse }.none { it.id in setOf(a.anschlüsse.single().id, b.anschlüsse.single().id) })
        assertNotEquals(gruppe.id, neu.gruppen.single().id)
    }

    @Test fun `Paste ist ein Undo-Schritt und wiederholtes Paste ist versetzt`() {
        val a = knoten("a", 0f)
        val editor = KartenEditorZustand(KartenDaten(name = "T", knoten = listOf(a)), GraphPrüfung(register))
        editor.wähleKnoten(a.id)
        val befehle = AtlasBefehlsAusführer(editor)
        val kontext = BefehlsKontext(sichtbareMitte = GraphPunkt(100f, 100f))
        assertTrue(befehle.führeAus(AtlasBefehl.AuswahlKopieren, kontext))
        assertTrue(befehle.führeAus(AtlasBefehl.AuswahlEinfügen(), kontext))
        val erstePosition = editor.karte.knoten.last().position
        assertTrue(befehle.führeAus(AtlasBefehl.AuswahlEinfügen(), kontext))
        val zweitePosition = editor.karte.knoten.last().position
        assertEquals(GraphPunkt(24f, 24f), zweitePosition - erstePosition)
        editor.rückgängig()
        assertEquals(2, editor.karte.knoten.size)
    }

    @Test fun `Textfokus schützt Graphshortcuts`() {
        val a = knoten("a", 0f)
        val editor = KartenEditorZustand(KartenDaten(name = "T", knoten = listOf(a)), GraphPrüfung(register))
        editor.wähleKnoten(a.id)
        val befehle = AtlasBefehlsAusführer(editor)
        val text = BefehlsKontext(fokus = AtlasFokusBereich.TextEditor)
        assertFalse(befehle.führeAus(AtlasBefehl.AuswahlLöschen, text))
        assertEquals(1, editor.karte.knoten.size)
    }

    @Test fun `Escape bricht Verbindung vor Auswahl ab`() {
        val a = knoten("a", 0f, ausgang = true)
        val editor = KartenEditorZustand(KartenDaten(name = "T", knoten = listOf(a)), GraphPrüfung(register))
        editor.wähleKnoten(a.id)
        editor.beginneVerbindung(AnschlussVerweis(a.id, a.anschlüsse.single().id))
        assertTrue(editor.brecheInteraktionAb())
        assertNull(editor.verbindungsStart)
        assertEquals(setOf(a.id), editor.ausgewählteKnoten)
        assertTrue(editor.brecheInteraktionAb())
        assertTrue(editor.ausgewählteKnoten.isEmpty())
    }
}
