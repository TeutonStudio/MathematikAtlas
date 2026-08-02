package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Maschinenlesbare Ursache mit lokalem Ausdruckspfad. */
data class StrukturierterAuswertungsGrund(
    val code: String,
    val nachricht: String,
    val pfad: List<String> = emptyList(),
    val details: Map<String, String> = emptyMap(),
) {
    fun unter(segment: String): StrukturierterAuswertungsGrund = copy(pfad = listOf(segment) + pfad)
}

/** Domänenerhaltende Wertfamilie für R, C und H. */
sealed interface DomaenenWert {
    data class Reell(val wert: BigDecimal) : DomaenenWert
    data class Komplex(val reell: BigDecimal, val imaginaer: BigDecimal) : DomaenenWert
    data class Quaternion(
        val reell: BigDecimal,
        val i: BigDecimal,
        val j: BigDecimal,
        val k: BigDecimal,
    ) : DomaenenWert
}

sealed interface DomaenenErgebnis {
    data class Wert(val wert: DomaenenWert) : DomaenenErgebnis
    data class NichtDefiniert(val grund: StrukturierterAuswertungsGrund) : DomaenenErgebnis
    data class NichtEndlich(val grund: StrukturierterAuswertungsGrund) : DomaenenErgebnis
    data class NichtUnterstuetzt(val grund: StrukturierterAuswertungsGrund) : DomaenenErgebnis
    data class Unentscheidbar(val grund: StrukturierterAuswertungsGrund) : DomaenenErgebnis
}

sealed interface ReelleKoordinateErgebnis {
    data class Wert(val wert: BigDecimal) : ReelleKoordinateErgebnis
    data class ProjektionErforderlich(val grund: StrukturierterAuswertungsGrund) : ReelleKoordinateErgebnis
}

fun DomaenenWert.alsReelleKoordinate(
    reellEingebetteteKomplexeZulassen: Boolean = true,
): ReelleKoordinateErgebnis = when (this) {
    is DomaenenWert.Reell -> ReelleKoordinateErgebnis.Wert(wert)
    is DomaenenWert.Komplex -> if (
        reellEingebetteteKomplexeZulassen && imaginaer.compareTo(BigDecimal.ZERO) == 0
    ) {
        ReelleKoordinateErgebnis.Wert(reell)
    } else {
        ReelleKoordinateErgebnis.ProjektionErforderlich(
            StrukturierterAuswertungsGrund(
                "komplexe_projektion_erforderlich",
                "Eine komplexe Zahl ist ohne explizite Abbildung keine reelle Koordinate.",
            ),
        )
    }
    is DomaenenWert.Quaternion -> ReelleKoordinateErgebnis.ProjektionErforderlich(
        StrukturierterAuswertungsGrund(
            "quaternion_projektion_erforderlich",
            "Ein Quaternion benötigt eine explizite R⁴-Darstellung oder Projektion.",
        ),
    )
}

data class DomaenenKontext(
    val variablen: Map<String, DomaenenWert> = emptyMap(),
    val mathContext: MathContext = MathContext.DECIMAL128,
    val maxKnoten: Int = 10_000,
    val maxTiefe: Int = 256,
)

/**
 * Neuer domänenerhaltender Auswertungspfad. Der bisherige Double-Adapter bleibt
 * vorübergehend als Kompatibilitätsschicht bestehen und kann auf diesen Dienst
 * migriert werden.
 */
object DomaenenAuswerter {
    fun wert(
        ausdruck: ZahlAusdruck,
        kontext: DomaenenKontext = DomaenenKontext(),
    ): DomaenenErgebnis = Lauf(kontext).werte(ausdruck, 0)

    private class Lauf(private val kontext: DomaenenKontext) {
        private var besucht = 0

