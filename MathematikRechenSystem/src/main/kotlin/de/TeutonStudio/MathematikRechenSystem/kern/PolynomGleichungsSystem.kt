package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

/** Komponentenreihenfolge: Variablenliste, über C jeweils Realteil und Imaginärteil. */
data class PolynomGleichungsProblem(
    val original: Aussage,
    val variablen: List<Variable>,
    val komponenten: List<ZahlAusdruck>,
    val komplex: Boolean,
    val matrixDimension: Int? = null,
    val eigenwertMatrix: Matrix? = null,
    val bedingungen: List<Aussage> = emptyList(),
) {
    init {
        require(variablen.isNotEmpty() && variablen.map { it.name }.distinct().size == variablen.size)
        require(komponenten.isNotEmpty() || eigenwertMatrix!=null)
        require(matrixDimension == null || matrixDimension > 0 && matrixDimension.toLong()*matrixDimension == variablen.size.toLong())
    }
}

fun polynomGleichungsProblem(
    aussage: Aussage, variablen: List<GebundeneMengenVariable>, matrixDimension: Int? = null,
): PolynomGleichungsProblem? {
    if (variablen.isEmpty() || variablen.any { it.grundMenge !in setOf(ReelleZahlen,KomplexeZahlen) }) return null
    if (variablen.map { it.grundMenge }.distinct().size != 1) return null
    fun komponenten(p: Aussage): List<ZahlAusdruck>? = when (p) {
        is Gleichheit -> {
            val l=p.links; val r=p.rechts
            when {
                l is ZahlAusdruck && r is ZahlAusdruck -> listOf(subtraktion(l,r))
                l is Matrix && r is Matrix -> {
                    require(l.tensorForm==r.tensorForm) { "Gleichungsmatrizen benötigen dieselbe Form." }
                    l.zeilen.flatten().zip(r.zeilen.flatten()) { a,b -> subtraktion(a,b) }
                }
                l is OrientierterVektor && r is OrientierterVektor -> {
                    require(l::class==r::class && l.werte.size==r.werte.size) { "Gleichungsvektoren benötigen gleiche Form und Orientierung." }
                    l.werte.zip(r.werte) { a,b -> subtraktion(a,b) }
                }
                else -> null
            }
        }
        is Konjunktion -> p.aussagen.flatMap { komponenten(it) ?: return null }
        else -> null
    }
    val k=komponenten(aussage) ?: return null
    val namen=variablen.map { it.variable.name }.toSet()
    if (k.flatMap { it.freieMethodenParameter() }.any { it.name !in namen }) return null
    return PolynomGleichungsProblem(aussage,variablen.map { it.variable },k,variablen.first().grundMenge==KomplexeZahlen,matrixDimension)
}

/** Baut die angefragten Matrix-/Vektorgleichungen mit normalen Kernobjekten. */
fun matrixPolynomProblem(a: Matrix, b: Matrix, c: Matrix, komplex: Boolean = false, name: String = "X"): PolynomGleichungsProblem {
    require(a.tensorForm==b.tensorForm && a.tensorForm==c.tensorForm) { "A, B und C müssen dieselbe Form besitzen." }
    val n=a.zeilenAnzahl
    require(n in 1..8) { "Matrixunbekannte derzeit mit Dimension 1 bis 8." }
    val vars=List(n*n) { Variable("${name}_${it/n+1}_${it%n+1}") }
    val x=Matrix(List(n) { i -> List(n) { j -> vars[i*n+j] } })
    val links=(x*x)*a
    val xb=x*b
    val summe=Matrix(links.zeilen.mapIndexed { i,row -> row.mapIndexed { j,v -> addition(v,xb.zeilen[i][j],c.zeilen[i][j]) } })
    val nullmatrix=Matrix(List(n) { List(a.spaltenAnzahl) { pNull } })
    return PolynomGleichungsProblem(Gleichheit(summe,nullmatrix),vars,summe.zeilen.flatten(),komplex,n)
}
fun matrixPolynomProblem(a: SpaltenVektor,b: SpaltenVektor,c: SpaltenVektor,komplex: Boolean=false,name:String="X"): PolynomGleichungsProblem =
    matrixPolynomProblem(Matrix(a.werte.map(::listOf)),Matrix(b.werte.map(::listOf)),Matrix(c.werte.map(::listOf)),komplex,name)

