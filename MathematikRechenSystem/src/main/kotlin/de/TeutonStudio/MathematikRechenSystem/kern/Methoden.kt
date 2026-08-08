package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Einziger physischer Laufzeittyp für mathematische Methoden.
 *
 * Mehrere öffentliche Kartenausgänge werden als ein geordnetes Tupel in [vorschrift]
 * und als ein Tupelraum in [zielMenge] gespeichert. [ausgabeNamen] bewahrt nur die
 * öffentliche Benennung und Reihenfolge der Tupelkomponenten.
 */
data class Methode(
    val name: String,
    val parameter: List<MethodenParameter>,
    val vorschrift: MathematischesObjekt,
    val zielMenge: MengenAusdruck,
    /** Definitionsmengen der Parameter, in derselben Reihenfolge wie [parameter]. */
    val werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    val ausgabeNamen: List<String> = listOf("wert"),
    /**
     * Optionaler gemeinsamer Definitionsbereich der vollständigen Argumentbelegung.
     * Er überschreibt nur die aus den Parameter-Wertevorräten abgeleitete Gesamtmenge
     * und erlaubt insbesondere nicht-kartesische Restriktionen mehrstelliger Methoden.
     */
    val effektiverWerteVorrat: MengenAusdruck? = null,
    /** Strukturierte Herkunft einer Restriktion mit optionalen Ergänzungszweigen. */
    val bereichsanpassung: MethodenBereichsanpassung? = null,
) : MathematischesObjekt {
    init {
        require(parameter.map { it.name }.distinct().size == parameter.size) {
            "Methodenparameter müssen eindeutige Namen haben."
        }
        require(werteVorräte.keys.all { key -> parameter.any { it.name == key } }) {
            "Wertevorräte dürfen nur für Parameter definiert werden."
        }
        require(ausgabeNamen.isNotEmpty()) { "Eine Methode benötigt genau eine Vorschrift." }
        require(ausgabeNamen.distinct().size == ausgabeNamen.size) {
            "Öffentliche Methodenausgaben benötigen eindeutige Namen."
        }
        if (ausgabeNamen.size > 1) {
            require(vorschrift is Tupel && vorschrift.elemente.size == ausgabeNamen.size) {
                "Mehrere öffentliche Ausgaben müssen als geordnetes Ergebnistupel gespeichert werden."
            }
            require(zielMenge is Tupelraum && zielMenge.komponenten.size == ausgabeNamen.size) {
                "Mehrere öffentliche Ausgaben benötigen einen synchronen Produktzielraum."
            }
        }
    }

    /**
     * Ausschließlich für Lademigrationen und historische Testdaten. Produktiver Code
     * konstruiert Methoden über [vorschrift], [zielMenge] und [ausgabeNamen].
     */
    @Deprecated("Nur für historische Daten; verwende den kanonischen Methoden-Konstruktor.")
    constructor(
        name: String,
        parameter: List<MethodenParameter>,
        ausgaben: Map<String, MathematischesObjekt>,
        zielMengen: Map<String, MengenAusdruck> = emptyMap(),
        werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    ) : this(
        name = name,
        parameter = parameter,
        vorschrift = kanonischeVorschrift(name, ausgaben),
        zielMenge = kanonischeZielMenge(name, ausgaben, zielMengen, werteVorräte),
        werteVorräte = werteVorräte,
        ausgabeNamen = ausgaben.keys.toList(),
    )

    fun vorschriftFür(ausgabe: String): MathematischesObjekt {
        val index = ausgabeNamen.indexOf(ausgabe)
        require(index >= 0) { "Die Methode '$name' besitzt keine öffentliche Ausgabe '$ausgabe'." }
        return if (ausgabeNamen.size == 1) vorschrift else (vorschrift as Tupel).elemente[index]
    }

    override fun zuLatex(): String = zuFallunterscheidungsLatex()

    /** Gemeinsame große Darstellung einer Methode mit Signatur und Termzeile. */
    fun zuFallunterscheidungsLatex(): String {
        val signatur = runCatching { methodenSignatur() }.getOrNull()
        val argumente = parameter.joinToString(",") { it.zuLatex() }
        val argumentTupel = when (parameter.size) {
            0 -> "\\varnothing"
            1 -> argumente
            else -> "\\left($argumente\\right)"
        }
        val bild = when (val ausdruck = vorschrift) {
            is AbleitungsMethodenAusdruck -> {
                val aufrufArgumente = parameter.joinToString(",") { it.zuLatex() }
                "${ausdruck.zuLatex()}\\left($aufrufArgumente\\right)"
            }
            else -> ausdruck.zuLatex()
        }
        return "$name:\\begin{cases}" +
            "${signatur?.werteVorrat?.zuLatex() ?: "?"} \\longrightarrow " +
            "${signatur?.zielMenge?.zuLatex() ?: zielMenge.zuLatex()}\\\\" +
            "$argumentTupel \\mapsto $bild\\end{cases}"
    }

    fun zielMengeFür(ausgabe: String): MengenAusdruck {
        val index = ausgabeNamen.indexOf(ausgabe)
        require(index >= 0) { "Die Methode '$name' besitzt keine öffentliche Ausgabe '$ausgabe'." }
        val ziel = if (ausgabeNamen.size == 1) zielMenge else (zielMenge as Tupelraum).komponenten[index]
        check(ziel !is FehlendeObermenge) {
            "Für die öffentliche Ausgabe '$ausgabe' der Methode '$name' fehlt die Zielmenge."
        }
        return ziel
    }

    fun zielMengeFür(ausgabe: String, bindungen: Map<String, MathematischesObjekt>): MengenAusdruck =
        ersetze(zielMengeFür(ausgabe), bindungen) as MengenAusdruck

    val einzigeZielMenge: MengenAusdruck
        get() = zielMengeFür(einzigeAusgabe().first)

    val grundMenge: MengenAusdruck
        get() = grundMengeFürMengenAusgabe()

    fun grundMengeFürMengenAusgabe(): MengenAusdruck {
        require(parameter.size == 1) { "Die Methode '$name' muss genau einen freien Parameter besitzen." }
        val (_, wert) = einzigeAusgabe()
        require(wert is MengenAusdruck) { "Die Methode '$name' muss eine Menge ausgeben." }
        val grundMenge = einzigeZielMenge
        val parameter = parameter.single() as? Variable
            ?: error("Die mengenwertige Iterationsmethode '$name' benötigt einen Zahlenparameter.")
        require(!grundMenge.enthältVariable(parameter)) {
            "Die Zielmenge der mengenwertigen Methode '$name' darf nicht vom Iterationsparameter " +
                "'${parameter.name}' abhängen, da sie als feste Grundmenge für den leeren Schnitt verwendet wird."
        }
        return grundMenge
    }

    fun binde(bindungen: Map<String, MathematischesObjekt>): GebundeneMethode =
        GebundeneMethode(this, bindungen.filterKeys { key -> parameter.any { it.name == key } })

    /** Kanonische Anwendung mit genau einem Ergebnisobjekt. */
    fun wendeAn(argumente: List<MathematischesObjekt>): MathematischesObjekt {
        require(argumente.size == parameter.size) {
            "Die Methode '$name' erwartet ${parameter.size} Argumente, erhielt aber ${argumente.size}."
        }
        return wendeAn(parameter.map(MethodenParameter::name).zip(argumente).toMap())
    }

    /** Namensbasierte Anwendung mit genau einem Ergebnisobjekt. */
    fun wendeAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
        val fehlend = parameter.map(MethodenParameter::name).filterNot(argumente::containsKey)
        require(fehlend.isEmpty()) {
            "Für die Methode '$name' fehlen die Argumente ${fehlend.joinToString()}."
        }
        return vereinfacheObjekt(ersetze(vorschrift, argumente))
    }

    fun einzigeAusgabe(): Pair<String, MathematischesObjekt> {
        require(ausgabeNamen.size == 1) { "Die Methode '$name' muss genau eine öffentliche Ausgabe besitzen." }
        return ausgabeNamen.single() to vorschriftFür(ausgabeNamen.single())
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
        if (erwartetMengenwert) grundMengeFürMengenAusgabe() else einzigeZielMenge
        return ausgabe
    }
}

