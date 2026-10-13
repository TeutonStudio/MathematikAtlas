package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.*

/** Arbeitsgrenzen sind Teil jeder numerischen Anfrage. Fehlerabschätzungen sind keine Einschließungsbeweise. */
data class MethodenNumerikOptionen(
    val absoluteToleranz: Double = 1e-8,
    val relativeToleranz: Double = 1e-8,
    val maximaleAuswertungen: Int = 10_000,
    val maximaleIterationen: Int = 100,
    val maximaleTiefe: Int = 20,
) {
    init {
        require(absoluteToleranz.isFinite() && absoluteToleranz > 0)
        require(relativeToleranz.isFinite() && relativeToleranz >= 0)
        require(maximaleAuswertungen > 0 && maximaleIterationen > 0 && maximaleTiefe in 1..30)
    }
    internal fun toleranz(wert: Double) = absoluteToleranz + relativeToleranz * abs(wert)
}

sealed interface MethodenNumerikErgebnis<out T> {
    data class Approximation<T>(val wert: T, val fehlerSchaetzung: Double, val auswertungen: Int) : MethodenNumerikErgebnis<T>
    data class Fehler(val nachricht: String) : MethodenNumerikErgebnis<Nothing>
}

private class MethodenNumerikLauf(val methode: MathematischeMethode, val optionen: MethodenNumerikOptionen) {
    var auswertungen = 0
    fun wert(punkt: List<Double>): List<Double> {
        require(punkt.size == methode.parameter.size && punkt.all { it.isFinite() }) { "Ungültiger numerischer Argumentpunkt." }
        check(++auswertungen <= optionen.maximaleAuswertungen) { "Numerisches Auswertungsbudget erschöpft." }
        val argumente = Tupel(punkt.map { parseRationaleEingabe(java.math.BigDecimal.valueOf(it).toPlainString()) })
        val anwendung = methode.wendeMitBedingungenAn(argumente)
        check(anwendung.bedingungen.isEmpty()) { "Definitionsbedingungen am numerischen Stützpunkt bleiben offen." }
        return anwendung.ergebnis.elemente.map { wert ->
            val zahl = wert as? ZahlAusdruck ?: error("Numerik benötigt skalare reelle Ergebniskomponenten.")
            val ergebnis = NumerischerAuswerter.wert(zahl)
            val reell = (ergebnis as? NumerischesErgebnis.Wert)?.wert ?: error("Methodenergebnis ist nicht reell numerisch auswertbar.")
            check(reell.isFinite()) { "Numerisches Ergebnis ist nicht endlich." }
            reell
        }
    }

    fun jacobi(punkt: List<Double>): Pair<List<List<Double>>, Double> {
        val basis = wert(punkt)
        var fehler = 0.0
        val spalten = punkt.indices.map { achse ->
            var schritt = Math.cbrt(Math.ulp(1.0)) * max(1.0, abs(punkt[achse]))
            var vorher: List<Double>? = null
            var akzeptiert: List<Double>? = null
            repeat(12) {
                if (akzeptiert == null) {
                    val plusPunkt = punkt.toMutableList().also { it[achse] += schritt }
                    val minusPunkt = punkt.toMutableList().also { it[achse] -= schritt }
                    check(plusPunkt[achse] != punkt[achse] && minusPunkt[achse] != punkt[achse]) { "Differenzenschritt ist numerisch nicht mehr auflösbar." }
                    val plus = wert(plusPunkt); val minus = wert(minusPunkt)
                    val zentral = basis.indices.map { i -> (plus[i] - minus[i]) / (2 * schritt) }
                    val alt = vorher
                    if (alt != null) {
                        val delta = zentral.indices.maxOfOrNull { i -> abs(zentral[i] - alt[i]) } ?: 0.0
                        val seiten = basis.indices.maxOfOrNull { i -> abs((plus[i] - basis[i]) / schritt - (basis[i] - minus[i]) / schritt) } ?: 0.0
                        val toleranz = optionen.toleranz(zentral.maxOfOrNull { abs(it) } ?: 0.0)
                        if (delta <= toleranz && seiten <= sqrt(toleranz)) {
                            akzeptiert = zentral
                            fehler = max(fehler, delta)
                        }
                    }
                    vorher = zentral
                    schritt /= 2
                }
            }
            akzeptiert ?: error("Numerische Ableitung stabilisiert sich nicht oder besitzt widersprüchliche einseitige Differenzen.")
        }
        return basis.indices.map { zeile -> spalten.map { it[zeile] } } to fehler
    }
}

