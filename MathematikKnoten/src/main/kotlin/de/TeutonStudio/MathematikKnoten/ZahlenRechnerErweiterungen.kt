package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val ZAHLENRECHNER_FORMEL_ID = "zahl.formel"
const val ZAHLENRECHNER_FORMEL_LATEX = "formelLatex"
const val ZAHLENRECHNER_FORMEL_VARIABLEN = "formelVariablen"

/** Ergänzende stabile Operatorzustände des universellen Zahlenrechners. */
enum class ErweiterterZahlenOperator(
    val stabileId: String,
    val titel: String,
    val symbolLatex: String,
) {
    TANGENS("zahl.tan", "Tangens", "\\tan"),
    COTANGENS("zahl.cot", "Cotangens", "\\cot"),
    SEKANS("zahl.sec", "Sekans", "\\sec"),
    KOSEKANS("zahl.csc", "Kosekans", "\\csc"),
    ARCTANGENS("zahl.arctan", "Arcus Tangens", "\\arctan"),
    SINUS_HYPERBOLICUS("zahl.sinh", "Sinus hyperbolicus", "\\sinh"),
    COSINUS_HYPERBOLICUS("zahl.cosh", "Cosinus hyperbolicus", "\\cosh"),
    TANGENS_HYPERBOLICUS("zahl.tanh", "Tangens hyperbolicus", "\\tanh"),
    COTANGENS_HYPERBOLICUS("zahl.coth", "Cotangens hyperbolicus", "\\coth"),
    SEKANS_HYPERBOLICUS("zahl.sech", "Sekans hyperbolicus", "\\operatorname{sech}"),
    KOSEKANS_HYPERBOLICUS("zahl.csch", "Kosekans hyperbolicus", "\\operatorname{csch}"),
    ARITHMETISCHES_MITTEL("zahl.arithmetischesMittel", "Arithmetisches Mittel", "\\operatorname{AM}"),
    GEOMETRISCHES_MITTEL("zahl.geometrischesMittel", "Geometrisches Mittel", "\\operatorname{GM}"),
    STAMMFUNKTION("zahl.stammfunktion", "Stammfunktion", "\\int"),
    POLYNOM("zahl.polynom", "Polynom", "(c_k)_k\\cdot(x^k)_k"),
    ;

    companion object {
        fun vonId(id: String?): ErweiterterZahlenOperator? = entries.firstOrNull { operator ->
            id == operator.stabileId || id.equals(operator.name, ignoreCase = true)
        }
    }

    val hebungsArt: ZahlenOperatorHebungsArt
        get() = when (this) {
            POLYNOM,
            STAMMFUNKTION,
            -> ZahlenOperatorHebungsArt.METHODENSPEZIFISCH
            else -> ZahlenOperatorHebungsArt.PUNKTWEISE
        }
}

fun istZahlenRechnerFormel(knoten: KnotenDaten): Boolean =
    knoten.art == ZAHLENRECHNER_ART && knoten.parameter[ZAHLENRECHNER_OPERATOR] == ZAHLENRECHNER_FORMEL_ID

fun verwendetGradWinkel(operator: ErweiterterZahlenOperator): Boolean = operator in setOf(
    ErweiterterZahlenOperator.TANGENS,
    ErweiterterZahlenOperator.COTANGENS,
    ErweiterterZahlenOperator.SEKANS,
    ErweiterterZahlenOperator.KOSEKANS,
)

fun konfiguriereStandardZahlenRechner(
    knoten: KnotenDaten,
    operator: UniversellerZahlenOperator,
): KnotenDaten {
    val bereinigt = knoten.copy(
        parameter = knoten.parameter - ZAHLENRECHNER_FORMEL_LATEX - ZAHLENRECHNER_FORMEL_VARIABLEN,
    )
    return konfiguriereZahlenRechner(bereinigt, operator = operator)
}

