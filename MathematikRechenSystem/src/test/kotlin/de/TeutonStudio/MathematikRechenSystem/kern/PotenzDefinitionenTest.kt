package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PotenzDefinitionenTest {
    @Test
    fun `Katalog enthaelt Struktur Rekursion Methoden Matrix und Mehrdeutigkeit`() {
        assertEquals(
            setOf(
                "potenz.struktur",
                "potenz.natuerlich",
                "potenz.ordnungsFaelle",
                "potenz.methode.punktweise",
                "potenz.matrix",
                "potenz.nichtkommutativ",
                "potenz.mehrdeutigeProdukte",
                "potenz.persistenz",
            ),
            PotenzDefinitionsKatalog.alle.map { it.id }.toSet(),
        )
    }

    @Test
    fun `Definitionsreferenzen sind intern aufloesbar`() {
        val pruefungen = PotenzDefinitionsKatalog.register.pruefeAlle()

        assertEquals(PotenzDefinitionsKatalog.alle.size, pruefungen.size)
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Ungueltig })
        assertFalse(pruefungen.values.any { it is DefinitionsPruefung.Unvollstaendig })
    }

    @Test
    fun `natuerliche Potenz besitzt Null Eins und Rekursionsregel`() {
        val definition = assertIs<InduktiveDefinition>(
            assertNotNull(PotenzDefinitionsKatalog.register["potenz.natuerlich"]),
        )

        assertEquals(setOf("potenz.null", "potenz.eins"), definition.basisRegeln.map { it.id }.toSet())
        assertEquals(listOf("potenz.rekursion"), definition.abschlussRegeln.map { it.id })
        assertEquals(IterationsArt.MULTIPLIKATION.operatorId, (definition.ziel as DefinitionsZiel.Operation).operatorId)
    }

    @Test
    fun `Matrixdefinition trennt rechteckigen Einsfall und quadratische Potenzen`() {
        val definition = assertIs<ImpliziteDefinition>(
            assertNotNull(PotenzDefinitionsKatalog.register["potenz.matrix"]),
        )

        assertEquals(
            setOf("potenz.matrix.eins", "potenz.matrix.null", "potenz.matrix.hoeher"),
            definition.charakterisierendeRegeln.map { it.id }.toSet(),
        )
        assertTrue(definition.charakterisierendeRegeln.any { it.folgerungLatex.contains("I_n") })
    }

    @Test
    fun `Mehrdeutigkeitsdefinition erfindet keine kanonische Tensorpotenz`() {
        val definition = assertIs<ImpliziteDefinition>(
            assertNotNull(PotenzDefinitionsKatalog.register["potenz.mehrdeutigeProdukte"]),
        )

        assertEquals(NachweisStatus.Unentscheidbar, definition.eindeutigkeitsStatus)
        assertTrue(definition.charakterisierendeRegeln.any { it.folgerungLatex.contains("Hadamard") })
    }
}
