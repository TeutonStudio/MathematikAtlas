package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val ZAHLENRECHNER_ART = "mathematik.zahlenrechner"
const val ZAHLENRECHNER_OPERATOR = "operator"
const val ZAHLENRECHNER_KOMPLEX_EINGABE = "komplexEingabe"
const val ZAHLENRECHNER_KOMPLEX_SEPARIERT = "separiert"
const val ZAHLENRECHNER_KOMPLEX_TUPEL = "tupel"
const val ZAHLENRECHNER_GRADWINKEL = "gradWinkel"
const val ZAHLENRECHNER_GRAD_AUSWERTEN = "gradAlsBogenmassAuswerten"
const val ZAHLENRECHNER_VARIABLE = "variable"

private fun zahlenEingang(
    name: String,
    reihenfolge: Int,
    erweiterbar: Boolean = false,
) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.Zahl.id,
    reihenfolge = reihenfolge,
    kannSichErweitern = erweiterbar,
)

private fun spezialEingang(name: String, art: AnschlussArtId, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
)

private fun zahlenAusgang() = AnschlussDaten(
    name = "wert",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = MathematikAnschlussArten.Zahl.id,
)

private fun standardAnschluesse(operator: UniversellerZahlenOperator): List<AnschlussDaten> = when (operator) {
    UniversellerZahlenOperator.ADDITION,
    UniversellerZahlenOperator.MULTIPLIKATION,
    UniversellerZahlenOperator.MINIMUM,
    UniversellerZahlenOperator.MAXIMUM,
    -> listOf(zahlenEingang("a", 0, true), zahlenEingang("b", 1, true), zahlenAusgang())

    UniversellerZahlenOperator.ITERIERTE_SUMME,
    UniversellerZahlenOperator.ITERIERTES_PRODUKT,
    -> listOf(
        spezialEingang("methode", MathematikAnschlussArten.ZahlMethode.id, 0),
        spezialEingang("indexmenge", MathematikAnschlussArten.Menge.id, 1),
        zahlenAusgang(),
    )

    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
    UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH,
    -> listOf(
        zahlenEingang("a", 0),
        zahlenEingang("b", 1),
        spezialEingang("tupel", MathematikAnschlussArten.Tupel.id, 2),
        zahlenAusgang(),
    )

    UniversellerZahlenOperator.DIVISION -> listOf(
        zahlenEingang("a", 0),
        zahlenEingang("b", 1),
        zahlenEingang("c", 2),
        zahlenAusgang(),
    )

    UniversellerZahlenOperator.SUBTRAKTION,
    UniversellerZahlenOperator.POTENZ,
    UniversellerZahlenOperator.WURZEL,
    UniversellerZahlenOperator.LOGARITHMUS,
    UniversellerZahlenOperator.MODULO,
    -> listOf(zahlenEingang("a", 0), zahlenEingang("b", 1), zahlenAusgang())

    else -> listOf(zahlenEingang("a", 0), zahlenAusgang())
}

/**
 * Der Erstellen-Dialog darf mehrere vorkonfigurierte Varianten zeigen. Alle
 * Varianten erzeugen dieselbe stabile Knotenart und unterscheiden sich nur im
 * persistierten Operatorzustand.
 */
object ZahlenRechnerKnotenVorlagen {
    val alle: List<KnotenVorlage> = UniversellerZahlenOperator.entries.map { operator ->
        KnotenVorlage(
            art = ZAHLENRECHNER_ART,
            name = operator.titel,
            kategorie = "Rechnen: Zahlenrechner",
            beschreibung = "Universeller Zahlenrechner im Zustand ${operator.titel}; Operator und Darstellungsoptionen bleiben im Inspector änderbar.",
            standardGröße = GraphGröße(270f, 145f),
            anschlüsse = standardAnschluesse(operator),
            standardParameter = buildMap {
                put(ZAHLENRECHNER_OPERATOR, operator.stabileId)
                put(ZAHLENRECHNER_VARIABLE, "x")
                put(ZAHLENRECHNER_KOMPLEX_EINGABE, ZAHLENRECHNER_KOMPLEX_SEPARIERT)
                put(ZAHLENRECHNER_GRADWINKEL, "false")
                put(ZAHLENRECHNER_GRAD_AUSWERTEN, "true")
                if (operator in setOf(
                        UniversellerZahlenOperator.ADDITION,
                        UniversellerZahlenOperator.MULTIPLIKATION,
                        UniversellerZahlenOperator.MINIMUM,
                        UniversellerZahlenOperator.MAXIMUM,
                    )
                ) {
                    put("festeEingänge", "2")
                    put("operatorAnzeige", "wert")
                }
            },
        )
    }

