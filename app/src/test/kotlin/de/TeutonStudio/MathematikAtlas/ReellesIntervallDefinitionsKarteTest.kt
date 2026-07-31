package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReellesIntervallDefinitionsKarteTest {
    @Test
    fun `Definitionskarte verwendet die vollständige Filterstruktur ohne Selbstbezug`() {
        val karte = definitionsKarte()

        assertFalse(karte.knoten.any { it.art == MathematikKnotenVorlagen.ReellesIntervall.art })
        assertEquals(4, karte.knoten.count { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART })
        assertEquals(2, karte.knoten.count {
            it.art == MathematikKnotenVorlagen.ORDNUNGSRELATION_ART && it.parameter["relation"] == "kleiner"
        })
        assertEquals(2, karte.knoten.count {
            it.art == MathematikKnotenVorlagen.ORDNUNGSRELATION_ART && it.parameter["relation"] == "kleinerGleich"
        })
        assertEquals(2, karte.knoten.count { it.art == MathematikKnotenVorlagen.Fall.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.Konjunktion.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.TermZuMethode.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.ReelleZahlen.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.Mengenfilter.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.KartenAusgang.art })
        assertEquals(16, karte.knoten.size)
        assertEquals(20, karte.verbindungen.size)

        val eingänge = karte.knoten
            .filter { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART }
            .sortedBy { knoten -> knoten.position.y }
        assertEquals(listOf("links", "linksOffen", "rechts", "rechtsOffen"), eingänge.map { it.name })
        assertEquals("false", eingänge.single { it.name == "linksOffen" }.parameter["vorschauWert"])
        assertEquals("false", eingänge.single { it.name == "rechtsOffen" }.parameter["vorschauWert"])

        val variable = karte.knoten.single { it.art == MathematikKnotenVorlagen.Variable.art }
        assertEquals("x", variable.parameter["name"])
        assertEquals("R", variable.parameter["werteVorrat"])
        assertEquals(GraphPunkt(240.20312f, 374.42264f), variable.position)

        val konjunktion = karte.knoten.single { it.art == MathematikKnotenVorlagen.Konjunktion.art }
        val termZuMethode = karte.knoten.single { it.art == MathematikKnotenVorlagen.TermZuMethode.art }
        val reelleZahlen = karte.knoten.single { it.art == MathematikKnotenVorlagen.ReelleZahlen.art }
        val mengenfilter = karte.knoten.single { it.art == MathematikKnotenVorlagen.Mengenfilter.art }
        val ausgang = karte.knoten.single { it.art == MathematikKnotenVorlagen.KartenAusgang.art }

        assertTrue(karte.istVerbunden(konjunktion, "aussage", termZuMethode, "term"))
        assertTrue(karte.istVerbunden(termZuMethode, "methode", mengenfilter, "methode"))
        assertTrue(karte.istVerbunden(reelleZahlen, "menge", mengenfilter, "menge"))
        assertTrue(karte.istVerbunden(mengenfilter, "menge", ausgang, "wert"))
        assertEquals(
            1,
            ausgang.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang },
        )
    }

    @Test
    fun `Definitionskarte zeigt die geschlossene Relationskette und typisiert ihre Filtermethode`() {
        val karte = definitionsKarte()
        val auswertung = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())
            .werteKonzeptKarteAus(karte)

        assertTrue(auswertung.fehler.isEmpty(), auswertung.fehler.joinToString())

        val konjunktion = karte.knoten.single { it.art == MathematikKnotenVorlagen.Konjunktion.art }
        val konjunktionsWert = auswertung.knoten.getValue(konjunktion.id).ausgaben.getValue("aussage")
        assertEquals("links \\le x \\le rechts", konjunktionsWert.anzeigeLatex())

        val termZuMethode = karte.knoten.single { it.art == MathematikKnotenVorlagen.TermZuMethode.art }
        val methode = auswertung.knoten.getValue(termZuMethode.id).ausgaben.getValue("methode").objekt as Funktion
        assertEquals(listOf(Variable("x")), methode.parameter)
        assertEquals(ReelleZahlen, methode.werteVorräte.getValue("x"))
        assertEquals(
            EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false))),
            methode.einzigeZielMenge,
        )
    }

    @Test
    fun `Knoten und Anschluss IDs der Definition bleiben stabil`() {
        val erste = TestDefinitionsKarten.definitionsKarte(MathematikKnotenVorlagen.ReellesIntervall, 0)
        val zweite = TestDefinitionsKarten.definitionsKarte(MathematikKnotenVorlagen.ReellesIntervall, 0)

        assertEquals(erste.knoten.map { it.id }, zweite.knoten.map { it.id })
        assertEquals(
            erste.knoten.flatMap { it.anschlüsse }.map { it.id },
            zweite.knoten.flatMap { it.anschlüsse }.map { it.id },
        )
        assertEquals(erste.verbindungen.map { it.id }, zweite.verbindungen.map { it.id })
    }

    private fun definitionsKarte(): KartenDaten {
        val karte = TestDefinitionsKarten
            .fürKnoten(MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero))
            ?.reiter
            ?.single { it.rolle == KonzeptReiterRolle.Definition }
            ?.karte
        return assertNotNull(karte)
    }
}

private fun KartenDaten.istVerbunden(
    von: KnotenDaten,
    vonName: String,
    zu: KnotenDaten,
    zuName: String,
): Boolean {
    val vonAnschluss = von.anschlüsse.single {
        it.name == vonName && it.richtung == AnschlussRichtung.Ausgang
    }
    val zuAnschluss = zu.anschlüsse.single {
        it.name == zuName && it.richtung == AnschlussRichtung.Eingang
    }
    return verbindungen.any { verbindung ->
        verbindung.von == AnschlussVerweis(von.id, vonAnschluss.id) &&
            verbindung.zu == AnschlussVerweis(zu.id, zuAnschluss.id)
    }
}
