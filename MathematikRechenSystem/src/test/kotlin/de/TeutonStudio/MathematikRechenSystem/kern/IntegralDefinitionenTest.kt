package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntegralDefinitionenTest {
    @Test
    fun `Katalog enthaelt alle zehn geplanten Integralkarten`() {
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
    fun `alle Integralreferenzen sind aufloesbar`() {
        val pruefungen = IntegralDefinitionsKatalog.register.pruefeAlle()

        assertEquals(IntegralDefinitionsKatalog.alle.size, pruefungen.size)
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Ungueltig })
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Unvollstaendig })
    }

    @Test
    fun `Methodenform und Termform bleiben unterschiedliche Operatorziele`() {
        val methode = assertNotNull(IntegralDefinitionsKatalog.register["integral.methodenForm"])
        val term = assertNotNull(IntegralDefinitionsKatalog.register["integral.termForm"])

        assertEquals("analysis.integral.methode", (methode.ziel as DefinitionsZiel.Operation).operatorId)
        assertEquals("analysis.integral.term", (term.ziel as DefinitionsZiel.Operation).operatorId)
        assertTrue(term.referenzen.contains(methode.id))
    }

    @Test
    fun `Massdefinition dokumentiert Ableitung und expliziten Fallback`() {
        val mass = assertIs<ImpliziteDefinition>(
            assertNotNull(IntegralDefinitionsKatalog.register["integral.mass"]),
        )

        assertEquals(
            setOf("integral.mass.reell", "integral.mass.diskret", "integral.mass.explizit"),
            mass.charakterisierendeRegeln.map { it.id }.toSet(),
        )
        assertIs<NachweisStatus.Bedingt>(mass.eindeutigkeitsStatus)
    }

    @Test
    fun `Nichtstandardkarte traegt alle drei notwendigen Voraussetzungen`() {
        val definition = assertIs<ImpliziteDefinition>(
            assertNotNull(IntegralDefinitionsKatalog.register["integral.nichtstandard"]),
        )

        val status = assertIs<NachweisStatus.Bedingt>(definition.existenzStatus)
        assertEquals(3, status.bedingungen.size)
        assertTrue(definition.charakterisierendeRegeln.any { it.id == "integral.nichtstandard.unabhaengig" })
    }
}
