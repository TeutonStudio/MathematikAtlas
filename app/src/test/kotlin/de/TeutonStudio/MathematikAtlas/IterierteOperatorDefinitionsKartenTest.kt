package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikKartenAdapter.FALTUNGSKONSTRUKTOR_ART
import de.TeutonStudio.MathematikKartenAdapter.FALTUNGSDEFINATOR_ART
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IterierteOperatorDefinitionsKartenTest {
    private val vorlagen = alleMathematikKnotenVorlagen().filter { vorlage ->
        vorlage.art in setOf(
            MathematikKnotenVorlagen.IterierteSumme.art,
            MathematikKnotenVorlagen.IteriertesProdukt.art,
            MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART,
            MathematikKnotenVorlagen.IterierteVereinigung.art,
            MathematikKnotenVorlagen.IterierterSchnitt.art,
            MathematikKnotenVorlagen.IteriertesKartesischesProdukt.art,
        )
    }

    @Test
    fun `alle Varianten erhalten deterministische selbstbezugfreie Definitionskarten`() {
        vorlagen.forEachIndexed { index, vorlage ->
            val erste = TestDefinitionsKarten.definitionsKarte(vorlage, index)
            val zweite = TestDefinitionsKarten.definitionsKarte(vorlage, index)

            assertFalse(erste.knoten.any { it.art == vorlage.art }, vorlage.name)
            assertEquals(erste.knoten.map { it.id }, zweite.knoten.map { it.id }, vorlage.name)
            assertEquals(erste.verbindungen.map { it.id }, zweite.verbindungen.map { it.id }, vorlage.name)
            assertEquals(1, erste.knoten.count { it.art == FALTUNGSKONSTRUKTOR_ART }, vorlage.name)
            assertEquals(1, erste.knoten.count { it.art == FALTUNGSDEFINATOR_ART }, vorlage.name)
        }
    }

    @Test
    fun `Definitionskarten werden mit symbolischen Eingängen vollständig ausgewertet`() {
        val auswerter = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())

        vorlagen.forEachIndexed { index, vorlage ->
            val karte = TestDefinitionsKarten.definitionsKarte(vorlage, index)
            val ergebnis = auswerter.werteKonzeptKarteAus(karte)

            assertTrue(ergebnis.fehler.isEmpty(), "${vorlage.name}: ${ergebnis.fehler.joinToString()}")
            val ausgang = karte.knoten.single { it.art == "mathematik.kartenAusgang" }
            assertTrue(ergebnis.knoten[ausgang.id]?.ausgaben?.get("wert") != null, vorlage.name)
        }
    }
}
