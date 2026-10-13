package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.*

/** Exakte Gauß-rationale Koeffizienten, kein Double-Zertifikat. */
data class KomplexRational(val re: RationaleZahl, val im: RationaleZahl = RationaleZahl.Null) {
    operator fun plus(b: KomplexRational) = KomplexRational(re + b.re, im + b.im)
    operator fun unaryMinus() = KomplexRational(-re, -im)
    operator fun minus(b: KomplexRational) = this + -b
    operator fun times(b: KomplexRational) = KomplexRational(re*b.re - im*b.im, re*b.im + im*b.re)
    operator fun div(b: KomplexRational): KomplexRational {
        val n = b.re*b.re + b.im*b.im
        return KomplexRational((re*b.re + im*b.im)/n, (im*b.re - re*b.im)/n)
    }
    fun istNull() = re.istNull() && im.istNull()
    fun alsAusdruck(): ZahlAusdruck = if (im.istNull()) re else KomplexeZahl(re, im)
    internal fun oben() = re.pAbs() + im.pAbs()
    internal fun unten() = maxOf(re.pAbs(), im.pAbs())
    companion object {
        fun von(a: ZahlAusdruck): KomplexRational? = when (val q = vereinfache(a)) {
            is RationaleZahl -> KomplexRational(q)
            is KomplexeZahl -> {
                val r = vereinfache(q.realteil) as? RationaleZahl
                val i = vereinfache(q.imaginärteil) as? RationaleZahl
                if (r == null || i == null) null else KomplexRational(r, i)
            }
            else -> null
        }
    }
}
internal val kNull = KomplexRational(pNull)
internal val kEins = KomplexRational(pEins)
internal class KPolynom(werte: List<KomplexRational>, val budget: PolynomRechenBudget) {
    val c = werte.dropLastWhile { it.istNull() }
    val grad get() = c.lastIndex
    fun wert(z: KomplexRational) = c.asReversed().fold(kNull) { a, b -> (a*z+b).also(::pruefe) }
    private fun pruefe(z: KomplexRational) { budget.zahl(z.re); budget.zahl(z.im) }
    fun ableitung() = KPolynom(c.drop(1).mapIndexed { i, q -> q*KomplexRational(pQ(i+1)) }, budget)
    fun div(b: KPolynom): Pair<KPolynom, KPolynom> {
        require(b.grad >= 0)
        val r = c.toMutableList(); val q = MutableList(maxOf(0, grad-b.grad+1)) { kNull }
        while (r.size >= b.c.size) {
            budget.schritt()
            val k = r.lastIndex-b.grad; val f = r.last()/b.c.last(); q[k] = f
            b.c.forEachIndexed { j, v -> r[k+j] = (r[k+j]-f*v).also(::pruefe) }
            while (r.isNotEmpty() && r.last().istNull()) r.removeAt(r.lastIndex)
        }
        return KPolynom(q,budget) to KPolynom(r,budget)
    }
    fun ggt(b: KPolynom): KPolynom {
        var a = this; var r = b
        while (r.grad >= 0) { val t = a.div(r).second; a=r; r=t }
        return if (a.grad < 0) a else KPolynom(a.c.map { it/a.c.last() }, budget)
    }
    /** Koeffizienten von p(z+w), durch exaktes Horner-Verschieben. */
    fun verschoben(z: KomplexRational): List<KomplexRational> {
        var t = emptyList<KomplexRational>()
        c.asReversed().forEach { a ->
            val n = MutableList(t.size+1) { kNull }; n[0] = a
            t.forEachIndexed { j,v -> n[j] = n[j]+v*z; n[j+1]=n[j+1]+v }
            n.forEach(::pruefe); t=n
        }
        return t
    }
}

/** Rouché: lineares Taylor-Glied dominiert auf dem Kreis alle übrigen Glieder. */
fun pruefeKomplexeWurzelScheibe(
    koeffizienten: List<KomplexRational>, zentrum: KomplexRational, radius: RationaleZahl,
): Boolean = try {
    if (radius <= pNull) false else pruefeScheibe(KPolynom(koeffizienten, PolynomRechenBudget()), zentrum, radius)
} catch (_: PolynomBudgetEnde) { false }
private fun pruefeScheibe(p: KPolynom, z: KomplexRational, r: RationaleZahl): Boolean {
    val t = p.verschoben(z)
    if (t.size < 2) return false
    var summe = t[0].oben(); var potenz = r
    for (j in 2 until t.size) { potenz *= r; summe += t[j].oben()*potenz; p.budget.zahl(summe) }
    return t[1].unten()*r > summe
}
/** Spiegelung an Re(z)=a bzw. Im(z)=b. Phasengleiche Koeffizienten ergeben ein reelles Polynom.
 * Eine eindeutige Wurzel in einer dazu symmetrischen Scheibe muss auf der Achse liegen. */
