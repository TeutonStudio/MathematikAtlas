package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikKnoten.ZahlenRechnerKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.konfiguriereZahlenRechnerFormel
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZahlenRechnerFormelKonzeptTest {
    @Test
    fun `Definitionskarte wird aus den Operatoren der Formel erzeugt`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val formel = konfiguriereZahlenRechnerFormel(basis, "x+y\\cdot 2")

        val konzept = zahlenRechnerFormelKonzept(formel)
        val karte = konzept.reiter.single().karte
        val operatorIds = karte.knoten
            .filter { it.art == ZAHLENRECHNER_ART }
            .mapNotNull { it.parameter[ZAHLENRECHNER_OPERATOR] }
            .toSet()
        val eingänge = karte.knoten
            .filter { it.art == KonzeptKnotenArten.EINGANG }
            .map { it.name }
            .sorted()

        assertEquals(listOf("x", "y"), eingänge)
        assertTrue(UniversellerZahlenOperator.ADDITION.stabileId in operatorIds)
        assertTrue(UniversellerZahlenOperator.MULTIPLIKATION.stabileId in operatorIds)
    }

    @Test
    fun `Formelwechsel synchronisiert freie Variablen und erhaelt gleiche Anschluss IDs`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val ersteFormel = konfiguriereZahlenRechnerFormel(basis, "x+y")
        val yId = ersteFormel.anschlüsse.single {
            it.richtung == AnschlussRichtung.Eingang && it.name == "y"
        }.id

        val zweiteFormel = konfiguriereZahlenRechnerFormel(ersteFormel, "y+z")
        val eingänge = zweiteFormel.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }

        assertEquals(listOf("y", "z"), eingänge.map { it.name })
        assertEquals(yId, eingänge.first().id)
    }
}
