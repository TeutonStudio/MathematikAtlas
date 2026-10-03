package de.TeutonStudio.MathematikRechenSystem.kern

/** Geschlossene einschließende rationale Schranken; keine Gleitkommatoleranz. */
data class RationalesIntervall(val minimum: RationaleZahl, val maximum: RationaleZahl = minimum) {
    init { require(minimum <= maximum) }
    val punkt: Boolean get() = minimum == maximum
    operator fun plus(b: RationalesIntervall) = RationalesIntervall(minimum + b.minimum, maximum + b.maximum)
    operator fun unaryMinus() = RationalesIntervall(-maximum, -minimum)
    operator fun minus(b: RationalesIntervall) = this + -b
    operator fun times(b: RationalesIntervall): RationalesIntervall {
        val produkte = listOf(minimum * b.minimum, minimum * b.maximum, maximum * b.minimum, maximum * b.maximum)
        return RationalesIntervall(produkte.minOrNull()!!, produkte.maxOrNull()!!)
    }
    fun enthältNull() = minimum <= RationaleZahl.Null && maximum >= RationaleZahl.Null
}

sealed interface IntervallNachweis {
    data class Schranke(val intervall: RationalesIntervall) : IntervallNachweis
    data class Offen(val grund: String) : IntervallNachweis
}

/** Pro Auftrag geteilt; der Aufrufer kann hier z.B. Coroutine-Abbruch prüfen. */
class AuswertungsBudget(val maximum: Int, private val abbruchPrüfen: () -> Unit = {}) {
    init { require(maximum > 0) }
    var verbraucht: Int = 0
        private set
    fun schritt(): Boolean {
        abbruchPrüfen()
        if (verbraucht >= maximum) return false
        verbraucht++
        return true
    }
}

