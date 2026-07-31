package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_ELEMENTART
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_PAAR
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenParameter
import de.TeutonStudio.MathematikRechenSystem.kern.PrädikatsMenge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MengenoperatorDefinitionsKartenTest {
    private data class Fall(
        val vorlage: KnotenVorlage,
        val logikArt: KnotenArtId,
        val ergebnisLatex: String,
        val operatorLatex: String,
        val besitztNegation: Boolean = false,
    )

    private val fälle = listOf(
        Fall(
            MathematikKnotenVorlagen.Vereinigung,
            MathematikKnotenVorlagen.Disjunktion.art,
            "A\\cup B",
            "\\lor",
        ),
        Fall(
            MathematikKnotenVorlagen.Schnitt,
            MathematikKnotenVorlagen.Konjunktion.art,
            "A\\cap B",
            "\\land",
        ),
        Fall(
            MathematikKnotenVorlagen.Differenz,
            MathematikKnotenVorlagen.Konjunktion.art,
            "A\\setminus B",
            "\\land",
            besitztNegation = true,
        ),
        Fall(
            MengenraumKnotenVorlagen.SymmetrischeDifferenz,
            AussagenLogikKnotenVorlagen.Adjunktion.art,
            "A\\triangle B",
            "\\stackrel{\\circ}{\\lor}",
        ),
    )

    @Test
    fun `Karten verwenden stabile gekoppelte Mengendefinitionen ohne Selbstbezug`() {
        fälle.forEach { fall ->
            val karte = definitionsKarte(fall.vorlage)
            val erneut = definitionsKarte(fall.vorlage)

            assertFalse(karte.knoten.any { it.art == fall.vorlage.art }, fall.vorlage.name)
            val eingänge = karte.knoten.filter { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART }
            assertEquals(listOf("A", "B"), eingänge.sortedBy { it.position.y }.map { it.name })
            assertTrue(eingänge.all { knoten ->
                knoten.anschlüsse.single().art == MathematikAnschlussArten.Menge.id
            })

            val konstruktor = karte.knoten.single {
                it.art == MengendefinitionKnotenVorlagen.Mengenkonstruktor.art
            }
            val definator = karte.knoten.single {
                it.art == MengendefinitionKnotenVorlagen.Mengendefinator.art
            }
            val paarId = assertNotNull(konstruktor.parameter[MENGENDEFINITION_PAAR])
            assertEquals(paarId, definator.parameter[MENGENDEFINITION_PAAR])
            assertEquals(MathematikAnschlussArten.Objekt.id.wert, konstruktor.parameter[MENGENDEFINITION_ELEMENTART])
            assertEquals(MathematikAnschlussArten.Objekt.id, konstruktor.anschlüsse.single().art)
            assertEquals(2, karte.knoten.count { it.art == MathematikKnotenVorlagen.Element.art })
            assertEquals(1, karte.knoten.count { it.art == fall.logikArt })
            assertEquals(
                fall.besitztNegation,
                karte.knoten.any { it.art == AussagenLogikKnotenVorlagen.Negation.art },
            )
            if (fall.vorlage == MengenraumKnotenVorlagen.SymmetrischeDifferenz) {
                val adjunktion = karte.knoten.single { it.art == AussagenLogikKnotenVorlagen.Adjunktion.art }
                assertEquals(AUSSAGEN_LOGIK_XOR, adjunktion.parameter[AUSSAGEN_LOGIK_SEMANTIK])
            }
            assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.KartenAusgang.art })

            assertEquals(karte.knoten.map { it.id }, erneut.knoten.map { it.id })
            assertEquals(
                karte.knoten.flatMap { it.anschlüsse }.map { it.id },
                erneut.knoten.flatMap { it.anschlüsse }.map { it.id },
            )
            assertEquals(karte.verbindungen.map { it.id }, erneut.verbindungen.map { it.id })
        }
    }

    @Test
    fun `Karten werden zu typisierten Praedikatsmengen ausgewertet`() {
        val auswerter = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())

        fälle.forEach { fall ->
            val karte = definitionsKarte(fall.vorlage)
            val auswertung = auswerter.werteKonzeptKarteAus(karte)
            assertTrue(auswertung.fehler.isEmpty(), "${fall.vorlage.name}: ${auswertung.fehler.joinToString()}")

            val elementKnoten = karte.knoten.filter { it.art == MathematikKnotenVorlagen.Element.art }
            assertEquals(2, elementKnoten.size)
            elementKnoten.forEach { knoten ->
                val mengenEingang = auswertung.knoten.getValue(knoten.id).eingänge.getValue("rechts")
                assertIs<MengenParameter>(mengenEingang.objekt)
            }

            val ausgang = karte.knoten.single { it.art == MathematikKnotenVorlagen.KartenAusgang.art }
            val wert = auswertung.knoten.getValue(ausgang.id).ausgaben.getValue("wert")
            assertIs<PrädikatsMenge>(wert.objekt)
            val latex = wert.anzeigeLatex()
            assertTrue(latex.startsWith("${fall.ergebnisLatex}="), latex)
            assertTrue(latex.contains("x \\in A"), latex)
            assertTrue(latex.contains("x \\in B"), latex)
            assertTrue(latex.contains(fall.operatorLatex), latex)
            if (fall.besitztNegation) assertTrue(latex.contains("\\neg"), latex)
        }
    }

    private fun definitionsKarte(vorlage: KnotenVorlage): KartenDaten {
        val karte = TestDefinitionsKarten
            .fürKnoten(vorlage.erzeuge(GraphPunkt.Zero))
            ?.reiter
            ?.single { it.rolle == KonzeptReiterRolle.Definition }
            ?.karte
        return assertNotNull(karte, "Definitionskarte für ${vorlage.name} fehlt.")
    }
}
