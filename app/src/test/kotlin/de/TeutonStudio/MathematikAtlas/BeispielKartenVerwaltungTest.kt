package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnung
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BeispielKartenVerwaltungTest {
    @Test
    fun `kleinste freie Beispielkarten Nummer wird verwendet`() {
        val ordnung = KartenOrdnung()
            .mitOrdner(listOf("Beispiel Karten"))
            .mitOrdner(listOf("Beispiel Karten 3"))

        assertEquals(listOf("Beispiel Karten 2"), eindeutigerBeispielKartenOrdner(ordnung))
    }

    @Test
    fun `erzeugter Satz landet gemeinsam im neuen Ordner`() {
        val alt = KartenDaten(id = KartenId("alt"), name = "Vorhanden")
        val neuA = KartenDaten(id = KartenId("neu-a"), name = "A")
        val neuB = KartenDaten(id = KartenId("neu-b"), name = "B")
        var ordnung = KartenOrdnung().mitKarteInOrdner(alt.id, listOf("Eigene Karten"))
        val gespeichert = mutableListOf<KartenDaten>()
        val gelöscht = mutableSetOf<KartenId>()

        val verwaltung = BeispielKartenVerwaltung(
            ladeOrdnung = { ordnung },
            speichereOrdnung = { ordnung = it },
            speichereKarte = { karte -> gespeichert += karte; karte },
            löscheKarten = { gelöscht += it },
            erzeugeKarten = { listOf(neuA, neuB) },
        )

        val ergebnis = verwaltung.erstelleNeu()

        assertEquals(listOf("Beispiel Karten"), ergebnis.ordnerPfad)
        assertEquals(2, ergebnis.anzahl)
        assertEquals(listOf(neuA, neuB), gespeichert)
        assertTrue(gelöscht.isEmpty())
        assertEquals(listOf("Eigene Karten"), ordnung.ordnerFür(alt.id))
        assertEquals(listOf("Beispiel Karten"), ordnung.ordnerFür(neuA.id))
        assertEquals(listOf("Beispiel Karten"), ordnung.ordnerFür(neuB.id))
    }

    @Test
    fun `Fehler beim Kartenspeichern rollt bereits erzeugte Karten zurück`() {
        val neuA = KartenDaten(id = KartenId("neu-a"), name = "A")
        val neuB = KartenDaten(id = KartenId("neu-b"), name = "B")
        val vorher = KartenOrdnung().mitOrdner(listOf("Vorhanden"))
        var ordnung = vorher
        val gelöscht = mutableSetOf<KartenId>()
        var aufruf = 0

        val verwaltung = BeispielKartenVerwaltung(
            ladeOrdnung = { ordnung },
            speichereOrdnung = { ordnung = it },
            speichereKarte = { karte ->
                aufruf += 1
                if (aufruf == 2) error("Speichern fehlgeschlagen")
                karte
            },
            löscheKarten = { gelöscht += it },
            erzeugeKarten = { listOf(neuA, neuB) },
        )

        assertFailsWith<IllegalStateException> { verwaltung.erstelleNeu() }

        assertEquals(setOf(neuA.id), gelöscht)
        assertEquals(vorher, ordnung)
    }

    @Test
    fun `Fehler beim Ordnungsspeichern löscht den vollständigen neuen Satz`() {
        val neuA = KartenDaten(id = KartenId("neu-a"), name = "A")
        val neuB = KartenDaten(id = KartenId("neu-b"), name = "B")
        val vorher = KartenOrdnung().mitOrdner(listOf("Vorhanden"))
        var ordnung = vorher
        val gelöscht = mutableSetOf<KartenId>()

        val verwaltung = BeispielKartenVerwaltung(
            ladeOrdnung = { ordnung },
            speichereOrdnung = { neu ->
                if (neu != vorher) error("Ordnung fehlgeschlagen")
                ordnung = neu
            },
            speichereKarte = { it },
            löscheKarten = { gelöscht += it },
            erzeugeKarten = { listOf(neuA, neuB) },
        )

        assertFailsWith<IllegalStateException> { verwaltung.erstelleNeu() }

        assertEquals(setOf(neuA.id, neuB.id), gelöscht)
        assertEquals(vorher, ordnung)
    }
}