fun konfiguriereErweitertenZahlenRechner(
    knoten: KnotenDaten,
    operator: ErweiterterZahlenOperator,
): KnotenDaten {
    require(knoten.art == ZAHLENRECHNER_ART)
    return when (operator) {
        ErweiterterZahlenOperator.POLYNOM -> konfigurierePolynomZahlenRechner(knoten)
        ErweiterterZahlenOperator.ARITHMETISCHES_MITTEL,
        ErweiterterZahlenOperator.GEOMETRISCHES_MITTEL,
        -> konfiguriereMittelZahlenRechner(knoten, operator)
        ErweiterterZahlenOperator.STAMMFUNKTION -> konfiguriereStammfunktionZahlenRechner(knoten)
        else -> konfiguriereUnaerenErweitertenZahlenRechner(knoten, operator)
    }
}

private fun konfiguriereUnaerenErweitertenZahlenRechner(
    knoten: KnotenDaten,
    operator: ErweiterterZahlenOperator,
): KnotenDaten {
    val bisherigerEingang = knoten.anschlüsse
        .firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == "a" }
        ?: knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang }
    val bisherigerAusgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    val eingang = (bisherigerEingang ?: AnschlussDaten(
        name = "a",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Zahl.id,
    )).copy(
        name = "a",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Objekt.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
        zulässigeArten = setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Methode.id),
    )
    val ausgang = zahlenOderMethodenAusgang(bisherigerAusgang, listOf("a"))
    return knoten.copy(
        name = operator.titel,
        anschlüsse = listOf(eingang, ausgang),
        parameter = (knoten.parameter - ZAHLENRECHNER_FORMEL_LATEX - ZAHLENRECHNER_FORMEL_VARIABLEN) +
            (ZAHLENRECHNER_OPERATOR to operator.stabileId),
    )
}

private fun konfiguriereMittelZahlenRechner(
    knoten: KnotenDaten,
    operator: ErweiterterZahlenOperator,
): KnotenDaten {
    val bisherige = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
    val anzahl = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
    val eingänge = List(anzahl) { index ->
        val name = ('a'.code + index).toChar().toString()
        (bisherige.firstOrNull { it.name == name } ?: bisherige.getOrNull(index) ?: AnschlussDaten(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Objekt.id,
        )).copy(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Objekt.id,
            reihenfolge = index,
            kannSichErweitern = true,
            dynamischErzeugt = index >= 2,
            zulässigeArten = setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Methode.id),
        )
    }
    val alterAusgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    return knoten.copy(
        name = operator.titel,
        anschlüsse = eingänge + zahlenOderMethodenAusgang(alterAusgang, eingänge.map { it.name }),
        parameter = (knoten.parameter - ZAHLENRECHNER_FORMEL_LATEX - ZAHLENRECHNER_FORMEL_VARIABLEN) + mapOf(
            ZAHLENRECHNER_OPERATOR to operator.stabileId,
            "festeEingänge" to anzahl.toString(),
        ),
    )
}

private fun konfiguriereStammfunktionZahlenRechner(knoten: KnotenDaten): KnotenDaten {
    val vorhandene = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    val methode = (vorhandene.firstOrNull { it.name == "methode" } ?: AnschlussDaten(
        name = "methode",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Methode.id,
    )).copy(name = "methode", art = MathematikAnschlussArten.Methode.id, reihenfolge = 0, zulässigeArten = emptySet())
    val startwert = (vorhandene.firstOrNull { it.name == "startwert" } ?: AnschlussDaten(
        name = "startwert",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Zahl.id,
    )).copy(name = "startwert", art = MathematikAnschlussArten.Zahl.id, reihenfolge = 1, zulässigeArten = emptySet())
    val alt = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    val ausgang = (alt ?: AnschlussDaten(
        name = "stammfunktion",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Methode.id,
    )).copy(
        name = "stammfunktion",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Methode.id,
        reihenfolge = 0,
        zulässigeArten = emptySet(),
        artPriorisiertEingänge = null,
    )
    return knoten.copy(
        name = ErweiterterZahlenOperator.STAMMFUNKTION.titel,
        anschlüsse = listOf(methode, startwert, ausgang),
        parameter = (knoten.parameter - ZAHLENRECHNER_FORMEL_LATEX - ZAHLENRECHNER_FORMEL_VARIABLEN) +
            (ZAHLENRECHNER_OPERATOR to ErweiterterZahlenOperator.STAMMFUNKTION.stabileId),
    )
}

