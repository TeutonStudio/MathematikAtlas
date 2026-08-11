package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RelationsOperatorenTest {
    @Test
    fun `Gleichheit ist als Aequivalenzrelation zertifiziert`() {
        val relation = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.gleichheit"))
        val struktur = checkNotNull(relation.relationsStruktur)

        assertTrue(RelationsKlasse.AEQUIVALENZRELATION in struktur.klassen())
        assertEquals(NachweisStatus.Nachgewiesen, struktur.status(RelationsAxiom.REFLEXIV))
        assertEquals(NachweisStatus.Nachgewiesen, struktur.status(RelationsAxiom.SYMMETRISCH))
        assertEquals(NachweisStatus.Nachgewiesen, struktur.status(RelationsAxiom.TRANSITIV))
    }

    @Test
    fun `Teilmenge ist Halbordnung aber keine behauptete Totalordnung`() {
        val relation = checkNotNull(
            RelationsOperatoren.vonIdOderNull(MengenRelationsOperator.TEIL_ODER_GLEICHMENGE.stabileId),
        )
        val klassen = checkNotNull(relation.relationsStruktur).klassen()

        assertTrue(RelationsKlasse.HALBORDNUNG in klassen)
        assertTrue(RelationsKlasse.TOTALORDNUNG !in klassen)
    }

    @Test
    fun `Mengenrelationsnamen unterscheiden echte und nicht strikte Inklusion`() {
        assertEquals("Echte Teilmenge", MengenRelationsOperator.TEILMENGE.titel)
        assertEquals("Echte Übermenge", MengenRelationsOperator.UEBERMENGE.titel)
        assertEquals("Teilmenge", MengenRelationsOperator.TEIL_ODER_GLEICHMENGE.titel)
        assertEquals("Übermenge", MengenRelationsOperator.UEBER_ODER_GLEICHMENGE.titel)
    }

    @Test
    fun `numerischer Vergleich behauptet ohne zertifizierten Traeger keine Ordnungsklasse`() {
        val relation = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.kleiner"))

        assertTrue(checkNotNull(relation.relationsStruktur).kompakteKlassen().isEmpty())
    }

    @Test
    fun `Relationsdefinition wertet Argumentrollen in deklarierter Reihenfolge aus`() {
        val relation = checkNotNull(RelationsOperatoren.vonIdOderNull("relation.gleichheit"))
        val aussage = relation.werteAus(
            mapOf(
                "rechts" to RationaleZahl.von(2),
                "links" to RationaleZahl.Eins,
            ),
        )

        val gleichheit = assertIs<Gleichheit>(aussage)
        assertEquals(RationaleZahl.Eins, gleichheit.links)
        assertEquals(RationaleZahl.von(2), gleichheit.rechts)
    }
}
