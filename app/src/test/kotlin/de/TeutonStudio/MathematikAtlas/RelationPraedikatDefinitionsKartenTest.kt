package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.AxiomKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.PraedikatKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.definitionsFormel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RelationPraedikatDefinitionsKartenTest {
    @Test
    fun `jede registrierte Relation besitzt eine vollständige Definitionskarte`() {
        RelationsOperatoren.alle.forEach { definition ->
            val knoten = PraedikatKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)
            val konzept = assertNotNull(
                relationPraedikatKonzept(knoten),
                "Für ${definition.stabileId} fehlt die Definitionskarte.",
            )
            val formel = definition.definitionsFormel
            val karte = konzept.reiter.single().karte
            val regel = karte.knoten.single { it.art == KonzeptKnotenArten.REGEL }

            assertEquals("Relation: ${definition.titel}", konzept.name)
            assertEquals(definition.symbolLatex, konzept.reiter.single().titel)
            assertEquals(definition.argumente.size + 2, karte.knoten.size)
            assertEquals(definition.argumente.size + 1, karte.verbindungen.size)
            assertEquals(formel.latex, regel.parameter["regel"])
            assertEquals(formel.latex, regel.parameter["definition"])
            assertEquals(definition.symbolLatex, regel.parameter["symbol"])
            assertEquals(formel.hinweis, regel.parameter["definitionsHinweis"])
            assertEquals(
                formel.vorausgesetzteAxiomIds.sorted().joinToString(),
                regel.parameter["vorausgesetzteAxiome"],
            )
            assertTrue(formel.vorausgesetzteAxiomIds.all { it in konzept.tags })
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
    fun `Gleichheitskarte zeigt Mengenmitgliedschaftsdefinition und Paarmengenabhaengigkeit`() {
        val definition = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.gleichheit"))
        val knoten = PraedikatKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)
        val konzept = assertNotNull(relationPraedikatKonzept(knoten))
        val regel = konzept.reiter.single().karte.knoten.single { it.art == KonzeptKnotenArten.REGEL }

        assertEquals(
            "a=b\\Longleftrightarrow\\forall M\\left(\\operatorname{Menge}(M)\\Rightarrow\\left(a\\in M\\Leftrightarrow b\\in M\\right)\\right)",
            regel.parameter["regel"],
        )
        assertEquals("axiom.zf.paarmenge", regel.parameter["vorausgesetzteAxiome"])
        assertTrue(checkNotNull(regel.parameter["definitionsHinweis"]).contains("Typbedingung"))
    }

    @Test
    fun `Axiom-Prädikate werden nicht als Relationskarte behandelt`() {
        val definition = AxiomOperatoren.alle.first()
        val knoten = AxiomKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)

        assertNull(relationPraedikatKonzept(knoten))
    }
}
