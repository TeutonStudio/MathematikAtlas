package de.TeutonStudio.MathematikRechenSystem.kern

/** Eine mathematische Methode mit benannten Ausgaben und deren deklarierten Zielmengen. */
data class Funktion(
    val name: String,
    val parameter: List<Variable>,
    val ausgaben: Map<String, MathematischesObjekt>,
    val zielMengen: Map<String, MengenAusdruck> = emptyMap(),
    /** Definitionsmengen der Parameter, in derselben Reihenfolge wie [parameter]. */
    val werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
) : MathematischesObjekt {
    init {
        require(parameter.map { it.name }.distinct().size == parameter.size) { "Funktionsparameter müssen eindeutige Namen haben." }
        require(zielMengen.keys.all { it in ausgaben }) { "Zielmengen dürfen nur für vorhandene Ausgaben definiert werden." }
        require(werteVorräte.keys.all { key -> parameter.any { it.name == key } }) { "Wertevorräte dürfen nur für Parameter definiert werden." }
    }

    override fun zuLatex(): String {
        val p = parameter.joinToString(",") { it.zuLatex() }
        return if (ausgaben.size == 1) "$name($p) = ${ausgaben.values.single().zuLatex()}"
        else "$name($p) = \\left(${ausgaben.values.joinToString(",") { it.zuLatex() }}\\right)"
    }

    fun zielMengeFür(ausgabe: String): MengenAusdruck = zielMengen[ausgabe]
        ?: error("Für die Ausgabe '$ausgabe' der Methode '$name' ist keine Zielmenge definiert.")

    fun zielMengeFür(ausgabe: String, bindungen: Map<String, ZahlAusdruck>): MengenAusdruck =
        ersetze(zielMengeFür(ausgabe), bindungen) as MengenAusdruck

    val einzigeZielMenge: MengenAusdruck
        get() = zielMengeFür(einzigeAusgabe().first)

    /** Abgeleitete Bezeichnung für mengenwertige einwertige Methoden; kein eigener Zustand. */
    val grundMenge: MengenAusdruck
        get() = einzigeZielMenge

    fun binde(bindungen: Map<String, ZahlAusdruck>): GebundeneFunktion =
        GebundeneFunktion(this, bindungen.filterKeys { key -> parameter.any { it.name == key } })

    fun wendeAn(argumente: Map<String, ZahlAusdruck>): Map<String, MathematischesObjekt> {
        require(parameter.all { it.name in argumente }) { "Nicht alle Parameter sind gebunden." }
        return ausgaben.mapValues { (_, wert) -> vereinfacheObjekt(ersetze(wert, argumente)) }
    }

    fun einzigeAusgabe(): Pair<String, MathematischesObjekt> {
        require(ausgaben.size == 1) { "Die Methode '$name' muss genau eine Ausgabe besitzen." }
        return ausgaben.entries.single().toPair()
    }

    fun prüfeAlsIterationsMethode(erwartetMengenwert: Boolean): Pair<String, MathematischesObjekt> {
        require(parameter.size == 1) { "Die Methode '$name' muss genau einen freien Parameter besitzen." }
        val ausgabe = einzigeAusgabe()
        zielMengeFür(ausgabe.first)
        if (erwartetMengenwert) require(ausgabe.second is MengenAusdruck) {
            "Die Methode '$name' muss eine Menge ausgeben."
        } else require(ausgabe.second is ZahlAusdruck) {
            "Die Methode '$name' muss eine Zahl ausgeben."
        }
        return ausgabe
    }
}

/** Bild einer Menge unter einer einwertigen Methode: f[M] = { f(x) : x ∈ M }. */
data class Abbild(val menge: MengenAusdruck, val methode: Funktion) : MengenAusdruck {
    override fun zuLatex() = "${methode.name}[${menge.zuLatex()}]"
}

fun bildeAb(menge: MengenAusdruck, methode: Funktion): MengenAusdruck {
    val (ausgabe, _) = methode.prüfeAlsIterationsMethode(erwartetMengenwert = false)
    if (menge !is EndlicheMenge) return Abbild(menge, methode)
    val parameter = methode.parameter.single()
    return EndlicheMenge(menge.elemente.map { element ->
        val zahl = element as? ZahlAusdruck ?: error("Die abzubildende Menge muss Zahlen enthalten.")
        methode.wendeAn(mapOf(parameter.name to zahl)).getValue(ausgabe)
    }.toSet())
}

data class GebundeneFunktion(val funktion: Funktion, val bindungen: Map<String, ZahlAusdruck>) : MathematischesObjekt {
    val freieParameter get() = funktion.parameter.filterNot { it.name in bindungen }
    override fun zuLatex(): String = funktion.copy(
        parameter = freieParameter,
        ausgaben = funktion.ausgaben.mapValues { ersetze(it.value, bindungen) },
        zielMengen = funktion.zielMengen.mapValues { ersetze(it.value, bindungen) as MengenAusdruck },
        werteVorräte = funktion.werteVorräte.filterKeys { it !in bindungen }.mapValues { ersetze(it.value, bindungen) as MengenAusdruck },
    ).zuLatex()
    fun binde(weitere: Map<String, ZahlAusdruck>) = GebundeneFunktion(funktion, bindungen + weitere)
    fun auswerten(): Map<String, MathematischesObjekt> {
        require(freieParameter.isEmpty()) { "Die Funktion besitzt noch freie Parameter." }
        return funktion.wendeAn(bindungen)
    }
}