        fun werte(ausdruck: ZahlAusdruck, tiefe: Int): DomaenenErgebnis {
            if (++besucht > kontext.maxKnoten) return nichtDefiniert(
                "budget_ueberschritten",
                "Das Auswertungsbudget von ${kontext.maxKnoten} Ausdrucksknoten wurde überschritten.",
            )
            if (tiefe > kontext.maxTiefe) return nichtDefiniert(
                "maximale_tiefe",
                "Die maximale Auswertungstiefe von ${kontext.maxTiefe} wurde überschritten.",
            )
            val ergebnis = when (ausdruck) {
                is RationaleZahl -> DomaenenErgebnis.Wert(
                    DomaenenWert.Reell(
                        BigDecimal(ausdruck.zähler).divide(BigDecimal(ausdruck.nenner), kontext.mathContext),
                    ),
                )
                is Variable -> kontext.variablen[ausdruck.name]?.let(DomaenenErgebnis::Wert)
                    ?: DomaenenErgebnis.Unentscheidbar(
                        StrukturierterAuswertungsGrund(
                            "variable_unbelegt",
                            "Für die Variable ${ausdruck.name} fehlt eine numerische Belegung.",
                        ),
                    )
                is MathematischeKonstante -> when (ausdruck) {
                    Pi -> reell(Math.PI)
                    EulerscheZahl -> reell(Math.E)
                    else -> nichtUnterstuetzt("Die Konstante ${ausdruck.name} besitzt keinen registrierten Wert.")
                }
                is Addition -> reduziere(ausdruck.summanden, tiefe, ::addiere)
                is Multiplikation -> reduziere(ausdruck.faktoren, tiefe, ::multipliziere)
                is Division -> binaer(ausdruck.dividend, ausdruck.divisor, tiefe, ::dividiere)
                is Potenz -> potenz(ausdruck, tiefe)
                is Betrag -> unaer(ausdruck.argument, tiefe, ::betrag)
                is Sinus -> reelleFunktion(ausdruck.argument, tiefe, "sin", ::sin)
                is Cosinus -> reelleFunktion(ausdruck.argument, tiefe, "cos", ::cos)
                is ArcSinus -> reelleFunktionMitBereich(ausdruck.argument, tiefe, "arcsin", -1.0, 1.0, ::asin)
                is ArcCosinus -> reelleFunktionMitBereich(ausdruck.argument, tiefe, "arccos", -1.0, 1.0, ::acos)
                is Exponentialfunktion -> reelleFunktion(ausdruck.argument, tiefe, "exp", ::exp)
                is NatürlicherLogarithmus -> reelleFunktionMitPruefung(
                    ausdruck.argument,
                    tiefe,
                    "ln",
                    { it > 0.0 },
                    ::ln,
                )
                is Wurzel -> wurzel(ausdruck.argument, tiefe)
                is KomplexeZahl -> komplex(ausdruck, tiefe)
                is Argument -> argument(ausdruck, tiefe)
                is Logarithmus -> logarithmus(ausdruck, tiefe)
                else -> nichtUnterstuetzt(
                    "Die Ausdrucksart ${ausdruck::class.simpleName} wird domänenerhaltend noch nicht unterstützt.",
                )
            }
            return ergebnis.mitPfad(ausdruck::class.simpleName ?: "Ausdruck")
        }

        private fun reduziere(
            ausdruecke: List<ZahlAusdruck>,
            tiefe: Int,
            operation: (DomaenenWert, DomaenenWert) -> DomaenenErgebnis,
        ): DomaenenErgebnis {
            if (ausdruecke.isEmpty()) return nichtDefiniert("leere_operation", "Eine leere Operation ist nicht definiert.")
            var akk: DomaenenWert? = null
            ausdruecke.forEach { ausdruck ->
                val aktueller = werte(ausdruck, tiefe + 1)
                if (aktueller !is DomaenenErgebnis.Wert) return aktueller
                akk = if (akk == null) aktueller.wert else when (val ergebnis = operation(akk!!, aktueller.wert)) {
                    is DomaenenErgebnis.Wert -> ergebnis.wert
                    else -> return ergebnis
                }
            }
            return DomaenenErgebnis.Wert(requireNotNull(akk))
        }

