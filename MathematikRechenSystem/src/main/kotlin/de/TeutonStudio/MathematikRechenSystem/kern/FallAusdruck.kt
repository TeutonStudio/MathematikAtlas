package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Gemeinsamer Vertrag symbolischer Fallunterscheidungen.
 *
 * Die konkrete Unterklasse bewahrt die mathematische Art beider Zweige. Dadurch
 * bleibt ein unentschiedener Zahlenfall ein [ZahlAusdruck], ein Mengenfall ein
 * [MengenAusdruck] und ein Aussagenfall eine [Aussage].
 */
sealed class FallAusdruck : Ausdruck {
    abstract val wahr: MathematischesObjekt
    abstract val aussage: Aussage
    abstract val lüge: MathematischesObjekt

    abstract fun copy(
        wahr: MathematischesObjekt = this.wahr,
        aussage: Aussage = this.aussage,
        lüge: MathematischesObjekt = this.lüge,
    ): FallAusdruck

    override fun zuLatex(): String =
        "\\begin{cases}${wahr.zuLatex()} & \\text{falls } ${aussage.zuLatex()} \\\\ ${lüge.zuLatex()} & \\text{sonst}\\end{cases}"

    override fun equals(other: Any?): Boolean =
        other is FallAusdruck && this::class == other::class && wahr == other.wahr && aussage == other.aussage && lüge == other.lüge

    override fun hashCode(): Int = 31 * (31 * wahr.hashCode() + aussage.hashCode()) + lüge.hashCode()
}

private class AllgemeinerFallAusdruck(
    override val wahr: MathematischesObjekt,
    override val aussage: Aussage,
    override val lüge: MathematischesObjekt,
) : FallAusdruck() {
    override fun copy(wahr: MathematischesObjekt, aussage: Aussage, lüge: MathematischesObjekt): FallAusdruck =
        FallAusdruck(wahr, aussage, lüge)
}

class ZahlFallAusdruck internal constructor(
    override val wahr: ZahlAusdruck,
    override val aussage: Aussage,
    override val lüge: ZahlAusdruck,
) : FallAusdruck(), ZahlAusdruck {
    override fun copy(wahr: MathematischesObjekt, aussage: Aussage, lüge: MathematischesObjekt): FallAusdruck =
        FallAusdruck(wahr, aussage, lüge)
}

class MengenFallAusdruck internal constructor(
    override val wahr: MengenAusdruck,
    override val aussage: Aussage,
    override val lüge: MengenAusdruck,
) : FallAusdruck(), MengenAusdruck {
    override fun copy(wahr: MathematischesObjekt, aussage: Aussage, lüge: MathematischesObjekt): FallAusdruck =
        FallAusdruck(wahr, aussage, lüge)
}

class AussagenFallAusdruck internal constructor(
    override val wahr: Aussage,
    override val aussage: Aussage,
    override val lüge: Aussage,
) : FallAusdruck(), Aussage {
    override fun copy(wahr: MathematischesObjekt, aussage: Aussage, lüge: MathematischesObjekt): FallAusdruck =
        FallAusdruck(wahr, aussage, lüge)

    override fun entscheide(kontext: RechenKontext): AussageErgebnis = when (aussage.entscheide(kontext).wahrheitswert) {
        Wahrheitswert.Wahr -> wahr.entscheide(kontext.copy(annahmen = kontext.annahmen + aussage))
        Wahrheitswert.Lüge -> lüge.entscheide(kontext.copy(annahmen = kontext.annahmen + Negation(aussage)))
        null -> {
            val wahrErgebnis = wahr.entscheide(kontext.copy(annahmen = kontext.annahmen + aussage))
            val lügeErgebnis = lüge.entscheide(kontext.copy(annahmen = kontext.annahmen + Negation(aussage)))
            if (wahrErgebnis.wahrheitswert != null && wahrErgebnis.wahrheitswert == lügeErgebnis.wahrheitswert) {
                AussageErgebnis(wahrErgebnis.wahrheitswert, EntscheidungsStatus.Unbekannt, "Beide Fallzweige besitzen denselben Wahrheitswert.")
            } else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
    }
}

/** Erzeugt die spezifischste Fallausdrucksart, die beide Zweige gemeinsam besitzen. */
@Suppress("FunctionName")
fun FallAusdruck(
    wahr: MathematischesObjekt,
    aussage: Aussage,
    lüge: MathematischesObjekt,
): FallAusdruck = when {
    wahr is ZahlAusdruck && lüge is ZahlAusdruck -> ZahlFallAusdruck(wahr, aussage, lüge)
    wahr is MengenAusdruck && lüge is MengenAusdruck -> MengenFallAusdruck(wahr, aussage, lüge)
    wahr is Aussage && lüge is Aussage -> AussagenFallAusdruck(wahr, aussage, lüge)
    else -> AllgemeinerFallAusdruck(wahr, aussage, lüge)
}
