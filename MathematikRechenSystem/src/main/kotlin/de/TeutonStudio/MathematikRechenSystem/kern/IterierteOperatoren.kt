package de.TeutonStudio.MathematikRechenSystem.kern

data class IterierteSumme(val methode: Funktion, val indexMenge: MengenAusdruck) : ZahlAusdruck {
    override fun zuLatex() = "\\sum_{${methode.parameter.single().zuLatex()} \\in ${indexMenge.zuLatex()}} ${methode.name}(${methode.parameter.single().zuLatex()})"
}

data class IteriertesProdukt(val methode: Funktion, val indexMenge: MengenAusdruck) : ZahlAusdruck {
    override fun zuLatex() = "\\prod_{${methode.parameter.single().zuLatex()} \\in ${indexMenge.zuLatex()}} ${methode.name}(${methode.parameter.single().zuLatex()})"
}

data class IterierteVereinigung(val methode: Funktion, val indexMenge: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex() = "\\bigcup_{${methode.parameter.single().zuLatex()} \\in ${indexMenge.zuLatex()}} ${methode.name}(${methode.parameter.single().zuLatex()})"
}

data class IteriertesKartesischesProdukt(val methode: Funktion, val indexMenge: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex() = "\\mathop{\\times}_{${methode.parameter.single().zuLatex()} \\in ${indexMenge.zuLatex()}} ${methode.name}(${methode.parameter.single().zuLatex()})"
}

/** Die Grundmenge wird ausschließlich aus der validierten Zielmenge der Methode abgeleitet. */
data class IterierterSchnitt(val methode: Funktion, val indexMenge: MengenAusdruck) : MengenAusdruck {
    val grundMenge get() = methode.grundMengeFürMengenAusgabe()
    override fun zuLatex() = "\\bigcap_{${methode.parameter.single().zuLatex()} \\in ${indexMenge.zuLatex()}} ${methode.name}(${methode.parameter.single().zuLatex()})"
}
data class IterierteKonjunktion(val methode: Funktion, val indexMenge: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext) = iteriereAussagen(methode, indexMenge, true).entscheide(kontext)
    override fun zuLatex() = "\\bigwedge_{${methode.parameter.single().zuLatex()} \\in ${indexMenge.zuLatex()}} ${methode.name}(${methode.parameter.single().zuLatex()})"
}
data class IterierteDisjunktion(val methode: Funktion, val indexMenge: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext) = iteriereAussagen(methode, indexMenge, false).entscheide(kontext)
    override fun zuLatex() = "\\bigvee_{${methode.parameter.single().zuLatex()} \\in ${indexMenge.zuLatex()}} ${methode.name}(${methode.parameter.single().zuLatex()})"
}

fun iterierteSumme(methode: Funktion, indexMenge: MengenAusdruck): ZahlAusdruck = iteriereZahlen(methode, indexMenge, false)
fun iteriertesProdukt(methode: Funktion, indexMenge: MengenAusdruck): ZahlAusdruck = iteriereZahlen(methode, indexMenge, true)

private fun iteriereZahlen(methode: Funktion, indexMenge: MengenAusdruck, produkt: Boolean): ZahlAusdruck {
    methode.prüfeAlsIterationsMethode(erwartetMengenwert = false)
    if (indexMenge == LeereMenge) return if (produkt) RationaleZahl.Eins else RationaleZahl.Null
    if (indexMenge !is EndlicheMenge) return if (produkt) IteriertesProdukt(methode, indexMenge) else IterierteSumme(methode, indexMenge)
    val parameter = methode.parameter.single()
    val werte = indexMenge.elemente.sortedBy(::strukturellerSchlüssel).map { index ->
        val zahl = index as? ZahlAusdruck ?: error("Die Indexmenge der Methode '${methode.name}' muss Zahlen enthalten.")
        methode.wendeAn(mapOf(parameter.name to zahl)).values.single() as? ZahlAusdruck
            ?: error("Die Methode '${methode.name}' liefert keinen Zahlwert.")
    }
    return if (produkt) multiplikation(werte) else addition(werte)
}

