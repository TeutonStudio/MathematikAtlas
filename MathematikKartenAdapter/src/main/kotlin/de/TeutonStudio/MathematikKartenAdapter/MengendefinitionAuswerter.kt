package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val MENGENKONSTRUKTOR_ART = "mathematik.mengenkonstruktor"
const val MENGENDEFINATOR_ART = "mathematik.mengendefinator"
const val MENGENDEFINITION_PAAR = "mengendefinition.paar"
const val MENGENDEFINITION_MENGENNAME = "mengenName"
const val MENGENDEFINITION_ELEMENTNAME = "elementName"
const val MENGENDEFINITION_ELEMENTART = "elementArt"
const val MENGENDEFINITION_ELEMENTMENGE = "elementMenge"

internal object MengenkonstruktorAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val paarId = kontext.knoten.parameter[MENGENDEFINITION_PAAR]
            ?.trim()?.takeIf(String::isNotEmpty)
            ?: error("Der Mengenkonstruktor gehört zu keinem Mengendefinitionspaar.")
        val mengenName = kontext.knoten.parameter[MENGENDEFINITION_MENGENNAME]
            ?.trim().orEmpty().ifBlank { "M" }
        val elementName = kontext.knoten.parameter[MENGENDEFINITION_ELEMENTNAME]
            ?.trim().orEmpty().ifBlank { "x" }
        val elementArt = AnschlussArtId(
            kontext.knoten.parameter[MENGENDEFINITION_ELEMENTART]
                ?.trim().orEmpty().ifBlank { "mathematik.zahl" },
        )
        val grundMenge = elementGrundMenge(
            elementArt,
            kontext.knoten.parameter[MENGENDEFINITION_ELEMENTMENGE].orEmpty(),
        )
        val element = elementAusdruck(elementName, elementArt)
        val reelleVariable = if (
            elementArt.wert == "mathematik.zahl" &&
            grundMenge in setOf(NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen)
        ) mapOf(elementName to grundMenge) else emptyMap()

        return KnotenAuswertungsErgebnis(mapOf(
            "element" to BedingterWert(
                objekt = element,
                werteVorrat = grundMenge,
                reelleVariablen = reelleVariable,
                variablenQuellen = listOf(
                    VariablenQuelle(
                        knotenId = kontext.knoten.id,
                        name = elementName,
                        werteVorrat = grundMenge,
                        alsMethodenParameter = false,
                        bindungsId = paarId,
                        bindungsName = mengenName,
                        gebundeneArt = elementArt,
                    ),
                ),
            ),
        ))
    }
}

internal object MengendefinatorAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val paarId = kontext.knoten.parameter[MENGENDEFINITION_PAAR]
            ?.trim()?.takeIf(String::isNotEmpty)
            ?: error("Der Mengendefinator gehört zu keinem Mengendefinitionspaar.")
        val aussageWert = kontext.eingänge["aussage"]
            ?: error("Eine Aussage muss mit dem Mengendefinator verbunden sein.")
        val aussage = aussageWert.objekt as? Aussage
            ?: error("Der Mengendefinator akzeptiert ausschließlich Aussagen.")
        val quellen = aussageWert.variablenQuellen
            .filter { it.bindungsId == paarId }
            .distinctBy { Triple(it.name, it.werteVorrat, it.gebundeneArt) }
        require(quellen.size == 1) {
            when {
                quellen.isEmpty() -> "Die Aussage verwendet das Element des gekoppelten Mengenkonstruktors nicht."
                else -> "Die Aussage enthält das gekoppelte Mengenelement mehrdeutig."
            }
        }
        val quelle = quellen.single()
        val mengenName = quelle.bindungsName?.trim().orEmpty().ifBlank { "M" }
        val menge = DefinierteMenge(
            variablen = listOf(GebundeneMengenVariable(Variable(quelle.name), quelle.werteVorrat)),
            bedingung = aussage,
        )
        return KnotenAuswertungsErgebnis(mapOf(
            "menge" to BedingterWert(
                objekt = menge,
                annahmen = aussageWert.annahmen,
                reelleVariablen = aussageWert.reelleVariablen,
                variablenQuellen = aussageWert.variablenQuellen.filterNot { it.bindungsId == paarId },
                latexDarstellung = "$mengenName=${menge.zuLatex()}",
            ),
        ))
    }
}

private fun elementAusdruck(name: String, art: AnschlussArtId): MathematischesObjekt = when (art.wert) {
    "mathematik.zahl" -> Variable(name)
    "mathematik.aussage" -> Gleichheit(Variable(name), WahrheitsKonstante(true))
    else -> error("Unbekannter Mengenelementtyp '${art.wert}'.")
}

private fun elementGrundMenge(art: AnschlussArtId, text: String): MengenAusdruck {
    if (art.wert == "mathematik.aussage") {
        return EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))
    }
    val name = text.trim().ifBlank { if (art.wert == "mathematik.zahl") "R" else "U" }
    return when (name.uppercase()) {
        "N", "ℕ" -> NatürlicheZahlen
        "Z", "ℤ" -> GanzeZahlen
        "Q", "ℚ" -> RationaleZahlen
        "R", "ℝ" -> ReelleZahlen
        "C", "ℂ" -> KomplexeZahlen
        else -> BenannteMenge(name)
    }
}
