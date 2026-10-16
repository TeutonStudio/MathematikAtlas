package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VisualisierungsKorrekturenTest {
    private val x = Variable("x")
    private fun methode() = Methode(name="f", parameter=listOf(x), vorschrift=multiplikation(x,x), zielMenge=ReelleZahlen, werteVorräte=mapOf("x" to ReelleZahlen))
    private fun punkte(e: VisualisierungsErgebnis) = when(e) {
        is VisualisierungsErgebnis.Erfolgreich -> e.punkte
        is VisualisierungsErgebnis.Teilweise -> e.punkte
        else -> error("Keine Geometrie: $e")
    }
    @Test fun graphUndBildmodusLiefernDieselbenFunktionsgraphPunkte() {
        val m=methode();val c=VisualisierungsKonfiguration()
        assertEquals(punkte(VisualisierungsSampler.sample(Abbild(ReelleZahlen,m),c)), punkte(VisualisierungsSampler.sample(m.graphMenge(),c)))
        assertEquals(RaumDimension.R2,empfohleneRaumDimension(m.graphMenge()))
    }
    @Test fun strukturierterGraphBehaeltEinGesamtArgument() {
        val t=TypisiertesElement("x", MathematikAnschlussArten.Tupel.id.wert)
        val m=Methode(name="f",parameter=listOf(t),vorschrift=RationaleZahl.Eins,zielMenge=ReelleZahlen,
            werteVorräte=mapOf("x" to KartesischesProdukt(listOf(ReelleZahlen,ReelleZahlen))))
        val c=VisualisierungsKonfiguration(dimension=RaumDimension.R3)
        val p=punkte(VisualisierungsSampler.sample(m.graphMenge(),c))
        assertTrue(p.isNotEmpty());assertTrue(p.all { it.z==1.0 });assertEquals(1,m.parameter.size)
    }
    @Test fun neueVisualisierungIstSenkeUndHatErweiterbareEingaenge() {
        val n=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero)
        assertEquals(2,n.anschlüsse.size);assertTrue(n.anschlüsse.all { it.richtung==AnschlussRichtung.Eingang && it.kannSichErweitern })
        val inputs=mapOf("menge" to BedingterWert(ReelleZahlen),"menge2" to BedingterWert(GanzeZahlen))
        val result=GesamterMathematikAuswerter.erzeugeRegister().finde(n.art)!!.auswerten(KnotenAuswertungsKontext(n,inputs,RechenKontext()))
        assertTrue(result.ausgaben.isEmpty());assertEquals(inputs,result.eingänge)
    }
    @Test fun historischeAusgangsverbindungWirdErstAlsAtomareAktionEntfernt() {
        val n=MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero)
        val out=AnschlussDaten(name="menge",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Menge.id)
        val alt=n.copy(anschlüsse=listOf(n.anschlüsse.first(),out));val ziel=n.copy(id=neueKnotenId())
        val v=VerbindungDaten(von=AnschlussVerweis(alt.id,out.id),zu=AnschlussVerweis(ziel.id,ziel.anschlüsse.first().id))
        val karte=KartenDaten(name="alt",knoten=listOf(alt,ziel),verbindungen=listOf(v))
        val kandidat=konfiguriereVisualisierungsEingänge(alt)
        assertEquals(listOf(v),karte.vorschauKnotenErsetzen(kandidat).entfallendeVerbindungen)
        val e=KartenEditorZustand(karte,MathematikKartenLaufzeit().graphPrüfung)
        e.führeAus(KartenAktion.KnotenErsetzen(kandidat));assertTrue(e.karte.verbindungen.isEmpty())
        e.rückgängig();assertEquals(karte,e.karte);e.wiederholen();assertTrue(e.karte.verbindungen.isEmpty())
    }
    @Test fun ebenenStileSindPersistierbarUndNichtTeilDerSamplingSignatur() {
        val a=VisualisierungsKonfiguration();val b=a.copy(ebenen=mapOf("menge2" to EbenenStil(false,"B",0xFFCC0000,.4,FüllArt.Hexagonal)))
        assertEquals(a.samplingSignatur(),b.samplingSignatur());assertEquals(b,VisualisierungsKonfiguration.aus(b.zuEigenschaften()))
        assertEquals(64000,a.copy(qualität=DarstellungsQualität.Hoch).detailBudget)
        assertEquals(8000,a.detailBudget)
    }
    @Test fun graphModusWechseltMitStabilerPortIdentitaet() {
        val n=MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt.Zero)
        val term=konfiguriereGraphEingang(n,true)
        assertEquals(n.anschlüsse.map { it.id },term.anschlüsse.map { it.id });assertEquals("term",term.anschlüsse.first().name)
        assertEquals(n.anschlüsse,konfiguriereGraphEingang(term,false).anschlüsse)
    }
    @Test fun latexMengenklammernUndDoppelstrichWerdenKorrektVorbereitet() {
        val text=atlasLatexQuelltext("\\mathbb R \\cup \\{x\\mid x>0\\}",false)
        assertTrue(text.contains("\\mathbb{R}"));assertTrue(text.contains("\\lbrace x"));assertTrue(text.contains("\\rbrace"))
        assertTrue(atlasLatexQuelltext("\\frac{x}{2}",false).contains("\\frac{x}{2}"))
    }
    @Test fun dimensionsmigrationEntferntNurUnverbundeneDuplikate() {
        val n=MengenKnotenVorlagen.vorlage(MengenKnotenAuswahl.GRADMENGE).erzeuge(GraphPunkt.Zero)
        val dim=n.anschlüsse.first().copy(id=neueAnschlussId(),name="dim")
        val alt=n.copy(anschlüsse=n.anschlüsse+dim)
        val quell=n.copy(id=neueKnotenId());val out=quell.anschlüsse.last()
        fun edge(port:AnschlussDaten)=VerbindungDaten(von=AnschlussVerweis(quell.id,out.id),zu=AnschlussVerweis(alt.id,port.id))
        val karte=KartenDaten(name="alt",knoten=listOf(alt,quell),verbindungen=listOf(edge(dim)))
        val migriert=karte.migriereGradmengenDimension()
        val port=migriert.knoten.first().anschlüsse.single {it.richtung==AnschlussRichtung.Eingang}
        assertEquals(dim.id,port.id);assertEquals("dimension",port.name);assertEquals(karte.verbindungen,migriert.verbindungen)
        val konflikt=karte.copy(verbindungen=karte.verbindungen+edge(n.anschlüsse.first()))
        assertEquals(alt.anschlüsse,konflikt.migriereGradmengenDimension().knoten.first().anschlüsse)
    }
    @Test fun zerlegenAnzeigeEnthaeltAlleAusgaengeInAnschlussreihenfolge() {
        val n=konfiguriereVektorRechner(KnotenDaten(art=VektorRechner.KNOTEN_ART,name="Zerlegen"),VektorRechnerOperator.ZERLEGEN)
        val ports=(1..2).map { AnschlussDaten(name="element$it",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Zahl.id,reihenfolge=it) }
        val r=KnotenAuswertungsErgebnis(mapOf("element2" to BedingterWert(Variable("x_2")),"element1" to BedingterWert(Variable("x_1"))))
        assertEquals("\\begin{pmatrix}x_1 & x_2\\end{pmatrix}",zerlegenAnzeigeLatex(n.copy(anschlüsse=ports),r))
    }

    @Test fun ungleichungsRandIstSeparatUndOffenGeschlossenUnterscheidbar() {
        val y=Variable("y")
        fun sample(art:VergleichsArt):List<VisualisierungsLinie> {
            val m=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)), Vergleich(y,art,multiplikation(x,x)))
            return when(val e=VisualisierungsSampler.sample(m,VisualisierungsKonfiguration())) {
                is VisualisierungsErgebnis.Erfolgreich -> e.linien
                is VisualisierungsErgebnis.Teilweise -> e.linien
                else -> error("Rand fehlt: $e")
            }
        }
        val offen=sample(VergleichsArt.Kleiner);val geschlossen=sample(VergleichsArt.KleinerGleich)
        assertTrue(offen.isNotEmpty());assertTrue(offen.all { it.offenerRand });assertTrue(geschlossen.none { it.offenerRand })
    }
    @Test fun flaecheMitIsoliertemPunktBewahrtDiskreteGeometrie() {
        val y=Variable("y")
        val region=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)), Vergleich(y,VergleichsArt.Kleiner,RationaleZahl.Null))
        val m=Vereinigung(listOf(region,EndlicheMenge(setOf(Tupel(listOf(RationaleZahl.von(3),RationaleZahl.von(7)))))))
        val p=punkte(VisualisierungsSampler.sample(m,VisualisierungsKonfiguration()))
        assertTrue(p.any {it.x==3.0 && it.y==7.0 && !it.rasterProbe})
    }
    @Test fun termModusZeigtBisherigeMethodenverbindungAlsVerlust() {
        val graph=MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt.Zero)
        val quelle=graph.copy(id=neueKnotenId())
        val v=VerbindungDaten(von=AnschlussVerweis(quelle.id,quelle.anschlüsse.first().copy().id),zu=AnschlussVerweis(graph.id,graph.anschlüsse.first().id))
        val source=quelle.copy(anschlüsse=listOf(quelle.anschlüsse.first().copy(richtung=AnschlussRichtung.Ausgang)))
        val karte=KartenDaten(name="Modus",knoten=listOf(graph,source),verbindungen=listOf(v))
        assertEquals(listOf(v),karte.vorschauKnotenErsetzen(konfiguriereGraphEingang(graph,true),MathematikKartenLaufzeit().graphPrüfung).entfallendeVerbindungen)
    }

    @Test fun graphTermmodusVerwendetDieselbeQuellenbindungWieUebersetzer() {
        val n=konfiguriereGraphEingang(MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt.Zero),true)
        val wert=BedingterWert(multiplikation(x,x),variablenQuellen=listOf(VariablenQuelle(KnotenId("quelle"),"x",ReelleZahlen)))
        val k=KnotenAuswertungsKontext(n,mapOf("term" to wert),RechenKontext())
        val r=GesamterMathematikAuswerter.erzeugeRegister().finde(n.art)!!.auswerten(k)
        val graph=assertIs<MethodenGraphMenge>(r.ausgaben.getValue("graph").objekt)
        assertEquals(termAlsMethode(k,wert).objekt,graph.methode)
        assertTrue(punkte(VisualisierungsSampler.sample(graph,VisualisierungsKonfiguration())).isNotEmpty())
    }
    @Test fun graphDefinitionSchaltetUebersetzerVorAlleMethodenVerbraucher() {
        val json=java.io.File("src/main/assets/de/TeutonStudio/MathematikKnoten/konzeptkarte/karte-methodengraph-definition-v7.json").readText()
        val karte=KartenDatenJson.lese(json)
        val graph=konfiguriereGraphEingang(MethodenGraphKnotenVorlagen.Graph.erzeuge(GraphPunkt.Zero),true)
        val definition=graphDefinitionsKarte(graph,karte)
        val translator=definition.knoten.single {it.art==ÜBERSETZER_ART}
        assertEquals("methode",übersetzerZiel(translator))
        assertEquals(1,definition.verbindungen.count {it.von.knotenId.wert=="graph-funktion"})
        assertEquals(3,definition.verbindungen.count {it.von.knotenId==translator.id})
        assertTrue(MathematikKartenLaufzeit().graphPrüfung.inkompatibleVerbindungen(definition).isEmpty())
    }

    @Test fun entarteteNullniveausErzeugenKeineErfundenenGebietsraender() {
        val y=Variable("y")
        for(b in listOf(Vergleich(x,VergleichsArt.Kleiner,x),Vergleich(x,VergleichsArt.KleinerGleich,x),Vergleich(multiplikation(x,x),VergleichsArt.Kleiner,RationaleZahl.Null))) {
            val m=DefinierteMenge(listOf(GebundeneMengenVariable(x,ReelleZahlen),GebundeneMengenVariable(y,ReelleZahlen)),b)
            val e=VisualisierungsSampler.sample(m,VisualisierungsKonfiguration())
            val linien=when(e) {is VisualisierungsErgebnis.Erfolgreich -> e.linien;is VisualisierungsErgebnis.Teilweise -> e.linien;else -> error("$e")}
            assertTrue(linien.isEmpty(),"Falscher Rand für $b")
        }
    }
}
