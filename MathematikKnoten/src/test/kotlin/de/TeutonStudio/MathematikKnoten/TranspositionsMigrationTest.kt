package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class TranspositionsMigrationTest {
    @Test fun `alte Transpositionsvarianten werden mit unveränderten Anschluss IDs migriert`() {
        val alte = listOf(
            MathematikKnotenVorlagen.TransponiereSpalte,
            MathematikKnotenVorlagen.TransponiereZeile,
            MathematikKnotenVorlagen.TransponiereMatrix,
        ).mapIndexed { index, vorlage -> vorlage.erzeuge(GraphPunkt(index * 250f, 0f)) }
        val vorherigeIds = alte.associate { it.id to it.anschlüsse.map(AnschlussDaten::id) }
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(alte[0].id, alte[0].anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id),
            zu = AnschlussVerweis(alte[1].id, alte[1].anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.id),
        )
        val karte = KartenDaten(name = "Alt", knoten = alte, verbindungen = listOf(verbindung))

        val migriert = migriereTranspositionsKnoten(karte)

        assertTrue(migriert.knoten.all { it.art == "mathematik.transponieren" })
        migriert.knoten.forEach { knoten ->
            assertEquals(vorherigeIds.getValue(knoten.id), knoten.anschlüsse.map(AnschlussDaten::id))
            assertEquals(listOf("wert", "wert"), knoten.anschlüsse.map(AnschlussDaten::name))
        }
        assertEquals(listOf(verbindung), migriert.verbindungen)
    }
}