private fun symmetrischeAchse(p:KPolynom,koordinate:RationaleZahl,vertikal:Boolean):Boolean {
    val offset=if(vertikal) KomplexRational(koordinate) else KomplexRational(pNull,koordinate)
    val c=p.verschoben(offset).mapIndexed { j,a ->
        if(!vertikal) a else a*when(j%4) { 0 -> kEins; 1 -> KomplexRational(pNull,pEins); 2 -> -kEins; else -> KomplexRational(pNull,-pEins) }
    }
    val phase=c.firstOrNull { !it.istNull() }?:return false
    return c.all { (it/phase).im.istNull() }
}

private fun rationalisiere(x: Double): RationaleZahl {
    val b = BigDecimal.valueOf(x); return if (b.scale() >= 0) RationaleZahl.von(b.unscaledValue(), BigInteger.TEN.pow(b.scale()))
        else RationaleZahl.von(b.unscaledValue()*BigInteger.TEN.pow(-b.scale()))
}
private data class DKomplex(val re: Double, val im: Double = 0.0) {
    operator fun plus(b: DKomplex) = DKomplex(re+b.re,im+b.im)
    operator fun minus(b: DKomplex) = DKomplex(re-b.re,im-b.im)
    operator fun times(b: DKomplex) = DKomplex(re*b.re-im*b.im,re*b.im+im*b.re)
    operator fun div(b: DKomplex): DKomplex { val n=b.re*b.re+b.im*b.im; return DKomplex((re*b.re+im*b.im)/n,(im*b.re-re*b.im)/n) }
    fun abs() = hypot(re,im)
}