fun ersetze(ausdruck: ZahlAusdruck, bindungen: Map<String, ZahlAusdruck>): ZahlAusdruck =
    ersetze(ausdruck as MathematischesObjekt, bindungen) as ZahlAusdruck

/** Rekursive, typübergreifende Substitution für Funktionsausgaben und Zielmengen. */
fun ersetze(objekt: MathematischesObjekt, bindungen: Map<String, ZahlAusdruck>): MathematischesObjekt = when (objekt) {
    is Variable -> bindungen[objekt.name] ?: objekt
    is Addition -> addition(objekt.summanden.map { ersetze(it, bindungen) })
    is Multiplikation -> multiplikation(objekt.faktoren.map { ersetze(it, bindungen) })
    is Division -> Division(ersetze(objekt.dividend, bindungen), ersetze(objekt.divisor, bindungen))
    is Potenz -> Potenz(ersetze(objekt.basis, bindungen), ersetze(objekt.exponent, bindungen))
    is Betrag -> Betrag(ersetze(objekt.argument, bindungen))
    is Sinus -> Sinus(ersetze(objekt.argument, bindungen))
    is Cosinus -> Cosinus(ersetze(objekt.argument, bindungen))
    is Exponentialfunktion -> Exponentialfunktion(ersetze(objekt.argument, bindungen))
    is NatürlicherLogarithmus -> NatürlicherLogarithmus(ersetze(objekt.argument, bindungen))
    is Wurzel -> Wurzel(ersetze(objekt.argument, bindungen))
    is KomplexeZahl -> KomplexeZahl(ersetze(objekt.realteil, bindungen), ersetze(objekt.imaginärteil, bindungen))
    is Logarithmus -> Logarithmus(ersetze(objekt.basis, bindungen), ersetze(objekt.argument, bindungen))
    is EndlicheMenge -> EndlicheMenge(objekt.elemente.map { ersetze(it, bindungen) }.toSet())
    is Vereinigung -> vereinige(objekt.mengen.map { ersetze(it, bindungen) as MengenAusdruck })
    is Schnitt -> schneide(objekt.mengen.map { ersetze(it, bindungen) as MengenAusdruck }, objekt.grundMenge?.let { ersetze(it, bindungen) as MengenAusdruck })
    is MengenDifferenz -> mengenDifferenz(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck)
    is KartesischesProdukt -> kartesischesProdukt(objekt.mengen.map { ersetze(it, bindungen) as MengenAusdruck })
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
    is Ungleichheit -> Ungleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
    is Vergleich -> Vergleich(ersetze(objekt.links, bindungen), objekt.art, ersetze(objekt.rechts, bindungen))
    is Negation -> Negation(ersetze(objekt.aussage, bindungen) as Aussage)
    is Konjunktion -> Konjunktion(objekt.aussagen.map { ersetze(it, bindungen) as Aussage })
    is Disjunktion -> Disjunktion(objekt.aussagen.map { ersetze(it, bindungen) as Aussage })
    is ElementBeziehung -> ElementBeziehung(ersetze(objekt.element, bindungen), ersetze(objekt.menge, bindungen) as MengenAusdruck)
    is TeilmengenBeziehung -> TeilmengenBeziehung(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck)
    is EchteTeilmengeBeziehung -> EchteTeilmengeBeziehung(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck)
    is ObermengenBeziehung -> ObermengenBeziehung(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck, objekt.echt)
    is Disjunktheit -> Disjunktheit(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck)
    is Tupel -> Tupel(objekt.elemente.map { ersetze(it, bindungen) })
    is Vektor -> Vektor(objekt.werte.map { ersetze(it, bindungen) })
    is Matrix -> Matrix(objekt.zeilen.map { zeile -> zeile.map { ersetze(it, bindungen) } })
    is Funktion -> objekt.copy(
        ausgaben = objekt.ausgaben.mapValues { ersetze(it.value, bindungen - objekt.parameter.map { it.name }.toSet()) },
        zielMengen = objekt.zielMengen.mapValues { ersetze(it.value, bindungen - objekt.parameter.map { it.name }.toSet()) as MengenAusdruck },
        werteVorräte = objekt.werteVorräte.mapValues { ersetze(it.value, bindungen - objekt.parameter.map { it.name }.toSet()) as MengenAusdruck },
    )
    is Abbild -> Abbild(ersetze(objekt.menge, bindungen) as MengenAusdruck, ersetze(objekt.methode, bindungen) as Funktion)
    is GebundeneFunktion -> objekt.copy(bindungen = objekt.bindungen.mapValues { ersetze(it.value, bindungen) })
    is IterierteSumme -> objekt.copy(indexMenge = ersetze(objekt.indexMenge, bindungen) as MengenAusdruck)
    is IteriertesProdukt -> objekt.copy(indexMenge = ersetze(objekt.indexMenge, bindungen) as MengenAusdruck)
    is IterierteVereinigung -> objekt.copy(indexMenge = ersetze(objekt.indexMenge, bindungen) as MengenAusdruck)
    is IterierterSchnitt -> objekt.copy(indexMenge = ersetze(objekt.indexMenge, bindungen) as MengenAusdruck)
    else -> objekt
}

private fun vereinfacheObjekt(objekt: MathematischesObjekt): MathematischesObjekt = when (objekt) {
    is ZahlAusdruck -> vereinfache(objekt)
    is EndlicheMenge -> EndlicheMenge(objekt.elemente.map(::vereinfacheObjekt).toSet())
    is Vereinigung -> vereinige(objekt.mengen)
    is Schnitt -> schneide(objekt.mengen, objekt.grundMenge)
    else -> objekt
}
