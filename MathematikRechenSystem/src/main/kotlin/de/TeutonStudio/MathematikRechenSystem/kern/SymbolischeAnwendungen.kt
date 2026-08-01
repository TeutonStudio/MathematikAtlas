package de.TeutonStudio.MathematikRechenSystem.kern

/** Allgemeine symbolische Anwendung eines Funktionsobjekts. */
sealed interface SymbolischeAnwendung : MathematischesObjekt {
    val methode: MathematischesObjekt
    val argumente: List<MathematischesObjekt>

    fun anwendungsLatex(): String =
        "${methode.zuLatex()}(${argumente.joinToString(",") { it.zuLatex() }})"
}

data class SymbolischeZahlAnwendung(
    override val methode: MathematischesObjekt,
    override val argumente: List<MathematischesObjekt>,
) : ZahlAusdruck, SymbolischeAnwendung {
    override fun zuLatex(): String = anwendungsLatex()
}

data class SymbolischeMengenAnwendung(
    override val methode: MathematischesObjekt,
    override val argumente: List<MathematischesObjekt>,
) : MengenAusdruck, SymbolischeAnwendung {
    override fun zuLatex(): String = anwendungsLatex()
}

data class SymbolischeAussagenAnwendung(
    override val methode: MathematischesObjekt,
    override val argumente: List<MathematischesObjekt>,
) : Aussage, SymbolischeAnwendung {
    override fun entscheide(kontext: RechenKontext) = AussageErgebnis(
        wahrheitswert = null,
        status = EntscheidungsStatus.Unbekannt,
        begründung = "Die symbolische Methoden-Anwendung ${zuLatex()} ist nicht belegt.",
    )

    override fun zuLatex(): String = anwendungsLatex()
}

data class SymbolischeObjektAnwendung(
    override val methode: MathematischesObjekt,
    override val argumente: List<MathematischesObjekt>,
) : Ausdruck, SymbolischeAnwendung {
    override fun zuLatex(): String = anwendungsLatex()
}

fun symbolischeAnwendung(
    methode: MathematischesObjekt,
    argumente: List<MathematischesObjekt>,
    ergebnisArt: String,
): MathematischesObjekt = when (ergebnisArt) {
    "mathematik.zahl" -> SymbolischeZahlAnwendung(methode, argumente)
    "mathematik.aussage" -> SymbolischeAussagenAnwendung(methode, argumente)
    "mathematik.menge" -> SymbolischeMengenAnwendung(methode, argumente)
    else -> SymbolischeObjektAnwendung(methode, argumente)
}
