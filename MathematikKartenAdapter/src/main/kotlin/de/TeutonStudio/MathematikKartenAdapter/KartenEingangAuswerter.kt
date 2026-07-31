package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val KARTEN_EINGANG_ART = "mathematik.kartenEingang"

private val ZAHL_KARTEN_ART = AnschlussArtId("mathematik.zahl")
private val AUSSAGE_KARTEN_ART = AnschlussArtId("mathematik.aussage")
private val MENGE_KARTEN_ART = AnschlussArtId("mathematik.menge")
private val OBJEKT_KARTEN_ART = AnschlussArtId("mathematik.objekt")

/** Erzeugt für öffentliche Karten-Eingänge ein Symbol, das ihrer Anschlussart tatsächlich entspricht. */
internal object KartenEingangAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val name = kontext.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
        val ausgangsArt = kontext.knoten.anschlüsse.firstOrNull {
            it.richtung == AnschlussRichtung.Ausgang
        }?.art ?: OBJEKT_KARTEN_ART

        val parameter: FunktionsParameter = when (ausgangsArt) {
            ZAHL_KARTEN_ART -> Variable(name)
            AUSSAGE_KARTEN_ART -> AussagenParameter(name)
            MENGE_KARTEN_ART -> MengenParameter(name)
            else -> TypisiertesElement(name, ausgangsArt.wert)
        }
        val werteVorrat: MengenAusdruck = when (ausgangsArt) {
            ZAHL_KARTEN_ART -> ReelleZahlen
            AUSSAGE_KARTEN_ART -> EndlicheMenge(
                setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)),
            )
            MENGE_KARTEN_ART -> BenannteMenge("mengen_$name", "\\mathcal{P}(\\mathcal{U})")
            else -> BenannteMenge("werte_$name", "\\mathcal{W}_{${name}}")
        }

        return KnotenAuswertungsErgebnis(mapOf(
            "wert" to BedingterWert(
                objekt = parameter,
                werteVorrat = werteVorrat,
                reelleVariablen = if (parameter is Variable) mapOf(name to werteVorrat) else emptyMap(),
                variablenQuellen = listOf(
                    VariablenQuelle(
                        knotenId = kontext.knoten.id,
                        name = name,
                        werteVorrat = werteVorrat,
                        alsMethodenParameter = false,
                    ),
                ),
            ),
        ))
    }
}
