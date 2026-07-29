package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import kotlin.test.*

class TestDefinitionsKartenTest {
    @Test
    fun `jedes Testkonzept besitzt genau einen Definitionsreiter`() {
        assertTrue(TestDefinitionsKarten.alle.isNotEmpty())
        TestDefinitionsKarten.alle.forEach { konzept ->
            assertEquals(1, konzept.reiter.count { it.rolle == KonzeptReiterRolle.Definition }, konzept.name)
            assertEquals(KonzeptReiterRolle.Definition, konzept.sortierteReiter.first().rolle)
        }
    }

    @Test
    fun `alle Navigationsziele existieren im Katalog`() {
        val ids = TestDefinitionsKarten.alle.map { it.id }.toSet()
        TestDefinitionsKarten.alle.forEach { konzept ->
            konzept.navigation.values.forEach { ziel -> assertTrue(ziel in ids, "Fehlendes Ziel $ziel") }
        }
    }

    @Test
    fun `Erkundungsfreigaben verweisen auf vorhandene Parameter`() {
        TestDefinitionsKarten.alle.forEach { konzept ->
            konzept.erkundungsFreigaben.forEach { freigabe ->
                val karte = konzept.reiter(freigabe.reiterId).karte
                val knoten = assertNotNull(karte.knoten.firstOrNull { it.id == freigabe.knotenId })
                assertTrue(freigabe.parameter in knoten.parameter, "${konzept.name}: ${freigabe.parameter}")
            }
        }
    }

    @Test
    fun `Overlay verändert nicht die originale Testkarte`() {
        val sitzung = KonzeptSitzung()
        sitzung.öffne(KonzeptId("addition"))
        val original = assertNotNull(sitzung.aktuelleKarte())
        val zahl = original.knoten.first { it.id == KnotenId("addition-a") }

        sitzung.wähleKnoten(zahl.id)
        sitzung.setzeParameter(zahl.id, "wert", "99")

        val verändert = assertNotNull(sitzung.aktuelleKarte())
        assertEquals("99", verändert.knoten.first { it.id == zahl.id }.parameter["wert"])
        assertEquals("2", original.knoten.first { it.id == zahl.id }.parameter["wert"])
        assertNotEquals(original, verändert)
    }

    @Test
    fun `Breadcrumb kürzt Navigation ohne neue Dialoginstanz`() {
        val sitzung = KonzeptSitzung()
        sitzung.öffne(KonzeptId("addition"))
        sitzung.navigiere(KonzeptId("zahl"))
        sitzung.navigiere(KonzeptId("division"))
        assertEquals(3, sitzung.pfad.size)

        sitzung.springeZu(0)

        assertEquals(listOf(KonzeptId("addition")), sitzung.pfad.map { it.konzeptId })
    }

    @Test
    fun `Kehrwert besitzt eine eigene Definitionskarte`() {
        val kehrwert = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("kehrwert")))
        val definition = kehrwert.sortierteReiter.first().karte

        assertTrue(definition.knoten.any { it.art == "mathematik.kehrwert" })
        assertTrue(definition.knoten.any { it.art == "mathematik.zahl" })
    }

    @Test
    fun `Division wird ohne zirkulären Divisionsknoten definiert`() {
        val division = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("division")))
        val definition = division.reiter("definition").karte

        assertTrue(definition.knoten.any { it.art == "mathematik.kehrwert" })
        assertTrue(definition.knoten.any { it.art == "mathematik.multiplikation" })
        assertTrue(definition.knoten.any { it.art == "mathematik.fall" })
        assertFalse(definition.knoten.any { it.art == "mathematik.division" })
    }

    @Test
    fun `Divisionsdefinition wählt bei Nenner null den Ersatzwert`() {
        val definition = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("division"))).reiter("definition").karte
        val fall = definition.knoten.single { it.art == "mathematik.fall" }
        val ersatz = definition.knoten.single { it.parameter["name"] == "falls Nenner null" }
        val produkt = definition.knoten.single { it.art == "mathematik.multiplikation" }
        val wahr = fall.anschlüsse.single { it.name == "wahr" }.id
        val lüge = fall.anschlüsse.single { it.name == "lüge" }.id

        assertTrue(definition.verbindungen.any { it.von.knotenId == ersatz.id && it.zu.anschlussId == wahr })
        assertTrue(definition.verbindungen.any { it.von.knotenId == produkt.id && it.zu.anschlussId == lüge })
    }

    @Test
    fun `Komplexer Divisor wird mit der Konjugierten rationalisiert`() {
        val division = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("division")))
        val sonderfall = division.reiter("komplexer-divisor").karte

        assertTrue(sonderfall.knoten.any { it.art == "mathematik.konjugierte" })
        assertEquals(3, sonderfall.knoten.count { it.art == "mathematik.multiplikation" })
        assertTrue(sonderfall.knoten.any { it.art == "mathematik.kehrwert" })
    }
}
