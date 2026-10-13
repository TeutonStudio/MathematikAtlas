package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

/** Die Statusachsen dürfen insbesondere bei einer lokalen Lösung nicht zusammenfallen. */
enum class LoesungsNachweis { Bewiesen, Widerlegt, Offen }
data class PolynomNumerikKonfiguration(val stellen: Int = 6, val suchradius: Int = 10) {
    init { require(stellen in 0..100); require(suchradius in 1..1000) }
}
data class PolynomIterationsSchritt(val einschliessung: List<RationalesIntervall>, val begruendung: String)
data class ZertifiziertePolynomLoesung(
    val komponenten: List<RationalesIntervall>,
    val dezimalstellen: List<String>?,
    val schritte: List<PolynomIterationsSchritt>,
    val vielfachheit: Int? = null,
    val komplexeScheibe: KomplexeWurzelScheibe? = null,
)
data class PolynomLoesungsErgebnis(
    val loesungen: List<ZertifiziertePolynomLoesung> = emptyList(),
    val existenz: LoesungsNachweis = LoesungsNachweis.Offen,
    val vollstaendigkeit: LoesungsNachweis = LoesungsNachweis.Offen,
    /** Dimension über dem angegebenen Grundkörper, sofern bewiesen. */
    val dimension: Int? = null,
    val analytisch: List<String> = emptyList(),
    val hinweis: String = "",
    val familie: List<ZahlAusdruck>? = null,
    val parameter: List<Variable> = emptyList(),
    val analytischeFormeln: List<String> = emptyList(),
    val suchschritte: List<PolynomIterationsSchritt> = emptyList(),
)

/** Abschneiden gegen null. Exakte Ganzzahlarithmetik, auch für negative Zahlen. */
fun abgeschnitteneDezimalzahl(wert: RationaleZahl, stellen: Int): String {
    require(stellen in 0..100)
    val skala = BigInteger.TEN.pow(stellen)
    val ziffern = wert.zähler * skala / wert.nenner
    val betrag = ziffern.abs().toString().padStart(stellen + 1, '0')
    val vorzeichen = if (ziffern.signum() < 0) "-" else ""
    return vorzeichen + if (stellen == 0) betrag else betrag.dropLast(stellen) + "," + betrag.takeLast(stellen)
}
fun zertifizierteDezimalzahl(intervall: RationalesIntervall, stellen: Int): String? {
    val links = abgeschnitteneDezimalzahl(intervall.minimum, stellen)
    return links.takeIf { it == abgeschnitteneDezimalzahl(intervall.maximum, stellen) }
}

internal val pNull = RationaleZahl.Null
internal val pEins = RationaleZahl.Eins
internal fun pQ(n: Int) = RationaleZahl.von(n.toLong())
internal fun RationaleZahl.pAbs() = if (this < pNull) -this else this
internal fun RationalesIntervall.pSkalar(q: RationaleZahl) = this * RationalesIntervall(q)

/** Ein gemeinsames Budget erfasst auch innere Algebraoperationen und Abbruch. */
class PolynomRechenBudget(private var rest: Int = 100_000, private val abbruch: () -> Unit = {}) {
    fun schritt() {
        abbruch()
        if (Thread.currentThread().isInterrupted || --rest < 0) throw PolynomBudgetEnde()
    }
    internal fun zahl(q: RationaleZahl) {
        schritt()
        if (q.zähler.bitLength() > 16384 || q.nenner.bitLength() > 16384) throw PolynomBudgetEnde()
    }
}
internal class PolynomBudgetEnde : RuntimeException()

