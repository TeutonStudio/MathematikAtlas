package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZahlenRechnerOperatorAufloesungTest {
    private fun basis(): KnotenDaten = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)

    @Test
    fun `Polynomzustand kann ohne Exception zu Standardoperator konfiguriert werden`() {
        val polynom = konfiguriereErweitertenZahlenRechner(
            basis(),
            ErweiterterZahlenOperator.POLYNOM,
        )

        val addition = konfiguriereStandardZahlenRechner(
            polynom,
            UniversellerZahlenOperator.ADDITION,
        )

        assertEquals(UniversellerZahlenOperator.ADDITION.stabileId, addition.parameter[ZAHLENRECHNER_OPERATOR])
        assertEquals(UniversellerZahlenOperator.ADDITION.titel, addition.name)
    }

    @Test
    fun `alle persistierbaren Zahlenrechnerzustände können alle Standardkandidaten erzeugen`() {
        val ausgangszustände = buildList {
            UniversellerZahlenOperator.entries.forEach { operator ->
                add(konfiguriereZahlenRechner(basis(), operator = operator))
            }
            ErweiterterZahlenOperator.entries.forEach { operator ->
                add(konfiguriereErweitertenZahlenRechner(basis(), operator))
            }
            add(
                basis().copy(
                    name = "Formel",
                    parameter = basis().parameter + (ZAHLENRECHNER_OPERATOR to ZAHLENRECHNER_FORMEL_ID),
                ),
            )
        }

        ausgangszustände.forEach { ausgang ->
            UniversellerZahlenOperator.entries.forEach { ziel ->
                val kandidat = konfiguriereStandardZahlenRechner(ausgang, ziel)
                assertEquals(ziel.stabileId, kandidat.parameter[ZAHLENRECHNER_OPERATOR])
            }
        }
    }

    @Test
    fun `familienweite Titelauflösung kennt Standard Erweiterung Formel und unbekannt`() {
        assertEquals(
            UniversellerZahlenOperator.ADDITION.titel,
            zahlenRechnerOperatorTitelOderNull(UniversellerZahlenOperator.ADDITION.stabileId),
        )
        assertEquals(
            ErweiterterZahlenOperator.POLYNOM.titel,
            zahlenRechnerOperatorTitelOderNull(ErweiterterZahlenOperator.POLYNOM.stabileId),
        )
        assertEquals("Formel", zahlenRechnerOperatorTitelOderNull(ZAHLENRECHNER_FORMEL_ID))
        assertNull(zahlenRechnerOperatorTitelOderNull("zahl.nichtVorhanden"))
    }

    @Test
    fun `benutzerdefinierter Polynomname bleibt beim Standardoperatorwechsel erhalten`() {
        val polynom = konfiguriereErweitertenZahlenRechner(
            basis(),
            ErweiterterZahlenOperator.POLYNOM,
        ).copy(name = "Mein Polynom")

        val kandidat = konfiguriereStandardZahlenRechner(
            polynom,
            UniversellerZahlenOperator.DIFFERENTIAL,
        )

        assertEquals("Mein Polynom", kandidat.name)
    }

    @Test
    fun `unbekannte persistierte ID wirft beim bestätigten Wechsel nicht und behält eigenen Namen`() {
        val unbekannt = basis().copy(
            name = "Historischer Rechner",
            parameter = basis().parameter + (ZAHLENRECHNER_OPERATOR to "zahl.nichtVorhanden"),
        )

        val kandidat = konfiguriereStandardZahlenRechner(
            unbekannt,
            UniversellerZahlenOperator.MULTIPLIKATION,
        )

        assertEquals("Historischer Rechner", kandidat.name)
        assertEquals(
            UniversellerZahlenOperator.MULTIPLIKATION.stabileId,
            kandidat.parameter[ZAHLENRECHNER_OPERATOR],
        )
    }
}