private fun kanonischeVorschrift(
    name: String,
    ausgaben: Map<String, MathematischesObjekt>,
): MathematischesObjekt {
    require(ausgaben.isNotEmpty()) { "Die Methode '$name' benötigt eine Vorschrift." }
    return if (ausgaben.size == 1) ausgaben.values.single() else Tupel(ausgaben.values.toList())
}

private fun kanonischeZielMenge(
    name: String,
    ausgaben: Map<String, MathematischesObjekt>,
    zielMengen: Map<String, MengenAusdruck>,
    werteVorräte: Map<String, MengenAusdruck>,
): MengenAusdruck {
    require(zielMengen.keys.all(ausgaben::containsKey)) {
        "Zielmengen dürfen nur für vorhandene Ausgaben definiert werden."
    }
    val ziele = ausgaben.keys.map { ausgabe ->
        zielMengen[ausgabe] ?: FehlendeObermenge("methode.$name.$ausgabe")
    }
    return if (ziele.size == 1) ziele.single() else Tupelraum(ziele)
}

/** Erzeugt eine [höhe] mal [breite]-Matrix aus einer Zahlmethode `f(zeile, spalte)`. */
fun matrixAusMethode(methode: Methode, höhe: Int, breite: Int): Matrix {
    require(höhe > 0) { "Die Matrixhöhe muss positiv sein." }
    require(breite > 0) { "Die Matrixbreite muss positiv sein." }
    require(methode.parameter.size == 2) { "Die Matrixmethode muss genau zwei Parameter für Zeile und Spalte besitzen." }
    val (ausgabeName, ausgabe) = methode.einzigeAusgabe()
    require(ausgabe is ZahlAusdruck) { "Die Matrixmethode muss eine Zahl ausgeben." }
    val (zeilenParameter, spaltenParameter) = methode.parameter
    return Matrix(List(höhe) { zeile ->
        List(breite) { spalte ->
            methode.wendeAn(
                listOf(
                    RationaleZahl.von(zeile.toLong()),
                    RationaleZahl.von(spalte.toLong()),
                ),
            ) as? ZahlAusdruck
                ?: error("Die Matrixmethode muss für jeden Index eine Zahl ausgeben.")
        }
    })
}

