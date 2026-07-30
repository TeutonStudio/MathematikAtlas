package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.DEFINITIONSMENGE_DOPPELPUNKT_DARSTELLUNG
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ReellesIntervallDefinitionsKarteTest {
    @Test
    fun `Definitionskarte enthält die vollständige Fallstruktur ohne Selbstbezug`() {
        val karte = TestDefinitionsKarten
            .fürKnoten(MathematikKnotenVorlagen.ReellesIntervall.erzeuge(de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt.Zero))
            ?.reiter
            ?.single { it.rolle == KonzeptReiterRolle.Definition }
            ?.karte
        assertNotNull(karte)

        assertFalse(karte.knoten.any { it.art == MathematikKnotenVorlagen.ReellesIntervall.art })
        assertEquals(4, karte.knoten.count { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART })
        assertEquals(2, karte.knoten.count { it.art == MathematikKnotenVorlagen.Kleiner.art })
        assertEquals(2, karte.knoten.count { it.art == MathematikKnotenVorlagen.KleinerGleich.art })
        assertEquals(2, karte.knoten.count { it.art == MathematikKnotenVorlagen.Fall.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.Konjunktion.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.Lösungsmenge.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.Darstellungsoptimierung.art })
        assertEquals(1, karte.knoten.count { it.art == MathematikKnotenVorlagen.KartenAusgang.art })

        val eingänge = karte.knoten
            .filter { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART }
            .sortedBy { knoten -> knoten.position.y }
        assertEquals(listOf("links", "linksOffen", "rechts", "rechtsOffen"), eingänge.map { it.name })

        val darstellung = karte.knoten.single { it.art == MathematikKnotenVorlagen.Darstellungsoptimierung.art }
        assertEquals(DEFINITIONSMENGE_DOPPELPUNKT_DARSTELLUNG, darstellung.parameter["latex"])
        assertEquals(
            1,
            karte.knoten.single { it.art == MathematikKnotenVorlagen.KartenAusgang.art }
                .anschlüsse.count { it.richtung == AnschlussRichtung.Eingang },
        )
    }

    @Test
    fun `Definitionsausgabe verwendet Doppelpunkt und verkettete Relationen`() {
        val x = Variable("x")
        val a = Variable("a")
        val b = Variable("b")
        val menge = DefinierteMenge(
            variablen = listOf(GebundeneMengenVariable(x, ReelleZahlen)),
            bedingung = Konjunktion(
                listOf(
                    Vergleich(a, VergleichsArt.Kleiner, x),
                    Vergleich(x, VergleichsArt.KleinerGleich, b),
                ),
            ),
        )

        assertEquals(
            "\\left\\{x\\in\\mathbb{R}:a < x \\le b\\right\\}",
            BedingterWert(
                objekt = menge,
                latexDarstellung = DEFINITIONSMENGE_DOPPELPUNKT_DARSTELLUNG,
            ).anzeigeLatex(),
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
}
