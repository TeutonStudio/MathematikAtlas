package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.GeometrieKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_KOMPLEX_TUPEL
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_KOMPLEX_EINGABE
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.*

class KonzeptKatalogV2311Test {
    @Test
    fun `jede feste Vorlage besitzt genau eine selbstbezugsfreie Definition`() {
        val vorlagen = (alleMathematikDefinitionsVorlagen() + MengenraumKnotenVorlagen.alle + GeometrieKnotenVorlagen.alle)
            .distinctBy { it.art to it.name }

        vorlagen.forEach { vorlage ->
            val konzept = assertNotNull(TestDefinitionsKarten.fürKnoten(vorlage.erzeuge(GraphPunkt.Zero)), vorlage.art)
            assertEquals(1, konzept.reiter.count { it.rolle == KonzeptReiterRolle.Definition }, vorlage.art)
            konzept.reiter.forEach { reiter ->
                (listOf(reiter.karte) + reiter.darstellungsVarianten.values).forEach { karte ->
                    assertFalse(karte.knoten.any(konzept::erklärt), "${konzept.id}/${reiter.id}/${karte.id}")
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
        val division = alleMathematikDefinitionsVorlagen().single {
            it.art == ZAHLENRECHNER_ART &&
                it.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.DIVISION.stabileId
        }
        val konzept = assertNotNull(TestDefinitionsKarten.fürKnoten(division.erzeuge(GraphPunkt.Zero)))
        assertEquals(setOf("definition", "positiver-nenner", "negativer-nenner", "komplexer-nenner", "komplexer-zaehler"), konzept.reiter.map { it.id }.toSet())

        val definition = konzept.reiter.single { it.id == "definition" }.karte
        val fall = definition.knoten.single { it.art == MathematikKnotenVorlagen.Fall.art }
        val nullwert = definition.knoten.single { it.parameter["name"] == "Falls Nenner 0" }
        assertTrue(definition.knoten.any {
            it.art == ZAHLENRECHNER_ART &&
                it.parameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.KEHRWERT.stabileId
        })
        assertFalse(definition.knoten.any {
            it.art == ZAHLENRECHNER_ART &&
                it.parameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.DIVISION.stabileId
        })
        assertTrue(definition.verbindungen.any { it.von.knotenId == nullwert.id && it.zu.anschlussId == fall.anschlüsse.single { a -> a.name == "wahr" }.id })

        listOf("komplexer-nenner", "komplexer-zaehler").forEach { id ->
            val reiter = konzept.reiter.single { it.id == id }
            assertTrue(reiter.besitztDarstellungsVarianten)
            assertNotEquals(reiter.karte.id, reiter.karteFür(KomplexDarstellung.Polar).id)
            assertTrue(reiter.karteFür(KomplexDarstellung.Polar).knoten.any {
                it.art == ZAHLENRECHNER_ART &&
                    it.parameter[ZAHLENRECHNER_OPERATOR] ==
                    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR.stabileId &&
                    it.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE] == ZAHLENRECHNER_KOMPLEX_TUPEL
            })
        }
    }

    @Test
    fun `Katalogvalidator meldet keine Strukturfehler`() {
        assertEquals(emptyList(), TestDefinitionsKarten.validierungsFehler())
    }
}