package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsAxiom
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.praedikatDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AxiomPraedikatKnotenTest {
    @Test
    fun `Peano Nachfolgerabschluss verwendet Menge und Methodenhandle`() {
        val axiom = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.peano.nachfolgerAbgeschlossen"))
        val knoten = konfigurierePraedikat(
            PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),
            axiom,
        )
        val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(PRAEDIKAT_SEITE_AXIOME, knoten.parameter[PRAEDIKAT_SEITE_PARAMETER])
        assertEquals("axiom.peano.nachfolgerAbgeschlossen", knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER])
        assertEquals(listOf("menge", "nachfolger"), eingänge.map { it.name })
        assertEquals(MathematikAnschlussArten.Menge.id, eingänge[0].art)
        assertEquals(MathematikAnschlussArten.Methode.id, eingänge[1].art)
        assertEquals(MathematikAnschlussArten.Aussage.id, knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.art)
    }

    @Test
    fun `Peano Induktion besitzt Prädikat als kanonischen Methodenanschluss`() {
        val axiom = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.peano.induktion"))
        val knoten = konfigurierePraedikat(PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), axiom)
        val praedikat = knoten.anschlüsse.single { it.name == "praedikat" }

        assertEquals(MathematikAnschlussArten.Methode.id, praedikat.art)
        assertEquals(1, axiom.argumente.single { it.rolle == "praedikat" }.stelligkeit)
    }

    @Test
    fun `Körperprädikat stellt alle Strukturbausteine als Handles bereit`() {
        val axiom = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.algebra.koerper"))
        val knoten = konfigurierePraedikat(PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), axiom)
        val rollen = knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .map { it.name }

        assertEquals(
            listOf("menge", "addition", "multiplikation", "null", "eins", "negation", "inverse"),
            rollen,
        )
    }

    @Test
    fun `Relationsseite bleibt kompatibel`() {
        val relation = RelationsOperatoren.standard()
        val knoten = konfigurierePraedikat(PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), relation)

        assertEquals(PRAEDIKAT_SEITE_RELATIONEN, knoten.parameter[PRAEDIKAT_SEITE_PARAMETER])
        assertEquals(relation.stabileId, knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER])
    }

    @Test
    fun `jede Relationsmetadatenklasse verweist auf ein echtes Axiomprädikat`() {
        RelationsAxiom.entries.forEach { relationsAxiom ->
            val definition = relationsAxiom.praedikatDefinition()
            assertNotNull(AxiomOperatoren.vonIdOderNull(definition.stabileId))
            assertTrue(definition.stabileId.startsWith("axiom.relation."))
        }
    }
}
