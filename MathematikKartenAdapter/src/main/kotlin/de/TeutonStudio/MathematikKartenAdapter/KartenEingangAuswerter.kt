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

/**
 * Erzeugt einen symbolischen Eingabewert, dessen Laufzeitobjekt zur deklarierten
 * Anschlussart passt. Karten- und Konzept-Eingänge verwenden damit dieselbe
 * Typisierung statt zweier auseinanderlaufender Sonderwege.
 */
fun symbolischerEingangswert(
    art: AnschlussArtId,
    name: String,
    knotenId: KnotenId,
    aussagenVorschau: Aussage? = null,
): BedingterWert {
    val parameterName = name.trim().ifBlank { "x" }
    symbolischeMethode(art, parameterName)?.let { methode ->
        return BedingterWert(
            objekt = methode,
            latexDarstellung = parameterName,
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

private fun symbolischeMethode(art: AnschlussArtId, name: String): Methode? {
    if (art !in setOf(
            METHODE_KARTEN_ART,
            LEGACY_FUNKTION_KARTEN_ART,
            ZAHL_FUNKTION_KARTEN_ART,
            AUSSAGE_FUNKTION_KARTEN_ART,
            MENGEN_FUNKTION_KARTEN_ART,
        )
    ) return null

    val index = Variable("i")
    val anwendungsLatex = "$name(${index.zuLatex()})"
    val kennung = "${name}_von_${index.name}"
    val (wert, zielMenge) = when (art) {
        ZAHL_FUNKTION_KARTEN_ART -> Variable(kennung) to ReelleZahlen
        AUSSAGE_FUNKTION_KARTEN_ART -> AussagenParameter(kennung, anwendungsLatex) to WahrheitsMenge
        MENGEN_FUNKTION_KARTEN_ART -> MengenParameter(kennung, anwendungsLatex) to BenannteMenge("G_$name", "G")
        else -> TypisiertesElement(kennung, OBJEKT_KARTEN_ART.wert, anwendungsLatex) to
            BenannteMenge("W_$name", "\\mathcal{W}")
    }
    return Methode(
        name = name,
        parameter = listOf(index),
        ausgaben = mapOf("wert" to wert),
        zielMengen = mapOf("wert" to zielMenge),
        werteVorräte = mapOf(index.name to ReelleZahlen),
    )
}

/** Erzeugt für öffentliche Karten-Eingänge ein Symbol, das ihrer Anschlussart tatsächlich entspricht. */
internal object KartenEingangAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val name = kontext.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
        val ausgangsArt = kontext.knoten.anschlüsse.firstOrNull {
            it.richtung == AnschlussRichtung.Ausgang
        }?.art ?: OBJEKT_KARTEN_ART

        return KnotenAuswertungsErgebnis(mapOf(
            "wert" to symbolischerEingangswert(
                art = ausgangsArt,
                name = name,
                knotenId = kontext.knoten.id,
            ),
        ))
    }
}
