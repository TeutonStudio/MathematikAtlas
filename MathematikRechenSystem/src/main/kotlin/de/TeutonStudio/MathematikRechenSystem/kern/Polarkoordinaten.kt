package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.*

enum class PolarKonvention { CLASSIC, SIGNED }

/** Numerische Hypersphärenkoordinaten in Grad: (r, (φ, θ₁, …)), Azimut zuerst.
 * R¹ verwendet (|x|, (sign(x))). Alle Ausgaben sind Zweiertupel (Radius, Richtungs-Tupel).
 * Die Ergebnisse sind Gleitkommanäherungen.
 */
object Polarkoordinaten {
    const val KONVENTION = "Grad: Azimut φ in [0°,360°), Polarwinkel θ₁,… in [0°,180°]. " +
        "θ₁ wird zur x₃-Achse gemessen, θ₂ zur x₄-Achse usw. " +
        "R¹ verwendet ein einelementiges Richtungstupel mit Vorzeichen −1, 0 oder +1."

    fun polar(kartesisch: Tupel, konvention: PolarKonvention = PolarKonvention.CLASSIC): Tupel {
        if(konvention==PolarKonvention.SIGNED) return polarSigniert(kartesisch)
        val x = komponenten(kartesisch)
        require(x.size<=MAXIMALE_RICHTUNGS_WINKEL+1) { "Die Koordinatendimension überschreitet die gemeinsame Richtungsgrenze." }
        require(x.isNotEmpty()) { "Polarkoordinaten benötigen mindestens eine Dimension." }
        if (x.size == 1) return Tupel(listOf(zahl(abs(x.single())), zahl(sign(x.single()))))
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
        require(polar.elemente.size>=2) { "Polartupel benötigt Radius und Winkel bzw. Vorzeichen." }
        val radius=polar.elemente.first() as? ZahlAusdruck ?: error("Der Radius muss eine reelle Zahl sein.")
        if(eindimensional) {
            require(polar.elemente.size==2) { "R¹ benötigt genau Radius und Vorzeichen." }
            val richtung=polar.elemente[1]
            return kartesisch(radius,if(richtung is Tupel && richtung.elemente.size==1) richtung.elemente.single() else richtung)
        }
        val winkel=if(polar.elemente.size==2 && polar.elemente[1] is Tupel) polar.elemente[1] else Tupel(polar.elemente.drop(1))
        return kartesisch(radius,winkel)
    }

    fun kartesisch(polar:Tupel,konvention:PolarKonvention,eindimensional:Boolean=false):Tupel =
        if(konvention==PolarKonvention.SIGNED) {
            require(polar.elemente.size==2) { "Signierte Polarkoordinaten bestehen aus Radius und Richtungstupel." }
            kartesischAusRichtung(polar.elemente[0] as? ZahlAusdruck ?: error("Radius muss eine Zahl sein."),polar.elemente[1] as? Tupel ?: error("Eine Wn-Richtung wird benötigt."))
        } else if(eindimensional) kartesisch(polar,true) else kartesischMitVariablen(polar)