        private fun binaer(
            links: ZahlAusdruck,
            rechts: ZahlAusdruck,
            tiefe: Int,
            operation: (DomaenenWert, DomaenenWert) -> DomaenenErgebnis,
        ): DomaenenErgebnis {
            val l = werte(links, tiefe + 1)
            if (l !is DomaenenErgebnis.Wert) return l
            val r = werte(rechts, tiefe + 1)
            if (r !is DomaenenErgebnis.Wert) return r
            return operation(l.wert, r.wert)
        }

        private fun unaer(
            argument: ZahlAusdruck,
            tiefe: Int,
            operation: (DomaenenWert) -> DomaenenErgebnis,
        ): DomaenenErgebnis {
            val wert = werte(argument, tiefe + 1)
            return if (wert is DomaenenErgebnis.Wert) operation(wert.wert) else wert
        }

        private fun komplex(ausdruck: KomplexeZahl, tiefe: Int): DomaenenErgebnis {
            val re = werte(ausdruck.realteil, tiefe + 1)
            if (re !is DomaenenErgebnis.Wert) return re
            val im = werte(ausdruck.imaginärteil, tiefe + 1)
            if (im !is DomaenenErgebnis.Wert) return im
            val r = re.wert.alsReellOderNull()
                ?: return nichtUnterstuetzt("Der Realteil einer komplexen Zahl muss reell sein.")
            val i = im.wert.alsReellOderNull()
                ?: return nichtUnterstuetzt("Der Imaginärteil einer komplexen Zahl muss reell sein.")
            return DomaenenErgebnis.Wert(DomaenenWert.Komplex(r, i))
        }

        private fun argument(ausdruck: Argument, tiefe: Int): DomaenenErgebnis = unaer(ausdruck.zahl, tiefe) { wert ->
            val komplex = wert.alsKomplexOderNull()
                ?: return@unaer nichtUnterstuetzt("Das Argument ist für echte Quaternionen nicht registriert.")
            if (komplex.reell.signum() == 0 && komplex.imaginaer.signum() == 0) {
                nichtDefiniert("argument_von_null", "Das Argument der Zahl 0 ist nicht definiert.")
            } else {
                reell(atan2(komplex.imaginaer.toDouble(), komplex.reell.toDouble()))
            }
        }

        private fun logarithmus(ausdruck: Logarithmus, tiefe: Int): DomaenenErgebnis {
            val basis = werte(ausdruck.basis, tiefe + 1)
            if (basis !is DomaenenErgebnis.Wert) return basis
            val argument = werte(ausdruck.argument, tiefe + 1)
            if (argument !is DomaenenErgebnis.Wert) return argument
            val b = basis.wert.alsReellOderNull()?.toDouble()
                ?: return nichtUnterstuetzt("Komplexe Logarithmusbasen benötigen eine eigene Definition.")
            val a = argument.wert.alsReellOderNull()?.toDouble()
                ?: return nichtUnterstuetzt("Komplexe Logarithmen benötigen eine eigene Definition.")
            if (b <= 0.0 || b == 1.0 || a <= 0.0) return nichtDefiniert(
                "logarithmus_definitionsbereich",
                "Logarithmus verlangt eine positive Basis ungleich 1 und ein positives Argument.",
            )
            return reell(ln(a) / ln(b))
        }

        private fun potenz(ausdruck: Potenz, tiefe: Int): DomaenenErgebnis {
            val basis = werte(ausdruck.basis, tiefe + 1)
            if (basis !is DomaenenErgebnis.Wert) return basis
            val exponent = werte(ausdruck.exponent, tiefe + 1)
            if (exponent !is DomaenenErgebnis.Wert) return exponent
            val e = exponent.wert.alsReellOderNull()?.toDouble()
                ?: return nichtUnterstuetzt("Nichtreelle Exponenten sind noch nicht registriert.")
            if (e % 1.0 == 0.0 && abs(e) <= 4096) return ganzzahligePotenz(basis.wert, e.toInt())
            val b = basis.wert.alsReellOderNull()?.toDouble()
                ?: return nichtUnterstuetzt("Nichtganzzahlige Potenzen sind derzeit nur reell unterstützt.")
            if (b < 0.0) return nichtUnterstuetzt("Die komplexe Fortsetzung dieser Potenz ist nicht registriert.")
            return reell(b.pow(e))
        }