    val standard: KnotenVorlage = alle.first { it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.ADDITION.stabileId }
}

/** Nur diese historischen Zahl-zu-Zahl-Knoten verschwinden aus dem Katalog. */
val historischeZahlenRechnerArten: Set<String> = setOf(
    "mathematik.addition",
    "mathematik.subtraktion",
    "mathematik.multiplikation",
    "mathematik.division",
    "mathematik.kehrwert",
    "mathematik.potenz",
    "mathematik.wurzel",
    "mathematik.quadrat",
    "mathematik.kubik",
    "mathematik.quadratwurzel",
    "mathematik.kubikwurzel",
    "mathematik.logarithmus",
    "mathematik.logarithmusBasis2",
    "mathematik.lb",
    "mathematik.ln",
    "mathematik.log10",
    "mathematik.iterierteSumme",
    "mathematik.iteriertesProdukt",
    "mathematik.integrieren",
    "mathematik.ableiten",
    "mathematik.extremwert",
    "mathematik.norm",
    "mathematik.abrundung",
    "mathematik.aufrundung",
    "mathematik.rundung",
    "mathematik.konjugierte",
    "mathematik.realteil",
    "mathematik.imaginärteil",
    "mathematik.imaginaerteil",
    "mathematik.winkel",
    "mathematik.komplexerRadius",
    "mathematik.komplexAusTupel",
    "mathematik.komplexAusPolar",
    "mathematik.komplexAusKartesisch",
    "mathematik.modulo",
)

private val alteStandardNamen = setOf(
    "Addition", "Subtraktion", "Multiplikation", "Division", "Kehrwert", "Potenz", "Wurzel",
    "Quadrat", "Kubik", "Quadratwurzel", "Kubikwurzel", "Logarithmus", "Iterierte Summe",
    "Iteriertes Produkt", "Integrieren", "Differentieren", "Maximum", "Minimum", "Konjugierte",
    "Realteil", "Imaginärteil", "Winkel einer Zahl", "Radius einer Zahl", "Komplexe Zahl aus Tupel",
)

fun historischerZahlenOperator(knoten: KnotenDaten): UniversellerZahlenOperator? = when (knoten.art) {
    "mathematik.addition" -> UniversellerZahlenOperator.ADDITION
    "mathematik.subtraktion" -> UniversellerZahlenOperator.SUBTRAKTION
    "mathematik.multiplikation" -> UniversellerZahlenOperator.MULTIPLIKATION
    "mathematik.division" -> UniversellerZahlenOperator.DIVISION
    "mathematik.kehrwert" -> UniversellerZahlenOperator.KEHRWERT
    "mathematik.potenz" -> UniversellerZahlenOperator.POTENZ
    "mathematik.quadrat" -> UniversellerZahlenOperator.QUADRAT
    "mathematik.kubik" -> UniversellerZahlenOperator.KUBIK
    "mathematik.wurzel" -> UniversellerZahlenOperator.WURZEL
    "mathematik.quadratwurzel" -> UniversellerZahlenOperator.QUADRATWURZEL
    "mathematik.kubikwurzel" -> UniversellerZahlenOperator.KUBIKWURZEL
    "mathematik.logarithmus" -> UniversellerZahlenOperator.LOGARITHMUS
    "mathematik.logarithmusBasis2", "mathematik.lb" -> UniversellerZahlenOperator.LOGARITHMUS_BASIS_2
    "mathematik.ln" -> UniversellerZahlenOperator.NATUERLICHER_LOGARITHMUS
    "mathematik.log10" -> UniversellerZahlenOperator.LOGARITHMUS_BASIS_10
    "mathematik.iterierteSumme" -> UniversellerZahlenOperator.ITERIERTE_SUMME
    "mathematik.iteriertesProdukt" -> UniversellerZahlenOperator.ITERIERTES_PRODUKT
    "mathematik.integrieren" -> UniversellerZahlenOperator.INTEGRAL
    "mathematik.ableiten" -> UniversellerZahlenOperator.DIFFERENTIAL
    "mathematik.extremwert" -> if (knoten.parameter["modus"] == "minimum") {
        UniversellerZahlenOperator.MINIMUM
    } else {
        UniversellerZahlenOperator.MAXIMUM
    }
    "mathematik.norm" -> UniversellerZahlenOperator.NORM
    "mathematik.abrundung" -> UniversellerZahlenOperator.ABRUNDUNG
    "mathematik.aufrundung" -> UniversellerZahlenOperator.AUFRUNDUNG
    "mathematik.rundung" -> UniversellerZahlenOperator.RUNDUNG
    "mathematik.konjugierte" -> UniversellerZahlenOperator.KONJUGIERTE
    "mathematik.realteil" -> UniversellerZahlenOperator.REALTEIL
    "mathematik.imaginärteil", "mathematik.imaginaerteil" -> UniversellerZahlenOperator.IMAGINAERTEIL
    "mathematik.winkel" -> UniversellerZahlenOperator.KOMPLEXER_WINKEL
    "mathematik.komplexerRadius" -> UniversellerZahlenOperator.KOMPLEXER_RADIUS
    "mathematik.komplexAusTupel" -> if (knoten.parameter["modus"] == "polar") {
        UniversellerZahlenOperator.KOMPLEX_AUS_POLAR
    } else {
        UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH
    }
    "mathematik.komplexAusPolar" -> UniversellerZahlenOperator.KOMPLEX_AUS_POLAR
    "mathematik.komplexAusKartesisch" -> UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH
    "mathematik.modulo" -> UniversellerZahlenOperator.MODULO
    else -> null
}

