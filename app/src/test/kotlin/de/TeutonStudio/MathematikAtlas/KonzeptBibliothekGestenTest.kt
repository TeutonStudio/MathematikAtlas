package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KonzeptBibliothekGestenTest {
    @Test fun `kurzer Klick fügt exakt einmal ein`() {
        val automat = KonzeptGestenAutomat(8f)
        automat.drücken()
        assertEquals(listOf(KonzeptGestenEffekt.Einfügen), automat.loslassen())
        assertTrue(automat.loslassen().isEmpty())
    }

    @Test fun `Bewegung vor Haltezeit wird Scrollabbruch`() {
        val automat = KonzeptGestenAutomat(8f)
        automat.drücken()
        assertTrue(automat.bewegen(8f).isEmpty())
        assertEquals(KonzeptGestenZustand.Gedrückt, automat.zustand)
        assertTrue(automat.bewegen(8.01f).isEmpty())
        assertEquals(KonzeptGestenZustand.Abgebrochen, automat.zustand)
        assertTrue(automat.loslassen().isEmpty())
    }

    @Test fun `Halten ohne Drag öffnet nur Definition`() {
        val automat = KonzeptGestenAutomat(8f)
        automat.drücken()
        automat.haltezeitErreicht()
        assertEquals(listOf(KonzeptGestenEffekt.DefinitionÖffnen), automat.loslassen())
    }

    @Test fun `Halten und Bewegung startet und beendet Drag exklusiv`() {
        val automat = KonzeptGestenAutomat(8f)
        automat.drücken()
        automat.haltezeitErreicht()
        assertEquals(listOf(KonzeptGestenEffekt.DragBeginnen), automat.bewegen(9f))
        assertEquals(listOf(KonzeptGestenEffekt.DragVerschieben), automat.bewegen(10f))
        assertEquals(listOf(KonzeptGestenEffekt.DragBeenden), automat.loslassen())
    }

    @Test fun `Cancel nach Drag fordert Abbruch an`() {
        val automat = KonzeptGestenAutomat(1f)
        automat.drücken()
        automat.haltezeitErreicht()
        automat.bewegen(2f)
        assertEquals(listOf(KonzeptGestenEffekt.DragAbbrechen), automat.abbrechen())
        assertEquals(KonzeptGestenZustand.Bereit, automat.zustand)
    }

    @Test fun `Schwellen folgen Pointerpolicy`() {
        assertEquals(12f, KonzeptGestenSchwellen.für(KonzeptZeigerArt.Touch, 12f).bewegungDp)
        assertEquals(9f, KonzeptGestenSchwellen.für(KonzeptZeigerArt.Stift, 12f).bewegungDp)
        assertEquals(6f, KonzeptGestenSchwellen.für(KonzeptZeigerArt.Maus, 12f).bewegungDp)
        assertEquals(3f, KonzeptGestenSchwellen.für(KonzeptZeigerArt.Stift, 2f).bewegungDp)
        assertEquals(2f, KonzeptGestenSchwellen.für(KonzeptZeigerArt.Maus, 2f).bewegungDp)
        assertEquals(12f, KonzeptGestenSchwellen.für(KonzeptZeigerArt.Unbekannt, 12f).bewegungDp)
    }
}
