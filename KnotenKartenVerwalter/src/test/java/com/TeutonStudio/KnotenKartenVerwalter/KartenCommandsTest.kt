package com.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlEinfuegen
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlLoeschen
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenControllerZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVerschieben
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.kopiereAuswahl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KartenCommandsTest {
    @Test
    fun controllerFuehrtCommandAusUndKannRueckgaengigMachen() {
        val controller = KartenControllerZustand(beispielKarte())
            .fuehreAus(KnotenErstellen(KnotenDaten(id = "c", name = "C")))

        assertEquals(listOf("a", "b", "c"), controller.karte.knoten.map { it.id })
        assertEquals(setOf("c"), controller.auswahl.knotenIds)
        assertTrue(controller.kannRueckgaengig)

        val rueckgaengig = controller.rueckgaengig()

        assertEquals(listOf("a", "b"), rueckgaengig.karte.knoten.map { it.id })
        assertTrue(rueckgaengig.kannWiederholen)

        val wiederholt = rueckgaengig.wiederholen()

        assertEquals(listOf("a", "b", "c"), wiederholt.karte.knoten.map { it.id })
        assertEquals(setOf("c"), wiederholt.auswahl.knotenIds)
    }

    @Test
    fun knotenVerschiebenSelektiertVerschobenenKnoten() {
        val controller = KartenControllerZustand(beispielKarte())
            .fuehreAus(KnotenVerschieben("a", Offset(50f, 60f)))

        assertEquals(Offset(50f, 60f), controller.karte.knoten.first { it.id == "a" }.position)
        assertEquals(setOf("a"), controller.auswahl.knotenIds)
    }

    @Test
    fun verbindungErstellenErsetztBestehendenEingang() {
        val verbindung = VerbindungDaten("ac", "a", "out", "b", "in")
        val controller = KartenControllerZustand(beispielKarte())
            .fuehreAus(VerbindungErstellen(verbindung))

        assertEquals(listOf("ac"), controller.karte.verbindungen.map { it.id })
        assertEquals(setOf("ac"), controller.auswahl.verbindungIds)
    }

    @Test
    fun auswahlEinfuegenUndLoeschenLaufenDurchHistory() {
        val ids = mutableListOf("a2", "b2", "ab2")
        val zwischenablage = beispielKarte().kopiereAuswahl(AuswahlDaten(knotenIds = setOf("a", "b")))
        val eingefuegt = KartenControllerZustand(KarteDaten(id = "ziel", name = "Ziel"))
            .fuehreAus(AuswahlEinfuegen(zwischenablage, Offset(100f, 100f), neueId = { ids.removeAt(0) }))

        assertEquals(setOf("a2", "b2"), eingefuegt.auswahl.knotenIds)
        assertEquals(2, eingefuegt.karte.knoten.size)
        assertEquals(1, eingefuegt.karte.verbindungen.size)

        val geloescht = eingefuegt.fuehreAus(AuswahlLoeschen(eingefuegt.auswahl))

        assertTrue(geloescht.karte.knoten.isEmpty())
        assertTrue(geloescht.karte.verbindungen.isEmpty())
        assertFalse(geloescht.rueckgaengig().karte.knoten.isEmpty())
    }

    @Test
    fun undoRedoKannDeaktiviertWerden() {
        val controller = KartenControllerZustand(beispielKarte(), undoRedoAktiv = false)
            .fuehreAus(KnotenErstellen(KnotenDaten(id = "c", name = "C")))

        assertEquals(3, controller.karte.knoten.size)
        assertFalse(controller.kannRueckgaengig)
        assertTrue(controller.undoStack.isEmpty())
    }

    private fun beispielKarte() = KarteDaten(
        id = "karte",
        name = "Karte",
        knoten = listOf(
            KnotenDaten(id = "a", name = "A", position = Offset(0f, 0f)),
            KnotenDaten(id = "b", name = "B", position = Offset(100f, 0f)),
        ),
        verbindungen = listOf(
            VerbindungDaten("ab", "a", "out", "b", "in"),
        ),
    )
}
