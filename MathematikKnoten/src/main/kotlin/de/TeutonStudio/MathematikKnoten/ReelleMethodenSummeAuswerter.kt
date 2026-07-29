package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.util.Locale
import kotlin.math.*

internal enum class ReelleSummenArt { Untersumme, Obersumme }
internal data class ReellerSummenBalken(val links: Double, val rechts: Double, val höhe: Double)
internal data class ReelleSummenBerechnung(
    val art: ReelleSummenArt,
    val minimum: Double,
    val maximum: Double,
    val balken: List<ReellerSummenBalken>,
    val wert: Double,
)

/** Ober- und Untersummen für einstellige reelle Methoden. */
internal fun MathematikAuswerterRegister.registriereReelleMethodenSumme() {
    registriere("mathematik.reelleMethodenSumme") { k ->
        val methodenWert = k.eingänge["methode"] ?: error("Eine Methode muss verbunden sein.")
        val methode = methodenWert.objekt as? Funktion ?: error("Der Methodeneingang enthält keine Methode.")
        val parameter = methode.parameter.singleOrNull() as? Variable
            ?: error("Die Methodensumme benötigt genau einen reellen Zahlenparameter.")
        val (ausgabeName, ausgabe) = methode.einzigeAusgabe()
        val term = ausgabe as? ZahlAusdruck ?: error("Die Methode muss eine Zahl ausgeben.")
        val definitionsmenge = methode.werteVorräte[parameter.name]
            ?: error("Für den Methodenparameter fehlt ein Wertevorrat.")
        val zielmenge = methode.zielMengeFür(ausgabeName)
        require(definitionsmenge.istReelleTeilmenge()) { "Der Wertevorrat der Methode muss eine Teilmenge von ℝ sein." }
        require(zielmenge.istReelleTeilmenge()) { "Die Zielmenge der Methode muss eine Teilmenge von ℝ sein." }

        val partitionenWert = k.eingänge["partitionen"] ?: error("Die Partitionsanzahl muss verbunden sein.")
        val partitionenZahl = vereinfache(
            partitionenWert.objekt as? ZahlAusdruck ?: error("Die Partitionsanzahl muss eine Zahl sein."),
            k.rechenKontext,
        ) as? RationaleZahl ?: error("Die Partitionsanzahl muss eine konkrete natürliche Zahl sein.")
        require(partitionenZahl.nenner == java.math.BigInteger.ONE && partitionenZahl.zähler.signum() > 0 && partitionenZahl.zähler.bitLength() < 31) {
            "Die Partitionsanzahl muss in ℕ liegen; 0 ist nicht zulässig."
        }
        val partitionen = partitionenZahl.zähler.toInt()

        val (minimum, maximum) = grenzen(k)
        require(minimum.isFinite() && maximum.isFinite() && minimum < maximum) {
            "Das Intervall benötigt endliche Grenzen mit Minimum < Maximum."
        }
        val art = if (k.knoten.parameter["summenArt"] == "obersumme") ReelleSummenArt.Obersumme else ReelleSummenArt.Untersumme
        val berechnung = berechneReelleSumme(term, parameter.name, minimum, maximum, partitionen, art)
        val wert = rationaleNäherung(berechnung.wert)
        val eingänge = k.eingänge.values
        val annahmen = eingänge.flatMap { it.annahmen }.toSet()
        val symbol = if (art == ReelleSummenArt.Untersumme) "\\underline{S}" else "\\overline{S}"
        val formel = "$symbol_{${partitionen}}(${methode.name};[${format(minimum)},${format(maximum)}]) \\approx ${format(berechnung.wert)}"
        KnotenAuswertungsErgebnis(mapOf(
            "wert" to BedingterWert(
                objekt = wert,
                annahmen = annahmen,
                zielMenge = ReelleZahlen,
                reelleVariablen = reelleVariablen(eingänge),
                variablenQuellen = eingänge.flatMap { it.variablenQuellen }.distinctBy {
                    Pair(Triple(it.knotenId, it.name, it.werteVorrat), it.alsMethodenParameter)
                },
                latexDarstellung = formel,
            ),
            "visualisierung" to BedingterWert(berechnung.zuTupel()),
        ))
    }
}

private fun grenzen(k: KnotenAuswertungsKontext): Pair<Double, Double> {
    if (k.knoten.parameter["bereichsArt"] == "intervall") {
        val intervall = k.eingänge["intervall"]?.objekt as? ReellesIntervall
            ?: error("Im Intervallmodus muss ein reelles Intervall verbunden sein.")
        val minimum = numerischerMethodenWert(intervall.untereGrenze, emptyMap())
            ?: error("Die untere Intervallgrenze ist nicht numerisch auswertbar.")
        val maximum = numerischerMethodenWert(intervall.obereGrenze, emptyMap())
            ?: error("Die obere Intervallgrenze ist nicht numerisch auswertbar.")
        return minimum to maximum
    }
    val minimum = numerischerMethodenWert(k.eingänge["minimum"]?.objekt as? ZahlAusdruck, emptyMap())
        ?: error("Das Minimum muss als konkrete reelle Zahl verbunden sein.")
    val maximum = numerischerMethodenWert(k.eingänge["maximum"]?.objekt as? ZahlAusdruck, emptyMap())
        ?: error("Das Maximum muss als konkrete reelle Zahl verbunden sein.")
    return minimum to maximum
}

