package com.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenGrenzenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahlImBereich
import com.TeutonStudio.KnotenKartenVerwalter.daten.dupliziereAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.daten.loescheAuswahl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KartenBearbeitungTest {
    @Test
    fun auswahlImBereichWaehltKnotenUndInterneVerbindungen() {
        val auswahl = beispielKarte().auswahlImBereich(
            KartenGrenzenDaten(links = 0f, oben = 0f, rechts = 160f, unten = 120f),
        )

        assertEquals(setOf("a", "b"), auswahl.knotenIds)
        assertEquals(setOf("ab"), auswahl.verbindungIds)
    }

    @Test
    fun auswahlImBereichKannBestehendeAuswahlErweitern() {
        val auswahl = beispielKarte().auswahlImBereich(
            bereich = KartenGrenzenDaten(links = 0f, oben = 0f, rechts = 70f, unten = 70f),
            bestehendeAuswahl = AuswahlDaten(knotenIds = setOf("c")),
            erweitern = true,
        )

        assertEquals(setOf("a", "c"), auswahl.knotenIds)
    }

    @Test
    fun loeschenEntferntAusgewaehlteKnotenUndAngrenzendeVerbindungen() {
        val karte = beispielKarte().loescheAuswahl(AuswahlDaten(knotenIds = setOf("b")))

        assertEquals(listOf("a", "c"), karte.knoten.map { it.id })
        assertTrue(karte.verbindungen.isEmpty())
    }

    @Test
    fun duplizierenErzeugtNeueKnotenUndInterneVerbindungen() {
        val ids = mutableListOf("a2", "b2", "ab2")
        val ergebnis = beispielKarte().dupliziereAuswahl(
            auswahl = AuswahlDaten(knotenIds = setOf("a", "b")),
            verschiebung = Offset(10f, 20f),
            neueId = { ids.removeAt(0) },
        )

        assertEquals(setOf("a2", "b2"), ergebnis.auswahl.knotenIds)
        assertEquals(setOf("ab2"), ergebnis.auswahl.verbindungIds)
        assertEquals(5, ergebnis.karte.knoten.size)
        assertEquals(3, ergebnis.karte.verbindungen.size)
        assertEquals(Offset(10f, 20f), ergebnis.karte.knoten.first { it.id == "a2" }.position)
        assertEquals(Offset(110f, 20f), ergebnis.karte.knoten.first { it.id == "b2" }.position)
    }

    private fun beispielKarte() = KarteDaten(
        id = "karte",
        name = "Karte",
        knoten = listOf(
            KnotenDaten(id = "a", name = "A", position = Offset(0f, 0f), fläche = Offset(40f, 40f)),
            KnotenDaten(id = "b", name = "B", position = Offset(100f, 0f), fläche = Offset(40f, 40f)),
            KnotenDaten(id = "c", name = "C", position = Offset(300f, 300f), fläche = Offset(40f, 40f)),
        ),
        verbindungen = listOf(
            VerbindungDaten("ab", "a", "out", "b", "in"),
            VerbindungDaten("bc", "b", "out", "c", "in"),
        ),
    )
}
