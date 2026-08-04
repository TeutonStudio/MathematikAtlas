package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FormelTastaturVollstaendigkeitTest {
    @Test
    fun `Formelbauer bietet jeden universellen Zahlenoperator an`() {
        val angeboteneIds = FormelTastatur.standard.mapNotNull(FormelTastaturTaste::operatorId).toSet()
        val erwarteteIds = UniversellerZahlenOperator.entries.map(UniversellerZahlenOperator::stabileId).toSet()

        assertEquals(emptySet(), erwarteteIds - angeboteneIds)
    }

    @Test
    fun `Minimum und Maximum bleiben beim Latex Roundtrip strukturiert`() {
        listOf("\\min\\left\\{x,y\\right\\}", "\\max\\left\\{x,y\\right\\}").forEach { latex ->
            val import = FormelLatexCodec.importiere(latex)
            val ausdruck = assertIs<FormelLatexImportErgebnis.Erfolg>(import).ausdruck
            val operation = assertIs<FormelAusdruck.Operation>(ausdruck)

            assertEquals(2, operation.argumente.size)
            assertIs<FormelLatexImportErgebnis.Erfolg>(
                FormelLatexCodec.importiere(FormelLatexCodec.exportiere(operation)),
            )
        }
    }
}
