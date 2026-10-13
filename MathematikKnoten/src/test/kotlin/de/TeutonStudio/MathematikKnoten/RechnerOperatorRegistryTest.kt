package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RechnerOperatorRegistryTest {
    @Test
    fun `Registry deckt alle vier bestehenden Rechnerfamilien vollständig ab`() {
        val erwarteteZahlen = UniversellerZahlenOperator.entries.map { it.stabileId }.toSet()
        assertTrue(RechnerOperatorRegistry.fürFamilie(RechnerFamilie.ZAHL).map { it.operatorId }.toSet().containsAll(erwarteteZahlen))
        assertTrue(RechnerOperatorRegistry.fürFamilie(RechnerFamilie.VEKTOR).map { it.operatorId }.toSet()
            .containsAll(VektorRechnerOperator.entries.map { it.stabileId }))
        assertTrue(RechnerOperatorRegistry.fürFamilie(RechnerFamilie.MATRIX).map { it.operatorId }.toSet()
            .containsAll(StrukturRechnerOperatoren.fuer(StrukturRechnerKnotenFamilie.MATRIX).filterNot { it.id == "matrix.zerlegen" }.map { it.id }))
        assertTrue(RechnerOperatorRegistry.fürFamilie(RechnerFamilie.TENSOR).map { it.operatorId }.toSet()
            .containsAll(TensorRechnerOperator.entries.map { it.stabileId }))
    }

    @Test
    fun `jeder Vertrag besitzt eindeutige Rollen und mindestens einen Ausgang`() {
        RechnerOperatorRegistry.alle.forEach { vertrag ->
            assertTrue(vertrag.signaturen.isNotEmpty(), vertrag.operatorId)
            vertrag.signaturen.forEach { signatur ->
                val rollen = signatur.anschlüsse.map { it.richtung to it.name }
                assertEquals(rollen.size, rollen.distinct().size, vertrag.operatorId)
                assertTrue(signatur.ausgänge.isNotEmpty(), vertrag.operatorId)
            }
        }
    }

    @Test
    fun `unbekannte Operator ID bleibt unverändert und wird diagnostiziert`() {
        val knoten = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(
            parameter = ZahlenRechnerKnotenVorlagen.standard.standardParameter +
                (ZAHLENRECHNER_OPERATOR to "zahl.zukuenftig"),
        )

        assertIs<RechnerOperatorAuflösung.Unbekannt>(
            RechnerOperatorRegistry.finde(RechnerFamilie.ZAHL, "zahl.zukuenftig"),
        )
        assertEquals(knoten, RechnerOperatorRegistry.konfiguriere(knoten, "zahl.zukuenftig"))
    }
}
