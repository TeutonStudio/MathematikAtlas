package de.TeutonStudio.MathematikKnoten.visualisierung

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.VisualisierungsEntwurf
import kotlin.test.*

class VisualisierungsAnsichtTest {
    @Test fun weltBildRoundtripInAllenRaeumen() {
        RaumDimension.entries.forEach { d ->
            val c = VisualisierungsKonfiguration(dimension = d, kamera = KameraZustand(27.0, -41.0, 12.0, 2.0, -3.0, 0.0, 2.3))
            val t = VisualisierungsTransformation(c, 800.0, 350.0)
            val b = t.bild(1.2, if (d == RaumDimension.R1) 0.0 else -4.2, 2.7)
            val w = t.welt(b.x, b.y, b.tiefe)
            assertEquals(1.2, w.x, 1e-10)
            if (d != RaumDimension.R1) assertEquals(-4.2, w.y, 1e-10)
            if (d == RaumDimension.R3) assertEquals(2.7, w.tiefe, 1e-10)
        }
    }
    @Test fun zoomHaeltDenWeltpunktUnterDemAnkerFest() {
        val c = VisualisierungsKonfiguration()
        val t = VisualisierungsTransformation(c, 900.0, 400.0)
        val w = t.welt(700.0, 80.0)
        val k = t.navigiere(0.0, 0.0, 1.7, 700.0, 80.0)
        val p = VisualisierungsTransformation(c.copy(kamera = k), 900.0, 400.0).bild(w.x, w.y)
        assertEquals(700.0, p.x, 1e-10); assertEquals(80.0, p.y, 1e-10)
    }
    @Test fun panEntsprichtPixelnBeiBeliebigerGroesseUndZoom() {
        val c = VisualisierungsKonfiguration(kamera = KameraZustand(0.0,0.0,0.0,2.0,3.0,0.0,4.0))
        val t = VisualisierungsTransformation(c, 1000.0, 300.0)
        val a = t.bild(1.0,2.0)
        val k = t.navigiere(93.0,-42.0,1.0,500.0,150.0)
        val b = VisualisierungsTransformation(c.copy(kamera=k),1000.0,300.0).bild(1.0,2.0)
        assertEquals(93.0,b.x-a.x,1e-10);assertEquals(-42.0,b.y-a.y,1e-10)
    }
    @Test fun quadratischeEinheitenUndSichtreserve() {
        val c=VisualisierungsKonfiguration()
        val t=VisualisierungsTransformation(c,800.0,400.0)
        val a=t.bild(0.0,0.0);val x=t.bild(1.0,0.0);val y=t.bild(0.0,1.0)
        assertEquals(x.x-a.x,a.y-y.y,1e-10)
        assertEquals(-24.0,t.sichtBereiche().x.minimum,1e-10); assertEquals(24.0,t.sichtBereiche().x.maximum,1e-10)
        assertEquals(-12.0,t.sichtBereiche().y.minimum,1e-10); assertEquals(12.0,t.sichtBereiche().y.maximum,1e-10)
    }
    @Test fun achsenSchritteSindEinsZweiFuenf() {
        assertEquals(1.0,achsenSchritt(.01));assertEquals(2.0,achsenSchritt(.02));assertEquals(5.0,achsenSchritt(.05))
    }
    @Test fun alteZoomkameraWirdZentriertUndNeueAnsichtBleibtImRoundtrip() {
        val c=VisualisierungsKonfiguration(kamera=KameraZustand(0.0,0.0,0.0,0.0,0.0,0.0,2.0))
        val alt=c.zuEigenschaften()-"ansichtVersion"
        val geladen=VisualisierungsKonfiguration.aus(alt)
        assertEquals(5.0,geladen.kamera.translationX)
        assertEquals(geladen,VisualisierungsKonfiguration.aus(geladen.zuEigenschaften()))
    }
    @Test fun entwurfBewahrtFremdeEigenschaftenUndErkenntKonflikte() {
        val c=VisualisierungsKonfiguration()
        val original=c.zuEigenschaften()+ ("fremd" to KnotenEigenschaft.Text("bewahren"))
        val e=VisualisierungsEntwurf(original)
        e.konfiguration=c.copy(rasterSichtbar=false)
        assertTrue(e.geändert);assertFalse(e.konflikt(original))
        assertEquals(KnotenEigenschaft.Text("bewahren"),e.übernehmen(original)["fremd"])
        assertFailsWith<IllegalStateException>{e.übernehmen(c.copy(achsenSichtbar=false).zuEigenschaften())}
        e.neuLaden(original);assertFalse(e.geändert)
        assertEquals(c,VisualisierungsKonfiguration.aus(original))
    }
    @Test fun schnitteWerdenNieTeilweiseUebernommen() {
        assertEquals(mapOf("t" to 3.0,"u" to 2.0),parseFesteSchnitte("t=3;u=2"))
        listOf("t=3;kaputt","t=3;t=4","t=NaN","=2").forEach { assertFailsWith<IllegalArgumentException>{parseFesteSchnitte(it)} }
        assertFailsWith<IllegalArgumentException>{parseAusgabeKoordinaten("1,1")}
        assertEquals(listOf(1,3),parseAusgabeKoordinaten("1,3"))
    }
    @Test fun rotationAendertDenSamplingSchluesselNicht() {
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3)
        val k=c.copy(kamera=c.kamera.copy(rotationX=42.0,rotationY=12.0))
        assertEquals(c.samplingSignatur(),k.samplingSignatur())
        assertEquals(VisualisierungsTransformation(c,500.0,400.0).sichtBereiche(),VisualisierungsTransformation(k,500.0,400.0).sichtBereiche())
    }
    @Test fun einpassenZentriertAsymmetrischesIntervallAuchAufBreiterZahlengerade() {
        val c = VisualisierungsKonfiguration(dimension = RaumDimension.R1)
        val e = de.TeutonStudio.MathematikKnoten.visualisierung.sampling.VisualisierungsErgebnis.Erfolgreich(emptyList(), intervalle = listOf(
            de.TeutonStudio.MathematikKnoten.visualisierung.sampling.VisualisierungsIntervall(4.0, 6.0, true, true)))
        val k = de.TeutonStudio.MathematikKnoten.visualisierung.ui.kameraEinpassen(e, c, 800.0, 400.0)!!
        assertEquals(400.0, VisualisierungsTransformation(c.copy(kamera = k),800.0,400.0).bild(5.0,0.0).x, 1e-10)
    }
    @Test fun unbekannteVerschachtelteEigenschaftenBleibenErhaltenUndLaufzeitdatenWerdenNichtGespeichert() {
        val c = VisualisierungsKonfiguration()
        val alt = c.zuEigenschaften().toMutableMap()
        alt["sampling"] = KnotenEigenschaft.Objekt((alt["sampling"] as KnotenEigenschaft.Objekt).felder + ("erweiterung" to KnotenEigenschaft.Text("bleibt")))
        val neu = c.copy(sampling = c.sampling.copy(bildBreite = 900.0, zielFehlerPixel = 3.0)).inEigenschaften(alt)
        assertEquals(KnotenEigenschaft.Text("bleibt"), (neu["sampling"] as KnotenEigenschaft.Objekt).felder["erweiterung"])
        assertEquals(0.0, VisualisierungsKonfiguration.aus(neu).sampling.bildBreite)
    }

    @Test fun entfernteSchnitteWerdenNichtDurchDieEigenschaftserhaltungWiederhergestellt() {
        val alt = VisualisierungsKonfiguration(festeSchnitte=mapOf("t" to 2.0)).zuEigenschaften()
        val neu = VisualisierungsKonfiguration(festeSchnitte=emptyMap()).inEigenschaften(alt)
        assertTrue(VisualisierungsKonfiguration.aus(neu).festeSchnitte.isEmpty())
        val umbenannt = VisualisierungsKonfiguration(festeSchnitte=mapOf("u" to 2.0)).inEigenschaften(alt)
        assertEquals(mapOf("u" to 2.0),VisualisierungsKonfiguration.aus(umbenannt).festeSchnitte)
    }

    @Test fun alte3DKameraBewahrtDieBildlageBeiRotationUndTranslation() {
        val b = AchsenBereiche(ZahlenBereich(0.0,20.0),ZahlenBereich(-5.0,15.0),ZahlenBereich(4.0,24.0))
        val k = KameraZustand(0.0,90.0,0.0,5.0,3.0,7.0,2.0)
        val alt = VisualisierungsKonfiguration(dimension=RaumDimension.R3,bereiche=b,kamera=k).zuEigenschaften()-"ansichtVersion"
        val c = VisualisierungsKonfiguration.aus(alt)
        val p = VisualisierungsTransformation(c,800.0,800.0).bild(2.0,3.0,4.0)
        // v1: Y-Rotation um 90 Grad macht x'=z+tz und y'=y+ty.
        assertEquals((4.0+7.0-0.0)/20.0*800.0*2,p.x,1e-9)
        assertEquals(800.0-(3.0+3.0+5.0)/20.0*800.0*2,p.y,1e-9)
        assertEquals(c,VisualisierungsKonfiguration.aus(c.zuEigenschaften()))
    }
    @Test fun dreidimensionalesPanUndZoomErweiternDieAbdeckungOhneRotationsabhaengigkeit() {
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3)
        val vorher=VisualisierungsTransformation(c,500.0,500.0).sichtBereiche()
        val pan=VisualisierungsTransformation(c.copy(kamera=c.kamera.copy(translationX=40.0)),500.0,500.0).sichtBereiche()
        val zoom=VisualisierungsTransformation(c.copy(kamera=c.kamera.copy(zoom=.5)),500.0,500.0).sichtBereiche()
        assertTrue(pan.x.maximum>vorher.x.maximum+30);assertTrue(zoom.x.maximum>vorher.x.maximum)
    }

}
