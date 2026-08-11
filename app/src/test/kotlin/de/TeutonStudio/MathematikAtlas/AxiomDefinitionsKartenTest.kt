package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.AxiomKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.PraedikatKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AxiomDefinitionsKartenTest {
    @Test
    fun `jedes registrierte Axiom Praedikat besitzt eine vollstaendige Definitionskarte`() {
        assertTrue(AxiomOperatoren.alle.isNotEmpty())

        AxiomOperatoren.alle.forEach { definition ->
            val knoten = AxiomKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)
            val konzept = assertNotNull(
                axiomKonzeptFürKnoten(knoten),
                "${definition.stabileId} besitzt keine dynamische Definitionskarte.",
            )

            assertTrue(konzept.erklärt(knoten), "${definition.stabileId} wird von seiner Definition nicht erkannt.")
            assertEquals(definition.titel, konzept.name)
            assertEquals(1, konzept.reiter.count { it.rolle == KonzeptReiterRolle.Definition })

            val karte = konzept.reiter.single { it.rolle == KonzeptReiterRolle.Definition }.karte
            val regel = karte.knoten.single { it.art == KonzeptKnotenArten.REGEL }
            val eingänge = karte.knoten.filter { it.art == KonzeptKnotenArten.EINGANG }
            val ausgänge = karte.knoten.filter { it.art == KonzeptKnotenArten.AUSGANG }

            assertEquals(definition.symbolLatex, regel.parameter["regel"])
            assertEquals(definition.stabileId, regel.parameter["axiomId"])
            assertEquals(definition.argumente.map { it.rolle }, eingänge.map { it.name })
            assertEquals(listOf("aussage"), ausgänge.map { it.name })
            assertEquals(definition.argumente.size + 1, karte.verbindungen.size)
        }
    }

    @Test
    fun `normales Relations Praedikat wird nicht als Axiom erklaert`() {
        val relation = PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        assertNull(axiomKonzeptFürKnoten(relation))
    }
}
