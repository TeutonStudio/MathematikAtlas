package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HyperDefinitionenTest {
    @Test
    fun `Katalog enthaelt alle zehn geplanten Definitionskarten`() {
        assertEquals(
            setOf(
                "hyper.filteraxiome",
                "hyper.ultrapotenz",
                "hyper.erweiterung",
                "hyper.interneAussagen",
                "hyper.transfer",
                "hyper.externeBegriffe",
                "hyper.endlichUnendlich",
                "hyper.infinitesimal",
                "hyper.standardteilLimes",
                "hyper.symbolischeGrenzen",
            ),
            HyperDefinitionsKatalog.alle.map { it.id }.toSet(),
        )
    }

    @Test
    fun `alle Referenzen sind im Hyperkatalog aufloesbar`() {
        val pruefungen = HyperDefinitionsKatalog.register.pruefeAlle()

        assertEquals(HyperDefinitionsKatalog.alle.size, pruefungen.size)
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Ungueltig })
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Unvollstaendig })
    }

    @Test
    fun `Filterdefinition bleibt bedingt und nicht eindeutig materialisiert`() {
        val definition = assertIs<ImpliziteDefinition>(
            assertNotNull(HyperDefinitionsKatalog.register["hyper.filteraxiome"]),
        )

        assertIs<NachweisStatus.Bedingt>(definition.existenzStatus)
        assertIs<NachweisStatus.Unentscheidbar>(definition.eindeutigkeitsStatus)
        assertEquals(KanonischesHyperModell.modell.alleAxiome.size, definition.charakterisierendeRegeln.size)
    }

    @Test
    fun `direkter Hyper Limes und symbolischer Grenzwert bleiben getrennte Ziele`() {
        val hyper = assertNotNull(HyperDefinitionsKatalog.register["hyper.standardteilLimes"])
        val grenzwert = assertNotNull(HyperDefinitionsKatalog.register["hyper.symbolischeGrenzen"])

        assertEquals("analysis.hyperLimes", (hyper.ziel as DefinitionsZiel.Operation).operatorId)
        assertEquals("analysis.grenzwert", (grenzwert.ziel as DefinitionsZiel.Operation).operatorId)
        assertTrue(hyper.referenzen.contains("hyper.infinitesimal"))
    }
}
