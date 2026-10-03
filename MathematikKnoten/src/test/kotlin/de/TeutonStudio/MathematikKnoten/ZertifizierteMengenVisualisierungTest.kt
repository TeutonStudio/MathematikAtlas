package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ZertifizierteMengenVisualisierungTest {
    private val x = Variable("x")
    private val y = Variable("y")
    private val z = Variable("z")
    private fun q(wert: Long) = RationaleZahl.von(wert)
    private fun mandelbrotMenge(): OrbitBeschraenktheitsMenge {
        val zustand = Variable("zustand")
        val parameter = Variable("parameter")
        val schritt = Methode(
            name = "quadratisch",
            parameter = listOf(zustand, parameter),
            vorschrift = addition(Potenz(zustand, q(2)), parameter),
            zielMenge = KomplexeZahlen,
            werteVorräte = mapOf("zustand" to KomplexeZahlen, "parameter" to KomplexeZahlen),
        )
        return OrbitBeschraenktheitsMenge(OrbitFamilie(schritt, q(0)), KomplexeZahlen)
    }
    private fun config(
        dimension: RaumDimension = RaumDimension.R2,
        achsen: AchsenZuordnung = AchsenZuordnung("x", "y", null),
        farbe: FarbZuordnung = FarbZuordnung(FarbModus.FesteFarbe, null, 0xFF2563EB, "Ozean", null),
        schnitte: Map<String, Double> = emptyMap(),
    ) = VisualisierungsKonfiguration(
        dimension = dimension,
        achsen = achsen,
        bereiche = AchsenBereiche(ZahlenBereich(-2.0, 2.0), ZahlenBereich(-2.0, 2.0), ZahlenBereich(-2.0, 2.0)),
        farbe = farbe,
        sampling = SamplingKonfiguration(17, 8, 0.02, maximalesRasterBudget = 8_000),
        festeSchnitte = schnitte,
    )

    @Test fun `bewiesene und unbestimmte Zellen bleiben gleichzeitig sichtbar`() {
        val kreis = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen)),
            Vergleich(addition(Potenz(x, q(2)), Potenz(y, q(2))), VergleichsArt.KleinerGleich, q(1)),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(VisualisierungsSampler.sample(kreis, config()))
        assertTrue(ergebnis.zellen.any { it.status == ZellenStatus.Enthalten })
        assertTrue(ergebnis.zellen.any { it.status == ZellenStatus.Unbekannt })
        assertTrue(ergebnis.punkte.isNotEmpty())
    }

    @Test fun `Unstetigkeit erzeugt aus Vorzeichenwechsel keine Nullstelle`() {
        val menge = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen)),
            Gleichheit(Division(q(1), x), q(0)),
        )
        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(VisualisierungsSampler.sample(menge, config()))
        assertTrue(ergebnis.punkte.isEmpty())
        assertTrue(ergebnis.zellen.any { it.status == ZellenStatus.Unbekannt })
    }

    @Test fun `komplexe Ebene behält auch reelle Werte zweidimensional`() {
        val ganz = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(KomplexeZahlen, config(RaumDimension.C, AchsenZuordnung("re", "im", null))))
        assertTrue(ganz.zellen.any { it.status == ZellenStatus.Enthalten })
        val punkt = EndlicheMenge(setOf(KomplexeZahl(q(1), q(0))))
        val dargestellt = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(punkt, config(RaumDimension.C, AchsenZuordnung("re", "im", null))))
        assertEquals(listOf(1.0 to 0.0), dargestellt.punkte.map { it.x to it.y })
    }

    @Test fun `nicht dargestellte Variable ist ausschließlich ein fester Schnitt`() {
        val kugel = DefinierteMenge(
            listOf(
                GebundeneMengenVariable(x, ReelleZahlen),
                GebundeneMengenVariable(y, ReelleZahlen),
                GebundeneMengenVariable(z, ReelleZahlen),
            ),
            Vergleich(addition(Potenz(x, q(2)), Potenz(y, q(2)), Potenz(z, q(2))), VergleichsArt.KleinerGleich, q(1)),
        )
        val ohne = assertIs<VisualisierungsErgebnis.NichtDarstellbar>(VisualisierungsSampler.sample(kugel, config()))
        assertTrue("festen Schnitt" in ohne.grund)
        val mit = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(kugel, config(schnitte = mapOf("z" to 2.0))),
        )
        assertTrue(mit.punkte.isEmpty())
        assertTrue(mit.zellen.all { it.status == ZellenStatus.Ausgeschlossen })
    }

    @Test fun `mehrere Werte der Farbdimension werden am Bildpunkt markiert`() {
        val würfel = DefinierteMenge(
            listOf(
                GebundeneMengenVariable(x, ReellesIntervall(q(0), false, q(0), false)),
                GebundeneMengenVariable(y, ReellesIntervall(q(0), false, q(0), false)),
                GebundeneMengenVariable(z, ReellesIntervall(q(-1), false, q(1), false)),
            ),
            WahrheitsKonstante(true),
        )
        val farbe = FarbZuordnung(FarbModus.Spektrum, "z", null, "Ozean", ZahlenBereich(-1.0, 1.0))
        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(VisualisierungsSampler.sample(würfel, config(farbe = farbe)))
        val ursprung = ergebnis.punkte.single { it.x == 0.0 && it.y == 0.0 }
        assertNotNull(ursprung.farbwert)
        assertTrue(ursprung.weitereFarbwerte.isNotEmpty())
        assertTrue(ergebnis.hinweise.any { "17 einzelne Rasterwerte" in it })
        assertTrue(ergebnis.hinweise.any { "Farbbereich [-1, 1]" in it && "17 Rasterwerten" in it && "unvollständig" in it })
        assertTrue(ergebnis.hinweise.any { "Mehrdeutige Farbe bei (0, 0)" in it && "unvollständig" in it })
    }

    @Test fun `mehrdimensionale endliche Differenz entscheidet vor der Double Projektion`() {
        val nullpunkt = Tupel(listOf(q(0), q(0)))
        val naherPunkt = Tupel(listOf(RationaleZahl.von(1, 10_000_000_000L), q(0)))
        val menge = MengenDifferenz(EndlicheMenge(setOf(nullpunkt)), EndlicheMenge(setOf(naherPunkt)))

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(
            VisualisierungsSampler.sample(menge, config()),
        )
        assertEquals(listOf(0.0 to 0.0), ergebnis.punkte.map { it.x to it.y })
        assertEquals(VisualisierungsQualität.Exakt, ergebnis.qualität)
    }

    @Test fun `endlicher Filter verwirft weder exakte Wahrheit noch offene Kandidaten`() {
        val p = TypisiertesElement("p", "tupel")
        val basis = EndlicheMenge(setOf(Tupel(listOf(q(0), q(0)))))
        val wahr = GefilterteMenge(basis, Methode("wahr", listOf(p), mapOf("aussage" to Gleichheit(p, p))))
        val offen = GefilterteMenge(
            basis,
            Methode("offen", listOf(p), mapOf("aussage" to Gleichheit(p, TypisiertesElement("u", "tupel")))),
        )

        val exakt = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(wahr, config()))
        assertEquals(1, exakt.punkte.size)
        val unbestimmt = assertIs<VisualisierungsErgebnis.Teilweise>(VisualisierungsSampler.sample(offen, config()))
        assertTrue(unbestimmt.zellen.any { it.status == ZellenStatus.Unbekannt })
    }

    @Test fun `leerer Schnitt verwendet seine Grundmenge und fehlt ohne sie nicht als leere Menge aus`() {
        val mitGrund = Schnitt(emptyList(), EndlicheMenge(setOf(Tupel(listOf(q(0), q(0))))))
        val ohneGrund = Schnitt(emptyList(), null)

        val exakt = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(mitGrund, config()))
        assertEquals(1, exakt.punkte.size)
        val unbestimmt = assertIs<VisualisierungsErgebnis.Teilweise>(VisualisierungsSampler.sample(ohneGrund, config()))
        assertTrue(unbestimmt.zellen.any { it.status == ZellenStatus.Unbekannt })
    }

    @Test fun `eindimensionaler Filter erhält einen Skalar und erreicht den numerischen Regionspfad`() {
        val positiv = GefilterteMenge(
            ReelleZahlen,
            Methode("positiv", listOf(x), mapOf("aussage" to Vergleich(x, VergleichsArt.Größer, q(0)))),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(positiv, config(RaumDimension.R1, AchsenZuordnung("x", "", null))),
        )

        assertTrue(ergebnis.punkte.isNotEmpty())
        assertTrue(ergebnis.punkte.all { it.x > 0.0 })
        assertTrue(ergebnis.zellen.any { it.status == ZellenStatus.Unbekannt })
    }

    @Test fun `fensterbegrenzte eindimensionale Prädikatsmenge markiert unbewiesene Zellen`() {
        val positiv = PrädikatsMenge(x, Vergleich(x, VergleichsArt.Größer, q(0)))
        val fenster = config(RaumDimension.R1, AchsenZuordnung("x", "", null)).copy(
            sampling = config().sampling.copy(fensterBegrenztePrädikatsMengen = true),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(VisualisierungsSampler.sample(positiv, fenster))

        assertTrue(ergebnis.punkte.isNotEmpty())
        assertTrue(ergebnis.punkte.all { it.x > 0.0 })
        assertTrue(ergebnis.zellen.any { it.status == ZellenStatus.Unbekannt })
        assertTrue(ergebnis.hinweise.any { "Fensterbegrenzte" in it })
    }

    @Test fun `rationale und irrationale Anteile werden nicht als volle Intervalle behauptet`() {
        val r1 = config(RaumDimension.R1, AchsenZuordnung("x", "", null))
        val rationale = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(RationaleZahlen, r1),
        )
        assertTrue(rationale.punkte.isNotEmpty())
        assertTrue(rationale.intervalle.isEmpty())
        assertTrue(rationale.zellen.any { it.status == ZellenStatus.Gemischt })

        val irrationale = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(MengenDifferenz(ReelleZahlen, RationaleZahlen), r1),
        )
        assertTrue(irrationale.intervalle.isEmpty())
        assertTrue(irrationale.zellen.any { it.status == ZellenStatus.Gemischt })
    }

    @Test fun `korrelierte gemischte Operanden erzeugen keine falschen Zellbehauptungen`() {
        val r1 = config(RaumDimension.R1, AchsenZuordnung("x", "", null))
        val zelle = listOf(RationalesIntervall(q(0), q(1)))
        val gleichesXor = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                SymmetrischeDifferenz(RationaleZahlen, RationaleZahlen),
                r1,
            ),
        )
        assertIs<ZellNachweis.Ausgeschlossen>(gleichesXor.zellNachweis!!.invoke(zelle))

        val irrationale = MengenDifferenz(ReelleZahlen, RationaleZahlen)
        val komplementSchnitt = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(Schnitt(listOf(RationaleZahlen, irrationale)), r1),
        )
        val komplementVereinigung = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(Vereinigung(listOf(RationaleZahlen, irrationale)), r1),
        )
        assertIs<ZellNachweis.Unbekannt>(komplementSchnitt.zellNachweis!!.invoke(zelle))
        assertIs<ZellNachweis.Unbekannt>(komplementVereinigung.zellNachweis!!.invoke(zelle))
    }

    @Test fun `Achsenbereiche lehnen unendliche Grenzen ab und alte Daten fallen auf Standard zurück`() {
        assertFailsWith<IllegalArgumentException> {
            ZahlenBereich(0.0, Double.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            ZahlenBereich(-Double.MAX_VALUE, Double.MAX_VALUE)
        }
        val geladen = VisualisierungsKonfiguration.aus(
            mapOf(
                "bereiche" to KnotenEigenschaft.Objekt(
                    mapOf(
                        "x" to KnotenEigenschaft.Objekt(
                            mapOf(
                                "minimum" to KnotenEigenschaft.Dezimalzahl(0.0),
                                "maximum" to KnotenEigenschaft.Dezimalzahl(Double.POSITIVE_INFINITY),
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(ZahlenBereich(0.0, 10.0), geladen.bereiche.x)

        val überlaufendGeladen = VisualisierungsKonfiguration.aus(
            mapOf(
                "bereiche" to KnotenEigenschaft.Objekt(
                    mapOf(
                        "x" to KnotenEigenschaft.Objekt(
                            mapOf(
                                "minimum" to KnotenEigenschaft.Dezimalzahl(-Double.MAX_VALUE),
                                "maximum" to KnotenEigenschaft.Dezimalzahl(Double.MAX_VALUE),
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(ZahlenBereich(-10.0, 10.0), überlaufendGeladen.bereiche.x)
    }

    @Test fun `verschachteltes endliches Produkt wird vor Materialisierung budgetbegrenzt`() {
        val faktor = EndlicheMenge((0L until 100L).map(::q).toSet())
        val verschachtelt = Vereinigung(listOf(KartesischesProdukt(listOf(faktor, faktor)), LeereMenge))

        val definition = assertIs<VisualisierungsDefinition.NichtRäumlich>(
            VisualisierungsSampler.normalisiere(verschachtelt, config()),
        )
        assertTrue("Rasterbudget" in definition.grund)
    }

    @Test fun `direkte endliche Menge wird vor Projektion am Gesamtbudget begrenzt`() {
        val zuGroß = EndlicheMenge((0L..8_000L).map(::q).toSet())

        val definition = assertIs<VisualisierungsDefinition.NichtRäumlich>(
            VisualisierungsSampler.normalisiere(
                zuGroß,
                config(RaumDimension.R1, AchsenZuordnung("x", "", null)),
            ),
        )
        assertTrue("Rasterbudget" in definition.grund)
    }

    @Test fun `endliche Algebra wird vor quadratischen Mitgliedschaftspruefungen begrenzt`() {
        val links = EndlicheMenge((0L until 8_000L).map(::q).toSet())
        val rechts = EndlicheMenge((8_000L until 16_000L).map(::q).toSet())
        val vereinigung = Vereinigung(listOf(links, rechts))
        val knapp = config(RaumDimension.R1, AchsenZuordnung("x", "", null)).copy(
            sampling = config().sampling.copy(maximalesRasterBudget = 8_000),
        )

        val definition = assertIs<VisualisierungsDefinition.NichtRäumlich>(
            VisualisierungsSampler.normalisiere(vereinigung, knapp),
        )

        assertTrue("Auswertungsschritte" in definition.grund)
        assertTrue("Rasterbudget" in definition.grund)
    }

    @Test fun `definierte Zellen teilen ein gemeinsames Intervallbudget`() {
        val knapp = config(RaumDimension.R1, AchsenZuordnung("x", "", null)).copy(
            sampling = config().sampling.copy(maximalesRasterBudget = 6),
        )
        val definition = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen)), WahrheitsKonstante(true)),
                knapp,
            ),
        )
        val zelle = listOf(RationalesIntervall(q(0), q(1)))

        repeat(3) { assertIs<ZellNachweis.Enthalten>(definition.zellNachweis!!.invoke(zelle)) }
        val erschöpft = assertIs<ZellNachweis.Unbekannt>(definition.zellNachweis!!.invoke(zelle))
        assertTrue("budget" in erschöpft.grund.lowercase())
    }

    @Test fun `definierte natürliche und ganze Mengen verwenden keine Zeichentoleranz`() {
        val r1 = config(RaumDimension.R1, AchsenZuordnung("x", "", null))
        val natürliche = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                DefinierteMenge(listOf(GebundeneMengenVariable(x, NatürlicheZahlen)), WahrheitsKonstante(true)),
                r1,
            ),
        )
        val ganze = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                DefinierteMenge(listOf(GebundeneMengenVariable(x, GanzeZahlen)), WahrheitsKonstante(true)),
                r1,
            ),
        )

        assertEquals(NumerischeMitgliedschaft.NichtEnthalten, natürliche.mitgliedschaft(listOf(0.0)))
        assertEquals(NumerischeMitgliedschaft.Enthalten, natürliche.mitgliedschaft(listOf(1.0)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten, ganze.mitgliedschaft(listOf(1.01)))
    }

    @Test fun `definierte Intervalle und endliche Grundmengen verwenden exakte Grenzen`() {
        val r1 = config(RaumDimension.R1, AchsenZuordnung("x", "", null))
        val intervall = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                DefinierteMenge(
                    listOf(GebundeneMengenVariable(x, ReellesIntervall(q(0), false, q(1), false))),
                    WahrheitsKonstante(true),
                ),
                r1,
            ),
        )
        val einzelpunkt = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                DefinierteMenge(
                    listOf(GebundeneMengenVariable(x, EndlicheMenge(setOf(q(0))))),
                    WahrheitsKonstante(true),
                ),
                r1,
            ),
        )

        assertEquals(NumerischeMitgliedschaft.NichtEnthalten, intervall.mitgliedschaft(listOf(-0.01)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten, einzelpunkt.mitgliedschaft(listOf(0.01)))
    }

    @Test fun `R1 Gleichheit verwendet die absolute Konfigurationstoleranz`() {
        val nullstelle = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen)),
            Gleichheit(x, q(0)),
        )
        val r1 = config(RaumDimension.R1, AchsenZuordnung("x", "", null)).copy(
            bereiche = AchsenBereiche(
                ZahlenBereich(-100.0, 100.0),
                ZahlenBereich(-2.0, 2.0),
                ZahlenBereich(-2.0, 2.0),
            ),
            sampling = config().sampling.copy(auflösung1D = 101, toleranz = 0.02),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(VisualisierungsSampler.sample(nullstelle, r1))

        assertTrue(ergebnis.punkte.isNotEmpty())
        assertTrue(ergebnis.punkte.all { kotlin.math.abs(it.x) <= r1.sampling.toleranz })
    }

    @Test fun `Produktfaktoren unterscheiden Leere Sichtfenster von leeren Mengen`() {
        val negativ = config().copy(
            bereiche = AchsenBereiche(
                ZahlenBereich(-10.0, -1.0),
                ZahlenBereich(-2.0, 2.0),
                ZahlenBereich(-2.0, 2.0),
            ),
        )
        val produkt = KartesischesProdukt(listOf(NatürlicheZahlen, ReelleZahlen))

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(produkt, negativ))

        assertTrue(ergebnis.punkte.isEmpty())
        assertEquals(VisualisierungsQualität.KeineTrefferImFenster, ergebnis.qualität)
    }

    @Test fun `offenes schmales Intervall bleibt im Produkt sichtbar`() {
        val produkt = KartesischesProdukt(
            listOf(
                ReellesIntervall(q(0), true, RationaleZahl.von(1, 100), true),
                EndlicheMenge(setOf(q(0))),
            ),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(produkt, config()))

        assertTrue(ergebnis.punkte.isNotEmpty())
        assertTrue(ergebnis.punkte.all { it.x > 0.0 && it.x < 0.01 })
    }

    @Test fun `breites Intervall wird vor dem Raster auf das Sichtfenster gekappt`() {
        val grenze = RationaleZahl.von(java.math.BigInteger.TEN.pow(308))
        val produkt = KartesischesProdukt(
            listOf(
                ReellesIntervall(-grenze, false, grenze, false),
                EndlicheMenge(setOf(q(0))),
            ),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(produkt, config()))

        assertTrue(ergebnis.punkte.isNotEmpty())
        assertTrue(ergebnis.punkte.all { it.x.isFinite() && it.x in -2.0..2.0 })
    }

    @Test fun `ganzzahliger Bereich oberhalb Long bleibt budgetbegrenzt`() {
        val groß = config().copy(
            bereiche = AchsenBereiche(
                ZahlenBereich(1e19, Math.nextUp(1e19)),
                ZahlenBereich(-2.0, 2.0),
                ZahlenBereich(-2.0, 2.0),
            ),
            sampling = config().sampling.copy(maximalesRasterBudget = 1_000),
        )
        val produkt = KartesischesProdukt(listOf(GanzeZahlen, EndlicheMenge(setOf(q(0)))))

        val ergebnis = assertIs<VisualisierungsErgebnis.NichtDarstellbar>(VisualisierungsSampler.sample(produkt, groß))

        assertTrue("Rasterbudget" in ergebnis.grund)
    }

    @Test fun `dichte Produktmengen behalten offene Zellzustaende`() {
        val irrationaleEbene = KartesischesProdukt(
            listOf(MengenDifferenz(ReelleZahlen, RationaleZahlen), ReelleZahlen),
        )
        val rationaleEbene = KartesischesProdukt(listOf(RationaleZahlen, RationaleZahlen))

        val irrational = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(irrationaleEbene, config()),
        )
        val rational = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(rationaleEbene, config()),
        )

        assertTrue(irrational.zellen.any { it.status == ZellenStatus.Gemischt || it.status == ZellenStatus.Unbekannt })
        assertTrue(rational.zellen.any { it.status == ZellenStatus.Unbekannt })
        assertTrue(irrational.qualität != VisualisierungsQualität.MathematischLeer)
        assertTrue(rational.qualität != VisualisierungsQualität.MathematischLeer)
    }

    @Test fun `komplexe Produktfaktoren behalten Real Imaginaer und Faktor Reihenfolge`() {
        val ausgeschlossen = KomplexeZahl(q(1), q(2))
        val komplexOhnePunkt = MengenDifferenz(
            KomplexeZahlen,
            EndlicheMenge(setOf(ausgeschlossen)),
        )
        val drei = EndlicheMenge(setOf(q(3)))
        val r3 = config(RaumDimension.R3, AchsenZuordnung("a", "b", "c"))

        val komplexDannReell = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                KartesischesProdukt(listOf(komplexOhnePunkt, drei)),
                r3,
            ),
        )
        val reellDannKomplex = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                KartesischesProdukt(listOf(drei, komplexOhnePunkt)),
                r3,
            ),
        )

        assertEquals(
            NumerischeMitgliedschaft.NichtEnthalten,
            komplexDannReell.mitgliedschaft(listOf(1.0, 2.0, 3.0)),
        )
        assertEquals(
            NumerischeMitgliedschaft.Enthalten,
            komplexDannReell.mitgliedschaft(listOf(2.0, 1.0, 3.0)),
        )
        assertEquals(
            NumerischeMitgliedschaft.NichtEnthalten,
            reellDannKomplex.mitgliedschaft(listOf(3.0, 1.0, 2.0)),
        )
        assertEquals(
            NumerischeMitgliedschaft.Enthalten,
            reellDannKomplex.mitgliedschaft(listOf(3.0, 2.0, 1.0)),
        )
        assertTrue("Re(Faktor 1), Im(Faktor 1), Faktor 2" in komplexDannReell.hinweise.single())
        assertTrue("Faktor 1, Re(Faktor 2), Im(Faktor 2)" in reellDannKomplex.hinweise.single())
        val komplexDannReellZelle = listOf(
            RationalesIntervall(q(1)),
            RationalesIntervall(q(2)),
            RationalesIntervall(q(3)),
        )
        val reellDannKomplexZelle = listOf(
            RationalesIntervall(q(3)),
            RationalesIntervall(q(1)),
            RationalesIntervall(q(2)),
        )
        assertIs<ZellNachweis.Unbekannt>(komplexDannReell.zellNachweis!!.invoke(komplexDannReellZelle))
        assertIs<ZellNachweis.Unbekannt>(reellDannKomplex.zellNachweis!!.invoke(reellDannKomplexZelle))
    }

    @Test fun `riesige ganzzahlige Produktdomäne überschreitet sicher das Budget`() {
        val produkt = KartesischesProdukt(listOf(GanzeZahlen, EndlicheMenge(setOf(q(0)))))
        val riesig = config().copy(
            bereiche = AchsenBereiche(
                ZahlenBereich(-1e300, 1e300),
                ZahlenBereich(-2.0, 2.0),
                ZahlenBereich(-2.0, 2.0),
            ),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.NichtDarstellbar>(
            VisualisierungsSampler.sample(produkt, riesig),
        )
        assertTrue("Rasterbudget" in ergebnis.grund)
    }

    @Test fun `neue Konfiguration bleibt abwärtskompatibel und rundtripfähig`() {
        val alt = VisualisierungsKonfiguration.aus(emptyMap())
        assertEquals(256, alt.sampling.maximaleOrbitSchritte)
        assertTrue(alt.festeSchnitte.isEmpty())
        val neu = config(RaumDimension.C, AchsenZuordnung("re", "im", null), schnitte = mapOf("t" to 1.25))
            .copy(sampling = SamplingKonfiguration(22, 9, 0.1, maximaleOrbitSchritte = 777))
        assertEquals(neu, VisualisierungsKonfiguration.aus(neu.zuEigenschaften()))
    }

    @Test fun `Orbitmenge liefert in C getrennte Beweis und Unbestimmtheitszellen`() {
        val definition = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                mandelbrotMenge(),
                config(RaumDimension.C, AchsenZuordnung("re", "im", null)),
            ),
        )

        assertEquals(NumerischeMitgliedschaft.Enthalten, definition.mitgliedschaft(listOf(0.0, 0.0)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten, definition.mitgliedschaft(listOf(1.0, 0.0)))
        assertIs<ZellNachweis.Enthalten>(
            definition.zellNachweis!!.invoke(
                listOf(
                    RationalesIntervall(RationaleZahl.von(-1, 10), RationaleZahl.von(1, 10)),
                    RationalesIntervall(RationaleZahl.von(-1, 10), RationaleZahl.von(1, 10)),
                ),
            ),
        )
        assertIs<ZellNachweis.Unbekannt>(
            definition.zellNachweis.invoke(
                listOf(
                    RationalesIntervall(RationaleZahl.von(-2), RationaleZahl.von(1)),
                    RationalesIntervall(RationaleZahl.von(-1), RationaleZahl.von(1)),
                ),
            ),
        )
    }

    @Test fun `komplexe Teilmenge bleibt ein komplexer Orbitparameterraum`() {
        val nullpunkt = KomplexeZahl(q(0), q(0))
        val menge = mandelbrotMenge().copy(
            parameterRaum = MengenDifferenz(KomplexeZahlen, EndlicheMenge(setOf(nullpunkt))),
        )
        val definition = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                menge,
                config(RaumDimension.C, AchsenZuordnung("re", "im", null)),
            ),
        )

        assertEquals(2, definition.dimension)
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten, definition.mitgliedschaft(listOf(0.0, 0.0)))
        assertEquals(NumerischeMitgliedschaft.Enthalten, definition.mitgliedschaft(listOf(0.01, 0.0)))
        assertEquals(NumerischeMitgliedschaft.Enthalten, definition.mitgliedschaft(listOf(-1.0, 0.0)))
        assertIs<ZellNachweis.Unbekannt>(
            definition.zellNachweis!!.invoke(
                listOf(RationalesIntervall(q(-1)), RationalesIntervall(q(0))),
            ),
        )
    }

    @Test fun `symbolisches Element einer endlichen Menge bleibt bei Punktprüfung unbestimmt`() {
        val menge = MengenDifferenz(ReelleZahlen, EndlicheMenge(setOf(x)))
        val definition = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(
                menge,
                config(RaumDimension.R1, AchsenZuordnung("x", "", null)),
            ),
        )

        assertIs<NumerischeMitgliedschaft.Unbekannt>(definition.mitgliedschaft(listOf(0.0)))
    }

    @Test fun `zusammengesetzter Parameterraum bindet ein Tupel in R2`() {
        val zustand = Variable("zustand")
        val parameter = AllgemeinerParameter("parameter")
        val parameterRaum = KartesischesProdukt(listOf(ReelleZahlen, ReelleZahlen))
        val schritt = Methode(
            name = "tupelParameter",
            parameter = listOf(zustand, parameter),
            vorschrift = zustand,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("zustand" to ReelleZahlen, "parameter" to parameterRaum),
        )
        val menge = OrbitBeschraenktheitsMenge(OrbitFamilie(schritt, q(0)), parameterRaum)
        val definition = assertIs<VisualisierungsDefinition.Region>(
            VisualisierungsSampler.normalisiere(menge, config()),
        )

        assertEquals(2, definition.dimension)
        assertEquals(NumerischeMitgliedschaft.Enthalten, definition.mitgliedschaft(listOf(0.25, -0.5)))
        assertIs<ZellNachweis.Unbekannt>(
            definition.zellNachweis!!.invoke(
                listOf(RationalesIntervall(q(0)), RationalesIntervall(q(0))),
            ),
        )
    }

    @Test fun `reelle Orbitmenge erreicht in R1 den sichtbaren Unbestimmtheitspfad`() {
        val zustand = Variable("zustand")
        val parameter = Variable("parameter")
        val identität = Methode(
            name = "identität",
            parameter = listOf(zustand, parameter),
            vorschrift = zustand,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("zustand" to ReelleZahlen, "parameter" to ReelleZahlen),
        )
        val menge = OrbitBeschraenktheitsMenge(OrbitFamilie(identität, q(0)), ReelleZahlen)

        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(
                menge,
                config(RaumDimension.R1, AchsenZuordnung("p", "", null)),
            ),
        )
        assertTrue(ergebnis.punkte.isNotEmpty())
        assertTrue(ergebnis.zellen.any { it.status == ZellenStatus.Unbekannt })
        assertTrue(ergebnis.hinweise.any { "Punktentscheidung" in it })
    }

    @Test fun `Orbitläufe teilen ein gemeinsames Auswertungsbudget`() {
        val zustand = Variable("zustand")
        val parameter = Variable("parameter")
        val verschiebung = Methode(
            name = "verschiebung",
            parameter = listOf(zustand, parameter),
            vorschrift = addition(zustand, q(1)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("zustand" to ReelleZahlen, "parameter" to ReelleZahlen),
        )
        val menge = OrbitBeschraenktheitsMenge(OrbitFamilie(verschiebung, q(0)), ReelleZahlen)
        val knapp = config(RaumDimension.R1, AchsenZuordnung("p", "", null)).copy(
            sampling = SamplingKonfiguration(
                auflösung2D = 17,
                auflösung3D = 8,
                toleranz = 0.02,
                auflösung1D = 80,
                maximalesRasterBudget = 1_000,
                maximaleOrbitSchritte = 64,
            ),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Teilweise>(
            VisualisierungsSampler.sample(menge, knapp),
        )
        assertTrue(ergebnis.hinweise.any { "gemeinsame Orbit-Auswertungsbudget" in it })
    }

    @Test fun `Sampling prüft einen angeforderten Abbruch`() {
        assertFailsWith<java.util.concurrent.CancellationException> {
            VisualisierungsSampler.sample(
                mandelbrotMenge(),
                config(RaumDimension.C, AchsenZuordnung("re", "im", null)),
            ) { throw java.util.concurrent.CancellationException("Testabbruch") }
        }
    }
}
