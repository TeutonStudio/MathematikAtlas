package de.TeutonStudio.MathematikRechenSystem.kern

/** Eine mathematische Methode mit benannten Ausgaben und deren deklarierten Zielmengen. */
data class Funktion(
    val name: String,
    val parameter: List<FunktionsParameter>,
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

    fun zielMengeFür(ausgabe: String, bindungen: Map<String, MathematischesObjekt>): MengenAusdruck =
        ersetze(zielMengeFür(ausgabe), bindungen) as MengenAusdruck

    val einzigeZielMenge: MengenAusdruck
        get() = zielMengeFür(einzigeAusgabe().first)

    /** Abgeleitete Bezeichnung für mengenwertige einwertige Iterationsmethoden; kein eigener Zustand. */
    val grundMenge: MengenAusdruck
        get() = grundMengeFürMengenAusgabe()

    /**
     * Die Zielmenge einer mengenwertigen Iterationsmethode ist ihre feste Grundmenge.
     * Sie darf daher nicht vom gebundenen Iterationsparameter abhängen.
     */
    fun grundMengeFürMengenAusgabe(): MengenAusdruck {
        require(parameter.size == 1) { "Die Methode '$name' muss genau einen freien Parameter besitzen." }
        val (ausgabe, wert) = einzigeAusgabe()
        require(wert is MengenAusdruck) { "Die Methode '$name' muss eine Menge ausgeben." }
        val grundMenge = zielMengeFür(ausgabe)
        val parameter = parameter.single() as? Variable
            ?: error("Die mengenwertige Iterationsmethode '$name' benötigt einen Zahlenparameter.")
        require(!grundMenge.enthältVariable(parameter)) {
            "Die Zielmenge der mengenwertigen Methode '$name' darf nicht vom Iterationsparameter " +
                "'${parameter.name}' abhängen, da sie als feste Grundmenge für den leeren Schnitt verwendet wird."
        }
        return grundMenge
    }

    fun binde(bindungen: Map<String, MathematischesObjekt>): GebundeneFunktion =
        GebundeneFunktion(this, bindungen.filterKeys { key -> parameter.any { it.name == key } })

    fun wendeAn(argumente: Map<String, MathematischesObjekt>): Map<String, MathematischesObjekt> {
        require(parameter.all { it.name in argumente }) { "Nicht alle Parameter sind gebunden." }
        return ausgaben.mapValues { (_, wert) -> vereinfacheObjekt(ersetze(wert, argumente)) }
    }

    fun einzigeAusgabe(): Pair<String, MathematischesObjekt> {
        require(ausgaben.size == 1) { "Die Methode '$name' muss genau eine Ausgabe besitzen." }
        return ausgaben.entries.single().toPair()
    }

    fun prüfeAlsIterationsMethode(erwartetMengenwert: Boolean): Pair<String, MathematischesObjekt> {
        require(parameter.size == 1) { "Die Methode '$name' muss genau einen freien Parameter besitzen." }
        require(parameter.single() is Variable) { "Die Methode '$name' benötigt für eine Iteration einen Zahlenparameter." }
        val ausgabe = einzigeAusgabe()
        if (erwartetMengenwert) require(ausgabe.second is MengenAusdruck) {
            "Die Methode '$name' muss eine Menge ausgeben."
        } else require(ausgabe.second is ZahlAusdruck) {
            "Die Methode '$name' muss eine Zahl ausgeben."
        }
        if (erwartetMengenwert) grundMengeFürMengenAusgabe() else zielMengeFür(ausgabe.first)
        return ausgabe
    }
}

/** Erzeugt eine [höhe] mal [breite]-Matrix aus einer Zahlmethode `f(zeile, spalte)`. */
fun matrixAusMethode(methode: Funktion, höhe: Int, breite: Int): Matrix {
    require(höhe > 0) { "Die Matrixhöhe muss positiv sein." }
    require(breite > 0) { "Die Matrixbreite muss positiv sein." }
    require(methode.parameter.size == 2) { "Die Matrixmethode muss genau zwei Parameter für Zeile und Spalte besitzen." }
    val (ausgabeName, ausgabe) = methode.einzigeAusgabe()
    require(ausgabe is ZahlAusdruck) { "Die Matrixmethode muss eine Zahl ausgeben." }
    val (zeilenParameter, spaltenParameter) = methode.parameter
    return Matrix(List(höhe) { zeile ->
        List(breite) { spalte ->
            methode.wendeAn(
                mapOf(
                    zeilenParameter.name to RationaleZahl.von(zeile.toLong()),
                    spaltenParameter.name to RationaleZahl.von(spalte.toLong()),
                ),
            ).getValue(ausgabeName) as? ZahlAusdruck
                ?: error("Die Matrixmethode muss für jeden Index eine Zahl ausgeben.")
        }
    })
}