private class SPolynom(val terme: Map<List<Int>,KomplexRational>, val n: Int, val budget: PolynomRechenBudget) {
    operator fun plus(b: SPolynom): SPolynom {
        val t=terme.toMutableMap()
        b.terme.forEach { (m,c) -> budget.schritt(); t[m]=(t[m]?:kNull)+c }
        return SPolynom(t.filterValues { !it.istNull() },n,budget)
    }
    operator fun times(b: SPolynom): SPolynom {
        val t=mutableMapOf<List<Int>,KomplexRational>()
        for ((a,c) in terme) for ((d,e) in b.terme) {
            budget.schritt(); val m=a.zip(d) { x,y -> x+y }
            if (m.sum()>256 || t.size>4096) throw PolynomBudgetEnde()
            val v=(t[m]?:kNull)+c*e; budget.zahl(v.re); budget.zahl(v.im); t[m]=v
        }
        return SPolynom(t.filterValues { !it.istNull() },n,budget)
    }
    fun skaliere(c:KomplexRational)=SPolynom(terme.mapValues { it.value*c },n,budget)
}
private fun zerlegeSystem(problem:PolynomGleichungsProblem,budget:PolynomRechenBudget): List<RPolynom>? {
    val n=problem.variablen.size*(if(problem.komplex) 2 else 1)
    if (n>128) return null
    val nullmonom=List(n) { 0 }
    fun konstant(c:KomplexRational)=SPolynom(if(c.istNull()) emptyMap() else mapOf(nullmonom to c),n,budget)
    fun variable(index:Int)=SPolynom(mapOf(List(n) { if(it==index) 1 else 0 } to kEins),n,budget)
    val vars=problem.variablen.mapIndexed { j,v -> v.name to if(problem.komplex)
        variable(2*j)+variable(2*j+1).skaliere(KomplexRational(pNull,pEins)) else variable(j) }.toMap()
    fun parse(z:ZahlAusdruck):SPolynom? {
        budget.schritt()
        return when(z) {
            is Variable -> vars[z.name]
            is RationaleZahl -> konstant(KomplexRational(z))
            is KomplexeZahl -> {
                val r=parse(z.realteil)?:return null; val i=parse(z.imaginärteil)?:return null
                r+i.skaliere(KomplexRational(pNull,pEins))
            }
            is Addition -> z.summanden.map { parse(it)?:return null }.fold(konstant(kNull)) { a,b -> a+b }
            is Multiplikation -> z.faktoren.map { parse(it)?:return null }.fold(konstant(kEins)) { a,b -> a*b }
            is Division -> {
                val d=KomplexRational.von(z.divisor)?:return null
                if(d.istNull()) return null
                (parse(z.dividend)?:return null).skaliere(kEins/d)
            }
            is Potenz -> {
                val e=z.exponent as? RationaleZahl?:return null
                if(e.nenner!=BigInteger.ONE || e.zähler.signum()<0 || e.zähler>BigInteger.valueOf(256)) return null
                val b=parse(z.basis)?:return null
                var r=konstant(kEins); repeat(e.zähler.toInt()) { r=r*b }; r
            }
            else -> KomplexRational.von(z)?.let(::konstant)
        }
    }
    return problem.komponenten.flatMap { z ->
        val p=parse(z)?:return null
        // Auch über R müssen imaginäre Koeffizienten als zusätzliche Gleichungen gelten.
        listOf(RPolynom(p.terme.mapValues { it.value.re }.filterValues { !it.istNull() },n,budget),
            RPolynom(p.terme.mapValues { it.value.im }.filterValues { !it.istNull() },n,budget))
    }.filter { it.terme.isNotEmpty() }
}
internal class RPolynom(val terme:Map<List<Int>,RationaleZahl>,val n:Int,val budget:PolynomRechenBudget) {
    fun ableitung(k:Int)=RPolynom(terme.filterKeys { it[k]>0 }.map { (m,c) ->
        m.mapIndexed { j,e -> if(j==k) e-1 else e } to c*pQ(m[k]) }.toMap(),n,budget)
    fun wert(x:List<RationaleZahl>):RationaleZahl = terme.entries.fold(pNull) { a,(m,c) ->
        var t=c
        m.forEachIndexed { j,e -> repeat(e) { t*=x[j]; budget.zahl(t) } }; (a+t).also(budget::zahl)
    }
    fun intervall(x:List<RationalesIntervall>):RationalesIntervall = terme.entries.fold(RationalesIntervall(pNull)) { a,(m,c) ->
        var t=RationalesIntervall(c)
        m.forEachIndexed { j,e -> repeat(e) { budget.schritt(); t*=x[j] } }
        (a+t).also { budget.zahl(it.minimum); budget.zahl(it.maximum) }
    }
}