private inline fun <T> numerik(block: () -> MethodenNumerikErgebnis.Approximation<T>): MethodenNumerikErgebnis<T> =
    try { block() } catch (e: IllegalArgumentException) { MethodenNumerikErgebnis.Fehler(e.message ?: "Ungültige Numerikanfrage.") }
    catch (e: IllegalStateException) { MethodenNumerikErgebnis.Fehler(e.message ?: "Numerik nicht auswertbar.") }

fun MathematischeMethode.jacobiNumerisch(
    punkt: List<Double>, optionen: MethodenNumerikOptionen = MethodenNumerikOptionen(),
): MethodenNumerikErgebnis<List<List<Double>>> = numerik {
    val lauf = MethodenNumerikLauf(this, optionen)
    val (matrix, fehler) = lauf.jacobi(punkt)
    MethodenNumerikErgebnis.Approximation(matrix, fehler, lauf.auswertungen)
}

/** Lokaler Kandidat innerhalb einer expliziten Box. Kein Beweis einer globalen Umkehrmethode. */
fun MathematischeMethode.invertiereLokalNumerisch(
    ziel: List<Double>, start: List<Double>, unten: List<Double>, oben: List<Double>,
    optionen: MethodenNumerikOptionen = MethodenNumerikOptionen(),
): MethodenNumerikErgebnis<List<Double>> = numerik {
    val n = parameter.size
    require(n in 1..16 && ziel.size == n && start.size == n && unten.size == n && oben.size == n)
    require((ziel + start + unten + oben).all { it.isFinite() })
    require(unten.indices.all { unten[it] < oben[it] && start[it] in unten[it]..oben[it] })
    val lauf = MethodenNumerikLauf(this, optionen)
    var punkt = start
    fun residuum(p: List<Double>): List<Double> {
        val wert = lauf.wert(p); require(wert.size == n) { "Lokale Inversion benötigt eine quadratische Signatur." }
        return wert.indices.map { wert[it] - ziel[it] }
    }
    fun norm(r: List<Double>) = r.maxOf { abs(it) }
    var rest = residuum(punkt)
    repeat(optionen.maximaleIterationen) {
        if (norm(rest) <= optionen.toleranz(ziel.maxOf { abs(it) })) return@numerik MethodenNumerikErgebnis.Approximation(punkt, norm(rest), lauf.auswertungen)
        val (jacobi, _) = lauf.jacobi(punkt)
        val inverse = Matrix(jacobi.map { zeile -> zeile.map { parseRationaleEingabe(java.math.BigDecimal.valueOf(it).toPlainString()) } }).inverseRational()
        val richtung = inverse.zeilen.map { zeile -> zeile.indices.sumOf { i -> (zeile[i] as RationaleZahl).zuDezimal().toDouble() * rest[i] } }
        check(richtung.all { it.isFinite() }) { "Newton-Richtung ist nicht endlich." }
        var faktor = 1.0
        var neu: List<Double>? = null
        var neuerRest: List<Double>? = null
        repeat(24) {
            if (neu == null) {
                val kandidat = punkt.indices.map { punkt[it] - faktor * richtung[it] }
                if (kandidat.indices.all { kandidat[it] in unten[it]..oben[it] }) {
                    val r = residuum(kandidat)
                    if (norm(r) < norm(rest)) { neu = kandidat; neuerRest = r }
                }
                faktor /= 2
            }
        }
        punkt = neu ?: error("Gedämpftes Newton-Verfahren findet keinen zulässigen Verbesserungsschritt.")
        rest = neuerRest!!
    }
    error("Lokale Inversion erreicht die Toleranz nicht innerhalb des Iterationsbudgets.")
}

