package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
}
