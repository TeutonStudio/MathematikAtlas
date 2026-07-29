package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.*
import kotlin.test.*

class TestDefinitionsKartenTest {
    private val festeVorlagen
        get() = MathematikKnotenVorlagen.alle +
            ErweiterteMathematikKnotenVorlagen.alle +
            GeometrieKnotenVorlagen.alle

    @Test
    fun `jedes Konzept besitzt genau einen Definitionsreiter`() {
        assertTrue(TestDefinitionsKarten.alle.isNotEmpty())
        TestDefinitionsKarten.alle.forEach { konzept ->
            assertEquals(1, konzept.reiter.count { it.rolle == KonzeptReiterRolle.Definition }, konzept.name)
            assertEquals(KonzeptReiterRolle.Definition, konzept.sortierteReiter.first().rolle)
        }
    }

    @Test
    fun `jede feste Knotenart besitzt eine Definitionskarte`() {
        val erwartet = festeVorlagen.map { it.art }.toSet()
        val tatsächlich = TestDefinitionsKarten.alle.flatMap { it.knotenArten }.toSet()

        assertEquals(erwartet, tatsächlich)
        festeVorlagen.forEach { vorlage ->
            assertNotNull(TestDefinitionsKarten.fürKnoten(vorlage.erzeuge(GraphPunkt(0f, 0f))), vorlage.art.toString())
        }
    }

    @Test
    fun `keine Konzeptkarte enthält den von ihr definierten Knoten`() {
        TestDefinitionsKarten.alle.forEach { konzept ->
            konzept.reiter.forEach { reiter ->
                val selbstbezüge = reiter.karte.knoten.filter { it.art in konzept.knotenArten }
                assertTrue(selbstbezüge.isEmpty(), "${konzept.id}/${reiter.id}: ${selbstbezüge.map { it.id }}")
            }
        }
    }

    @Test
    fun `Definitionsabhängigkeiten sind vollständig und azyklisch`() {
        assertEquals(emptyList(), TestDefinitionsKarten.validierungsFehler())
    }

    @Test
    fun `Definitionskarte bildet den vollständigen Anschlussvertrag ab`() {
        val vorlage = MathematikKnotenVorlagen.Potenz
        val konzept = assertNotNull(TestDefinitionsKarten.fürKnoten(vorlage.erzeuge(GraphPunkt(0f, 0f))))
        val karte = konzept.reiter("definition").karte
        val regel = karte.knoten.single { it.art == TestDefinitionsKarten.KONZEPT_REGEL_ART }

        assertEquals(
            vorlage.anschlüsse.map { Triple(it.name, it.richtung, it.art) },
            regel.anschlüsse.map { Triple(it.name, it.richtung, it.art) },
        )
        assertEquals(vorlage.beschreibung, regel.parameter["regel"])
        assertEquals(
            vorlage.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang },
            karte.knoten.count { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART },
        )
        assertEquals(
            vorlage.anschlüsse.count { it.richtung == AnschlussRichtung.Ausgang },
            karte.knoten.count { it.art == TestDefinitionsKarten.KONZEPT_AUSGANG_ART },
        )
        assertEquals(vorlage.anschlüsse.size, karte.verbindungen.size)
    }

    @Test
    fun `Varianten derselben Knotenart teilen ein Konzept`() {
        val extremwerte = festeVorlagen.filter { it.art == "mathematik.extremwert" }
        assertTrue(extremwerte.size >= 2)

        val konzepte = extremwerte.map { vorlage ->
            assertNotNull(TestDefinitionsKarten.fürKnoten(vorlage.erzeuge(GraphPunkt(0f, 0f))))
        }.distinctBy { it.id }

        assertEquals(1, konzepte.size)
        assertEquals(extremwerte.size, konzepte.single().reiter.size)
    }

    @Test
    fun `nicht freigegebene Änderungen verändern eine Definitionskarte nicht`() {
        val sitzung = KonzeptSitzung()
        sitzung.öffne(KonzeptId("potenz"))
        val original = assertNotNull(sitzung.aktuelleKarte())
        val regel = original.knoten.single { it.art == TestDefinitionsKarten.KONZEPT_REGEL_ART }

        sitzung.wähleKnoten(regel.id)
        sitzung.setzeParameter(regel.id, "regel", "Manipuliert")

        assertEquals(original, sitzung.aktuelleKarte())
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
    fun `Dokumentationsknoten sind keine auswählbaren Vorlagen`() {
        val arten = festeVorlagen.map { it.art }.toSet()
        assertFalse(TestDefinitionsKarten.KONZEPT_REGEL_ART in arten)
        assertFalse(TestDefinitionsKarten.KONZEPT_EINGANG_ART in arten)
        assertFalse(TestDefinitionsKarten.KONZEPT_AUSGANG_ART in arten)
    }
}