    /** Kanonische Inverse der Vorzeichen-Halbkreisparametrisierung einschließlich aller Quadranten. */
    private fun polarSigniert(kartesisch:Tupel):Tupel {
        val x=komponenten(kartesisch)
        require(x.size in 1..MAXIMALE_RICHTUNGS_WINKEL+1) { "Die Koordinatendimension liegt außerhalb der gemeinsamen Richtungsgrenze." }
        val radius=x.fold(0.0) { a,b -> hypot(a,b) }
        require(radius.isFinite()) { "Der Radius überschreitet den numerisch darstellbaren Bereich." }
        val s=if(x.lastOrNull { it!=0.0 }?.let { it<0 }==true) -1.0 else 1.0
        fun winkel(v:List<Double>):List<Double> {
            if(v.size==1) return emptyList()
            val prefix=v.dropLast(1)
            val norm=prefix.fold(0.0) { a,b -> hypot(a,b) }
            val letzte=prefix.lastOrNull { it!=0.0 }
            val c=if(letzte!=null && sign(letzte)!=s) -1.0 else 1.0
            val alpha=if(norm==0.0 && v.last()==0.0) 0.0 else Math.toDegrees(atan2(s*v.last(),c*norm)).coerceAtLeast(0.0).let { if(it>=180.0) Math.nextDown(180.0) else it }
            return winkel(prefix.map { c*it })+alpha
        }
        val richtung=Tupel(listOf(zahl(s))+winkel(x).map { GradWinkel(zahl(it)) })
        return Tupel(listOf(zahl(radius),richtung))
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
        require(winkel.isNotEmpty() && winkel.size<=MAXIMALE_RICHTUNGS_WINKEL) { "Die Winkelanzahl liegt außerhalb der gemeinsamen Richtungsgrenze." }
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



    /** Richtungsmodell {-1,+1} × [0°,180°)^n, Ergebnis in R^(n+1). */
    fun kartesischAusRichtung(radius: ZahlAusdruck, richtung: Tupel): Tupel {
        require(richtung.elemente.size in 1..MAXIMALE_RICHTUNGS_WINKEL+1) { "Eine Richtung benötigt Vorzeichen und höchstens $MAXIMALE_RICHTUNGS_WINKEL Winkel." }
        fun konkret(z:ZahlAusdruck):Double? = when(val wert=DomaenenAuswerter.wert(z)) {
            is DomaenenErgebnis.Wert -> (wert.wert as? DomaenenWert.Reell)?.wert?.toDouble()?.also { require(it.isFinite()) { "Richtungswerte müssen endlich und reell sein." } } ?: error("Richtungswerte müssen reell sein.")
            is DomaenenErgebnis.NichtDefiniert,is DomaenenErgebnis.NichtEndlich -> error("Richtungswerte müssen endlich und definiert sein.")
            else -> null
        }
        konkret(radius)?.let { require(it>=0) { "Der Radius muss nichtnegativ sein." } }
        val vorzeichen=richtung.elemente.first() as? ZahlAusdruck ?: error("Das Richtungsvorzeichen muss eine Zahl sein.")
        konkret(vorzeichen)?.let { require(it== -1.0 || it==1.0) { "Das Richtungsvorzeichen muss -1 oder +1 sein." } }
        val winkel=richtung.elemente.drop(1).map { wert ->
            val grad=when(wert) { is GradWinkel -> wert.grad; is ZahlAusdruck -> wert; is WinkelAusdruck -> Division(multiplikation(winkelBogenmass(wert),RationaleZahl.von(180)),Pi); else -> error("Halbkreiswinkel müssen reelle Gradwerte sein.") }
            konkret(grad)?.let { require(it>=0 && it<180) { "Halbkreiswinkel müssen in [0°,180°) liegen." } }
            gradZuBogenmass(grad)
        }
        var koordinaten:List<ZahlAusdruck> = listOf(vorzeichen)
        for(w in winkel) koordinaten=koordinaten.map { multiplikation(it,Cosinus(w)) }+multiplikation(vorzeichen,Sinus(w))
        return Tupel(koordinaten.map { vereinfache(multiplikation(radius,it)) })
    }

    /** Parametrisierung für Methoden mit freien reellen Winkelvariablen in Grad. */
    fun kartesischSymbolisch(polar: Tupel): Tupel {
        require(polar.elemente.size == 2) { "Polar benötigt Radius und Winkel-Tupel." }
        val radius = polar.elemente[0] as? ZahlAusdruck
            ?: error("Der Radius muss ein Zahlausdruck sein.")
        val richtung = polar.elemente[1] as? Tupel
            ?: error("Die Richtung muss ein Winkel-Tupel sein.")
        require(richtung.elemente.isNotEmpty()) { "Mindestens ein Winkel ist erforderlich." }
        val winkelRad = richtung.elemente.map { winkel ->
            when (winkel) {
                is ZahlAusdruck -> gradZuBogenmass(winkel)
                is WinkelAusdruck -> winkelBogenmass(winkel)
                else -> error("Alle Winkel müssen Grad-Zahlausdrücke oder Gradwinkel sein.")
            }
        }
        val komponenten = MutableList<ZahlAusdruck>(winkelRad.size + 1) { RationaleZahl.Null }
        var teilRadius = radius
        for (i in winkelRad.lastIndex downTo 1) {
            komponenten[i + 1] = multiplikation(teilRadius, Cosinus(winkelRad[i]))
            teilRadius = multiplikation(teilRadius, Sinus(winkelRad[i]))
        }
        komponenten[0] = multiplikation(teilRadius, Cosinus(winkelRad[0]))
        komponenten[1] = multiplikation(teilRadius, Sinus(winkelRad[0]))
        return Tupel(komponenten)
    }

    /** Nur bei nicht numerisch entscheidbaren Größen wird die symbolische Form benutzt. */
    fun kartesischMitVariablen(polar: Tupel): Tupel {
        val richtung = polar.elemente.getOrNull(1) as? Tupel
        fun symbolisch(objekt: MathematischesObjekt): Boolean = when (objekt) {
            is GradWinkel -> DomaenenAuswerter.wert(objekt.grad) !is DomaenenErgebnis.Wert
            is ZahlAusdruck -> DomaenenAuswerter.wert(objekt) !is DomaenenErgebnis.Wert
            is WinkelAusdruck -> true
            else -> false
        }
        return if (richtung != null && (
            polar.elemente.firstOrNull()?.let(::symbolisch) == true ||
            richtung.elemente.any(::symbolisch)
        )) kartesischSymbolisch(polar) else kartesisch(polar)
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
