package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

/**
 * Vollständige Operatoridentität des erzeugbaren Zahlenrechners.
 *
 * Die stabilen IDs sind Persistenzvertrag. Sichtbare Namen und Notation dürfen
 * verbessert werden, ohne gespeicherte Karten umzuschreiben.
 */
enum class UniversellerZahlenOperator(
    val stabileId: String,
    val titel: String,
    val symbolLatex: String,
) {
    ADDITION("zahl.addition", "Addition", "+"),
    SUBTRAKTION("zahl.subtraktion", "Subtraktion", "-"),
    MULTIPLIKATION("zahl.multiplikation", "Multiplikation", "\\cdot"),
    DIVISION("zahl.division", "Division", "\\div"),
    KEHRWERT("zahl.kehrwert", "Kehrwert", "^{-1}"),
    POTENZ("zahl.potenz", "Potenz", "^"),
    QUADRAT("zahl.quadrat", "Quadrat", "^2"),
    KUBIK("zahl.kubik", "Kubik", "^3"),
    WURZEL("zahl.wurzel", "Wurzel", "\\sqrt[{}]{}"),
    QUADRATWURZEL("zahl.quadratwurzel", "Quadratwurzel", "\\sqrt{}"),
    KUBIKWURZEL("zahl.kubikwurzel", "Kubikwurzel", "\\sqrt[3]{}"),
    LOGARITHMUS("zahl.logarithmus", "Logarithmus", "\\log_{}"),
    LOGARITHMUS_BASIS_2("zahl.lb", "Binärer Logarithmus", "\\operatorname{lb}"),
    NATUERLICHER_LOGARITHMUS("zahl.ln", "Natürlicher Logarithmus", "\\ln"),
    LOGARITHMUS_BASIS_10("zahl.log10", "Zehnerlogarithmus", "\\log"),
    ITERIERTE_SUMME("zahl.iterierteSumme", "Iterierte Summe", "\\sum"),
    ITERIERTES_PRODUKT("zahl.iteriertesProdukt", "Iteriertes Produkt", "\\prod"),
    INTEGRAL("zahl.integral", "Integral", "\\int"),
    DIFFERENTIAL("zahl.differential", "Differential", "\\frac{d}{d x}"),
    MINIMUM("zahl.minimum", "Minimum", "\\min"),
    MAXIMUM("zahl.maximum", "Maximum", "\\max"),
    NORM("zahl.norm", "Norm", "\\lVert\\cdot\\rVert"),
    ABRUNDUNG("zahl.abrundung", "Abrundung", "\\lfloor\\cdot\\rfloor"),
    AUFRUNDUNG("zahl.aufrundung", "Aufrundung", "\\lceil\\cdot\\rceil"),
    RUNDUNG("zahl.rundung", "Rundung", "\\lfloor\\cdot\\rceil"),
    KONJUGIERTE("zahl.konjugierte", "Konjugierte", "\\overline{\\cdot}"),
    REALTEIL("zahl.realteil", "Realteil", "\\operatorname{Re}"),
    IMAGINAERTEIL("zahl.imaginaerteil", "Imaginärteil", "\\operatorname{Im}"),
    KOMPLEXER_WINKEL("zahl.komplexerWinkel", "Komplexer Winkel", "\\arg"),
    KOMPLEXER_RADIUS("zahl.komplexerRadius", "Komplexer Radius", "|\\cdot|"),
    KOMPLEX_AUS_POLAR("zahl.komplexAusPolar", "Komplex aus Polarform", "r e^{i\\varphi}"),
    KOMPLEX_AUS_KARTESISCH("zahl.komplexAusKartesisch", "Komplex aus kartesischer Form", "a+b i"),
    MODULO("zahl.modulo", "Modulo", "\\bmod"),
    BETRAG("zahl.betrag", "Betrag", "|\\cdot|"),
    EXPONENTIALFUNKTION("zahl.exp", "Exponentialfunktion", "\\exp"),
    SINUS("zahl.sin", "Sinus", "\\sin"),
    COSINUS("zahl.cos", "Cosinus", "\\cos"),
    ARCSINUS("zahl.arcsin", "Arcus Sinus", "\\arcsin"),
    ARCCOSINUS("zahl.arccos", "Arcus Cosinus", "\\arccos"),
    LIMES_HYPERREELL_ZU_REELL("zahl.limes", "Limes", "\\lim"),
    ;

    companion object {
        fun vonId(id: String?): UniversellerZahlenOperator = entries.firstOrNull { operator ->
            id == operator.stabileId ||
                id.equals(operator.name, ignoreCase = true) ||
                id.equals(operator.stabileId.substringAfterLast('.'), ignoreCase = true)
        } ?: ADDITION
    }
}