/** Aufsteigende rationale Koeffizienten; identisches Nullpolynom hat Grad -1. */
internal class QPolynom(werte: List<RationaleZahl>, val budget: PolynomRechenBudget) {
    val c = werte.dropLastWhile { it.istNull() }
    val grad get() = c.lastIndex
    fun wert(x: RationaleZahl): RationaleZahl = c.asReversed().fold(pNull) { a, b ->
        (a * x + b).also(budget::zahl)
    }
    fun ableitung() = QPolynom(c.drop(1).mapIndexed { i, q -> q * pQ(i + 1) }, budget)
    fun negativ() = QPolynom(c.map { -it }, budget)
    fun normiert() = if (grad < 0) this else QPolynom(c.map { it / c.last() }, budget)
    fun div(b: QPolynom): Pair<QPolynom, QPolynom> {
        require(b.grad >= 0)
        val r = c.toMutableList(); val q = MutableList(maxOf(0, grad - b.grad + 1)) { pNull }
        while (r.size >= b.c.size) {
            budget.schritt()
            val k = r.lastIndex - b.grad; val a = r.last() / b.c.last(); q[k] = a
            b.c.forEachIndexed { j, v -> r[k + j] = (r[k + j] - a * v).also(budget::zahl) }
            while (r.isNotEmpty() && r.last().istNull()) r.removeAt(r.lastIndex)
        }
        return QPolynom(q, budget) to QPolynom(r, budget)
    }
    fun ggt(b: QPolynom): QPolynom {
        var a = this; var r = b
        while (r.grad >= 0) { val t = a.div(r).second; a = r; r = t }
        return a.normiert()
    }
    fun sturm(): List<QPolynom> = buildList {
        add(this@QPolynom); val d = ableitung(); if (d.grad < 0) return@buildList
        add(d)
        while (last().grad > 0) {
            val r = this[size - 2].div(last()).second.negativ()
            if (r.grad < 0) break
            add(r)
        }
    }
}
private fun variation(folge: List<QPolynom>, x: RationaleZahl): Int {
    val s = folge.map { it.wert(x).zähler.signum() }.filter { it != 0 }
    return s.zipWithNext().count { it.first != it.second }
}

/** Kontrolliert den Wurzelanzahlnachweis erneut; Endpunkte müssen wurzelfrei sein. */
fun pruefeReellesWurzelIntervall(koeffizienten: List<RationaleZahl>, intervall: RationalesIntervall): Boolean {
    if (koeffizienten.isEmpty()) return false
    return try {
        val p = QPolynom(koeffizienten, PolynomRechenBudget())
        if (p.grad < 1) false
        else if (intervall.punkt) p.wert(intervall.minimum).istNull()
        else if (p.wert(intervall.minimum).istNull() || p.wert(intervall.maximum).istNull()) false
        else {
            val s = p.div(p.ggt(p.ableitung())).first.sturm()
            variation(s, intervall.minimum) - variation(s, intervall.maximum) == 1
        }
    } catch (_: PolynomBudgetEnde) { false }
}