private fun zahlenOderMethodenAusgang(
    bisherigerAusgang: AnschlussDaten?,
    eingänge: List<String>,
): AnschlussDaten = (bisherigerAusgang ?: AnschlussDaten(
    name = "wert",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = MathematikAnschlussArten.Zahl.id,
)).copy(
    name = "wert",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = MathematikAnschlussArten.Zahl.id,
    reihenfolge = 0,
    kannSichErweitern = false,
    dynamischErzeugt = false,
    artFolgtEingang = null,
    artVereinigtEingänge = emptyList(),
    zulässigeArten = emptySet(),
    artAbbildungVonEingang = null,
    artPriorisiertEingänge = AnschlussArtPriorisierung(
        eingänge = eingänge,
        prioritäten = listOf(MathematikAnschlussArten.Methode.id),
    ),
)

private fun konfigurierePolynomZahlenRechner(knoten: KnotenDaten): KnotenDaten {
    val vorhandeneEingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
    val bisherigeKoeffizienten = vorhandeneEingänge.firstOrNull { it.name == "koeffizienten" }
    val bisherigesArgument = vorhandeneEingänge.firstOrNull { it.name == "argument" }
    val bisherigerAusgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    val koeffizienten = (bisherigeKoeffizienten ?: AnschlussDaten(
        name = "koeffizienten", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Objekt.id,
    )).copy(
        name = "koeffizienten", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Objekt.id, reihenfolge = 0, kannSichErweitern = false,
        dynamischErzeugt = false,
        zulässigeArten = setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.Vektor.id),
    )
    val argument = (bisherigesArgument ?: AnschlussDaten(
        name = "argument", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Objekt.id,
    )).copy(
        name = "argument", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Objekt.id, reihenfolge = 1, kannSichErweitern = false,
        dynamischErzeugt = false,
        zulässigeArten = setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Methode.id),
    )
    val ausgang = zahlenOderMethodenAusgang(bisherigerAusgang, listOf("argument"))
    return knoten.copy(
        name = ErweiterterZahlenOperator.POLYNOM.titel,
        anschlüsse = listOf(koeffizienten, argument, ausgang),
        parameter = (knoten.parameter - ZAHLENRECHNER_FORMEL_LATEX - ZAHLENRECHNER_FORMEL_VARIABLEN) +
            (ZAHLENRECHNER_OPERATOR to ErweiterterZahlenOperator.POLYNOM.stabileId),
    )
}

fun konfiguriereZahlenRechnerFormel(knoten: KnotenDaten, latex: String): KnotenDaten {
    require(knoten.art == ZAHLENRECHNER_ART)
    val import = FormelLatexCodec.importiere(latex)
    val ausdruck = (import as? FormelLatexImportErgebnis.Erfolg)?.ausdruck
        ?: error((import as FormelLatexImportErgebnis.Fehler).nachricht)
    require(FormelAusdruckPruefer.pruefe(ausdruck) == FormelPruefung.Gueltig) {
        "Die Formel enthält noch Platzhalter oder ungültige Teilstrukturen."
    }
    val kanonisch = FormelLatexCodec.exportiere(ausdruck)
    val variablen = formelVariablen(ausdruck)
    val vorhandene = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.associateBy { it.name }
    val eingänge = variablen.mapIndexed { index, name ->
        (vorhandene[name] ?: AnschlussDaten(
            name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Zahl.id,
        )).copy(
            name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Zahl.id, reihenfolge = index, kannSichErweitern = false,
            dynamischErzeugt = false, zulässigeArten = emptySet(),
        )
    }
    val alterAusgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    val ausgang = (alterAusgang ?: AnschlussDaten(
        name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id,
    )).copy(
        name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id, reihenfolge = 0, kannSichErweitern = false,
        dynamischErzeugt = false, artFolgtEingang = null, artVereinigtEingänge = emptyList(),
        zulässigeArten = emptySet(), artAbbildungVonEingang = null, artPriorisiertEingänge = null,
    )
    return knoten.copy(
        name = "Formel",
        anschlüsse = eingänge + ausgang,
        parameter = knoten.parameter + mapOf(
            ZAHLENRECHNER_OPERATOR to ZAHLENRECHNER_FORMEL_ID,
            ZAHLENRECHNER_FORMEL_LATEX to kanonisch,
            ZAHLENRECHNER_FORMEL_VARIABLEN to variablen.joinToString(","),
        ),
    )
}