        private fun ganzzahligePotenz(basis: DomaenenWert, exponent: Int): DomaenenErgebnis {
            if (exponent == 0) return DomaenenErgebnis.Wert(DomaenenWert.Reell(BigDecimal.ONE))
            if (exponent < 0) {
                val positiv = ganzzahligePotenz(basis, -exponent)
                return if (positiv is DomaenenErgebnis.Wert) {
                    dividiere(DomaenenWert.Reell(BigDecimal.ONE), positiv.wert)
                } else positiv
            }
            var ergebnis: DomaenenWert = DomaenenWert.Reell(BigDecimal.ONE)
            repeat(exponent) {
                when (val produkt = multipliziere(ergebnis, basis)) {
                    is DomaenenErgebnis.Wert -> ergebnis = produkt.wert
                    else -> return produkt
                }
            }
            return DomaenenErgebnis.Wert(ergebnis)
        }

        private fun wurzel(argument: ZahlAusdruck, tiefe: Int): DomaenenErgebnis = unaer(argument, tiefe) { wert ->
            val reell = wert.alsReellOderNull()?.toDouble()
                ?: return@unaer nichtUnterstuetzt("Die Wurzel ist für diesen Zahlbereich nicht registriert.")
            if (reell >= 0.0) reell(sqrt(reell))
            else DomaenenErgebnis.Wert(DomaenenWert.Komplex(BigDecimal.ZERO, dezimal(sqrt(-reell))))
        }

        private fun reelleFunktion(
            argument: ZahlAusdruck,
            tiefe: Int,
            name: String,
            funktion: (Double) -> Double,
        ): DomaenenErgebnis = reelleFunktionMitPruefung(argument, tiefe, name, { true }, funktion)

        private fun reelleFunktionMitBereich(
            argument: ZahlAusdruck,
            tiefe: Int,
            name: String,
            minimum: Double,
            maximum: Double,
            funktion: (Double) -> Double,
        ): DomaenenErgebnis = reelleFunktionMitPruefung(
            argument,
            tiefe,
            name,
            { it in minimum..maximum },
            funktion,
        )

        private fun reelleFunktionMitPruefung(
            argument: ZahlAusdruck,
            tiefe: Int,
            name: String,
            pruefung: (Double) -> Boolean,
            funktion: (Double) -> Double,
        ): DomaenenErgebnis = unaer(argument, tiefe) { wert ->
            val reell = wert.alsReellOderNull()?.toDouble()
                ?: return@unaer nichtUnterstuetzt("$name ist für nichtreelle Werte nicht registriert.")
            if (!pruefung(reell)) return@unaer nichtDefiniert(
                "definitionsbereich_$name",
                "$name ist für den gegebenen reellen Wert nicht definiert.",
            )
            reell(funktion(reell))
        }

        private fun betrag(wert: DomaenenWert): DomaenenErgebnis = when (wert) {
            is DomaenenWert.Reell -> DomaenenErgebnis.Wert(DomaenenWert.Reell(wert.wert.abs()))
            is DomaenenWert.Komplex -> reell(
                sqrt(wert.reell.toDouble().pow(2) + wert.imaginaer.toDouble().pow(2)),
            )
            is DomaenenWert.Quaternion -> reell(
                sqrt(
                    wert.reell.toDouble().pow(2) + wert.i.toDouble().pow(2) +
                        wert.j.toDouble().pow(2) + wert.k.toDouble().pow(2),
                ),
            )
        }

