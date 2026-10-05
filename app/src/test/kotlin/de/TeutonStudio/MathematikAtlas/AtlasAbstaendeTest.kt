package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtlasAbstaendeTest {
    @Test
    fun `kompakte Skala ist streng geordnet und vollständig`() {
        val a = AtlasAbstände.Kompakt
        val werte = listOf(a.haarlinie, a.winzig, a.eng, a.standard, a.bereich, a.inhalt, a.dialog, a.weit)

        assertEquals(listOf(2, 4, 6, 8, 12, 16, 20, 24).map(Int::dp), werte)
        assertTrue(werte.zipWithNext().all { (links, rechts) -> links < rechts })
    }

    @Test
    fun `semantische Innenabstände verwenden die kompakte Skala`() {
        val a = AtlasAbstände.Kompakt

        assertEquals(a.standard, a.liste.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(a.eng, a.liste.calculateTopPadding())
        assertEquals(a.inhalt, a.dialogInnen.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(a.bereich, a.dialogInnen.calculateTopPadding())
        assertEquals(a.inhalt, a.aktionsLeiste.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(a.standard, a.aktionsLeiste.calculateBottomPadding())
    }
}
