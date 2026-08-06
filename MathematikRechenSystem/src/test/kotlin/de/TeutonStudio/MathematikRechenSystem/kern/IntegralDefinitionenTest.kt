package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntegralDefinitionenTest {
    @Test
    fun `Katalog enthaelt Formen Bindung Bereich Mass und Semantiken`() {
        assertEquals(
            setOf(
                "integral.operator",
                "integral.methodenForm",
                "integral.termForm",
                "integral.bindung",
                "integral.bereich",
                "integral.mass",
                "integral.riemann",
                "integral.zaehlmass",
                "integral.nichtstandard",
                "integral.klassischeGrenzen",
            ),
            IntegralDefinitionsKatalog.alle.map { it.id }.toSet(),
        )
    }

    @Test
    fun `Definitionsreferenzen sind intern aufloesbar`() {
        val pruefungen = IntegralDefinitionsKatalog.register.pruefeAlle()

        assertEquals(IntegralDefinitionsKatalog.alle.size, pruefungen.size)
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Ungueltig })
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Unvollstaendig })
    }

    @Test
    fun `Massdefinition trennt reelles diskretes und explizites Mass`() {
        val definition = assertIs<ImpliziteDefinition>(
            assertNotNull(IntegralDefinitionsKatalog.register["integral.mass"]),
        )

        assertEquals(
            setOf(
                "integral.mass.reell",
                "integral.mass.diskret",
                "integral.mass.explizit",
            ),
            definition.charakterisierendeRegeln.map { it.id }.toSet(),
        )
        assertIs<NachweisStatus.Bedingt>(definition.eindeutigkeitsStatus)
    }

    @Test
    fun `Methoden und Termform bleiben getrennte Ziele`() {
        val methode = assertNotNull(IntegralDefinitionsKatalog.register["integral.methodenForm"])
        val term = assertNotNull(IntegralDefinitionsKatalog.register["integral.termForm"])

        assertEquals("analysis.integral.methode", (methode.ziel as DefinitionsZiel.Operation).operatorId)
        assertEquals("analysis.integral.term", (term.ziel as DefinitionsZiel.Operation).operatorId)
        assertTrue(term.referenzen.contains(methode.id))
    }

    @Test
    fun `Nichtstandarddefinition traegt drei explizite Voraussetzungen`() {
        val definition = assertIs<ImpliziteDefinition>(
            assertNotNull(IntegralDefinitionsKatalog.register["integral.nichtstandard"]),
        )
        val status = assertIs<NachweisStatus.Bedingt>(definition.existenzStatus)

        assertEquals(3, status.voraussetzungen.size)
        assertTrue(definition.charakterisierendeRegeln.any { it.folgerungLatex.contains("operatorname{st}") })
    }
}