fun MathematikAuswerterRegister.registriereZahlenRechnerErweiterungen() {
    val basis = requireNotNull(finde(ZAHLENRECHNER_ART))
    registriere(ZAHLENRECHNER_ART) { kontext ->
        val operatorId = kontext.knoten.parameter[ZAHLENRECHNER_OPERATOR]
        when {
            operatorId == ZAHLENRECHNER_FORMEL_ID -> werteFormelAus(kontext)
            ErweiterterZahlenOperator.vonId(operatorId) != null ->
                werteErweitertenOperatorAus(kontext, requireNotNull(ErweiterterZahlenOperator.vonId(operatorId)))
            else -> basis.auswerten(kontext)
        }
    }
}

private fun werteErweitertenOperatorAus(
    kontext: KnotenAuswertungsKontext,
    operator: ErweiterterZahlenOperator,
): KnotenAuswertungsErgebnis = when (operator) {
    ErweiterterZahlenOperator.POLYNOM -> wertePolynomAus(kontext)
    ErweiterterZahlenOperator.ARITHMETISCHES_MITTEL,
    ErweiterterZahlenOperator.GEOMETRISCHES_MITTEL,
    -> werteMittelAus(kontext, operator)
    ErweiterterZahlenOperator.STAMMFUNKTION -> werteStammfunktionAus(kontext)
    else -> werteUnaerenErweitertenOperatorAus(kontext, operator)
}

private fun werteUnaerenErweitertenOperatorAus(
    kontext: KnotenAuswertungsKontext,
    operator: ErweiterterZahlenOperator,
): KnotenAuswertungsErgebnis {
    val eingang = kontext.eingänge["a"] ?: error("Der Eingang a fehlt.")
    val methode = eingang.objekt as? Methode
    val vorbereitung = methode?.let {
        require(operator.hebungsArt == ZahlenOperatorHebungsArt.PUNKTWEISE)
        bereitePunktweiseZahlenfunktionVor(mapOf("a" to it))
    }
    val argument = vorbereitung?.operanden?.getValue("a")
        ?: (eingang.objekt as? ZahlAusdruck ?: error("Der Eingang a enthält weder eine Zahl noch eine Zahlenfunktion."))
    val argumentLatex = eingang.anzeigeLatex()
    val grad = kontext.knoten.parameter[ZAHLENRECHNER_GRADWINKEL].toBoolean()
    val gradAuswerten = kontext.knoten.parameter[ZAHLENRECHNER_GRAD_AUSWERTEN] != "false"
    val effektivLatex = if (grad && verwendetGradWinkel(operator)) gradWinkelLatex(argument, gradAuswerten) else argumentLatex
    val latex = "${operator.symbolLatex}\\left($effektivLatex\\right)"
    val zahlObjekt = symbolischerZahlterm("${operator.stabileId}:$latex", latex)
    val bedingungen = erweiterteDefinitionsBedingungen(operator, argument)
    val objekt: MathematischesObjekt = if (vorbereitung == null) {
        zahlObjekt
    } else {
        val ziel = methode.zielMenge.fundamentalerZahlbereichOderNull()?.let { bereich ->
            if (FundamentaleZahlbereiche.istTeilbereich(bereich, FundamentalerZahlbereich.REELL)) ReelleZahlen else KomplexeZahlen
        } ?: KomplexeZahlen
        vorbereitung.erzeugeMethode(
            name = "${operator.symbolLatex}(${methode.name})", vorschrift = zahlObjekt,
            zielMenge = ziel, definitionsBedingungen = bedingungen,
        )
    }
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt, annahmen = eingang.annahmen,
                zielMenge = (objekt as? Methode)?.zielMenge,
                werteVorrat = (objekt as? Methode)?.methodenSignatur()?.werteVorrat ?: eingang.werteVorrat,
                reelleVariablen = eingang.reelleVariablen, variablenQuellen = eingang.variablenQuellen,
                latexDarstellung = if (objekt is Methode) null else latex,
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = buildList {
            addAll(definitionsHinweise(operator))
            (objekt as? Methode)?.let { punktweise ->
                add("Signatur: ${punktweise.methodenSignatur().werteVorrat.zuLatex()} → ${punktweise.zielMenge.zuLatex()}")
                if (bedingungen.isNotEmpty()) add("Definitionsbedingung: ${bedingungen.joinToString(" \\land ") { it.zuLatex() }}")
            }
        },
    )
}

