package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DifferentialDefinitionenTest {
    @Test fun `Katalog enthaelt Operator Formen Ordnung Bereiche und Modelle`() {
        assertEquals(setOf("differential.operator","differential.methodenForm","differential.termForm","differential.partiell","differential.ordnung","differential.werteVorrat","differential.zielRaum","differential.begriffe"), DifferentialDefinitionsKatalog.alle.map { it.id }.toSet())
    }
    @Test fun `Definitionsreferenzen sind intern aufloesbar`() {
        val pruefungen = DifferentialDefinitionsKatalog.register.pruefeAlle()
        assertEquals(DifferentialDefinitionsKatalog.alle.size, pruefungen.size)
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Ungueltig })
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Unvollstaendig })
    }
    @Test fun `Ordnungsdefinition trennt Nullfall Rekursion und Renderer`() {
        val definition = assertIs<ImpliziteDefinition>(assertNotNull(DifferentialDefinitionsKatalog.register["differential.ordnung"]))
        assertEquals(setOf("differential.ordnung.null","differential.ordnung.rekursion","differential.ordnung.renderer"), definition.charakterisierendeRegeln.map { it.id }.toSet())
        assertTrue(definition.charakterisierendeRegeln.any { it.folgerungLatex.contains("mathrm{IV}") })
    }
    @Test fun `Methoden und Termform bleiben getrennte Operatorziele`() {
        val methode = assertNotNull(DifferentialDefinitionsKatalog.register["differential.methodenForm"])
        val term = assertNotNull(DifferentialDefinitionsKatalog.register["differential.termForm"])
        assertEquals("analysis.differential.methode", (methode.ziel as DefinitionsZiel.Operation).operatorId)
        assertEquals("analysis.differential.term", (term.ziel as DefinitionsZiel.Operation).operatorId)
        assertTrue(term.referenzen.contains(methode.id))
    }
}
