package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Rekursive Parametrisierung einer Einheitsrichtung in R^n.
 *
 * Vorzeichen s in {-1,1}, n-1 Winkel in [0,180) Grad:
 * u_1=(s), u_(k+1)=(cos(a_k)*u_k, s*sin(a_k)).
 * Damit sind auch R2 (voller Kreis) und alle höheren Sphären abgedeckt.
 * An den Polen ist die Parametrisierung nicht eindeutig.
 */
fun richtungAusHalbkreiswinkeln(vorzeichen: Int, winkelGrad: List<Double>): List<Double> {
    require(vorzeichen == -1 || vorzeichen == 1) { "Richtungsvorzeichen muss -1 oder +1 sein." }
    require(winkelGrad.size <= 31) { "Maximal 32 Dimensionen sind zulässig." }
    require(winkelGrad.all { it.isFinite() && it >= 0.0 && it < 180.0 }) {
        "Jeder Halbkreiswinkel muss in [0°,180°) liegen."
    }
    var koordinaten = listOf(vorzeichen.toDouble())
    winkelGrad.forEach { grad ->
        val rad = grad * (PI / 180.0)
        val c = when (grad) { 90.0 -> 0.0; 0.0 -> 1.0; else -> cos(rad) }
        val sinus = when (grad) { 90.0 -> 1.0; 0.0 -> 0.0; else -> sin(rad) }
        koordinaten = koordinaten.map { it * c } + (vorzeichen * sinus)
    }
    val laenge = sqrt(koordinaten.sumOf { it * it })
    require(laenge.isFinite() && laenge > 0.0)
    return koordinaten.map { if (it == 0.0) 0.0 else it / laenge }
}

/** Gibt einen orientierten Einheitsvektor zurück; keine Radiusinformation. */
fun richtungAlsSpaltenVektor(vorzeichen: Int, winkelGrad: List<Double>): SpaltenVektor =
    SpaltenVektor(richtungAusHalbkreiswinkeln(vorzeichen, winkelGrad).map(::rationaleKoordinate))