/**
 * Idempotente Lade-Migration. Knoten- und Anschluss-IDs bleiben erhalten, damit
 * bestehende Edges exakt auf denselben semantischen Argumenten landen.
 */
fun KartenDaten.migriereUniversellenZahlenRechner(): KartenDaten = copy(
    knoten = knoten.map { alt ->
        if (alt.art == ZAHLENRECHNER_ART) {
            alt.copy(parameter = standardParameter(UniversellerZahlenOperator.vonId(alt.parameter[ZAHLENRECHNER_OPERATOR])) + alt.parameter)
        } else {
            val operator = historischerZahlenOperator(alt) ?: return@map alt
            val standard = standardAnschluesse(operator)
            val alteEingaenge = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
            val alterAusgang = alt.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
            val umbenannt = alteEingaenge.mapIndexed { index, anschluss ->
                val zielName = kanonischerEingangsName(operator, anschluss.name, index)
                val zielArt = standard.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == zielName }?.art
                    ?: anschluss.art
                anschluss.copy(
                    name = zielName,
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = zielArt,
                    reihenfolge = index,
                    kannSichErweitern = operator in setOf(
                        UniversellerZahlenOperator.ADDITION,
                        UniversellerZahlenOperator.MULTIPLIKATION,
                        UniversellerZahlenOperator.MINIMUM,
                        UniversellerZahlenOperator.MAXIMUM,
                    ),
                )
            }
            val vorhanden = umbenannt.mapTo(mutableSetOf()) { it.name }
            val ergaenzt = standard.filter { it.richtung == AnschlussRichtung.Eingang && it.name !in vorhanden }
            val ausgang = (alterAusgang ?: zahlenAusgang()).copy(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
                reihenfolge = 0,
                kannSichErweitern = false,
                dynamischErzeugt = false,
            )
            alt.copy(
                art = ZAHLENRECHNER_ART,
                name = if (alt.name in alteStandardNamen) operator.titel else alt.name,
                anschlüsse = umbenannt + ergaenzt + ausgang,
                parameter = standardParameter(operator) + alt.parameter + mapOf(
                    ZAHLENRECHNER_OPERATOR to operator.stabileId,
                    ZAHLENRECHNER_KOMPLEX_EINGABE to if (alt.art == "mathematik.komplexAusTupel") {
                        ZAHLENRECHNER_KOMPLEX_TUPEL
                    } else {
                        alt.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE] ?: ZAHLENRECHNER_KOMPLEX_SEPARIERT
                    },
                ),
            )
        }
    },
)

private fun standardParameter(operator: UniversellerZahlenOperator): Map<String, String> = mapOf(
    ZAHLENRECHNER_OPERATOR to operator.stabileId,
    ZAHLENRECHNER_VARIABLE to "x",
    ZAHLENRECHNER_KOMPLEX_EINGABE to ZAHLENRECHNER_KOMPLEX_SEPARIERT,
    ZAHLENRECHNER_GRADWINKEL to "false",
    ZAHLENRECHNER_GRAD_AUSWERTEN to "true",
)