/** Krawczyk mit rationalem Vorconditionierer; strikte Inklusion beweist eine lokale Nullstelle. */
internal fun krawczyk(system:List<RPolynom>,box:List<RationalesIntervall>):List<RationalesIntervall>? {
    val n=box.size
    if(system.size!=n || box.any { it.punkt }) return null
    val mitte=box.map { (it.minimum+it.maximum)/pQ(2) }
    val jac=system.map { p -> List(n) { p.ableitung(it) } }
    val j=Matrix(jac.map { row -> row.map { it.wert(mitte) } })
    val inverse=try { inverseMitGauss(j).inverse } catch (_: IllegalArgumentException) { return null } catch (_: IllegalStateException) { return null }
    val c=inverse.zeilen.map { row -> row.map { it as RationaleZahl } }
    val f=system.map { it.wert(mitte) }
    val ji=jac.map { row -> row.map { it.intervall(box) } }
    val k=List(n) { i ->
        var r=RationalesIntervall(mitte[i]-c[i].indices.fold(pNull) { a,s -> a+c[i][s]*f[s] })
        for(h in 0 until n) {
            var v=RationalesIntervall(if(i==h) pEins else pNull)
            for(s in 0 until n) v-=ji[s][h].pSkalar(c[i][s])
            r+=v*(box[h]-RationalesIntervall(mitte[h]))
        }; r
    }
    return k.takeIf { it.indices.all { i -> it[i].minimum>box[i].minimum && it[i].maximum<box[i].maximum } }
}

