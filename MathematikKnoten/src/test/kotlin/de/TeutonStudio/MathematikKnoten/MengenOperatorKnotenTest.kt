package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MengenOperatorKnotenTest {
    @Test
    fun `sichtbarer mengenrechner umfasst exakt die acht konsolidierten operatoren`() {
        assertEquals(
            setOf(
                MengenRechnerOperator.SCHNITT,
                MengenRechnerOperator.VEREINIGUNG,
                MengenRechnerOperator.DIFFERENZ,
                MengenRechnerOperator.KARTESISCHES_PRODUKT,
                MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT,
                MengenRechnerOperator.SYMMETRISCHE_DIFFERENZ,
                MengenRechnerOperator.ITERIERTE_VEREINIGUNG,
                MengenRechnerOperator.ITERIERTER_SCHNITT,
            ),
            sichtbareMengenRechnerOperatoren().toSet(),
        )
    }

    @Test
    fun `iterierte mengenoperatoren besitzen methode und indexmenge`() {
        val basis = MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        listOf(
            MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT,
            MengenRechnerOperator.ITERIERTE_VEREINIGUNG,
            MengenRechnerOperator.ITERIERTER_SCHNITT,
        ).forEach { operator ->
            val knoten = konfiguriereMengenRechner(basis, operator)
            val eingangsNamen = knoten.anschlüsse
                .filter { it.richtung == AnschlussRichtung.Eingang }
                .sortedBy { it.reihenfolge }
                .map { it.name }
            assertEquals(listOf("methode", "indexmenge"), eingangsNamen)
        }
    }

    @Test
    fun `elementrelation wechselt auf objekt und menge`() {
        val basis = MengenRelationsKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereMengenRelation(basis, MengenRelationsOperator.ELEMENT)
        val eingaenge = knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }

        assertEquals(listOf("element", "menge"), eingaenge.map { it.name })
        assertEquals(MathematikAnschlussArten.Objekt.id, eingaenge[0].art)
        assertEquals(MathematikAnschlussArten.Menge.id, eingaenge[1].art)
    }

    @Test
    fun `historische einzelknoten migrieren idempotent`() {
        val alt = MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(
            art = "mathematik.vereinigung",
            parameter = emptyMap(),
        )
        val einmal = KartenDaten(knoten = listOf(alt)).migriereMengenOperatorKnoten()
        val zweimal = einmal.migriereMengenOperatorKnoten()

        assertEquals(MengenRechner.KNOTEN_ART, einmal.knoten.single().art)
        assertEquals(MengenRechnerOperator.VEREINIGUNG.stabileId, einmal.knoten.single().parameter[MENGENRECHNER_OPERATOR_PARAMETER])
        assertEquals(einmal, zweimal)
    }

    @Test
    fun `alte relationen migrieren in mengenrelationsknoten`() {
        val alt = MengenRelationsKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(
            art = "mathematik.disjunkt",
            parameter = emptyMap(),
        )
        val migriert = KartenDaten(knoten = listOf(alt)).migriereMengenOperatorKnoten().knoten.single()

        assertEquals(MengenRelationRechner.KNOTEN_ART, migriert.art)
        assertEquals(MengenRelationsOperator.DISJUNKT.stabileId, migriert.parameter[MENGENRELATION_OPERATOR_PARAMETER])
    }

    @Test
    fun `mass und mengennorm sind typisiert`() {
        val mass = MengenMassKnotenVorlagen.Mass.erzeuge(GraphPunkt.Zero)
        val norm = MengenMassKnotenVorlagen.MengenNorm.erzeuge(GraphPunkt.Zero)

        assertEquals(MathematikAnschlussArten.Mass.id, mass.anschlüsse.single().art)
        assertTrue(norm.anschlüsse.any { it.richtung == AnschlussRichtung.Eingang && it.name == "maß" && it.art == MathematikAnschlussArten.Mass.id })
        assertTrue(norm.anschlüsse.any { it.richtung == AnschlussRichtung.Ausgang && it.name == "wert" && it.art == MathematikAnschlussArten.Zahl.id })
    }
}
