package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.util.concurrent.CancellationException
import kotlin.math.abs
import kotlin.test.*

class WinkelraumSamplingTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun z(x: Long) = RationaleZahl.von(x)
    private fun eval(k: KnotenDaten, werte: Map<String, BedingterWert>): BedingterWert {
        val e = register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k, werte, RechenKontext()))
        assertNull(e.fehler, e.fehler)
        return e.ausgaben.values.single()
    }
    private fun bild(n: Int, diagonal: List<Long>? = null): Abbild {
        val variable = konfiguriereVariablenStruktur(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), VARIABLE_STRUKTUR_WINKEL)
            .let { it.copy(parameter = it.parameter + mapOf(VARIABLE_DIMENSION to n.toString(), "name" to "w")) }
        val w = eval(variable, emptyMap())
        val polar = konfiguriereÜbersetzerPolarAnschlüsse(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "kartesisch"), true)
        var f = eval(polar, mapOf("radius" to BedingterWert(z(1)), "winkel" to w))
        if (diagonal != null) {
            assertEquals(n + 1, diagonal.size)
            f = eval(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "spalte"), mapOf("wert" to f))
            val d = Matrix(List(n + 1) { i -> List(n + 1) { j -> if (i == j) z(diagonal[i]) else z(0) } })
            val op = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero), StrukturRechnerKnotenFamilie.MATRIX, "matrix.produkt")
            val ports = op.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }.take(2)
            f = eval(op, mapOf(ports[0].name to BedingterWert(d), ports[1].name to f))
        }
        val m = eval(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "methode"), mapOf("wert" to f))
        val methode = m.objekt as Methode
        assertEquals(listOf("w"), methode.parameter.map { it.name })
        val df = eval(KnotenDaten(art = METHODEN_WERTEVORRAT_ART, name = "Definitionsmenge"), mapOf("methode" to m))
        assertEquals(winkelraum(n), df.objekt)
        return eval(MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero), mapOf("menge" to df, "methode" to m)).objekt as Abbild
    }
    private fun config(d: RaumDimension, budget: Int = 128, projektion: List<Int> = emptyList()) = VisualisierungsKonfiguration(
        dimension = d, dimensionsModus = DimensionsModus.Manuell, ausgabeKoordinaten = projektion,
        methodenModus = MethodenDarstellungsModus.Bild,
        bereiche = AchsenBereiche(ZahlenBereich(-0.01, 0.01), ZahlenBereich(-0.01, 0.01), ZahlenBereich(-0.01, 0.01)),
        sampling = SamplingKonfiguration(8, 4, 0.01, auflösung1D = 8, maximalesRasterBudget = budget),
    )
    @Test fun `W0 besucht beide diskreten Zeichen exakt`() {
        val e = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(bild(0), config(RaumDimension.R1)))
        assertEquals(setOf(-1.0, 1.0), e.punkte.map { it.x }.toSet())
        assertFalse(e.istApproximation)
        assertEquals(2, e.statistik.auswertungen)
        assertTrue(e.linien.isEmpty() && e.dreiecke.isEmpty())
    }
    @Test fun `W1 Kreis enthaelt beide Halbkreise und verbindet keine Vorzeichenzweige`() {
        val e = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(bild(1), config(RaumDimension.R2)))
        assertTrue(e.punkte.all { abs(it.x * it.x + it.y * it.y - 1.0) < 1e-12 })
        assertTrue(e.punkte.any { it.y > 0.99 } && e.punkte.any { it.y < -0.99 })
        assertEquals(2, e.linien.size)
        e.linien.forEach { l -> assertTrue(l.punkte.all { it.y >= -1e-12 } || l.punkte.all { it.y <= 1e-12 }) }
    }
    @Test fun `W2 Kugel erreicht alle sechs Achsen unabhaengig vom Kamerafenster`() {
        val e = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(bild(2), config(RaumDimension.R3)))
        assertTrue(e.punkte.all { abs(it.x * it.x + it.y * it.y + it.z!! * it.z - 1.0) < 1e-12 })
        for (achse in 0..2) for (s in listOf(-1, 1)) assertTrue(e.punkte.any {
            val p = listOf(it.x, it.y, it.z!!)
            abs(p[achse] - s) < 1e-12
        })
        assertTrue(e.dreiecke.isNotEmpty())
        e.dreiecke.forEach { t ->
            val zs = listOf(t.a.z!!, t.b.z!!, t.c.z!!)
            assertTrue(zs.all { it >= -1e-12 } || zs.all { it <= 1e-12 })
        }
    }
    @Test fun `DF Pipeline erfuellt die Ellipsoidgleichung`() {
        val e = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(bild(2, listOf(2, 3, 4)), config(RaumDimension.R3)))
        assertTrue(e.punkte.all { abs(it.x * it.x / 4 + it.y * it.y / 9 + it.z!! * it.z / 16 - 1.0) < 1e-12 })
    }
    @Test fun `R4 verlangt eine ausdrueckliche Projektion`() {
        val b = bild(3)
        assertIs<VisualisierungsErgebnis.ProjektionErforderlich>(VisualisierungsSampler.sample(b, config(RaumDimension.R3, 16)))
        val e = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(b, config(RaumDimension.R2, 16, listOf(1, 4))))
        assertTrue(e.punkte.any { abs(it.y) > 0.99 })
        assertEquals(winkelraum(3), b.menge)
    }
    @Test fun `Halton ist deterministisch budgetiert und paart die Vorzeichen bis Dimension65`() {
        for (n in listOf(8, 64)) {
            val b = bild(n)
            val c = config(RaumDimension.R3, 17, listOf(1, 2, n + 1))
            val e = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(b, c))
            assertEquals(e, VisualisierungsSampler.sample(b, c))
            assertEquals(16, e.statistik.auswertungen)
            assertTrue(e.hinweise.any { "Halton" in it })
            assertTrue(e.istApproximation && e.linien.isEmpty() && e.dreiecke.isEmpty())
            e.punkte.forEach { p -> assertTrue(e.punkte.any { abs(p.x + it.x) < 1e-12 && abs(p.y + it.y) < 1e-12 && abs(p.z!! + it.z!!) < 1e-12 }) }
            assertTrue(e.punkte.map { it.z }.distinct().size > 2)
        }
    }
    @Test fun `Alle inneren Winkelachsen variieren statt versteckter Schnitte`() {
        val n = 8
        val w = TypisiertesElement("w", MathematikAnschlussArten.Tupel.id.wert, strukturForm = listOf(z(9)))
        val m = MathematischeMethode("id", listOf(w), Tupel(listOf(w)), Tupelraum(listOf(winkelraum(n))), mapOf("w" to winkelraum(n)))
        for (achse in 0 until n) {
            val e = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(Abbild(winkelraum(n), m), config(RaumDimension.R2, 16, listOf(achse + 2, 1))))
            assertTrue(e.punkte.map { it.x }.distinct().size > 1, "Winkelachse $achse wurde festgesetzt")
            assertEquals(setOf(-1.0, 1.0), e.punkte.map { it.y }.toSet())
        }
    }
    @Test fun `Projektion persistiert mit Undo und verbirgt keine undefinierten Komponenten`() {
        val c = config(RaumDimension.R3, projektion = listOf(2, 4, 5)).copy(sampling = SamplingKonfiguration(16, 8, 0.01, auflösung1D = 16, maximalesRasterBudget = 1_000))
        assertEquals(c, VisualisierungsKonfiguration.aus(c.zuEigenschaften()))
        assertNotEquals(c.samplingSignatur(), c.copy(ausgabeKoordinaten = listOf(1, 2, 3)).samplingSignatur())
        val knoten = KnotenDaten(art = "mathematik.visualisierung", name = "Projektion", eigenschaften = c.zuEigenschaften())
        val karte = KartenDaten(name = "Projektion", knoten = listOf(knoten))
        val geladen = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))
        assertEquals(knoten.id, geladen.knoten.single().id)
        assertEquals(c, VisualisierungsKonfiguration.aus(geladen.knoten.single().eigenschaften))
        val editor = de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(karte,
            de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)))
        editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion.KnotenErsetzen(
            knoten.copy(eigenschaften = c.copy(ausgabeKoordinaten = listOf(1, 2, 3)).zuEigenschaften())))
        editor.rückgängig()
        assertEquals(c, VisualisierungsKonfiguration.aus(editor.karte.knoten.single().eigenschaften))
        editor.wiederholen()
        assertEquals(listOf(1, 2, 3), VisualisierungsKonfiguration.aus(editor.karte.knoten.single().eigenschaften).ausgabeKoordinaten)
        val ungültig = Tupel(listOf(z(1), z(2), Division(z(1), z(0)), z(4)))
        assertIs<KoordinatenErgebnis.NichtDarstellbar>(KoordinatenAdapter.extrahiere(ungültig, 2, ausgabeKoordinaten = listOf(1, 2)))
        assertIs<KoordinatenErgebnis.NichtDarstellbar>(KoordinatenAdapter.extrahiere(Tupel(listOf(z(1), z(2), z(3), z(4))), 2, ausgabeKoordinaten = listOf(1, 5)))
    }
    @Test fun `Benannte Ausgabewahl prueft ebenfalls das gesamte Ergebnis`() {
        val w = TypisiertesElement("w", MathematikAnschlussArten.Tupel.id.wert, strukturForm = listOf(z(1)))
        val m = MathematischeMethode("f", listOf(w), Tupel(listOf(z(1), z(2), Division(z(1), z(0)))),
            Tupelraum(List(3) { ReelleZahlen }), mapOf("w" to winkelraum(0)), listOf("x", "y", "z"))
        assertIs<VisualisierungsErgebnis.NichtDarstellbar>(VisualisierungsSampler.sample(Abbild(winkelraum(0), m),
            config(RaumDimension.R2).copy(methodenModus = MethodenDarstellungsModus.Koordinatenausgabe)))
    }
    @Test fun `Echter Kartengraph behaelt Wn nach JSON und Kopieren bis zum Visualisierer`() {
        val laufzeit = MathematikKartenLaufzeit()
        val w = konfiguriereVariablenStruktur(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero), VARIABLE_STRUKTUR_WINKEL)
            .let { it.copy(parameter = it.parameter + mapOf(VARIABLE_DIMENSION to "2", "name" to "w")) }
        val r = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).let { it.copy(parameter = mapOf("wert" to "1")) }
        val polar = konfiguriereÜbersetzerPolarAnschlüsse(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "kartesisch"), true)
        val m = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "methode")
        val df = alleMathematikKnotenVorlagen().first { it.art == METHODEN_WERTEVORRAT_ART }.erzeuge(GraphPunkt.Zero)
        val b = MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero)
        val v = MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero).copy(eigenschaften = config(RaumDimension.R3).zuEigenschaften())
        fun port(k: KnotenDaten, name: String, richtung: AnschlussRichtung) = AnschlussVerweis(k.id, k.anschlüsse.single { it.name == name && it.richtung == richtung }.id)
        fun verbindung(von: KnotenDaten, aus: String, zu: KnotenDaten, ein: String) = VerbindungDaten(
            von = port(von, aus, AnschlussRichtung.Ausgang), zu = port(zu, ein, AnschlussRichtung.Eingang))
        val karte = KartenDaten(name = "W2 Pipeline", knoten = listOf(w, r, polar, m, df, b, v), verbindungen = listOf(
            verbindung(w, "wert", polar, "winkel"), verbindung(r, "wert", polar, "radius"), verbindung(polar, "ergebnis", m, "wert"),
            verbindung(m, "ergebnis", df, "methode"), verbindung(df, "menge", b, "menge"), verbindung(m, "ergebnis", b, "methode"), verbindung(b, "menge", v, "menge")))
        val geladen = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))
        assertEquals(karte.knoten.map { it.id }, geladen.knoten.map { it.id })
        assertEquals(karte.verbindungen.map { it.id }, geladen.verbindungen.map { it.id })
        val clip = de.TeutonStudio.KnotenKartenVerwalter.logik.AtlasZwischenablage()
        assertTrue(clip.kopiere(geladen, geladen.knoten.mapTo(mutableSetOf()) { it.id }))
        val kopie = clip.erzeugeEinfügung(GraphPunkt(2000f, 0f))!!
        val kopiert = KartenDaten(name = "Kopie", knoten = kopie.knoten, verbindungen = kopie.verbindungen)
        for (k in listOf(karte, geladen, kopiert)) {
            val e = laufzeit.auswerten(k)
            assertTrue(e.fehler.isEmpty(), e.fehler.toString())
            val ausgang = k.knoten.single { it.art == "mathematik.visualisierung" }
            val menge = e.knoten.getValue(ausgang.id).ausgaben.getValue("menge").objekt as Abbild
            assertEquals(winkelraum(2), menge.menge)
            val bild = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(menge, config(RaumDimension.R3)))
            assertTrue(bild.punkte.any { it.z!! > 0.99 } && bild.punkte.any { it.z!! < -0.99 })
        }
    }
    @Test fun `Sampling bleibt abbrechbar`() {
        var aufrufe = 0
        assertFailsWith<CancellationException> {
            VisualisierungsSampler.sample(bild(3), config(RaumDimension.R3, 128, listOf(1, 2, 4))) {
                if (++aufrufe == 4) throw CancellationException("Abbruch")
            }
        }
    }
}
