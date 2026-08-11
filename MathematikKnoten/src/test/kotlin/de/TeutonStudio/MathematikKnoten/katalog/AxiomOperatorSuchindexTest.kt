package de.TeutonStudio.MathematikKnoten.katalog

import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_AXIOME
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_PARAMETER
import de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AxiomOperatorSuchindexTest {
    @Test
    fun `Peano Induktion ist unter Peano 5 direkt auffindbar`() {
        val treffer = OperatorKnotenSuchindex.suche("Peano 5", alleMathematikKnotenVorlagen())
        val peano = treffer.firstOrNull {
            it.standardParameter[RelationsOperatoren.OPERATOR_PARAMETER] == "axiom.peano.induktion"
        }

        assertTrue(peano != null)
        assertEquals(PRAEDIKAT_SEITE_AXIOME, peano.standardParameter[PRAEDIKAT_SEITE_PARAMETER])
    }

    @Test
    fun `Schiefkoerper und Koerper liefern unterschiedliche Axiomvarianten`() {
        val schief = OperatorKnotenSuchindex.suche("Schiefkörper", alleMathematikKnotenVorlagen())
        val koerper = OperatorKnotenSuchindex.suche("Körperaxiome", alleMathematikKnotenVorlagen())

        assertTrue(schief.any {
            it.standardParameter[RelationsOperatoren.OPERATOR_PARAMETER] == "axiom.algebra.schiefkoerper"
        })
        assertTrue(koerper.any {
            it.standardParameter[RelationsOperatoren.OPERATOR_PARAMETER] == "axiom.algebra.koerper"
        })
    }

    @Test
    fun `ZF Schemata sind über ihre gebräuchlichen Namen auffindbar`() {
        val separation = OperatorKnotenSuchindex.suche("Separation", alleMathematikKnotenVorlagen())
        val replacement = OperatorKnotenSuchindex.suche("Replacement", alleMathematikKnotenVorlagen())

        assertTrue(separation.any {
            it.standardParameter[RelationsOperatoren.OPERATOR_PARAMETER] == "axiom.zf.aussonderung"
        })
        assertTrue(replacement.any {
            it.standardParameter[RelationsOperatoren.OPERATOR_PARAMETER] == "axiom.zf.ersetzung"
        })
    }
}