private fun kanonischerEingangsName(
    operator: UniversellerZahlenOperator,
    alterName: String,
    index: Int,
): String = when {
    alterName == "methode" || alterName == "indexmenge" || alterName == "tupel" -> alterName
    alterName in setOf("dividend", "zaehler", "basis", "radikand", "zahl", "term", "argument", "links", "minuend") -> "a"
    alterName in setOf("divisor", "nenner", "exponent", "rechts", "subtrahend", "grad") -> "b"
    alterName == "fallsNennerNull" -> "c"
    operator in setOf(
        UniversellerZahlenOperator.ADDITION,
        UniversellerZahlenOperator.MULTIPLIKATION,
        UniversellerZahlenOperator.MINIMUM,
        UniversellerZahlenOperator.MAXIMUM,
    ) -> ('a'.code + index).toChar().toString()
    else -> ('a'.code + index).toChar().toString()
}

internal fun MathematikAuswerterRegister.registriereUniversellenZahlenRechner() {
    registriere(ZAHLENRECHNER_ART) { kontext ->
        val operator = UniversellerZahlenOperator.vonId(kontext.knoten.parameter[ZAHLENRECHNER_OPERATOR])
        val ausgabe = universellAuswerten(kontext, operator)
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
        val reelle = reelleVariablen(kontext.eingänge.values)
        val definition = ZahlenRechnerDefinition(operator, ausgabe.bereich)
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = ausgabe.objekt,
                    annahmen = annahmen,
                    werteVorrat = ausgabe.bereich.alsMenge(),
                    reelleVariablen = reelle,
                    variablenQuellen = kontext.eingänge.values.flatMap { it.variablenQuellen }.distinctBy { it.identität },
                    latexDarstellung = ausgabe.latex,
                ),
            ),
            schritte = ausgabe.schritte,
            eingänge = kontext.eingänge,
            warnungen = definition.regeln,
        )
    }
}

private data class UniverselleZahlenAusgabe(
    val objekt: ZahlAusdruck,
    val bereich: ZahlenRechnerBereich,
    val latex: String? = null,
    val schritte: List<UmformungsSchritt> = emptyList(),
)

