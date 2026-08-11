package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.MathematischAuswertbareMethode
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeMethode
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private data class FremdMethode(
    override val name: String,
) : Methode

class MethodenErweiterungsnahtTest {
    @Test
    fun `fremde methode kann ausserhalb des mathematikkerns implementiert werden`() {
        val fremd: Methode = FremdMethode("engine_method")

        assertEquals("engine_method", fremd.name)
        assertEquals("engine_method", fremd.zuLatex())
        assertFalse(fremd is MathematischAuswertbareMethode)
    }

    @Test
    fun `fremde methode wird nicht versehentlich mathematisch ausgewertet`() {
        val fremd: Methode = FremdMethode("engine_method")

        val fehler = assertFailsWith<IllegalStateException> {
            fremd.wendeAn(emptyMap())
        }

        assertContains(fehler.message.orEmpty(), "keine mathematische Auswertungs-Capability")
    }

    @Test
    fun `bestehender Methode Konstruktor erzeugt weiterhin mathematische Implementierung`() {
        val x = Variable("x")
        val methode: Methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertIs<MathematischeMethode>(methode)
        assertTrue(methode is MathematischAuswertbareMethode)
        assertEquals(RationaleZahl.von(2), methode.wendeAn(mapOf("x" to RationaleZahl.von(2))))
    }
}
