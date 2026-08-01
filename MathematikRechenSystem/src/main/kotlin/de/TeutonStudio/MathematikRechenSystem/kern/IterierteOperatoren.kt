package de.TeutonStudio.MathematikRechenSystem.kern

data class IterierteSumme(val methode: Funktion, val indexMenge: MengenAusdruck) : ZahlAusdruck {
    override fun zuLatex() = iterationsLatex("\\sum", methode, indexMenge)
}

data class IteriertesProdukt(val methode: Funktion, val indexMenge: MengenAusdruck) : ZahlAusdruck {
    override fun zuLatex() = iterationsLatex("\\prod", methode, indexMenge)
}

data class IterierteVereinigung(val methode: Funktion, val indexMenge: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex() = iterationsLatex("\\bigcup", methode, indexMenge)
}

data class IteriertesKartesischesProdukt(val methode: Funktion, val indexMenge: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex() = iterationsLatex("\\mathop{\\times}", methode, indexMenge)
}

/** Die Grundmenge wird ausschließlich aus der validierten Zielmenge der Methode abgeleitet. */
data class IterierterSchnitt(val methode: Funktion, val indexMenge: MengenAusdruck) : MengenAusdruck {
    val grundMenge get() = methode.grundMengeFürMengenAusgabe()
    override fun zuLatex() = iterationsLatex("\\bigcap", methode, indexMenge)
}

data class IterierteKonjunktion(val methode: Funktion, val indexMenge: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis =
        if (indexMenge is EndlicheMenge) iteriereAussagen(methode, indexMenge, IterierteAussagenArt.Konjunktion).entscheide(kontext)
        else symbolischeAussagenIteration()

    override fun zuLatex() = iterationsLatex("\\bigwedge", methode, indexMenge)
}

data class IterierteDisjunktion(val methode: Funktion, val indexMenge: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis =
        if (indexMenge is EndlicheMenge) iteriereAussagen(methode, indexMenge, IterierteAussagenArt.Disjunktion).entscheide(kontext)
        else symbolischeAussagenIteration()

    override fun zuLatex() = iterationsLatex("\\bigvee", methode, indexMenge)
}

data class IterierteAdjunktion(val methode: Funktion, val indexMenge: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis =
        if (indexMenge is EndlicheMenge) iteriereAussagen(methode, indexMenge, IterierteAussagenArt.Adjunktion).entscheide(kontext)
        else symbolischeAussagenIteration()

    override fun zuLatex() = iterationsLatex("\\mathop{\\stackrel{\\bullet}{\\bigvee}}", methode, indexMenge)
}

/** Gemeinsame kanonische Darstellung großer Operatoren für Rechenkern und UI. */
fun großerOperatorLatex(
    operator: String,
    indexBedingung: String,
    rumpf: String,
): String = "$operator\\limits_{${indexBedingung}} $rumpf"

private fun iterationsLatex(operator: String, methode: Funktion, indexMenge: MengenAusdruck): String {
    val parameter = methode.parameter.single().zuLatex()
    return großerOperatorLatex(
        operator = operator,
        indexBedingung = "$parameter \\in ${indexMenge.zuLatex()}",
        rumpf = "${methode.name}($parameter)",
    )
}

private fun symbolischeAussagenIteration() = AussageErgebnis(
    wahrheitswert = null,
    status = EntscheidungsStatus.Unbekannt,
    begründung = "Die Aussage wird über einer nicht endlich auswertbaren Indexmenge symbolisch dargestellt.",
)

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
    if (indexMenge == LeereMenge) return leeresIndexProdukt()
    if (indexMenge !is EndlicheMenge) return IteriertesKartesischesProdukt(methode, indexMenge)
    val parameter = methode.parameter.single()
    val indexe = indexMenge.elemente.sortedBy(::strukturellerSchlüssel).map { index ->
        index as? ZahlAusdruck ?: error("Die Indexmenge muss Zahlen enthalten.")
    }
    if (indexe.isEmpty()) return leeresIndexProdukt()
    val mengen = indexe.map { index ->
        methode.wendeAn(mapOf(parameter.name to index)).values.single() as? MengenAusdruck
            ?: error("Die Methode '${methode.name}' muss Mengen liefern.")
    }
    if (mengen.any { it !is EndlicheMenge }) return IteriertesKartesischesProdukt(methode, indexMenge)
    val endlicheMengen = mengen.map { it as EndlicheMenge }
    val kombinationen = endlicheMengen.fold(listOf(emptyList<MathematischesObjekt>())) { bisher, menge ->
        val elemente = menge.elemente.sortedBy(::strukturellerSchlüssel)
        bisher.flatMap { präfix -> elemente.map { element -> präfix + element } }
    }
    val zielMenge = vereinige(endlicheMengen)
    val auswahlIndex = Variable(parameter.name)
    val auswahlFunktionen = kombinationen.map { werte ->
        val körper = auswahlKörper(auswahlIndex, indexe, werte)
        Funktion(
            name = "g",
            parameter = listOf(auswahlIndex),
            ausgaben = mapOf("wert" to körper),
            zielMengen = mapOf("wert" to zielMenge),
            werteVorräte = mapOf(auswahlIndex.name to indexMenge),
        )
    }.toSet()
    return EndlicheMenge(auswahlFunktionen)
}

