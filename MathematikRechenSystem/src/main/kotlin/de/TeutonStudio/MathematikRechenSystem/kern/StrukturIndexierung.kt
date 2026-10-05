package de.TeutonStudio.MathematikRechenSystem.kern

/** Endlicher, gegebenenfalls symbolisch langer 1-basierter Indexbereich `{1,…,n}`. */
data class EndlicheIndexMenge(val obergrenze: ZahlAusdruck) : MengenAusdruck {
    init {
        require(obergrenze !is RationaleZahl || obergrenze.positiveGanzeZahlOderNull() != null) {
            "Die obere Grenze einer endlichen Indexmenge muss positiv und ganzzahlig sein."
        }
    }

    override fun zuLatex(): String = "\\left\\{1,\\ldots,${obergrenze.zuLatex()}\\right\\}"
}

/** Symbolische Achsenlänge einer Quelle, deren konkrete Form noch nicht bekannt ist. */
data class StrukturAchsenLaenge(
    val quelle: MathematischesObjekt,
    /** Nullbasierte interne Achse. */
    val achse: Int,
) : ZahlAusdruck {
    init { require(achse >= 0) { "Die Strukturachse darf nicht negativ sein." } }
    override fun zuLatex(): String = "\\dim_{${achse + 1}}\\left(${quelle.zuLatex()}\\right)"
}

enum class StrukturZugriffsArt { Komponente, Zeile, Spalte, Schnitt }

/** Symbolischer allgemeiner Komponenten- oder Schnittzugriff. */
data class SymbolischerStrukturZugriff(
    val quelle: MathematischesObjekt,
    val index: ZahlAusdruck,
    val art: StrukturZugriffsArt,
    /** Nullbasierte interne Achse. */
    val achse: Int = 0,
    val ergebnisAnschlussArt: String,
    val ergebnisForm: List<ZahlAusdruck>? = null,
) : Ausdruck {
    init { require(achse >= 0) }
    override fun zuLatex(): String = when (art) {
        StrukturZugriffsArt.Komponente -> "${quelle.zuLatex()}_{${index.zuLatex()}}"
        StrukturZugriffsArt.Zeile -> "\\operatorname{Zeile}_{${index.zuLatex()}}\\left(${quelle.zuLatex()}\\right)"
        StrukturZugriffsArt.Spalte -> "\\operatorname{Spalte}_{${index.zuLatex()}}\\left(${quelle.zuLatex()}\\right)"
        StrukturZugriffsArt.Schnitt -> "\\operatorname{Schnitt}_{${achse + 1},${index.zuLatex()}}\\left(${quelle.zuLatex()}\\right)"
    }
}

/** Numerischer Sonderfall, damit symbolische Vektorkomponenten Zahlterme bleiben. */
data class SymbolischeZahlKomponente(
    val quelle: MathematischesObjekt,
    val index: ZahlAusdruck,
) : ZahlAusdruck {
    override fun zuLatex(): String = "${quelle.zuLatex()}_{${index.zuLatex()}}"
}

/** Zielmenge einer strukturerhaltend typisierten Indexmethode. */
data class StrukturErgebnisMenge(
    val anschlussArt: String,
    val form: List<ZahlAusdruck>? = null,
    val bezeichnung: String,
) : MengenAusdruck {
    init { require(anschlussArt.isNotBlank() && bezeichnung.isNotBlank()) }
    override fun zuLatex(): String = bezeichnung
}

fun strukturAchsenLaenge(quelle: MathematischesObjekt, achse: Int): ZahlAusdruck {
    require(achse >= 0)
    val konkreteForm = when (quelle) {
        is Tensorartig -> quelle.tensorForm.map { RationaleZahl.von(it.toLong()) }
        is Tupel -> listOf(RationaleZahl.von(quelle.elemente.size.toLong()))
        is TypisiertesElement -> quelle.strukturForm
        else -> null
    }
    konkreteForm?.let { require(achse in it.indices) { "Die Struktur besitzt keine Achse ${achse + 1}." } }
    return konkreteForm?.get(achse) ?: StrukturAchsenLaenge(quelle, achse)
}

