package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Ausführbare Zerlegung: Komponentenabbildungen, skalare Arithmetik, Indexsumme, Konstruktion. */
internal fun indexRechnerDefinitionsKarte(original: KnotenDaten): KartenDaten? {
    val familie = RechnerOperatorRegistry.familieFür(original.art) ?: return null
    val id = original.parameter[familie.operatorParameter] ?: return null
    val operation = id.substringAfter('.')
    if (operation !in setOf("addition", "subtraktion", "hadamard", "skalierung", "skalarmultiplikation", "negation", "produkt", "vektorProdukt", "skalarprodukt", "tensorprodukt", "norm", "radius", "normalisierung", "distanz", "kontraktion", "transponieren", "achsenpermutation", "achsenschnitt", "spur", "hauptdiagonale", "projektion", "winkel", "winkelZuAchse", "determinante", "inverse", "hadamardDivision", "nebendiagonale", "kreuzprodukt", "indexauswertung", "charakteristischesPolynom", "potenz", "polarwinkel", "kartesisch", "zusammenfuehren", "zerlegen", "rang", "konstruktion", "reshape")) return null
    if (familie !in setOf(RechnerFamilie.VEKTOR, RechnerFamilie.MATRIX, RechnerFamilie.TENSOR)) return null
    val b = IndexDefinitionsBauer(original, id)
    val eingänge = original.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
    val inputs = eingänge.map { it to b.eingang(it) }
    if (inputs.isEmpty()) return null
    fun index(i: Int, namen: String? = null, präfix: String? = null, matrix: Boolean = false) = b.index(inputs[i].second, namen, präfix, matrix)
    if(operation=="zerlegen" && familie==RechnerFamilie.VEKTOR) {
        val methode=b.index(inputs.first().second)
        original.anschlüsse.filter { it.richtung==AnschlussRichtung.Ausgang }.sortedBy { it.reihenfolge }.forEachIndexed { i,port ->
            val wert=if(port.name=="methode") methode else b.aufruf(methode,b.tupel(listOf(b.konstante((i+1).toLong()))))
            b.ausgang(wert,port.name)
        }
        return b.karte()
    }
    fun punktweise(op: UniversellerZahlenOperator): IndexDefinitionsWert {
        val werte = inputs.mapIndexed { i, (port, _) ->
            if (port.name in setOf("skalar", "faktor")) inputs[i].second else index(i)
        }
        return b.zahl(op, werte)
    }
    val strukturInputs = inputs.filter { it.first.name != "achsen" && !it.first.name.startsWith("achse.") }
    val achsenInput = inputs.firstOrNull { it.first.name == "achsen" }?.second
    fun achsenMethode(modus: String, standard: String = ""): IndexDefinitionsWert = b.indexAchsen(strukturInputs.first().second, modus,
        original.parameter[TENSOR_ACHSEN_SPEZIFIKATION] ?: original.parameter["achsen"] ?: original.parameter["permutation"] ?: standard, achsenInput, schnittIndex = (original.parameter[TENSOR_OPERATION_PARAMETER] ?: original.parameter["indizes"])?.toIntOrNull()?.plus(1))
    fun norm(a: IndexDefinitionsWert) = b.zahl(UniversellerZahlenOperator.QUADRATWURZEL, listOf(b.summe(b.zahl(UniversellerZahlenOperator.POTENZ, listOf(a, b.konstante(2))), "*")))
    fun dot(a: IndexDefinitionsWert, c: IndexDefinitionsWert) = b.summe(b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, listOf(a,c)), "*")
    fun determinant(quelle: IndexDefinitionsWert, minor: Boolean = false): IndexDefinitionsWert {
        val komponenten = b.auswahl(quelle, if (minor) "minor" else "permutationen")
        val produkte = b.summe(komponenten, "k", "produkt")
        val zeichen = b.auswahl(quelle, if (minor) "minorvorzeichen" else "vorzeichen")
        return b.summe(b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, listOf(produkte, zeichen)), "p", skalar = !minor)
    }
    fun matrixprodukt(a:IndexDefinitionsWert,c:IndexDefinitionsWert)=b.konstruiere(b.summe(b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(b.index(a,"i,k",matrix=true),b.index(c,"k,j",matrix=true))),"k"),"matrix")
    var skalar = false
    val ergebnis = when (operation) {
        "nebendiagonale" -> b.indexAchsen(inputs.first().second,"gegendiagonale","",null)
        "kreuzprodukt" -> {
            val a=inputs[0].second; val c=inputs[1].second
            b.zahl(UniversellerZahlenOperator.SUBTRAKTION,listOf(
                b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(b.auswahl(a,"zyklus3:1"),b.auswahl(c,"zyklus3:2"))),
                b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(b.auswahl(a,"zyklus3:2"),b.auswahl(c,"zyklus3:1")))))
        }
        "indexauswertung" -> {
            skalar=true
            val indizes=inputs.firstOrNull { it.first.name=="indizes" }?.second ?: b.tupel((original.parameter[TENSOR_OPERATION_PARAMETER] ?: original.parameter["indizes"]).orEmpty().split(',').filter { it.isNotBlank() }.map { b.konstante(it.trim().toLong()+1) })
            b.aufruf(index(0),indizes)
        }
        "konstruktion", "reshape" -> b.auswahl(inputs.first().second,"umformen")
        "rang" -> {
            skalar=true
            val quelle=inputs.first().second
            val produkte=b.summe(b.auswahl(quelle,"minoren:komponenten"),"k","produkt")
            val det=b.summe(b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(produkte,b.auswahl(quelle,"minoren:vorzeichen"))),"p")
            val nichtnull=b.auswahl(det,"nichtnullIndikator")
            b.summe(b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(nichtnull,b.auswahl(quelle,"minoren:grade"))),"*","maximum")
        }
        "zusammenfuehren" -> {
            skalar=true
            val flach=b.abflachen(b.tupel(inputs.map { it.second }))
            b.übersetze(flach,VektorStrukturAusgabe.vonIdOderStandard(original.parameter[VEKTOR_RECHNER_STRUKTUR_AUSGABE]).stabileId)
        }
        "polarwinkel" -> {
            skalar=true
            b.aufruf(b.index(b.übersetze(inputs.first().second,"polar")),b.tupel(listOf(b.konstante(2))))
        }
        "kartesisch" -> {
            skalar=true
            val radius=inputs.first { it.first.name=="radius" }.second
            val winkel=inputs.first { it.first.name=="winkel" }.second
            b.übersetze(b.tupel(listOf(radius,winkel)),"kartesisch")
        }
        "potenz" -> {
            if(familie!=RechnerFamilie.MATRIX) return null
            skalar=true
            val a=inputs.first().second
            val exponent=inputs.firstOrNull { it.first.name=="exponent" }?.second ?: return null
            val symbolisch=b.auswahl(a,"symbolischeMatrix")
            val negativ=b.kleiner(exponent,b.konstante(0))
            val inverseQuelle=b.auswahl(b.fall(negativ,b.auswahl(a,"matrixQuelle"),b.einsMatrix()),"rationaleMatrix")
            val kofaktoren=b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(determinant(inverseQuelle,true),b.auswahl(inverseQuelle,"alternierend")))
            val inverse=b.konstruiere(b.zahl(UniversellerZahlenOperator.DIVISION,listOf(kofaktoren,determinant(inverseQuelle))),"matrix")
            val faktor=b.fall(negativ,inverse,symbolisch)
            val neutral=b.konstruiere(b.auswahl(a,"einheit"),"matrix")
            val (konstruktor,definator)=b.faltungsAnfang(b.auswahl(exponent,"iterationsbereich"),neutral,faktor)
            val acc=IndexDefinitionsWert(konstruktor.knoten,"akkumulator")
            val indexwert=IndexDefinitionsWert(konstruktor.knoten,"index")
            val mitIndex=b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(b.index(faktor),b.zahl(UniversellerZahlenOperator.ADDITION,listOf(b.konstante(1),b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(b.konstante(0),indexwert))))))
            val nächster=matrixprodukt(acc,b.konstruiere(mitIndex,"matrix"))
            val term=b.faltungsEnde(definator,nächster)
            b.aufruf(b.methode(term,symbolisch),b.tupel(listOf(a)))
        }
        "charakteristischesPolynom" -> {
            skalar=true
            val verschoben=b.zahl(UniversellerZahlenOperator.SUBTRAKTION,listOf(index(0),b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(b.variable("lambda"),b.auswahl(inputs.first().second,"einheit")))))
            b.übersetze(determinant(b.konstruiere(verschoben,"matrix")),"methode")
        }
        "determinante" -> { skalar = true; determinant(inputs.first().second) }
        "inverse" -> {
            val quelle=inputs.first().second
            val kofaktoren=b.zahl(UniversellerZahlenOperator.MULTIPLIKATION,listOf(determinant(quelle,true),b.auswahl(quelle,"alternierend")))
            b.zahl(UniversellerZahlenOperator.DIVISION,listOf(kofaktoren,determinant(quelle)))
        }
        "projektion" -> {
            val a = index(0); val c = index(1)
            val faktor = b.zahl(UniversellerZahlenOperator.DIVISION, listOf(dot(a,c), dot(c,c)))
            b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, listOf(faktor,c))
        }
        "winkel" -> {
            skalar = true
            val a = index(0); val c = index(1)
            b.zahl(UniversellerZahlenOperator.ARCCOSINUS, listOf(b.zahl(UniversellerZahlenOperator.DIVISION,
                listOf(dot(a,c), b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, listOf(norm(a),norm(c)))))))
        }
        "winkelZuAchse" -> {
            skalar = true
            val a = index(0)
            val komponente = b.indexAchsen(inputs.first().second, "schnitt", "1,${original.parameter[VEKTOR_RECHNER_ACHSE] ?: "1"}", null)
            val zahl = b.konstruiere(komponente, "tensor")
            b.zahl(UniversellerZahlenOperator.ARCCOSINUS, listOf(b.zahl(UniversellerZahlenOperator.DIVISION, listOf(zahl, norm(a)))))
        }
        "transponieren", "achsenpermutation" -> achsenMethode("permutation")
        "achsenschnitt" -> achsenMethode("schnitt")
        "hauptdiagonale" -> achsenMethode("hauptdiagonale", "1,2")
        "kontraktion", "spur" -> {
            skalar = operation == "spur"
            b.summe(achsenMethode("diagonale", "1,2"), "@letzte")
        }
        "addition" -> punktweise(UniversellerZahlenOperator.ADDITION)
        "hadamardDivision" -> punktweise(UniversellerZahlenOperator.DIVISION)
        "subtraktion" -> punktweise(UniversellerZahlenOperator.SUBTRAKTION)
        "hadamard", "skalarmultiplikation", "skalierung" -> punktweise(UniversellerZahlenOperator.MULTIPLIKATION)
        "negation" -> b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, listOf(index(0), b.konstante(-1)))
        "produkt", "vektorProdukt" -> {
            var links = inputs.first().second
            inputs.drop(1).forEachIndexed { i, (_, rechts) ->
                val a = b.index(links, "i,k", matrix = true)
                val c = b.index(if(operation=="vektorProdukt") b.übersetze(rechts,"spalte") else rechts, "k,j", matrix = true)
                val produkt = b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, listOf(a, c))
                val summe = b.summe(produkt, "k")
                links = b.konstruiere(summe, "matrix")
            }
            if(operation=="vektorProdukt") b.konstruiere(b.indexAchsen(links,"schnitt","2,1",null),"spalte") else links
        }
        "tensorprodukt" -> {
            val a = index(0, präfix = "l")
            val c = index(1, präfix = "r")
            b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, listOf(a, c))
        }
        "skalarprodukt" -> {
            skalar = true
            if(original.parameter[SKALARPRODUKT_DEFINITION_PARAMETER]==SKALARPRODUKT_DEFINITION_ZERTIFIZIERT) {
                val zertifikat=inputs.firstOrNull { it.first.name==SKALARPRODUKT_DEFINITION_EINGANG }?.second ?: return null
                b.aufruf(b.zeugnis(zertifikat,inputs[0].second,inputs[1].second),b.tupel(listOf(inputs[0].second,inputs[1].second)))
            } else {
            val a = index(0)
            val c = index(1)
            val linkslinear = original.parameter[SKALARPRODUKT_LINEARITAET_PARAMETER] == SkalarproduktLinearitaet.LINKSLINEAR.name
            val konjugiert = true
            val faktoren = if (!konjugiert) listOf(a, c) else if (linkslinear)
                listOf(a, b.zahl(UniversellerZahlenOperator.KONJUGIERTE, listOf(c)))
            else listOf(b.zahl(UniversellerZahlenOperator.KONJUGIERTE, listOf(a)), c)
            b.summe(b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, faktoren), "*")
            }
        }
        "norm", "radius", "normalisierung", "distanz" -> {
            val a = if (operation == "distanz") b.zahl(UniversellerZahlenOperator.SUBTRAKTION, listOf(index(0), index(1))) else index(0)
            val quadrate = if (familie == RechnerFamilie.TENSOR)
                b.zahl(UniversellerZahlenOperator.MULTIPLIKATION, listOf(b.zahl(UniversellerZahlenOperator.KONJUGIERTE, listOf(a)), a))
            else b.zahl(UniversellerZahlenOperator.POTENZ, listOf(a, b.konstante(2)))
            val metrik = if (operation == "distanz") original.parameter[VEKTOR_RECHNER_METRIK] else null
            val norm = when (metrik) {
                "metrik.l1" -> b.summe(b.zahl(UniversellerZahlenOperator.BETRAG, listOf(a)), "*")
                "metrik.linf" -> b.summe(b.zahl(UniversellerZahlenOperator.BETRAG, listOf(a)), "*", "maximum")
                else -> b.zahl(UniversellerZahlenOperator.QUADRATWURZEL, listOf(b.summe(quadrate, "*")))
            }
            skalar = operation != "normalisierung"
            if (operation == "normalisierung") b.zahl(UniversellerZahlenOperator.DIVISION, listOf(a, norm)) else norm
        }
        else -> return null
    }
    val struktur = when {
        skalar || operation in setOf("produkt", "vektorProdukt") -> ergebnis
        operation in setOf("hauptdiagonale","nebendiagonale") -> b.konstruiere(ergebnis, "tupel")
        familie == RechnerFamilie.MATRIX -> b.konstruiere(ergebnis, "matrix")
        familie == RechnerFamilie.TENSOR -> b.konstruiere(ergebnis, "tensor")
        else -> b.konstruiere(ergebnis, "vorbild", if (operation == "projektion") inputs[1].second else inputs.first().second)
    }
    b.ausgang(struktur)
    return b.karte()
}

