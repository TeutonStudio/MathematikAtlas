package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import kotlin.test.*

class RechnerMethodenOperatorwechselTest {
    @Test
    fun `Matrixoperatorwechsel erzeugt sofort methodenfaehige Anschluesse`() {
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereStrukturRechner(
            basis,
            StrukturRechnerKnotenFamilie.MATRIX,
            "matrix.determinante",
        )

        assertTrue(knoten.anschlüsse.any {
            it.richtung == AnschlussRichtung.Eingang && MathematikAnschlussArten.Methode.id in it.zulässigeArten
        })
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
        assertNotNull(ausgang.artPriorisiertEingänge)
        assertEquals(MatrixRechner.KNOTEN_ART, knoten.art)
    }

    @Test
    fun `Vektoroperatorwechsel erzeugt sofort methodenfaehige Anschluesse`() {
        val basis = vektorRechnerVorlage(VektorRechnerOperator.ADDITION).erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereVektorRechner(basis, VektorRechnerOperator.NORM)

        assertTrue(knoten.anschlüsse.any {
            it.richtung == AnschlussRichtung.Eingang && MathematikAnschlussArten.Methode.id in it.zulässigeArten
        })
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
        assertNotNull(ausgang.artPriorisiertEingänge)
    }
}
