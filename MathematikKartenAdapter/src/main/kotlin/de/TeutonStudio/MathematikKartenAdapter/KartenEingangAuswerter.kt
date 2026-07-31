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
    val parameter: FunktionsParameter = when (art) {
        ZAHL_KARTEN_ART -> Variable(parameterName)
        AUSSAGE_KARTEN_ART -> aussagenVorschau as? FunktionsParameter ?: AussagenParameter(parameterName)
        MENGE_KARTEN_ART -> MengenParameter(parameterName)
        else -> TypisiertesElement(parameterName, art.wert)
    }
    val werteVorrat: MengenAusdruck = when (art) {
        ZAHL_KARTEN_ART -> ReelleZahlen
        AUSSAGE_KARTEN_ART -> EndlicheMenge(
            setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)),
        )
        MENGE_KARTEN_ART -> BenannteMenge("mengen_$parameterName", "\\mathcal{P}(\\mathcal{U})")
        else -> BenannteMenge("werte_$parameterName", "\\mathcal{W}_{${parameterName}}")
    }

    return BedingterWert(
        objekt = parameter,
        werteVorrat = werteVorrat,
        reelleVariablen = if (parameter is Variable) mapOf(parameterName to werteVorrat) else emptyMap(),
        variablenQuellen = listOf(
            VariablenQuelle(
                knotenId = knotenId,
                name = parameterName,
                werteVorrat = werteVorrat,
                alsMethodenParameter = false,
            ),
        ),
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
