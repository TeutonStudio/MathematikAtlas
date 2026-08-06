package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IterationsDefinitionenTest {
    @Test
    fun `Katalog enthaelt Ordnung drei Arten Restriktion Identitaet und Renderer`() {
        assertEquals(
            setOf(
                "iteration.ordnung",
                "iteration.multiplikation",
                "iteration.differentiation",
                "iteration.selbstkomposition",
                "methode.einschraenkung",
                "methode.identitaet.eingeschraenkt",
                "iteration.renderer",
            ),
            IterationsDefinitionsKatalog.alle.map { it.id }.toSet(),
        )
    }

    @Test
    fun `Definitionsreferenzen sind intern aufloesbar`() {
        val pruefungen = IterationsDefinitionsKatalog.register.pruefeAlle()

        assertEquals(IterationsDefinitionsKatalog.alle.size, pruefungen.size)
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Ungueltig })
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Unvollstaendig })
    }

    @Test
    fun `Restriktion dokumentiert Zielmengenerhalt und Normalisierung`() {
        val definition = assertIs<ImpliziteDefinition>(
            assertNotNull(IterationsDefinitionsKatalog.register["methode.einschraenkung"]),
        )

        assertEquals(
            setOf(
                "methode.einschraenkung.domain",
                "methode.einschraenkung.target",
                "methode.einschraenkung.image",
                "methode.einschraenkung.normalisierung",
            ),
            definition.charakterisierendeRegeln.map { it.id }.toSet(),
        )
        assertTrue(definition.charakterisierendeRegeln.any { it.folgerungLatex.contains("Ziel") })
    }

    @Test
    fun `drei Iterationsdefinitionen verwenden getrennte Operator IDs`() {
        val ids = listOf(
            "iteration.multiplikation",
            "iteration.differentiation",
            "iteration.selbstkomposition",
        ).map { id ->
            val definition = assertNotNull(IterationsDefinitionsKatalog.register[id])
            (definition.ziel as DefinitionsZiel.Operation).operatorId
        }

        assertEquals(3, ids.distinct().size)
        assertEquals(IterationsArt.entries.map { it.operatorId }, ids)
    }
}
