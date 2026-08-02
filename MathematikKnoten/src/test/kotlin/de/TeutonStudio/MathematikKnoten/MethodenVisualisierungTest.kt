package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.VisualisierungsErgebnis
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.VisualisierungsSampler
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MethodenVisualisierungTest {
    @Test
    fun `skalare einstellige Methode wird automatisch als R2 Funktionsgraph dargestellt`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = multiplikation(x, x),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val abbild = Abbild(
            ReellesIntervall(RationaleZahl.von(-1), false, RationaleZahl.Eins, false),
            methode,
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(abbild, konfiguration(RaumDimension.R2, auflösung = 5)),
        )

        assertEquals(5, ergebnis.punkte.size)
        assertTrue(ergebnis.punkte.any { it.x == -1.0 && it.y == 1.0 })
        assertTrue(ergebnis.punkte.any { it.x == 0.0 && it.y == 0.0 })
        assertTrue(ergebnis.hinweise.any { "Funktionsgraph" in it })
    }

    @Test
    fun `skalare zweistellige Methode wird als R3 Flaeche mit zwei unabhaengigen Intervallen dargestellt`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(multiplikation(x, x), multiplikation(y, y)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val abbild = Abbild(
            KartesischesProdukt(
                listOf(
                    ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false),
                    ReellesIntervall(RationaleZahl.von(-1), false, RationaleZahl.Eins, false),
                ),
            ),
            methode,
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(abbild, konfiguration(RaumDimension.R3, auflösung = 4, budget = 16)),
        )

        assertEquals(16, ergebnis.punkte.size)
        assertTrue(ergebnis.punkte.all { it.x in 0.0..1.0 && it.y in -1.0..1.0 && it.z != null })
        assertTrue(ergebnis.punkte.any { it.x == 1.0 && it.y == -1.0 && it.z == 2.0 })
    }

    @Test
    fun `parametrisierte Kurve mit Tupelausgabe bleibt im Bildmodus darstellbar`() {
        val t = Variable("t")
        val methode = Methode(
            name = "kurve",
            parameter = listOf(t),
            vorschrift = Tupel(listOf(t, multiplikation(t, t))),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf(t.name to ReelleZahlen),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(
                Abbild(ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false), methode),
                konfiguration(RaumDimension.R2, auflösung = 3),
            ),
        )

        assertEquals(listOf(0.0, 0.5, 1.0), ergebnis.punkte.map { it.x })
        assertEquals(listOf(0.0, 0.25, 1.0), ergebnis.punkte.map { it.y })
    }

    @Test
    fun `parametrisierte Flaeche mit Vektorausgabe verwendet das Gesamtbudget`() {
        val u = Variable("u")
        val v = Variable("v")
        val methode = Methode(
            name = "flaeche",
            parameter = listOf(u, v),
            vorschrift = SpaltenVektor(listOf(u, v, addition(u, v))),
            zielMenge = Vektorraum(VektorOrientierung.Spalte, 3, ReelleZahlen),
            werteVorräte = mapOf(u.name to ReelleZahlen, v.name to ReelleZahlen),
        )
        val domäne = KartesischesProdukt(listOf(ReelleZahlen, ReelleZahlen))

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(
                Abbild(domäne, methode),
                konfiguration(RaumDimension.R3, auflösung = 6, budget = 25),
            ),
        )

        assertEquals(25, ergebnis.punkte.size)
        assertTrue(ergebnis.punkte.all { it.z == it.x + it.y })
        assertTrue(ergebnis.hinweise.any { "ℝ wird auf den sichtbaren Achsenbereich begrenzt" in it })
    }

    @Test
    fun `nicht definierte Einzelwerte werden gezaehlt statt die Darstellung abzubrechen`() {
        val x = Variable("x")
        val methode = Methode(
            name = "kehrwert",
            parameter = listOf(x),
            vorschrift = Division(RationaleZahl.Eins, x),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val domäne = EndlicheMenge(setOf(RationaleZahl.von(-1), RationaleZahl.Null, RationaleZahl.Eins))

        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(Abbild(domäne, methode), konfiguration(RaumDimension.R2, auflösung = 5)),
        )

        assertEquals(2, ergebnis.punkte.size)
        assertTrue(ergebnis.hinweise.any { "1 × Funktionswert ist nicht numerisch definiert" in it })
    }

    @Test
    fun `benannte Ausgaben werden im Koordinatenmodus den persistierten Achsen zugeordnet`() {
        val t = Variable("t")
        val methode = Methode(
            name = "koordinaten",
            parameter = listOf(t),
            vorschrift = Tupel(listOf(multiplikation(t, t), t)),
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf(t.name to ReelleZahlen),
            ausgabeNamen = listOf("u", "v"),
        )
        val config = konfiguration(
            RaumDimension.R2,
            auflösung = 3,
            modus = MethodenDarstellungsModus.Koordinatenausgabe,
        ).copy(achsen = AchsenZuordnung("v", "u", null))

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(
                Abbild(ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false), methode),
                config,
            ),
        )

        assertEquals(listOf(0.0, 0.5, 1.0), ergebnis.punkte.map { it.x })
        assertEquals(listOf(0.0, 0.25, 1.0), ergebnis.punkte.map { it.y })
        assertEquals(
            MethodenDarstellungsModus.Koordinatenausgabe,
            VisualisierungsKonfiguration.aus(config.zuEigenschaften()).methodenModus,
        )
    }

    @Test
    fun `mehr als drei Parameter liefern einen konkreten Projektionshinweis`() {
        val parameter = listOf("a", "b", "c", "d").map(::Variable)
        val methode = Methode(
            name = "vierdimensional",
            parameter = parameter,
            vorschrift = Tupel(parameter.take(3)),
            zielMenge = Tupelraum(List(3) { ReelleZahlen }),
            werteVorräte = parameter.associate { it.name to ReelleZahlen },
        )
        val domäne = KartesischesProdukt(List(4) { EndlicheMenge(setOf(RationaleZahl.Null)) })

        val ergebnis = assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
            VisualisierungsSampler.sample(Abbild(domäne, methode), konfiguration(RaumDimension.R3)),
        )

        assertTrue("Projektion" in ergebnis.grund)
    }

    @Test
    fun `endliche kartesische Domäne wird vor Materialisierung am Gesamtbudget abgewiesen`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "summe",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val faktor = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins, RationaleZahl.von(2)))

        val ergebnis = assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
            VisualisierungsSampler.sample(
                Abbild(KartesischesProdukt(listOf(faktor, faktor)), methode),
                konfiguration(RaumDimension.R3, budget = 4),
            ),
        )

        assertTrue("Gesamtbudget" in ergebnis.grund)
    }

    private fun konfiguration(
        dimension: RaumDimension,
        auflösung: Int = 5,
        budget: Int = 1_000,
        modus: MethodenDarstellungsModus = MethodenDarstellungsModus.Automatisch,
    ) = VisualisierungsKonfiguration(
        dimension = dimension,
        achsen = AchsenZuordnung("x", "y", if (dimension == RaumDimension.R3) "z" else null),
        bereiche = AchsenBereiche(
            ZahlenBereich(-1.0, 1.0),
            ZahlenBereich(-1.0, 1.0),
            ZahlenBereich(-1.0, 2.0),
        ),
        sampling = SamplingKonfiguration(
            auflösung2D = auflösung,
            auflösung3D = auflösung,
            toleranz = 1e-9,
            auflösung1D = auflösung,
            maximalesRasterBudget = budget,
        ),
        methodenModus = modus,
    )
}
