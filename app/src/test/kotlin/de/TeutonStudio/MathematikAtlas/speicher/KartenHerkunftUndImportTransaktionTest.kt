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
    @Test
    fun `Abbruch nach jeder Bestandsphase stellt alle fuenf Ziele bytegenau wieder her`() {
        val ziele = listOf("karten", "sicherungen", "karten-ordnung.json", "freigabe-quellen.json", "karten-herkunft.json")
        for (vorhanden in listOf(true, false)) for (phase in ziele.indices) {
            val basis = Files.createTempDirectory("matlas-import-phase").toFile()
            try {
                if (vorhanden) ziele.forEachIndexed { index, name ->
                    val datei = if (index < 2) basis.resolve("$name/alt/v1.json") else basis.resolve(name)
                    datei.parentFile.mkdirs()
                    datei.writeBytes(byteArrayOf(0, index.toByte(), -1, 10))
                }
                fun bestand() = ziele.flatMap { name ->
                    val ziel = basis.resolve(name)
                    if (!ziel.exists()) emptyList() else if (ziel.isDirectory) ziel.walkTopDown().filter { it.isFile }.toList() else listOf(ziel)
                }.associate { it.relativeTo(basis).invariantSeparatorsPath to it.readBytes().toList() }
                val vorher = bestand()
                val transaktionen = KartenImportTransaktionen(basis)
                assertFailsWith<IllegalStateException>("vorhanden=$vorhanden phase=$phase") {
                    transaktionen.ausführen {
                        ziele.take(phase + 1).forEachIndexed { index, name ->
                            val ziel = basis.resolve(name)
                            ziel.deleteRecursively()
                            val neu = if (index < 2) ziel.resolve("neu/v2.json") else ziel
                            neu.parentFile.mkdirs()
                            neu.writeText("neu-$index")
                        }
                        error("Abbruch nach ${ziele[phase]}")
                    }
                }
                assertEquals(vorher, bestand(), "vorhanden=$vorhanden phase=$phase")
                assertTrue(transaktionen.wiederherstellungsFehler.isEmpty())
                assertTrue(basis.resolve("import-transaktionen").listFiles().orEmpty().isEmpty())
                // Die Wiederholung nach erfolgreichem Rollback benötigt keine Sonderbehandlung.
                KartenImportTransaktionen(basis).ausführen { assertEquals(vorher, bestand()) }
            } finally { basis.deleteRecursively() }
        }
    }

    @Test
    fun `kaputtes Journal erhält den Bestand und blockiert weitere Importe`() {
        val basis = Files.createTempDirectory("matlas-kaputtes-journal").toFile()
        try {
            basis.resolve("karten").mkdirs()
            basis.resolve("karten/vorher.json").writeText("wertvoll")
            basis.resolve("import-transaktionen/defekt").mkdirs()
            basis.resolve("import-transaktionen/defekt/journal.json").writeText("{kaputt")
            val transaktionen = KartenImportTransaktionen(basis)
            assertEquals("wertvoll", basis.resolve("karten/vorher.json").readText())
            assertTrue(transaktionen.wiederherstellungsFehler.isNotEmpty())
            assertFailsWith<IllegalStateException> { transaktionen.ausführen { error("darf nicht beginnen") } }
            assertTrue(basis.resolve("import-transaktionen/defekt/journal.json").exists())
        } finally { basis.deleteRecursively() }
    }

    @Test
    fun `Vorbereitungsabbruch ohne Journal verändert keine Bestandsdatei`() {
        val basis = Files.createTempDirectory("matlas-vorbereitung").toFile()
        try {
            basis.resolve("karten").mkdirs()
            basis.resolve("karten/wert.json").writeText("wertvoll")
            basis.resolve("import-transaktionen/abbruch/vorher/karten").mkdirs()
            KartenImportTransaktionen(basis)
            assertEquals("wertvoll", basis.resolve("karten/wert.json").readText())
        } finally { basis.deleteRecursively() }
    }

    @Test
    fun `unvollständige Sicherung wird vor jeder Bestandsänderung abgewiesen`() {
        val basis = Files.createTempDirectory("matlas-sicherung").toFile()
        try {
            basis.resolve("karten").mkdirs()
            basis.resolve("karten/wert.json").writeText("wertvoll")
            basis.resolve("import-transaktionen/abbruch").mkdirs()
            basis.resolve("import-transaktionen/abbruch/journal.json").writeText(
                """{"formatVersion":1,"status":"VORBEREITET","vorhanden":["karten"],"pruefsummen":{"karten":"falsch"}}""",
            )
            val transaktionen = KartenImportTransaktionen(basis)
            assertTrue(transaktionen.wiederherstellungsFehler.isNotEmpty())
            assertEquals("wertvoll", basis.resolve("karten/wert.json").readText())
        } finally { basis.deleteRecursively() }
    }

    @Test
    fun `Linkimport weist private lokale und spezielle Adressen ab`() {
        listOf("127.0.0.1", "10.0.0.1", "192.168.1.1", "172.16.0.1", "169.254.1.1", "100.64.0.1", "::1", "fc00::1", "fe80::1").forEach {
            assertFalse(SichererKartenAbruf.istÖffentlich(java.net.InetAddress.getByName(it)), it)
        }
        assertTrue(SichererKartenAbruf.istÖffentlich(java.net.InetAddress.getByName("8.8.8.8")))
        assertFailsWith<IllegalArgumentException> { SichererKartenAbruf.prüfeHttpsAdresse("https://localhost/a") }
    }

    @Test
    fun `kontrolliertes DNS gibt ausschliesslich gepruefte Transportadressen zurueck`() {
        val öffentlich = java.net.InetAddress.getByName("8.8.8.8")
        val privat = java.net.InetAddress.getByName("127.0.0.1")
        var aufrufe = 0
        val dns = SichererKartenAbruf.öffentlichesDns { if (aufrufe++ == 0) listOf(öffentlich) else listOf(privat) }
        assertEquals(listOf(öffentlich), dns.lookup("example.org"))
        assertFailsWith<IllegalArgumentException> { dns.lookup("example.org") }
        val gemischt = SichererKartenAbruf.öffentlichesDns { listOf(öffentlich, privat) }
        assertFailsWith<IllegalArgumentException> { gemischt.lookup("example.org") }
    }

}