private fun loeseUnbedingtesPolynomSystem(problem:PolynomGleichungsProblem,konfiguration:PolynomNumerikKonfiguration=PolynomNumerikKonfiguration(),
    budget:PolynomRechenBudget=PolynomRechenBudget()):PolynomLoesungsErgebnis {
    val gefunden=mutableListOf<ZertifiziertePolynomLoesung>()
    val suche=mutableListOf<PolynomIterationsSchritt>()
    return try {
        if(problem.eigenwertMatrix!=null) return loesePolynomGleichungsSystem(eigenwertPolynomProblem(problem.eigenwertMatrix,budget),konfiguration,budget)
        if(problem.variablen.size==1 && problem.komponenten.size==1) {
            val p=zerlegeAlsPolynom(problem.komponenten.single(),problem.variablen.single())
            if(p!=null) {
                val c=p.koeffizienten.map { KomplexRational.von(it) }
                if(c.all { it!=null }) {
                    val k=c.filterNotNull()
                    if(problem.komplex) return loeseKomplexesPolynom(k,konfiguration,budget)
                    if(k.all { it.im.istNull() }) return loeseReellesPolynom(k.map { it.re },konfiguration,budget)
                }
            }
        }
        val system=zerlegeSystem(problem,budget)?:return PolynomLoesungsErgebnis(hinweis="Nicht unterstützte oder ungebundene Koeffizienten; Originalgleichung bleibt erhalten.")
        val n=problem.variablen.size*(if(problem.komplex) 2 else 1)
        if(system.isEmpty()) return PolynomLoesungsErgebnis(existenz=LoesungsNachweis.Bewiesen,vollstaendigkeit=LoesungsNachweis.Bewiesen,
            dimension=problem.variablen.size,analytisch=listOf("Identische Gleichung: der gesamte Variablenraum ist die Lösungsmenge."))
        if(system.all { p -> p.terme.keys.all { it.sum()<=1 } }) {
            val a=Matrix(system.map { p -> List(n) { j -> p.terme[List(n) { if(it==j) 1 else 0 }]?:pNull } })
            val b=SpaltenVektor(system.map { -(it.terme[List(n) { 0 }]?:pNull) })
            val gauss=loeseLinearesSystem(a,b)
            val info=listOf("Rang der Koeffizientenmatrix: ${gauss.rangKoeffizienten}; Rang der erweiterten Matrix: ${gauss.rangErweitert}; Anzahl reeller Unbekannter: $n.")
            when(val l=gauss.loesung) {
                KeineLineareLoesung -> return PolynomLoesungsErgebnis(existenz=LoesungsNachweis.Widerlegt,vollstaendigkeit=LoesungsNachweis.Bewiesen,analytisch=info+"Die Ränge unterscheiden sich: keine Lösung.")
                is ParametrischeLineareLoesung -> return PolynomLoesungsErgebnis(existenz=LoesungsNachweis.Bewiesen,vollstaendigkeit=LoesungsNachweis.Bewiesen,
                    dimension=(n-gauss.rangKoeffizienten)/(if(problem.komplex) 2 else 1),analytisch=info+"Exakte affine Familie; freie Parameter über R.",familie=l.loesungsVektor.werte,parameter=l.freieParameter)
                is EindeutigeLineareLoesung -> {
                    val box=l.loesungsVektor.werte.map { RationalesIntervall(it as RationaleZahl) }
                    return PolynomLoesungsErgebnis(listOf(ZertifiziertePolynomLoesung(box,box.map { abgeschnitteneDezimalzahl(it.minimum,konfiguration.stellen) },emptyList())),
                        LoesungsNachweis.Bewiesen,LoesungsNachweis.Bewiesen,0,info+"Eindeutige exakte Lösung durch Gauß.")
                }
            }
        }
        val linearerRang=if(n<=16) gauss(Matrix(system.map { p -> List(n) { j -> p.terme[List(n) { if(it==j) 1 else 0 }]?:pNull } })).rang else null
        val info=listOf("Rang des linearen Anteils: ${linearerRang?:"nicht berechnet"}; Dimension des reellen Unbekanntenraums: $n.", "Nichtlineares System mit ${system.size} reellen Bedingungen und $n reellen Unbekannten.",
            "Ein Rangkriterium allein entscheidet die quadratische Gleichung nicht.")
        if(n>16) return PolynomLoesungsErgebnis(analytisch=info,
            hinweis="Systemzertifizierung derzeit bis 16 reellen Unbekannten. Lösungsfamilien und Dimension bleiben offen.")
        val jac=system.map { p -> List(n) { p.ableitung(it) } }
        val skala=BigInteger.TEN.pow(konfiguration.stellen+24)
        fun kuerze(q:RationaleZahl)=RationaleZahl.von(q.zähler*skala/q.nenner,skala)
        // Deterministische Startpunkte einschließlich Null und Achsen; kein Vollständigkeitsanspruch.
        val raster = if(n<=6) List(minOf(256, (1..n).fold(1) { a,_ -> a*3 })) { index ->
            var rest=index; List(n) { val v=rest%3-1; rest/=3; pQ(v) }
        } else emptyList()
        val starts=raster+List(1+2*n+24) { s -> List(n) { j -> when {
            s==0 -> pNull
            s<=2*n -> if((s-1)/2==j) pQ(if(s%2==1) 1 else -1) else pNull
            else -> pQ((((s*37+j*19)%101)-50)*konfiguration.suchradius)/pQ(50)
        } } }
        for(start in starts) {
            var z=start
            val verlauf=mutableListOf<PolynomIterationsSchritt>()
            var letzte:ZertifiziertePolynomLoesung?=null
            for(k in 0..konfiguration.stellen+24) {
                budget.schritt()
                val f=system.map { it.wert(z) }
                if(f.all { it.istNull() }) {
                    val box=z.map { RationalesIntervall(it) }
                    verlauf+=PolynomIterationsSchritt(box,"Exaktes Einsetzen in alle ursprünglichen Komponenten beweist die Lösung; keine Behauptung lokaler Eindeutigkeit.")
                    letzte=ZertifiziertePolynomLoesung(box,box.map { abgeschnitteneDezimalzahl(it.minimum,konfiguration.stellen) },verlauf.toList())
                    break
                }
                if(system.size!=n) break
                val linear=loeseLinearesSystem(Matrix(jac.map { row -> row.map { it.wert(z) } }),SpaltenVektor(f))
                val schritt=linear.loesung as? EindeutigeLineareLoesung
                if(schritt==null) {
                    verlauf+=PolynomIterationsSchritt(z.map { RationalesIntervall(it) },"Keine eindeutige Newton-Richtung; Lösbarkeit bleibt offen.")
                    break
                }
                z=z.zip(schritt.loesungsVektor.werte) { a,b -> kuerze(a-(b as RationaleZahl)) }
                if(z.any { it.pAbs()>pQ(konfiguration.suchradius) }) break
                val r=RationaleZahl.von(BigInteger.ONE,BigInteger.TEN.pow(k+1))
                val box=z.map { RationalesIntervall(it-r,it+r) }
                val enger=krawczyk(system,box)
                if(enger==null) {
                    verlauf+=PolynomIterationsSchritt(box,"Newton-Kandidatenregion; Krawczyk-Nachweis noch nicht erbracht.")
                    continue
                }
                val exakt=system.all { it.wert(z).istNull() }
                val einschluss=if(exakt) z.map { RationalesIntervall(it) } else enger
                verlauf+=PolynomIterationsSchritt(einschluss,"Strikte Krawczyk-Inklusion beweist Existenz und lokale Eindeutigkeit."+(if(exakt) " Exaktes Einsetzen bestätigt den Punkt." else ""))
                val texte=einschluss.map { zertifizierteDezimalzahl(it,konfiguration.stellen) }
                letzte=ZertifiziertePolynomLoesung(einschluss,if(texte.all { it!=null }) texte.filterNotNull() else null,verlauf.toList())
                if(letzte.dezimalstellen!=null) break
            }
            suche+=verlauf
            if(letzte!=null && gefunden.all { a -> a.komponenten.indices.any { j ->
                a.komponenten[j].maximum<letzte.komponenten[j].minimum || letzte.komponenten[j].maximum<a.komponenten[j].minimum
            } }) gefunden+=letzte
        }
        PolynomLoesungsErgebnis(gefunden,if(gefunden.isEmpty()) LoesungsNachweis.Offen else LoesungsNachweis.Bewiesen,
            analytisch=info,hinweis="Nachgewiesene Lösungen im Suchbereich; globale Vollständigkeit und Dimension bleiben offen.",suchschritte=suche)
    } catch (_:PolynomBudgetEnde) {
        PolynomLoesungsErgebnis(gefunden,if(gefunden.isEmpty()) LoesungsNachweis.Offen else LoesungsNachweis.Bewiesen,
            hinweis="Rechenbudget erschöpft. Keine Aussage über Unlösbarkeit oder Vollständigkeit.",suchschritte=suche)
    }
}

