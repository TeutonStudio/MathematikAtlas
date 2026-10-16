package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseKnotenKatalogTest {
    @Test fun `Releasekatalog entspricht den wirklich erzeugbaren Typen`() {
        val typen = KanonischerMathematikKnotenKatalog.alle().map { it.art }.distinct().sorted()
        System.getenv("ATLAS_KNOTEN_KATALOG_EXPORT")?.let { pfad ->
            File(pfad).writeText(typen.joinToString(prefix = "[\n", postfix = "\n]\n", separator = ",\n") { "  \"$it\"" })
        }
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "release/roadmap.toml").isFile }
        val manifest = File(root, "release/knotentypen.json")
        if (manifest.exists()) {
            val gespeichert = Regex("\"([^\"]+)\"").findAll(manifest.readText()).map { it.groupValues[1] }.toList()
            assertEquals(gespeichert.filterNot { it in setOf("karte.notiz", "mathematik.gruppe") }, typen,
                "Manifest und gemeinsamer Mathematikkatalog müssen exakt übereinstimmen")
        } else check(System.getenv("ATLAS_KNOTEN_KATALOG_EXPORT") != null) { "Release-Knotenkatalog fehlt" }
    }
}
