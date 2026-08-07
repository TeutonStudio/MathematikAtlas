package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BeispielKartenTest {
    @Test
    fun `alle Beispielkarten lassen sich erzeugen und korrekt gerichtet verbinden`() {
        val karten = BeispielKarten.alle()

        assertTrue(karten.isNotEmpty())
        karten.forEach { karte ->
            val knotenNachId = karte.knoten.associateBy { it.id }
            karte.verbindungen.forEach { verbindung ->
                val vonKnoten = requireNotNull(knotenNachId[verbindung.von.knotenId])
                val zuKnoten = requireNotNull(knotenNachId[verbindung.zu.knotenId])
                val vonAnschluss = requireNotNull(
                    vonKnoten.anschlüsse.firstOrNull { it.id == verbindung.von.anschlussId },
                )
                val zuAnschluss = requireNotNull(
                    zuKnoten.anschlüsse.firstOrNull { it.id == verbindung.zu.anschlussId },
                )

                assertEquals(
                    AnschlussRichtung.Ausgang,
                    vonAnschluss.richtung,
                    "Quelle '${vonKnoten.name}.${vonAnschluss.name}' in Karte '${karte.name}' muss ein Ausgang sein.",
                )
                assertEquals(
                    AnschlussRichtung.Eingang,
                    zuAnschluss.richtung,
                    "Ziel '${zuKnoten.name}.${zuAnschluss.name}' in Karte '${karte.name}' muss ein Eingang sein.",
                )
            }
        }
    }
}
