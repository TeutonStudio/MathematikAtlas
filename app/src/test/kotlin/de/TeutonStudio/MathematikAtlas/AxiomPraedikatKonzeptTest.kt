package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.AxiomKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_AXIOME
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_PARAMETER
import de.TeutonStudio.MathematikKnoten.PraedikatKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AxiomPraedikatKonzeptTest {
    @Test
    fun `jedes Axiom besitzt eine Definitionskarte`() {
        AxiomOperatoren.alle.forEach { definition ->
            val knoten = AxiomKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)
            val konzept = assertNotNull(axiomPraedikatKonzept(knoten), definition.stabileId)
            val karte = konzept.reiter.single { it.rolle == KonzeptReiterRolle.Definition }.karte

            assertEquals(definition.titel, konzept.name, definition.stabileId)
            assertEquals(
                definition.stabileId,
                konzept.knotenParameter[RelationsOperatoren.OPERATOR_PARAMETER],
                definition.stabileId,
            )
            assertEquals(
                PRAEDIKAT_SEITE_AXIOME,
                konzept.knotenParameter[PRAEDIKAT_SEITE_PARAMETER],
                definition.stabileId,
            )
            assertTrue(
                karte.knoten.any {
                    it.art == KonzeptKnotenArten.REGEL && it.parameter["regel"] == definition.symbolLatex
                },
                "${definition.stabileId}: Axiomformel fehlt",
            )
            definition.argumente.forEach { argument ->
                assertTrue(
                    karte.knoten.any {
                        it.art == KonzeptKnotenArten.EINGANG && it.name == argument.rolle
                    },
                    "${definition.stabileId}: Eingang ${argument.rolle} fehlt",
                )
            }
        }
    }

    @Test
    fun `Relationspraedikat bleibt Relationspraedikat`() {
        val knoten = PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        assertNull(axiomPraedikatKonzept(knoten))
    }
}
