package de.TeutonStudio.MathematikAtlas

import kotlin.test.*

class GanzzahlKonzeptTest {
    @Test fun `Zahlkonzept enthält positive und negative Nachfolger`() {
        val zahl = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("zahl")))

        assertNotNull(zahl.reiter.firstOrNull { it.id == "positiver-nachfolger" })
        assertNotNull(zahl.reiter.firstOrNull { it.id == "negativer-nachfolger" })
        assertTrue(zahl.beschreibung.contains("0"))
        assertTrue(zahl.beschreibung.contains("+1"))
        assertTrue(zahl.beschreibung.contains("−1"))
    }

    @Test fun `Eins und minus eins werden als gleichmächtige Einzelmengen dargestellt`() {
        val definition = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("zahl"))).reiter("definition").karte

        assertEquals(2, definition.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(2, definition.knoten.count { it.art == "mathematik.mächtigkeit" })
        assertTrue(definition.knoten.any { it.parameter["name"] == "+" })
        assertTrue(definition.knoten.any { it.parameter["name"] == "−" })
    }

    @Test fun `Zahlbereiche zeigen N und N0 als Teilmengen von Z`() {
        val karte = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("zahl"))).reiter("zahlbereiche").karte

        assertEquals(2, karte.knoten.count { it.art == "mathematik.teilOderGleichmenge" })
        assertTrue(karte.knoten.any { it.art == "mathematik.natürlicheZahlen" })
        assertTrue(karte.knoten.any { it.art == "mathematik.ganzeZahlen" })
    }

    @Test fun `Subtraktion besitzt Definition und ganzen Sonderfall`() {
        val konzept = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("subtraktion")))

        assertTrue(konzept.reiter("definition").karte.knoten.any { it.art == "mathematik.subtraktion" })
        assertTrue(konzept.reiter("ganze-zahlen").karte.knoten.any { it.art == "mathematik.addition" })
        assertTrue(konzept.reiter("ganze-zahlen").karte.knoten.any { it.art == "mathematik.multiplikation" })
    }
}
