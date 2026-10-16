package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class VerbindungsPieAdapterTest {
    private val laufzeit = MathematikKartenLaufzeit()
    private fun k(art: AnschlussArtId, eingang: Boolean) = KnotenDaten(art = "test", name = "Test",
        anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = if (eingang) AnschlussRichtung.Eingang else AnschlussRichtung.Ausgang,
            kante = if (eingang) AnschlussKante.Links else AnschlussKante.Rechts, art = art)))
    private fun ref(k: KnotenDaten) = AnschlussVerweis(k.id, k.anschlüsse.single().id)

    @Test fun realeUebersetzerEinfügungIstInBeidenStartrichtungenEinUndoSchritt() {
        for (vomEingang in listOf(false, true)) {
            val start = k(if (vomEingang) MathematikAnschlussArten.Winkel.id else MathematikAnschlussArten.Zahl.id, vomEingang)
            val neu = k(if (vomEingang) MathematikAnschlussArten.Zahl.id else MathematikAnschlussArten.Winkel.id, !vomEingang)
            val editor = KartenEditorZustand(KartenDaten(name = "Adapter", knoten = listOf(start)), laufzeit.graphPrüfung)
            val planer = ÜbersetzerVerbindungsPlaner(laufzeit.graphPrüfung)
            editor.verbindungsPlaner = planer::vorschläge; editor.verbindungÜberAdapterMöglich = planer::möglich
            val basis = editor.karte
            editor.beginneVerbindung(ref(start)); val kontext = assertNotNull(editor.übergibVerbindungZurEinfügung())
            assertTrue(editor.beginneVerbindungsEinfügeZiehen(neu, kontext))
            val treffer = editor.verbindungsEinfügeTreffer(kontext, editor.karte, neu).single()
            assertFalse(treffer.direkt); assertEquals(1, treffer.wege.size)
            assertTrue(editor.bestätigeVerbindungsEinfügung(kontext, editor.karte, neu.id, ref(neu), 0))
            assertEquals(3, editor.karte.knoten.size); assertEquals(2, editor.karte.verbindungen.size)
            assertEquals("grad", übersetzerZiel(editor.karte.knoten.single { it.art == ÜBERSETZER_ART }))
            editor.rückgängig(); assertEquals(basis, editor.karte); assertFalse(editor.kannRückgängig())
            editor.wiederholen(); assertEquals(3, editor.karte.knoten.size)
        }
    }
    @Test fun mehrereRealeUebersetzerwegeBleibenBisZurAuswahlUnveraendert() {
        val start = k(MathematikAnschlussArten.Matrix.id, false); val neu = k(MathematikAnschlussArten.Methode.id, true)
        val editor = KartenEditorZustand(KartenDaten(name = "Wege", knoten = listOf(start)), laufzeit.graphPrüfung)
        val planer = ÜbersetzerVerbindungsPlaner(laufzeit.graphPrüfung)
        editor.verbindungsPlaner = planer::vorschläge; editor.verbindungÜberAdapterMöglich = planer::möglich
        val basis = editor.karte
        editor.beginneVerbindung(ref(start)); val kontext = assertNotNull(editor.übergibVerbindungZurEinfügung())
        editor.beginneVerbindungsEinfügeZiehen(neu, kontext)
        val treffer = editor.verbindungsEinfügeTreffer(kontext, editor.karte, neu).single()
        assertTrue(treffer.wege.size >= 2); assertTrue(editor.karte.verbindungen.isEmpty())
        assertTrue(editor.bestätigeVerbindungsEinfügung(kontext, editor.karte, neu.id, ref(neu), 1))
        assertEquals(3, editor.karte.knoten.size)
        editor.rückgängig(); assertEquals(basis, editor.karte)
    }
}
