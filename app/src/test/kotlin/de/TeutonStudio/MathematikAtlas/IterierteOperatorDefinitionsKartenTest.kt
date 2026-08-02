package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKartenAdapter.FALTUNGSKONSTRUKTOR_ART
import de.TeutonStudio.MathematikKartenAdapter.FALTUNGSDEFINATOR_ART
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_ELEMENTART
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_PAAR
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINATOR_ART
import de.TeutonStudio.MathematikKartenAdapter.MENGENKONSTRUKTOR_ART
import de.TeutonStudio.MathematikKartenAdapter.METHODEN_AUFRUF_ART
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IterierteOperatorDefinitionsKartenTest {
    private val iterierteMengenArten = setOf(
        MathematikKnotenVorlagen.IterierteVereinigung.art,
        MathematikKnotenVorlagen.IterierterSchnitt.art,
    )
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
            if (vorlage.art in iterierteMengenArten) {
                assertEquals(0, erste.knoten.count { it.art == FALTUNGSKONSTRUKTOR_ART }, vorlage.name)
                assertEquals(0, erste.knoten.count { it.art == FALTUNGSDEFINATOR_ART }, vorlage.name)
            } else {
                assertEquals(1, erste.knoten.count { it.art == FALTUNGSKONSTRUKTOR_ART }, vorlage.name)
                assertEquals(1, erste.knoten.count { it.art == FALTUNGSDEFINATOR_ART }, vorlage.name)
            }
        }
    }

    @Test
    fun `iterierte Mengenfunktionen verwenden Praedikat und Mengendefinition statt Faltung`() {
        val erwarteteOperatoren = mapOf(
            MathematikKnotenVorlagen.IterierteVereinigung.art to "disjunktion",
            MathematikKnotenVorlagen.IterierterSchnitt.art to "konjunktion",
        )

        vorlagen.filter { it.art in iterierteMengenArten }.forEachIndexed { index, vorlage ->
            val karte = TestDefinitionsKarten.definitionsKarte(vorlage, index)
            val eingänge = karte.knoten.filter { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART }
            val konstruktor = karte.knoten.single { it.art == MENGENKONSTRUKTOR_ART }
            val definator = karte.knoten.single { it.art == MENGENDEFINATOR_ART }
            val methodenAufruf = karte.knoten.single { it.art == METHODEN_AUFRUF_ART }
            val iteration = karte.knoten.single {
                it.art == MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART
            }

            assertEquals(listOf("A", "I"), eingänge.map { it.name }, vorlage.name)
            assertEquals(konstruktor.parameter[MENGENDEFINITION_PAAR], definator.parameter[MENGENDEFINITION_PAAR])
            assertEquals(MathematikAnschlussArten.Objekt.id.wert, konstruktor.parameter[MENGENDEFINITION_ELEMENTART])
            assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.Element.art }, vorlage.name)
            assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.AussageZuMethode.art }, vorlage.name)
            assertEquals(erwarteteOperatoren.getValue(vorlage.art), iteration.parameter["operator"], vorlage.name)
            assertEquals(
                listOf("argument-0"),
                methodenAufruf.anschlüsse
                    .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
                    .map { it.name },
                vorlage.name,
            )
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