/** Bild einer Menge unter einer einwertigen Methode: f[M] = { f(x) : x ∈ M }. */
data class Abbild(val menge: MengenAusdruck, val methode: Funktion) : MengenAusdruck {
    override fun zuLatex() = "${methode.name}[${menge.zuLatex()}]"
}

fun MengenAusdruck.hatDifferentialBegriff() = this == ReelleZahlen
fun MengenAusdruck.hatIntegralBegriff() = this == ReelleZahlen

private fun Funktion.einwertigeZahlMethode(): Triple<Variable, String, ZahlAusdruck> {
    require(parameter.size == 1 && ausgaben.size == 1) { "Die Methode muss genau einen Parameter und eine Ausgabe besitzen." }
    val (name, ausgabe) = einzigeAusgabe()
    return Triple(parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen."), name, ausgabe as? ZahlAusdruck ?: error("Die Methode muss eine Zahl ausgeben."))
}

fun komponiere(außen: Funktion, innen: Funktion): Funktion {
    val (x, ausgabeAußen, termAußen) = außen.einwertigeZahlMethode()
    val (t, ausgabeInnen, termInnen) = innen.einwertigeZahlMethode()
    val wertevorrat = innen.werteVorräte[t.name] ?: error("Die innere Methode benötigt einen Wertevorrat.")
    val zielInnen = innen.zielMengeFür(ausgabeInnen)
    val wertevorratAußen = außen.werteVorräte[x.name] ?: error("Die äußere Methode benötigt einen Wertevorrat.")
    require(zielInnen == wertevorratAußen) { "Zielmenge der inneren Methode und Wertevorrat der äußeren Methode müssen übereinstimmen." }
    return Funktion("${außen.name}\\circ${innen.name}", listOf(t), mapOf("wert" to ersetze(termAußen, mapOf(x.name to termInnen))), mapOf("wert" to außen.zielMengeFür(ausgabeAußen)), mapOf(t.name to wertevorrat))
}

fun iteriere(methode: Funktion, exponent: Int): Funktion {
    require(exponent >= 0) { "Der Iterationsexponent muss nichtnegativ sein." }
    val (x, ausgabe, term) = methode.einwertigeZahlMethode()
    val wertevorrat = methode.werteVorräte[x.name] ?: error("Die Methode benötigt einen Wertevorrat.")
    require(methode.zielMengeFür(ausgabe) == wertevorrat) { "Iteration ist nur für Endomorphismen definiert." }
    var ergebnis = Funktion("id", listOf(x), mapOf("wert" to x), mapOf("wert" to wertevorrat), mapOf(x.name to wertevorrat))
    repeat(exponent) { ergebnis = komponiere(methode, ergebnis) }
    return ergebnis.copy(name = "${methode.name}^{${exponent}}")
}

fun differenziereMethode(methode: Funktion): Funktion {
    require(methode.parameter.size == 1 && methode.ausgaben.size == 1) { "Die Methode muss genau einen Parameter und eine Ausgabe besitzen." }
    val x = methode.parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen."); val (ausgabe, wert) = methode.einzigeAusgabe()
    val wertevorrat = methode.werteVorräte[x.name] ?: error("Die Methode benötigt einen Wertevorrat.")
    require(wertevorrat.hatDifferentialBegriff()) { "Der Wertevorrat definiert keinen Differentialbegriff." }
    val abgeleitet = when (wert) {
        is ZahlAusdruck -> ableiten(wert, x).ergebnis
        is SpaltenVektor -> SpaltenVektor(wert.werte.map { ableiten(it, x).ergebnis })
        is ZeilenVektor -> ZeilenVektor(wert.werte.map { ableiten(it, x).ergebnis })
        else -> error("Die Methode muss eine Zahl oder einen orientierten Vektor ausgeben.")
    }
    return methode.copy(name = "${methode.name}'", ausgaben = mapOf(ausgabe to abgeleitet))
}

fun integriereMethode(methode: Funktion): Funktion {
    require(methode.parameter.size == 1 && methode.ausgaben.size == 1) { "Die Methode muss genau einen Parameter und eine Ausgabe besitzen." }
    val x = methode.parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen."); val (ausgabe, wert) = methode.einzigeAusgabe()
    val wertevorrat = methode.werteVorräte[x.name] ?: error("Die Methode benötigt einen Wertevorrat.")
    require(wertevorrat.hatIntegralBegriff()) { "Der Wertevorrat definiert keinen Integralbegriff." }
    val integriert = when (wert) {
        is ZahlAusdruck -> integrieren(wert, x).ergebnis
        is SpaltenVektor -> SpaltenVektor(wert.werte.map { integrieren(it, x).ergebnis })
        is ZeilenVektor -> ZeilenVektor(wert.werte.map { integrieren(it, x).ergebnis })
        else -> error("Die Methode muss eine Zahl oder einen orientierten Vektor ausgeben.")
    }
    return methode.copy(name = "\\int ${methode.name}", ausgaben = mapOf(ausgabe to integriert))
}

fun bildeAb(menge: MengenAusdruck, methode: Funktion): MengenAusdruck {
    require(methode.parameter.size == 1) { "Die Abbildung muss genau einen freien Parameter besitzen." }
    val (ausgabe, _) = methode.einzigeAusgabe()
    if (menge !is EndlicheMenge) return Abbild(menge, methode)
    val parameter = methode.parameter.single()
    return EndlicheMenge(menge.elemente.map { element ->
        methode.wendeAn(mapOf(parameter.name to element)).getValue(ausgabe)
    }.toSet())
}

data class GebundeneFunktion(val funktion: Funktion, val bindungen: Map<String, MathematischesObjekt>) : MathematischesObjekt {
    val freieParameter get() = funktion.parameter.filterNot { it.name in bindungen }
    override fun zuLatex(): String = funktion.copy(
        parameter = freieParameter,
        ausgaben = funktion.ausgaben.mapValues { ersetze(it.value, bindungen) },
        zielMengen = funktion.zielMengen.mapValues { ersetze(it.value, bindungen) as MengenAusdruck },
        werteVorräte = funktion.werteVorräte.filterKeys { it !in bindungen }.mapValues { ersetze(it.value, bindungen) as MengenAusdruck },
    ).zuLatex()
    fun binde(weitere: Map<String, MathematischesObjekt>) = GebundeneFunktion(funktion, bindungen + weitere)
    fun auswerten(): Map<String, MathematischesObjekt> {
        require(freieParameter.isEmpty()) { "Die Funktion besitzt noch freie Parameter." }
        return funktion.wendeAn(bindungen)
    }
}

fun ersetze(ausdruck: ZahlAusdruck, bindungen: Map<String, MathematischesObjekt>): ZahlAusdruck =
    ersetze(ausdruck as MathematischesObjekt, bindungen) as ZahlAusdruck

/** Typsichere Aussage-Substitution, die die Struktur der Aussage erhält. */
fun ersetze(aussage: Aussage, bindungen: Map<String, MathematischesObjekt>): Aussage =
    ersetze(aussage as MathematischesObjekt, bindungen) as Aussage

/** Rekursive, typübergreifende Substitution für Funktionsausgaben und Zielmengen. */
fun ersetze(objekt: MathematischesObjekt, bindungen: Map<String, MathematischesObjekt>): MathematischesObjekt = when (objekt) {
    is FunktionsParameter -> bindungen[objekt.name] ?: objekt
    is Addition -> addition(objekt.summanden.map { ersetze(it, bindungen) })
    is Multiplikation -> multiplikation(objekt.faktoren.map { ersetze(it, bindungen) })
    is Maximum -> maximum(objekt.operanden.map { ersetze(it, bindungen) })
    is Minimum -> minimum(objekt.operanden.map { ersetze(it, bindungen) })
    is Division -> Division(ersetze(objekt.dividend, bindungen), ersetze(objekt.divisor, bindungen))
    is Potenz -> Potenz(ersetze(objekt.basis, bindungen), ersetze(objekt.exponent, bindungen))
    is Betrag -> Betrag(ersetze(objekt.argument, bindungen))
    is Sinus -> Sinus(ersetze(objekt.argument, bindungen))
    is Cosinus -> Cosinus(ersetze(objekt.argument, bindungen))
    is ArcSinus -> ArcSinus(ersetze(objekt.argument, bindungen))
    is ArcCosinus -> ArcCosinus(ersetze(objekt.argument, bindungen))
    is Exponentialfunktion -> Exponentialfunktion(ersetze(objekt.argument, bindungen))
    is NatürlicherLogarithmus -> NatürlicherLogarithmus(ersetze(objekt.argument, bindungen))
    is Wurzel -> Wurzel(ersetze(objekt.argument, bindungen))
    is KomplexeZahl -> KomplexeZahl(ersetze(objekt.realteil, bindungen), ersetze(objekt.imaginärteil, bindungen))
    is Logarithmus -> Logarithmus(ersetze(objekt.basis, bindungen), ersetze(objekt.argument, bindungen))
    is Argument -> Argument(ersetze(objekt.zahl, bindungen) as KomplexeZahl)
    is EndlicheMenge -> EndlicheMenge(objekt.elemente.map { ersetze(it, bindungen) }.toSet())
    is ReellesIntervall -> reellesIntervall(
        links = ersetze(objekt.links, bindungen),
        linksOffen = objekt.linksOffen,
        rechts = ersetze(objekt.rechts, bindungen),
        rechtsOffen = objekt.rechtsOffen,
    )
    is Vereinigung -> vereinige(objekt.mengen.map { ersetze(it, bindungen) as MengenAusdruck })
    is Schnitt -> schneide(objekt.mengen.map { ersetze(it, bindungen) as MengenAusdruck }, objekt.grundMenge?.let { ersetze(it, bindungen) as MengenAusdruck })
    is MengenDifferenz -> mengenDifferenz(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck)
    is KartesischesProdukt -> kartesischesProdukt(objekt.mengen.map { ersetze(it, bindungen) as MengenAusdruck })
    is Tupelraum -> Tupelraum(objekt.komponenten.map { ersetze(it, bindungen) as MengenAusdruck })
    is Folgenraum -> Folgenraum(ersetze(objekt.elementMenge, bindungen) as MengenAusdruck)
    is Vektorraum -> objekt.copy(skalarMenge = ersetze(objekt.skalarMenge, bindungen) as MengenAusdruck)
    is Matrizenraum -> objekt.copy(skalarMenge = ersetze(objekt.skalarMenge, bindungen) as MengenAusdruck)
    is DefinierteMenge -> {
        val gebundeneNamen = objekt.variablen.map { it.variable.name }.toSet()
        val freieBindungen = bindungen - gebundeneNamen
        objekt.copy(
            variablen = objekt.variablen.map { it.copy(grundMenge = ersetze(it.grundMenge, freieBindungen) as MengenAusdruck) },
            bedingung = ersetze(objekt.bedingung, freieBindungen),
        )
    }
    is GefilterteMenge -> filtereMenge(
        ersetze(objekt.menge, bindungen) as MengenAusdruck,
        ersetze(objekt.methode, bindungen) as Funktion,
    )
    is FallAusdruck -> FallAusdruck(
        wahr = ersetze(objekt.wahr, bindungen),
        aussage = ersetze(objekt.aussage, bindungen),
        lüge = ersetze(objekt.lüge, bindungen),
    )
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
    is Ungleichheit -> Ungleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
    is Vergleich -> Vergleich(ersetze(objekt.links, bindungen), objekt.art, ersetze(objekt.rechts, bindungen))
    is Negation -> Negation(ersetze(objekt.aussage, bindungen) as Aussage)
    is Konjunktion -> Konjunktion(objekt.aussagen.map { ersetze(it, bindungen) as Aussage })
    is Disjunktion -> Disjunktion(objekt.aussagen.map { ersetze(it, bindungen) as Aussage })
    is Implikation -> Implikation(ersetze(objekt.voraussetzung, bindungen) as Aussage, ersetze(objekt.folgerung, bindungen) as Aussage)
    is Äquivalenz -> Äquivalenz(ersetze(objekt.links, bindungen) as Aussage, ersetze(objekt.rechts, bindungen) as Aussage)
    is Adjunktion -> Adjunktion(ersetze(objekt.links, bindungen) as Aussage, ersetze(objekt.rechts, bindungen) as Aussage)
    is ElementBeziehung -> ElementBeziehung(ersetze(objekt.element, bindungen), ersetze(objekt.menge, bindungen) as MengenAusdruck)
    is TeilmengenBeziehung -> TeilmengenBeziehung(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck)
    is EchteTeilmengeBeziehung -> EchteTeilmengeBeziehung(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck)
    is ObermengenBeziehung -> ObermengenBeziehung(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck, objekt.echt)
    is Disjunktheit -> Disjunktheit(ersetze(objekt.links, bindungen) as MengenAusdruck, ersetze(objekt.rechts, bindungen) as MengenAusdruck)
    is Tupel -> Tupel(objekt.elemente.map { ersetze(it, bindungen) })
    is SpaltenVektor -> SpaltenVektor(objekt.werte.map { ersetze(it, bindungen) })
    is ZeilenVektor -> ZeilenVektor(objekt.werte.map { ersetze(it, bindungen) })
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
    is IterierteKonjunktion -> objekt.copy(indexMenge = ersetze(objekt.indexMenge, bindungen) as MengenAusdruck)
    is IterierteDisjunktion -> objekt.copy(indexMenge = ersetze(objekt.indexMenge, bindungen) as MengenAusdruck)
    is IterierteAdjunktion -> objekt.copy(indexMenge = ersetze(objekt.indexMenge, bindungen) as MengenAusdruck)
    else -> objekt
}

private fun vereinfacheObjekt(
    objekt: MathematischesObjekt,
    kontext: RechenKontext = RechenKontext(),
): MathematischesObjekt = when (objekt) {
    is ZahlAusdruck -> vereinfache(objekt, kontext)
    is EndlicheMenge -> EndlicheMenge(objekt.elemente.map { vereinfacheObjekt(it, kontext) }.toSet())
    is ReellesIntervall -> reellesIntervall(objekt.links, objekt.linksOffen, objekt.rechts, objekt.rechtsOffen, kontext)
    is Vereinigung -> vereinige(objekt.mengen)
    is Schnitt -> schneide(objekt.mengen, objekt.grundMenge)
    is GefilterteMenge -> filtereMenge(objekt.menge, objekt.methode, kontext)
    is FallAusdruck -> {
        val wahr = vereinfacheObjekt(objekt.wahr, kontext)
        val lüge = vereinfacheObjekt(objekt.lüge, kontext)
        if (wahr == lüge) wahr else when (objekt.aussage.entscheide(kontext).wahrheitswert) {
            Wahrheitswert.Wahr -> wahr
            Wahrheitswert.Lüge -> lüge
            null -> objekt.copy(wahr = wahr, lüge = lüge)
        }
    }
    else -> objekt
}

/** Rekursive Analyse aller bindbaren Funktionsparameter. */
fun MathematischesObjekt.enthalteneFunktionsParameter(): Set<FunktionsParameter> = when (this) {
    is FunktionsParameter -> setOf(this)
    is Addition -> summanden.enthalteneFunktionsParameter()
    is Multiplikation -> faktoren.enthalteneFunktionsParameter()
    is Division -> listOf(dividend, divisor).enthalteneFunktionsParameter()
    is Potenz -> listOf(basis, exponent).enthalteneFunktionsParameter()
    is Betrag -> argument.enthalteneFunktionsParameter()
    is Sinus -> argument.enthalteneFunktionsParameter()
    is Cosinus -> argument.enthalteneFunktionsParameter()
    is ArcSinus -> argument.enthalteneFunktionsParameter()
    is ArcCosinus -> argument.enthalteneFunktionsParameter()
    is Exponentialfunktion -> argument.enthalteneFunktionsParameter()
    is NatürlicherLogarithmus -> argument.enthalteneFunktionsParameter()
    is Wurzel -> argument.enthalteneFunktionsParameter()
    is KomplexeZahl -> listOf(realteil, imaginärteil).enthalteneFunktionsParameter()
    is Logarithmus -> listOf(basis, argument).enthalteneFunktionsParameter()
    is Argument -> zahl.enthalteneFunktionsParameter()
    is EndlicheMenge -> elemente.enthalteneFunktionsParameter()
    is ReellesIntervall -> listOf(links, rechts).enthalteneFunktionsParameter()
    is Vereinigung -> mengen.enthalteneFunktionsParameter()
    is Schnitt -> (mengen + listOfNotNull(grundMenge)).enthalteneFunktionsParameter()
    is MengenDifferenz -> listOf(links, rechts).enthalteneFunktionsParameter()
    is KartesischesProdukt -> mengen.enthalteneFunktionsParameter()
    is DefinierteMenge -> {
        val gebundeneNamen = variablen.map { it.variable.name }.toSet()
        variablen.map { it.grundMenge }.enthalteneFunktionsParameter() +
            bedingung.enthalteneFunktionsParameter().filterNot { it.name in gebundeneNamen }
    }
    is GefilterteMenge -> setOf(menge, methode).enthalteneFunktionsParameter()
    is Tupel -> elemente.enthalteneFunktionsParameter()
    is SpaltenVektor -> werte.enthalteneFunktionsParameter()
    is ZeilenVektor -> werte.enthalteneFunktionsParameter()
    is Matrix -> zeilen.flatten().enthalteneFunktionsParameter()
    is FallAusdruck -> listOf(wahr, aussage, lüge).enthalteneFunktionsParameter()
    is Gleichheit -> listOf(links, rechts).enthalteneFunktionsParameter()
    is Ungleichheit -> listOf(links, rechts).enthalteneFunktionsParameter()
    is Vergleich -> listOf(links, rechts).enthalteneFunktionsParameter()
    is Negation -> aussage.enthalteneFunktionsParameter()
    is Konjunktion -> aussagen.enthalteneFunktionsParameter()
    is Disjunktion -> aussagen.enthalteneFunktionsParameter()
    is Implikation -> listOf(voraussetzung, folgerung).enthalteneFunktionsParameter()
    is Äquivalenz -> listOf(links, rechts).enthalteneFunktionsParameter()
    is Adjunktion -> listOf(links, rechts).enthalteneFunktionsParameter()
    is ElementBeziehung -> listOf(element, menge).enthalteneFunktionsParameter()
    is TeilmengenBeziehung -> listOf(links, rechts).enthalteneFunktionsParameter()
    is EchteTeilmengeBeziehung -> listOf(links, rechts).enthalteneFunktionsParameter()
    is ObermengenBeziehung -> listOf(links, rechts).enthalteneFunktionsParameter()
    is Disjunktheit -> listOf(links, rechts).enthalteneFunktionsParameter()
    is Funktion -> {
        val gebundeneNamen = parameter.map { it.name }.toSet()
        (ausgaben.values + zielMengen.values + werteVorräte.values)
            .enthalteneFunktionsParameter()
            .filterNot { it.name in gebundeneNamen }
            .toSet()
    }
    is GebundeneFunktion -> {
        val gebundeneNamen = bindungen.keys
        funktion.enthalteneFunktionsParameter().filterNot { it.name in gebundeneNamen }.toSet() +
            bindungen.values.enthalteneFunktionsParameter()
    }
    is Abbild -> setOf(menge, methode).enthalteneFunktionsParameter()
    is IterierteSumme -> setOf(methode, indexMenge).enthalteneFunktionsParameter()
    is IteriertesProdukt -> setOf(methode, indexMenge).enthalteneFunktionsParameter()
    is IterierteVereinigung -> setOf(methode, indexMenge).enthalteneFunktionsParameter()
    is IterierterSchnitt -> setOf(methode, indexMenge).enthalteneFunktionsParameter()
    is IteriertesKartesischesProdukt -> setOf(methode, indexMenge).enthalteneFunktionsParameter()
    is IterierteKonjunktion -> setOf(methode, indexMenge).enthalteneFunktionsParameter()
    is IterierteDisjunktion -> setOf(methode, indexMenge).enthalteneFunktionsParameter()
    is IterierteAdjunktion -> setOf(methode, indexMenge).enthalteneFunktionsParameter()
    else -> emptySet()
}

/** Rekursive Analyse der weiterhin ausschließlich numerischen Variablen. */
fun MathematischesObjekt.enthalteneVariablen(): Set<Variable> = enthalteneFunktionsParameter().filterIsInstance<Variable>().toSet()

fun MathematischesObjekt.enthältVariable(variable: Variable): Boolean =
    enthalteneVariablen().any { it.name == variable.name }

/** Zentrale Analyse der freien Variablen eines mathematischen Objekts. */
fun MathematischesObjekt.freieVariablen(): Set<Variable> = enthalteneVariablen()
fun MathematischesObjekt.freieFunktionsParameter(): Set<FunktionsParameter> = enthalteneFunktionsParameter()

private fun Iterable<MathematischesObjekt>.enthalteneFunktionsParameter(): Set<FunktionsParameter> =
    flatMap { it.enthalteneFunktionsParameter() }.toSet()
