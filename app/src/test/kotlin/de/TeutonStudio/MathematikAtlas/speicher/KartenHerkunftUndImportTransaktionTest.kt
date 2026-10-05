package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class KartenHerkunftUndImportTransaktionTest {
    @Test
    fun `Linkimport akzeptiert nur HTTPS ohne eingebettete Anmeldedaten`() {
        assertEquals("example.org", SichererKartenAbruf.prüfeHttpsAdresse("https://example.org/karte.matlas").host)
        assertFailsWith<IllegalArgumentException> { SichererKartenAbruf.prüfeHttpsAdresse("http://example.org/karte.json") }
        assertFailsWith<IllegalArgumentException> { SichererKartenAbruf.prüfeHttpsAdresse("https://name:pass@example.org/karte.json") }
    }

    @Test
    fun `externe Herkunft wird versioniert und verlustfrei gelesen`() {
        val basis = Files.createTempDirectory("matlas-herkunft").toFile()
        try {
            val speicher = KartenHerkunftSpeicher(basis.resolve("karten-herkunft.json"))
            val id = KartenId("fremd-1")
            speicher.markiereFremd(setOf(id), setOf(listOf("Freigaben", "Ada")))

            assertEquals(
                ExterneKartenHerkunft(setOf(id), setOf(listOf("Freigaben", "Ada"))),
                speicher.lade(),
            )
            speicher.entferne(setOf(id))
            assertFalse(id in speicher.lade().fremdeKarten)
            assertTrue(listOf("Freigaben", "Ada") in speicher.lade().fremdeOrdner)
        } finally {
            basis.deleteRecursively()
        }
    }

    @Test
    fun `fehlgeschlagener Import stellt Karten Ordnung und Herkunft gemeinsam wieder her`() {
        val basis = Files.createTempDirectory("matlas-import").toFile()
        try {
            basis.resolve("karten").mkdirs()
            basis.resolve("karten/vorher.json").writeText("vorher")
            basis.resolve("karten-ordnung.json").writeText("ordnung-vorher")
            basis.resolve("karten-herkunft.json").writeText("herkunft-vorher")
            val transaktionen = KartenImportTransaktionen(basis)

            runCatching {
                transaktionen.ausführen {
                    basis.resolve("karten/vorher.json").writeText("nachher")
                    basis.resolve("karten/neu.json").writeText("neu")
                    basis.resolve("karten-ordnung.json").writeText("ordnung-nachher")
                    basis.resolve("karten-herkunft.json").writeText("herkunft-nachher")
                    error("simulierter Abbruch")
                }
            }

            assertEquals("vorher", basis.resolve("karten/vorher.json").readText())
            assertFalse(basis.resolve("karten/neu.json").exists())
            assertEquals("ordnung-vorher", basis.resolve("karten-ordnung.json").readText())
            assertEquals("herkunft-vorher", basis.resolve("karten-herkunft.json").readText())
        } finally {
            basis.deleteRecursively()
        }
    }
}
