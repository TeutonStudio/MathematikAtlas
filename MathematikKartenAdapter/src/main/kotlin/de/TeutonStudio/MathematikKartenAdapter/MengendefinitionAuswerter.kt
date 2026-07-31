package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val MENGENKONSTRUKTOR_ART = "mathematik.mengenkonstruktor"
const val MENGENDEFINATOR_ART = "mathematik.mengendefinator"
const val MENGENDEFINITION_PAAR = "mengendefinition.paar"
const val MENGENDEFINITION_MENGENNAME = "mengenName"
const val MENGENDEFINITION_ELEMENTNAME = "elementName"
const val MENGENDEFINITION_ELEMENTART = "elementArt"
/** Altparameter aus v2.8.0; wird nur noch beim Laden verborgen und fachlich ignoriert. */
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
        val element = elementAusdruck(elementName, elementArt)
        val fehlendeObermenge = FehlendeObermenge(elementArt.wert)

        return KnotenAuswertungsErgebnis(mapOf(
            "element" to BedingterWert(
                objekt = element,
                werteVorrat = null,
                variablenQuellen = listOf(
                    VariablenQuelle(
                        knotenId = kontext.knoten.id,
                        name = elementName,
                        werteVorrat = fehlendeObermenge,
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
            .distinctBy { Triple(it.name, it.bindungsId, it.gebundeneArt) }
        require(quellen.size == 1) {
            when {
                quellen.isEmpty() -> "Die Aussage verwendet das Element des gekoppelten Mengenkonstruktors nicht."
                else -> "Die Aussage enthält das gekoppelte Mengenelement mehrdeutig."
            }
        }
        val quelle = quellen.single()
        val mengenName = quelle.bindungsName?.trim().orEmpty().ifBlank { "M" }
        val elementArt = quelle.gebundeneArt ?: AnschlussArtId("mathematik.objekt")
        val element = elementAusdruck(quelle.name, elementArt)
        val menge = definierePrädikatsMenge(element, aussage, kontext.rechenKontext)
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

private fun elementAusdruck(name: String, art: AnschlussArtId): FunktionsParameter = when (art.wert) {
    "mathematik.zahl" -> Variable(name)
    "mathematik.aussage" -> AussagenParameter(name)
    "mathematik.menge" -> MengenParameter(name)
    else -> TypisiertesElement(name, art.wert)
}
