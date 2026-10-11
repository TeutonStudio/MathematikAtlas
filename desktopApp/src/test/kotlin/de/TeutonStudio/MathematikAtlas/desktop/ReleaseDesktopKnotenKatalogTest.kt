package de.TeutonStudio.MathematikAtlas.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ReleaseDesktopKnotenKatalogTest {
    @Test fun `Release-Manifest erfasst alle Desktop-Erstelltypen`() {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "release/roadmap.toml").isFile }
        val typen = File(root, "release/knotentypen.json").readLines().map { it.trim().trim(',').trim('"') }.toSet()
        assertTrue(typen.containsAll(desktopKnotenVorlagen().map { it.art }), "Neue Desktoptypen fehlen im Release-Manifest")
    }
}