/** Vollständige reelle Isolation mit Sturm; Mehrfachwurzeln werden vorab entfernt. */
fun loeseReellesPolynom(
    koeffizienten: List<RationaleZahl>,
    konfiguration: PolynomNumerikKonfiguration = PolynomNumerikKonfiguration(),
    budget: PolynomRechenBudget = PolynomRechenBudget(),
): PolynomLoesungsErgebnis {
    require(koeffizienten.isNotEmpty())
    val gefunden = mutableListOf<ZertifiziertePolynomLoesung>()
    return try {
        val original = QPolynom(koeffizienten, budget)
        if (original.grad < 0) return PolynomLoesungsErgebnis(existenz = LoesungsNachweis.Bewiesen,
            vollstaendigkeit = LoesungsNachweis.Bewiesen, dimension = 1, analytisch = listOf("0 = 0: jeder reelle Wert ist eine Lösung."))
        if (original.grad == 0) return PolynomLoesungsErgebnis(existenz = LoesungsNachweis.Widerlegt,
            vollstaendigkeit = LoesungsNachweis.Bewiesen, analytisch = listOf("Eine von null verschiedene Konstante kann nicht null sein."))
        val p = original.div(original.ggt(original.ableitung())).first.normiert()
        val sturm = p.sturm()
        val max = p.c.dropLast(1).maxOf { it.pAbs() }
        val grenze = RationaleZahl.von(max.zähler / max.nenner + BigInteger.TWO)
        data class Aufgabe(val l: RationaleZahl, val r: RationaleZahl, val verlauf: List<PolynomIterationsSchritt>)
        val queue = ArrayDeque<Aufgabe>(); queue.add(Aufgabe(-grenze, grenze, emptyList()))
        fun vielfachheit(i: RationalesIntervall): Int {
            var a = original; var m = 0
            while (a.grad > 0) {
                val hat = if (i.punkt) a.wert(i.minimum).istNull() else {
                    val s = a.div(a.ggt(a.ableitung())).first.sturm()
                    variation(s, i.minimum) - variation(s, i.maximum) > 0
                }
                if (!hat) break
                m++; a = a.ggt(a.ableitung())
            }
            return m
        }
        while (queue.isNotEmpty()) {
            budget.schritt()
            val a = queue.removeFirst()
            val anzahl = variation(sturm, a.l) - variation(sturm, a.r)
            if (anzahl == 0) continue
            val i = RationalesIntervall(a.l, a.r)
            val verlauf = a.verlauf + PolynomIterationsSchritt(listOf(i), "Sturm-Nachweis: $anzahl verschiedene reelle Nullstelle(n).")
            val ziffern = zertifizierteDezimalzahl(i, konfiguration.stellen)
            if (anzahl == 1 && ziffern != null) {
                gefunden += ZertifiziertePolynomLoesung(listOf(i), listOf(ziffern), verlauf, vielfachheit(i)); continue
            }
            // Exakte Dezimalgrenzen verhindern endloses Verfeinern um rationale Wurzeln.
            val skala = BigInteger.TEN.pow(konfiguration.stellen)
            val unten = a.l.zähler * skala / a.l.nenner
            val oben = a.r.zähler * skala / a.r.nenner
            val grenzkandidat = if (oben - unten <= BigInteger.TWO) (0..2).map { unten + BigInteger.valueOf(it.toLong()) }.filter { it <= oben }.firstOrNull { z ->
                val q = RationaleZahl.von(z, skala)
                q > a.l && q < a.r && p.wert(q).istNull()
            }?.let { RationaleZahl.von(it, skala) } else null
            if (anzahl == 1 && grenzkandidat != null) {
                val punkt = RationalesIntervall(grenzkandidat)
                gefunden += ZertifiziertePolynomLoesung(listOf(punkt), listOf(abgeschnitteneDezimalzahl(grenzkandidat, konfiguration.stellen)),
                    verlauf + PolynomIterationsSchritt(listOf(punkt), "Exaktes Einsetzen beweist die Dezimalgrenze."), vielfachheit(punkt)); continue
            }
            var mitte = (a.l + a.r) / pQ(2)
            // Teilungsgrenzen dürfen keine Wurzeln sein; rationale Wurzeln separat bei Einwurzelintervallen.
            var teiler = 3
            while (p.wert(mitte).istNull()) { budget.schritt(); mitte = a.l + (a.r - a.l) / pQ(teiler++) }
            queue.addFirst(Aufgabe(mitte, a.r, verlauf)); queue.addFirst(Aufgabe(a.l, mitte, verlauf))
        }
        PolynomLoesungsErgebnis(gefunden.sortedBy { it.komponenten.single().minimum },
            if (gefunden.isEmpty()) LoesungsNachweis.Widerlegt else LoesungsNachweis.Bewiesen,
            LoesungsNachweis.Bewiesen, if (gefunden.isEmpty()) null else 0,
            analytisch = listOf("Exakte algebraische Wurzeln: Polynom und rationales Isolationsintervall.",
                "Vielfachheiten durch ggT mit der Ableitung; vollständige Wurzelzählung durch Sturm."))
    } catch (_: PolynomBudgetEnde) {
        PolynomLoesungsErgebnis(gefunden, if (gefunden.isEmpty()) LoesungsNachweis.Offen else LoesungsNachweis.Bewiesen,
            hinweis = "Rechenbudget erschöpft; Vollständigkeit und weitere Ziffern bleiben offen.")
    }
}

data class KomplexeWurzelScheibe(val zentrum: KomplexRational, val radius: RationaleZahl)
