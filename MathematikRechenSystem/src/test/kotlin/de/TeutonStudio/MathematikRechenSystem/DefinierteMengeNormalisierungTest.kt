package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.DefinierteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.GebundeneMengenVariable
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.Vergleich
import de.TeutonStudio.MathematikRechenSystem.kern.VergleichsArt
import de.TeutonStudio.MathematikRechenSystem.kern.normalisiereDefinierteMenge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DefinierteMengeNormalisierungTest {
    private val x = Variable("x")

    @Test
    fun `entscheidbar wahre konstante Bedingung kollabiert zur Grundmenge`() {
        val menge = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen)),
            Vergleich(RationaleZahl.von(2), VergleichsArt.GrößerGleich, RationaleZahl.Null),
        )

        assertEquals(ReelleZahlen, normalisiereDefinierteMenge(menge))
    }

    @Test
    fun `entscheidbar falsche konstante Bedingung kollabiert zur leeren Menge`() {
        val menge = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen)),
            Vergleich(RationaleZahl.von(2), VergleichsArt.Kleiner, RationaleZahl.Null),
        )

        assertEquals(LeereMenge, normalisiereDefinierteMenge(menge))
    }

    @Test
    fun `variable unentscheidbare Bedingung bleibt strukturell erhalten`() {
        val menge = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen)),
            Vergleich(x, VergleichsArt.GrößerGleich, RationaleZahl.Null),
        )

        assertSame(menge, normalisiereDefinierteMenge(menge))
    }
}