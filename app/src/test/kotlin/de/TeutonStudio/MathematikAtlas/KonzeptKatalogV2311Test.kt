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
                assertFalse(reiter.karte.knoten.any { it.art in konzept.knotenArten }, "${konzept.id}/${reiter.id}")
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
    fun `Katalogvalidator meldet keine Strukturfehler`() {
        assertEquals(emptyList(), TestDefinitionsKarten.validierungsFehler())
    }
}
