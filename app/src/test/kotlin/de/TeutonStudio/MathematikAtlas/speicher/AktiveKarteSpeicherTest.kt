package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AktiveKarteSpeicherTest {
    @Test fun `gespeicherte Karten-ID wird verlustfrei wiederhergestellt`() {
        val ordner = Files.createTempDirectory("aktive-karte").toFile()
        val speicher = AktiveKarteSpeicher(ordner)

        speicher.speichere(KartenId("karte-a"))

        assertEquals(KartenId("karte-a"), AktiveKarteSpeicher(ordner).lade())
    }

    @Test fun `fehlende oder defekte Referenz bleibt kontrolliert leer`() {
        val ordner = Files.createTempDirectory("aktive-karte").toFile()
        val speicher = AktiveKarteSpeicher(ordner)

        assertNull(speicher.lade())
        ordner.resolve("aktive-karte.json").writeText("kein JSON")
        assertNull(speicher.lade())
        speicher.löschen()
        assertNull(speicher.lade())
    }
}
