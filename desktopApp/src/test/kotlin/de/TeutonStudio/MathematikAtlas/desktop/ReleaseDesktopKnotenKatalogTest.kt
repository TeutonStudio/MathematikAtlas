package de.TeutonStudio.MathematikAtlas.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseDesktopKnotenKatalogTest {
    @Test fun `Release-Manifest erfasst alle Desktop-Erstelltypen`() {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "release/roadmap.toml").isFile }
        val typen = Regex("\"([^\"]+)\"").findAll(File(root, "release/knotentypen.json").readText()).map { it.groupValues[1] }.toSet()
        assertEquals(typen - setOf("karte.notiz", "mathematik.gruppe"), desktopKnotenVorlagen().map { it.art }.toSet(),
            "Desktop und gemeinsamer Releasekatalog müssen exakt übereinstimmen")
    }
}