fun loeseKomplexesPolynom(
    koeffizienten: List<KomplexRational>,
    konfiguration: PolynomNumerikKonfiguration = PolynomNumerikKonfiguration(),
    budget: PolynomRechenBudget = PolynomRechenBudget(),
): PolynomLoesungsErgebnis {
    require(koeffizienten.isNotEmpty())
    val gefunden = mutableListOf<ZertifiziertePolynomLoesung>()
    data class Scheibe(val z: KomplexRational, val r: RationaleZahl)
    val scheiben = mutableListOf<Scheibe>()
    val suche = mutableListOf<PolynomIterationsSchritt>()
    return try {
        val original = KPolynom(koeffizienten,budget)
        if (original.grad < 0) return PolynomLoesungsErgebnis(existenz=LoesungsNachweis.Bewiesen,
            vollstaendigkeit=LoesungsNachweis.Bewiesen,dimension=1,analytisch=listOf("0 = 0: jeder komplexe Wert ist eine Lösung."))
        if (original.grad == 0) return PolynomLoesungsErgebnis(existenz=LoesungsNachweis.Widerlegt,
            vollstaendigkeit=LoesungsNachweis.Bewiesen,analytisch=listOf("Konstante ungleich null."))
        val p = original.div(original.ggt(original.ableitung())).first
        if (p.grad > 32) return PolynomLoesungsErgebnis(hinweis="Komplexe Suche derzeit bis Grad 32; exakte Gleichung bleibt erhalten.")
        val d = p.ableitung()
        val normiert = p.c.map { it/p.c.last() }
        val c = normiert.map { DKomplex(it.re.zuDezimal().toDouble(),it.im.zuDezimal().toDouble()) }
        val radius = 1.0+c.dropLast(1).maxOf { it.abs() }
        if (!radius.isFinite() || c.any { !it.abs().isFinite() }) return PolynomLoesungsErgebnis(hinweis="Startwertsuche überschreitet den Gleitkommabereich; kein Unlösbarkeitsnachweis.")
        var starts = List(p.grad) { j -> val winkel=2*PI*(j+0.271)/p.grad; DKomplex(radius*cos(winkel),radius*sin(winkel)) }
        for (iteration in 0 until 200) {
            budget.schritt()
            val vorher=starts
            starts = starts.mapIndexed { j,z ->
                val wert=c.asReversed().fold(DKomplex(0.0)) { a,b -> a*z+b }
                val nenner=starts.filterIndexed { k,_ -> k!=j }.fold(DKomplex(1.0)) { a,b -> a*(z-b) }
                z-wert/nenner
            }
            suche += PolynomIterationsSchritt(starts.filter { it.abs().isFinite() }.flatMap {
                listOf(RationalesIntervall(rationalisiere(it.re)),RationalesIntervall(rationalisiere(it.im)))
            }, "Startwertiteration ${iteration+1}: Durand–Kerner-Kandidaten, ohne Existenz- oder Genauigkeitsnachweis.")
            // Nur Ende der heuristischen Startwertsuche; der exakte Nachweis folgt getrennt.
            if(starts.indices.all { (starts[it]-vorher[it]).abs()<1e-12 }) break
        }
        fun kuerze(q: RationaleZahl): RationaleZahl {
            val skala=BigInteger.TEN.pow(konfiguration.stellen+24)
            return RationaleZahl.von(q.zähler*skala/q.nenner,skala)
        }
        for (start in starts) {
            if (!start.abs().isFinite()) continue
            var z=KomplexRational(rationalisiere(start.re),rationalisiere(start.im))
            val verlauf=mutableListOf<PolynomIterationsSchritt>()
            var letzte: ZertifiziertePolynomLoesung?=null; var letzteScheibe: Scheibe?=null
            for (k in 1..konfiguration.stellen+20) {
                budget.schritt()
                val dz=d.wert(z)
                if (dz.istNull()) break
                val neu=z-p.wert(z)/dz; z=KomplexRational(kuerze(neu.re),kuerze(neu.im))
                val r=RationaleZahl.von(BigInteger.ONE,BigInteger.TEN.pow(k))
                if (!pruefeScheibe(p,z,r)) {
                    verlauf += PolynomIterationsSchritt(listOf(RationalesIntervall(z.re-r,z.re+r),RationalesIntervall(z.im-r,z.im+r)),
                        "Newton-Verfeinerung: Kandidatenregion, Rouché-Nachweis noch nicht erbracht.")
                    continue
                }
                val skala=BigInteger.TEN.pow(konfiguration.stellen)
                fun grenzwerte(q:RationaleZahl):List<RationaleZahl> {
                    val mitte=q.zähler*skala/q.nenner
                    return listOf(q)+( -1..1).map { RationaleZahl.von(mitte+BigInteger.valueOf(it.toLong()),skala) }
                        .filter { (it-q).pAbs()<=r }
                }
                for (grenze in grenzwerte(z.re)) {
                    val neu=KomplexRational(grenze,z.im)
                    if(symmetrischeAchse(p,grenze,true) && pruefeScheibe(p,neu,r)) { z=neu; break }
                }
                for (grenze in grenzwerte(z.im)) {
                    val neu=KomplexRational(z.re,grenze)
                    if(symmetrischeAchse(p,grenze,false) && pruefeScheibe(p,neu,r)) { z=neu; break }
                }
                val exakt=p.wert(z).istNull()
                val realFest=symmetrischeAchse(p,z.re,true)
                val imagFest=symmetrischeAchse(p,z.im,false)
                val box=listOf(
                    if (exakt || realFest) RationalesIntervall(z.re) else RationalesIntervall(z.re-r,z.re+r),
                    if (exakt || imagFest) RationalesIntervall(z.im) else RationalesIntervall(z.im-r,z.im+r))
                verlauf += PolynomIterationsSchritt(box, if (exakt) "Exaktes Einsetzen; lokale Eindeutigkeit durch Rouché." else "Rouché-Nachweis: genau eine Nullstelle in der Scheibe mit Radius $r."+(if(realFest || imagFest) " Spiegelungssymmetrie beweist konstante Koordinaten." else ""))
                var m: Int?=1; var g=original.ggt(original.ableitung())
                while (g.grad>0) {
                    val s=g.div(g.ggt(g.ableitung())).first
                    if (!pruefeScheibe(s,z,r)) {
                        val t=s.verschoben(z); var rest=pNull; var potenz=pEins
                        for (j in 1 until t.size) { potenz*=r; rest+=t[j].oben()*potenz }
                        if (!(t[0].unten()>rest)) m=null
                        break
                    }
                    m=m?.plus(1); g=g.ggt(g.ableitung())
                }
                val texte=box.map { zertifizierteDezimalzahl(it,konfiguration.stellen) }
                letzte=ZertifiziertePolynomLoesung(box, if (texte.all { it!=null }) texte.filterNotNull() else null,verlauf.toList(),m,KomplexeWurzelScheibe(z,r))
                letzteScheibe=Scheibe(z,r)
                if (letzte.dezimalstellen!=null) break
            }
            if (letzte!=null && letzteScheibe!=null && scheiben.all {
                maxOf((it.z.re-letzteScheibe.z.re).pAbs(),(it.z.im-letzteScheibe.z.im).pAbs()) > it.r+letzteScheibe.r
            }) { gefunden+=letzte; scheiben+=letzteScheibe }
        }
        PolynomLoesungsErgebnis(gefunden,if (gefunden.isEmpty()) LoesungsNachweis.Offen else LoesungsNachweis.Bewiesen,
            if (gefunden.size==p.grad) LoesungsNachweis.Bewiesen else LoesungsNachweis.Offen,
            if (gefunden.size==p.grad) 0 else null,
            analytisch=listOf("Exakte algebraische Wurzeln des angegebenen Polynoms, isoliert durch rationale Rouché-Scheiben."),
            hinweis=if (gefunden.size==p.grad) "Alle verschiedenen komplexen Wurzeln durch disjunkte Scheiben erfasst." else "Lokale Nachweise; Vollständigkeit der Suche bleibt offen.",suchschritte=suche)
    } catch (_: PolynomBudgetEnde) {
        PolynomLoesungsErgebnis(gefunden,if (gefunden.isEmpty()) LoesungsNachweis.Offen else LoesungsNachweis.Bewiesen,
            hinweis="Rechenbudget erschöpft; weitere Lösungen und Ziffern bleiben offen.",suchschritte=suche)
    }
}
