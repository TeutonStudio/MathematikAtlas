package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationsDefinitionsFormelnTest {
    @Test
    fun `jede registrierte Relation besitzt eine kanonische Definitionsformel`() {
        assertTrue(
            RelationsDefinitionsFormeln.fehlendeDefinitionen().isEmpty(),
            "Für registrierte Relationsoperatoren fehlen Definitionsformeln: ${RelationsDefinitionsFormeln.fehlendeDefinitionen()}",
        )
        RelationsOperatoren.alle.forEach { definition ->
            assertTrue(definition.definitionsFormel.latex.isNotBlank(), definition.stabileId)
        }
    }

    @Test
    fun `Selbigkeit verwendet Mitgliedschaft und Gleichheit verwendet Inhalte`() {
        val gleich = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.gleichheit")).definitionsFormel
        val selbig = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.selbigkeit")).definitionsFormel
        assertTrue("Inhaltsgleich" in gleich.latex)
        assertTrue("\\forall M" in selbig.latex)
        assertEquals(setOf("axiom.zf.paarmenge"), selbig.vorausgesetzteAxiomIds)
        assertTrue("Selbigkeit" in selbig.hinweis)
    }

    @Test
    fun `Ungleichheit negiert Inhalte und nicht Mitgliedschaft`() {
        val formel = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.ungleichheit")).definitionsFormel
        assertEquals("a\\neq b\\Longleftrightarrow\\neg(a=b)", formel.latex)
        assertTrue(formel.vorausgesetzteAxiomIds.isEmpty())
    }

    @Test
    fun `kombinierte Ordnungsrelationen bauen auf strikter Ordnung und Gleichheit auf`() {
        assertEquals(
            "a\\le b\\Longleftrightarrow(a<b)\\lor(a=b)",
            checkNotNull(RelationsOperatoren.vonIdOderNull("relation.kleinerGleich")).definitionsFormel.latex,
        )
        assertEquals(
            "a\\ge b\\Longleftrightarrow(a>b)\\lor(a=b)",
            checkNotNull(RelationsOperatoren.vonIdOderNull("relation.groesserGleich")).definitionsFormel.latex,
        )
        assertEquals(
            "a>b\\Longleftrightarrow b<a",
            checkNotNull(RelationsOperatoren.vonIdOderNull("relation.groesser")).definitionsFormel.latex,
        )
    }

    @Test
    fun `Mengeninklusion wird elementweise und echte Inklusion zusaetzlich ueber Ungleichheit definiert`() {
        assertEquals(
            "A\\subseteq B\\Longleftrightarrow\\forall x\\left(x\\in A\\Rightarrow x\\in B\\right)",
            checkNotNull(RelationsOperatoren.vonIdOderNull(MengenRelationsOperator.TEIL_ODER_GLEICHMENGE.stabileId))
                .definitionsFormel.latex,
        )
        assertEquals(
            "A\\subset B\\Longleftrightarrow(A\\subseteq B)\\land(A\\neq B)",
            checkNotNull(RelationsOperatoren.vonIdOderNull(MengenRelationsOperator.TEILMENGE.stabileId))
                .definitionsFormel.latex,
        )
    }
}
