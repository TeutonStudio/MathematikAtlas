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

/** Die Grundmenge wird ausschließlich aus [methode.einzigeZielMenge] abgeleitet. */
data class IterierterSchnitt(val methode: Funktion, val indexMenge: MengenAusdruck) : MengenAusdruck {
    val grundMenge get() = methode.einzigeZielMenge
    override fun zuLatex() = "\\bigcap_{${methode.parameter.single().zuLatex()} \\in ${indexMenge.zuLatex()}} ${methode.name}(${methode.parameter.single().zuLatex()})"
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

private fun iteriereMengen(methode: Funktion, indexMenge: MengenAusdruck, schnitt: Boolean): MengenAusdruck {
    methode.prüfeAlsIterationsMethode(erwartetMengenwert = true)
    if (indexMenge == LeereMenge) return if (schnitt) methode.einzigeZielMenge else LeereMenge
    if (indexMenge !is EndlicheMenge) return if (schnitt) IterierterSchnitt(methode, indexMenge) else IterierteVereinigung(methode, indexMenge)
    val parameter = methode.parameter.single()
    val werte = indexMenge.elemente.sortedBy(::strukturellerSchlüssel).map { index ->
        val zahl = index as? ZahlAusdruck ?: error("Die Indexmenge der Methode '${methode.name}' muss Zahlen enthalten.")
        val menge = methode.wendeAn(mapOf(parameter.name to zahl)).values.single() as? MengenAusdruck
            ?: error("Die Methode '${methode.name}' liefert keinen Mengenwert.")
        prüfeZielmenge(methode, index, menge)
        menge
    }
    return if (schnitt) schneide(werte, methode.einzigeZielMenge) else vereinige(werte)
}

private fun prüfeZielmenge(methode: Funktion, index: MathematischesObjekt, ergebnis: MengenAusdruck) {
    val ausgabe = methode.einzigeAusgabe().first
    val zahlIndex = index as? ZahlAusdruck ?: return
    val grundMenge = methode.zielMengeFür(ausgabe, mapOf(methode.parameter.single().name to zahlIndex))
    if (ergebnis is EndlicheMenge && grundMenge is EndlicheMenge && !grundMenge.elemente.containsAll(ergebnis.elemente)) {
        error("Die Methode liefert für ${methode.parameter.single().name} = ${index.zuLatex()} eine Menge, die nicht Teil der definierten Grundmenge ist.")
    }
}