/** Zahlbereiche, die für die automatische Definitions- und Regelauswahl relevant sind. */
enum class ZahlenRechnerBereich(
    val id: String,
    val latex: String,
    val rang: Int,
    val geordnet: Boolean,
    val multiplikativKommutativ: Boolean,
) {
    NATUERLICH("N", "\\mathbb N", 0, true, true),
    NATUERLICH_MIT_NULL("N0", "\\mathbb N_0", 1, true, true),
    GANZ("Z", "\\mathbb Z", 2, true, true),
    RATIONAL("Q", "\\mathbb Q", 3, true, true),
    REELL("R", "\\mathbb R", 4, true, true),
    KOMPLEX("C", "\\mathbb C", 5, false, true),
    HYPERREELL("*R", "{}^*\\mathbb R", 6, true, true),
    QUATERNION("H", "\\mathbb H", 7, false, false),
    MODULO("Zn", "\\mathbb Z/n\\mathbb Z", 8, false, true),
    UNBEKANNT("?", "\\mathcal Z", 9, false, false),
}

data class ZahlenRechnerDefinition(
    val operator: UniversellerZahlenOperator,
    val bereich: ZahlenRechnerBereich,
    val latex: String = "${operator.symbolLatex}\\vert_{${bereich.latex}}",
    val regeln: List<String> = rechenRegeln(operator, bereich),
)

fun rechenRegeln(
    operator: UniversellerZahlenOperator,
    bereich: ZahlenRechnerBereich,
): List<String> = buildList {
    add("Definition: ${operator.symbolLatex}\\vert_{${bereich.latex}}")
    if (operator == UniversellerZahlenOperator.MULTIPLIKATION && !bereich.multiplikativKommutativ) {
        add("Die Faktorordnung bleibt erhalten; Multiplikation ist auf ${bereich.latex} nicht kommutativ.")
    }
    if (operator in setOf(UniversellerZahlenOperator.MINIMUM, UniversellerZahlenOperator.MAXIMUM) && !bereich.geordnet) {
        add("Minimum und Maximum benötigen einen geordneten Zahlbereich.")
    }
    if (operator == UniversellerZahlenOperator.DIVISION) {
        add("Der Nenner muss von null verschieden sein.")
    }
    if (operator == UniversellerZahlenOperator.MODULO) {
        add("Modulo verwendet ganzzahlige Vertreter und einen positiven Modul.")
    }
}

fun inferiereZahlenRechnerBereich(
    ausdruck: ZahlAusdruck,
    werteVorrat: MengenAusdruck? = null,
): ZahlenRechnerBereich {
    val vorratLatex = werteVorrat?.zuLatex().orEmpty()
    return when {
        "{}^*\\mathbb R" in vorratLatex || "^*\\mathbb R" in vorratLatex -> ZahlenRechnerBereich.HYPERREELL
        "\\mathbb H" in vorratLatex -> ZahlenRechnerBereich.QUATERNION
        "\\mathbb Z/" in vorratLatex || "\\bmod" in vorratLatex -> ZahlenRechnerBereich.MODULO
        werteVorrat == KomplexeZahlen || ausdruck is KomplexeZahl -> ZahlenRechnerBereich.KOMPLEX
        werteVorrat == ReelleZahlen -> ZahlenRechnerBereich.REELL
        werteVorrat == RationaleZahlen -> ZahlenRechnerBereich.RATIONAL
        werteVorrat == GanzeZahlen -> ZahlenRechnerBereich.GANZ
        werteVorrat == NatürlicheZahlen -> if (ausdruck == RationaleZahl.Null) {
            ZahlenRechnerBereich.NATUERLICH_MIT_NULL
        } else {
            ZahlenRechnerBereich.NATUERLICH
        }
        ausdruck is RationaleZahl -> when {
            ausdruck.nenner != BigInteger.ONE -> ZahlenRechnerBereich.RATIONAL
            ausdruck.zähler.signum() < 0 -> ZahlenRechnerBereich.GANZ
            ausdruck.zähler.signum() == 0 -> ZahlenRechnerBereich.NATUERLICH_MIT_NULL
            else -> ZahlenRechnerBereich.NATUERLICH
        }
        else -> ZahlenRechnerBereich.UNBEKANNT
    }
}

fun gemeinsamerZahlenRechnerBereich(
    bereiche: Iterable<ZahlenRechnerBereich>,
): ZahlenRechnerBereich {
    val liste = bereiche.toList()
    require(liste.isNotEmpty())
    if (ZahlenRechnerBereich.MODULO in liste) {
        return if (liste.all { it == ZahlenRechnerBereich.MODULO }) ZahlenRechnerBereich.MODULO
        else ZahlenRechnerBereich.UNBEKANNT
    }
    return liste.maxBy(ZahlenRechnerBereich::rang)
}

/**
 * Wählt Bruch- oder Divisionsnotation aus der sichtbaren Struktur.
 * Ein kurzer atomarer Nenner bleibt in einer Zeile; zusammengesetzte oder lange
 * Nenner werden als Bruch dargestellt.
 */
