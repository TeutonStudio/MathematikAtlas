package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StrukturAnforderungsRegisterTest {
    @Test
    fun `zentrales Eigenschaftsregister trennt Strukturbedarf`() {
        assertEquals(emptySet(), MathematischeEigenschaftRegister.Endlich.strukturAnforderungen)
        assertEquals(emptySet(), MathematischeEigenschaftRegister.Abzaehlbar.strukturAnforderungen)
        assertEquals(
            setOf(StrukturAnforderung.TOPOLOGISCHER_RAUM),
            MathematischeEigenschaftRegister.Offen.strukturAnforderungen,
        )
        assertEquals(
            setOf(StrukturAnforderung.QUELL_TOPOLOGIE, StrukturAnforderung.ZIEL_TOPOLOGIE),
            MathematischeEigenschaftRegister.Stetig.strukturAnforderungen,
        )
        assertEquals(
            setOf(StrukturAnforderung.AFFINE_STRUKTUR),
            MathematischeEigenschaftRegister.KonvexeMenge.strukturAnforderungen,
        )
    }

    @Test
    fun `nackte reelle Menge erhält nur intrinsische automatische Adjektive`() {
        val ids = automatischeAdjektive(ReelleZahlen as Any).map { it.eigenschaftId }

        assertEquals(listOf("unendlich", "überabzählbar"), ids)
        assertFalse("offen" in ids)
        assertFalse("abgeschlossen" in ids)
    }
}
