package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IterierteOperatorKonzeptReiterTest {
    private val variantenAnzahl = mapOf(
        MathematikKnotenVorlagen.IterierteSumme.art to 1,
        MathematikKnotenVorlagen.IteriertesProdukt.art to 1,
        MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART to 3,
        MathematikKnotenVorlagen.IterierteVereinigung.art to 1,
        MathematikKnotenVorlagen.IterierterSchnitt.art to 1,
        MathematikKnotenVorlagen.IteriertesKartesischesProdukt.art to 1,
    )

    @Test
    fun `jede Iterationsvariante besitzt Definition leere Indexmenge und endlichen Spezialfall`() {
        val konzepte = TestDefinitionsKarten.alle.filter { konzept ->
            konzept.knotenArten.singleOrNull() in variantenAnzahl.keys
        }

        assertEquals(variantenAnzahl.size, konzepte.size)
        konzepte.forEach { konzept ->
            val art = konzept.knotenArten.single()
            val erwarteteVarianten = variantenAnzahl.getValue(art)
            assertEquals(erwarteteVarianten * 3, konzept.reiter.size, konzept.name)
            assertEquals(1, konzept.reiter.count { it.rolle == KonzeptReiterRolle.Definition }, konzept.name)

            konzept.reiter.chunked(3).forEach { gruppe ->
                assertEquals(3, gruppe.size)
                assertTrue(gruppe[0].titel.endsWith("Definition"), gruppe[0].titel)
                assertTrue(gruppe[1].titel.endsWith("Leere Indexmenge"), gruppe[1].titel)
                assertTrue(gruppe[2].titel.endsWith("Endlicher Spezialfall"), gruppe[2].titel)
            }
        }
    }

    @Test
    fun `Spezialfallkarten sind selbstbezugfrei konkret und auswertbar`() {
        val auswerter = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())
        val konzepte = TestDefinitionsKarten.alle.filter { konzept ->
            konzept.knotenArten.singleOrNull() in variantenAnzahl.keys
        }

        konzepte.forEach { konzept ->
            val erklärteArt = konzept.knotenArten.single()
            konzept.reiter.drop(1).filter { reiter ->
                reiter.titel.endsWith("Leere Indexmenge") || reiter.titel.endsWith("Endlicher Spezialfall")
            }.forEach { reiter ->
                val karte = reiter.karte
                assertFalse(karte.knoten.any { it.art == erklärteArt }, reiter.titel)

                if (reiter.titel.endsWith("Leere Indexmenge")) {
                    assertTrue(karte.knoten.any { it.art == MengenraumKnotenVorlagen.LeereMenge.art }, reiter.titel)
                } else {
                    assertTrue(
                        karte.knoten.any {
                            it.art == MathematikKnotenVorlagen.EndlicheMenge.art &&
                                it.parameter["elemente"] == "1,2,3"
                        },
                        reiter.titel,
                    )
                }

                val ergebnis = auswerter.werteKonzeptKarteAus(karte)
                assertTrue(ergebnis.fehler.isEmpty(), "${reiter.titel}: ${ergebnis.fehler.joinToString()}")
                val ausgang = karte.knoten.single { it.art == "mathematik.kartenAusgang" }
                assertTrue(ergebnis.knoten[ausgang.id]?.ausgaben?.get("wert") != null, reiter.titel)
            }
        }
    }
}
