package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class GefilterteRegionDarstellungTest {
    private val x=Variable("x");private val y=Variable("y")
    private val basis=KartesischesProdukt(listOf(ReelleZahlen,ReelleZahlen))
    private val c=VisualisierungsKonfiguration(achsen=AchsenZuordnung("x","y",null),
        bereiche=AchsenBereiche(ZahlenBereich(-2.0,2.0),ZahlenBereich(-2.0,2.0),null),
        sampling=SamplingKonfiguration(32,8,.001,maximalesRasterBudget=8000,bildBreite=400.0,bildHöhe=400.0))
    private fun aus(e:VisualisierungsErgebnis):List<VisualisierungsZelle> = when(e) {
        is VisualisierungsErgebnis.Erfolgreich -> e.zellen
        is VisualisierungsErgebnis.Teilweise -> e.zellen
        else -> error("Keine Fläche: $e")
    }
    private fun linien(e:VisualisierungsErgebnis)=when(e) {
        is VisualisierungsErgebnis.Erfolgreich -> e.linien
        is VisualisierungsErgebnis.Teilweise -> e.linien
        else -> error("Kein Rand: $e")
    }
    private fun statistik(e:VisualisierungsErgebnis)=when(e) {
        is VisualisierungsErgebnis.Erfolgreich -> e.statistik
        is VisualisierungsErgebnis.Teilweise -> e.statistik
        else -> error("Keine Statistik: $e")
    }
    private fun nutzerMenge():GefilterteMenge {
        val json=javaClass.getResource("/regression/tupel-filter-visualisierung-v3.json")!!.readText()
        val karte=MathematikKartenCodec.lade(json)
        val r=MathematikKartenLaufzeit().auswerten(karte)
        assertTrue(r.fehler.isEmpty(),r.fehler.joinToString())
        val v=karte.knoten.single {it.art=="mathematik.visualisierung"}
        val m=assertIs<GefilterteMenge>(r.knoten.getValue(v.id).eingänge.getValue("menge").objekt)
        assertEquals(1,m.methode.parameter.size)
        assertEquals(basis,m.menge)
        // Laden darf auch den historischen Visualisiererausgang nicht stillschweigend entfernen.
        assertEquals(2,v.anschlüsse.size)
        val geladen=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))
        assertEquals(karte.knoten.map {it.id},geladen.knoten.map {it.id})
        assertEquals(karte.verbindungen,geladen.verbindungen)
        return m
    }
    @Test fun nutzerkarteBindetDasTupelUndFiltertDieRichtigenPunkte() {
        val region=assertIs<VisualisierungsDefinition.Region>(VisualisierungsSampler.normalisiere(nutzerMenge(),c))
        assertEquals(NumerischeMitgliedschaft.Enthalten,region.mitgliedschaft(listOf(0.0,1.0)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(1.0,0.0)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(1.0,1.0)))
        assertEquals(ZellNachweis.Enthalten,region.zellNachweis!!(listOf(RationalesIntervall(RationaleZahl.von(-2),RationaleZahl.von(-1)),RationalesIntervall(RationaleZahl.von(-1),RationaleZahl.Eins))))
    }
    @Test fun direkteDefinitionUndFilterBesitzenDieselbeFlaecheUndOffenenRand() {
        val direkt=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)),Vergleich(x,VergleichsArt.Kleiner,multiplikation(y,y)))
        val a=VisualisierungsSampler.sample(nutzerMenge(),c);val b=VisualisierungsSampler.sample(direkt,c)
        assertEquals(aus(b),aus(a));assertEquals(linien(b),linien(a))
        assertTrue(linien(a).isNotEmpty());assertTrue(linien(a).all {it.offenerRand})
        assertTrue(aus(a).any {it.status==ZellenStatus.Enthalten})
        assertTrue(aus(a).any {it.status==ZellenStatus.Ausgeschlossen})
        assertFalse(aus(a).any {it.status==ZellenStatus.Unbekannt && it.minimum==listOf(-2.0,-2.0) && it.maximum==listOf(2.0,2.0)})
    }
    @Test fun arbeitsbudgetZaehltZellenPunkteUndRandGemeinsam() {
        val menge=nutzerMenge()
        for(budget in listOf(1,4,16,64,512,8000)) {
            val e=VisualisierungsSampler.sample(menge,c.copy(sampling=c.sampling.copy(maximalesRasterBudget=budget)))
            val s=statistik(e)
            assertTrue(s.auswertungen<=budget,"$budget: $s")
            assertEquals(s.auswertungen,s.zellArbeit+s.punktArbeit+s.randArbeit)
        }
    }
    @Test fun konstanteFilterFuellenAllesOderNichts() {
        val p=TypisiertesElement("p",MathematikAnschlussArten.Tupel.id.wert)
        for(wahr in listOf(false,true)) {
            val m=GefilterteMenge(basis,Methode(name="P",parameter=listOf(p),vorschrift=WahrheitsKonstante(wahr),zielMenge=WahrheitsMenge,werteVorräte=mapOf("p" to basis)))
            val z=aus(VisualisierungsSampler.sample(m,c))
            assertTrue(z.all {it.status==if(wahr) ZellenStatus.Enthalten else ZellenStatus.Ausgeschlossen})
        }
    }
    @Test fun voraussetzungWirdNichtVomFilterVerloren() {
        val m=MathematischeMethode(name="P",parameter=listOf(x),vorschrift=WahrheitsKonstante(true),zielMenge=WahrheitsMenge,
            werteVorräte=mapOf("x" to ReelleZahlen),voraussetzungen=setOf(Vergleich(x,VergleichsArt.Größer,RationaleZahl.Null)))
        val region=assertIs<VisualisierungsDefinition.Region>(VisualisierungsSampler.normalisiere(GefilterteMenge(ReelleZahlen,m),c.copy(dimension=RaumDimension.R1,achsen=AchsenZuordnung("x","",null))))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(-1.0)))
        assertEquals(NumerischeMitgliedschaft.Enthalten,region.mitgliedschaft(listOf(1.0)))
    }
    @Test fun unbekanntePraedikateBleibenUnbekanntUndVerbrauchenKeinZellraster() {
        val p=TypisiertesElement("p","tupel")
        val m=GefilterteMenge(basis,Methode(name="P",parameter=listOf(p),vorschrift=UnentscheidbareAussage("offenes Prädikat","Test"),zielMenge=WahrheitsMenge,werteVorräte=mapOf("p" to basis)))
        val e=VisualisierungsSampler.sample(m,c)
        assertTrue(aus(e).all {it.status==ZellenStatus.Unbekannt})
        assertTrue(statistik(e).auswertungen<=8000)
    }
    @Test fun falscheVoraussetzungErzeugtKeineUnbekannteVollflaeche() {
        val m=MathematischeMethode(name="P",parameter=listOf(x),vorschrift=WahrheitsKonstante(true),zielMenge=WahrheitsMenge,
            werteVorräte=mapOf("x" to ReelleZahlen),voraussetzungen=setOf(WahrheitsKonstante(false)))
        val region=assertIs<VisualisierungsDefinition.Region>(VisualisierungsSampler.normalisiere(GefilterteMenge(ReelleZahlen,m),c.copy(dimension=RaumDimension.R1,achsen=AchsenZuordnung("x","",null))))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(1.0)))
    }
    @Test fun verschachteltesTupelBleibtEinOeffentlichesArgument() {
        val p=TypisiertesElement("p","tupel")
        val basis=KartesischesProdukt(listOf(ReelleZahlen,KartesischesProdukt(listOf(ReelleZahlen,ReelleZahlen))))
        val a=strukturKomponente(p,RationaleZahl.Eins,true) as ZahlAusdruck
        val inneres=strukturKomponente(p,RationaleZahl.von(2),false)
        val b=strukturKomponente(inneres,RationaleZahl.von(2),true) as ZahlAusdruck
        val m=MathematischeMethode(name="P",parameter=listOf(p),vorschrift=Vergleich(a,VergleichsArt.Kleiner,b),zielMenge=WahrheitsMenge,werteVorräte=mapOf("p" to basis))
        val region=assertIs<VisualisierungsDefinition.Region>(VisualisierungsSampler.normalisiere(GefilterteMenge(basis,m),c.copy(dimension=RaumDimension.R3,achsen=AchsenZuordnung("x","y","z"),bereiche=c.bereiche.copy(z=ZahlenBereich(-2.0,2.0)))))
        assertEquals(NumerischeMitgliedschaft.Enthalten,region.mitgliedschaft(listOf(0.0,9.0,1.0)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(2.0,9.0,1.0)))
    }
    @Test fun partielleTermvoraussetzungWirdNichtAlsReellAngenommen() {
        val m=MathematischeMethode(name="P",parameter=listOf(x),vorschrift=WahrheitsKonstante(true),zielMenge=WahrheitsMenge,
            werteVorräte=mapOf("x" to ReelleZahlen),voraussetzungen=setOf(ElementBeziehung(Wurzel(x),ReelleZahlen)))
        val region=assertIs<VisualisierungsDefinition.Region>(VisualisierungsSampler.normalisiere(GefilterteMenge(ReelleZahlen,m),c.copy(dimension=RaumDimension.R1,achsen=AchsenZuordnung("x","",null))))
        assertNotEquals(NumerischeMitgliedschaft.Enthalten,region.mitgliedschaft(listOf(-1.0)))
        assertEquals(NumerischeMitgliedschaft.Enthalten,region.mitgliedschaft(listOf(1.0)))
    }
    @Test fun abhaengigeGrundmengenDesDefinitionsraumsWerdenMitgebunden() {
        val u=Variable("u");val v=Variable("v");val p=TypisiertesElement("p","tupel")
        val dom=DefinierteMenge(listOf(GebundeneMengenVariable(u,ReelleZahlen),GebundeneMengenVariable(v,ReellesIntervall(RationaleZahl.Null,true,u,true))),WahrheitsKonstante(true))
        val m=MathematischeMethode(name="P",parameter=listOf(p),vorschrift=WahrheitsKonstante(true),zielMenge=WahrheitsMenge,werteVorräte=mapOf("p" to basis),effektiverWerteVorrat=dom)
        val region=assertIs<VisualisierungsDefinition.Region>(VisualisierungsSampler.normalisiere(GefilterteMenge(basis,m),c))
        assertEquals(NumerischeMitgliedschaft.Enthalten,region.mitgliedschaft(listOf(1.0,.5)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(1.0,1.5)))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(-1.0,.5)))
    }
    @Test fun nichtZertifizierbareStetigeBedingungErhaeltLokaleNumerischeFlaechen() {
        val m=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)),Vergleich(y,VergleichsArt.Kleiner,Sinus(x)))
        val z=aus(VisualisierungsSampler.sample(m,c))
        assertTrue(z.any {it.status==ZellenStatus.NumerischEnthalten})
        assertTrue(z.any {it.status==ZellenStatus.Ausgeschlossen})
        assertTrue(z.any {it.status==ZellenStatus.Unbekannt})
        assertFalse(z.any {it.status==ZellenStatus.Enthalten})
    }
    @Test fun externerGleichnamigerParameterWirdNichtZurAchse() {
        val extern=Variable("y",bindungsId="extern")
        val m=MathematischeMethode(name="P",parameter=listOf(x),vorschrift=Vergleich(x,VergleichsArt.Kleiner,extern),zielMenge=WahrheitsMenge,werteVorräte=mapOf("x" to ReelleZahlen))
        assertIs<VisualisierungsDefinition.BedingtRäumlich>(VisualisierungsSampler.normalisiere(GefilterteMenge(ReelleZahlen,m),c.copy(dimension=RaumDimension.R1,achsen=AchsenZuordnung("y","",null))))
    }
    @Test fun gemeinsamerDefinitionsraumBleibtAlsEinschraenkungErhalten() {
        val p=TypisiertesElement("p","tupel")
        val dom=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)),Vergleich(x,VergleichsArt.Größer,RationaleZahl.Null))
        val m=MathematischeMethode(name="P",parameter=listOf(p),vorschrift=WahrheitsKonstante(true),zielMenge=WahrheitsMenge,werteVorräte=mapOf("p" to basis),effektiverWerteVorrat=dom)
        val region=assertIs<VisualisierungsDefinition.Region>(VisualisierungsSampler.normalisiere(GefilterteMenge(basis,m),c))
        assertEquals(NumerischeMitgliedschaft.NichtEnthalten,region.mitgliedschaft(listOf(-1.0,0.0)))
        assertEquals(NumerischeMitgliedschaft.Enthalten,region.mitgliedschaft(listOf(1.0,0.0)))
    }
    @Test fun sichtbereichHatZwanzigProzentReserveUndWirdNachPanVerwendet() {
        val k=c.copy(kamera=c.kamera.copy(translationX=-40.0,zoom=2.0))
        val t=VisualisierungsTransformation(k,600.0,300.0)
        val sicht=t.sichtBereiche(0.0);val berechnet=t.sichtBereiche()
        assertEquals((sicht.x.maximum-sicht.x.minimum)*1.2,berechnet.x.maximum-berechnet.x.minimum,1e-10)
        assertEquals((sicht.y.maximum-sicht.y.minimum)*1.2,berechnet.y.maximum-berechnet.y.minimum,1e-10)
        val e=VisualisierungsSampler.sample(nutzerMenge(),k.copy(sampling=k.sampling.copy(sichtBereiche=berechnet)))
        assertTrue(aus(e).all {it.minimum[0]>=berechnet.x.minimum && it.maximum[0]<=berechnet.x.maximum})
        assertTrue(berechnet.x.minimum>30)
    }
}