private fun universellAuswerten(
    k: KnotenAuswertungsKontext,
    operator: UniversellerZahlenOperator,
): UniverselleZahlenAusgabe {
    fun wert(name: String): BedingterWert = k.eingänge[name] ?: error("Zahleingang '$name' fehlt.")
    fun zahl(name: String): ZahlAusdruck = wert(name).objekt as? ZahlAusdruck ?: error("Eingang '$name' enthält keine Zahl.")
    fun bereich(wert: BedingterWert): ZahlenRechnerBereich = inferiereZahlenRechnerBereich(
        wert.objekt as? ZahlAusdruck ?: error("Ein Zahlenrechner-Eingang enthält keine Zahl."),
        wert.werteVorrat,
    )
    val zahlenWerte = k.knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang && it.art == MathematikAnschlussArten.Zahl.id }
        .sortedBy { it.reihenfolge }
        .mapNotNull { anschluss -> k.eingänge[anschluss.name] }
    val gemeinsam = zahlenWerte.takeIf { it.isNotEmpty() }
        ?.map(::bereich)
        ?.let(::gemeinsamerZahlenRechnerBereich)
        ?: ZahlenRechnerBereich.UNBEKANNT
    val grad = k.knoten.parameter[ZAHLENRECHNER_GRADWINKEL].toBoolean()
    val gradAuswerten = k.knoten.parameter[ZAHLENRECHNER_GRAD_AUSWERTEN] != "false"
    val variable = Variable(k.knoten.parameter[ZAHLENRECHNER_VARIABLE]?.ifBlank { "x" } ?: "x")

    fun unär(erzeuge: (ZahlAusdruck) -> ZahlAusdruck, ziel: ZahlenRechnerBereich = gemeinsam) =
        UniverselleZahlenAusgabe(erzeuge(zahl("a")), ziel)

    return when (operator) {
        UniversellerZahlenOperator.ADDITION -> {
            require(zahlenWerte.size >= 2) { "Addition benötigt mindestens zwei Summanden." }
            UniverselleZahlenAusgabe(addition(zahlenWerte.map { it.objekt as ZahlAusdruck }), gemeinsam)
        }
        UniversellerZahlenOperator.SUBTRAKTION -> UniverselleZahlenAusgabe(subtraktion(zahl("a"), zahl("b")), gemeinsam)
        UniversellerZahlenOperator.MULTIPLIKATION -> {
            require(zahlenWerte.size >= 2) { "Multiplikation benötigt mindestens zwei Faktoren." }
            UniverselleZahlenAusgabe(multiplikation(zahlenWerte.map { it.objekt as ZahlAusdruck }), gemeinsam)
        }
        UniversellerZahlenOperator.DIVISION -> {
            val zaehler = zahl("a")
            val nenner = zahl("b")
            if (nenner == RationaleZahl.Null) {
                val ersatz = k.eingänge["c"]?.objekt as? ZahlAusdruck
                    ?: error("Division durch null ist ohne verbundenen Ersatzwert nicht definiert.")
                return UniverselleZahlenAusgabe(ersatz, inferiereZahlenRechnerBereich(ersatz, k.eingänge["c"]?.werteVorrat))
            }
            val ziel = if (gemeinsam.rang < ZahlenRechnerBereich.RATIONAL.rang) ZahlenRechnerBereich.RATIONAL else gemeinsam
            UniverselleZahlenAusgabe(Division(zaehler, nenner), ziel, intelligenteDivisionLatex(zaehler, nenner))
        }
        UniversellerZahlenOperator.KEHRWERT -> {
            val argument = zahl("a")
            require(argument != RationaleZahl.Null) { "Der Kehrwert von null ist nicht definiert." }
            UniverselleZahlenAusgabe(Division(RationaleZahl.Eins, argument), gemeinsam, intelligenteDivisionLatex(RationaleZahl.Eins, argument))
        }
        UniversellerZahlenOperator.POTENZ -> UniverselleZahlenAusgabe(Potenz(zahl("a"), zahl("b")), gemeinsam)
        UniversellerZahlenOperator.QUADRAT -> unär({ Potenz(it, RationaleZahl.von(2)) })
        UniversellerZahlenOperator.KUBIK -> unär({ Potenz(it, RationaleZahl.von(3)) })
        UniversellerZahlenOperator.WURZEL -> {
            val radikand = zahl("a")
            val gradWert = k.eingänge["b"]?.objekt as? ZahlAusdruck
            if (gradWert == null || gradWert == RationaleZahl.von(2)) {
                UniverselleZahlenAusgabe(wurzel(radikand, k.rechenKontext), wurzelBereich(radikand, gemeinsam))
            } else {
                UniverselleZahlenAusgabe(Potenz(radikand, Division(RationaleZahl.Eins, gradWert)), gemeinsam)
            }
        }
        UniversellerZahlenOperator.QUADRATWURZEL -> UniverselleZahlenAusgabe(wurzel(zahl("a"), k.rechenKontext), wurzelBereich(zahl("a"), gemeinsam))
        UniversellerZahlenOperator.KUBIKWURZEL -> unär({ Potenz(it, RationaleZahl.von(1, 3)) })
        UniversellerZahlenOperator.LOGARITHMUS -> UniverselleZahlenAusgabe(Logarithmus(zahl("a"), zahl("b")), maxBereich(gemeinsam, ZahlenRechnerBereich.REELL))
        UniversellerZahlenOperator.LOGARITHMUS_BASIS_2 -> unär({ Logarithmus(RationaleZahl.von(2), it) }, maxBereich(gemeinsam, ZahlenRechnerBereich.REELL))
        UniversellerZahlenOperator.NATUERLICHER_LOGARITHMUS -> unär(::NatürlicherLogarithmus, maxBereich(gemeinsam, ZahlenRechnerBereich.REELL))
        UniversellerZahlenOperator.LOGARITHMUS_BASIS_10 -> unär({ Logarithmus(RationaleZahl.von(10), it) }, maxBereich(gemeinsam, ZahlenRechnerBereich.REELL))
        UniversellerZahlenOperator.ITERIERTE_SUMME,
        UniversellerZahlenOperator.ITERIERTES_PRODUKT,
        -> {
            val methode = k.eingänge["methode"]?.objekt as? Methode ?: error("Iterationsmethode fehlt.")
            val indexmenge = k.eingänge["indexmenge"]?.objekt as? MengenAusdruck ?: error("Indexmenge fehlt.")
            val objekt = if (operator == UniversellerZahlenOperator.ITERIERTE_SUMME) {
                iterierteSumme(methode, indexmenge)
            } else {
                iteriertesProdukt(methode, indexmenge)
            }
            UniverselleZahlenAusgabe(objekt, gemeinsam)
        }
        UniversellerZahlenOperator.INTEGRAL -> {
            val ergebnis = integrieren(zahl("a"), variable)
            UniverselleZahlenAusgabe(ergebnis.ergebnis, gemeinsam, schritte = ergebnis.schritte)
        }
        UniversellerZahlenOperator.DIFFERENTIAL -> {
            val ergebnis = ableiten(zahl("a"), variable)
            UniverselleZahlenAusgabe(ergebnis.ergebnis, gemeinsam, schritte = ergebnis.schritte)
        }
        UniversellerZahlenOperator.MINIMUM,
        UniversellerZahlenOperator.MAXIMUM,
        -> {
            require(gemeinsam.geordnet) { "${operator.titel} ist auf ${gemeinsam.latex} nicht definiert, da der Bereich nicht geordnet ist." }
            require(zahlenWerte.size >= 2) { "${operator.titel} benötigt mindestens zwei Operanden." }
            val zahlen = zahlenWerte.map { it.objekt as ZahlAusdruck }
            UniverselleZahlenAusgabe(if (operator == UniversellerZahlenOperator.MINIMUM) minimum(zahlen) else maximum(zahlen), gemeinsam)
        }
        UniversellerZahlenOperator.NORM -> {
            val argument = zahl("a")
            UniverselleZahlenAusgabe(if (argument is KomplexeZahl) komplexerBetrag(argument) else Betrag(argument), ZahlenRechnerBereich.REELL)
        }
        UniversellerZahlenOperator.ABRUNDUNG -> unär(::abrunden, ZahlenRechnerBereich.GANZ)
        UniversellerZahlenOperator.AUFRUNDUNG -> unär(::aufrunden, ZahlenRechnerBereich.GANZ)
        UniversellerZahlenOperator.RUNDUNG -> unär(::runden, ZahlenRechnerBereich.GANZ)
        UniversellerZahlenOperator.KONJUGIERTE -> {
            val argument = zahl("a")
            UniverselleZahlenAusgabe(
                when (argument) {
                    is KomplexeZahl -> konjugiere(argument)
                    else -> if (gemeinsam.rang <= ZahlenRechnerBereich.REELL.rang) argument else symbolischerZahlterm("konjugiert-${argument.zuLatex()}", "\\overline{${argument.zuLatex()}}")
                },
                gemeinsam,
            )
        }
        UniversellerZahlenOperator.REALTEIL -> {
            val argument = zahl("a")
            UniverselleZahlenAusgabe((argument as? KomplexeZahl)?.realteil ?: argument, ZahlenRechnerBereich.REELL)
        }
        UniversellerZahlenOperator.IMAGINAERTEIL -> {
            val argument = zahl("a")
            UniverselleZahlenAusgabe((argument as? KomplexeZahl)?.imaginärteil ?: RationaleZahl.Null, ZahlenRechnerBereich.REELL)
        }
        UniversellerZahlenOperator.KOMPLEXER_WINKEL -> {
            val argument = zahl("a")
            val objekt = (argument as? KomplexeZahl)?.let(::Argument)
                ?: symbolischerZahlterm("arg-${argument.zuLatex()}", "\\arg\\left(${argument.zuLatex()}\\right)")
            UniverselleZahlenAusgabe(objekt, ZahlenRechnerBereich.REELL)
        }
        UniversellerZahlenOperator.KOMPLEXER_RADIUS -> {
            val argument = zahl("a")
            UniverselleZahlenAusgabe((argument as? KomplexeZahl)?.let(::komplexerBetrag) ?: Betrag(argument), ZahlenRechnerBereich.REELL)
        }
        UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH,
        UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
        -> {
            val tupelModus = k.knoten.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE] == ZAHLENRECHNER_KOMPLEX_TUPEL
            val (erste, zweite) = if (tupelModus) {
                val tupel = k.eingänge["tupel"]?.objekt as? Tupel ?: error("Ein Tupel aus zwei Zahlen fehlt.")
                require(tupel.elemente.size == 2 && tupel.elemente.all { it is ZahlAusdruck }) { "Das komplexe Tupel muss genau zwei Zahlen enthalten." }
                (tupel.elemente[0] as ZahlAusdruck) to (tupel.elemente[1] as ZahlAusdruck)
            } else {
                zahl("a") to zahl("b")
            }
            val objekt = if (operator == UniversellerZahlenOperator.KOMPLEX_AUS_POLAR) {
                komplexAusPolar(erste, zweite, grad, gradAuswerten)
            } else {
                komplexAusKartesisch(erste, zweite)
            }
            val latex = if (operator == UniversellerZahlenOperator.KOMPLEX_AUS_POLAR && grad) {
                val winkelLatex = gradWinkelLatex(zweite, gradAuswerten)
                "${erste.zuLatex()}\\left(\\cos\\left($winkelLatex\\right)+i\\sin\\left($winkelLatex\\right)\\right)"
            } else null
            UniverselleZahlenAusgabe(objekt, ZahlenRechnerBereich.KOMPLEX, latex)
        }
        UniversellerZahlenOperator.MODULO -> UniverselleZahlenAusgabe(modulo(zahl("a"), zahl("b")), ZahlenRechnerBereich.MODULO)
        UniversellerZahlenOperator.BETRAG -> unär(::Betrag, if (gemeinsam.rang >= ZahlenRechnerBereich.KOMPLEX.rang) ZahlenRechnerBereich.REELL else gemeinsam)
        UniversellerZahlenOperator.EXPONENTIALFUNKTION -> unär(::Exponentialfunktion, maxBereich(gemeinsam, ZahlenRechnerBereich.REELL))
        UniversellerZahlenOperator.SINUS,
        UniversellerZahlenOperator.COSINUS,
        -> {
            val argument = zahl("a")
            val effektiv = if (grad && gradAuswerten) gradZuBogenmass(argument) else argument
            val objekt = if (operator == UniversellerZahlenOperator.SINUS) Sinus(effektiv) else Cosinus(effektiv)
            val latex = if (grad) {
                val name = if (operator == UniversellerZahlenOperator.SINUS) "\\sin" else "\\cos"
                "$name\\left(${gradWinkelLatex(argument, gradAuswerten)}\\right)"
            } else null
            UniverselleZahlenAusgabe(objekt, maxBereich(gemeinsam, ZahlenRechnerBereich.REELL), latex)
        }
        UniversellerZahlenOperator.ARCSINUS -> unär(::ArcSinus, ZahlenRechnerBereich.REELL)
        UniversellerZahlenOperator.ARCCOSINUS -> unär(::ArcCosinus, ZahlenRechnerBereich.REELL)
        UniversellerZahlenOperator.LIMES_HYPERREELL_ZU_REELL -> {
            val argument = zahl("a")
            UniverselleZahlenAusgabe(argument, ZahlenRechnerBereich.REELL, "\\lim\\left(${argument.zuLatex()}\\right)")
        }
    }
}

