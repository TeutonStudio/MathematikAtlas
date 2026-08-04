package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UniversellerZahlenOperatorIdTest {
    @Test
    fun `unbekannte Operator-IDs werden nicht als Addition interpretiert`() {
        assertEquals(null, UniversellerZahlenOperator.vonIdOderNull("zahl.nicht-registriert"))
        assertEquals(null, UniversellerZahlenOperator.vonIdOderNull(null))
        assertFailsWith<IllegalArgumentException> {
            UniversellerZahlenOperator.vonId("zahl.nicht-registriert")
        }
    }
}
