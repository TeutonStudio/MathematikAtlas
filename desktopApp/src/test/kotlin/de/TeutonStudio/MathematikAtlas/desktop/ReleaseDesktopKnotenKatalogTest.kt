package de.TeutonStudio.MathematikAtlas.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseDesktopKnotenKatalogTest {
    @Test fun `Release-Manifest erfasst alle Desktop-Erstelltypen`() {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "release/roadmap.toml").isFile }
        val typen = Regex("\"([^\"]+)\"").findAll(File(root, "release/knotentypen.json").readText()).map { it.groupValues[1] }.toSet()
        assertEquals(typen - setOf("karte.notiz", "mathematik.gruppe"), desktopKnotenVorlagen().map { it.art }.toSet(),
            "Desktop und gemeinsamer Releasekatalog müssen exakt übereinstimmen")
    }
    @Test fun `Desktop sucht geometrische Relationen als konfigurierte Praedikate`() {
        val arten = setOf("inzidenz", "zwischen", "kollinear", "parallel", "orthogonal", "gleichheit", "strecken", "winkel")
        arten.forEach { text ->
            val treffer = desktopKnotenSuchVorlagen(text)
            assertTrue(treffer.any { it.art == "mathematik.praedikat" &&
                it.standardParameter["operator"].orEmpty().startsWith("geometrie.relation.") }, text)
            assertTrue(treffer.none { it.art in de.TeutonStudio.MathematikKnoten.PraedikatKnotenMigration.alteKnotenArten }, text)
        }
    }

    @Test fun `Desktop Operatoralias erzeugt richtige Konfiguration ohne leere Suchduplikate`() {
        val spur = desktopKnotenSuchVorlagen("matrix.spur").single()
        assertEquals("mathematik.matrixrechner", spur.art)
        assertEquals("matrix.spur", spur.standardParameter["operator"])
        val basis = desktopKnotenVorlagen()
        val leer = desktopKnotenSuchVorlagen("", basis)
        assertEquals(basis.map { it.art }.toSet(), leer.map { it.art }.toSet())
        assertEquals(leer.size, leer.map(de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog::variantenKennung).distinct().size)
    }

}
