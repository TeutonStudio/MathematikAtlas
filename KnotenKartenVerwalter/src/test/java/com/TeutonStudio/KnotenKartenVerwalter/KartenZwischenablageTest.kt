package com.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fuegeEin
import com.TeutonStudio.KnotenKartenVerwalter.daten.kopiereAuswahl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KartenZwischenablageTest {
    @Test
    fun kopiertAusgewaehlteKnotenUndInterneVerbindungen() {
        val karte = beispielKarte()

        val zwischenablage = karte.kopiereAuswahl(AuswahlDaten(knotenIds = setOf("a", "b")))

        assertEquals(listOf("a", "b"), zwischenablage.knoten.map { it.id })
        assertEquals(listOf("ab"), zwischenablage.verbindungen.map { it.id })
    }

    @Test
    fun einfuegenErzeugtNeueIdsUndVerschiebtRelativZurZielposition() {
        val ids = mutableListOf("a-neu", "b-neu", "ab-neu")
        val zwischenablage = beispielKarte().kopiereAuswahl(AuswahlDaten(knotenIds = setOf("a", "b")))

        val ergebnis = KarteDaten(id = "ziel", name = "Ziel").fuegeEin(
            zwischenablage = zwischenablage,
            zielPosition = Offset(100f, 200f),
            neueId = { ids.removeAt(0) },
        )

        assertEquals(setOf("a-neu", "b-neu"), ergebnis.auswahl.knotenIds)
        assertEquals(setOf("ab-neu"), ergebnis.auswahl.verbindungIds)
        assertEquals(Offset(100f, 200f), ergebnis.karte.knoten[0].position)
        assertEquals(Offset(140f, 230f), ergebnis.karte.knoten[1].position)
        assertEquals("a-neu", ergebnis.karte.verbindungen.single().quellKnotenId)
        assertEquals("b-neu", ergebnis.karte.verbindungen.single().zielKnotenId)
        assertTrue(ergebnis.karte.knoten.all { it.ausgewaehlt })
        assertTrue(ergebnis.karte.verbindungen.all { it.ausgewaehlt })
    }

    private fun beispielKarte() = KarteDaten(
        id = "karte",
        name = "Karte",
        knoten = listOf(
            KnotenDaten(
                id = "a",
                name = "A",
                position = Offset(10f, 20f),
            ),
            KnotenDaten(
                id = "b",
                name = "B",
                position = Offset(50f, 50f),
            ),
            KnotenDaten(
                id = "c",
                name = "C",
                position = Offset(200f, 200f),
            ),
        ),
        verbindungen = listOf(
            VerbindungDaten(
                id = "ab",
                quellKnotenId = "a",
                quellAnschlussId = "out",
                zielKnotenId = "b",
                zielAnschlussId = "in",
            ),
            VerbindungDaten(
                id = "bc",
                quellKnotenId = "b",
                quellAnschlussId = "out",
                zielKnotenId = "c",
                zielAnschlussId = "in",
            ),
        ),
    )
}
