package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.FALTUNGSKONSTRUKTOR_ART
import de.TeutonStudio.MathematikKartenAdapter.FALTUNGSDEFINATOR_ART
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_ELEMENTART
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_PAAR
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINATOR_ART
import de.TeutonStudio.MathematikKartenAdapter.MENGENKONSTRUKTOR_ART
import de.TeutonStudio.MathematikKartenAdapter.METHODEN_AUFRUF_ART
import de.TeutonStudio.MathematikKartenAdapter.METHODEN_ZIELMENGE_ART
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
            val methodenZielmenge = karte.knoten.single { it.art == METHODEN_ZIELMENGE_ART }
            val methodenAufruf = karte.knoten.single { it.art == METHODEN_AUFRUF_ART }
            val iteration = karte.knoten.single {
                it.art == MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART
            }
            val oberMenge = konstruktor.anschlüsse.single {
                it.richtung == AnschlussRichtung.Eingang && it.name == "oberMenge"
            }
            val elementbindung = iteration.anschlüsse.single {
                it.richtung == AnschlussRichtung.Eingang && it.name == "elementbindung"
            }

            assertEquals(listOf("A", "I"), eingänge.map { it.name }, vorlage.name)
            assertEquals(konstruktor.parameter[MENGENDEFINITION_PAAR], definator.parameter[MENGENDEFINITION_PAAR])
            assertEquals(MathematikAnschlussArten.Objekt.id.wert, konstruktor.parameter[MENGENDEFINITION_ELEMENTART])
            assertEquals(MathematikAnschlussArten.Menge.id, oberMenge.art, vorlage.name)
            assertTrue(
                karte.verbindungen.any { verbindung ->
                    verbindung.von.knotenId == methodenZielmenge.id &&
                        verbindung.zu.knotenId == konstruktor.id &&
                        verbindung.zu.anschlussId == oberMenge.id
                },
                vorlage.name,
            )
            assertEquals(MathematikAnschlussArten.Objekt.id, elementbindung.art, vorlage.name)
            assertTrue(
                karte.verbindungen.any { verbindung ->
                    verbindung.von.knotenId == konstruktor.id &&
                        verbindung.zu.knotenId == iteration.id &&
                        verbindung.zu.anschlussId == elementbindung.id
                },
                vorlage.name,
            )
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

    @Test
    fun `leere Indexmenge liefert leere Vereinigung und Grundmenge des Schnitts`() {
        val grundMenge = BenannteMenge("X", "X")
        val indexMenge = BenannteMenge("I", "I")
        val familie = symbolischeMengenFamilie(grundMenge, indexMenge)
        val erwartungen = mapOf(
            MathematikKnotenVorlagen.IterierteVereinigung.art to LeereMenge,
            MathematikKnotenVorlagen.IterierterSchnitt.art to grundMenge,
        )

        vorlagen.filter { it.art in erwartungen }.forEachIndexed { index, vorlage ->
            val karte = TestDefinitionsKarten.definitionsKarte(vorlage, index)
            val ergebnis = werteMengenDefinitionAus(
                karte = karte,
                familie = familie,
                indexMenge = LeereMenge,
            )

            assertTrue(ergebnis.fehler.isEmpty(), "${vorlage.name}: ${ergebnis.fehler.joinToString()}")
            assertEquals(erwartungen.getValue(vorlage.art), ergebnis.ausgabe, vorlage.name)
        }
    }

    @Test
    fun `endliche nichtnumerische Indexfamilie bleibt ausführbar`() {
        val grundMenge = BenannteMenge("X", "X")
        val indexMenge = EndlicheMenge(
            setOf(
                TypisiertesElement("alpha", "test.index", "\\alpha"),
                TypisiertesElement("beta", "test.index", "\\beta"),
            ),
        )
        val familie = symbolischeMengenFamilie(grundMenge, indexMenge)

        vorlagen.filter { it.art in iterierteMengenArten }.forEachIndexed { index, vorlage ->
            val karte = TestDefinitionsKarten.definitionsKarte(vorlage, index)
            val ergebnis = werteMengenDefinitionAus(karte, familie, indexMenge)

            assertTrue(ergebnis.fehler.isEmpty(), "${vorlage.name}: ${ergebnis.fehler.joinToString()}")
            assertIs<MengenAusdruck>(ergebnis.ausgabe, vorlage.name)
        }
    }

    private data class AusgewerteteMengenDefinition(
        val ausgabe: MathematischesObjekt?,
        val fehler: List<String>,
    )

    private fun werteMengenDefinitionAus(
        karte: de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten,
        familie: Methode,
        indexMenge: MengenAusdruck,
    ): AusgewerteteMengenDefinition {
        val a = karte.knoten.single { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART && it.name == "A" }
        val i = karte.knoten.single { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART && it.name == "I" }
        val ausgang = karte.knoten.single { it.art == "mathematik.kartenAusgang" }
        val auswertung = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(
            karte,
            mapOf(
                a.id to mapOf("wert" to BedingterWert(familie)),
                i.id to mapOf("wert" to BedingterWert(indexMenge)),
            ),
        )
        return AusgewerteteMengenDefinition(
            ausgabe = auswertung.knoten[ausgang.id]?.ausgaben?.get("wert")?.objekt,
            fehler = auswertung.fehler,
        )
    }

    private fun symbolischeMengenFamilie(
        grundMenge: MengenAusdruck,
        indexMenge: MengenAusdruck,
    ): Methode {
        val index = Variable("i")
        val element = TypisiertesElement("familienElement", "test.objekt", "x")
        val graph = BenannteMenge("graph_A", "\\operatorname{Graph}(A)")
        val prädikat = Methode(
            name = "A_graph",
            parameter = listOf(element),
            ausgaben = mapOf("wert" to ElementBeziehung(Tupel(listOf(index, element)), graph)),
            zielMengen = mapOf(
                "wert" to EndlicheMenge(
                    setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)),
                ),
            ),
            werteVorräte = mapOf(element.name to grundMenge),
        )
        return Methode(
            name = "A",
            parameter = listOf(index),
            ausgaben = mapOf("wert" to GefilterteMenge(grundMenge, prädikat)),
            zielMengen = mapOf("wert" to grundMenge),
            werteVorräte = mapOf(index.name to indexMenge),
        )
    }
}
