package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKnoten.ITERIERTE_SUMME_TUPEL_MODUS
import de.TeutonStudio.MathematikKnoten.MATRIXDIAGONALE_ART
import de.TeutonStudio.MathematikKnoten.MATRIXDIAGONALE_ART_PARAMETER
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.SPUR_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixDiagonalArt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpurDefinitionsKarteTest {
    @Test
    fun `Spurkonzept verwendet Hauptdiagonale und iterierte Tupelsumme`() {
        val konzept = TestDefinitionsKarten.alle.single { it.knotenArten == setOf(SPUR_ART) }
        val karte = konzept.reiter.single { it.rolle == KonzeptReiterRolle.Definition }.karte

        assertFalse(karte.knoten.any { it.art == SPUR_ART })
        val diagonale = karte.knoten.single { it.art == MATRIXDIAGONALE_ART }
        assertEquals(
            MatrixDiagonalArt.HAUPTDIAGONALE.parameterWert,
            diagonale.parameter[MATRIXDIAGONALE_ART_PARAMETER],
        )
        val summe = karte.knoten.single {
            it.art == ZAHLENRECHNER_ART &&
                it.parameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.ITERIERTE_SUMME.stabileId &&
                it.parameter["eingabeModus"] == ITERIERTE_SUMME_TUPEL_MODUS
        }
        assertTrue(summe.anschlüsse.any {
            it.richtung == AnschlussRichtung.Eingang && it.name == "tupel"
        })
        assertEquals(3, karte.verbindungen.size)
    }

    @Test
    fun `Spurknoten findet seine Definitionskarte im Katalog`() {
        val knoten = de.TeutonStudio.MathematikKnoten.SpurKnotenVorlagen.Spur
            .erzeuge(de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt.Zero)

        val konzept = TestDefinitionsKarten.fürKnoten(knoten)

        assertNotNull(konzept)
        assertEquals(KonzeptId("spur"), konzept.id)
        assertTrue(TestDefinitionsKarten.validierungsFehler().isEmpty())
    }
}
