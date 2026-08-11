package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.AxiomKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.PraedikatKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AxiomPraedikatDefinitionsKartenTest {
    @Test
    fun `jedes registrierte Axiom besitzt eine vollständige Definitionskarte`() {
        AxiomOperatoren.alle.forEach { definition ->
            val knoten = AxiomKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)
            val konzept = assertNotNull(
                axiomPraedikatKonzept(knoten),
                "Für ${definition.stabileId} fehlt die Definitionskarte.",
            )
            val karte = konzept.reiter.single().karte

            assertEquals("Axiom: ${definition.titel}", konzept.name)
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
    fun `normale Relationspraedikate werden nicht als Axiomkarte behandelt`() {
        val knoten = PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)

        assertNull(axiomPraedikatKonzept(knoten))
    }
}
