package de.TeutonStudio.MathematikKnoten.visualisierung

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VisualisierungsSichtfensterTest {
    private val x=Variable("x")
    private fun graph(v: ZahlAusdruck, domäne: MengenAusdruck=ReelleZahlen)=Abbild(domäne,Methode(name="f",parameter=listOf(x),vorschrift=v,zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen)))
    private fun erfolgreich(m: MengenAusdruck,c:VisualisierungsKonfiguration):VisualisierungsErgebnis.Erfolgreich = assertIs(VisualisierungsSampler.sample(m,c))
    @Test fun geradeDecktSchonInDerVorschauDasGesamteFensterAb() {
        val c=VisualisierungsKonfiguration().alsVorschau().copy(sampling=VisualisierungsKonfiguration().alsVorschau().sampling.copy(
            bildBreite=800.0,bildHöhe=400.0,zielFehlerPixel=3.0,sichtBereiche=AchsenBereiche(ZahlenBereich(-24.0,24.0),ZahlenBereich(-12.0,12.0),null)))
        val e=erfolgreich(graph(x),c)
        assertEquals(-24.0,e.punkte.minOf{it.x});assertEquals(24.0,e.punkte.maxOf{it.x})
        assertTrue(e.statistik.auswertungen<=192);assertTrue(e.linien.isNotEmpty())
    }
    @Test fun funktionsgraphLaedtNeueSichtbereicheNachAberBeachtetDieDefinitionsmenge() {
        val c=VisualisierungsKonfiguration().copy(sampling=VisualisierungsKonfiguration().sampling.copy(
            sichtBereiche=AchsenBereiche(ZahlenBereich(40.0,50.0),ZahlenBereich(-10.0,10.0),null)))
        val e=erfolgreich(graph(x),c)
        assertEquals(40.0,e.punkte.minOf{it.x});assertEquals(50.0,e.punkte.maxOf{it.x})
        val beschränkt=graph(x,ReellesIntervall(RationaleZahl.von(-1),false,RationaleZahl.Eins,false))
        assertTrue(erfolgreich(beschränkt,c).punkte.isEmpty())
    }
    @Test fun parametrischesBildBehältSeinenVollstaendigenParameterbereich() {
        val domäne=ReellesIntervall(RationaleZahl.von(-1),false,RationaleZahl.Eins,false)
        val bild=Abbild(domäne,Methode(name="p",parameter=listOf(x),vorschrift=Tupel(listOf(x,multiplikation(x,x))),zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen)))
        val c=VisualisierungsKonfiguration(methodenModus=MethodenDarstellungsModus.Bild).copy(sampling=VisualisierungsKonfiguration().sampling.copy(
            sichtBereiche=AchsenBereiche(ZahlenBereich(40.0,50.0),ZahlenBereich(-10.0,10.0),null)))
        val e=erfolgreich(bild,c)
        assertEquals(-1.0,e.punkte.minOf{it.x});assertEquals(1.0,e.punkte.maxOf{it.x})
    }
    @Test fun kurveVerbindetNichtUeberEinePolstelle() {
        val ergebnis=VisualisierungsSampler.sample(graph(Division(RationaleZahl.Eins,x)),VisualisierungsKonfiguration())
        val linien = when(ergebnis) { is VisualisierungsErgebnis.Erfolgreich -> ergebnis.linien; is VisualisierungsErgebnis.Teilweise -> ergebnis.linien; else -> error("Kurve fehlt") }
        assertTrue(linien.none { l -> l.punkte.zipWithNext().any{(a,b)->a.x<0 && b.x>0} })
    }
    @Test fun sinusMitAchtPeriodenWirdNichtAlsNullgeradeVerkannt() {
        val c=VisualisierungsKonfiguration(bereiche=AchsenBereiche(ZahlenBereich(0.0,1.0),ZahlenBereich(-1.2,1.2),null)).alsVorschau()
        val runtime=c.copy(sampling=c.sampling.copy(bildBreite=800.0,bildHöhe=400.0,zielFehlerPixel=3.0))
        val ergebnis=VisualisierungsSampler.sample(graph(Sinus(multiplikation(RationaleZahl.von(16),Pi,x)),ReellesIntervall(RationaleZahl.Null,false,RationaleZahl.Eins,false)),runtime)
        val punkte = when(ergebnis){is VisualisierungsErgebnis.Erfolgreich->ergebnis.punkte;is VisualisierungsErgebnis.Teilweise->ergebnis.punkte;else->error("Kurve fehlt")}
        assertTrue(punkte.maxOf{it.y}>.9);assertTrue(punkte.minOf{it.y}<-.9)
        assertEquals(0.0,punkte.minOf{it.x});assertEquals(1.0,punkte.maxOf{it.x})
    }
    @Test fun dreidimensionaleRegionWirdNachPanAusserhalbDesAltenFenstersBerechnet() {
        val y=Variable("y");val z=Variable("z")
        val region=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen),GebundeneMengenVariable(z,ReelleZahlen)),
            Vergleich(addition(Potenz(addition(x,RationaleZahl.von(-40)),RationaleZahl.von(2)),Potenz(y,RationaleZahl.von(2)),Potenz(z,RationaleZahl.von(2))),VergleichsArt.KleinerGleich,RationaleZahl.von(100)))
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3,kamera=KameraZustand(0.0,0.0,0.0,-40.0,0.0,0.0,1.0))
        fun punkte(e:VisualisierungsErgebnis)=when(e){is VisualisierungsErgebnis.Erfolgreich->e.punkte;is VisualisierungsErgebnis.Teilweise->e.punkte;else->error("Region fehlt")}
        assertTrue(punkte(VisualisierungsSampler.sample(region,c)).isEmpty())
        val sicht=VisualisierungsTransformation(c,500.0,500.0).sichtBereiche()
        val e=VisualisierungsSampler.sample(region,c.copy(sampling=c.sampling.copy(sichtBereiche=sicht)))
        assertTrue(punkte(e).any{it.x>30.0})
    }

}
