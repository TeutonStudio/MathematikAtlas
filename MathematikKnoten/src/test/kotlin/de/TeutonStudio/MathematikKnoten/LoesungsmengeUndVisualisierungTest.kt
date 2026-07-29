package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class LoesungsmengeUndVisualisierungTest {
    private val register = StandardMathematikAuswerter.erzeugeRegister()

    @Test fun `Lösungsmenge bildet definierte Menge mit automatischen Variablen`() {
        val knoten = MathematikKnotenVorlagen.Lösungsmenge.erzeuge(GraphPunkt.Zero)
        val x = Variable("x"); val y = Variable("y")
        val ausgabe = register.finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(knoten, mapOf("bedingung" to BedingterWert(Gleichheit(x, y))), RechenKontext())).ausgaben.getValue("menge").objekt
        assertEquals(listOf("x", "y"), assertIs<DefinierteMenge>(ausgabe).variablen.map { it.variable.name })
    }

    @Test fun `Lösungsmenge respektiert manuelle Reihenfolge`() {
        val basis = MathematikKnotenVorlagen.Lösungsmenge.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf("automatisch" to "false", "variablen" to "y,x", "grundmengen" to "R,N"))
        val x = Variable("x"); val y = Variable("y")
        val menge = register.finde(basis.art)!!.auswerten(KnotenAuswertungsKontext(basis, mapOf("bedingung" to BedingterWert(Gleichheit(x, y))), RechenKontext())).ausgaben.getValue("menge").objekt as DefinierteMenge
        assertEquals(listOf("y", "x"), menge.variablen.map { it.variable.name })
        assertEquals(NatürlicheZahlen, menge.variablen[1].grundMenge)
    }

    @Test fun `Visualisierung reicht Menge und Annahmen weiter`() {
        val knoten = MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero)
        val eingang = BedingterWert(EndlicheMenge(emptySet()), setOf(WahrheitsKonstante(true)))
        assertEquals(eingang, register.finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(knoten, mapOf("menge" to eingang), RechenKontext())).ausgaben.getValue("menge"))
    }

    @Test fun `Sampler zeichnet Punkte Kreis und Kugel`() {
        val c2 = VisualisierungsKonfiguration(
            achsen = AchsenZuordnung("x", "y", "z"),
            bereiche = AchsenBereiche(ZahlenBereich(-2.0, 2.0), ZahlenBereich(-2.0, 2.0), ZahlenBereich(-2.0, 2.0)),
            sampling = SamplingKonfiguration(40, 16, .1),
        )
        assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(EndlicheMenge(setOf(Tupel(listOf(RationaleZahl.Eins, RationaleZahl.Null)))), c2))
        val x = Variable("x"); val y = Variable("y"); val z = Variable("z")
        val kreis = DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen)), Gleichheit(addition(Potenz(x, RationaleZahl.von(2)), Potenz(y, RationaleZahl.von(2))), RationaleZahl.Eins))
        assertTrue((VisualisierungsSampler.sample(kreis, c2) as VisualisierungsErgebnis.Erfolgreich).punkte.isNotEmpty())
        val kugel = DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen), GebundeneMengenVariable(z, ReelleZahlen)), Gleichheit(addition(Potenz(x, RationaleZahl.von(2)), Potenz(y, RationaleZahl.von(2)), Potenz(z, RationaleZahl.von(2))), RationaleZahl.Eins))
        assertTrue((VisualisierungsSampler.sample(kugel, c2.copy(dimension = RaumDimension.R3)) as VisualisierungsErgebnis.Erfolgreich).punkte.isNotEmpty())
    }

    @Test fun `Kartesisches Produkt zweier Intervalle wird als R2 Fläche gesampelt`() {
        val intervall = ReellesIntervall(RationaleZahl.von(2), RationaleZahl.von(6))
        val produkt = KartesischesProdukt(listOf(intervall, intervall))
        val config = VisualisierungsKonfiguration(
            dimension = RaumDimension.R2,
            bereiche = AchsenBereiche(ZahlenBereich(0.0, 8.0), ZahlenBereich(0.0, 8.0), null),
            sampling = SamplingKonfiguration(24, 12, .05),
        )

        val ergebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(produkt, config))

        assertEquals(24 * 24, ergebnis.punkte.size)
        assertTrue(ergebnis.punkte.all { it.x in 2.0..6.0 && it.y in 2.0..6.0 })
    }

    @Test fun `Intervall und eindimensionale Lösungsmenge werden in R1 dargestellt`() {
        val config = VisualisierungsKonfiguration(
            dimension = RaumDimension.R1,
            achsen = AchsenZuordnung("x", "y", "z"),
            bereiche = AchsenBereiche(ZahlenBereich(-3.0, 3.0), ZahlenBereich(-1.0, 1.0), null),
            sampling = SamplingKonfiguration(32, 12, .05),
        )
        val intervall = ReellesIntervall(RationaleZahl.von(-2), RationaleZahl.von(2))
        val intervallErgebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(intervall, config))
        assertTrue(intervallErgebnis.punkte.all { it.y == 0.0 && it.x in -2.0..2.0 })

        val x = Variable("x")
        val lösung = DefinierteMenge(
            listOf(GebundeneMengenVariable(x, ReelleZahlen)),
            Konjunktion(listOf(Vergleich(x, VergleichsArt.GrößerGleich, RationaleZahl.von(-1)), Vergleich(x, VergleichsArt.KleinerGleich, RationaleZahl.Eins))),
        )
        val lösungsErgebnis = assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(lösung, config))
        assertTrue(lösungsErgebnis.punkte.isNotEmpty())
        assertTrue(lösungsErgebnis.punkte.all { it.x in -1.05..1.05 })
    }
}
