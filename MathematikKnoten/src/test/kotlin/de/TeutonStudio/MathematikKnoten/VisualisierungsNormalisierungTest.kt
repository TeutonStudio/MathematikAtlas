package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VisualisierungsNormalisierungTest {
    private fun konfiguration(
        dimension: RaumDimension = RaumDimension.R2,
        auflösung: Int = 5,
        budget: Int = 10_000,
        prädikatsFenster: Boolean = false,
    ) = VisualisierungsKonfiguration(
        dimension = dimension,
        achsen = AchsenZuordnung("x", "y", "z"),
        bereiche = AchsenBereiche(
            ZahlenBereich(-1.0, 2.0),
            ZahlenBereich(-1.0, 2.0),
            ZahlenBereich(-1.0, 2.0),
        ),
        sampling = SamplingKonfiguration(
            auflösung2D = auflösung,
            auflösung3D = auflösung,
            toleranz = 1e-8,
            maximalesRasterBudget = budget,
            fensterBegrenztePrädikatsMengen = prädikatsFenster,
        ),
    )

    @Test
    fun `leere Menge ist ein erfolgreiches mathematisch leeres Ergebnis`() {
        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(LeereMenge, konfiguration()),
        )

        assertTrue(ergebnis.punkte.isEmpty())
        assertFalse(ergebnis.istApproximation)
        assertEquals(VisualisierungsQualität.MathematischLeer, ergebnis.qualität)
    }

    @Test
    fun `Tupel Zeilenvektor und Spaltenvektor verwenden denselben Koordinatenadapter`() {
        val menge = EndlicheMenge(
            setOf(
                Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
                ZeilenVektor(listOf(RationaleZahl.von(3), RationaleZahl.von(4))),
                SpaltenVektor(listOf(RationaleZahl.von(5), RationaleZahl.von(6))),
            ),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(menge, konfiguration()),
        )

        assertEquals(setOf(1.0 to 2.0, 3.0 to 4.0, 5.0 to 6.0), ergebnis.punkte.map { it.x to it.y }.toSet())
        assertFalse(ergebnis.istApproximation)
    }

    @Test
    fun `endliche Menge schluesselt verworfene Elemente nach Ursache auf`() {
        val menge = EndlicheMenge(
            setOf(
                Tupel(listOf(RationaleZahl.Eins, RationaleZahl.Null)),
                Tupel(listOf(RationaleZahl.Eins)),
                WahrheitsKonstante(true),
            ),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(menge, konfiguration()),
        )

        assertEquals(1, ergebnis.punkte.size)
        assertTrue(ergebnis.hinweise.any { "Koordinatendimension" in it })
        assertTrue(ergebnis.hinweise.any { "weder Tupel" in it })
    }

    @Test
    fun `ueberdimensionierte Koordinate bleibt sichtbarer Projektionsbedarf`() {
        val menge = EndlicheMenge(
            setOf(Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3)))),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.ProjektionErforderlich>(
            VisualisierungsSampler.sample(menge, konfiguration()),
        )

        assertEquals(3, ergebnis.vorhandeneDimension)
        assertEquals(2, ergebnis.erwarteteDimension)
        assertTrue("Projektion" in ergebnis.grund)
    }

    @Test
    fun `symbolisch offene Koordinate bleibt bedingt darstellbar`() {
        val menge = EndlicheMenge(
            setOf(Tupel(listOf(Variable("x"), RationaleZahl.Eins))),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.BedingtDarstellbar>(
            VisualisierungsSampler.sample(menge, konfiguration()),
        )

        assertTrue("nicht entscheidbar" in ergebnis.grund)
    }

    @Test
    fun `Koordinatenbild wird nur ueber sein Koordinatensystem normalisiert`() {
        val raum = EuklidischerRaum("E", 2)
        val system = GeometrischesKoordinatensystem(raum, "K")
        val bild = KoordinatenBild(
            GeometriePunkt("A", raum, Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2)))),
            system,
        )

        val r2 = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(bild, konfiguration()),
        )
        val r1 = assertIs<VisualisierungsErgebnis.ProjektionErforderlich>(
            VisualisierungsSampler.sample(bild, konfiguration(RaumDimension.R1)),
        )

        assertEquals(listOf(VisualisierungsPunkt(1.0, 2.0)), r2.punkte)
        assertEquals(2, r1.vorhandeneDimension)
        assertTrue("K" in r1.grund)
    }

    @Test
    fun `kartesische Intervalle werden in R2 und R3 materialisiert`() {
        val r2 = KartesischesProdukt(
            listOf(
                ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false),
                ReellesIntervall(RationaleZahl.von(2), false, RationaleZahl.von(3), false),
            ),
        )
        val r3 = KartesischesProdukt(
            listOf(
                ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false),
                ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false),
                ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false),
            ),
        )

        val ergebnis2 = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(r2, konfiguration(auflösung = 5)),
        )
        val ergebnis3 = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(r3, konfiguration(RaumDimension.R3, auflösung = 4)),
        )

        assertEquals(25, ergebnis2.punkte.size)
        assertTrue(ergebnis2.punkte.all { it.x in 0.0..1.0 && it.y in 2.0..3.0 })
        assertEquals(64, ergebnis3.punkte.size)
        assertTrue(ergebnis3.punkte.all { it.z != null && it.x in 0.0..1.0 && it.y in 0.0..1.0 && it.z!! in 0.0..1.0 })
    }

    @Test
    fun `offene Grenzen und diskrete kontinuierliche Produkte bleiben semantisch korrekt`() {
        val produkt = KartesischesProdukt(
            listOf(
                EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.von(2))),
                ReellesIntervall(RationaleZahl.Null, true, RationaleZahl.Eins, true),
            ),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(produkt, konfiguration(auflösung = 5)),
        )

        assertEquals(6, ergebnis.punkte.size)
        assertEquals(setOf(0.0, 2.0), ergebnis.punkte.map { it.x }.toSet())
        assertTrue(ergebnis.punkte.all { it.y > 0.0 && it.y < 1.0 })
    }

    @Test
    fun `falsche Produktdimension wird ohne stillschweigende Projektion abgelehnt`() {
        val produkt = KartesischesProdukt(
            listOf(ReelleZahlen, ReelleZahlen, ReelleZahlen),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
            VisualisierungsSampler.sample(produkt, konfiguration()),
        )

        assertTrue("genau 2" in ergebnis.grund)
        assertTrue("Projektion" in ergebnis.grund)
    }

    @Test
    fun `definierte Menge respektiert jede Grundmenge`() {
        val x = Variable("x")
        val y = Variable("y")
        val menge = DefinierteMenge(
            listOf(
                GebundeneMengenVariable(x, ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false)),
                GebundeneMengenVariable(y, EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))),
            ),
            WahrheitsKonstante(true),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(menge, konfiguration(auflösung = 7)),
        )

        assertTrue(ergebnis.punkte.isNotEmpty())
        assertTrue(ergebnis.punkte.all { it.x in 0.0..1.0 && it.y in setOf(0.0, 1.0) })
    }

    @Test
    fun `Dimensionsvertrag meldet zusätzliche freie und fehlende Raumvariablen getrennt`() {
        val x = Variable("x")
        val y = Variable("y")
        val z = Variable("z")
        val mitZusatz = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen)),
            Vergleich(z, VergleichsArt.Größer, RationaleZahl.Null),
        )
        val zuWenig = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen)),
            WahrheitsKonstante(true),
        )

        assertTrue(
            "zusätzliche freie Variablen" in assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
                VisualisierungsSampler.sample(mitZusatz, konfiguration()),
            ).grund,
        )
        assertTrue(
            "bindet 1 Variablen" in assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
                VisualisierungsSampler.sample(zuWenig, konfiguration()),
            ).grund,
        )
    }

    @Test
    fun `Mengenoperationen verwenden eine gemeinsame Mitgliedschaft statt Punktlistenrechnung`() {
        val links = KartesischesProdukt(
            listOf(
                ReellesIntervall(RationaleZahl.von(-1), false, RationaleZahl.Null, false),
                ReellesIntervall(RationaleZahl.von(-1), false, RationaleZahl.Eins, false),
            ),
        )
        val rechts = KartesischesProdukt(
            listOf(
                ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false),
                ReellesIntervall(RationaleZahl.von(-1), false, RationaleZahl.Eins, false),
            ),
        )

        val vereinigung = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(Vereinigung(listOf(links, rechts)), konfiguration(auflösung = 7)),
        )
        val schnitt = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(Schnitt(listOf(links, rechts)), konfiguration(auflösung = 7)),
        )
        val differenz = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(MengenDifferenz(links, rechts), konfiguration(auflösung = 7)),
        )
        val symmetrisch = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(SymmetrischeDifferenz(links, rechts), konfiguration(auflösung = 7)),
        )

        assertTrue(vereinigung.punkte.size > schnitt.punkte.size)
        assertTrue(differenz.punkte.all { it.x < 0.0 })
        assertTrue(symmetrisch.punkte.none { it.x == 0.0 })
    }

    @Test
    fun `Filter Grundmenge und Mengenfall werden gemeinsam ausgewertet`() {
        val element = TypisiertesElement("p", "tupel")
        val filter = Methode("immer", listOf(element), mapOf("aussage" to WahrheitsKonstante(true)))
        val basis = KartesischesProdukt(
            listOf(
                EndlicheMenge(setOf(RationaleZahl.von(-1), RationaleZahl.Eins)),
                EndlicheMenge(setOf(RationaleZahl.Null)),
            ),
        )
        val gefiltert = GefilterteMenge(basis, filter)
        val fall = FallAusdruck(
            gefiltert,
            WahrheitsKonstante(true),
            LeereMenge,
        ) as MengenAusdruck

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(fall, konfiguration()),
        )

        assertEquals(setOf(-1.0, 1.0), ergebnis.punkte.map { it.x }.toSet())
        assertTrue(ergebnis.punkte.all { it.y == 0.0 })
    }

    @Test
    fun `grundmengenfreie Prädikatsmenge verlangt ausdrückliche Fensterapproximation`() {
        val element = TypisiertesElement("p", "tupel")
        val menge = PrädikatsMenge(element, WahrheitsKonstante(true))

        assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
            VisualisierungsSampler.sample(menge, konfiguration(prädikatsFenster = false)),
        )
        val angenähert = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(menge, konfiguration(auflösung = 4, prädikatsFenster = true)),
        )
        assertEquals(16, angenähert.punkte.size)
        assertTrue(angenähert.hinweise.any { "Fensterbegrenzte Approximation" in it })
        assertTrue(angenähert.hinweise.any { "Ergebnisqualität" in it && "Sichtfenster" in it })
    }

    @Test
    fun `gemeinsamer Aussageauswerter trägt Implikation Äquivalenz Adjunktion und Elementbeziehung`() {
        val x = Variable("x")
        val y = Variable("y")
        val bedingung = Konjunktion(
            listOf(
                Implikation(WahrheitsKonstante(true), Vergleich(x, VergleichsArt.GrößerGleich, RationaleZahl.Null)),
                Äquivalenz(WahrheitsKonstante(true), ElementBeziehung(y, GanzeZahlen)),
                Adjunktion(WahrheitsKonstante(true), WahrheitsKonstante(false)),
            ),
        )
        val menge = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen)),
            bedingung,
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(menge, konfiguration(auflösung = 7)),
        )

        assertTrue(ergebnis.punkte.isNotEmpty())
        assertTrue(ergebnis.punkte.all { it.x >= 0.0 && it.y % 1.0 == 0.0 })
    }

    @Test
    fun `Rasterbudget wird vor der Materialisierung geprüft`() {
        val produkt = KartesischesProdukt(listOf(ReelleZahlen, ReelleZahlen))
        val ergebnis = assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
            VisualisierungsSampler.sample(produkt, konfiguration(auflösung = 40, budget = 1_000)),
        )

        assertTrue("Rasterbudget" in ergebnis.grund)
    }
}