private data class IndexDefinitionsWert(val knoten: KnotenDaten, val ausgang: String)
private class IndexDefinitionsBauer(private val original: KnotenDaten, private val operator: String) {
    private val prefix = "indexdefinition-${original.id.wert}-${operator.replace('.', '-')}"
    private val knoten = mutableListOf<KnotenDaten>()
    private val verbindungen = mutableListOf<VerbindungDaten>()
    private val kontext = mutableListOf<IndexDefinitionsWert>()
    private fun add(node: KnotenDaten, ausgang: String): IndexDefinitionsWert {
        val id = "$prefix-${knoten.size}"
        val indexknoten=node.art==ÜBERSETZER_ART && übersetzerZiel(node)=="indexmethode"
        val mitKontext=if(indexknoten) node.copy(parameter=node.parameter+("indexmethode.rollenKontext" to "true"),anschlüsse=node.anschlüsse+kontext.indices.map { i -> AnschlussDaten(name="indexkontext$i",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.AtlasWert.id,reihenfolge=100+i) }) else node
        val neu = mitKontext.copy(id = KnotenId(id), position = GraphPunkt(knoten.size * 280f, 80f + knoten.size % 3 * 120f),
            anschlüsse = mitKontext.anschlüsse.mapIndexed { i, p -> p.copy(id = AnschlussId("$id-$i")) })
        knoten += neu
        val wert=IndexDefinitionsWert(neu,ausgang)
        if(indexknoten) kontext.forEachIndexed { i,quelle -> verbinde(quelle,wert,"indexkontext$i") }
        return wert
    }
    private fun verbinde(quelle: IndexDefinitionsWert, ziel: IndexDefinitionsWert, name: String) {
        verbindungen += VerbindungDaten(id = VerbindungsId("$prefix-v-${verbindungen.size}"),
            von = AnschlussVerweis(quelle.knoten.id, quelle.knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang && it.name == quelle.ausgang }.id),
            zu = AnschlussVerweis(ziel.knoten.id, ziel.knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == name }.id))
    }
    fun eingang(port: AnschlussDaten): IndexDefinitionsWert {
        val wert=add(KnotenDaten(art = KonzeptKnotenArten.EINGANG, name = port.name,
        parameter = mapOf("rolle" to port.name, "typ" to port.art.wert), anschlüsse = listOf(port.copy(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts,
            kannSichErweitern = false, dynamischErzeugt = false))), "wert")
        kontext+=wert
        return wert
    }
    fun index(quelle: IndexDefinitionsWert, namen: String? = null, präfix: String? = null, matrix: Boolean = false): IndexDefinitionsWert {
        val param = buildMap { namen?.let { put("indexmethode.parameterNamen", it) }; präfix?.let { put("indexmethode.parameterPräfix", it) }; if (matrix) put("indexmethode.matrixFaktor", "true") }
        val node = add(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "indexmethode").copy(parameter = param), "ergebnis")
        verbinde(quelle, node, "wert")
        return node
    }
    fun indexAchsen(quelle: IndexDefinitionsWert, modus: String, achsen: String, eingang: IndexDefinitionsWert?, schnittIndex: Int? = null): IndexDefinitionsWert {
        val basis = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "indexmethode")
        val ports = if (eingang == null) basis.anschlüsse else basis.anschlüsse + AnschlussDaten(name = "achsen", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Tupel.id, reihenfolge = 1)
        val node = add(basis.copy(anschlüsse = ports, parameter = basis.parameter + mapOf("indexmethode.achsenModus" to modus, "indexmethode.achsen" to achsen) + (schnittIndex?.let { mapOf("indexmethode.schnittIndex" to it.toString()) } ?: emptyMap())), "ergebnis")
        verbinde(quelle, node, "wert")
        eingang?.let { verbinde(it, node, "achsen") }
        return node
    }
    fun auswahl(quelle: IndexDefinitionsWert, auswahl: String): IndexDefinitionsWert {
        val basis=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"indexmethode")
        val node=add(basis.copy(name="Indexauswahl: $auswahl",parameter=basis.parameter+mapOf("indexmethode.auswahl" to auswahl,"indexmethode.form" to original.parameter[TENSOR_OPERATION_PARAMETER].orEmpty(),"indexmethode.rationalQuelle" to (operator=="matrix.charakteristischesPolynom").toString()),anschlüsse=basis.anschlüsse.map { if(it.richtung==AnschlussRichtung.Ausgang) it.copy(art=when(auswahl) { "symbolischeMatrix", "rationaleMatrix", "matrixQuelle" -> MathematikAnschlussArten.Matrix.id; "iterationsbereich" -> MathematikAnschlussArten.Menge.id; else -> it.art }) else if(auswahl=="iterationsbereich") it.copy(art=MathematikAnschlussArten.AtlasWert.id,zulässigeArten=emptySet()) else it }),"ergebnis")
        verbinde(quelle,node,"wert")
        return node
    }
    fun übersetze(quelle:IndexDefinitionsWert,ziel:String):IndexDefinitionsWert {
        val basis=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),ziel)
        val node=add(basis.copy(parameter=basis.parameter+("uebersetzer.r1Automatisch" to (ziel=="kartesisch").toString()),anschlüsse=basis.anschlüsse.map { if(it.richtung==AnschlussRichtung.Eingang) it.copy(art=MathematikAnschlussArten.AtlasWert.id,zulässigeArten=emptySet()) else it }),"ergebnis")
        verbinde(quelle,node,"wert"); return node
    }
    fun einsMatrix():IndexDefinitionsWert {
        val basis=MathematikKnotenVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val eingang=basis.anschlüsse.first { it.richtung==AnschlussRichtung.Eingang }
        val node=add(basis.copy(parameter=basis.parameter+mapOf("höhe" to "1","breite" to "1"),anschlüsse=basis.anschlüsse.filter { it.richtung==AnschlussRichtung.Ausgang || it.id==eingang.id }),"matrix")
        verbinde(konstante(1),node,eingang.name);return node
    }
    fun methode(term:IndexDefinitionsWert,parameterQuelle:IndexDefinitionsWert):IndexDefinitionsWert {
        val basis=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"methode")
        val ports=basis.anschlüsse.map { if(it.richtung==AnschlussRichtung.Eingang) it.copy(art=MathematikAnschlussArten.AtlasWert.id,zulässigeArten=emptySet()) else it }+AnschlussDaten(name="parameterQuelle",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.AtlasWert.id,reihenfolge=1)
        val node=add(basis.copy(anschlüsse=ports),"ergebnis")
        verbinde(term,node,"wert");verbinde(parameterQuelle,node,"parameterQuelle");return node
    }
    fun kleiner(a:IndexDefinitionsWert,c:IndexDefinitionsWert):IndexDefinitionsWert {
        val basis=MathematikKnotenVorlagen.Kleiner.erzeuge(GraphPunkt.Zero)
        val node=add(basis.copy(anschlüsse=basis.anschlüsse.map { if(it.richtung==AnschlussRichtung.Eingang) it.copy(art=MathematikAnschlussArten.AtlasWert.id,zulässigeArten=emptySet()) else it }),"aussage")
        verbinde(a,node,"links");verbinde(c,node,"rechts");return node
    }
    fun fall(aussage:IndexDefinitionsWert,wahr:IndexDefinitionsWert,lüge:IndexDefinitionsWert):IndexDefinitionsWert {
        val basis=MathematikKnotenVorlagen.Fall.erzeuge(GraphPunkt.Zero)
        val node=add(basis.copy(parameter=basis.parameter+("fall.annahmenNurGewaehlterZweig" to "true"),anschlüsse=basis.anschlüsse.map { if(it.name!="aussage") it.copy(art=MathematikAnschlussArten.Matrix.id,zulässigeArten=emptySet()) else it }),"wert")
        verbinde(aussage,node,"aussage");verbinde(wahr,node,"wahr");verbinde(lüge,node,"lüge");return node
    }
    fun faltungsAnfang(menge:IndexDefinitionsWert,neutral:IndexDefinitionsWert,quelle:IndexDefinitionsWert):Pair<IndexDefinitionsWert,KnotenDaten> {
        val paar=erzeugeFaltungsPaar(GraphPunkt.Zero,"produkt",MathematikAnschlussArten.Matrix.id)
        val id="$prefix-faltung"
        val basis=paar.konstruktor.copy(parameter=paar.konstruktor.parameter+mapOf(FALTUNG_PAAR to id,"faltung.frischeNamen" to "true"),anschlüsse=paar.konstruktor.anschlüsse+AnschlussDaten(name="quelle",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.AtlasWert.id,reihenfolge=2))
        val konstruktor=add(basis,"akkumulator")
        verbinde(menge,konstruktor,"indexmenge");verbinde(neutral,konstruktor,"neutral");verbinde(quelle,konstruktor,"quelle")
        return konstruktor to paar.definator.copy(parameter=paar.definator.parameter+(FALTUNG_PAAR to id))
    }
    fun faltungsEnde(basis:KnotenDaten,nächster:IndexDefinitionsWert):IndexDefinitionsWert {
        val node=add(basis,"wert");verbinde(nächster,node,"nächsterAkkumulator");return node
    }
    fun zeugnis(zertifikat:IndexDefinitionsWert,links:IndexDefinitionsWert,rechts:IndexDefinitionsWert):IndexDefinitionsWert {
        val basis=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"methode")
        val ports=basis.anschlüsse.map { if(it.richtung==AnschlussRichtung.Eingang) it.copy(art=MathematikAnschlussArten.Methode.id,zulässigeArten=emptySet()) else it } + listOf("links","rechts").mapIndexed { i,name -> AnschlussDaten(name=name,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.AtlasWert.id,reihenfolge=i+1) }
        val node=add(basis.copy(anschlüsse=ports,parameter=basis.parameter+original.parameter.filterKeys { it.startsWith("skalarprodukt.") }+("uebersetzer.zeugnis" to "skalarprodukt")),"ergebnis")
        verbinde(zertifikat,node,"wert");verbinde(links,node,"links");verbinde(rechts,node,"rechts");return node
    }
    fun abflachen(quelle:IndexDefinitionsWert):IndexDefinitionsWert {
        val basis=ÜbersetzungsKnotenVorlagen.Abflachen.erzeuge(GraphPunkt.Zero)
        val node=add(basis.copy(parameter=basis.parameter+("abflachen.ebenen" to "1")),"tupel")
        verbinde(quelle,node,"tupel");return node
    }
    fun tupel(werte:List<IndexDefinitionsWert>):IndexDefinitionsWert {
        val basis=konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero),TUPEL_EINZEL_EINGABEN)
        val ports=werte.indices.map { i -> AnschlussDaten(name="element$i",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.AtlasWert.id,reihenfolge=i) }
        val node=add(basis.copy(anschlüsse=ports+basis.anschlüsse.filter { it.richtung==AnschlussRichtung.Ausgang }),"tupel")
        werte.forEachIndexed { i,w -> verbinde(w,node,ports[i].name) }; return node
    }
    fun aufruf(methode:IndexDefinitionsWert,argumente:IndexDefinitionsWert):IndexDefinitionsWert {
        val basis=FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val node=add(basis.copy(parameter=basis.parameter+(METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_DIREKT),anschlüsse=basis.anschlüsse.map { if(it.richtung==AnschlussRichtung.Ausgang) it.copy(art=MathematikAnschlussArten.AtlasWert.id) else it }),"wert")
        verbinde(methode,node,"methode");verbinde(argumente,node,"argument-0");return node
    }
    fun variable(name:String)=add(MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("name" to name,"werteVorrat" to "Q")),"wert")
    fun konstante(wert: Long) = add(MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf("wert" to wert.toString())), "wert")
    fun zahl(op: UniversellerZahlenOperator, werte: List<IndexDefinitionsWert>): IndexDefinitionsWert {
        val basis = konfiguriereZahlenRechner(ZahlenRechnerKnotenVorlagen.alle.first().erzeuge(GraphPunkt.Zero), op)
        val ports = basis.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
        val eingänge = werte.indices.map { i -> ports[minOf(i, ports.lastIndex)].copy(name = if (i < ports.size) ports[i].name else "operand${i + 1}", reihenfolge = i,
            kannSichErweitern = false, dynamischErzeugt = false) }
        val node = add(basis.copy(anschlüsse = eingänge + basis.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang },
            parameter = basis.parameter + mapOf("indexmethode.parameterModus" to "benannt", "festeEingänge" to werte.size.toString())), "wert")
        werte.forEachIndexed { i, wert -> verbinde(wert, node, eingänge[i].name) }
        return node
    }
    fun summe(quelle: IndexDefinitionsWert, parameter: String, reduktion: String = "summe", skalar: Boolean = false): IndexDefinitionsWert {
        val basis = konfiguriereZahlenRechner(ZahlenRechnerKnotenVorlagen.alle.first().erzeuge(GraphPunkt.Zero), UniversellerZahlenOperator.ITERIERTE_SUMME)
        val node = add(basis.copy(name = "Indexsumme über $parameter", parameter = basis.parameter + mapOf("indexmethode.summenParameter" to if(parameter in setOf("*","@letzte")) parameter else "@rolle:$parameter", "indexmethode.reduktion" to reduktion),
            anschlüsse = basis.anschlüsse.filter { it.name != "indexmenge" }.map { if (it.richtung == AnschlussRichtung.Ausgang) it.copy(art = if (parameter == "*" || skalar) MathematikAnschlussArten.Zahl.id else if (parameter == "@letzte") MathematikAnschlussArten.AtlasWert.id else MathematikAnschlussArten.Methode.id) else it }), "wert")
        verbinde(quelle, node, "methode")
        return node
    }
    fun konstruiere(quelle: IndexDefinitionsWert, art: String, vorbild: IndexDefinitionsWert? = null): IndexDefinitionsWert {
        val ports = listOf(AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.AtlasWert.id),
            AnschlussDaten(name = "vorbild", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.AtlasWert.id, reihenfolge = 1),
            AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = when (art) { "matrix" -> MathematikAnschlussArten.Matrix.id; "tensor" -> MathematikAnschlussArten.Tensor.id; "spalte" -> MathematikAnschlussArten.SpaltenVektor.id; "zeile" -> MathematikAnschlussArten.ZeilenVektor.id; else -> MathematikAnschlussArten.AtlasWert.id }))
        val node = add(KnotenDaten(art = TensorRechner.KNOTEN_ART, name = "Struktur aus Indexmethode", anschlüsse = ports,
            parameter = mapOf(TENSOR_OPERATION_ID to "tensor.konstruktion", "erzeugungsArt" to "indexmethode", "indexmethode.ausgabe" to art)), "wert")
        verbinde(quelle, node, "methode")
        if (vorbild != null) verbinde(vorbild, node, "vorbild")
        return node
    }
    fun ausgang(quelle: IndexDefinitionsWert, name: String = "wert") {
        val node = add(MathematikKnotenVorlagen.KartenAusgang.erzeuge(GraphPunkt.Zero).copy(name = "Ergebnis", parameter = mapOf("name" to name,"rolle" to name), anschlüsse = MathematikKnotenVorlagen.KartenAusgang.erzeuge(GraphPunkt.Zero).anschlüsse.map { it.copy(art = MathematikAnschlussArten.AtlasWert.id) }), "")
        verbinde(quelle, node, node.knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.name)
    }
    fun karte() = KartenDaten(id = KartenId(prefix), name = "Indexdefinition: $operator", erstelltAm = 0L, knoten = knoten, verbindungen = verbindungen).normalisiereDynamischeRechnerOperanden().normalisiereStrukturierteDivisionVorSpeichern()
}
