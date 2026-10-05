package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class MatlasKartenContainerTest {
    @Test
    fun `Container ist deterministisch und Manifest referenziert Karten JSON`() {
        val karte = KartenDaten(
            id = KartenId("karte-1"),
            name = "Testkarte",
            version = 7,
            erstelltAm = 1234L,
        )

        val erster = MatlasKartenContainer.schreibe(karte, "2.32.1")
        val zweiter = MatlasKartenContainer.schreibe(karte, "2.32.1")
        assertContentEquals(erster, zweiter)

        val dateien = zipDateien(erster)
        assertEquals(
            setOf(MatlasKartenContainer.MANIFEST_DATEI, MatlasKartenContainer.KARTEN_DATEI),
            dateien.keys,
        )

        val manifest = JSONObject(dateien.getValue(MatlasKartenContainer.MANIFEST_DATEI).toString(StandardCharsets.UTF_8))
        assertEquals(MatlasKartenContainer.FORMAT_ID, manifest.getString("format"))
        assertEquals(MatlasKartenContainer.FORMAT_VERSION, manifest.getInt("formatVersion"))
        assertEquals("2.32.1", manifest.getString("erstellerVersion"))
        assertEquals("karte-1", manifest.getJSONObject("karte").getString("id"))
        assertEquals("Testkarte", manifest.getJSONObject("karte").getString("titel"))
        assertEquals(7, manifest.getJSONObject("karte").getInt("version"))

        val kartenJson = dateien.getValue(MatlasKartenContainer.KARTEN_DATEI)
        val kartenEintrag = manifest.getJSONArray("dateien").getJSONObject(0)
        assertEquals(MatlasKartenContainer.KARTEN_DATEI, kartenEintrag.getString("pfad"))
        assertEquals("karte", kartenEintrag.getString("rolle"))
        assertEquals(kartenJson.size, kartenEintrag.getInt("bytes"))
        assertEquals(MatlasKartenContainer.sha256(kartenJson), kartenEintrag.getString("sha256"))
        assertTrue(kartenEintrag.getString("sha256").matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `atomarer Writer ersetzt Ziel und hinterlaesst keine temporaere Datei`() {
        val ordner = Files.createTempDirectory("matlas-test")
        try {
            val ziel = ordner.resolve("karte.matlas")
            Files.write(ziel, "alt".toByteArray(StandardCharsets.UTF_8))
            val karte = KartenDaten(id = KartenId("atomar"), name = "Atomar")

            MatlasKartenContainer.schreibeAtomar(ziel, karte, "test")

            assertContentEquals(MatlasKartenContainer.schreibe(karte, "test"), ziel.readBytes())
            assertFalse(ordner.listDirectoryEntries().any { it.fileName.toString().endsWith(".tmp") })
        } finally {
            ordner.toFile().deleteRecursively()
        }
    }

    @Test
    fun `container kann validiert und verlustfrei gelesen werden`() {
        val karte = KartenDaten(id = KartenId("lesbar"), name = "Lesbar", version = 3, erstelltAm = 42L)
        assertEquals(karte, MatlasKartenContainer.lese(MatlasKartenContainer.schreibe(karte, "test")))
    }

    @Test
    fun `manipulierte container werden wegen pruefsumme abgelehnt`() {
        val karte = KartenDaten(id = KartenId("manipuliert"), name = "Original")
        val dateien = zipDateien(MatlasKartenContainer.schreibe(karte, "test")).toMutableMap()
        dateien[MatlasKartenContainer.KARTEN_DATEI] = "{}".toByteArray()
        val manipuliert = ByteArrayOutputStream().use { ziel ->
            ZipOutputStream(ziel).use { zip ->
                dateien.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
                }
            }
            ziel.toByteArray()
        }
        assertFailsWith<IllegalArgumentException> { MatlasKartenContainer.lese(manipuliert) }
    }

    private fun zipDateien(container: ByteArray): Map<String, ByteArray> {
        val ergebnis = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(container), StandardCharsets.UTF_8).use { zip ->
            var eintrag = zip.nextEntry
            while (eintrag != null) {
                assertFalse(eintrag.name.startsWith('/'))
                assertFalse(".." in eintrag.name.split('/'))
                ergebnis[eintrag.name] = zip.readBytes()
                zip.closeEntry()
                eintrag = zip.nextEntry
            }
        }
        return ergebnis
    }
}
