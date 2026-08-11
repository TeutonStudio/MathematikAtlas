package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.AxiomKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.PraedikatKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RelationPraedikatDefinitionsKartenTest {
    @Test
    fun `jede registrierte Relation besitzt eine vollständige Definitionskarte`() {
        RelationsOperatoren.alle.forEach { definition ->
            val knoten = PraedikatKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)
            val konzept = assertNotNull(
                relationPraedikatKonzept(knoten),
                "Für ${definition.stabileId} fehlt die Definitionskarte.",
            )
            val karte = konzept.reiter.single().karte

            assertEquals("Relation: ${definition.titel}", konzept.name)
            assertEquals(definition.symbolLatex, konzept.reiter.single().titel)
            assertEquals(definition.argumente.size + 2, karte.knoten.size)
            assertEquals(definition.argumente.size + 1, karte.verbindungen.size)
            assertEquals(
                definition.symbolLatex,
                karte.knoten.single { it.art == KonzeptKnotenArten.REGEL }.parameter["regel"],
            )
            assertEquals(
                definition.argumente.map { it.rolle },
                karte.knoten
                    .filter { it.art == KonzeptKnotenArten.EINGANG }
                    .sortedBy { it.position.y }
                    .map { it.name },
            )
        }
    }

    @Test
    fun `Axiom-Prädikate werden nicht als Relationskarte behandelt`() {
        val definition = AxiomOperatoren.alle.first()
        val knoten = AxiomKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)

        assertNull(relationPraedikatKonzept(knoten))
    }
}
