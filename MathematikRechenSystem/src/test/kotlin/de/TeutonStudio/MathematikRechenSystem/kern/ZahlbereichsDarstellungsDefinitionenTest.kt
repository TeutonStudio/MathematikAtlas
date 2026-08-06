package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ZahlbereichsDarstellungsDefinitionenTest {
    @Test
    fun `Matrixdarstellungen sind echte Definitionskarten Eintraege`() {
        val definitionen = ZahlbereichsDefinitionsKatalog.alle.associateBy { it.id }
        val komplex = assertIs<ExpliziteDefinition>(
            definitionen.getValue("definition.zahlbereich.darstellung.C.M2R"),
        )
        val quaternion = assertIs<ExpliziteDefinition>(
            definitionen.getValue("definition.zahlbereich.darstellung.H.M2C"),
        )

        assertEquals(
            "zahlbereich.darstellung.C.M2R",
            assertIs<DefinitionsZiel.Operation>(komplex.ziel).operatorId,
        )
        assertEquals(
            "zahlbereich.darstellung.H.M2C",
            assertIs<DefinitionsZiel.Operation>(quaternion.ziel).operatorId,
        )
        assertTrue(komplex.wert.zuLatex().contains("a+bi"))
        assertTrue(quaternion.wert.zuLatex().contains("a+bi+cj+dk"))
        assertTrue(komplex.wert.zuLatex().contains("pmatrix"))
        assertTrue(quaternion.wert.zuLatex().contains("pmatrix"))
    }

    @Test
    fun `Darstellungsdefinitionen bestehen die Registerpruefung`() {
        val bekannteIds = ZahlbereichsDefinitionsKatalog.alle.mapTo(linkedSetOf()) { it.id }

        assertEquals(
            DefinitionsPruefung.Gueltig,
            DefinitionsPruefer.pruefe(
                ZahlbereichsDefinitionsKatalog.komplexeMatrixdarstellung,
                bekannteIds,
            ),
        )
        assertEquals(
            DefinitionsPruefung.Gueltig,
            DefinitionsPruefer.pruefe(
                ZahlbereichsDefinitionsKatalog.quaternionenMatrixdarstellung,
                bekannteIds,
            ),
        )
    }
}
