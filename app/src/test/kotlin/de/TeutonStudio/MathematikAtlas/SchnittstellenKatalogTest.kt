package de.TeutonStudio.MathematikAtlas

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchnittstellenKatalogTest {
    private val ordner: Path by lazy {
        listOf(
            Path.of("src/debug/kotlin/de/TeutonStudio/MathematikAtlas/schnittstellen"),
            Path.of("app/src/debug/kotlin/de/TeutonStudio/MathematikAtlas/schnittstellen"),
        ).firstOrNull { it.exists() }
            ?: error("Der Debug-Schnittstellenkatalog wurde nicht gefunden.")
    }

    @Test
    fun katalogEnthältEigenständigePreviewDateien() {
        val dateien = previewDateien()

        assertTrue(dateien.size >= 18, "Der Katalog muss die wesentlichen sichtbaren Oberflächen abdecken.")
        dateien.forEach { datei ->
            val inhalt = datei.readText()
            assertTrue(
                "@Preview" in inhalt || "@HelleVorschau" in inhalt || "@DunkleVorschau" in inhalt,
                "${datei.name} besitzt keine Compose-Preview.",
            )
            assertFalse("VorschauInhalt" in inhalt, "${datei.name} darf keine nachgebaute Schattenoberfläche enthalten.")
        }
    }

    @Test
    fun readmeDokumentiertJedePreviewUndIhreTestdaten() {
        val readme = ordner.resolve("README.md").readText()

        assertTrue("Aussagekräftige Testdaten" in readme)
        previewDateien().forEach { datei ->
            assertTrue(datei.name in readme, "${datei.name} fehlt im Schnittstellenkatalog.")
        }
    }

    @Test
    fun vorschauDatenSindDeterministischUndKeinePlatzhalter() {
        val texte = Files.list(ordner).use { pfade ->
            pfade
                .filter { Files.isRegularFile(it) }
                .map { it.readText() }
                .toList()
        }.joinToString("\n")

        listOf("Lorem ipsum", "testData", "foo@example", "System.currentTimeMillis()", "UUID.randomUUID()")
            .forEach { verboten ->
                assertFalse(verboten in texte, "Ungeeignete oder nichtdeterministische Vorschau-Daten: $verboten")
            }
        listOf(
            "Primzahlen unter zehn",
            "Lineares Gleichungssystem mit Gauß-Verfahren",
            "Summe der Quadratzahlen",
            "Matrixoperationen und Auswertung",
        ).forEach { erwartet ->
            assertTrue(erwartet in texte, "Aussagekräftiges Testszenario fehlt: $erwartet")
        }
    }

    @Test
    fun produktionscodeEnthältKeinenVorschaukatalog() {
        val möglicheMainOrdner = listOf(
            Path.of("src/main/kotlin/de/TeutonStudio/MathematikAtlas/schnittstellen"),
            Path.of("app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/schnittstellen"),
        )
        assertTrue(möglicheMainOrdner.none { it.exists() })
    }

    private fun previewDateien(): List<Path> = Files.list(ordner).use { pfade ->
        pfade
            .filter { Files.isRegularFile(it) }
            .filter { it.fileName.toString().endsWith("Vorschau.kt") }
            .sorted()
            .toList()
    }
}
