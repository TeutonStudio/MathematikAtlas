package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatrixProduktKonzeptTest {
    @Test
    fun `Matrixprodukt besitzt Definition und Falksches Schema`() {
        val konzept = matrixProduktKonzept()

        assertEquals(listOf("Definition", "Falksches Schema"), konzept.reiter.map { it.titel })
        assertEquals(1, konzept.reiter.count { it.rolle == KonzeptReiterRolle.Definition })
        assertEquals(MathematikKnotenVorlagen.MatrixProdukt.art, konzept.knotenArten.single())
    }

    @Test
    fun `Falk Karte dokumentiert Reihenfolge Dimension und fehlende Konjugation ohne Selbstbezug`() {
        val karte = falkschesSchemaDefinitionsKarte()
        val text = karte.knoten.joinToString(" ") { it.parameter["regel"].orEmpty() }

        assertTrue("spalten(A)=zeilen(B)" in text)
        assertTrue("aᵢₖ·bₖⱼ" in text)
        assertTrue("Keine Konjugation" in text)
        assertTrue("ℍ" in text)
        assertFalse(karte.knoten.any { it.art == MathematikKnotenVorlagen.MatrixProdukt.art })
    }
}
