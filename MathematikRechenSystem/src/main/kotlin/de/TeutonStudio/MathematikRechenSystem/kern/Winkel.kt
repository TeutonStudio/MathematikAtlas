package de.TeutonStudio.MathematikRechenSystem.kern

/** Additive Winkelgruppe; keine Teilmenge des reellen Zahlenkörpers. */
data object Gradmenge : MengenAusdruck {
    override fun zuLatex() = "\\mathbb{G}"
}

interface WinkelAusdruck : MathematischesObjekt

/** Freie Parameter sind hier reell gebunden; bekannte nichtreelle/undefinierte Vertreter sind ungültig. */
private fun prüfeReellenWinkelVertreter(z: ZahlAusdruck) {
    when (val wert = DomaenenAuswerter.wert(z)) {
        is DomaenenErgebnis.Wert -> require(wert.wert is DomaenenWert.Reell) { "Ein Winkelrepräsentant muss reell sein." }
        is DomaenenErgebnis.NichtDefiniert, is DomaenenErgebnis.NichtEndlich -> error("Ein Winkelrepräsentant muss endlich und definiert sein.")
        else -> Unit // Symbolische reelle Parameter bleiben erlaubt.
    }
}

/** Ein Gradrepräsentant, rational exakt modulo 360 normalisiert. */
class GradWinkel(grad: ZahlAusdruck) : WinkelAusdruck {
    val grad: ZahlAusdruck = vereinfache(grad).let { normal ->
        prüfeReellenWinkelVertreter(normal)
        if (normal is RationaleZahl) RationaleZahl.von(normal.zähler.mod(normal.nenner * java.math.BigInteger.valueOf(360)), normal.nenner)
        else normal
    }
    override fun zuLatex() = "[${grad.zuLatex()}]^{\\circ}"
    override fun equals(other: Any?) = other is GradWinkel && grad == other.grad
    override fun hashCode() = grad.hashCode()
}

data class WinkelParameter(override val name: String) : WinkelAusdruck, MethodenParameter {
    override fun zuLatex() = name
}

/** Die Quotientenabbildung q: R → Grad. Freie reelle Parameter bleiben substituierbar. */
data class WinkelAusBogenmass(val argument: ZahlAusdruck) : WinkelAusdruck {
    init { prüfeReellenWinkelVertreter(argument) }
    override fun zuLatex() = "q\\left(${argument.zuLatex()}\\right)"
}

enum class WinkelVertreter(val id: String) {
    POSITIV("positiv"), SYMMETRISCH("symmetrisch");
    val menge: ReellesIntervall get() = when (this) {
        POSITIV -> ReellesIntervall(RationaleZahl.Null, false, multiplikation(RationaleZahl.von(2), Pi), true)
        SYMMETRISCH -> ReellesIntervall(negation(Pi), false, Pi, true)
    }
    companion object { fun vonId(id: String?) = entries.firstOrNull { it.id == id } ?: POSITIV }
}

data class WinkelBogenmass(val winkel: WinkelAusdruck, val vertreter: WinkelVertreter = WinkelVertreter.POSITIV) : ZahlAusdruck {
    override fun zuLatex() = "\\operatorname{rad}_{${vertreter.menge.zuLatex()}}\\left(${winkel.zuLatex()}\\right)"
}

/** y-Koordinate des Einheitskreispunkts zum Winkel; Ergebnis reell. */
data class SinusGrad(val winkel: WinkelAusdruck) : ZahlAusdruck {
    override fun zuLatex() = "\\sin_{\\mathbb{G}}\\left(${winkel.zuLatex()}\\right)"
}

fun winkelAusBogenmass(argument: ZahlAusdruck): WinkelAusdruck {
    if (argument is WinkelBogenmass) return argument.winkel
    fun piFaktor(z: ZahlAusdruck): RationaleZahl? = when (z) {
        Pi -> RationaleZahl.Eins
        RationaleZahl.Null -> RationaleZahl.Null
        is Division -> (z.divisor as? RationaleZahl)?.takeIf { it != RationaleZahl.Null }?.let { nenner -> piFaktor(z.dividend)?.div(nenner) }
        is Multiplikation -> if (z.faktoren.count { it == Pi } == 1 && z.faktoren.all { it == Pi || it is RationaleZahl })
            z.faktoren.filterIsInstance<RationaleZahl>().fold(RationaleZahl.Eins) { a, b -> a * b } else null
        else -> null
    }
    return piFaktor(argument)?.let { GradWinkel(it * RationaleZahl.von(180)) } ?: WinkelAusBogenmass(argument)
}

/** Exakte symbolische Ausgabe für rationale Gradwerte, sonst Hauptwertausdruck. */
fun winkelBogenmass(winkel: WinkelAusdruck, vertreter: WinkelVertreter = WinkelVertreter.POSITIV): ZahlAusdruck {
    val grad = (winkel as? GradWinkel)?.grad as? RationaleZahl ?: return WinkelBogenmass(winkel, vertreter)
    val hauptwert = if (vertreter == WinkelVertreter.SYMMETRISCH && grad >= RationaleZahl.von(180)) grad - RationaleZahl.von(360) else grad
    return multiplikation(hauptwert / RationaleZahl.von(180), Pi)
}

internal fun winkelBogenmassUnnormiert(winkel: WinkelAusdruck): ZahlAusdruck = when (winkel) {
    is GradWinkel -> gradZuBogenmass(winkel.grad)
    is WinkelAusBogenmass -> winkel.argument
    else -> WinkelBogenmass(winkel)
}

internal fun hauptwertNumerisch(rad: Double, vertreter: WinkelVertreter): Double {
    require(rad.isFinite()) { "Der Winkel muss endlich sein." }
    val umlauf = 2 * kotlin.math.PI
    val rest = rad % umlauf
    if (vertreter == WinkelVertreter.SYMMETRISCH) return when {
        rest >= Math.PI -> rest - umlauf
        rest < -Math.PI -> rest + umlauf
        rest == 0.0 -> 0.0
        else -> rest
    }
    val positiv = if (rest < 0.0) (rest + umlauf).let { if (it >= umlauf) Math.nextDown(umlauf) else it } else if (rest == 0.0) 0.0 else rest
    return positiv
}
