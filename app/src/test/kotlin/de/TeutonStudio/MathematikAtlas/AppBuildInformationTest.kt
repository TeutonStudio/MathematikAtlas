package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppBuildInformationTest {
    @Test
    fun `lange Revision wird deterministisch auf zwölf Zeichen gekürzt`() {
        val information = AppBuildInformation(
            versionName = "2.19.0",
            versionCode = 2019000,
            commitSha = "0123456789ABCDEF0123456789ABCDEF01234567",
        )

        assertEquals("0123456789ab", information.kurzeCommitSha)
        assertEquals("Build 0123456789ab", information.buildZeile)
    }

    @Test
    fun `kurze gültige Revision bleibt vollständig sichtbar`() {
        val information = AppBuildInformation("2.19.0", 2019000, "a1b2c3d")

        assertEquals("a1b2c3d", information.kurzeCommitSha)
        assertEquals("Build a1b2c3d", information.buildZeile)
    }

    @Test
    fun `fehlende oder ungültige Revision wird als lokaler Build bezeichnet`() {
        listOf<String?>(null, "", "local", "unknown", "kein-hash").forEach { revision ->
            val information = AppBuildInformation("2.19.0", 2019000, revision)

            assertNull(information.kurzeCommitSha)
            assertEquals("Lokaler Build", information.buildZeile)
        }
    }

    @Test
    fun `Version wird aus den übergebenen Buildmetadaten formatiert`() {
        assertEquals(
            "Version 2.19.0",
            AppBuildInformation(" 2.19.0 ", 2019000, null).versionsZeile,
        )
        assertEquals(
            "Version unbekannt",
            AppBuildInformation(" ", 2019000, null).versionsZeile,
        )
    }
}
