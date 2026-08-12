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
    fun `Gleichheit wird ueber ununterscheidbare Mengenmitgliedschaft definiert`() {
        val definition = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.gleichheit"))
        val formel = definition.definitionsFormel

        assertEquals(
            "a=b\\Longleftrightarrow\\forall M\\left(\\operatorname{Menge}(M)\\Rightarrow\\left(a\\in M\\Leftrightarrow b\\in M\\right)\\right)",
            formel.latex,
        )
        assertEquals(setOf("axiom.zf.paarmenge"), formel.vorausgesetzteAxiomIds)
        assertTrue("Typbedingung" in formel.hinweis)
    }

    @Test
    fun `Ungleichheit ist die existentielle Gegenform der Gleichheitsdefinition`() {
        val definition = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.ungleichheit"))
        val formel = definition.definitionsFormel

        assertEquals(
            "a\\neq b\\Longleftrightarrow\\exists M\\left(\\operatorname{Menge}(M)\\land\\neg\\left(a\\in M\\Leftrightarrow b\\in M\\right)\\right)",
            formel.latex,
        )
        assertEquals(setOf("axiom.zf.paarmenge"), formel.vorausgesetzteAxiomIds)
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
