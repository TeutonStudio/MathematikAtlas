package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikKartenLaufzeit
import de.TeutonStudio.MathematikRechenSystem.kern.OrbitBeschraenktheitsMenge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    @Test
    fun `Mandelbrot Beispiel bleibt eine ausführbare generische Orbitkarte`() {
        val karte = BeispielKarten.alle().single { it.name == "Mandelbrot-Menge als Orbitfamilie" }
        assertEquals(karte, KartenJson.lese(KartenJson.schreibe(karte)))
        val ergebnis = MathematikKartenLaufzeit().auswerten(karte)

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        val mengenKnoten = karte.knoten.single { it.art == MathematikKnotenVorlagen.OrbitBeschraenktheit.art }
        assertIs<OrbitBeschraenktheitsMenge>(
            ergebnis.knoten.getValue(mengenKnoten.id).ausgaben.getValue("menge").objekt,
        )
    }
}
