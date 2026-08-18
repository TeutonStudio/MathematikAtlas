package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Konkrete symbolische Implementierung mathematischer Methoden.
 *
 * [Methode] ist der domänenneutrale Obervertrag. Diese Implementierung ergänzt die
 * mathematische Objekt-, Auswertungs- und Raumsemantik. Die historischen Felder
 * [vorschrift], [zielMenge], [werteVorräte] und [effektiverWerteVorrat] bleiben für
 * Quell-/Ladekompatibilität erhalten; alle neue Semantik läuft über
 * [kanonischeVorschrift] und [mathematischeSignatur].
 */
data class MathematischeMethode(
    override val name: String,
    val parameter: List<MethodenParameter>,
    /** Historische gespeicherte Vorschrift; kanonisch wird immer [kanonischeVorschrift] verwendet. */
    val vorschrift: MathematischesObjekt,
    /** Historische Zielmengenprojektion; kanonisch ist [mathematischeSignatur].zielRaum. */
    val zielMenge: MengenAusdruck,
    /** Historische Komponenten-Definitionsmengen, in derselben Reihenfolge wie [parameter]. */
    val werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    val ausgabeNamen: List<String> = listOf("wert"),
    /** @see MathematischeMethodenSignatur.effektiverDefinitionsRaum */
    val effektiverWerteVorrat: MengenAusdruck? = null,
    /** Strukturierte Herkunft einer Restriktion oder Bereichsanpassung. */
    override val bereichsanpassung: MethodenBereichsanpassung? = null,
) : MathematischAuswertbareMethode {
    init {
        require(parameter.map { it.name }.distinct().size == parameter.size) {
            "Methodenparameter müssen eindeutige Namen haben."
        }
        require(werteVorräte.keys.all { key -> parameter.any { it.name == key } }) {
            "Wertevorräte dürfen nur für Parameter definiert werden."
        }
        require(ausgabeNamen.distinct().size == ausgabeNamen.size) {
            "Öffentliche Methodenausgaben benötigen eindeutige Namen."
        }
        when (ausgabeNamen.size) {
            0 -> require(vorschrift is Tupel && vorschrift.elemente.isEmpty()) {
                "Eine Methode ohne Ergebnisse benötigt als Vorschrift das leere Tupel ()."
            }
            1 -> Unit
            else -> require(vorschrift is Tupel && vorschrift.elemente.size == ausgabeNamen.size) {
                "Mehrere öffentliche Ausgaben müssen als geordnetes Ergebnistupel gespeichert werden."
            }
        }
        when (ausgabeNamen.size) {
            0 -> require(zielMenge == LeereMenge || zielMenge is Tupelraum && zielMenge.komponenten.isEmpty()) {
                "Eine Methode ohne Ergebnisse benötigt den leeren Tupel-Zielraum {()}."
            }
            1 -> Unit
            else -> require(zielMenge is Tupelraum && zielMenge.komponenten.size == ausgabeNamen.size) {
                "Mehrere öffentliche Ausgaben benötigen einen synchronen Produktzielraum."
            }
        }
    }

    /** Kanonische Vorschrift als Ergebnistupel, einschließlich 0 und 1 Ergebnis. */
    val kanonischeVorschrift: Tupel
        get() = when (ausgabeNamen.size) {
            0 -> Tupel(emptyList())
            1 -> Tupel(listOf(vorschrift))
            else -> vorschrift as Tupel
        }

    private val zielKomponenten: List<MengenAusdruck>
        get() = when (ausgabeNamen.size) {
            0 -> emptyList()
            1 -> listOf(zielMenge)
            else -> (zielMenge as Tupelraum).komponenten
        }

    override val mathematischeSignatur: MathematischeMethodenSignatur
        get() = MathematischeMethodenSignatur(
            argumente = parameter.mapIndexed { index, parameter ->
                MathematischeArgumentKomponente(
                    id = "argument-${index + 1}",
                    name = parameter.name,
                    position = index,
                    parameter = parameter,
                    definitionsMenge = werteVorräte[parameter.name]
                        ?: error("Für das Methodenargument '${parameter.name}' konnte keine Definitionsmenge ermittelt werden."),
                )
            },
            ergebnisse = ausgabeNamen.mapIndexed { index, ausgabeName ->
                MathematischeErgebnisKomponente(
                    id = "ergebnis-${index + 1}",
                    name = ausgabeName,
                    position = index,
                    zielMenge = zielKomponenten[index],
                )
            },
            effektiverDefinitionsRaum = effektiverWerteVorrat,
        )

    override val signatur: MethodenSignatur
        get() = MethodenSignatur(
            argumente = mathematischeSignatur.argumente.map { argument ->
                MethodenKomponente(
                    id = argument.id,
                    name = argument.name,
                    position = argument.position,
                    typ = argument.definitionsMenge.elementTypAusdruck(),
                )
            },
            ergebnisse = mathematischeSignatur.ergebnisse.map { ergebnis ->
                MethodenKomponente(
                    id = ergebnis.id,
                    name = ergebnis.name,
                    position = ergebnis.position,
                    typ = ergebnis.zielMenge.elementTypAusdruck(),
                )
            },
        )

    /**
     * Ausschließlich für Lademigrationen und historische Testdaten. Alte skalare
     * Einzelausgaben werden beim Zugriff auf die kanonische Signatur zu Einertupeln.
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
        vorschrift = historischeVorschrift(name, ausgaben),
        zielMenge = historischeZielMenge(name, ausgaben, zielMengen),
        werteVorräte = werteVorräte,
        ausgabeNamen = ausgaben.keys.toList(),
    )

    fun vorschriftFür(ausgabe: String): MathematischesObjekt {
        val index = ausgabeNamen.indexOf(ausgabe)
        require(index >= 0) { "Die Methode '$name' besitzt keine öffentliche Ausgabe '$ausgabe'." }
        return kanonischeVorschrift.elemente[index]
    }

    override fun zuLatex(): String = zuFallunterscheidungsLatex()

    /** Gemeinsame große Darstellung einer Methode mit kanonischem Tupelraum und Tupelbild. */
    fun zuFallunterscheidungsLatex(): String {
        val argumentTupel = parameter.joinToString(",", prefix = "\\left(", postfix = "\\right)") { it.zuLatex() }
        val bild = if (ausgabeNamen.size == 1 && vorschrift is AbleitungsMethodenAusdruck) {
            val aufrufArgumente = parameter.joinToString(",") { it.zuLatex() }
            "\\left(${vorschrift.zuLatex()}\\left($aufrufArgumente\\right)\\right)"
        } else {
            kanonischeVorschrift.zuLatex()
        }
        return "$name:\\begin{cases}" +
            "${mathematischeSignatur.definitionsRaum.zuLatex()} \\longrightarrow " +
            "${mathematischeSignatur.zielRaum.zuLatex()}\\\\" +
            "$argumentTupel \\mapsto $bild\\end{cases}"
    }

    fun zielMengeFür(ausgabe: String): MengenAusdruck {
        val ergebnis = mathematischeSignatur.ergebnisse.firstOrNull { it.name == ausgabe }
            ?: error("Die Methode '$name' besitzt keine öffentliche Ausgabe '$ausgabe'.")
        check(ergebnis.zielMenge !is FehlendeObermenge) {
            "Für die öffentliche Ausgabe '$ausgabe' der Methode '$name' fehlt die Zielmenge."
        }
        return ergebnis.zielMenge
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

    /**
     * Kanonische mathematische Auswertung. Argument und Ergebnis bleiben unabhängig
     * von ihrer Stelligkeit Tupel.
     */
    override fun wendeKanonischMathematischAn(argumente: Tupel): Tupel {
        require(argumente.elemente.size == parameter.size) {
            "Die Methode '$name' erwartet ${parameter.size} Argumente, erhielt aber ${argumente.elemente.size}."
        }
        val bindungen = parameter.map(MethodenParameter::name).zip(argumente.elemente).toMap()
        return Tupel(
            kanonischeVorschrift.elemente.map { ausdruck ->
                vereinfacheObjekt(ersetze(ausdruck, bindungen))
            },
        )
    }

    /** Historische skalare Projektion für bestehende Mathematikoperatoren. */
    fun wendeAn(argumente: List<MathematischesObjekt>): MathematischesObjekt =
        projiziereLegacyErgebnis(wendeKanonischMathematischAn(Tupel(argumente)))

    /** Historische namenbasierte Projektion für bestehende Mathematikoperatoren. */
    override fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
        val fehlend = parameter.map(MethodenParameter::name).filterNot(argumente::containsKey)
        require(fehlend.isEmpty()) {
            "Für die Methode '$name' fehlen die Argumente ${fehlend.joinToString()}."
        }
        val ungeordnete = argumente.keys - parameter.map(MethodenParameter::name).toSet()
        require(ungeordnete.isEmpty()) {
            "Für die Methode '$name' wurden unbekannte Argumente ${ungeordnete.joinToString()} übergeben."
        }
        val tupel = Tupel(parameter.map { argumente.getValue(it.name) })
        return projiziereLegacyErgebnis(wendeKanonischMathematischAn(tupel))
    }

    fun einzigeAusgabe(): Pair<String, MathematischesObjekt> {
        require(ausgabeNamen.size == 1) { "Die Methode '$name' muss genau eine öffentliche Ausgabe besitzen." }
        return ausgabeNamen.single() to kanonischeVorschrift.elemente.single()
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

private fun projiziereLegacyErgebnis(ergebnis: Tupel): MathematischesObjekt =
    if (ergebnis.elemente.size == 1) ergebnis.elemente.single() else ergebnis

private fun historischeVorschrift(
    name: String,
    ausgaben: Map<String, MathematischesObjekt>,
): MathematischesObjekt {
    require(ausgaben.isNotEmpty()) { "Die historische Methode '$name' benötigt mindestens eine Ausgabe." }
    return if (ausgaben.size == 1) ausgaben.values.single() else Tupel(ausgaben.values.toList())
}

private fun historischeZielMenge(
    name: String,
    ausgaben: Map<String, MathematischesObjekt>,
    zielMengen: Map<String, MengenAusdruck>,
): MengenAusdruck {
    require(zielMengen.keys.all(ausgaben::containsKey)) {
        "Zielmengen dürfen nur für vorhandene Ausgaben definiert werden."
    }
    val ziele = ausgaben.keys.map { ausgabe ->
        zielMengen[ausgabe] ?: FehlendeObermenge("methode.$name.$ausgabe")
    }
    return if (ziele.size == 1) ziele.single() else Tupelraum(ziele)
}

/** Erzeugt eine [höhe] mal [breite]-Matrix aus einer mathematischen Zahlmethode `f(zeile, spalte)`. */
fun matrixAusMethode(methode: Methode, höhe: Int, breite: Int): Matrix {
    require(höhe > 0) { "Die Matrixhöhe muss positiv sein." }
    require(breite > 0) { "Die Matrixbreite muss positiv sein." }
    val mathematisch = methode.alsMathematischeMethode("Matrixkonstruktion")
    require(mathematisch.parameter.size == 2) { "Die Matrixmethode muss genau zwei Parameter für Zeile und Spalte besitzen." }
    val (_, ausgabe) = mathematisch.einzigeAusgabe()
    require(ausgabe is ZahlAusdruck) { "Die Matrixmethode muss eine Zahl ausgeben." }
    return Matrix(List(höhe) { zeile ->
        List(breite) { spalte ->
            mathematisch.wendeAn(
                listOf(
                    RationaleZahl.von(zeile.toLong()),
                    RationaleZahl.von(spalte.toLong()),
                ),
            ) as? ZahlAusdruck
                ?: error("Die Matrixmethode muss für jeden Index eine Zahl ausgeben.")
        }
    })
}

/** Erzeugt das Tupel `(f(1), ..., f(n))` aus einer einstelligen mathematischen Methode. */
fun tupelAusMethode(methode: Methode, dimension: Int): Tupel {
    require(dimension > 0) { "Die Tupeldimension muss positiv sein." }
    val mathematisch = methode.alsMathematischeMethode("Tupelkonstruktion")
    require(mathematisch.parameter.size == 1) { "Die Tupelmethode muss genau einen Indexparameter besitzen." }
    return Tupel(
        List(dimension) { index ->
            mathematisch.wendeAn(listOf(RationaleZahl.von((index + 1).toLong())))
        },
    )
}

/** Bild einer Menge unter einer einwertigen mathematischen Methode: f[M] = { f(x) : x ∈ M }. */
data class Abbild(val menge: MengenAusdruck, val methode: Methode) : MengenAusdruck {
    init { methode.alsMathematischeMethode("Bildmengenbildung") }
    override fun zuLatex() = "${methode.name}[${menge.zuLatex()}]"
}

fun MengenAusdruck.hatDifferentialBegriff() = this == ReelleZahlen
fun MengenAusdruck.hatIntegralBegriff() = this == ReelleZahlen

private fun Methode.einwertigeZahlMethode(): Triple<Variable, String, ZahlAusdruck> {
    val mathematisch = alsMathematischeMethode("symbolische Komposition oder Iteration")
    require(mathematisch.parameter.size == 1 && mathematisch.ausgabeNamen.size == 1) {
        "Die Methode muss genau einen Parameter und eine Ausgabe besitzen."
    }
    val (name, ausgabe) = mathematisch.einzigeAusgabe()
    return Triple(
        mathematisch.parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen."),
        name,
        ausgabe as? ZahlAusdruck ?: error("Die Methode muss eine Zahl ausgeben."),
    )
}

/**
 * Prüft den mathematischen Übergang mit der exakten Bildmenge, soweit sie für einen
 * endlichen Definitionsbereich berechenbar ist, andernfalls konservativ über die
 * Zielmenge der inneren Methode.
 */
fun prüfeMathematischenKompositionsÜbergang(
    innen: Methode,
    außen: Methode,
): AussageErgebnis {
    val innenMathematisch = innen.alsMathematischeMethode("mathematische Komposition")
    val außenMathematisch = außen.alsMathematischeMethode("mathematische Komposition")
    require(innenMathematisch.parameter.size == 1 && innenMathematisch.ausgabeNamen.size == 1)
    require(außenMathematisch.parameter.size == 1)

    val innenDefinition = innenMathematisch.mathematischeSignatur.argumente.single().definitionsMenge
    val außenDefinition = außenMathematisch.mathematischeSignatur.argumente.single().definitionsMenge
    if (innenDefinition is EndlicheMenge) {
        val exaktesBild = bildeAb(innenDefinition, innenMathematisch)
        return prüfeTeilmenge(exaktesBild, außenDefinition)
    }
    val inneresZiel = innenMathematisch.mathematischeSignatur.ergebnisse.single().zielMenge
    return prüfeTeilmenge(inneresZiel, außenDefinition)
}

fun komponiere(außen: Methode, innen: Methode): Methode {
    val außenMathematisch = außen.alsMathematischeMethode("symbolische Komposition")
    val innenMathematisch = innen.alsMathematischeMethode("symbolische Komposition")
    val (x, ausgabeAußen, termAußen) = außenMathematisch.einwertigeZahlMethode()
    val (t, _, termInnen) = innenMathematisch.einwertigeZahlMethode()
    val übergang = prüfeMathematischenKompositionsÜbergang(innenMathematisch, außenMathematisch)
    require(übergang.wahrheitswert == Wahrheitswert.Wahr) {
        when (übergang.wahrheitswert) {
            Wahrheitswert.Lüge -> "Die Bildmenge der inneren Methode liegt nicht im Definitionsbereich der äußeren Methode."
            null -> "Die Kompatibilität der Bild-/Zielmenge der inneren Methode mit dem Definitionsbereich der äußeren Methode ist unbestimmt."
            Wahrheitswert.Wahr -> error("unerreichbar")
        }
    }
    val definitionsMenge = innenMathematisch.mathematischeSignatur.argumente.single().definitionsMenge
    return Methode(
        name = "${außenMathematisch.name}\\circ${innenMathematisch.name}",
        parameter = listOf(t),
        vorschrift = ersetze(termAußen, mapOf(x.name to termInnen)),
        zielMenge = außenMathematisch.zielMengeFür(ausgabeAußen),
        werteVorräte = mapOf(t.name to definitionsMenge),
        effektiverWerteVorrat = innenMathematisch.mathematischeSignatur.effektiverDefinitionsRaum,
    )
}

fun iteriere(methode: Methode, exponent: Int): Methode {
    val mathematisch = methode.alsMathematischeMethode("symbolische Iteration")
    require(exponent >= 0) { "Der Iterationsexponent muss nichtnegativ sein." }
    val (x, _, _) = mathematisch.einwertigeZahlMethode()
    val definitionsMenge = mathematisch.mathematischeSignatur.argumente.single().definitionsMenge
    val endomorphismus = prüfeTeilmenge(
        mathematisch.mathematischeSignatur.ergebnisse.single().zielMenge,
        definitionsMenge,
    )
    require(endomorphismus.wahrheitswert == Wahrheitswert.Wahr) {
        "Iteration benötigt den Nachweis, dass die Bild-/Zielmenge im Definitionsbereich liegt."
    }
    var ergebnis = Methode(
        name = "id",
        parameter = listOf(x),
        vorschrift = x,
        zielMenge = definitionsMenge,
        werteVorräte = mapOf(x.name to definitionsMenge),
    )
    repeat(exponent) {
        ergebnis = komponiere(mathematisch, ergebnis).alsMathematischeMethode("symbolische Iteration")
    }
    return ergebnis.copy(name = "${mathematisch.name}^{${exponent}}")
}

fun differenziereMethode(methode: Methode): Methode {
    val mathematisch = methode.alsMathematischeMethode("symbolische Differentiation")
    require(mathematisch.parameter.size == 1 && mathematisch.ausgabeNamen.size == 1) {
        "Die Methode muss genau einen Parameter und eine Ausgabe besitzen."
    }
    val x = mathematisch.parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen.")
    val (_, wert) = mathematisch.einzigeAusgabe()
    val definitionsMenge = mathematisch.mathematischeSignatur.argumente.single().definitionsMenge
    require(definitionsMenge.hatDifferentialBegriff()) { "Der Definitionsbereich definiert keinen Differentialbegriff." }
    val abgeleitet = when (wert) {
        is ZahlAusdruck -> ableiten(wert, x).ergebnis
        is SpaltenVektor -> SpaltenVektor(wert.werte.map { ableiten(it, x).ergebnis })
        is ZeilenVektor -> ZeilenVektor(wert.werte.map { ableiten(it, x).ergebnis })
        else -> error("Die Methode muss eine Zahl oder einen orientierten Vektor ausgeben.")
    }
    return mathematisch.copy(name = "${mathematisch.name}'", vorschrift = abgeleitet)
}

fun integriereMethode(methode: Methode): Methode {
    val mathematisch = methode.alsMathematischeMethode("symbolische Integration")
    require(mathematisch.parameter.size == 1 && mathematisch.ausgabeNamen.size == 1) {
        "Die Methode muss genau einen Parameter und eine Ausgabe besitzen."
    }
    val x = mathematisch.parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen.")
    val (_, wert) = mathematisch.einzigeAusgabe()
    val definitionsMenge = mathematisch.mathematischeSignatur.argumente.single().definitionsMenge
    require(definitionsMenge.hatIntegralBegriff()) { "Der Definitionsbereich definiert keinen Integralbegriff." }
    val integriert = when (wert) {
        is ZahlAusdruck -> integrieren(wert, x).ergebnis
        is SpaltenVektor -> SpaltenVektor(wert.werte.map { integrieren(it, x).ergebnis })
        is ZeilenVektor -> ZeilenVektor(wert.werte.map { integrieren(it, x).ergebnis })
        else -> error("Die Methode muss eine Zahl oder einen orientierten Vektor ausgeben.")
    }
    return mathematisch.copy(name = "\\int ${mathematisch.name}", vorschrift = integriert)
}

fun bildeAb(menge: MengenAusdruck, methode: Methode): MengenAusdruck {
    val mathematisch = methode.alsMathematischeMethode("Bildmengenbildung")
    require(mathematisch.parameter.size == 1) { "Die Abbildung muss genau einen freien Parameter besitzen." }
    mathematisch.einzigeAusgabe()
    if (menge !is EndlicheMenge) return Abbild(menge, mathematisch)
    val elemente = menge.elemente.map { element -> mathematisch.wendeAn(listOf(element)) }.toSet()
    return EndlicheMenge(elemente)
}

data class GebundeneMethode(
    val methode: MathematischeMethode,
    val bindungen: Map<String, MathematischesObjekt>,
) : MathematischesObjekt {
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
        return methode.wendeMathematischAn(bindungen)
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
    is StrukturierteDivision -> objekt.copy(
        dividend = ersetze(objekt.dividend, bindungen),
        divisor = ersetze(objekt.divisor, bindungen),
    )
    is InversesElement -> InversesElement(ersetze(objekt.argument, bindungen))
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
    is Schnitt -> schneide(
        objekt.mengen.map { ersetze(it, bindungen) as MengenAusdruck },
        objekt.grundMenge?.let { ersetze(it, bindungen) as MengenAusdruck },
    )
    is MengenDifferenz -> mengenDifferenz(
        ersetze(objekt.links, bindungen) as MengenAusdruck,
        ersetze(objekt.rechts, bindungen) as MengenAusdruck,
    )
    is KartesischesProdukt -> kartesischesProdukt(objekt.mengen.map { ersetze(it, bindungen) as MengenAusdruck })
    is Tupelraum -> Tupelraum(objekt.komponenten.map { ersetze(it, bindungen) as MengenAusdruck })
    is Folgenraum -> Folgenraum(ersetze(objekt.elementMenge, bindungen) as MengenAusdruck)
    is Vektorraum -> objekt.copy(skalarMenge = ersetze(objekt.skalarMenge, bindungen) as MengenAusdruck)
    is Matrizenraum -> objekt.copy(skalarMenge = ersetze(objekt.skalarMenge, bindungen) as MengenAusdruck)
    is DefinierteMenge -> {
        val gebundeneNamen = objekt.variablen.map { it.variable.name }.toSet()
        val freieBindungen = bindungen - gebundeneNamen
        objekt.copy(
            variablen = objekt.variablen.map {
                it.copy(grundMenge = ersetze(it.grundMenge, freieBindungen) as MengenAusdruck)
            },
            bedingung = ersetze(objekt.bedingung, freieBindungen),
        )
    }
    is GefilterteMenge -> filtereMenge(
        ersetze(objekt.menge, bindungen) as MengenAusdruck,
        ersetze(objekt.methode, bindungen) as MathematischeMethode,
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
    is Implikation -> Implikation(
        ersetze(objekt.voraussetzung, bindungen) as Aussage,
        ersetze(objekt.folgerung, bindungen) as Aussage,
    )
    is Äquivalenz -> Äquivalenz(
        ersetze(objekt.links, bindungen) as Aussage,
        ersetze(objekt.rechts, bindungen) as Aussage,
    )
    is Adjunktion -> Adjunktion(
        ersetze(objekt.links, bindungen) as Aussage,
        ersetze(objekt.rechts, bindungen) as Aussage,
    )
    is ElementBeziehung -> ElementBeziehung(
        ersetze(objekt.element, bindungen),
        ersetze(objekt.menge, bindungen) as MengenAusdruck,
    )
    is TeilmengenBeziehung -> TeilmengenBeziehung(
        ersetze(objekt.links, bindungen) as MengenAusdruck,
        ersetze(objekt.rechts, bindungen) as MengenAusdruck,
    )
    is EchteTeilmengeBeziehung -> EchteTeilmengeBeziehung(
        ersetze(objekt.links, bindungen) as MengenAusdruck,
        ersetze(objekt.rechts, bindungen) as MengenAusdruck,
    )
    is ObermengenBeziehung -> ObermengenBeziehung(
        ersetze(objekt.links, bindungen) as MengenAusdruck,
        ersetze(objekt.rechts, bindungen) as MengenAusdruck,
        objekt.echt,
    )
    is Disjunktheit -> Disjunktheit(
        ersetze(objekt.links, bindungen) as MengenAusdruck,
        ersetze(objekt.rechts, bindungen) as MengenAusdruck,
    )
    is Tupel -> Tupel(objekt.elemente.map { ersetze(it, bindungen) })
    is SpaltenVektor -> SpaltenVektor(objekt.werte.map { ersetze(it, bindungen) })
    is ZeilenVektor -> ZeilenVektor(objekt.werte.map { ersetze(it, bindungen) })
    is Matrix -> Matrix(objekt.zeilen.map { zeile -> zeile.map { ersetze(it, bindungen) } })
    is MathematischeMethode -> {
        val freieBindungen = bindungen - objekt.parameter.map { it.name }.toSet()
        objekt.copy(
            vorschrift = ersetze(objekt.vorschrift, freieBindungen),
            zielMenge = ersetze(objekt.zielMenge, freieBindungen) as MengenAusdruck,
            werteVorräte = objekt.werteVorräte.mapValues { ersetze(it.value, freieBindungen) as MengenAusdruck },
            effektiverWerteVorrat = objekt.effektiverWerteVorrat?.let {
                ersetze(it, freieBindungen) as MengenAusdruck
            },
            bereichsanpassung = objekt.bereichsanpassung?.ersetze(freieBindungen),
        )
    }
    is Abbild -> Abbild(
        ersetze(objekt.menge, bindungen) as MengenAusdruck,
        ersetze(objekt.methode.alsMathematischeMethode("Bildmengensubstitution"), bindungen) as MathematischeMethode,
    )
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
    is ReellesIntervall -> reellesIntervall(
        objekt.links,
        objekt.linksOffen,
        objekt.rechts,
        objekt.rechtsOffen,
        kontext,
    )
    is Vereinigung -> vereinige(objekt.mengen)
    is Schnitt -> schneide(objekt.mengen, objekt.grundMenge)
    is GefilterteMenge -> filtereMenge(objekt.menge, objekt.methode, kontext)
    is Tupel -> Tupel(objekt.elemente.map { vereinfacheObjekt(it, kontext) })
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
    is GefilterteMenge -> setOf<MathematischesObjekt>(menge, methode).enthalteneMethodenParameter()
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
    is MathematischeMethode -> {
        val gebundeneNamen = parameter.map { it.name }.toSet()
        val direkteParameter = (
            listOf(vorschrift, zielMenge) + werteVorräte.values + listOfNotNull(effektiverWerteVorrat)
        ).enthalteneMethodenParameter()
        val herkunftsParameter = bereichsanpassung?.let { anpassung ->
            (listOf<MathematischesObjekt>(anpassung.basis, anpassung.werteVorrat) +
                anpassung.ergänzungen.flatMap {
                    listOf<MathematischesObjekt>(it.methode, it.werteVorrat, it.effektiverBereich)
                }).enthalteneMethodenParameter()
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
    is Abbild ->
        menge.enthalteneMethodenParameter() +
            methode.alsMathematischeMethode("Bildmengen-Parameteranalyse").enthalteneMethodenParameter()
    is IterierteSumme -> mathematischeParameterAus(methode) + indexMenge.enthalteneMethodenParameter()
    is IteriertesProdukt -> mathematischeParameterAus(methode) + indexMenge.enthalteneMethodenParameter()
    is IterierteVereinigung -> mathematischeParameterAus(methode) + indexMenge.enthalteneMethodenParameter()
    is IterierterSchnitt -> mathematischeParameterAus(methode) + indexMenge.enthalteneMethodenParameter()
    is IteriertesKartesischesProdukt -> mathematischeParameterAus(methode) + indexMenge.enthalteneMethodenParameter()
    is IterierteKonjunktion -> mathematischeParameterAus(methode) + indexMenge.enthalteneMethodenParameter()
    is IterierteDisjunktion -> mathematischeParameterAus(methode) + indexMenge.enthalteneMethodenParameter()
    is IterierteAdjunktion -> mathematischeParameterAus(methode) + indexMenge.enthalteneMethodenParameter()
    else -> emptySet()
}

private fun mathematischeParameterAus(methode: Methode): Set<MethodenParameter> =
    methode.alsMathematischeMethode("mathematische Parameteranalyse").enthalteneMethodenParameter()

/** Rekursive Analyse der weiterhin ausschließlich numerischen Variablen. */
fun MathematischesObjekt.enthalteneVariablen(): Set<Variable> =
    enthalteneMethodenParameter().filterIsInstance<Variable>().toSet()

fun MathematischesObjekt.enthältVariable(variable: Variable): Boolean =
    enthalteneVariablen().any { it.name == variable.name }

/** Zentrale Analyse der freien Variablen eines mathematischen Objekts. */
fun MathematischesObjekt.freieVariablen(): Set<Variable> = enthalteneVariablen()
fun MathematischesObjekt.freieMethodenParameter(): Set<MethodenParameter> = enthalteneMethodenParameter()

private fun Iterable<MathematischesObjekt>.enthalteneMethodenParameter(): Set<MethodenParameter> =
    flatMap { it.enthalteneMethodenParameter() }.toSet()
