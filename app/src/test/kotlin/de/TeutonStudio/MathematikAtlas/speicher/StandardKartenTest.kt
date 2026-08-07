package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StandardKartenTest {
    @Test
    fun `Inhaltshash ignoriert Laufzeitmetadaten aber erkennt Nutzerinhalt`() {
        val basis = KartenDaten(
            id = KartenId("standard-test"),
            name = "Standardkarte",
            version = 1,
            erstelltAm = 1L,
        )

        val nurLaufzeitmetadaten = basis.copy(
            version = 9,
            erstelltAm = 987654321L,
            archiviert = true,
        )
        val nutzerGeändert = basis.copy(name = "Meine bearbeitete Standardkarte")

        assertEquals(
            standardKartenInhaltsHash(basis),
            standardKartenInhaltsHash(nurLaufzeitmetadaten),
        )
        assertNotEquals(
            standardKartenInhaltsHash(basis),
            standardKartenInhaltsHash(nutzerGeändert),
        )
    }

    @Test
    fun `Provenienz roundtript aktive geänderte und gelöschte Karten`() {
        val ordner = Files.createTempDirectory("standardkarten-test").toFile()
        try {
            val datei = File(ordner, "standardkarten-quellen.json")
            val speicher = StandardKartenProvenienzSpeicher(datei)
            val einträge = listOf(
                InstallierteStandardKarte(
                    sourceId = "standard.a",
                    sourceVersion = "1.0.0",
                    sourceHash = "quelle-a",
                    lokaleKartenId = KartenId("lokal-a"),
                    installierterHash = "inhalt-a",
                    status = StandardKartenStatus.AKTIV,
                ),
                InstallierteStandardKarte(
                    sourceId = "standard.b",
                    sourceVersion = "1.0.0",
                    sourceHash = "quelle-b",
                    lokaleKartenId = KartenId("lokal-b"),
                    installierterHash = "inhalt-b",
                    status = StandardKartenStatus.BENUTZERGEÄNDERT,
                ),
                InstallierteStandardKarte(
                    sourceId = "standard.c",
                    sourceVersion = "1.0.0",
                    sourceHash = "quelle-c",
                    lokaleKartenId = KartenId("lokal-c"),
                    installierterHash = "inhalt-c",
                    status = StandardKartenStatus.GELÖSCHT,
                ),
            )

            speicher.speichere(einträge)

            assertEquals(einträge, speicher.liste())
        } finally {
            ordner.deleteRecursively()
        }
    }
}