        private fun addiere(a: DomaenenWert, b: DomaenenWert): DomaenenErgebnis = DomaenenErgebnis.Wert(
            when (maxOf(a.rang(), b.rang())) {
                0 -> DomaenenWert.Reell(a.reellTeil().add(b.reellTeil(), kontext.mathContext))
                1 -> {
                    val x = requireNotNull(a.alsKomplexOderNull())
                    val y = requireNotNull(b.alsKomplexOderNull())
                    DomaenenWert.Komplex(
                        x.reell.add(y.reell, kontext.mathContext),
                        x.imaginaer.add(y.imaginaer, kontext.mathContext),
                    )
                }
                else -> {
                    val x = a.alsQuaternion()
                    val y = b.alsQuaternion()
                    DomaenenWert.Quaternion(
                        x.reell.add(y.reell, kontext.mathContext),
                        x.i.add(y.i, kontext.mathContext),
                        x.j.add(y.j, kontext.mathContext),
                        x.k.add(y.k, kontext.mathContext),
                    )
                }
            },
        )

        private fun multipliziere(a: DomaenenWert, b: DomaenenWert): DomaenenErgebnis = DomaenenErgebnis.Wert(
            when (maxOf(a.rang(), b.rang())) {
                0 -> DomaenenWert.Reell(a.reellTeil().multiply(b.reellTeil(), kontext.mathContext))
                1 -> {
                    val x = requireNotNull(a.alsKomplexOderNull())
                    val y = requireNotNull(b.alsKomplexOderNull())
                    DomaenenWert.Komplex(
                        x.reell.multiply(y.reell, kontext.mathContext)
                            .subtract(x.imaginaer.multiply(y.imaginaer, kontext.mathContext), kontext.mathContext),
                        x.reell.multiply(y.imaginaer, kontext.mathContext)
                            .add(x.imaginaer.multiply(y.reell, kontext.mathContext), kontext.mathContext),
                    )
                }
                else -> hamiltonProdukt(a.alsQuaternion(), b.alsQuaternion())
            },
        )

        private fun hamiltonProdukt(a: DomaenenWert.Quaternion, b: DomaenenWert.Quaternion): DomaenenWert.Quaternion {
            fun mal(x: BigDecimal, y: BigDecimal) = x.multiply(y, kontext.mathContext)
            fun plus(vararg werte: BigDecimal) = werte.fold(BigDecimal.ZERO) { akk, wert ->
                akk.add(wert, kontext.mathContext)
            }
            return DomaenenWert.Quaternion(
                plus(mal(a.reell, b.reell), mal(a.i, b.i).negate(), mal(a.j, b.j).negate(), mal(a.k, b.k).negate()),
                plus(mal(a.reell, b.i), mal(a.i, b.reell), mal(a.j, b.k), mal(a.k, b.j).negate()),
                plus(mal(a.reell, b.j), mal(a.i, b.k).negate(), mal(a.j, b.reell), mal(a.k, b.i)),
                plus(mal(a.reell, b.k), mal(a.i, b.j), mal(a.j, b.i).negate(), mal(a.k, b.reell)),
            )
        }

        private fun dividiere(a: DomaenenWert, b: DomaenenWert): DomaenenErgebnis {
            if (b.istNull()) return nichtDefiniert("division_durch_null", "Division durch null ist nicht definiert.")
            val invers = when (b) {
                is DomaenenWert.Reell -> DomaenenWert.Reell(BigDecimal.ONE.divide(b.wert, kontext.mathContext))
                is DomaenenWert.Komplex -> {
                    val norm = b.reell.multiply(b.reell).add(b.imaginaer.multiply(b.imaginaer))
                    DomaenenWert.Komplex(
                        b.reell.divide(norm, kontext.mathContext),
                        b.imaginaer.negate().divide(norm, kontext.mathContext),
                    )
                }
                is DomaenenWert.Quaternion -> {
                    val norm = listOf(b.reell, b.i, b.j, b.k)
                        .map { it.multiply(it, kontext.mathContext) }
                        .fold(BigDecimal.ZERO) { akk, wert -> akk.add(wert, kontext.mathContext) }
                    DomaenenWert.Quaternion(
                        b.reell.divide(norm, kontext.mathContext),
                        b.i.negate().divide(norm, kontext.mathContext),
                        b.j.negate().divide(norm, kontext.mathContext),
                        b.k.negate().divide(norm, kontext.mathContext),
                    )
                }
            }
            return multipliziere(a, invers)
        }

