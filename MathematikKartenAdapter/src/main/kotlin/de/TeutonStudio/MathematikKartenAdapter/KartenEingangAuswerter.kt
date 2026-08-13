package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val KARTEN_EINGANG_ART = "mathematik.kartenEingang"
const val KARTEN_METHODEN_SIGNATUR_AKTIV = "kartenMethode.signatur.aktiv"
const val KARTEN_METHODEN_ARGUMENT_ANZAHL = "kartenMethode.signatur.argumentAnzahl"
const val KARTEN_METHODEN_ARGUMENT_PREFIX = "kartenMethode.signatur.argument."
const val KARTEN_METHODEN_ZIELMENGE = "kartenMethode.signatur.zielMenge"

private val ZAHL_KARTEN_ART = AnschlussArtId("mathematik.zahl")
private val AUSSAGE_KARTEN_ART = AnschlussArtId("mathematik.aussage")
private val MENGE_KARTEN_ART = AnschlussArtId("mathematik.menge")
private val OBJEKT_KARTEN_ART = AnschlussArtId("mathematik.objekt")
private val METHODE_KARTEN_ART = AnschlussArtId("mathematik.methode")
private val LEGACY_METHODE_KARTEN_ART = AnschlussArtId("mathematik.funktion")
private val ZAHL_METHODE_KARTEN_ART = AnschlussArtId("mathematik.funktion.zahl")
private val AUSSAGE_METHODE_KARTEN_ART = AnschlussArtId("mathematik.funktion.aussage")
private val MENGEN_METHODE_KARTEN_ART = AnschlussArtId("mathematik.funktion.menge")
private val SPALTEN_METHODE_KARTEN_ART = AnschlussArtId("mathematik.funktion.vektor.spalte")
private val ZEILEN_METHODE_KARTEN_ART = AnschlussArtId("mathematik.funktion.vektor.zeile")

fun kartenMethodenArgumentNameSchlüssel(index: Int): String = "$KARTEN_METHODEN_ARGUMENT_PREFIX$index.name"
fun kartenMethodenArgumentWerteVorratSchlüssel(index: Int): String = "$KARTEN_METHODEN_ARGUMENT_PREFIX$index.werteVorrat"

/**
 * Optionale, explizit deklarierte Methodensignatur eines öffentlichen Karteneingangs.
 * Fehlt die Deklaration, bleibt die Methode absichtlich ohne mathematische Signatur.
 */
fun deklarierteMethodenSignatur(knoten: KnotenDaten): MethodenSignatur? {
    if (knoten.parameter[KARTEN_METHODEN_SIGNATUR_AKTIV] != "true") return null
    val anzahl = knoten.parameter[KARTEN_METHODEN_ARGUMENT_ANZAHL]
        ?.toIntOrNull()
        ?.coerceAtLeast(0)
        ?: 1
    val argumente = List(anzahl) { index ->
        val name = knoten.parameter[kartenMethodenArgumentNameSchlüssel(index)]
            ?.trim()
            .orEmpty()
            .ifBlank { "x${index + 1}" }
        val menge = deklarierteMenge(
            knoten.parameter[kartenMethodenArgumentWerteVorratSchlüssel(index)],
            fallbackId = "W_${knoten.id.wert}_$index",
            fallbackLatex = "\\mathcal{W}_{${index + 1}}",
        )
        MethodenArgument(parameterFürDeklaration(name, menge), menge)
    }
    val zielMenge = deklarierteMenge(
        knoten.parameter[KARTEN_METHODEN_ZIELMENGE],
        fallbackId = "Z_${knoten.id.wert}",
        fallbackLatex = "\\mathcal{Z}",
    )
    return MethodenSignatur(argumente = argumente, zielMenge = zielMenge)
}

private fun parameterFürDeklaration(name: String, menge: MengenAusdruck): MethodenParameter = when {
    menge.istZahlenmenge() -> Variable(name)
    menge == WahrheitsMenge -> AussagenParameter(name)
    else -> AllgemeinerParameter(name)
}