/** Ein Mitglied einer bewiesenen affinen Familie; nach Substitution erneut gegen das System geprüft. */
fun wertePolynomFamilieAus(problem:PolynomGleichungsProblem,familie:PolynomLoesungsErgebnis,
    belegung:Map<String,RationaleZahl>,stellen:Int):ZertifiziertePolynomLoesung {
    require(familie.vollstaendigkeit==LoesungsNachweis.Bewiesen && familie.familie!=null)
    require(belegung.keys==familie.parameter.map { it.name }.toSet()) { "Alle freien Parameter benötigen eine Belegung." }
    val werte=familie.familie.map { vereinfache(ersetze(it,belegung)) as? RationaleZahl
        ?: error("Familienmitglied konnte nicht exakt ausgewertet werden.") }
    val system=zerlegeSystem(problem,PolynomRechenBudget())?:error("System nicht prüfbar.")
    require(system.all { it.wert(werte).istNull() }) { "Familienmitglied erfüllt die Originalgleichung nicht." }
    val box=werte.map { RationalesIntervall(it) }
    return ZertifiziertePolynomLoesung(box,werte.map { abgeschnitteneDezimalzahl(it,stellen) },
        listOf(PolynomIterationsSchritt(box,"Parameter exakt eingesetzt und sämtliche Gleichungskomponenten geprüft.")))
}

