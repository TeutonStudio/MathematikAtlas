package de.TeutonStudio.MathematikAtlas

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseAppKnotenKatalogTest {
    @Test fun `Manifest erfasst alle erzeugbaren Androidtypen einschliesslich Werkzeuge und Gruppen`() {
        val typen = erzeugbareKnotenArten().sorted()
        System.getenv("ATLAS_APP_KNOTEN_KATALOG_EXPORT")?.let { File(it).writeText(typen.joinToString(prefix = "[\n", postfix = "\n]\n", separator = ",\n") { "  \"$it\"" }) }
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }.first { File(it, "release/roadmap.toml").isFile }
        val manifest = Regex(""""([^"]+)"""").findAll(File(root, "release/knotentypen.json").readText()).map { it.groupValues[1] }.toList()
        assertEquals(typen, manifest, "Vollstaendiger Erstellenkatalog und Release-Manifest laufen auseinander")
    }
}