private fun werteMittelAus(
    kontext: KnotenAuswertungsKontext,
    operator: ErweiterterZahlenOperator,
): KnotenAuswertungsErgebnis {
    val rollen = kontext.knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
        .map { it.name }
    require(rollen.size >= 2) { "Ein Mittelwert benötigt mindestens zwei Operanden." }
    val werte = rollen.associateWith { name ->
        kontext.eingänge[name]?.objekt ?: error("Operand '$name' fehlt.")
    }
    val vorbereitung = if (werte.values.any { it is Methode }) bereitePunktweiseZahlenfunktionVor(werte) else null
    val zahlen = rollen.map { name ->
        vorbereitung?.operanden?.getValue(name)
            ?: (werte.getValue(name) as? ZahlAusdruck ?: error("Mittelwerte akzeptieren nur Zahlen oder Zahlenfunktionen."))
    }
    val n = RationaleZahl.von(zahlen.size)
    val ausdruck = when (operator) {
        ErweiterterZahlenOperator.ARITHMETISCHES_MITTEL -> Division(addition(zahlen), n)
        ErweiterterZahlenOperator.GEOMETRISCHES_MITTEL -> Potenz(
            multiplikation(zahlen),
            Division(RationaleZahl.Eins, n),
        )
        else -> error("Kein Mittelwertoperator.")
    }
    val bedingungen = if (operator == ErweiterterZahlenOperator.GEOMETRISCHES_MITTEL) {
        zahlen.map { zahl ->
            UnentscheidbareAussage("${zahl.zuLatex()}\\ge 0", "reelles geometrisches Mittel")
        }
    } else emptyList()
    val objekt: MathematischesObjekt = vorbereitung?.erzeugeMethode(
        name = "${operator.symbolLatex}(${vorbereitung.methodenNamen.values.joinToString(",")})",
        vorschrift = ausdruck,
        zielMenge = inferiereZahlenWertevorrat(ausdruck),
        definitionsBedingungen = bedingungen,
    ) ?: ausdruck
    val eingangsWerte = kontext.eingänge.values
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = eingangsWerte.flatMap { it.annahmen }.toSet() + bedingungen,
                zielMenge = (objekt as? Methode)?.zielMenge,
                werteVorrat = (objekt as? Methode)?.methodenSignatur()?.werteVorrat ?: inferiereZahlenWertevorrat(ausdruck),
                reelleVariablen = reelleVariablen(eingangsWerte),
                variablenQuellen = eingangsWerte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = if (bedingungen.isEmpty()) emptyList() else listOf("Geometrisches Mittel wird reell unter den Bedingungen $${bedingungen.joinToString(" \\land ") { it.zuLatex() }}$$ ausgewertet."),
    )
}

