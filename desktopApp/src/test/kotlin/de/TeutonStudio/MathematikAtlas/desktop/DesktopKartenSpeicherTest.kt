package de.TeutonStudio.MathematikAtlas.desktop

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.nio.file.Files
import kotlin.test.*

class DesktopKartenSpeicherTest {
    @Test fun `Speichern Neustart und Versionsfolge bleiben erhalten`() {
        val basis = Files.createTempDirectory("atlas-desktop-test")
        val erster = DesktopKartenSpeicher(basis)
        val karte = KartenDaten(name = "Persistenz", knoten = listOf(KnotenDaten(art = KnotenArtId("test"), name = "A")))
        val v1 = erster.speichere(karte)
        val v2 = erster.speichere(v1.copy(name = "Persistenz 2"))
        val nachNeustart = DesktopKartenSpeicher(basis).ladeAktuell()
        assertEquals(v1.version + 1, v2.version)
        assertEquals(v2, nachNeustart)
        assertEquals(v1, erster.lade(KartenVerweis(v1.id, v1.version)))
    }

    @Test fun `Ungültiger Import verändert gespeicherte Karte nicht`() {
        val basis = Files.createTempDirectory("atlas-desktop-import")
        val speicher = DesktopKartenSpeicher(basis)
        val karte = speicher.speichere(KartenDaten(name = "Sicher"))
        assertFails { speicher.importiere("kein json") }
        assertEquals(karte, speicher.ladeAktuell())
    }
}