private fun deklarierteMenge(
    roh: String?,
    fallbackId: String,
    fallbackLatex: String,
): MengenAusdruck {
    val text = roh?.trim().orEmpty()
    if (text.isBlank()) return BenannteMenge(fallbackId, fallbackLatex)
    val kompakt = text
        .replace(" ", "")
        .replace("{", "")
        .replace("}", "")
    val id = when (kompakt) {
        "N", "ℕ", "\\mathbbN", "\\mathbb{N}" -> FundamentalerZahlbereich.NATUERLICH_POSITIV
        "N0", "N_0", "ℕ₀", "\\mathbbN_0", "\\mathbb{N}_0", "\\mathbb{N_0}" -> FundamentalerZahlbereich.NATUERLICH_MIT_NULL
        "Z", "ℤ", "\\mathbbZ", "\\mathbb{Z}" -> FundamentalerZahlbereich.GANZ
        "Q", "ℚ", "\\mathbbQ", "\\mathbb{Q}" -> FundamentalerZahlbereich.RATIONAL
        "R", "ℝ", "\\mathbbR", "\\mathbb{R}" -> FundamentalerZahlbereich.REELL
        "C", "ℂ", "\\mathbbC", "\\mathbb{C}" -> FundamentalerZahlbereich.KOMPLEX
        "H", "ℍ", "\\mathbbH", "\\mathbb{H}" -> FundamentalerZahlbereich.QUATERNION
        else -> null
    }
    return id?.alsMenge() ?: when (text.lowercase()) {
        "bool", "boolean", "wahrheit", "wahrheitsmenge" -> WahrheitsMenge
        else -> BenannteMenge("deklaration_${text.hashCode()}", text)
    }
}

private data class UnbestimmteMethodenschnittstelle(
    override val name: String,
) : Methode

private data class DeklarierteMethodenschnittstelle(
    override val name: String,
    override val signatur: MethodenSignatur,
) : SignaturtragendeMethode

/**
 * Erzeugt einen symbolischen Eingabewert, dessen Laufzeitobjekt zur deklarierten
 * Anschlussart passt. Eine allgemeine Methode erhält ohne explizite Deklaration
 * keine geratene Stelligkeit und keinen erfundenen Wertevorrat.
 */
fun symbolischerEingangswert(
    art: AnschlussArtId,
    name: String,
    knotenId: KnotenId,
    aussagenVorschau: Aussage? = null,
    methodenErgebnisArt: String? = null,
    methodenSignatur: MethodenSignatur? = null,
): BedingterWert {
    val parameterName = name.trim().ifBlank { "x" }
    symbolischeMethode(art, parameterName, methodenErgebnisArt, methodenSignatur)?.let { methode ->
        return BedingterWert(
            objekt = methode,
            latexDarstellung = parameterName,
            symbolischeMethode = true,
        )
    }

    val objekt: MathematischesObjekt = when (art) {
        ZAHL_KARTEN_ART -> Variable(parameterName)
        AUSSAGE_KARTEN_ART -> aussagenVorschau ?: AussagenParameter(parameterName)
        MENGE_KARTEN_ART -> MengenParameter(parameterName)
        else -> TypisiertesElement(parameterName, art.wert)
    }
    val werteVorrat: MengenAusdruck = when (art) {
        ZAHL_KARTEN_ART -> ReelleZahlen
        AUSSAGE_KARTEN_ART -> WahrheitsMenge
        MENGE_KARTEN_ART -> BenannteMenge("mengen_$parameterName", "\\mathcal{P}(\\mathcal{U})")
        else -> BenannteMenge("werte_$parameterName", "\\mathcal{W}_{${parameterName}}")
    }
    val argumentArt = if (objekt is Aussage) ArgumentQuellenArt.Aussage else ArgumentQuellenArt.Wert

    return BedingterWert(
        objekt = objekt,
        werteVorrat = werteVorrat,
        reelleVariablen = if (objekt is Variable) mapOf(parameterName to werteVorrat) else emptyMap(),
        variablenQuellen = listOf(
            VariablenQuelle(
                knotenId = knotenId,
                name = parameterName,
                werteVorrat = werteVorrat,
                alsMethodenParameter = false,
                argumentArt = argumentArt,
                aussage = objekt as? Aussage,
            ),
        ),
    )
}

