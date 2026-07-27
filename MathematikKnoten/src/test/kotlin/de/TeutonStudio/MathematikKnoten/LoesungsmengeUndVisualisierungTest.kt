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
        val c2 = VisualisierungsKonfiguration(achsen = AchsenZuordnung("x", "y", "z"), bereiche = AchsenBereiche(ZahlenBereich(-2.0, 2.0), ZahlenBereich(-2.0, 2.0), ZahlenBereich(-2.0, 2.0)), sampling = SamplingKonfiguration(40, 16, .1))
        assertIs<VisualisierungsErgebnis.Erfolgreich>(VisualisierungsSampler.sample(EndlicheMenge(setOf(Tupel(listOf(RationaleZahl.Eins, RationaleZahl.Null)))), c2))
        val x = Variable("x"); val y = Variable("y"); val z = Variable("z")
        val kreis = DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen)), Gleichheit(addition(Potenz(x, RationaleZahl.von(2)), Potenz(y, RationaleZahl.von(2))), RationaleZahl.Eins))
        assertTrue((VisualisierungsSampler.sample(kreis, c2) as VisualisierungsErgebnis.Erfolgreich).punkte.isNotEmpty())
        val kugel = DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen), GebundeneMengenVariable(z, ReelleZahlen)), Gleichheit(addition(Potenz(x, RationaleZahl.von(2)), Potenz(y, RationaleZahl.von(2)), Potenz(z, RationaleZahl.von(2))), RationaleZahl.Eins))
        assertTrue((VisualisierungsSampler.sample(kugel, c2.copy(dimension = RaumDimension.R3)) as VisualisierungsErgebnis.Erfolgreich).punkte.isNotEmpty())
    }
}
