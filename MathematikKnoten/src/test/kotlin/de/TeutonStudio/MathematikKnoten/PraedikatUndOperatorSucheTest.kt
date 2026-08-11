package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import de.TeutonStudio.MathematikKnoten.katalog.OperatorKnotenSuchindex
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PraedikatUndOperatorSucheTest {
    @Test
    fun `historische Gleichheit migriert idempotent in Praedikat`() {
        val basis = PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val alt = basis.copy(
            art = "mathematik.gleichheit",
            name = "Gleichheit",
            parameter = emptyMap(),
        )

        val einmal = KartenDaten(name = "Relation", knoten = listOf(alt)).migrierePraedikatKnoten()
        val zweimal = einmal.migrierePraedikatKnoten()
        val migriert = einmal.knoten.single()

        assertEquals(RelationsOperatoren.KNOTEN_ART, migriert.art)
        assertEquals("relation.gleichheit", migriert.parameter[RelationsOperatoren.OPERATOR_PARAMETER])
        assertEquals(einmal, zweimal)
    }

    @Test
    fun `Suche nach Skalarprodukt liefert vorkonfigurierten Vektorrechner`() {
        val treffer = OperatorKnotenSuchindex.suche(
            "Skalarprodukt",
            KanonischerMathematikKnotenKatalog.alle(),
        )
        val vektorTreffer = treffer.firstOrNull {
            it.art == VektorRechner.KNOTEN_ART &&
                it.standardParameter[VEKTOR_RECHNER_OPERATOR] == VektorRechnerOperator.SKALARPRODUKT.stabileId
        }

        assertTrue(vektorTreffer != null)
    }

    @Test
    fun `Suche nach Gleichheit liefert vorkonfiguriertes Praedikat`() {
        val treffer = OperatorKnotenSuchindex.suche(
            "Gleichheit",
            KanonischerMathematikKnotenKatalog.alle(),
        )

        assertTrue(
            treffer.any {
                it.art == RelationsOperatoren.KNOTEN_ART &&
                    it.standardParameter[RelationsOperatoren.OPERATOR_PARAMETER] == "relation.gleichheit"
            },
        )
    }
}