        private fun reell(wert: Double): DomaenenErgebnis {
            if (!wert.isFinite()) return DomaenenErgebnis.NichtEndlich(
                StrukturierterAuswertungsGrund("nicht_endlich", "Die Auswertung ergab keinen endlichen Wert."),
            )
            return DomaenenErgebnis.Wert(DomaenenWert.Reell(dezimal(wert)))
        }

        private fun dezimal(wert: Double): BigDecimal = BigDecimal.valueOf(wert).round(kontext.mathContext)
        private fun nichtDefiniert(code: String, nachricht: String) = DomaenenErgebnis.NichtDefiniert(
            StrukturierterAuswertungsGrund(code, nachricht),
        )
        private fun nichtUnterstuetzt(nachricht: String) = DomaenenErgebnis.NichtUnterstuetzt(
            StrukturierterAuswertungsGrund("nicht_unterstuetzt", nachricht),
        )
    }
}

private fun DomaenenWert.rang(): Int = when (this) {
    is DomaenenWert.Reell -> 0
    is DomaenenWert.Komplex -> 1
    is DomaenenWert.Quaternion -> 2
}

private fun DomaenenWert.reellTeil(): BigDecimal = when (this) {
    is DomaenenWert.Reell -> wert
    is DomaenenWert.Komplex -> reell
    is DomaenenWert.Quaternion -> reell
}

private fun DomaenenWert.alsReellOderNull(): BigDecimal? = when (this) {
    is DomaenenWert.Reell -> wert
    is DomaenenWert.Komplex -> reell.takeIf { imaginaer.signum() == 0 }
    is DomaenenWert.Quaternion -> reell.takeIf { i.signum() == 0 && j.signum() == 0 && k.signum() == 0 }
}

private fun DomaenenWert.alsKomplexOderNull(): DomaenenWert.Komplex? = when (this) {
    is DomaenenWert.Reell -> DomaenenWert.Komplex(wert, BigDecimal.ZERO)
    is DomaenenWert.Komplex -> this
    is DomaenenWert.Quaternion -> if (j.signum() == 0 && k.signum() == 0) {
        DomaenenWert.Komplex(reell, i)
    } else null
}

private fun DomaenenWert.alsQuaternion(): DomaenenWert.Quaternion = when (this) {
    is DomaenenWert.Reell -> DomaenenWert.Quaternion(wert, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
    is DomaenenWert.Komplex -> DomaenenWert.Quaternion(reell, imaginaer, BigDecimal.ZERO, BigDecimal.ZERO)
    is DomaenenWert.Quaternion -> this
}

private fun DomaenenWert.istNull(): Boolean = when (this) {
    is DomaenenWert.Reell -> wert.signum() == 0
    is DomaenenWert.Komplex -> reell.signum() == 0 && imaginaer.signum() == 0
    is DomaenenWert.Quaternion -> reell.signum() == 0 && i.signum() == 0 && j.signum() == 0 && k.signum() == 0
}

private fun DomaenenErgebnis.mitPfad(segment: String): DomaenenErgebnis = when (this) {
    is DomaenenErgebnis.Wert -> this
    is DomaenenErgebnis.NichtDefiniert -> copy(grund = grund.unter(segment))
    is DomaenenErgebnis.NichtEndlich -> copy(grund = grund.unter(segment))
    is DomaenenErgebnis.NichtUnterstuetzt -> copy(grund = grund.unter(segment))
    is DomaenenErgebnis.Unentscheidbar -> copy(grund = grund.unter(segment))
}