/** Annahmen sind Teil des Problems, niemals nur ein unsichtbarer UI-Hinweis. */
fun loesePolynomGleichungsSystem(problem:PolynomGleichungsProblem,
    konfiguration:PolynomNumerikKonfiguration=PolynomNumerikKonfiguration(),
    budget:PolynomRechenBudget=PolynomRechenBudget()):PolynomLoesungsErgebnis {
    val r=loeseUnbedingtesPolynomSystem(problem,konfiguration,budget)
    val offen=problem.bedingungen.filter { it.entscheide().wahrheitswert!=Wahrheitswert.Wahr }
    if(offen.isEmpty()) return r
    if(offen.any { it.entscheide().wahrheitswert==Wahrheitswert.Lüge }) return PolynomLoesungsErgebnis(
        existenz=LoesungsNachweis.Widerlegt,vollstaendigkeit=LoesungsNachweis.Bewiesen,hinweis="Eine notwendige Annahme ist widerlegt.")
    if(r.familie!=null || r.existenz==LoesungsNachweis.Bewiesen && r.loesungen.isEmpty()) return PolynomLoesungsErgebnis(
        hinweis="Zusätzliche Annahmen beschränken die Familie; die genaue Teilmenge und Dimension bleiben offen.")
    fun pruefe(p:Aussage,l:ZertifiziertePolynomLoesung):Wahrheitswert? {
        val box=l.komponenten
        if(box.all { it.punkt }) {
            val bindung=problem.variablen.mapIndexed { j,v -> v.name to if(problem.komplex)
                KomplexeZahl(box[2*j].minimum,box[2*j+1].minimum) else box[j].minimum }.toMap()
            return ersetze(p,bindung).entscheide().wahrheitswert
        }
        if(problem.komplex) return null
        if(p is Konjunktion) {
            val teile=p.aussagen.map { pruefe(it,l) }
            return if(teile.any { it==Wahrheitswert.Lüge }) Wahrheitswert.Lüge else if(teile.all { it==Wahrheitswert.Wahr }) Wahrheitswert.Wahr else null
        }
        if(p is Vergleich) {
            val umgebung=problem.variablen.mapIndexed { j,v -> v.name to box[j] }.toMap()
            val d=ZertifizierterIntervallAuswerter.wert(subtraktion(p.links,p.rechts),umgebung) as? IntervallNachweis.Schranke?:return null
            val i=d.intervall
            val wahr=when(p.art) { VergleichsArt.Kleiner -> i.maximum<pNull; VergleichsArt.KleinerGleich -> i.maximum<=pNull; VergleichsArt.Größer -> i.minimum>pNull; VergleichsArt.GrößerGleich -> i.minimum>=pNull }
            val falsch=when(p.art) { VergleichsArt.Kleiner -> i.minimum>=pNull; VergleichsArt.KleinerGleich -> i.minimum>pNull; VergleichsArt.Größer -> i.maximum<=pNull; VergleichsArt.GrößerGleich -> i.maximum<pNull }
            return if(wahr) Wahrheitswert.Wahr else if(falsch) Wahrheitswert.Lüge else null
        }
        return null
    }
    var unbestimmt=false
    val gueltig=r.loesungen.filter { l ->
        val tests=offen.map { pruefe(it,l) }
        if(tests.any { it==Wahrheitswert.Lüge }) false
        else if(tests.all { it==Wahrheitswert.Wahr }) true else { unbestimmt=true; false }
    }
    val voll=r.vollstaendigkeit==LoesungsNachweis.Bewiesen && !unbestimmt
    return r.copy(loesungen=gueltig,existenz=if(gueltig.isNotEmpty()) LoesungsNachweis.Bewiesen else if(voll) LoesungsNachweis.Widerlegt else LoesungsNachweis.Offen,
        vollstaendigkeit=if(voll) LoesungsNachweis.Bewiesen else LoesungsNachweis.Offen,
        dimension=if(voll && gueltig.isNotEmpty()) 0 else null,
        hinweis=r.hinweis+" Zusätzliche Annahmen wurden geprüft; unbewiesene Kandidaten werden nicht als Lösungen ausgegeben.")
}