fun strukturKomponente(
    quelle: MathematischesObjekt,
    index: ZahlAusdruck,
    numerisch: Boolean,
): MathematischesObjekt {
    val konkret = index.konkreterEinsbasierterIndexOderNull()
    if (konkret != null) {
        val wert = when (quelle) {
            is Tupel -> quelle.elemente.getOrNull(konkret)
            is OrientierterVektor -> quelle.werte.getOrNull(konkret)
            else -> null
        }
        if (wert != null) return wert
        pruefeSymbolischeGrenze(quelle, 0, konkret)
    }
    return if (numerisch) SymbolischeZahlKomponente(quelle, index)
    else SymbolischerStrukturZugriff(quelle, index, StrukturZugriffsArt.Komponente, ergebnisAnschlussArt = "objekt")
}

fun strukturSchnitt(
    quelle: MathematischesObjekt,
    index: ZahlAusdruck,
    art: StrukturZugriffsArt,
    achse: Int,
    ergebnisAnschlussArt: String,
    ergebnisForm: List<ZahlAusdruck>? = null,
): MathematischesObjekt {
    require(art != StrukturZugriffsArt.Komponente)
    val konkret = index.konkreterEinsbasierterIndexOderNull()
    if (konkret != null) {
        when (quelle) {
            is Matrix -> return when (art) {
                StrukturZugriffsArt.Zeile -> ZeilenVektor(quelle.zeilen.getOrElse(konkret) { ausserhalb(konkret, quelle.zeilenAnzahl) })
                StrukturZugriffsArt.Spalte -> SpaltenVektor(quelle.zeilen.map { it.getOrElse(konkret) { ausserhalb(konkret, quelle.spaltenAnzahl) } })
                else -> tensorSchnitt(quelle, achse, konkret)
            }
            is Tensorartig -> return tensorSchnitt(quelle, achse, konkret)
            else -> pruefeSymbolischeGrenze(quelle, achse, konkret)
        }
    }
    return SymbolischerStrukturZugriff(quelle, index, art, achse, ergebnisAnschlussArt, ergebnisForm)
}

private fun tensorSchnitt(quelle: Tensorartig, achse: Int, index: Int): MathematischesObjekt {
    require(achse in quelle.tensorForm.indices) { "Die Struktur besitzt keine Achse ${achse + 1}." }
    require(index in 0 until quelle.tensorForm[achse]) {
        "Index ${index + 1} liegt außerhalb von {1,…,${quelle.tensorForm[achse]}}."
    }
    val restForm = quelle.tensorForm.filterIndexed { position, _ -> position != achse }
    if (restForm.isEmpty()) return quelle.tensorKomponente(listOf(index))
    val werte = restForm.indizesFolge().map { restIndizes ->
        val voll = restIndizes.toMutableList().also { it.add(achse, index) }
        quelle.tensorKomponente(voll)
    }
    return when (restForm.size) {
        1 -> Tensor(restForm, werte)
        2 -> Matrix(List(restForm[0]) { z -> List(restForm[1]) { s -> werte[z * restForm[1] + s] } })
        else -> Tensor(restForm, werte)
    }
}

private fun ZahlAusdruck.konkreterEinsbasierterIndexOderNull(): Int? {
    val rational = this as? RationaleZahl ?: return null
    require(rational.nenner == java.math.BigInteger.ONE && rational.zähler.signum() > 0 && rational.zähler.bitLength() < 31) {
        "Ein Strukturindex muss eine positive ganze Zahl sein."
    }
    return rational.zähler.toInt() - 1
}

private fun pruefeSymbolischeGrenze(quelle: MathematischesObjekt, achse: Int, index: Int) {
    val grenze = runCatching { strukturAchsenLaenge(quelle, achse).positiveGanzeZahlOderNull() }.getOrNull()
    if (grenze != null) require(index in 0 until grenze) {
        "Index ${index + 1} liegt außerhalb von {1,…,$grenze}."
    }
}

private fun ausserhalb(index: Int, grenze: Int): Nothing =
    throw IndexOutOfBoundsException("Index ${index + 1} liegt außerhalb von {1,…,$grenze}.")
