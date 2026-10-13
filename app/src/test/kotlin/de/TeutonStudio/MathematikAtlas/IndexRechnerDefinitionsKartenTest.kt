package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import de.TeutonStudio.KnotenKartenVerwalter.logik.*

class IndexRechnerDefinitionsKartenTest {
    private val laufzeit = MathematikKartenLaufzeit()
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun m(a: Long, b: Long, c: Long, d: Long) = Matrix(listOf(listOf(z(a), z(b)), listOf(z(c), z(d))))
    private fun wert(original: KnotenDaten, eingänge: Map<String, MathematischesObjekt>, quellen: List<VariablenQuelle> = emptyList()): MathematischesObjekt {
        val erzeugt = assertNotNull(indexRechnerDefinitionsKarte(original))
        val karte = KartenJson.lese(KartenJson.schreibe(erzeugt))
        erzeugt.knoten.zip(karte.knoten).forEach { (a,b) -> assertEquals(a, b, "Roundtrip ${a.name}") }
        assertTrue(karte.knoten.none { it.art == KonzeptKnotenArten.REGEL })
        assertTrue(karte.knoten.none { it.art == original.art && it.parameter[RechnerOperatorRegistry.familieFür(original.art)!!.operatorParameter] == original.parameter[RechnerOperatorRegistry.familieFür(original.art)!!.operatorParameter] })
        val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))
        karte.verbindungen.forEach { v ->
            assertEquals(VerbindungsPrüfung.Erlaubt, prüfung.prüfe(karte.copy(verbindungen = karte.verbindungen - v), v.von, v.zu), v.toString())
        }
        val vorgaben = karte.knoten.filter { it.art == KonzeptKnotenArten.EINGANG }.associate { k ->
            k.id to mapOf("wert" to BedingterWert(eingänge.getValue(k.parameter.getValue("rolle")),variablenQuellen=quellen))
        }
        val ergebnis = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(karte, vorgaben)
        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.toString())
        karte.knoten.filter { it.parameter["indexmethode.rollenKontext"]=="true" }.forEach { k ->
            val m=ergebnis.knoten.getValue(k.id).ausgaben.values.single().objekt as? Methode
            assertTrue(m?.parameter.orEmpty().all { "::" !in it.zuLatex() },"Interne Indexrollen dürfen nicht im LaTeX erscheinen")
        }
        val outputs = karte.knoten.filter { it.art == "mathematik.kartenAusgang" }
        val werte=outputs.map { ergebnis.knoten.getValue(it.id).eingänge.values.single().objekt as MathematischesObjekt }
        val wert=if(werte.size==1) werte.single() else Tupel(werte)
        val direkt=GesamterMathematikAuswerter.erzeugeRegister().finde(original.art)!!.auswerten(KnotenAuswertungsKontext(original,eingänge.mapValues { BedingterWert(it.value,variablenQuellen=quellen) },RechenKontext()))
        assertNull(direkt.fehler,"Direktauswertung: ${direkt.fehler}")
        val ports=original.anschlüsse.filter { it.richtung==AnschlussRichtung.Ausgang }.sortedBy { it.reihenfolge }
        val direkteWerte=ports.map { direkt.ausgaben.getValue(it.name).objekt as MathematischesObjekt }
        fachlichGleich(if(direkteWerte.size==1) direkteWerte.single() else Tupel(direkteWerte),wert)
        return wert
    }
    private fun fachlichGleich(direkt:MathematischesObjekt,graph:MathematischesObjekt) {
        when(direkt) {
            is ZahlAusdruck -> {
                assertIs<ZahlAusdruck>(graph)
                val a=DomaenenAuswerter.wert(vereinfache(direkt));val b=DomaenenAuswerter.wert(vereinfache(graph))
                if(a is DomaenenErgebnis.Wert && b is DomaenenErgebnis.Wert) {
                    val ar=a.wert.alsReelleKoordinate();val br=b.wert.alsReelleKoordinate()
                    if(ar is ReelleKoordinateErgebnis.Wert && br is ReelleKoordinateErgebnis.Wert) assertEquals(ar.wert.toDouble(),br.wert.toDouble(),1e-9)
                    else assertEquals(a,b)
                } else {
                    assertEquals(a::class,b::class,"Symbolische Auswertung muss denselben Unterstützungsstatus besitzen")
                    assertEquals(direkt.enthalteneMethodenParameter().map { it.name }.toSet(),graph.enthalteneMethodenParameter().map { it.name }.toSet())
                }
            }
            is Methode -> {
                assertIs<Methode>(graph)
                assertEquals(direkt.parameter.size,graph.parameter.size)
                assertEquals(direkt.zielMenge,graph.zielMenge)
                assertEquals(direkt.effektiverWerteVorrat,graph.effektiverWerteVorrat)
                assertEquals(direkt.werteVorräte.values.toList(),graph.werteVorräte.values.toList())
                for(x in if(direkt.werteVorräte.values.any { it is EndlicheIndexMenge }) emptyList() else listOf(z(-2),z(0),z(1),z(3))) fachlichGleich(direkt.wendeAn(listOf(x)),graph.wendeAn(listOf(x)))
            }
            is Tupel -> { assertIs<Tupel>(graph);assertEquals(direkt.elemente.size,graph.elemente.size);direkt.elemente.zip(graph.elemente).forEach { (a,b)->fachlichGleich(a,b) } }
            is OrientierterVektor -> { assertEquals(direkt::class,graph::class);graph as OrientierterVektor;assertEquals(direkt.werte.size,graph.werte.size);direkt.werte.zip(graph.werte).forEach { (a,b)->fachlichGleich(a,b) } }
            is Tensorartig -> { assertEquals(direkt::class,graph::class);graph as Tensorartig;assertEquals(direkt.tensorForm,graph.tensorForm);direkt.tensorForm.fold(listOf(emptyList<Int>())) { indices,n -> indices.flatMap { prefix -> (0 until n).map { prefix+it } } }.forEach { fachlichGleich(direkt.tensorKomponente(it),graph.tensorKomponente(it)) } }
            else -> assertEquals(direkt,graph)
        }
    }
    @Test fun `Tensor Konstruktion und Reshape bilden zeilenmajore Indexabbildungen`() {
        for(id in listOf("tensor.konstruktion","tensor.reshape")) {
            val basis=RechnerOperatorRegistry.konfiguriere(StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero),id)
            val op=basis.copy(parameter=basis.parameter+(TENSOR_OPERATION_PARAMETER to "2,3"))
            val quelle=if(id=="tensor.konstruktion") Tupel((1L..6).map(::z)) else Tensor(listOf(3,2),(1L..6).map(::z))
            val inputs=op.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.associate { it.name to quelle }
            assertEquals(Tensor(listOf(2,3),(1L..6).map(::z)),wert(op,inputs))
            assertFails { wert(op.copy(parameter=op.parameter+(TENSOR_OPERATION_PARAMETER to "2,4")),inputs) }
        }
    }
    @Test fun `Tensorrechner Determinante und Inverse akzeptieren echte Tensorquellen`() {
        val tensor=Tensor(listOf(2,2),listOf(z(1),z(2),z(3),z(5)))
        for(id in listOf("matrix.determinante","matrix.inverse")) {
            val op=RechnerOperatorRegistry.konfiguriere(StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero),id)
            wert(op,op.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.associate { it.name to tensor })
        }
    }
    @Test fun `Rang verwendet echte Minorprodukte und Maximum der nichtverschwindenden Grade`() {
        val basis=StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val op=konfiguriereStrukturRechner(basis,de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,"matrix.rang")
        for(a in listOf(Matrix(listOf(listOf(RationaleZahl.von(java.math.BigInteger.ONE,java.math.BigInteger.TEN.pow(100))))),m(1,2,2,4),m(1,2,3,4),m(0,0,0,0),Matrix(listOf(listOf(z(1),z(2),z(3)),listOf(z(2),z(4),z(6)))))) wert(op,mapOf("matrix" to a))
        wert(op,mapOf("matrix" to Matrix(listOf(List(10) { z((it+1).toLong()) },List(10) { z(2L*(it+1)) }))))
        wert(op,mapOf("matrix" to Matrix(listOf(List(10) { z((it+1).toLong()) },List(10) { z(if(it==0) 1 else 0) }))))
        val tensorOp=RechnerOperatorRegistry.konfiguriere(StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero),"matrix.rang")
        val tensor=Tensor(listOf(2,3),listOf(z(1),z(2),z(3),z(2),z(4),z(6)))
        wert(tensorOp,tensorOp.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.associate { it.name to tensor })
        assertFails { wert(op,mapOf("matrix" to Matrix(listOf(listOf(Variable("x")))))) }
        val gross=Matrix(List(5) { i -> List(5) { j -> if(i==j) z(1) else z(0) } })
        assertFails { wert(op,mapOf("matrix" to gross)) }
        val direkt=assertIs<MatrixRechnerErgebnis.ZahlWert>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.RANG,listOf(MatrixOperand("matrix",gross,FundamentalerZahlbereich.RATIONAL)))))
        assertEquals(z(5),direkt.wert)
    }
    @Test fun `Konkretes Zerlegen bildet jeden benannten Ausgang durch Indexabruf`() {
        val basis=konfiguriereVektorRechner(VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),VektorRechnerOperator.ZERLEGEN)
        val out=basis.anschlüsse.first { it.richtung==AnschlussRichtung.Ausgang }
        val node=basis.copy(anschlüsse=basis.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }+(0..2).map { i -> out.copy(id=AnschlussId("komponente-$i"),name="komponente${i+1}",reihenfolge=i,art=MathematikAnschlussArten.AtlasWert.id) })
        val source=Tupel(listOf(z(2),Tupel(listOf(z(3),z(4))),Variable("x")))
        assertEquals(source,wert(node,mapOf("struktur" to source)))
    }
    @Test fun `Symbolisches Zerlegen liefert eine Indexmethode mit symbolischem Definitionsbereich`() {
        val basis=konfiguriereVektorRechner(VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),VektorRechnerOperator.ZERLEGEN)
        val source=TypisiertesElement("v","mathematik.vektor.spalte")
        val graph=assertIs<Methode>(wert(basis,mapOf("struktur" to source)))
        assertEquals(1,graph.parameter.size)
    }
    @Test fun `Zusammenfuehren oeffnet genau eine Ebene und respektiert die Orientierung`() {
        val basis=konfiguriereVektorRechner(VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),VektorRechnerOperator.ZUSAMMENFUEHREN)
        for(ausgabe in VektorStrukturAusgabe.entries) {
            val node=basis.copy(parameter=basis.parameter+(VEKTOR_RECHNER_STRUKTUR_AUSGABE to ausgabe.stabileId))
            val ports=node.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }
            wert(node,ports.mapIndexed { i,p -> p.name to if(i==0) Tupel(listOf(z(1),z(2))) else SpaltenVektor(listOf(z(3),z(4))) }.toMap())
        }
        val nested=Tupel(listOf(Tupel(listOf(z(1),z(2)))))
        val port=basis.anschlüsse.first { it.richtung==AnschlussRichtung.Eingang }
        val einzel=basis.copy(anschlüsse=basis.anschlüsse.filter { it.richtung==AnschlussRichtung.Ausgang || it.id==port.id })
        assertEquals(nested,wert(einzel,mapOf(port.name to nested)))
    }
    @Test fun `Koordinatenoperatoren verwenden verschachtelte Indexabrufe und erhalten R1`() {
        val basis=VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        fun op(id:String)=konfiguriereVektorRechner(basis,VektorRechnerOperator.entries.single { it.stabileId==id })
        for(quelle in listOf(Tupel(listOf(z(-2))),Tupel(listOf(z(3),z(4))),Tupel(listOf(z(1),z(2),z(3))))) {
            wert(op("vektor.polarwinkel"),mapOf("kartesisch" to quelle))
            val polar=Polarkoordinaten.polar(quelle)
            wert(op("vektor.kartesisch"),mapOf("radius" to polar.elemente[0],"winkel" to polar.elemente[1]))
        }
    }
    @Test fun `Matrixaddition und Produkt sind echte unterschiedliche Indexgraphen`() {
        val a = m(1,2,3,4); val b = m(5,6,7,8)
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val addition = konfiguriereStrukturRechner(basis, de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX, "matrix.addition")
        val produkt = konfiguriereStrukturRechner(basis, de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX, "matrix.produkt")
        assertEquals(m(6,8,10,12), wert(addition, mapOf("links" to a, "rechts" to b, "a" to a, "b" to b)))
        assertEquals(m(19,22,43,50), wert(produkt, mapOf("links" to a, "rechts" to b, "a" to a, "b" to b)))
    }
    @Test fun `Matrixkette verwendet auch dynamisch erzeugten dritten Faktor in Reihenfolge`() {
        val basis = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero), de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX, "matrix.produkt")
        val rechts = basis.anschlüsse.single { it.name == "rechts" }
        val kette = basis.copy(anschlüsse = basis.anschlüsse + rechts.copy(id = AnschlussId("dritter"), name = "dritter", reihenfolge = 2, dynamischErzeugt = true))
        assertEquals(m(22,19,50,43), wert(kette, mapOf("links" to m(1,2,3,4), "rechts" to m(5,6,7,8), "dritter" to m(0,1,1,0))))
    }
    @Test fun `Spur Transposition und Diagonale werden durch Indexargumente definiert`() {
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        fun op(id: String) = konfiguriereStrukturRechner(basis, de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX, id)
        val input = mapOf("a" to m(1,2,3,4), "matrix" to m(1,2,3,4))
        assertEquals(z(5), wert(op("matrix.spur"), input))
        assertEquals(m(1,3,2,4), wert(op("matrix.transponieren"), input))
        assertEquals(Tupel(listOf(z(1), z(4))), wert(op("matrix.hauptdiagonale"), input))
        val rechteck = Matrix(listOf(listOf(z(1),z(2),z(3)),listOf(z(4),z(5),z(6))))
        val rechteckInput = mapOf("a" to rechteck,"matrix" to rechteck)
        assertEquals(Tupel(listOf(z(1),z(5))),wert(op("matrix.hauptdiagonale"),rechteckInput))
        assertEquals(Tupel(listOf(z(3),z(5))),wert(op("matrix.nebendiagonale"),rechteckInput))
    }
    @Test fun `Tensorkontraktion summiert nur gewählte Achsen mit dynamischem Achsentupel`() {
        val basis = konfiguriereTensorOperation(StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero), StandardTensorOperationen.registry.definition("tensor.kontraktion")!!)
        val tensor = Tensor(listOf(2,2,2), (1L..8L).map(::z))
        assertEquals(Tensor(listOf(2), listOf(z(7), z(11))), wert(basis, mapOf("tensor" to tensor, "achsen" to Tupel(listOf(z(1),z(3))))))
    }

    @Test fun `Skalarprodukt konjugiert komplexe Komponenten auch bei Standardkonfiguration`() {
        val basis = konfiguriereVektorRechner(StrukturFormelRechnerVorlagen.Vektor.erzeuge(GraphPunkt.Zero), VektorRechnerOperator.SKALARPRODUKT)
        val i = SpaltenVektor(listOf(KomplexeZahl(z(0),z(1))))
        val eingänge = basis.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.associate { it.name to i }
        val numerisch=assertIs<DomaenenErgebnis.Wert>(DomaenenAuswerter.wert(wert(basis,eingänge) as ZahlAusdruck))
        val real=assertIs<ReelleKoordinateErgebnis.Wert>(numerisch.wert.alsReelleKoordinate())
        assertEquals(0,real.wert.compareTo(java.math.BigDecimal.ONE))
    }
    @Test fun `Distanzmetrik und Projektionsorientierung bleiben gespeichert wirksam`() {
        val basis = StrukturFormelRechnerVorlagen.Vektor.erzeuge(GraphPunkt.Zero)
        val a = SpaltenVektor(listOf(z(1),z(2))); val c = ZeilenVektor(listOf(z(4),z(6)))
        for ((metrik, erwartet) in listOf("metrik.l1" to 7L, "metrik.linf" to 4L, "metrik.l2" to 5L)) {
            val knoten = konfiguriereVektorRechner(basis,VektorRechnerOperator.DISTANZ).let { it.copy(parameter=it.parameter + (VEKTOR_RECHNER_METRIK to metrik)) }
            val ports=knoten.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
            assertEquals(z(erwartet),vereinfache(wert(knoten,mapOf(ports[0].name to a,ports[1].name to c)) as ZahlAusdruck))
        }
        val projektion=konfiguriereVektorRechner(basis,VektorRechnerOperator.PROJEKTION)
        val ports=projektion.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        val ziel=ZeilenVektor(listOf(z(1),z(0)))
        assertEquals(ziel,wert(projektion,mapOf(ports[0].name to a,ports[1].name to ziel)))
    }

    @Test fun `Leibniz und Inverse enthalten echte Indexprodukte und Summen`() {
        val basis=StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        fun op(id:String)=konfiguriereStrukturRechner(basis,de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,id)
        val a=m(1,2,3,4)
        assertEquals(z(-2),vereinfache(wert(op("matrix.determinante"),mapOf("a" to a,"matrix" to a)) as ZahlAusdruck))
        val invers=assertIs<Matrix>(wert(op("matrix.inverse"),mapOf("a" to a,"matrix" to a)))
        assertEquals(m(-2,1,0,0).zeilen[0],invers.zeilen[0].map(::vereinfache))
        assertEquals(listOf(RationaleZahl.von(3,2),RationaleZahl.von(-1,2)),invers.zeilen[1].map(::vereinfache))
    }

    @Test fun `Komponentenweise Division hat eigenen Vertrag und ausführbare Indexdefinition`() {
        val basis=StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val k=konfiguriereStrukturRechner(basis,de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,"matrix.hadamardDivision")
        val result=assertIs<Matrix>(wert(k,mapOf("links" to m(2,6,12,20),"rechts" to m(2,3,4,5))))
        assertEquals(m(1,2,3,4),Matrix(result.zeilen.map { row -> row.map(::vereinfache) }))
        assertFails { wert(k,mapOf("links" to m(2,6,12,20),"rechts" to m(0,3,4,5))) }
        val inverse=konfiguriereStrukturRechner(basis,de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,"matrix.inverse")
        assertFails { wert(inverse,mapOf("a" to m(1,2,2,4),"matrix" to m(1,2,2,4))) }
        val a=Matrix(listOf(listOf(Variable("a"))))
        val ein=assertIs<Matrix>(wert(inverse,mapOf("a" to a,"matrix" to a)))
        assertEquals("\\frac{1}{a}",vereinfache(ein.zeilen[0][0]).zuLatex())
        assertFails { wert(inverse,mapOf("a" to Matrix(listOf(listOf(z(0)))),"matrix" to Matrix(listOf(listOf(z(0)))))) }
    }

    @Test fun `Kreuzprodukt Nebendiagonale und Matrixvektor verwenden Komponentenabbildungen`() {
        val v=konfiguriereVektorRechner(StrukturFormelRechnerVorlagen.Vektor.erzeuge(GraphPunkt.Zero),VektorRechnerOperator.KREUZPRODUKT)
        val e1=SpaltenVektor(listOf(z(1),z(0),z(0)));val e2=SpaltenVektor(listOf(z(0),z(1),z(0)))
        assertEquals(SpaltenVektor(listOf(z(0),z(0),z(1))),wert(v,mapOf("links" to e1,"rechts" to e2)))
        val basis=StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        fun op(id:String)=konfiguriereStrukturRechner(basis,de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,id)
        assertEquals(Tupel(listOf(z(2),z(3))),wert(op("matrix.nebendiagonale"),mapOf("a" to m(1,2,3,4),"matrix" to m(1,2,3,4))))
        val k=op("matrix.vektorProdukt")
        val ports=k.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        assertEquals(SpaltenVektor(listOf(z(5),z(11))),wert(k,mapOf(ports[0].name to m(1,2,3,4),ports[1].name to ZeilenVektor(listOf(z(1),z(2))))))
    }
    @Test fun `Tensorindexbewertung und Schnitt bewahren bestehende nullbasierte Operationsparameter`() {
        fun op(id:String,param:String)=konfiguriereTensorOperation(StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero),StandardTensorOperationen.registry.definition(id)!!).let { it.copy(parameter=it.parameter+(TENSOR_OPERATION_PARAMETER to param)+(TENSOR_ACHSEN_SPEZIFIKATION to "2")) }
        val t=Tensor(listOf(2,2),listOf(z(1),z(2),z(3),z(4)))
        assertEquals(z(3),wert(op("tensor.indexauswertung","1,0"),mapOf("tensor" to t)))
        assertEquals(Tensor(listOf(2),listOf(z(2),z(4))),wert(op("tensor.achsenschnitt","1"),mapOf("tensor" to t,"achsen" to Tupel(listOf(z(2))))))
    }

    @Test fun `Zertifiziertes Skalarprodukt ruft die hinterlegte Methode auf`() {
        val u=Variable("u");val v=Variable("v")
        val raum=Vektorraum(VektorOrientierung.Spalte,1,ReelleZahlen)
        val methode=Methode("gewichtet",listOf(u,v),multiplikation(z(2),SymbolischeZahlKomponente(u,z(1)),SymbolischeZahlKomponente(v,z(1))),ReelleZahlen,mapOf("u" to raum,"v" to raum))
        val zeugnis=SkalarproduktZeugnis(methode,raum,FundamentalerZahlbereich.REELL,SkalarproduktLinearitaet.RECHTSLINEAR,SKALARPRODUKT_ZERTIFIKAT_VERSION,setOf("linear","symmetrisch","positiv"))
        val aussage=BegriffsAussage(BegriffsPruefung(SKALARPRODUKT_BEGRIFF_ID,"Skalarprodukt",emptyList(),NachweisStatus.Nachgewiesen,zeugnis=zeugnis),"gewichtet")
        val zertifikat=Methode("Zertifikat",emptyList(),aussage,WahrheitsMenge)
        val basis=konfiguriereVektorRechner(VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),VektorRechnerOperator.SKALARPRODUKT)
        val original=basis.copy(parameter=basis.parameter+(SKALARPRODUKT_DEFINITION_PARAMETER to SKALARPRODUKT_DEFINITION_ZERTIFIZIERT),anschlüsse=basis.anschlüsse+AnschlussDaten(name=SKALARPRODUKT_DEFINITION_EINGANG,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Methode.id,reihenfolge=100))
        assertEquals(z(12),vereinfache(wert(original,mapOf("links" to SpaltenVektor(listOf(z(2))),"rechts" to SpaltenVektor(listOf(z(3))),"definition" to zertifikat)) as ZahlAusdruck))
    }

    @Test fun `Indexrollen bleiben von symbolischen Operandenparametern getrennt`() {
        val basis=StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        fun op(id:String)=konfiguriereStrukturRechner(basis,de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,id)
        for(name in listOf("i","k","p","__index::k","__index_::p")) {
            val a=Matrix(listOf(listOf(Variable(name))))
            val eins=Matrix(listOf(listOf(z(1))))
            assertEquals(a,wert(op("matrix.produkt"),mapOf("links" to a,"rechts" to eins)))
            assertEquals(Variable(name),vereinfache(wert(op("matrix.determinante"),mapOf("a" to a,"matrix" to a)) as ZahlAusdruck))
        }
    }

    @Test fun `Charakteristisches Polynom wird als Leibnizmethodengraph ausgeführt`() {
        val op=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,"matrix.charakteristischesPolynom")
        val a=m(1,2,3,4)
        val methode=wert(op,mapOf("a" to a,"matrix" to a)) as Methode
        assertEquals(z(-2),vereinfache(methode.wendeAn(listOf(z(0))) as ZahlAusdruck))
    }

    @Test fun `Registerinventar macht fehlende Indexrezepte explizit`() {
        val fehlend=RechnerOperatorRegistry.alle.filter { it.familie!=RechnerFamilie.ZAHL && !it.operatorId.endsWith(".formel") }.mapNotNull { vertrag ->
            val basis=when(vertrag.familie) {
                RechnerFamilie.MATRIX -> StrukturFormelRechnerVorlagen.Matrix
                RechnerFamilie.VEKTOR -> VektorRechnerKnotenVorlagen.standard
                RechnerFamilie.TENSOR -> StrukturRechnerKnotenVorlagen.Tensorrechner
                else -> error("Keine Strukturfamilie")
            }
            val original=RechnerOperatorRegistry.konfiguriere(basis.erzeuge(GraphPunkt.Zero),vertrag.operatorId)
            val karte=indexRechnerDefinitionsKarte(original)
            if(karte==null) "${vertrag.familie}:${vertrag.operatorId}" else {
                assertTrue(karte.knoten.none { it.art==KonzeptKnotenArten.REGEL })
                assertTrue(karte.knoten.none { it.art==original.art && it.parameter[vertrag.familie.operatorParameter]==vertrag.operatorId })
                null
            }
        }.toSet()
        assertEquals(emptySet(), fehlend, "Jeder registrierte Strukturoperator benötigt ein ausführbares Rezept.")
    }

    @Test fun `Inverse lehnt nichtkommutative Quellmetadaten in Kernadapter und Definition ab`() {
        for(operator in listOf("matrix.inverse","matrix.determinante")) {
        val op=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,operator)
        val matrix=Matrix(listOf(listOf(Variable("q"),z(0)),listOf(z(0),z(1))))
        val quellen=listOf(VariablenQuelle(KnotenId("q"),"q",FundamentalerZahlbereich.QUATERNION.alsMenge()))
        val direkt=GesamterMathematikAuswerter.erzeugeRegister().finde(op.art)!!.auswerten(KnotenAuswertungsKontext(op,mapOf("matrix" to BedingterWert(matrix,variablenQuellen=quellen)),RechenKontext()))
        assertNotNull(direkt.fehler)
        assertTrue(direkt.fehler.toString().contains("kommutativ"))
        assertFails { wert(op,mapOf("matrix" to matrix),quellen) }
        }
    }
    @Test fun `Matrixpotenz faltet geordnet mit neutraler Identitaet und bedingter Inverser`() {
        val op=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,"matrix.potenz")
        for(n in listOf(0L,1L,2L,3L,-1L,-2L)) {
            val a=m(1,1,0,1)
            val result=wert(op,mapOf("matrix" to a,"exponent" to z(n))) as Matrix
            assertEquals(m(1,n,0,1),Matrix(result.zeilen.map { it.map(::vereinfache) }))
        }
        assertEquals(m(1,0,0,1),wert(op,mapOf("matrix" to m(1,2,2,4),"exponent" to z(0))))
        val gross=Matrix(List(7) { i -> List(7) { j -> if(i==j) z(1) else z(0) } })
        for(n in listOf(0L,1L,2L)) assertEquals(gross,wert(op,mapOf("matrix" to gross,"exponent" to z(n))))
        val hquelle=listOf(VariablenQuelle(KnotenId("quaternionQuelle"),"q",FundamentalerZahlbereich.QUATERNION.alsMenge()))
        for(n in listOf(0L,1L,2L)) wert(op,mapOf("matrix" to Matrix(listOf(listOf(Variable("q")))),"exponent" to z(n)),hquelle)
        assertFails { wert(op,mapOf("matrix" to Matrix(listOf(listOf(Variable("q")))),"exponent" to z(-1)),hquelle) }
        assertFails { wert(op,mapOf("matrix" to Matrix(listOf(listOf(Variable("x")))),"exponent" to z(-1))) }
        assertFails { wert(op,mapOf("matrix" to m(1,2,2,4),"exponent" to z(-1))) }
    }

    @Test fun `Vektorpotenzen null bis vier verwenden Paritaet und Komponenten`() {
        for (p in 0L..4L) {
            val op = konfiguriereVektorRechner(VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), VektorRechnerOperator.POTENZ)
                .let { it.copy(parameter = it.parameter + (VEKTOR_POTENZ_EXPONENT to p.toString())) }
            val inputs = op.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.associate {
                it.name to if (it.name == "p") z(p) else SpaltenVektor(listOf(z(1), z(2)))
            }
            val w = wert(op, inputs)
            if (p % 2 == 0L) assertEquals(z(if (p == 0L) 1 else if (p == 2L) 5 else 25), vereinfache(w as ZahlAusdruck))
            else assertEquals(SpaltenVektor(if (p == 1L) listOf(z(1), z(2)) else listOf(z(5), z(10))), w)
        }
    }
    @Test fun `Minimalpolynom waehlt den kleinsten loesbaren Grad`() {
        val op = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero), de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX, "matrix.minimalpolynom")
        for (a in listOf(m(1,0,0,1), m(1,1,0,1), m(0,0,0,0))) {
            val result = wert(op, mapOf("matrix" to a, "a" to a)) as Methode
            for (x in 0L..2L) {
                val erwartet = if (a == m(1,1,0,1)) (x-1)*(x-1) else if (a == m(0,0,0,0)) x else x-1
                assertEquals(z(erwartet), vereinfache(result.wendeAn(listOf(z(x))) as ZahlAusdruck))
            }
        }
    }
    @Test fun `Diagonalmatrixrezept akzeptiert Tupel Zeile und Spalte`() {
        val op = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero), de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX, "matrix.diagonalmatrixErzeugen")
        val xs = listOf(z(2), z(3), z(4))
        for (quelle in listOf(Tupel(xs), ZeilenVektor(xs), SpaltenVektor(xs))) {
            assertEquals(Matrix(listOf(listOf(z(2),z(0),z(0)),listOf(z(0),z(3),z(0)),listOf(z(0),z(0),z(4)))), wert(op, mapOf("werte" to quelle)))
        }
    }
    @Test fun `Vektorfeldintegral verwendet bestehende skalare Integrale`() {
        val x = Variable("x")
        val feld = MathematischeMethode("F", listOf(x), Tupel(listOf(SpaltenVektor(listOf(z(1), z(2))))),
            Tupelraum(listOf(Vektorraum(VektorOrientierung.Spalte, 2, ReelleZahlen))), mapOf("x" to ReelleZahlen))
        val op = konfiguriereVektorRechner(VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), VektorRechnerOperator.VEKTORFELD_INTEGRIEREN)
        val inputs = op.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.associate { it.name to when(it.name) {
            "menge" -> ReellesIntervall(z(0), false, z(3), false)
            "mass" -> IntegralMass.StandardReell
            else -> feld
        } }
        assertEquals(Tupel(listOf(z(3), z(6))), wert(op, inputs))
    }
    private fun kandidaten(id: String, a: Matrix): MathematischeMethode {
        val original = RechnerOperatorRegistry.konfiguriere(StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero), id)
        val karte = assertNotNull(indexRechnerDefinitionsKarte(original))
        assertTrue(karte.knoten.none { it.art == original.art && it.parameter[TENSOR_OPERATION_ID] == id })
        val vorgaben = karte.knoten.filter { it.art == KonzeptKnotenArten.EINGANG }.associate { it.id to mapOf("wert" to BedingterWert(a)) }
        val ergebnis = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(karte, vorgaben)
        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.toString())
        val ausgang = karte.knoten.single { it.art == "mathematik.kartenAusgang" }
        assertEquals("bedingungen", ausgang.parameter["rolle"])
        return ergebnis.knoten.getValue(ausgang.id).eingänge.values.single().objekt as MathematischeMethode
    }
    private fun pruefeKandidat(m: MathematischeMethode, parameter: Map<String, MathematischesObjekt>, wahr: Boolean) {
        val aussage = m.wendeMathematischAn(parameter) as Aussage
        assertEquals(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge, aussage.entscheide().wahrheitswert, aussage.zuLatex())
    }
    @Test fun `QR SVD Eigen Jordan und Spektralgraphen pruefen richtige und falsche Kandidaten`() {
        val i = m(1,0,0,1); val d = m(2,0,0,3)
        val qr = kandidaten("matrix.qr", d)
        pruefeKandidat(qr, mapOf("Q" to i, "R" to d), true)
        pruefeKandidat(qr, mapOf("Q" to d, "R" to i), false)
        val svd = kandidaten("matrix.svd", m(-2,0,0,1))
        pruefeKandidat(svd, mapOf("U" to m(-1,0,0,1), "Sigma" to m(2,0,0,1), "V" to i), true)
        pruefeKandidat(svd, mapOf("U" to i, "Sigma" to m(-2,0,0,1), "V" to i), false)
        val eigen = kandidaten("matrix.eigen", m(1,1,0,1))
        pruefeKandidat(eigen, mapOf("V" to m(1,1,0,0), "Lambda" to i), true)
        pruefeKandidat(eigen, mapOf("V" to m(0,0,0,0), "Lambda" to i), false)
        val jordan = kandidaten("matrix.jordan", m(1,1,0,1))
        pruefeKandidat(jordan, mapOf("P" to i, "J" to m(1,1,0,1)), true)
        pruefeKandidat(jordan, mapOf("P" to m(0,0,0,0), "J" to i), false)
        val spektral = kandidaten("matrix.spektral", d)
        val ps = Tensor(listOf(2,2,2), listOf(z(1),z(0),z(0),z(0),z(0),z(0),z(0),z(1)))
        pruefeKandidat(spektral, mapOf("lambdas" to SpaltenVektor(listOf(z(2),z(3))), "Projektoren" to ps), true)
        pruefeKandidat(spektral, mapOf("lambdas" to SpaltenVektor(listOf(z(3),z(2))), "Projektoren" to ps), false)
    }


    @Test fun `Tensorzerlegung indiziert jede Scheibe ohne Abflachung`() {
        val t = Tensor(listOf(2,3,2), (1L..12L).map(::z))
        for (achse in 1..3) {
            val basis = konfiguriereTensorOperation(StrukturRechnerKnotenVorlagen.Tensorrechner.erzeuge(GraphPunkt.Zero), StandardTensorOperationen.registry.definition("tensor.zerlegen")!!)
            val ausgang = basis.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
            val op = basis.copy(parameter=basis.parameter+(TENSOR_ACHSEN_SPEZIFIKATION to achse.toString()), anschlüsse=basis.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang } + List(t.dimensionen[achse-1]) { i -> ausgang.copy(id=AnschlussId("schnitt-$i"),name="schnitt${i+1}",reihenfolge=i,art=MathematikAnschlussArten.AtlasWert.id) })
            val schnitte = wert(op, mapOf("tensor" to t, "achsen" to Tupel(listOf(z(achse.toLong()))))) as Tupel
            assertEquals(t.dimensionen[achse-1],schnitte.elemente.size)
            for (index in 1..t.dimensionen[achse-1]) {
                val methode = tensorZerlegeMethode(t,achse-1,t.dimensionen.filterIndexed { i, _ -> i != achse-1 }.map { z(it.toLong()) })
                assertEquals(methode.wendeAn(listOf(z(index.toLong()))),schnitte.elemente[index-1])
                assertFails { methode.wendeAn(listOf(z(0))) }
                assertFails { methode.wendeAn(listOf(z((t.dimensionen[achse-1]+1).toLong()))) }
            }
        }
    }
}
