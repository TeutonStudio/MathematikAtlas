package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val KARTEN_EINGANG_ART = "mathematik.kartenEingang"

private val ZAHL_KARTEN_ART = AnschlussArtId("mathematik.zahl")
private val AUSSAGE_KARTEN_ART = AnschlussArtId("mathematik.aussage")
private val MENGE_KARTEN_ART = AnschlussArtId("mathematik.menge")
private val OBJEKT_KARTEN_ART = AnschlussArtId("mathematik.objekt")
private val METHODE_KARTEN_ART = AnschlussArtId("mathematik.methode")
private val LEGACY_FUNKTION_KARTEN_ART = AnschlussArtId("mathematik.funktion")
private val ZAHL_FUNKTION_KARTEN_ART = AnschlussArtId("mathematik.funktion.zahl")
private val AUSSAGE_FUNKTION_KARTEN_ART = AnschlussArtId("mathematik.funktion.aussage")
private val MENGEN_FUNKTION_KARTEN_ART = AnschlussArtId("mathematik.funktion.menge")
private val SPALTEN_FUNKTION_KARTEN_ART = AnschlussArtId("mathematik.funktion.vektor.spalte")
private val ZEILEN_FUNKTION_KARTEN_ART = AnschlussArtId("mathematik.funktion.vektor.zeile")

/**
 * Erzeugt einen symbolischen Eingabewert, dessen Laufzeitobjekt zur deklarierten
 * Anschlussart passt. Bei der einheitlichen Methodenart wird die Ergebnisart
 * ausschließlich über einen semantischen Vertrag angegeben.
 */
fun symbolischerEingangswert(
    art: AnschlussArtId,
    name: String,
    knotenId: KnotenId,
    aussagenVorschau: Aussage? = null,
    methodenErgebnisArt: String? = null,
): BedingterWert {
    val parameterName = name.trim().ifBlank { "x" }
    symbolischeMethode(art, parameterName, methodenErgebnisArt)?.let { methode ->
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
): Methode? {
    if (art !in setOf(
            METHODE_KARTEN_ART,
            LEGACY_FUNKTION_KARTEN_ART,
            ZAHL_FUNKTION_KARTEN_ART,
            AUSSAGE_FUNKTION_KARTEN_ART,
            MENGEN_FUNKTION_KARTEN_ART,
            SPALTEN_FUNKTION_KARTEN_ART,
            ZEILEN_FUNKTION_KARTEN_ART,
        )
    ) return null

    val ergebnisArt = vertragErgebnisArt ?: when (art) {
        ZAHL_FUNKTION_KARTEN_ART -> "mathematik.zahl"
        AUSSAGE_FUNKTION_KARTEN_ART -> "mathematik.aussage"
        MENGEN_FUNKTION_KARTEN_ART -> "mathematik.menge"
        SPALTEN_FUNKTION_KARTEN_ART -> "mathematik.vektor.spalte"
        ZEILEN_FUNKTION_KARTEN_ART -> "mathematik.vektor.zeile"
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
        ausgaben = mapOf("wert" to wert),
        zielMengen = mapOf("wert" to zielMenge),
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
        ausgaben = mapOf("wert" to bedingung),
        zielMengen = mapOf("wert" to WahrheitsMenge),
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

        return KnotenAuswertungsErgebnis(mapOf(
            "wert" to symbolischerEingangswert(
                art = ausgangsArt,
                name = name,
                knotenId = kontext.knoten.id,
                methodenErgebnisArt = ergebnisArt,
            ),
        ))
    }
}