fun intelligenteDivisionLatex(
    zaehler: ZahlAusdruck,
    nenner: ZahlAusdruck,
): String {
    val z = zaehler.zuLatex()
    val n = nenner.zuLatex()
    val zusammengesetzt = n.length > 10 || listOf(" + ", " - ", "\\cdot", "\\frac", "\\sum", "\\prod", "\\int").any(n::contains)
    if (zusammengesetzt) return "\\frac{$z}{$n}"
    val zText = if (zaehler is Addition) "\\left($z\\right)" else z
    val nText = if (nenner is Addition || nenner is Multiplikation) "\\left($n\\right)" else n
    return "$zText \\div $nText"
}

/** Projektdefinition des Gradzeichens: ° := π/180. */
val GradWinkelEinheit: ZahlAusdruck = Division(Pi, RationaleZahl.von(180))

fun gradZuBogenmass(winkel: ZahlAusdruck): ZahlAusdruck = multiplikation(winkel, GradWinkelEinheit)

fun gradWinkelLatex(winkel: ZahlAusdruck, alsBogenmassAuswerten: Boolean): String =
    if (alsBogenmassAuswerten) "${winkel.zuLatex()} \\cdot \\pi \\div 180"
    else "${winkel.zuLatex()}^{\\circ}"

fun komplexAusKartesisch(realteil: ZahlAusdruck, imaginaerTeil: ZahlAusdruck): KomplexeZahl =
    KomplexeZahl(realteil, imaginaerTeil)

fun komplexAusPolar(
    radius: ZahlAusdruck,
    winkel: ZahlAusdruck,
    gradWinkel: Boolean,
    gradAlsBogenmassAuswerten: Boolean,
): KomplexeZahl {
    val phi = if (gradWinkel && gradAlsBogenmassAuswerten) gradZuBogenmass(winkel) else winkel
    return KomplexeZahl(
        multiplikation(radius, Cosinus(phi)),
        multiplikation(radius, Sinus(phi)),
    )
}

fun abrunden(ausdruck: ZahlAusdruck): ZahlAusdruck = rationaleRundung(ausdruck, RundungsArt.ABRUNDEN)
fun aufrunden(ausdruck: ZahlAusdruck): ZahlAusdruck = rationaleRundung(ausdruck, RundungsArt.AUFRUNDEN)
fun runden(ausdruck: ZahlAusdruck): ZahlAusdruck = rationaleRundung(ausdruck, RundungsArt.RUNDEN)

enum class RundungsArt { ABRUNDEN, AUFRUNDEN, RUNDEN }

private fun rationaleRundung(ausdruck: ZahlAusdruck, art: RundungsArt): ZahlAusdruck {
    val rational = ausdruck as? RationaleZahl ?: return symbolischerZahlterm(
        "${art.name.lowercase()}-${ausdruck.zuLatex()}",
        when (art) {
            RundungsArt.ABRUNDEN -> "\\left\\lfloor ${ausdruck.zuLatex()} \\right\\rfloor"
            RundungsArt.AUFRUNDEN -> "\\left\\lceil ${ausdruck.zuLatex()} \\right\\rceil"
            RundungsArt.RUNDEN -> "\\left\\lfloor ${ausdruck.zuLatex()} \\right\\rceil"
        },
    )
    val q = rational.zähler.divide(rational.nenner)
    val rest = rational.zähler.remainder(rational.nenner)
    val ganz = when (art) {
        RundungsArt.ABRUNDEN -> if (rest.signum() < 0) q - BigInteger.ONE else q
        RundungsArt.AUFRUNDEN -> if (rest.signum() > 0) q + BigInteger.ONE else q
        RundungsArt.RUNDEN -> {
            val doppelt = rest.abs() * BigInteger.TWO
            if (doppelt < rational.nenner) q
            else q + BigInteger.valueOf(rational.zähler.signum().toLong())
        }
    }
    return RationaleZahl.von(ganz)
}

fun modulo(dividend: ZahlAusdruck, modul: ZahlAusdruck): ZahlAusdruck {
    val a = dividend as? RationaleZahl
    val m = modul as? RationaleZahl
    if (a != null && m != null && a.nenner == BigInteger.ONE && m.nenner == BigInteger.ONE) {
        require(m.zähler.signum() > 0) { "Der Modul muss positiv sein." }
        return RationaleZahl.von(a.zähler.mod(m.zähler))
    }
    return symbolischerZahlterm(
        "modulo-${dividend.zuLatex()}-${modul.zuLatex()}",
        "${dividend.zuLatex()} \\bmod ${modul.zuLatex()}",
    )
}

fun symbolischerZahlterm(identitaet: String, latex: String): ZahlAusdruck =
    Variable("__zahlenrechner_${identitaet.hashCode().toUInt()}", latex)
