package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.*

/** Numerische Hypersphärenkoordinaten in Grad: (r, (φ, θ₁, …)), Azimut zuerst.
 * R¹ verwendet (|x|, (sign(x))). Alle Ausgaben sind Zweiertupel (Radius, Richtungs-Tupel).
 * Die Ergebnisse sind Gleitkommanäherungen.
 */
object Polarkoordinaten {
    const val KONVENTION = "Grad: Azimut φ in [0°,360°), Polarwinkel θ₁,… in [0°,180°]. " +
        "θ₁ wird zur x₃-Achse gemessen, θ₂ zur x₄-Achse usw. " +
        "R¹ verwendet ein einelementiges Richtungstupel mit Vorzeichen −1, 0 oder +1."

    fun polar(kartesisch: Tupel): Tupel {
        val x = komponenten(kartesisch)
        require(x.isNotEmpty()) { "Polarkoordinaten benötigen mindestens eine Dimension." }
        if (x.size == 1) return Tupel(listOf(zahl(abs(x.single())), Tupel(listOf(zahl(sign(x.single()))))))
        val winkel = mutableListOf<Double>()
        val azimut = if (x[0] == 0.0 && x[1] == 0.0) 0.0 else Math.toDegrees(atan2(x[1], x[0]))
        // Eine Addition von 360 kann einen sehr kleinen negativen Winkel auf 360 runden.
        winkel += if (azimut < 0.0) (azimut + 360.0).let { if (it >= 360.0) Math.nextDown(360.0) else it } else azimut
        var norm = hypot(x[0], x[1])
        for (i in 2 until x.size) {
            winkel += if (norm == 0.0 && x[i] == 0.0) 0.0 else Math.toDegrees(atan2(norm, x[i]))
            norm = hypot(norm, x[i])
        }
        require(norm.isFinite()) { "Der Radius überschreitet den numerisch darstellbaren Bereich." }
        return Tupel(listOf(zahl(norm), Tupel(winkel.map { GradWinkel(zahl(it)) })))
    }

    fun radius(kartesisch: Tupel): ZahlAusdruck = polar(kartesisch).elemente.first() as ZahlAusdruck

    fun winkelOderVorzeichen(kartesisch: Tupel): MathematischesObjekt {
        return polar(kartesisch).elemente[1]
    }

    fun kartesisch(polar: Tupel, eindimensional: Boolean = false): Tupel {
        require(polar.elemente.size == 2) { "Polar besteht aus genau (Radius, Richtungstupel)." }
        val r = polar.elemente.first() as? ZahlAusdruck ?: error("Der Radius muss eine reelle Zahl sein.")
        val richtung = polar.elemente[1]
        if (eindimensional || richtung is Tupel && richtung.elemente.size == 1 &&
            richtung.elemente.single() is ZahlAusdruck &&
            runCatching { reell(richtung.elemente.single() as ZahlAusdruck) }.getOrNull() in setOf(-1.0, 0.0, 1.0)) {
            return kartesisch(r, if (richtung is Tupel) richtung.elemente.single() else richtung)
        }
        // Historische Polartupel (r, Gradwert) bleiben für R² lesbar.
        return kartesisch(r, if (richtung is Tupel) richtung else Tupel(listOf(richtung)))
    }

    fun kartesisch(radius: ZahlAusdruck, winkelOderVorzeichen: MathematischesObjekt): Tupel {
        val r = reell(radius)
        require(r >= 0.0) { "Der Radius muss nichtnegativ sein." }
        if (winkelOderVorzeichen is ZahlAusdruck) {
            val s = reell(winkelOderVorzeichen)
            require(s == -1.0 || s == 0.0 || s == 1.0) { "R¹ benötigt das Vorzeichen −1, 0 oder +1." }
            require(s != 0.0 || r == 0.0) { "Vorzeichen 0 ist nur bei Radius 0 gültig." }
            return Tupel(listOf(zahl(r * s)))
        }
        val winkel = (winkelOderVorzeichen as? Tupel ?: error("Erwartet wird ein Winkeltupel oder skalares Vorzeichen.")).elemente.map { w ->
            when (w) {
                is GradWinkel -> reell(w.grad)
                is WinkelAusdruck -> Math.toDegrees(reell(winkelBogenmass(w)))
                is ZahlAusdruck -> reell(w) // Historische Karten speichern Gradrepräsentanten als Zahlen.
                else -> error("Ein Polarwinkel muss ein Winkel sein.")
            }
        }
        require(winkel.isNotEmpty()) { "Ein Winkeltupel muss mindestens den Azimut enthalten." }
        require(winkel.first() >= 0.0 && winkel.first() < 360.0) { "Der Azimut muss in [0°,360°) liegen." }
        require(winkel.drop(1).all { it >= 0.0 && it <= 180.0 }) { "Polarwinkel müssen in [0°,180°] liegen." }
        val x = MutableList(winkel.size + 1) { 0.0 }
        var teilRadius = r
        for (i in winkel.lastIndex downTo 1) {
            x[i + 1] = teilRadius * cosGrad(winkel[i])
            teilRadius *= sinGrad(winkel[i])
        }
        x[0] = teilRadius * cosGrad(winkel[0])
        x[1] = teilRadius * sinGrad(winkel[0])
        return Tupel(x.map(::zahl))
    }

    private fun sinGrad(w: Double) = when (w) {
        0.0, 180.0 -> 0.0
        90.0 -> 1.0
        270.0 -> -1.0
        else -> sin(Math.toRadians(w))
    }

    private fun cosGrad(w: Double) = when (w) {
        0.0 -> 1.0
        180.0 -> -1.0
        90.0, 270.0 -> 0.0
        else -> cos(Math.toRadians(w))
    }

    private fun komponenten(t: Tupel) = t.elemente.mapIndexed { i, element ->
        reell(element as? ZahlAusdruck ?: error("Koordinate ${i + 1} muss eine reelle Zahl sein."))
    }

    private fun reell(z: ZahlAusdruck): Double {
        val ergebnis = DomaenenAuswerter.wert(z)
        val wert = (ergebnis as? DomaenenErgebnis.Wert)?.wert as? DomaenenWert.Reell
            ?: error("Koordinate ist nicht als endliche reelle Zahl bestimmbar: ${z.zuLatex()}.")
        val d = wert.wert.toDouble()
        require(d.isFinite() && (d != 0.0 || wert.wert.signum() == 0)) {
            "Koordinate liegt außerhalb des numerisch darstellbaren Bereichs."
        }
        return d
    }

    private fun zahl(d: Double): ZahlAusdruck = rationaleKoordinate(if (d == 0.0) 0.0 else d)
}