/** Zertifikate betreffen alle Belegungen in den übergebenen Intervallen. */
object ZertifizierterIntervallAuswerter {
    fun wert(
        ausdruck: ZahlAusdruck,
        umgebung: Map<String, RationalesIntervall>,
        budget: AuswertungsBudget = AuswertungsBudget(10_000),
    ): IntervallNachweis {
        fun offen(grund: String) = IntervallNachweis.Offen(grund)
        if (!budget.schritt()) return offen("Auswertungsbudget ausgeschöpft.")
        fun eval(z: ZahlAusdruck) = wert(z, umgebung, budget)
        fun kombiniere(a: ZahlAusdruck, b: ZahlAusdruck, op: (RationalesIntervall, RationalesIntervall) -> IntervallNachweis): IntervallNachweis {
            val l = eval(a); if (l is IntervallNachweis.Offen) return l
            val r = eval(b); if (r is IntervallNachweis.Offen) return r
            return op((l as IntervallNachweis.Schranke).intervall, (r as IntervallNachweis.Schranke).intervall)
        }
        fun schranke(i: RationalesIntervall): IntervallNachweis = if (listOf(i.minimum, i.maximum).any {
            it.zähler.bitLength() > 4096 || it.nenner.bitLength() > 4096
        }) offen("Exakte Zahlen überschreiten das Präzisionsbudget.") else IntervallNachweis.Schranke(i)
        fun liste(zahlen: List<ZahlAusdruck>, op: (RationalesIntervall, RationalesIntervall) -> RationalesIntervall): IntervallNachweis {
            var result: RationalesIntervall? = null
            for (z in zahlen) {
                val n = eval(z); if (n is IntervallNachweis.Offen) return n
                val i = (n as IntervallNachweis.Schranke).intervall
                val next = schranke(result?.let { op(it, i) } ?: i)
                if (next is IntervallNachweis.Offen) return next
                result = (next as IntervallNachweis.Schranke).intervall
            }
            return result?.let(::schranke) ?: offen("Leere Operandenliste.")
        }
        return when (ausdruck) {
            is RationaleZahl -> schranke(RationalesIntervall(ausdruck))
            is Variable -> umgebung[ausdruck.name]?.let(::schranke) ?: offen("Fehlende Bindung: ${ausdruck.name}.")
            is Addition -> liste(ausdruck.summanden) { a, b -> a + b }
            is Multiplikation -> liste(ausdruck.faktoren) { a, b -> a * b }
            is Maximum -> liste(ausdruck.operanden) { a, b -> RationalesIntervall(maxOf(a.minimum, b.minimum), maxOf(a.maximum, b.maximum)) }
            is Minimum -> liste(ausdruck.operanden) { a, b -> RationalesIntervall(minOf(a.minimum, b.minimum), minOf(a.maximum, b.maximum)) }
            is Division -> kombiniere(ausdruck.dividend, ausdruck.divisor) { a, b ->
                if (b.enthältNull()) offen("Nenner enthält null; Definitionslage nicht für die gesamte Zelle gesichert.")
                else schranke(a * RationalesIntervall(RationaleZahl.Eins / b.maximum, RationaleZahl.Eins / b.minimum))
            }
            is Potenz -> kombiniere(ausdruck.basis, ausdruck.exponent) { a, b ->
                val n = b.minimum
                if (!b.punkt || n.nenner != java.math.BigInteger.ONE || n.zähler.abs() > java.math.BigInteger.valueOf(256)) {
                    offen("Nur konstante ganzzahlige Potenzen bis Betrag 256 sind hier zertifiziert.")
                } else if (n.zähler.signum() <= 0 && a.enthältNull()) {
                    offen("Nichtpositive Potenz mit möglicher Nullbasis.")
                } else {
                    val exponent = n.zähler.abs().toInt()
                    fun potenz(x: RationaleZahl): RationaleZahl = RationaleZahl.von(x.zähler.pow(exponent), x.nenner.pow(exponent))
                    // Begrenze vor BigInteger.pow, nicht erst nach einer riesigen Allokation.
                    if (maxOf(a.minimum.zähler.bitLength(), a.maximum.zähler.bitLength(), a.minimum.nenner.bitLength(), a.maximum.nenner.bitLength()).toLong() * exponent > 4096) {
                        offen("Potenz überschreitet das Präzisionsbudget.")
                    } else {
                        val l = potenz(a.minimum); val r = potenz(a.maximum)
                        val positiv = RationalesIntervall(if (exponent % 2 == 0 && a.enthältNull()) RationaleZahl.Null else minOf(l, r), maxOf(l, r))
                        if (n.zähler.signum() < 0) schranke(RationalesIntervall(RationaleZahl.Eins / positiv.maximum, RationaleZahl.Eins / positiv.minimum))
                        else schranke(positiv)
                    }
                }
            }
            is Betrag -> when (val a = eval(ausdruck.argument)) {
                is IntervallNachweis.Offen -> a
                is IntervallNachweis.Schranke -> {
                    val i = a.intervall
                    schranke(when {
                        i.minimum >= RationaleZahl.Null -> i
                        i.maximum <= RationaleZahl.Null -> -i
                        else -> RationalesIntervall(RationaleZahl.Null, maxOf(-i.minimum, i.maximum))
                    })
                }
            }
            is ZahlFallAusdruck -> when (aussage(ausdruck.aussage, umgebung, budget).wahrheitswert) {
                Wahrheitswert.Wahr -> eval(ausdruck.wahr)
                Wahrheitswert.Lüge -> eval(ausdruck.lüge)
                null -> offen("Fallbedingung ist nicht auf der gesamten Zelle entschieden.")
            }
            else -> offen("${ausdruck::class.simpleName} besitzt hier keinen Intervallnachweis.")
        }
    }