private fun symbolischeMethode(
    art: AnschlussArtId,
    name: String,
    vertragErgebnisArt: String?,
    deklarierteSignatur: MethodenSignatur?,
): Methode? {
    if (art !in setOf(
            METHODE_KARTEN_ART,
            LEGACY_METHODE_KARTEN_ART,
            ZAHL_METHODE_KARTEN_ART,
            AUSSAGE_METHODE_KARTEN_ART,
            MENGEN_METHODE_KARTEN_ART,
            SPALTEN_METHODE_KARTEN_ART,
            ZEILEN_METHODE_KARTEN_ART,
        )
    ) return null

    deklarierteSignatur?.let { return DeklarierteMethodenschnittstelle(name, it) }
    if (art == METHODE_KARTEN_ART || art == LEGACY_METHODE_KARTEN_ART) {
        return UnbestimmteMethodenschnittstelle(name)
    }

    // Historische spezialisierte Methodenanschlüsse behalten für die Lademigration
    // ihre bisherige symbolische Einargumentdarstellung. Neue Schnittstellen verwenden
    // ausschließlich mathematik.methode plus den optionalen Signaturvertrag oben.
    val ergebnisArt = vertragErgebnisArt ?: when (art) {
        ZAHL_METHODE_KARTEN_ART -> "mathematik.zahl"
        AUSSAGE_METHODE_KARTEN_ART -> "mathematik.aussage"
        MENGEN_METHODE_KARTEN_ART -> "mathematik.menge"
        SPALTEN_METHODE_KARTEN_ART -> "mathematik.vektor.spalte"
        ZEILEN_METHODE_KARTEN_ART -> "mathematik.vektor.zeile"
        else -> "mathematik.objekt"
    }
    val index = Variable("i")
    val anwendungsLatex = "$name(${index.zuLatex()})"
    val kennung = "${name}_von_${index.name}"
    val (wert, zielMenge) = when (ergebnisArt) {
        "mathematik.menge" -> symbolischerMengenMethodenwert(name, index)
        else -> {
            val wert = symbolischerMethodenWert(kennung, anwendungsLatex, ergebnisArt)
            wert to zielMengeFürMethodenErgebnisArt(name, wert, ergebnisArt)
        }
    }
    return Methode(
        name = name,
        parameter = listOf(index),
        vorschrift = wert,
        zielMenge = zielMenge,
        werteVorräte = mapOf(index.name to ReelleZahlen),
    )
}

private fun symbolischerMethodenWert(
    kennung: String,
    latex: String,
    ergebnisArt: String,
): MathematischesObjekt = when (ergebnisArt) {
    "mathematik.zahl" -> Variable(kennung)
    "mathematik.aussage" -> AussagenParameter(kennung, latex)
    else -> TypisiertesElement(kennung, ergebnisArt, latex)
}

private fun zielMengeFürMethodenErgebnisArt(
    name: String,
    wert: MathematischesObjekt,
    ergebnisArt: String,
): MengenAusdruck = when (ergebnisArt) {
    "mathematik.zahl" -> ReelleZahlen
    "mathematik.aussage" -> WahrheitsMenge
    else -> runCatching { inferiereZielmenge(wert) }
        .getOrElse { BenannteMenge("W_$name", "\\mathcal{W}") }
}

/**
 * Repräsentiert eine unbekannte mengenwertige Methode über ihren Graphen. So
 * bleibt der Index ein echter freier Parameter der Vorschrift.
 */
private fun symbolischerMengenMethodenwert(
    name: String,
    index: Variable,
): Pair<MengenAusdruck, MengenAusdruck> {
    val grundMenge = BenannteMenge("G_$name", "G")
    val element = TypisiertesElement("${name}_element", OBJEKT_KARTEN_ART.wert, "x")
    val graph = BenannteMenge("graph_$name", "\\operatorname{Graph}($name)")
    val bedingung = ElementBeziehung(Tupel(listOf(index, element)), graph)
    val prädikat = Methode(
        name = "${name}_graph",
        parameter = listOf(element),
        vorschrift = bedingung,
        zielMenge = WahrheitsMenge,
        werteVorräte = mapOf(element.name to grundMenge),
    )
    return GefilterteMenge(grundMenge, prädikat) to grundMenge
}

/** Erzeugt für öffentliche Karten-Eingänge ein Symbol, das ihrer Anschlussart tatsächlich entspricht. */
internal object KartenEingangAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val name = kontext.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
        val ausgang = kontext.knoten.anschlüsse.firstOrNull {
            it.richtung == AnschlussRichtung.Ausgang
        }
        val ausgangsArt = ausgang?.art ?: OBJEKT_KARTEN_ART
        val ergebnisArt = ausgang?.let { anschluss ->
            kontext.knoten.parameter[methodenErgebnisArtSchlüssel(anschluss.name)]
        }
        val signatur = if (ausgangsArt == METHODE_KARTEN_ART) {
            deklarierteMethodenSignatur(kontext.knoten)
        } else null

        return KnotenAuswertungsErgebnis(mapOf(
            "wert" to symbolischerEingangswert(
                art = ausgangsArt,
                name = name,
                knotenId = kontext.knoten.id,
                methodenErgebnisArt = ergebnisArt,
                methodenSignatur = signatur,
            ),
        ))
    }
}