internal fun berechneReelleSumme(
    term: ZahlAusdruck,
    parameter: String,
    minimum: Double,
    maximum: Double,
    partitionen: Int,
    art: ReelleSummenArt,
    stützstellenJeTeilintervall: Int = 33,
): ReelleSummenBerechnung {
    require(partitionen > 0)
    require(stützstellenJeTeilintervall >= 2)
    val breite = (maximum - minimum) / partitionen
    val balken = List(partitionen) { index ->
        val links = minimum + index * breite
        val rechts = links + breite
        val werte = List(stützstellenJeTeilintervall) { sample ->
            val x = links + (rechts - links) * sample.toDouble() / (stützstellenJeTeilintervall - 1)
            numerischerMethodenWert(term, mapOf(parameter to x))
                ?.takeIf(Double::isFinite)
                ?: error("Die Methode ist bei x=${format(x)} nicht reell numerisch auswertbar.")
        }
        ReellerSummenBalken(links, rechts, if (art == ReelleSummenArt.Untersumme) werte.min() else werte.max())
    }
    return ReelleSummenBerechnung(art, minimum, maximum, balken, breite * balken.sumOf { it.höhe })
}

internal fun numerischerMethodenWert(ausdruck: ZahlAusdruck?, werte: Map<String, Double>): Double? = when (ausdruck) {
    null -> null
    is ZahlFallAusdruck -> when (ausdruck.aussage.entscheide(RechenKontext()).wahrheitswert) {
        Wahrheitswert.Wahr -> numerischerMethodenWert(ausdruck.wahr, werte)
        Wahrheitswert.Lüge -> numerischerMethodenWert(ausdruck.lüge, werte)
        null -> null
    }
    is RationaleZahl -> ausdruck.zuDezimal(18).toDouble()
    is Variable -> werte[ausdruck.name]
    is MathematischeKonstante -> when (ausdruck) { Pi -> Math.PI; EulerscheZahl -> Math.E; else -> null }
    is Addition -> ausdruck.summanden.map { numerischerMethodenWert(it, werte) }.takeUnless { it.any { n -> n == null } }?.sumOf { it!! }
    is Multiplikation -> ausdruck.faktoren.map { numerischerMethodenWert(it, werte) }.takeUnless { it.any { n -> n == null } }?.fold(1.0) { acc, n -> acc * n!! }
    is Maximum -> ausdruck.operanden.mapNotNull { numerischerMethodenWert(it, werte) }.takeIf { it.size == ausdruck.operanden.size }?.maxOrNull()
    is Minimum -> ausdruck.operanden.mapNotNull { numerischerMethodenWert(it, werte) }.takeIf { it.size == ausdruck.operanden.size }?.minOrNull()
    is Division -> numerischerMethodenWert(ausdruck.dividend, werte)?.let { a -> numerischerMethodenWert(ausdruck.divisor, werte)?.takeIf { abs(it) > 1e-12 }?.let { a / it } }
    is Potenz -> numerischerMethodenWert(ausdruck.basis, werte)?.let { a -> numerischerMethodenWert(ausdruck.exponent, werte)?.let { a.pow(it) } }
    is Betrag -> numerischerMethodenWert(ausdruck.argument, werte)?.let(::abs)
    is Sinus -> numerischerMethodenWert(ausdruck.argument, werte)?.let(::sin)
    is Cosinus -> numerischerMethodenWert(ausdruck.argument, werte)?.let(::cos)
    is Exponentialfunktion -> numerischerMethodenWert(ausdruck.argument, werte)?.let(::exp)
    is NatürlicherLogarithmus -> numerischerMethodenWert(ausdruck.argument, werte)?.takeIf { it > 0 }?.let(::ln)
    is Logarithmus -> {
        val basis = numerischerMethodenWert(ausdruck.basis, werte)
        val argument = numerischerMethodenWert(ausdruck.argument, werte)
        if (basis == null || argument == null || basis <= 0 || basis == 1.0 || argument <= 0) null else ln(argument) / ln(basis)
    }
    is Wurzel -> numerischerMethodenWert(ausdruck.argument, werte)?.takeIf { it >= 0 }?.let(::sqrt)
    is KomplexeZahl, is Argument, is IterierteSumme, is IteriertesProdukt -> null
}

private fun MengenAusdruck.istReelleTeilmenge(): Boolean = when (this) {
    NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen, is ReellesIntervall -> true
    is EndlicheMenge -> elemente.all { it is ZahlAusdruck && istNachweisbarReell(it) }
    is Vereinigung -> mengen.all { it.istReelleTeilmenge() }
    is Schnitt -> mengen.all { it.istReelleTeilmenge() }
    is MengenDifferenz -> links.istReelleTeilmenge()
    else -> false
}

private fun ReelleSummenBerechnung.zuTupel(): Tupel = Tupel(balken.map { balkenWert ->
    Tupel(listOf(rationaleNäherung(balkenWert.links), rationaleNäherung(balkenWert.rechts), rationaleNäherung(balkenWert.höhe)))
})

private fun rationaleNäherung(wert: Double): RationaleZahl {
    val faktor = 1_000_000L
    return RationaleZahl.von((wert * faktor).roundToLong(), faktor)
}

private fun format(wert: Double): String = String.format(Locale.ROOT, "%.8g", wert)
