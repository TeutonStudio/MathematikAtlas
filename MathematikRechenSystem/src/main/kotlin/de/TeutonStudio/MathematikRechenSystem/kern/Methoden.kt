package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypPrüfung

/**
 * Konkrete symbolische Implementierung mathematischer Methoden.
 *
 * [Methode] ist der domänenneutrale Obervertrag. Die hier noch physisch vorhandenen
 * Felder [vorschrift], [zielMenge], [werteVorräte] und [effektiverWerteVorrat] sind
 * die zentrale Legacy-Speicheroberfläche für bestehende Karten. Die fachliche Semantik
 * wird über [mathematischeSignatur], [ergebnisTupel] und [zielRaum] kanonisch als
 * Tupel -> Tupel projiziert. Neu generischer Code darf diese Legacy-Felder nicht lesen.
 */
data class MathematischeMethode(
    override val name: String,
    override val parameter: List<MethodenParameter>,
    override val vorschrift: MathematischesObjekt,
    override val zielMenge: MengenAusdruck,
    /** Legacy-Speicher der Komponenten-Definitionsmengen. */
    override val werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    override val ausgabeNamen: List<String> = listOf("wert"),
    /** Legacy-Speicher eines tatsächlichen gemeinsamen Definitionsraums. */
    override val effektiverWerteVorrat: MengenAusdruck? = null,
    /** Strukturierte Herkunft einer Restriktion mit optionalen Ergänzungszweigen. */
    override val bereichsanpassung: MethodenBereichsanpassung? = null,
) : SymbolischMathematischeMethode {
    init {
        require(parameter.map { it.name }.distinct().size == parameter.size) {
            "Methodenparameter müssen eindeutige Namen haben."
        }
        require(werteVorräte.keys.all { key -> parameter.any { it.name == key } }) {
            "Definitionsmengen dürfen nur für vorhandene Methodenparameter definiert werden."
        }
        require(ausgabeNamen.distinct().size == ausgabeNamen.size) {
            "Öffentliche Methodenausgaben benötigen eindeutige Namen."
        }
        when (ausgabeNamen.size) {
            0 -> {
                require(vorschrift is Tupel && vorschrift.elemente.isEmpty()) {
                    "Eine ergebnislose Methode verwendet intern das leere Ergebnistupel ()."
                }
                require(zielMenge is Tupelraum && zielMenge.komponenten.isEmpty()) {
                    "Eine ergebnislose Methode verwendet den Zielraum {()} ."
                }
            }
            1 -> {
                if (vorschrift is Tupel) require(vorschrift.elemente.size == 1) {
                    "Eine einwertige kanonisch gepackte Methode benötigt genau eine Tupelkomponente."
                }
                if (zielMenge is Tupelraum) require(zielMenge.komponenten.size == 1) {
                    "Eine einwertige kanonisch gepackte Methode benötigt genau eine Zielkomponente."
                }
            }
            else -> {
                require(vorschrift is Tupel && vorschrift.elemente.size == ausgabeNamen.size) {
                    "Mehrere öffentliche Ausgaben müssen als geordnetes Ergebnistupel gespeichert werden."
                }
                require(zielMenge is Tupelraum && zielMenge.komponenten.size == ausgabeNamen.size) {
                    "Mehrere öffentliche Ausgaben benötigen einen synchronen Produktzielraum."
                }
            }
        }
    }

    /** Kanonische symbolische Ergebnistupel-Sicht, unabhängig von historischen Einzelwertspeichern. */
    val ergebnisTupel: Tupel
        get() = when (ausgabeNamen.size) {
            0 -> vorschrift as Tupel
            1 -> if (vorschrift is Tupel) vorschrift else Tupel(listOf(vorschrift))
            else -> vorschrift as Tupel
        }

    private val ergebnisZielMengen: List<MengenAusdruck>
        get() = when (ausgabeNamen.size) {
            0 -> emptyList()
            1 -> if (zielMenge is Tupelraum) zielMenge.komponenten else listOf(zielMenge)
            else -> (zielMenge as Tupelraum).komponenten
        }

    /** Kanonischer mathematischer Zielraum, auch bei null oder einer Ausgabe. */
    val zielRaum: Tupelraum
        get() = Tupelraum(ergebnisZielMengen)

    override val mathematischeSignatur: MathematischeMethodenSignatur
        get() = MathematischeMethodenSignatur(
            argumente = parameter.mapIndexed { index, parameter ->
                MathematischeArgumentKomponente(
                    id = "argument-$index",
                    name = parameter.name,
                    position = index,
                    parameter = parameter,
                    definitionsMenge = werteVorräte[parameter.name]
                        ?: error("Für das Methodenargument '${parameter.name}' konnte keine Definitionsmenge ermittelt werden."),
                )
            },
            ergebnisse = ausgabeNamen.mapIndexed { index, ausgabe ->
                MathematischeErgebnisKomponente(
                    id = "ergebnis-$index",
                    name = ausgabe,
                    position = index,
                    zielMenge = ergebnisZielMengen[index],
                )
            },
            effektiverDefinitionsRaum = normalisiereLegacyDefinitionsRaum(
                effektiverWerteVorrat,
                parameter.size,
            ),
        )

    /**
     * Ausschließlich für Lademigrationen und historische Testdaten. Die gelesenen
     * Einzelausgaben werden sofort zu einer konsistenten Tupelprojektion normalisiert.
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
        vorschrift = kanonischeVorschrift(ausgaben),
        zielMenge = kanonischeZielMenge(name, ausgaben.keys.toList(), zielMengen),
        werteVorräte = werteVorräte,
        ausgabeNamen = ausgaben.keys.toList(),
    )

    override fun vorschriftFür(ausgabe: String): MathematischesObjekt {
        val index = ausgabeNamen.indexOf(ausgabe)
        require(index >= 0) { "Die Methode '$name' besitzt keine öffentliche Ausgabe '$ausgabe'." }
        return ergebnisTupel.elemente[index]
    }

    override fun zuLatex(): String = zuFallunterscheidungsLatex()

    /** Gemeinsame große Darstellung einer Methode mit kanonischem Raumvertrag und Termzeile. */
    fun zuFallunterscheidungsLatex(): String {
        val signatur = runCatching { mathematischeSignatur }.getOrNull()
        val argumente = parameter.joinToString(",") { it.zuLatex() }
        val argumentTupel = when (parameter.size) {
            0 -> "()"
            1 -> "\\left($argumente\\right)"
            else -> "\\left($argumente\\right)"
        }
        val bildObjekt = if (ausgabeNamen.size == 1) ergebnisTupel.elemente.single() else ergebnisTupel
        val bild = when (bildObjekt) {
            is AbleitungsMethodenAusdruck -> {
                val aufrufArgumente = parameter.joinToString(",") { it.zuLatex() }
                "${bildObjekt.zuLatex()}\\left($aufrufArgumente\\right)"
            }
            else -> bildObjekt.zuLatex()
        }
        return "$name:\\begin{cases}" +
            "${signatur?.definitionsRaum?.zuLatex() ?: "?"} \\longrightarrow " +
            "${signatur?.zielRaum?.zuLatex() ?: zielRaum.zuLatex()}\\\\" +
            "$argumentTupel \\mapsto $bild\\end{cases}"
    }

    override fun zielMengeFür(ausgabe: String): MengenAusdruck {
        val index = ausgabeNamen.indexOf(ausgabe)
        require(index >= 0) { "Die Methode '$name' besitzt keine öffentliche Ausgabe '$ausgabe'." }
        val ziel = ergebnisZielMengen[index]
        check(ziel !is FehlendeObermenge) {
            "Für die öffentliche Ausgabe '$ausgabe' der Methode '$name' fehlt die Zielmenge."
        }
        return ziel
    }

    override fun zielMengeFür(ausgabe: String, bindungen: Map<String, MathematischesObjekt>): MengenAusdruck =
        ersetze(zielMengeFür(ausgabe), bindungen) as MengenAusdruck

    override val einzigeZielMenge: MengenAusdruck
        get() = zielMengeFür(einzigeAusgabe().first)

    override val grundMenge: MengenAusdruck
        get() = grundMengeFürMengenAusgabe()

    override fun grundMengeFürMengenAusgabe(): MengenAusdruck {
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

    override fun binde(bindungen: Map<String, MathematischesObjekt>): GebundeneMethode =
        GebundeneMethode(this, bindungen.filterKeys { key -> parameter.any { it.name == key } })

    /** Legacy-Direktprojektion einer mathematischen Anwendung. */
    override fun wendeAn(argumente: List<MathematischesObjekt>): MathematischesObjekt {
        require(argumente.size == parameter.size) {
            "Die Methode '$name' erwartet ${parameter.size} Argumente, erhielt aber ${argumente.size}."
        }
        return wendeMathematischAn(parameter.map(MethodenParameter::name).zip(argumente).toMap())
    }

    /** Kanonische mathematische Anwendung mit Tupelargument und Tupelergebnis. */
    fun wendeMathematischAlsTupelAn(argumente: Tupel): Tupel {
        require(argumente.elemente.size == parameter.size) {
            "Die Methode '$name' erwartet ein ${parameter.size}-Tupel, erhielt aber ${argumente.elemente.size} Komponenten."
        }
        val bindungen = parameter.map(MethodenParameter::name).zip(argumente.elemente).toMap()
        val ausgewertet = ergebnisTupel.elemente.map { vereinfacheObjekt(ersetze(it, bindungen)) }
        return Tupel(ausgewertet)
    }

    /**
     * Namensbasierte Legacy-Projektion. Der Kern wertet zuerst kanonisch als Tupel aus;
     * genau eine Ergebniskomponente wird für Bestandsaufrufer anschließend ausgepackt.
     */
    override fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
        val fehlend = parameter.map(MethodenParameter::name).filterNot(argumente::containsKey)
        require(fehlend.isEmpty()) {
            "Für die Methode '$name' fehlen die Argumente ${fehlend.joinToString()}."
        }
        val tupel = Tupel(parameter.map { argumente.getValue(it.name) })
        val ergebnis = wendeMathematischAlsTupelAn(tupel)
        return if (ergebnis.elemente.size == 1) ergebnis.elemente.single() else ergebnis
    }

    override fun einzigeAusgabe(): Pair<String, MathematischesObjekt> {
        require(ausgabeNamen.size == 1) { "Die Methode '$name' muss genau eine öffentliche Ausgabe besitzen." }
        return ausgabeNamen.single() to vorschriftFür(ausgabeNamen.single())
    }

    override fun prüfeAlsIterationsMethode(erwartetMengenwert: Boolean): Pair<String, MathematischesObjekt> {
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

private fun normalisiereLegacyDefinitionsRaum(
    raum: MengenAusdruck?,
    stelligkeit: Int,
): MengenAusdruck? = when {
    raum == null -> null
    stelligkeit == 0 && raum == LeereMenge -> Tupelraum(emptyList())
    stelligkeit == 1 && raum !is Tupelraum -> Tupelraum(listOf(raum))
    else -> raum
}

private fun kanonischeVorschrift(ausgaben: Map<String, MathematischesObjekt>): MathematischesObjekt =
    Tupel(ausgaben.values.toList())

private fun kanonischeZielMenge(
    name: String,
    ausgabeNamen: List<String>,
    zielMengen: Map<String, MengenAusdruck>,
): MengenAusdruck {
    require(zielMengen.keys.all(ausgabeNamen::contains)) {
        "Zielmengen dürfen nur für vorhandene Ausgaben definiert werden."
    }
    val ziele = ausgabeNamen.map { ausgabe ->
        zielMengen[ausgabe] ?: FehlendeObermenge("methode.$name.$ausgabe")
    }
    return Tupelraum(ziele)
}

/** Erzeugt eine [höhe] mal [breite]-Matrix aus einer Zahlmethode `f(zeile, spalte)`. */
fun matrixAusMethode(methode: Methode, höhe: Int, breite: Int): Matrix {
    require(höhe > 0) { "Die Matrixhöhe muss positiv sein." }
    require(breite > 0) { "Die Matrixbreite muss positiv sein." }
    val mathematisch = methode.alsMathematischeMethode("Matrixerzeugung")
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

/** Erzeugt das Tupel `(f(1), ..., f(n))` aus einer einstelligen Methode. */
fun tupelAusMethode(methode: Methode, dimension: Int): Tupel {
    require(dimension > 0) { "Die Tupeldimension muss positiv sein." }
    val mathematisch = methode.alsMathematischeMethode("Tupelerzeugung")
    require(mathematisch.parameter.size == 1) { "Die Tupelmethode muss genau einen Indexparameter besitzen." }
    return Tupel(
        List(dimension) { index ->
            mathematisch.wendeAn(listOf(RationaleZahl.von((index + 1).toLong())))
        },
    )
}

/** Bild einer Menge unter einer einwertigen mathematischen Methode: f[M]. */
data class Abbild(val menge: MengenAusdruck, val methode: Methode) : MengenAusdruck {
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

/** Ergebnis einer rein typseitigen, domänenneutralen Kompositionsprüfung. */
data class MethodenTypKompositionsPrüfung(val typPrüfung: TypPrüfung) {
    val kompatibel: Boolean
        get() = typPrüfung == TypPrüfung.Kompatibel
}

/** Allgemeine Methodenkomposition prüft ausschließlich Tupel-Ausgabe gegen Tupel-Eingabe. */
fun prüfeMethodenTypKomposition(außen: Methode, innen: Methode): MethodenTypKompositionsPrüfung {
    val außenSignatur = außen.methodenSignatur()
    val innenSignatur = innen.methodenSignatur()
    return MethodenTypKompositionsPrüfung(
        MathematischeTypen.typSystem.prüfe(innenSignatur.ergebnisTyp, außenSignatur.argumentTyp),
    )
}

/** Strukturierte mathematische Kompositionsdiagnose. */
data class MathematischeKompositionsPrüfung(
    val bildPrüfung: AussageErgebnis,
    val zielraumPrüfung: AussageErgebnis,
    val kompatibilität: AussageErgebnis,
    val bedingungen: Set<Aussage>,
)

/**
 * Prüft für g ∘ f zuerst die tatsächliche Bildmenge f[A], soweit sie bestimmbar ist.
 * Ist sie unbekannt, genügt konservativ die stärkere Bedingung Ziel(f) ⊆ Definitionsmenge(g).
 */
fun prüfeMathematischeKomposition(
    außen: Methode,
    innen: Methode,
    kontext: RechenKontext = RechenKontext(),
): MathematischeKompositionsPrüfung {
    val außenMathematisch = außen.alsMathematischeMethode("mathematische Kompositionsprüfung")
    val innenMathematisch = innen.alsMathematischeMethode("mathematische Kompositionsprüfung")
    val (x, _, _) = außenMathematisch.einwertigeZahlMethode()
    val (t, ausgabeInnen, _) = innenMathematisch.einwertigeZahlMethode()
    val innenDefinitionsmenge = innenMathematisch.werteVorräte[t.name]
        ?: error("Die innere Methode benötigt eine Komponenten-Definitionsmenge.")
    val außenDefinitionsmenge = außenMathematisch.werteVorräte[x.name]
        ?: error("Die äußere Methode benötigt eine Komponenten-Definitionsmenge.")
    val zielInnen = innenMathematisch.zielMengeFür(ausgabeInnen)
    val bildInnen = bildeAb(innenDefinitionsmenge, innenMathematisch)
    val bildPrüfung = prüfeTeilmenge(bildInnen, außenDefinitionsmenge, kontext)
    val zielraumPrüfung = prüfeTeilmenge(zielInnen, außenDefinitionsmenge, kontext)
    val kompatibilität = when {
        bildPrüfung.wahrheitswert == Wahrheitswert.Wahr -> bildPrüfung
        bildPrüfung.wahrheitswert == Wahrheitswert.Lüge -> bildPrüfung
        zielraumPrüfung.wahrheitswert == Wahrheitswert.Wahr -> zielraumPrüfung
        else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
    }
    val bedingungen = if (kompatibilität.wahrheitswert == null) {
        setOf(TeilmengenBeziehung(Abbild(innenDefinitionsmenge, innenMathematisch), außenDefinitionsmenge))
    } else emptySet()
    return MathematischeKompositionsPrüfung(
        bildPrüfung = bildPrüfung,
        zielraumPrüfung = zielraumPrüfung,
        kompatibilität = kompatibilität,
        bedingungen = bedingungen,
    )
}

fun komponiere(außen: Methode, innen: Methode): Methode {
    val außenMathematisch = außen.alsMathematischeMethode("symbolische Komposition")
    val innenMathematisch = innen.alsMathematischeMethode("symbolische Komposition")
    val typPrüfung = prüfeMethodenTypKomposition(außenMathematisch, innenMathematisch).typPrüfung
    require(typPrüfung !is TypPrüfung.Inkompatibel) {
        "Die neutralen Tupelsignaturen der Methoden sind nicht kompatibel: ${typPrüfung.grund}"
    }
    val mengenPrüfung = prüfeMathematischeKomposition(außenMathematisch, innenMathematisch)
    require(mengenPrüfung.kompatibilität.wahrheitswert == Wahrheitswert.Wahr) {
        when (mengenPrüfung.kompatibilität.wahrheitswert) {
            Wahrheitswert.Lüge -> "Die Bildmenge der inneren Methode liegt nicht im Definitionsbereich der äußeren Methode."
            else -> "Die Totalität der Komposition ist nicht nachweisbar; erforderlich ist f[A] ⊆ C."
        }
    }
    val (x, ausgabeAußen, termAußen) = außenMathematisch.einwertigeZahlMethode()
    val (t, _, termInnen) = innenMathematisch.einwertigeZahlMethode()
    val wertevorrat = innenMathematisch.werteVorräte[t.name]
        ?: error("Die innere Methode benötigt eine Definitionsmenge.")
    return Methode(
        name = "${außen.name}\\circ${innen.name}",
        parameter = listOf(t),
        vorschrift = ersetze(termAußen, mapOf(x.name to termInnen)),
        zielMenge = außenMathematisch.zielMengeFür(ausgabeAußen),
        werteVorräte = mapOf(t.name to wertevorrat),
        effektiverWerteVorrat = innenMathematisch.effektiverWerteVorrat,
    )
}

fun iteriere(methode: Methode, exponent: Int): Methode {
    val mathematisch = methode.alsMathematischeMethode("symbolische Iteration")
    require(exponent >= 0) { "Der Iterationsexponent muss nichtnegativ sein." }
    val (x, _, _) = mathematisch.einwertigeZahlMethode()
    val wertevorrat = mathematisch.werteVorräte[x.name] ?: error("Die Methode benötigt eine Definitionsmenge.")
    val selbstKompatibel = prüfeMathematischeKomposition(mathematisch, mathematisch).kompatibilität
    require(selbstKompatibel.wahrheitswert == Wahrheitswert.Wahr) {
        "Iteration benötigt den Nachweis f[D] ⊆ D."
    }
    var ergebnis = Methode(
        name = "id",
        parameter = listOf(x),
        vorschrift = x,
        zielMenge = wertevorrat,
        werteVorräte = mapOf(x.name to wertevorrat),
    )
    repeat(exponent) {
        ergebnis = komponiere(mathematisch, ergebnis).alsMathematischeMethode("symbolische Iteration")
    }
    return ergebnis.copy(name = "${methode.name}^{${exponent}}")
}

fun differenziereMethode(methode: Methode): Methode {
    val mathematisch = methode.alsMathematischeMethode("symbolische Differentiation")
    require(mathematisch.parameter.size == 1 && mathematisch.ausgabeNamen.size == 1) {
        "Die Methode muss genau einen Parameter und eine Ausgabe besitzen."
    }
    val x = mathematisch.parameter.single() as? Variable ?: error("Die Methode muss einen Zahlenparameter besitzen.")
    val (_, wert) = mathematisch.einzigeAusgabe()
    val wertevorrat = mathematisch.werteVorräte[x.name] ?: error("Die Methode benötigt eine Definitionsmenge.")
    require(wertevorrat.hatDifferentialBegriff()) { "Die Definitionsmenge definiert keinen Differentialbegriff." }
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
    val wertevorrat = mathematisch.werteVorräte[x.name] ?: error("Die Methode benötigt eine Definitionsmenge.")
    require(wertevorrat.hatIntegralBegriff()) { "Die Definitionsmenge definiert keinen Integralbegriff." }
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
    return EndlicheMenge(menge.elemente.map { element ->
        mathematisch.wendeAn(listOf(element))
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
    is MathematischeMethode -> {
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
    is MathematischeMethode -> {
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