private fun leeresIndexProdukt(): EndlicheMenge = EndlicheMenge(
    setOf(
        Funktion(
            name = "\\varnothing",
            parameter = emptyList(),
            ausgaben = emptyMap(),
        ),
    ),
)

private fun auswahlKörper(
    index: Variable,
    indexe: List<ZahlAusdruck>,
    werte: List<MathematischesObjekt>,
): MathematischesObjekt {
    require(indexe.isNotEmpty() && indexe.size == werte.size)
    var körper = werte.last()
    for (position in indexe.lastIndex - 1 downTo 0) {
        körper = FallAusdruck(
            wahr = werte[position],
            aussage = Gleichheit(index, indexe[position]),
            lüge = körper,
        )
    }
    return körper
}

private enum class IterierteAussagenArt { Konjunktion, Disjunktion, Adjunktion }

fun iterierteKonjunktion(methode: Funktion, indexMenge: MengenAusdruck): Aussage =
    iteriereAussagen(methode, indexMenge, IterierteAussagenArt.Konjunktion)
fun iterierteDisjunktion(methode: Funktion, indexMenge: MengenAusdruck): Aussage =
    iteriereAussagen(methode, indexMenge, IterierteAussagenArt.Disjunktion)
fun iterierteAdjunktion(methode: Funktion, indexMenge: MengenAusdruck): Aussage =
    iteriereAussagen(methode, indexMenge, IterierteAussagenArt.Adjunktion)

private fun iteriereAussagen(
    methode: Funktion,
    indexMenge: MengenAusdruck,
    art: IterierteAussagenArt,
): Aussage {
    require(methode.parameter.size == 1 && methode.ausgaben.size == 1 && methode.einzigeAusgabe().second is Aussage) {
        "Die Abbildung muss einwertig eine Aussage liefern."
    }
    if (indexMenge == LeereMenge) return when (art) {
        IterierteAussagenArt.Konjunktion -> WahrheitsKonstante(true)
        IterierteAussagenArt.Disjunktion -> WahrheitsKonstante(false)
        IterierteAussagenArt.Adjunktion -> WahrheitsKonstante(false)
    }
    if (indexMenge !is EndlicheMenge) return when (art) {
        IterierteAussagenArt.Konjunktion -> IterierteKonjunktion(methode, indexMenge)
        IterierteAussagenArt.Disjunktion -> IterierteDisjunktion(methode, indexMenge)
        IterierteAussagenArt.Adjunktion -> IterierteAdjunktion(methode, indexMenge)
    }
    val parameter = methode.parameter.single()
    val aussagen = indexMenge.elemente.sortedBy(::strukturellerSchlüssel).map { index ->
        methode.wendeAn(mapOf(parameter.name to index)).values.single() as? Aussage
            ?: error("Die Methode '${methode.name}' liefert keine Aussage.")
    }
    return when (art) {
        IterierteAussagenArt.Konjunktion -> Konjunktion(aussagen)
        IterierteAussagenArt.Disjunktion -> Disjunktion(aussagen)
        IterierteAussagenArt.Adjunktion -> adjunktion(aussagen)
    }
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
    if (prüfeTeilmenge(ergebnis, grundMenge).wahrheitswert == Wahrheitswert.Lüge) {
        error(
            "Die Methode '${methode.name}' liefert für ${methode.parameter.single().name} = ${index.zuLatex()} " +
                "eine Menge, die nicht Teil der Grundmenge ${grundMenge.zuLatex()} ist.",
        )
    }
}
