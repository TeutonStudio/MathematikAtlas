package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import kotlin.test.*

class GanzzahlKonzeptTest {
    @Test fun `Zahlbereiche besitzen eigene fachlich zugeordnete Konzepte`() {
        val zahl = konzept(MathematikKnotenVorlagen.Zahl)
        val ganze = konzept(MathematikKnotenVorlagen.GanzeZahlen)
        val natürliche = konzept(MathematikKnotenVorlagen.NatürlicheZahlen)

        assertEquals(KonzeptId("zahl"), zahl.id)
        assertEquals(KonzeptId("ganzezahlen"), ganze.id)
        assertEquals(KonzeptId("natuerlichezahlen"), natürliche.id)
        assertNotNull(ganze.reiter.firstOrNull { it.id == "ganze-beispiele" })
        assertNotNull(ganze.reiter.firstOrNull { it.id == "positiver-nachfolger" })
        assertNotNull(ganze.reiter.firstOrNull { it.id == "negativer-nachfolger" })
        assertNotNull(natürliche.reiter.firstOrNull { it.id == "definition" })
        assertNotNull(natürliche.reiter.firstOrNull { it.id == "beispiele" })
    }

    @Test fun `Eins und minus eins sind die Vorzeichen Einzelmengen und keine Mächtigkeiten`() {
        val definition = konzept(MathematikKnotenVorlagen.Zahl).reiter("definition").karte

        assertEquals(2, definition.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(0, definition.knoten.count { it.art == "mathematik.mächtigkeit" })
        assertTrue(definition.knoten.any { it.parameter["name"] == "+" })
        assertTrue(definition.knoten.any { it.parameter["name"] == "−" })
        assertTrue(definition.knoten.any { it.name == "1 = {+}" })
        assertTrue(definition.knoten.any { it.name == "−1 = {−}" })
        assertFalse(definition.knoten.any { it.art == "mathematik.zahl" })
    }

    @Test fun `Ganze Zahlen enthalten kumulativ alle Vorgänger und das Vorzeichen`() {
        val ganze = konzept(MathematikKnotenVorlagen.GanzeZahlen)
        val beispiele = ganze.reiter("ganze-beispiele").karte

        assertTrue(ganze.beschreibung.contains("1 = {+}"))
        assertTrue(ganze.beschreibung.contains("−1 = {−}"))
        assertEquals("3 = {2,1,+} und −6 = {−5,−4,−3,−2,−1,−}", beispiele.name)
        assertTrue(beispiele.knoten.any { it.parameter["regel"]?.contains("3 = {2,1,+}") == true })
        assertTrue(beispiele.knoten.any { it.parameter["regel"]?.contains("−6 = {−5,−4,−3,−2,−1,−}") == true })
    }

    @Test fun `Vorzeichen Nachfolger erweitern die bestehende Zahl um ihren Vorgänger`() {
        val ganze = konzept(MathematikKnotenVorlagen.GanzeZahlen)
        val positiv = ganze.reiter("positiver-nachfolger").karte
        val negativ = ganze.reiter("negativer-nachfolger").karte

        assertEquals("n+1 = n ∪ {n}, Basis 1 = {+}", positiv.name)
        assertEquals("n−1 = n ∪ {n}, Basis −1 = {−}", negativ.name)
        assertEquals(1, positiv.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(1, negativ.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(1, positiv.knoten.count { it.art == "mathematik.vereinigung" })
        assertEquals(1, negativ.knoten.count { it.art == "mathematik.vereinigung" })
        assertFalse(positiv.knoten.any { it.parameter["name"] == "+" })
        assertFalse(negativ.knoten.any { it.parameter["name"] == "−" })
    }

    @Test fun `Natürliche Zahlen folgen der von Neumann Nachfolgerkonstruktion`() {
        val natürliche = konzept(MathematikKnotenVorlagen.NatürlicheZahlen)
        val definition = natürliche.reiter("definition").karte
        val beispiele = natürliche.reiter("beispiele").karte

        assertEquals("n+1 = n ∪ {n}, Basis 0 = ∅", definition.name)
        assertEquals(1, definition.knoten.count { it.art == "mathematik.einzelmenge" })
        assertEquals(1, definition.knoten.count { it.art == "mathematik.vereinigung" })
        assertEquals("0 = ∅, 1 = {0}, 2 = {1,0}", beispiele.name)
        assertTrue(beispiele.knoten.any { it.name == "0 = ∅" })
        assertTrue(beispiele.knoten.any { it.name == "1 = {0}" })
        assertTrue(beispiele.knoten.any { it.name == "2 = {1,0}" })
    }

    @Test fun `Zahlbereiche zeigen N und N0 als Teilmengen von Z`() {
        val karte = konzept(MathematikKnotenVorlagen.Zahl).reiter("zahlbereiche").karte

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

    private fun konzept(vorlage: KnotenVorlage): KonzeptDefinition = assertNotNull(
        TestDefinitionsKarten.fürKnoten(vorlage.erzeuge(GraphPunkt(0f, 0f))),
        vorlage.art.toString(),
    )
}
