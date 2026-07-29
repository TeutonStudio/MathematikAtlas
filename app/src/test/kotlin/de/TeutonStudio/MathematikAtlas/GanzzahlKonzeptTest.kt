package de.TeutonStudio.MathematikAtlas

import kotlin.test.*

class GanzzahlKonzeptTest {
    @Test fun `Zahlkonzept trennt Vorzeichenzahlen und natürliche Nachfolger`() {
        val zahl = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("zahl")))

        assertNotNull(zahl.reiter.firstOrNull { it.id == "positiver-nachfolger" })
        assertNotNull(zahl.reiter.firstOrNull { it.id == "negativer-nachfolger" })
        assertNotNull(zahl.reiter.firstOrNull { it.id == "natürliche-beispiele" })
        assertNotNull(zahl.reiter.firstOrNull { it.id == "natürlicher-nachfolger" })
        assertTrue(zahl.beschreibung.contains("1 = {+}"))
        assertTrue(zahl.beschreibung.contains("−1 = {−}"))
        assertTrue(zahl.beschreibung.contains("n+1 = n ∪ {n}"))
    }

    @Test fun `Eins und minus eins sind die Vorzeichen Einzelmengen und keine Mächtigkeiten`() {
        val definition = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("zahl"))).reiter("definition").karte

        assertEquals(2, definition.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(0, definition.knoten.count { it.art == "mathematik.mächtigkeit" })
        assertTrue(definition.knoten.any { it.parameter["name"] == "+" })
        assertTrue(definition.knoten.any { it.parameter["name"] == "−" })
        assertTrue(definition.knoten.any { it.name == "1 = {+}" })
        assertTrue(definition.knoten.any { it.name == "−1 = {−}" })
        assertFalse(definition.knoten.any { it.art == "mathematik.zahl" })
    }

    @Test fun `Vorzeichen Nachfolger erzeugen Mengen aus Vorgänger und Vorzeichen`() {
        val zahl = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("zahl")))
        val positiv = zahl.reiter("positiver-nachfolger").karte
        val negativ = zahl.reiter("negativer-nachfolger").karte

        assertEquals("n+1 = {n,+}", positiv.name)
        assertEquals("n−1 = {n,−}", negativ.name)
        assertEquals(2, positiv.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(2, negativ.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(1, positiv.knoten.count { it.art == "mathematik.vereinigung" })
        assertEquals(1, negativ.knoten.count { it.art == "mathematik.vereinigung" })
    }

    @Test fun `Natürliche Zahlen folgen der von Neumann Nachfolgerkonstruktion`() {
        val zahl = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("zahl")))
        val beispiele = zahl.reiter("natürliche-beispiele").karte
        val nachfolger = zahl.reiter("natürlicher-nachfolger").karte

        assertEquals("0 = ∅, 1 = {0}, 2 = {0,1}", beispiele.name)
        assertTrue(beispiele.knoten.any { it.name == "0 = ∅" })
        assertTrue(beispiele.knoten.any { it.name == "1 = {0}" })
        assertTrue(beispiele.knoten.any { it.name == "2 = {0,1}" })
        assertEquals("n+1 = n ∪ {n}", nachfolger.name)
        assertEquals(1, nachfolger.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(1, nachfolger.knoten.count { it.art == "mathematik.vereinigung" })
    }

    @Test fun `Zahlbereiche zeigen N und N0 als Teilmengen von Z`() {
        val karte = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("zahl"))).reiter("zahlbereiche").karte

        assertEquals(2, karte.knoten.count { it.art == "mathematik.teilOderGleichmenge" })
        assertTrue(karte.knoten.any { it.art == "mathematik.natürlicheZahlen" })
        assertTrue(karte.knoten.any { it.art == "mathematik.ganzeZahlen" })
        assertFalse(karte.knoten.any { it.art == "mathematik.zahl" })
    }

    @Test fun `Subtraktion besitzt selbstbezugsfreie Definition und ganzen Sonderfall`() {
        val konzept = assertNotNull(TestDefinitionsKarten.finde(KonzeptId("subtraktion")))
        val definition = konzept.reiter("definition").karte

        assertFalse(definition.knoten.any { it.art == "mathematik.subtraktion" })
        assertTrue(definition.knoten.any { it.art == TestDefinitionsKarten.KONZEPT_REGEL_ART })
        assertTrue(konzept.reiter("ganze-zahlen").karte.knoten.any { it.art == "mathematik.addition" })
        assertTrue(konzept.reiter("ganze-zahlen").karte.knoten.any { it.art == "mathematik.multiplikation" })
    }
}