/** Adaptive Simpson-Quadratur über ein endliches Intervall, komponentenweise. */
fun MathematischeMethode.integriereBestimmtNumerisch(
    argumentId: String, restArgumente: Map<String, Double>, links: Double, rechts: Double,
    optionen: MethodenNumerikOptionen = MethodenNumerikOptionen(),
): MethodenNumerikErgebnis<List<Double>> = numerik {
    require(links.isFinite() && rechts.isFinite())
    val achse = parameter.indexOfFirst { argumentIdentitäten.getValue(it.name) == argumentId }
    require(achse >= 0)
    val restIds = parameter.filterIndexed { i, _ -> i != achse }.map { argumentIdentitäten.getValue(it.name) }.toSet()
    require(restArgumente.keys == restIds && restArgumente.values.all { it.isFinite() })
    val lauf = MethodenNumerikLauf(this, optionen)
    fun wert(x: Double) = lauf.wert(parameter.mapIndexed { i, p -> if (i == achse) x else restArgumente.getValue(argumentIdentitäten.getValue(p.name)) })
    fun simpson(a: Double, b: Double, fa: List<Double>, fm: List<Double>, fb: List<Double>) = fa.indices.map { (b - a) / 6 * (fa[it] + 4 * fm[it] + fb[it]) }
    fun quadratur(a: Double, b: Double, fa: List<Double>, fm: List<Double>, fb: List<Double>, grob: List<Double>, budget: Double, tiefe: Int): Pair<List<Double>, Double> {
        val mitte = a / 2 + b / 2
        check(mitte != a && mitte != b) { "Quadraturintervall ist numerisch nicht weiter teilbar." }
        val fl = wert(a / 2 + mitte / 2); val fr = wert(mitte / 2 + b / 2)
        val l = simpson(a, mitte, fa, fl, fm); val r = simpson(mitte, b, fm, fr, fb)
        val fein = l.indices.map { l[it] + r[it] }
        val delta = fein.indices.map { (fein[it] - grob[it]) / 15 }
        val fehler = delta.maxOfOrNull { abs(it) } ?: 0.0
        if (fehler <= budget) return fein.indices.map { fein[it] + delta[it] } to fehler
        check(tiefe < optionen.maximaleTiefe) { "Quadratur erreicht die Toleranz nicht innerhalb der maximalen Tiefe." }
        val (il, el) = quadratur(a, mitte, fa, fl, fm, l, budget / 2, tiefe + 1)
        val (ir, er) = quadratur(mitte, b, fm, fr, fb, r, budget / 2, tiefe + 1)
        return il.indices.map { il[it] + ir[it] } to (el + er)
    }
    if (links == rechts) return@numerik MethodenNumerikErgebnis.Approximation(List(ausgabeNamen.size) { 0.0 }, 0.0, 0)
    val a = min(links, rechts); val b = max(links, rechts)
    val fa = wert(a); val fm = wert(a / 2 + b / 2); val fb = wert(b)
    val grob = simpson(a, b, fa, fm, fb)
    val (integral, fehler) = quadratur(a, b, fa, fm, fb, grob, optionen.toleranz(grob.maxOfOrNull { abs(it) } ?: 0.0), 0)
    val vorzeichen = if (rechts > links) 1.0 else -1.0
    check(integral.all { it.isFinite() } && fehler.isFinite()) { "Quadratur ist numerisch übergelaufen." }
    MethodenNumerikErgebnis.Approximation(integral.map { vorzeichen * it }, fehler, lauf.auswertungen)
}