private fun werteStammfunktionAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val methodeWert = kontext.eingänge["methode"] ?: error("Die Integrandenmethode fehlt.")
    val startwertWert = kontext.eingänge["startwert"] ?: error("Der Startwert c fehlt.")
    val integrand = methodeWert.objekt as? Methode ?: error("Stammfunktion erwartet eine Methode.")
    MethodenAnforderung.Zahlenfunktion.prüfe(integrand)?.let(::error)
    require(integrand.argumentAnzahl == 1) { "Die Stammfunktion benötigt eine einstellige Zahlenfunktion." }
    val c = startwertWert.objekt as? ZahlAusdruck ?: error("Der Startwert muss eine Zahl sein.")
    val x = Variable("x")
    val intervall = ReellesIntervall(c, linksOffen = false, rechts = x, rechtsOffen = false)
    val integral = methodenIntegral(
        methode = integrand,
        bereich = IntegralBereich(listOf(intervall)),
        kurz = false,
        mass = IntegralMass.StandardReell,
    )
    val argumentMenge = schneide(listOf(integrand.methodenSignatur().argumente.single().werteVorrat, ReelleZahlen))
    val stammfunktion = Methode(
        name = "F",
        parameter = listOf(x),
        vorschrift = integral,
        zielMenge = integrand.zielMenge,
        werteVorräte = mapOf(x.name to argumentMenge),
        effektiverWerteVorrat = Tupelraum(listOf(argumentMenge)),
    )
    val annahmen = methodeWert.annahmen + startwertWert.annahmen + integral.voraussetzungen
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "stammfunktion" to BedingterWert(
                objekt = stammfunktion,
                annahmen = annahmen,
                zielMenge = stammfunktion.zielMenge,
                werteVorrat = stammfunktion.methodenSignatur().werteVorrat,
                reelleVariablen = reelleVariablen(kontext.eingänge.values),
                variablenQuellen = kontext.eingänge.values.flatMap { it.variablenQuellen }.geordnetEindeutig(),
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = listOf(
            "Definition: F(x)=${integral.zuLatex()}",
            "Der Startwert c fixiert genau eine Stammfunktion; andere Startwerte unterscheiden sich um eine additive Konstante.",
        ),
    )
}

private fun wertePolynomAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val koeffizientenWert = kontext.eingänge["koeffizienten"] ?: error("Das Koordinatentupel der Koeffizienten fehlt.")
    val argumentWert = kontext.eingänge["argument"] ?: error("Das Polynomargument fehlt.")
    val koeffizienten = when (val objekt = koeffizientenWert.objekt) {
        is Tupel -> objekt.elemente.mapIndexed { index, element ->
            element as? ZahlAusdruck ?: error("Koeffizient ${index + 1} des Tupels ist keine Zahl.")
        }
        is OrientierterVektor -> objekt.werte
        else -> error("Koeffizienten müssen als Koordinatentupel vorliegen.")
    }
    val argumentMethode = argumentWert.objekt as? Methode
    val vorbereitung = argumentMethode?.let { bereitePunktweiseZahlenfunktionVor(mapOf("argument" to it)) }
    val argument = vorbereitung?.operanden?.getValue("argument")
        ?: (argumentWert.objekt as? ZahlAusdruck ?: error("Das Polynomargument ist weder Zahl noch Zahlenfunktion."))
    val ausdruck = polynomAusKoeffizienten(koeffizienten, argument)
    val objekt: MathematischesObjekt = vorbereitung?.erzeugeMethode(
        name = "p(${argumentMethode?.name})",
        vorschrift = ausdruck,
        zielMenge = inferiereZahlenWertevorrat(ausdruck),
    ) ?: ausdruck
    val werte = listOf(koeffizientenWert, argumentWert)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = werte.flatMap { it.annahmen }.toSet(),
                zielMenge = (objekt as? Methode)?.zielMenge,
                werteVorrat = (objekt as? Methode)?.methodenSignatur()?.werteVorrat ?: inferiereZahlenWertevorrat(ausdruck),
                reelleVariablen = reelleVariablen(werte),
                variablenQuellen = werte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
            ),
        ),
        eingänge = kontext.eingänge,
    )
}

