package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.AxiomKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_AXIOME
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_PARAMETER
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AxiomPraedikatKonzeptTest {
    @Test
    fun `jedes Axiom Prädikat besitzt eine vollständige Definitionskarte`() {
        assertTrue(AxiomOperatoren.alle.isNotEmpty())

        AxiomOperatoren.alle.forEach { definition ->
            val ursprung = AxiomKnotenVorlagen.vorlage(definition).erzeuge(GraphPunkt.Zero)
            val konzept = assertNotNull(
                axiomPraedikatKonzept(ursprung),
                "Für ${definition.stabileId} fehlt die Definitionskarte.",
            )
            val karte = konzept.reiter.single { it.rolle == KonzeptReiterRolle.Definition }.karte
            val prädikat = karte.knoten.single { it.art == RelationsOperatoren.KNOTEN_ART }
            val regel = karte.knoten.single { it.art == KonzeptKnotenArten.REGEL }
            val eingänge = karte.knoten.filter { it.art == KonzeptKnotenArten.EINGANG }
            val ausgang = karte.knoten.single { it.art == KonzeptKnotenArten.AUSGANG }

            assertEquals(definition.titel, konzept.name)
            assertEquals(definition.symbolLatex, regel.parameter["regel"])
            assertEquals(definition.argumente.map { it.rolle }, eingänge.map { it.name })
            assertEquals(definition.stabileId, prädikat.parameter[RelationsOperatoren.OPERATOR_PARAMETER])
            assertEquals(PRAEDIKAT_SEITE_AXIOME, prädikat.parameter[PRAEDIKAT_SEITE_PARAMETER])
            assertEquals(definition.argumente.size + 1, karte.verbindungen.size)
            assertEquals(1, ausgang.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang })
            assertTrue(konzept.erklärt(ursprung))
        }
    }

    @Test
    fun `Relations Prädikate werden nicht als Axiome erklärt`() {
        val relation = de.TeutonStudio.MathematikKnoten.PraedikatKnotenVorlagen.standard
            .erzeuge(GraphPunkt.Zero)

        assertEquals(null, axiomPraedikatKonzept(relation))
    }
}
