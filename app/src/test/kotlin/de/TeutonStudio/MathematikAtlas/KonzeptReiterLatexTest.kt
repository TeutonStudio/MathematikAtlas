package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.text.style.BaselineShift
import de.TeutonStudio.MathematikKnoten.latexZuAnnotiertemText
import de.TeutonStudio.MathematikKnoten.vereinfacheLatexAnzeige
import kotlin.test.Test
import kotlin.test.assertEquals

class KonzeptReiterLatexTest {
    @Test
    fun `persistierte Doppel Escapes werden vor dem Rendern normalisiert`() {
        val normalisiert = normalisiereKonzeptReiterTitel("""a_{\\mathbb{N}} \\in \\mathbb R""")
        val anzeige = latexZuAnnotiertemText(normalisiert)

        assertEquals("aℕ ∈ ℝ", anzeige.text)
        assertEquals(BaselineShift.Subscript, anzeige.spanStyles.single().item.baselineShift)
    }

    @Test
    fun `Formelbegrenzer verschwinden aus Reitertiteln`() {
        assertEquals("ℝ", vereinfacheLatexAnzeige(normalisiereKonzeptReiterTitel("\\(\\mathbb{R}\\)")))
    }
}