    fun aussage(
        aussage: Aussage,
        umgebung: Map<String, RationalesIntervall>,
        budget: AuswertungsBudget = AuswertungsBudget(10_000),
    ): AussageErgebnis {
        if (!budget.schritt()) return unbekannt("Auswertungsbudget ausgeschöpft.")
        fun eval(a: Aussage) = aussage(a, umgebung, budget)
        return when (aussage) {
            is WahrheitsKonstante -> entschieden(aussage.wert)
            is Negation -> negiere(eval(aussage.aussage))
            is Konjunktion -> alle(aussage.aussagen.map(::eval))
            is Disjunktion -> mindestensEine(aussage.aussagen.map(::eval))
            is Implikation -> mindestensEine(listOf(negiere(eval(aussage.voraussetzung)), eval(aussage.folgerung)))
            is Äquivalenz -> {
                val a = eval(aussage.links); val b = eval(aussage.rechts)
                if (a.wahrheitswert != null && b.wahrheitswert != null) entschieden(a.wahrheitswert == b.wahrheitswert)
                else unbekannt("Äquivalenz ist nicht auf der ganzen Zelle entschieden.")
            }
            is Ungleichheit -> negiere(eval(Gleichheit(aussage.links, aussage.rechts)))
            is Gleichheit, is Vergleich -> {
                val links = (if (aussage is Gleichheit) aussage.links else (aussage as Vergleich).links) as? ZahlAusdruck
                val rechts = (if (aussage is Gleichheit) aussage.rechts else (aussage as Vergleich).rechts) as? ZahlAusdruck
                if (links == null || rechts == null) return unbekannt("Kein skalarer Vergleich.")
                val a = wert(links, umgebung, budget); val b = wert(rechts, umgebung, budget)
                if (a is IntervallNachweis.Offen) return unbekannt(a.grund)
                if (b is IntervallNachweis.Offen) return unbekannt(b.grund)
                val l = (a as IntervallNachweis.Schranke).intervall; val r = (b as IntervallNachweis.Schranke).intervall
                if (aussage is Gleichheit) {
                    when {
                        links == rechts -> entschieden(true) // beide Ausdrücke sind überall definiert
                        l.punkt && r.punkt -> entschieden(l.minimum == r.minimum)
                        l.maximum < r.minimum || r.maximum < l.minimum -> entschieden(false)
                        else -> unbekannt("Gleichheit innerhalb der Zelle nicht entschieden.")
                    }
                } else when ((aussage as Vergleich).art) {
                    VergleichsArt.Kleiner -> trenne(l.maximum < r.minimum, l.minimum >= r.maximum)
                    VergleichsArt.KleinerGleich -> trenne(l.maximum <= r.minimum, l.minimum > r.maximum)
                    VergleichsArt.Größer -> trenne(l.minimum > r.maximum, l.maximum <= r.minimum)
                    VergleichsArt.GrößerGleich -> trenne(l.minimum >= r.maximum, l.maximum < r.minimum)
                }
            }
            else -> unbekannt("${aussage::class.simpleName} besitzt hier keinen Zellnachweis.")
        }
    }

    internal fun entschieden(wahr: Boolean) = AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
    internal fun unbekannt(grund: String) = AussageErgebnis(null, EntscheidungsStatus.Unbekannt, grund)
    internal fun trenne(wahr: Boolean, falsch: Boolean) = when { wahr -> entschieden(true); falsch -> entschieden(false); else -> unbekannt("Zellgrenzen erlauben keine einheitliche Entscheidung.") }
    internal fun negiere(a: AussageErgebnis) = a.wahrheitswert?.let { entschieden(it == Wahrheitswert.Lüge) } ?: a
    internal fun alle(a: List<AussageErgebnis>): AussageErgebnis = when {
        a.any { it.wahrheitswert == Wahrheitswert.Lüge } -> entschieden(false)
        a.all { it.wahrheitswert == Wahrheitswert.Wahr } -> entschieden(true)
        else -> a.first { it.wahrheitswert == null }
    }
    internal fun mindestensEine(a: List<AussageErgebnis>): AussageErgebnis = when {
        a.any { it.wahrheitswert == Wahrheitswert.Wahr } -> entschieden(true)
        a.all { it.wahrheitswert == Wahrheitswert.Lüge } -> entschieden(false)
        else -> a.first { it.wahrheitswert == null }
    }
}