/** Bild einer Menge unter einer einwertigen Methode: f[M] = { f(x) : x ∈ M }. */
data class Abbild(val menge: MengenAusdruck, val methode: Methode) : MengenAusdruck {
    override fun zuLatex() = "${methode.name}[${menge.zuLatex()}]"
}

fun MengenAusdruck.hatDifferentialBegriff() = this == ReelleZahlen
fun MengenAusdruck.hatIntegralBegriff() = this == ReelleZahlen

private fun Methode.einwertigeZahlMethode(): Triple<Variable, String, ZahlAusdruck> {
    require(parameter.size == 1 && ausgabeNamen.size == 1) { "Die Methode muss genau einen Parameter und eine Ausgabe besitzen." }
    val (name, ausgabe) = einzigeAusgabe()
    return Triple(parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen."), name, ausgabe as? ZahlAusdruck ?: error("Die Methode muss eine Zahl ausgeben."))
}

fun komponiere(außen: Methode, innen: Methode): Methode {
    val (x, ausgabeAußen, termAußen) = außen.einwertigeZahlMethode()
    val (t, ausgabeInnen, termInnen) = innen.einwertigeZahlMethode()
    val wertevorrat = innen.werteVorräte[t.name] ?: error("Die innere Methode benötigt einen Wertevorrat.")
    val zielInnen = innen.zielMengeFür(ausgabeInnen)
    val wertevorratAußen = außen.werteVorräte[x.name] ?: error("Die äußere Methode benötigt einen Wertevorrat.")
    require(zielInnen == wertevorratAußen) { "Zielmenge der inneren Methode und Wertevorrat der äußeren Methode müssen übereinstimmen." }
    return Methode(
        name = "${außen.name}\\circ${innen.name}",
        parameter = listOf(t),
        vorschrift = ersetze(termAußen, mapOf(x.name to termInnen)),
        zielMenge = außen.zielMengeFür(ausgabeAußen),
        werteVorräte = mapOf(t.name to wertevorrat),
        effektiverWerteVorrat = innen.effektiverWerteVorrat,
    )
}

fun iteriere(methode: Methode, exponent: Int): Methode {
    require(exponent >= 0) { "Der Iterationsexponent muss nichtnegativ sein." }
    val (x, ausgabe, term) = methode.einwertigeZahlMethode()
    val wertevorrat = methode.werteVorräte[x.name] ?: error("Die Methode benötigt einen Wertevorrat.")
    require(methode.zielMengeFür(ausgabe) == wertevorrat) { "Iteration ist nur für Endomorphismen definiert." }
    var ergebnis = Methode(
        name = "id",
        parameter = listOf(x),
        vorschrift = x,
        zielMenge = wertevorrat,
        werteVorräte = mapOf(x.name to wertevorrat),
    )
    repeat(exponent) { ergebnis = komponiere(methode, ergebnis) }
    return ergebnis.copy(name = "${methode.name}^{${exponent}}")
}

fun differenziereMethode(methode: Methode): Methode {
    require(methode.parameter.size == 1 && methode.ausgabeNamen.size == 1) { "Die Methode muss genau einen Parameter und eine Ausgabe besitzen." }
    val x = methode.parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen."); val (ausgabe, wert) = methode.einzigeAusgabe()
    val wertevorrat = methode.werteVorräte[x.name] ?: error("Die Methode benötigt einen Wertevorrat.")
    require(wertevorrat.hatDifferentialBegriff()) { "Der Wertevorrat definiert keinen Differentialbegriff." }
    val abgeleitet = when (wert) {
        is ZahlAusdruck -> ableiten(wert, x).ergebnis
        is SpaltenVektor -> SpaltenVektor(wert.werte.map { ableiten(it, x).ergebnis })
        is ZeilenVektor -> ZeilenVektor(wert.werte.map { ableiten(it, x).ergebnis })
        else -> error("Die Methode muss eine Zahl oder einen orientierten Vektor ausgeben.")
    }
    return methode.copy(name = "${methode.name}'", vorschrift = abgeleitet)
}

fun integriereMethode(methode: Methode): Methode {
    require(methode.parameter.size == 1 && methode.ausgabeNamen.size == 1) { "Die Methode muss genau einen Parameter und eine Ausgabe besitzen." }
    val x = methode.parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen."); val (ausgabe, wert) = methode.einzigeAusgabe()
    val wertevorrat = methode.werteVorräte[x.name] ?: error("Die Methode benötigt einen Wertevorrat.")
    require(wertevorrat.hatIntegralBegriff()) { "Der Wertevorrat definiert keinen Integralbegriff." }
    val integriert = when (wert) {
        is ZahlAusdruck -> integrieren(wert, x).ergebnis
        is SpaltenVektor -> SpaltenVektor(wert.werte.map { integrieren(it, x).ergebnis })
        is ZeilenVektor -> ZeilenVektor(wert.werte.map { integrieren(it, x).ergebnis })
        else -> error("Die Methode muss eine Zahl oder einen orientierten Vektor ausgeben.")
    }
    return methode.copy(name = "\\int ${methode.name}", vorschrift = integriert)
}

fun bildeAb(menge: MengenAusdruck, methode: Methode): MengenAusdruck {
    require(methode.parameter.size == 1) { "Die Abbildung muss genau einen freien Parameter besitzen." }
    methode.einzigeAusgabe()
    if (menge !is EndlicheMenge) return Abbild(menge, methode)
    val parameter = methode.parameter.single()
    return EndlicheMenge(menge.elemente.map { element ->
        methode.wendeAn(listOf(element))
    }.toSet())
}

data class GebundeneMethode(val methode: Methode, val bindungen: Map<String, MathematischesObjekt>) : MathematischesObjekt {
    val freieParameter get() = methode.parameter.filterNot { it.name in bindungen }
    override fun zuLatex(): String = methode.copy(
        parameter = freieParameter,
        vorschrift = ersetze(methode.vorschrift, bindungen),
        zielMenge = ersetze(methode.zielMenge, bindungen) as MengenAusdruck,
        werteVorräte = methode.werteVorräte.filterKeys { it !in bindungen }
            .mapValues { ersetze(it.value, bindungen) as MengenAusdruck },
        effektiverWerteVorrat = methode.effektiverWerteVorrat?.let { ersetze(it, bindungen) as MengenAusdruck },
        bereichsanpassung = methode.bereichsanpassung?.ersetze(bindungen),
    ).zuLatex()
    fun binde(weitere: Map<String, MathematischesObjekt>) = GebundeneMethode(methode, bindungen + weitere)
    fun auswerten(): MathematischesObjekt {
        require(freieParameter.isEmpty()) { "Die Methode besitzt noch freie Parameter." }
        return methode.wendeAn(bindungen)
    }
}

fun ersetze(ausdruck: ZahlAusdruck, bindungen: Map<String, MathematischesObjekt>): ZahlAusdruck =
    ersetze(ausdruck as MathematischesObjekt, bindungen) as ZahlAusdruck

/** Typsichere Aussage-Substitution, die die Struktur der Aussage erhält. */
fun ersetze(aussage: Aussage, bindungen: Map<String, MathematischesObjekt>): Aussage =
    ersetze(aussage as MathematischesObjekt, bindungen) as Aussage

/** Rekursive, typübergreifende Substitution für Methodenausgaben und Zielmengen. */
fun ersetze(objekt: MathematischesObjekt, bindungen: Map<String, MathematischesObjekt>): MathematischesObjekt = when (objekt) {
    is MethodenParameter -> bindungen[objekt.name] ?: objekt
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
        ersetze(objekt.methode, bindungen) as Methode,
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
    is Methode -> {
        val freieBindungen = bindungen - objekt.parameter.map { it.name }.toSet()
        objekt.copy(
            vorschrift = ersetze(objekt.vorschrift, freieBindungen),
            zielMenge = ersetze(objekt.zielMenge, freieBindungen) as MengenAusdruck,
            werteVorräte = objekt.werteVorräte.mapValues { ersetze(it.value, freieBindungen) as MengenAusdruck },
            effektiverWerteVorrat = objekt.effektiverWerteVorrat?.let { ersetze(it, freieBindungen) as MengenAusdruck },
            bereichsanpassung = objekt.bereichsanpassung?.ersetze(freieBindungen),
        )
    }
    is Abbild -> Abbild(ersetze(objekt.menge, bindungen) as MengenAusdruck, ersetze(objekt.methode, bindungen) as Methode)
    is GebundeneMethode -> objekt.copy(bindungen = objekt.bindungen.mapValues { ersetze(it.value, bindungen) })
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

/** Rekursive Analyse aller bindbaren Methodenparameter. */
fun MathematischesObjekt.enthalteneMethodenParameter(): Set<MethodenParameter> = when (this) {
    is MethodenParameter -> setOf(this)
    is Addition -> summanden.enthalteneMethodenParameter()
    is Multiplikation -> faktoren.enthalteneMethodenParameter()
    is Division -> listOf(dividend, divisor).enthalteneMethodenParameter()
    is Potenz -> listOf(basis, exponent).enthalteneMethodenParameter()
    is Betrag -> argument.enthalteneMethodenParameter()
    is Sinus -> argument.enthalteneMethodenParameter()
    is Cosinus -> argument.enthalteneMethodenParameter()
    is ArcSinus -> argument.enthalteneMethodenParameter()
    is ArcCosinus -> argument.enthalteneMethodenParameter()
    is Exponentialfunktion -> argument.enthalteneMethodenParameter()
    is NatürlicherLogarithmus -> argument.enthalteneMethodenParameter()
    is Wurzel -> argument.enthalteneMethodenParameter()
    is KomplexeZahl -> listOf(realteil, imaginärteil).enthalteneMethodenParameter()
    is Logarithmus -> listOf(basis, argument).enthalteneMethodenParameter()
    is Argument -> zahl.enthalteneMethodenParameter()
    is EndlicheMenge -> elemente.enthalteneMethodenParameter()
    is ReellesIntervall -> listOf(links, rechts).enthalteneMethodenParameter()
    is Vereinigung -> mengen.enthalteneMethodenParameter()
    is Schnitt -> (mengen + listOfNotNull(grundMenge)).enthalteneMethodenParameter()
    is MengenDifferenz -> listOf(links, rechts).enthalteneMethodenParameter()
    is KartesischesProdukt -> mengen.enthalteneMethodenParameter()
    is DefinierteMenge -> {
        val gebundeneNamen = variablen.map { it.variable.name }.toSet()
        variablen.map { it.grundMenge }.enthalteneMethodenParameter() +
            bedingung.enthalteneMethodenParameter().filterNot { it.name in gebundeneNamen }
    }
    is GefilterteMenge -> setOf(menge, methode).enthalteneMethodenParameter()
    is Tupel -> elemente.enthalteneMethodenParameter()
    is SpaltenVektor -> werte.enthalteneMethodenParameter()
    is ZeilenVektor -> werte.enthalteneMethodenParameter()
    is Matrix -> zeilen.flatten().enthalteneMethodenParameter()
    is FallAusdruck -> listOf(wahr, aussage, lüge).enthalteneMethodenParameter()
    is Gleichheit -> listOf(links, rechts).enthalteneMethodenParameter()
    is Ungleichheit -> listOf(links, rechts).enthalteneMethodenParameter()
    is Vergleich -> listOf(links, rechts).enthalteneMethodenParameter()
    is Negation -> aussage.enthalteneMethodenParameter()
    is Konjunktion -> aussagen.enthalteneMethodenParameter()
    is Disjunktion -> aussagen.enthalteneMethodenParameter()
    is Implikation -> listOf(voraussetzung, folgerung).enthalteneMethodenParameter()
    is Äquivalenz -> listOf(links, rechts).enthalteneMethodenParameter()
    is Adjunktion -> listOf(links, rechts).enthalteneMethodenParameter()
    is ElementBeziehung -> listOf(element, menge).enthalteneMethodenParameter()
    is TeilmengenBeziehung -> listOf(links, rechts).enthalteneMethodenParameter()
    is EchteTeilmengeBeziehung -> listOf(links, rechts).enthalteneMethodenParameter()
    is ObermengenBeziehung -> listOf(links, rechts).enthalteneMethodenParameter()
    is Disjunktheit -> listOf(links, rechts).enthalteneMethodenParameter()
    is Methode -> {
        val gebundeneNamen = parameter.map { it.name }.toSet()
        val direkteParameter = (listOf(vorschrift, zielMenge) + werteVorräte.values + listOfNotNull(effektiverWerteVorrat))
            .enthalteneMethodenParameter()
        val herkunftsParameter = bereichsanpassung?.let { anpassung ->
            (listOf(anpassung.basis, anpassung.werteVorrat) +
                anpassung.ergänzungen.flatMap { listOf(it.methode, it.werteVorrat, it.effektiverBereich) })
                .enthalteneMethodenParameter()
        }.orEmpty()
        (direkteParameter + herkunftsParameter)
            .filterNot { it.name in gebundeneNamen }
            .toSet()
    }
    is GebundeneMethode -> {
        val gebundeneNamen = bindungen.keys
        methode.enthalteneMethodenParameter().filterNot { it.name in gebundeneNamen }.toSet() +
            bindungen.values.enthalteneMethodenParameter()
    }
    is Abbild -> setOf(menge, methode).enthalteneMethodenParameter()
    is IterierteSumme -> setOf(methode, indexMenge).enthalteneMethodenParameter()
    is IteriertesProdukt -> setOf(methode, indexMenge).enthalteneMethodenParameter()
    is IterierteVereinigung -> setOf(methode, indexMenge).enthalteneMethodenParameter()
    is IterierterSchnitt -> setOf(methode, indexMenge).enthalteneMethodenParameter()
    is IteriertesKartesischesProdukt -> setOf(methode, indexMenge).enthalteneMethodenParameter()
    is IterierteKonjunktion -> setOf(methode, indexMenge).enthalteneMethodenParameter()
    is IterierteDisjunktion -> setOf(methode, indexMenge).enthalteneMethodenParameter()
    is IterierteAdjunktion -> setOf(methode, indexMenge).enthalteneMethodenParameter()
    else -> emptySet()
}

/** Rekursive Analyse der weiterhin ausschließlich numerischen Variablen. */
fun MathematischesObjekt.enthalteneVariablen(): Set<Variable> = enthalteneMethodenParameter().filterIsInstance<Variable>().toSet()

fun MathematischesObjekt.enthältVariable(variable: Variable): Boolean =
    enthalteneVariablen().any { it.name == variable.name }

/** Zentrale Analyse der freien Variablen eines mathematischen Objekts. */
fun MathematischesObjekt.freieVariablen(): Set<Variable> = enthalteneVariablen()
fun MathematischesObjekt.freieMethodenParameter(): Set<MethodenParameter> = enthalteneMethodenParameter()

private fun Iterable<MathematischesObjekt>.enthalteneMethodenParameter(): Set<MethodenParameter> =
    flatMap { it.enthalteneMethodenParameter() }.toSet()