private fun wurzelBereich(argument: ZahlAusdruck, basis: ZahlenRechnerBereich): ZahlenRechnerBereich =
    if ((argument as? RationaleZahl)?.zähler?.signum() == -1) ZahlenRechnerBereich.KOMPLEX
    else maxBereich(basis, ZahlenRechnerBereich.REELL)

private fun maxBereich(a: ZahlenRechnerBereich, b: ZahlenRechnerBereich): ZahlenRechnerBereich =
    if (a.rang >= b.rang) a else b

private fun ZahlenRechnerBereich.alsMenge(): MengenAusdruck = when (this) {
    ZahlenRechnerBereich.NATUERLICH, ZahlenRechnerBereich.NATUERLICH_MIT_NULL -> NatürlicheZahlen
    ZahlenRechnerBereich.GANZ -> GanzeZahlen
    ZahlenRechnerBereich.RATIONAL -> RationaleZahlen
    ZahlenRechnerBereich.REELL -> ReelleZahlen
    ZahlenRechnerBereich.KOMPLEX -> KomplexeZahlen
    ZahlenRechnerBereich.HYPERREELL -> BenannteMenge("hyperreell", latex)
    ZahlenRechnerBereich.QUATERNION -> BenannteMenge("quaternion", latex)
    ZahlenRechnerBereich.MODULO -> BenannteMenge("modulo", latex)
    ZahlenRechnerBereich.UNBEKANNT -> BenannteMenge("zahlbereich-unbekannt", latex)
}