fun iterierteVereinigung(methode: Funktion, indexMenge: MengenAusdruck): MengenAusdruck = iteriereMengen(methode, indexMenge, false)
fun iterierterSchnitt(methode: Funktion, indexMenge: MengenAusdruck): MengenAusdruck = iteriereMengen(methode, indexMenge, true)
fun iteriertesKartesischesProdukt(methode: Funktion, indexMenge: MengenAusdruck): MengenAusdruck {
    methode.prüfeAlsIterationsMethode(erwartetMengenwert = true)
    if (indexMenge !is EndlicheMenge) return IteriertesKartesischesProdukt(methode, indexMenge)
    val parameter = methode.parameter.single()
    val mengen = indexMenge.elemente.sortedBy(::strukturellerSchlüssel).map { index ->
        val zahl = index as? ZahlAusdruck ?: error("Die Indexmenge muss Zahlen enthalten.")
        methode.wendeAn(mapOf(parameter.name to zahl)).values.single() as? MengenAusdruck
            ?: error("Die Methode '${methode.name}' muss Mengen liefern.")
    }
    return when (mengen.size) {
        0 -> EndlicheMenge(setOf(Tupel(emptyList())))
        1 -> mengen.single()
        else -> kartesischesProdukt(mengen)
    }
}

fun iterierteKonjunktion(methode: Funktion, indexMenge: MengenAusdruck): Aussage = iteriereAussagen(methode, indexMenge, true)
fun iterierteDisjunktion(methode: Funktion, indexMenge: MengenAusdruck): Aussage = iteriereAussagen(methode, indexMenge, false)
private fun iteriereAussagen(methode: Funktion, indexMenge: MengenAusdruck, konjunktion: Boolean): Aussage {
    require(methode.parameter.size == 1 && methode.ausgaben.size == 1 && methode.einzigeAusgabe().second is Aussage) { "Die Abbildung muss einwertig eine Aussage liefern." }
    if (indexMenge !is EndlicheMenge) return if (konjunktion) IterierteKonjunktion(methode, indexMenge) else IterierteDisjunktion(methode, indexMenge)
    val p = methode.parameter.single()
    val aussagen = indexMenge.elemente.map { index ->
        val zahl = index as? ZahlAusdruck ?: error("Indexmenge muss Zahlen enthalten.")
        methode.wendeAn(mapOf(p.name to zahl)).values.single() as? Aussage ?: error("Abbildung liefert keine Aussage.")
    }
    return if (konjunktion) Konjunktion(aussagen) else Disjunktion(aussagen)
}

private fun iteriereMengen(methode: Funktion, indexMenge: MengenAusdruck, schnitt: Boolean): MengenAusdruck {
    methode.prüfeAlsIterationsMethode(erwartetMengenwert = true)
    val grundMenge = methode.grundMengeFürMengenAusgabe()
    if (indexMenge == LeereMenge) return if (schnitt) grundMenge else LeereMenge
    if (indexMenge !is EndlicheMenge) return if (schnitt) IterierterSchnitt(methode, indexMenge) else IterierteVereinigung(methode, indexMenge)
    val parameter = methode.parameter.single()
    val werte = indexMenge.elemente.sortedBy(::strukturellerSchlüssel).map { index ->
        val zahl = index as? ZahlAusdruck ?: error("Die Indexmenge der Methode '${methode.name}' muss Zahlen enthalten.")
        val menge = methode.wendeAn(mapOf(parameter.name to zahl)).values.single() as? MengenAusdruck
            ?: error("Die Methode '${methode.name}' liefert keinen Mengenwert.")
        prüfeZielmenge(methode, index, menge)
        menge
    }
    return if (schnitt) schneide(werte, grundMenge) else vereinige(werte)
}

private fun prüfeZielmenge(methode: Funktion, index: MathematischesObjekt, ergebnis: MengenAusdruck) {
    val grundMenge = methode.grundMengeFürMengenAusgabe()
    if (prüfeTeilmenge(ergebnis, grundMenge).wahrheitswert == Wahrheitswert.Falsch) {
        error(
            "Die Methode '${methode.name}' liefert für ${methode.parameter.single().name} = ${index.zuLatex()} " +
                "eine Menge, die nicht Teil der Grundmenge ${grundMenge.zuLatex()} ist.",
        )
    }
}
