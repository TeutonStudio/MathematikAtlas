package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

class EinfügeDragTest {
    private val art = AnschlussArtId("test")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test"))))
    private fun knoten(name: String, x: Float) = KnotenDaten(art = "test", name = name, position = GraphPunkt(x, 100f), größe = GraphGröße(100f, 80f),
        anschlüsse = listOf(AnschlussDaten(name = "in", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
            AnschlussDaten(name = "out", richtung = AnschlussRichtung.Ausgang, art = art, kante = AnschlussKante.Rechts)))

    @Test fun einfuegenZiehenUndAblageSindEinUndoSchrittUndBleibenSerialisierbar() {
        val basis = KartenDaten(name = "Drag")
        val editor = KartenEditorZustand(basis, prüfung)
        val k = knoten("neu", 10f)
        editor.beginneKnotenEinfügeZiehen(k)
        assertTrue(editor.interaktionAktiv)
        assertFalse(editor.kannRückgängig())
        editor.aktualisiereKnotenZiehen(GraphPunkt(300f, 220f))
        editor.beendeKnotenZiehen()
        assertFalse(editor.interaktionAktiv)
        val fertig = editor.karte
        assertEquals(GraphPunkt(300f, 220f), fertig.knoten.single().position)
        assertEquals(fertig, KartenDatenJson.lese(KartenDatenJson.schreibe(fertig)))
        editor.rückgängig(); assertEquals(basis, editor.karte); assertFalse(editor.kannRückgängig())
        editor.wiederholen(); assertEquals(fertig, editor.karte)
    }
    @Test fun abbrechenEntferntEinfügungUndBenachrichtigtDieAuswertungOhneHistorie() {
        val editor = KartenEditorZustand(KartenDaten(name = "Drag"), prüfung)
        val basis = editor.karte
        editor.beginneKnotenEinfügeZiehen(knoten("neu", 10f))
        val revision = editor.auswertungsRevision
        editor.aktualisiereKnotenZiehen(GraphPunkt(500f, 300f)); editor.verwerfeKnotenZiehen()
        assertEquals(basis, editor.karte); assertFalse(editor.interaktionAktiv); assertFalse(editor.kannRückgängig())
        assertTrue(editor.auswertungsRevision > revision)
    }
    @Test fun leitungsablageVerdrahtetUndSchafftPlatzInnerhalbDerselbenTransaktion() {
        val a = knoten("A", 0f); val b = knoten("B", 200f)
        val kante = VerbindungDaten(von = AnschlussVerweis(a.id, a.anschlüsse[1].id), zu = AnschlussVerweis(b.id, b.anschlüsse[0].id))
        val basis = KartenDaten(name = "Drag", knoten = listOf(a, b), verbindungen = listOf(kante))
        val editor = KartenEditorZustand(basis, prüfung)
        val neu = knoten("Neu", 100f)
        editor.beginneKnotenEinfügeZiehen(neu)
        editor.aktualisiereKnotenZiehen(neu.position)
        assertEquals(kante.id, editor.einfügeVerbindungsZiel)
        editor.beendeKnotenZiehen()
        assertEquals(2, editor.karte.verbindungen.size)
        assertTrue(editor.karte.knoten.first { it.id == a.id }.position.x < a.position.x)
        editor.rückgängig(); assertEquals(basis, editor.karte); assertFalse(editor.kannRückgängig())
    }
    @Test fun aktiveGruppeNimmtEinfügungAufUndAbbruchStelltRahmenWiederHer() {
        val a = knoten("A", 0f)
        val gruppe = VisuelleKnotenGruppeDaten(knotenIds = setOf(a.id), position = GraphPunkt.Zero, größe = GraphGröße(200f, 250f))
        val editor = KartenEditorZustand(KartenDaten(name = "Gruppe", knoten = listOf(a), visuelleGruppen = listOf(gruppe)), prüfung)
        editor.öffneGruppe(gruppe.id)
        val basis = editor.karte
        val neu = knoten("Neu", 300f)
        editor.beginneKnotenEinfügeZiehen(neu)
        assertTrue(neu.id in editor.karte.visuelleGruppen.single().knotenIds)
        editor.verwerfeKnotenZiehen(); assertEquals(basis, editor.karte)
    }
    @Test fun gruppenauswahlVerschiebtBeimEinfuegedragKeineBestehendenKnoten() {
        val alt = knoten("Alt", 0f)
        val editor = KartenEditorZustand(KartenDaten(name = "Auswahl", knoten = listOf(alt)), prüfung)
        editor.setzeAuswahlModus(de.TeutonStudio.KnotenKartenVerwalter.zustand.AuswahlModus.Gruppe)
        editor.wähleKnoten(alt.id)
        val neu = knoten("Neu", 100f)
        editor.beginneKnotenEinfügeZiehen(neu)
        val token = editor.interaktionsKennung
        assertEquals(setOf(neu.id), editor.ausgewählteKnoten)
        editor.aktualisiereKnotenZiehen(GraphPunkt(500f, 300f)); editor.beendeKnotenZiehen()
        assertEquals(alt, editor.karte.knoten.first { it.id == alt.id })
        editor.rückgängig()
        editor.beginneKnotenZiehen(alt.id)
        assertNotEquals(token, editor.interaktionsKennung)
        editor.verwerfeKnotenZiehen()
    }

}