private fun erweiterteDefinitionsBedingungen(
    operator: ErweiterterZahlenOperator,
    argument: ZahlAusdruck,
): List<Aussage> = when (operator) {
    ErweiterterZahlenOperator.TANGENS,
    ErweiterterZahlenOperator.SEKANS,
    -> listOf(Ungleichheit(Cosinus(argument), RationaleZahl.Null))
    ErweiterterZahlenOperator.COTANGENS,
    ErweiterterZahlenOperator.KOSEKANS,
    -> listOf(Ungleichheit(Sinus(argument), RationaleZahl.Null))
    ErweiterterZahlenOperator.COTANGENS_HYPERBOLICUS,
    ErweiterterZahlenOperator.KOSEKANS_HYPERBOLICUS,
    -> listOf(
        Ungleichheit(
            symbolischerZahlterm("sinh:${argument.zuLatex()}", "\\sinh(${argument.zuLatex()})"),
            RationaleZahl.Null,
        ),
    )
    else -> emptyList()
}

private fun werteFormelAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val latex = kontext.knoten.parameter[ZAHLENRECHNER_FORMEL_LATEX] ?: error("Die gespeicherte Formel fehlt.")
    val import = FormelLatexCodec.importiere(latex)
    val ausdruck = (import as? FormelLatexImportErgebnis.Erfolg)?.ausdruck
        ?: error((import as FormelLatexImportErgebnis.Fehler).nachricht)
    require(FormelAusdruckPruefer.pruefe(ausdruck) == FormelPruefung.Gueltig) { "Die gespeicherte Formel ist unvollständig." }
    val symbolischeDarstellung = FormelLatexCodec.exportiere(ausdruck)
    var eingesetzt = symbolischeDarstellung
    formelVariablen(ausdruck).sortedByDescending(String::length).forEach { name ->
        val wert = kontext.eingänge[name] ?: error("Der Formeleingang '$name' fehlt.")
        val muster = Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])")
        eingesetzt = eingesetzt.replace(muster, "\\left(${wert.anzeigeLatex()}\\right)")
    }
    val werte = kontext.eingänge.values
    val objekt = symbolischerZahlterm("formel:${eingesetzt.hashCode().toUInt()}", eingesetzt)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = werte.flatMap { it.annahmen }.toSet(),
                werteVorrat = werte.mapNotNull { it.werteVorrat }.distinct().singleOrNull(),
                reelleVariablen = reelleVariablen(werte),
                variablenQuellen = werte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
                latexDarstellung = symbolischeDarstellung,
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = listOf("Formel: $symbolischeDarstellung"),
    )
}

private fun definitionsHinweise(operator: ErweiterterZahlenOperator): List<String> = when (operator) {
    ErweiterterZahlenOperator.TANGENS,
    ErweiterterZahlenOperator.SEKANS,
    -> listOf("Definitionslücke: cos(x) darf nicht null sein.")
    ErweiterterZahlenOperator.COTANGENS,
    ErweiterterZahlenOperator.KOSEKANS,
    -> listOf("Definitionslücke: sin(x) darf nicht null sein.")
    ErweiterterZahlenOperator.COTANGENS_HYPERBOLICUS,
    ErweiterterZahlenOperator.KOSEKANS_HYPERBOLICUS,
    -> listOf("Definitionslücke: sinh(x) darf nicht null sein.")
    else -> emptyList()
}

private fun formelVariablen(ausdruck: FormelAusdruck): List<String> {
    val namen = linkedSetOf<String>()
    fun besuche(teil: FormelAusdruck) {
        when (teil) {
            is FormelAusdruck.Variable -> namen += teil.name
            is FormelAusdruck.Operation -> teil.argumente.sortedBy { it.position }.forEach { besuche(it.ausdruck) }
            else -> Unit
        }
    }
    besuche(ausdruck)
    return namen.toList()
}
