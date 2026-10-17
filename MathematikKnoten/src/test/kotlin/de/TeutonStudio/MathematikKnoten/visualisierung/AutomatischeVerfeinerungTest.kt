package de.TeutonStudio.MathematikKnoten.visualisierung

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.test.*

class AutomatischeVerfeinerungTest {
    @Test fun endlicheMengeUeberAlterGrenzeWirdVollstaendigUndMitZwischenstaendenAufgezaehlt()=runBlocking {
        val menge=EndlicheMenge((0 until 12_000).map { Tupel(listOf(RationaleZahl.von(it.toLong()),RationaleZahl.Null)) }.toSet())
        val alt=VisualisierungsKonfiguration(dimension=RaumDimension.R2)
        assertIs<VisualisierungsErgebnis.NichtDarstellbar>(VisualisierungsSampler.sample(menge,alt))
        val b=VisualisierungsBerechnung()
        try {
            val stände=b.beobachte(menge,alt.automatischeDarstellung(),true).toList()
            assertTrue(stände.dropLast(1).all { it.ergebnis is VisualisierungsErgebnis.Teilweise })
            val zwischenstände=mutableListOf<VisualisierungsErgebnis>()
            val sitzung=SamplingSitzung(VisualisierungsRechenZeit(),{0},{},{ zwischenstände+=it })
            try {
                SamplingSitzung.mit(sitzung) { VisualisierungsSampler.sample(menge,alt.automatischeDarstellung(),sitzung::prüfe) }
                assertTrue(zwischenstände.isNotEmpty())
                assertTrue(zwischenstände.all { it is VisualisierungsErgebnis.Teilweise && it.punkte.size<12_000 })
            } finally { sitzung.close() }
            val e=assertIs<VisualisierungsErgebnis.Erfolgreich>(stände.last().ergebnis)
            assertEquals(12_000,e.punkte.size);assertFalse(e.istApproximation)
            assertEquals(VerfeinerungsPhase.Fertig,stände.last().phase)
        } finally { b.schließen() }
    }
    @Test fun fortsetzungLiefertDieselbenKurvenWieUnunterbrochenerSampler()=runBlocking {
        val x=Variable("x")
        val menge=Methode(name="sin",parameter=listOf(x),vorschrift=Sinus(multiplikation(RationaleZahl.von(20),x)),zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen)).graphMenge()
        val c=VisualisierungsKonfiguration().automatischeDarstellung().let { it.copy(sampling=it.sampling.copy(bildBreite=800.0,bildHöhe=600.0)) }
        val erwartet=VisualisierungsSampler.sample(menge,c)
        val b=VisualisierungsBerechnung()
        try { assertEquals(erwartet,b.beobachte(menge,c,true).last().ergebnis) } finally { b.schließen() }
    }
    @Test fun automatischeQualitaetErhaeltHistorischeBudgetwerteBeimSpeichern() {
        val alt=VisualisierungsKonfiguration(sampling=SamplingKonfiguration(72,22,.08,maximalesRasterBudget=1234))
        val geladen=VisualisierungsKonfiguration.aus(alt.zuEigenschaften())
        val automatisch=geladen.automatischeDarstellung()
        assertEquals(1234,geladen.sampling.maximalesRasterBudget)
        assertTrue(automatisch.sampling.maximalesRasterBudget>64_000)
        assertFalse(VisualisierungsKonfiguration.aus(automatisch.zuEigenschaften()).sampling.automatisch)
        assertEquals(.5,geladen.copy(qualität=DarstellungsQualität.Hoch).automatischeDarstellung().sampling.zielFehlerPixel)
    }
    @Test fun speicherWirdVorVergroesserungReserviertUndNachFreigabeWiederNutzbar() {
        val s=VisualisierungsSpeicher(100)
        val a=s.reserviere();val b=s.reserviere()
        a.setze(70);b.setze(20)
        assertFailsWith<VisualisierungsSpeicherGrenze> { b.setze(31) }
        assertEquals(90,s.bytes)
        a.close();assertEquals(20,s.bytes)
        b.setze(100);b.close();b.close();assertEquals(0,s.bytes)
    }
    @Test fun technischerAbbruchErhaeltLetzteGeometrieUndCacheStatus()=runBlocking {
        val b=VisualisierungsBerechnung()
        val c=VisualisierungsKonfiguration().automatischeDarstellung()
        val menge=EndlicheMenge(setOf(RationaleZahl.Eins))
        try {
            val ergebnis=b.berechne(menge,c,true) {
                VisualisierungsErgebnis.Teilweise(listOf(VisualisierungsPunkt(1.0,0.0)),listOf("Speichergrenze"))
            }
            val stand=b.beobachte(menge,c,true).last()
            assertSame(ergebnis,stand.ergebnis)
            assertEquals(VerfeinerungsPhase.Begrenzt,stand.phase)
        } finally { b.schließen() }
    }
    @org.junit.Test(timeout=5_000) fun zeitQuantumLaesstWartendeSitzungVorWeiterarbeitZumZugKommen() {
        var zeit=0L
        val verteiler=VisualisierungsRechenZeit(1)
        val s=SamplingSitzung(verteiler,{0},{},{},uhr={zeit})
        val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
        val reihenfolge=java.util.Collections.synchronizedList(mutableListOf<String>())
        try {
            SamplingSitzung.mit(s) {
                val wartet=verteiler.vormerken {0}
                val andere=executor.submit {
                    verteiler.betrete({0},{},wartet).use { reihenfolge+="B" }
                }
                zeit=9_000_000;s.prüfe();reihenfolge+="A"
                andere.get(2,java.util.concurrent.TimeUnit.SECONDS)
                assertEquals(listOf("B","A"),reihenfolge)
            }
        } finally { s.close();executor.shutdownNow() }
    }
    @Test fun echteSpeichergrenzeErhaeltLetztenZwischenstandUndGibtReservierungenFrei()=runBlocking {
        val speicher=VisualisierungsSpeicher(1_000_000)
        val b=VisualisierungsBerechnung(arbeitsSpeicher=speicher)
        val menge=EndlicheMenge((0 until 12_000).map { Tupel(listOf(RationaleZahl.von(it.toLong()),RationaleZahl.Null)) }.toSet())
        val c=VisualisierungsKonfiguration().automatischeDarstellung()
        try {
            val stand=b.beobachte(menge,c,true).last()
            val e=assertIs<VisualisierungsErgebnis.Teilweise>(stand.ergebnis)
            assertTrue(e.punkte.isNotEmpty() && e.punkte.size<12_000)
            assertTrue(e.hinweise.any { "Speichergrenze" in it })
            assertEquals(VerfeinerungsPhase.Begrenzt,stand.phase)
            assertEquals(0,speicher.bytes)
            assertEquals(stand,b.beobachte(menge,c,true).last())
        } finally { b.schließen() }
    }
    @Test fun geschlosseneReservierungKannNichtWiederBelegtWerden() {
        val speicher=VisualisierungsSpeicher(100)
        val r=speicher.reserviere();r.setze(50);r.close()
        assertFailsWith<CancellationException> { r.setze(50) }
        assertEquals(0,speicher.bytes)
    }
    @Test fun vieleVerbraucherBlockierenWederCacheNochDialogprioritaet()=runBlocking<Unit> {
        withTimeout(5_000) {
            val b=VisualisierungsBerechnung()
            fun punkt(i:Int)=EndlicheMenge(setOf(RationaleZahl.von(i.toLong())))
            val c=VisualisierungsKonfiguration()
            val start=kotlinx.coroutines.channels.Channel<Int>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            val gates=List(13) { CompletableDeferred<Unit>() }
            try {
                val cache=b.berechne(punkt(99),c,false)
                val aufträge=(0 until 12).map { i -> async(start=CoroutineStart.UNDISPATCHED) {
                    b.berechne(punkt(i),c,false) { start.send(i);gates[i].await();VisualisierungsErgebnis.Erfolgreich(emptyList()) }
                } }
                val erste=setOf(start.receive(),start.receive());assertEquals(2,erste.size)
                assertSame(cache,b.berechne(punkt(99),c,true))
                val dialog=async(start=CoroutineStart.UNDISPATCHED) {
                    b.berechne(punkt(12),c,true) { start.send(12);gates[12].await();VisualisierungsErgebnis.Erfolgreich(emptyList()) }
                }
                val freigeben=erste.first();gates[freigeben].complete(Unit);aufträge[freigeben].await()
                assertEquals(12,start.receive())
                gates.forEach { it.complete(Unit) };dialog.await();aufträge.awaitAll()
            } finally { gates.forEach {it.complete(Unit)};b.schließen() }
        }
    }
    @Test fun nachladenVerwendetFunktionswerteWiederAberNichtBeiAndererFormel()=runBlocking {
        val x=Variable("x")
        fun graph(f:ZahlAusdruck)=Methode(name="f",parameter=listOf(x),vorschrift=f,zielMenge=ReelleZahlen,werteVorräte=mapOf("x" to ReelleZahlen)).graphMenge()
        val menge=graph(Sinus(x))
        val c=VisualisierungsKonfiguration().automatischeDarstellung().let { it.copy(sampling=it.sampling.copy(bildBreite=800.0,bildHöhe=600.0)) }
        val b=VisualisierungsBerechnung()
        try {
            b.berechne(menge,c,true)
            val neu=c.copy(sampling=c.sampling.copy(bildBreite=801.0))
            val erwartet=VisualisierungsSampler.sample(menge,neu)
            assertEquals(erwartet,b.berechne(menge,neu,true))
            assertTrue(b.probenTreffer>0)
            val treffer=b.probenTreffer
            b.berechne(graph(Sinus(multiplikation(RationaleZahl.von(2),x))),neu,true)
            assertEquals(treffer,b.probenTreffer)
        } finally {b.schließen()}
    }
    @Test fun interaktionsQualitaetBeruecksichtigtDreidimensionalenZoom() {
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3,kamera=KameraZustand(0.0,0.0,0.0,0.0,0.0,0.0,10.0))
        assertEquals(.1,c.automatischeDarstellung().sampling.zielFehlerPixel)
        assertEquals(.3,c.copy(sampling=c.sampling.copy(interaktion=true)).automatischeDarstellung().sampling.zielFehlerPixel)
    }
    @Test fun produktIteratorKannAusMillionenKombinationenNurDenAnfangAnfordern() {
        val d=NumerischeDomäne((0 until 1000).map(Int::toDouble),false)
        assertEquals(listOf(listOf(0.0,0.0),listOf(0.0,1.0),listOf(0.0,2.0)),kartesischeStützwerte(listOf(d,d)).take(3).toList())
    }
    @Test fun fachlichUnbekanntIstKeinTechnischerAbbruch() {
        val e=VisualisierungsErgebnis.Teilweise(emptyList(),listOf("Die Mitgliedschaft ist noch nicht bewiesen."))
        assertEquals(VerfeinerungsPhase.Fertig,phaseFür(e))
    }

    @Test fun dreidimensionaleInteraktionsGeometrieDecktDasGesamteParameterfensterAb() {
        val b=ZahlenBereich(-1.0,1.0)
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3).automatischeDarstellung().let {
            it.copy(sampling=it.sampling.copy(bildBreite=800.0,bildHöhe=600.0))
        }
        val e=AdaptiveMethodenGeometrie.sample(listOf(b,b),c,{ a -> AdaptivePunktAuswertung(VisualisierungsPunkt(a[0],a[1],10*(a[0]*a[0]+a[1]*a[1]))) },{})
        assertTrue(e.interaktionsDreiecke.isNotEmpty())
        assertTrue(e.interaktionsDreiecke.size<=1024)
        fun fläche(l:List<VisualisierungsDreieck>)=l.sumOf { d -> kotlin.math.abs((d.b.x-d.a.x)*(d.c.y-d.a.y)-(d.c.x-d.a.x)*(d.b.y-d.a.y))/2 }
        assertEquals(4.0,fläche(e.interaktionsDreiecke),1e-9)
        assertEquals(4.0,fläche(e.dreiecke),1e-9)
    }
    @Test fun spaetEntdeckteDefinitionslueckeDeaktiviertVeralteteInteraktionsFlaeche() {
        val b=ZahlenBereich(-1.0,1.0)
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3).automatischeDarstellung().let {
            it.copy(sampling=it.sampling.copy(bildBreite=800.0,bildHöhe=600.0))
        }
        var entdeckt=false
        val e=AdaptiveMethodenGeometrie.sample(listOf(b,b),c,{ a ->
            if(a[0]==.03125 && a[1]==.03125) { entdeckt=true;AdaptivePunktAuswertung(null,"Definitionslücke") }
            else AdaptivePunktAuswertung(VisualisierungsPunkt(a[0],a[1],10*(a[0]*a[0]+a[1]*a[1])))
        },{})
        assertTrue(entdeckt);assertTrue(e.dreiecke.isNotEmpty())
        assertTrue(e.interaktionsDreiecke.isEmpty())
        assertTrue("Definitionslücke" in e.diagnosen)
    }
    @Test fun vereinigungBehaeltAlleTeilflaechenInDerInteraktion() {
        fun d(z:Double)=VisualisierungsDreieck(VisualisierungsPunkt(0.0,0.0,z),VisualisierungsPunkt(1.0,0.0,z),VisualisierungsPunkt(0.0,1.0,z))
        val a=d(0.0);val b=d(1.0);val lod=d(.5)
        val e=vereinigeVisualisierungsErgebnisse(listOf(
            VisualisierungsErgebnis.Erfolgreich(emptyList(),dreiecke=listOf(a),interaktionsDreiecke=listOf(lod)),
            VisualisierungsErgebnis.Erfolgreich(emptyList(),dreiecke=listOf(b))))
        assertEquals(listOf(lod,b),assertIs<VisualisierungsErgebnis.Erfolgreich>(e).interaktionsDreiecke)
    }

    @Test fun echterMethodengraphErhaeltInteraktionsGeometrieDurchDenGesamtenSampler()=runBlocking {
        val x=Variable("x");val y=Variable("y")
        val menge=Methode(name="f",parameter=listOf(x,y),vorschrift=addition(multiplikation(x,x),multiplikation(y,y)),zielMenge=ReelleZahlen,
            werteVorräte=mapOf("x" to ReelleZahlen,"y" to ReelleZahlen)).graphMenge()
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3,dimensionsModus=DimensionsModus.Manuell).automatischeDarstellung().let {
            it.copy(sampling=it.sampling.copy(bildBreite=600.0,bildHöhe=400.0))
        }
        val b=VisualisierungsBerechnung()
        try {
            val e=assertIs<VisualisierungsErgebnis.Erfolgreich>(b.berechne(menge,c,true))
            assertTrue(e.interaktionsDreiecke.isNotEmpty())
            assertTrue(e.interaktionsDreiecke.size<=1024)
            assertTrue(e.interaktionsDreiecke.size<e.dreiecke.size)
        } finally {b.schließen()}
    }

    @Test fun speicherbegrenzteFlaechenVorschauBehaeltDieZusammenhaengendeInteraktionsGeometrie()=runBlocking {
        val x=Variable("x");val y=Variable("y")
        val menge=Methode(name="f",parameter=listOf(x,y),vorschrift=addition(multiplikation(x,x),multiplikation(y,y)),zielMenge=ReelleZahlen,
            werteVorräte=mapOf("x" to ReelleZahlen,"y" to ReelleZahlen)).graphMenge()
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3,dimensionsModus=DimensionsModus.Manuell).automatischeDarstellung().let {
            it.copy(sampling=it.sampling.copy(bildBreite=600.0,bildHöhe=400.0,sichtBereiche=VisualisierungsTransformation(it,600.0,400.0).sichtBereiche()))
        }
        val b=VisualisierungsBerechnung(arbeitsSpeicher=VisualisierungsSpeicher(1_000_000))
        try {
            val e=assertIs<VisualisierungsErgebnis.Teilweise>(b.berechne(menge,c,true))
            assertTrue(e.hinweise.any {"Speichergrenze" in it})
            assertTrue(e.interaktionsDreiecke.isNotEmpty())
            assertTrue(e.interaktionsDreiecke.size<=1024)
        } finally {b.schließen()}
    }

    @Test fun neueLueckeNachLodSnapshotUndSpeicherabbruchVerwirftVeralteteFlaechen() {
        val speicher=VisualisierungsSpeicher(10_000)
        var letzter:VisualisierungsErgebnis?=null
        val sitzung=SamplingSitzung(VisualisierungsRechenZeit(),{0},{},{letzter=it},uhr={0},speicherDienst=speicher)
        val dreieck=VisualisierungsDreieck(VisualisierungsPunkt(0.0,0.0),VisualisierungsPunkt(1.0,0.0),VisualisierungsPunkt(0.0,1.0))
        try {
            SamplingSitzung.mit(sitzung) {
                samplingSpeicher(100)
                samplingZwischenstand(true) { VisualisierungsErgebnis.Teilweise(emptyList(),listOf("Vorschau"),
                    dreiecke=listOf(dreieck),interaktionsDreiecke=listOf(dreieck)) }
                assertFalse(sitzung.hatNeueDefinitionsLücken)
                sitzung.definitionsLücke()
                val grenze=assertFailsWith<VisualisierungsSpeicherGrenze> { samplingSpeicher(20_000) }
                val stand=assertIs<VisualisierungsErgebnis.Teilweise>(sitzung.begrenzterStand(letzter,grenze))
                assertTrue(stand.dreiecke.isEmpty());assertTrue(stand.interaktionsDreiecke.isEmpty())
                assertTrue(stand.hinweise.any {"Definitionslücken" in it})
                assertTrue(stand.hinweise.any {"Speichergrenze" in it})
            }
        } finally {sitzung.close()}
        assertEquals(0,speicher.bytes)
    }

}
