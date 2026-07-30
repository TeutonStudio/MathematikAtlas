package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.GeometrieKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen
import kotlin.test.*

class KonzeptKatalogV2311Test {
    @Test
    fun `jede feste Vorlage besitzt genau eine selbstbezugsfreie Definition`() {
        val vorlagen = MathematikKnotenVorlagen.alle + MengenraumKnotenVorlagen.alle + GeometrieKnotenVorlagen.alle

        vorlagen.forEach { vorlage ->
            val konzept = assertNotNull(TestDefinitionsKarten.fürKnoten(vorlage.erzeuge(GraphPunkt.Zero)), vorlage.art)
            assertEquals(1, konzept.reiter.count { it.rolle == KonzeptReiterRolle.Definition }, vorlage.art)
            konzept.reiter.forEach { reiter ->
                (listOf(reiter.karte) + reiter.darstellungsVarianten.values).forEach { karte ->
                    assertFalse(karte.knoten.any { it.art in konzept.knotenArten }, "${konzept.id}/${reiter.id}/${karte.id}")
                }
            }
        }
    }

    @Test
    fun `v2 3 10 Mengenraumknoten sind im Katalog enthalten`() {
        MengenraumKnotenVorlagen.alle.forEach { vorlage ->
            assertNotNull(TestDefinitionsKarten.fürKnoten(vorlage.erzeuge(GraphPunkt.Zero)), vorlage.art)
        }
    }

    @Test
    fun `Division besitzt praktische selbstbezugsfreie Karten in beiden komplexen Darstellungen`() {
        val konzept = assertNotNull(TestDefinitionsKarten.fürKnoten(MathematikKnotenVorlagen.Division.erzeuge(GraphPunkt.Zero)))
        assertEquals(setOf("definition", "positiver-nenner", "negativer-nenner", "komplexer-nenner", "komplexer-zaehler"), konzept.reiter.map { it.id }.toSet())

        val definition = konzept.reiter.single { it.id == "definition" }.karte
        val fall = definition.knoten.single { it.art == MathematikKnotenVorlagen.Fall.art }
        val nullwert = definition.knoten.single { it.parameter["name"] == "Falls Nenner 0" }
        assertTrue(definition.knoten.any { it.art == MathematikKnotenVorlagen.Kehrwert.art })
        assertFalse(definition.knoten.any { it.art == MathematikKnotenVorlagen.Division.art })
        assertTrue(definition.verbindungen.any { it.von.knotenId == nullwert.id && it.zu.anschlussId == fall.anschlüsse.single { a -> a.name == "wahr" }.id })

        listOf("komplexer-nenner", "komplexer-zaehler").forEach { id ->
            val reiter = konzept.reiter.single { it.id == id }
            assertTrue(reiter.besitztDarstellungsVarianten)
            assertNotEquals(reiter.karte.id, reiter.karteFür(KomplexDarstellung.Polar).id)
            assertTrue(reiter.karteFür(KomplexDarstellung.Polar).knoten.any { it.art == MathematikKnotenVorlagen.KomplexAusTupel.art && it.parameter["modus"] == "polar" })
        }
    }

    @Test
    fun `Katalogvalidator meldet keine Strukturfehler`() {
        assertEquals(emptyList(), TestDefinitionsKarten.validierungsFehler())
    }
}
