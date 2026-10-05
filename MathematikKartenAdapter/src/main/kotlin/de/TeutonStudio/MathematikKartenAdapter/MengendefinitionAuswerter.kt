package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val MENGENKONSTRUKTOR_ART = "mathematik.mengenkonstruktor"
const val MENGENDEFINATOR_ART = "mathematik.mengendefinator"
const val MENGENDEFINITION_PAAR = "mengendefinition.paar"
const val MENGENDEFINITION_MENGENNAME = "mengenName"
const val MENGENDEFINITION_ELEMENTNAME = "elementName"
const val MENGENDEFINITION_ELEMENTART = "elementArt"
const val MENGENDEFINITION_FORMMODUS = "strukturForm.modus"
const val MENGENDEFINITION_FORMEINGABE = "strukturForm.eingabe"
const val MENGENDEFINITION_FORM = "strukturForm.wert"
const val STRUKTURFORM_UNBEKANNT = "unbekannt"
const val STRUKTURFORM_INSPEKTOR = "inspektor"
const val STRUKTURFORM_EINGANG = "eingang"
const val STRUKTURFORM_EINZELN = "einzeln"
const val STRUKTURFORM_TUPEL = "tupel"
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
        val strukturForm = strukturForm(kontext, elementArt)
        val element = elementAusdruck(elementName, elementArt, strukturForm)
        val oberMenge = kontext.eingänge["oberMenge"]?.objekt as? MengenAusdruck
            ?: FehlendeObermenge(elementArt.wert)

        return KnotenAuswertungsErgebnis(mapOf(
            "element" to BedingterWert(
                objekt = element,
                werteVorrat = oberMenge.takeUnless { it is FehlendeObermenge },
                variablenQuellen = listOf(
                    VariablenQuelle(
                        knotenId = kontext.knoten.id,
                        name = elementName,
                        werteVorrat = oberMenge,
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
        val oberMenge = quelle.werteVorrat.takeUnless { it is FehlendeObermenge }
        val menge = definierePrädikatsMenge(
            element = element,
            bedingung = aussage,
            kontext = kontext.rechenKontext,
            oberMenge = oberMenge,
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

private fun elementAusdruck(
    name: String,
    art: AnschlussArtId,
    strukturForm: List<ZahlAusdruck>? = null,
): MethodenParameter = when (art.wert) {
    "mathematik.zahl" -> Variable(name)
    "mathematik.aussage" -> AussagenParameter(name)
    "mathematik.menge" -> MengenParameter(name)
    else -> TypisiertesElement(name, art.wert, strukturForm = strukturForm)
}

private fun strukturForm(kontext: KnotenAuswertungsKontext, art: AnschlussArtId): List<ZahlAusdruck>? {
    if (!art.istFormArt()) return null
    val form = when (kontext.knoten.parameter[MENGENDEFINITION_FORMMODUS] ?: STRUKTURFORM_UNBEKANNT) {
        STRUKTURFORM_INSPEKTOR -> parseStrukturForm(kontext.knoten.parameter[MENGENDEFINITION_FORM])
        STRUKTURFORM_EINGANG -> if (
            kontext.knoten.parameter[MENGENDEFINITION_FORMEINGABE] == STRUKTURFORM_TUPEL
        ) {
            val tupel = kontext.eingänge["dimensionen"]?.objekt as? Tupel
                ?: error("Für die Strukturform muss ein Zahlentupel verbunden sein.")
            tupel.elemente.mapIndexed { index, element ->
                element as? ZahlAusdruck ?: error("Dimension ${index + 1} ist keine Zahl.")
            }
        } else {
            kontext.knoten.anschlüsse
                .filter { it.richtung.name == "Eingang" && it.name.startsWith("dimension.") }
                .sortedBy { it.reihenfolge }
                .map { anschluss -> kontext.eingänge[anschluss.name]?.objekt as? ZahlAusdruck
                    ?: error("Für ${anschluss.name} muss eine Dimension verbunden sein.") }
        }
        else -> return null
    }
    validiereStrukturForm(form, art)
    return form
}

fun parseStrukturForm(text: String?): List<ZahlAusdruck> {
    val teile = text.orEmpty().split(',').map(String::trim)
    require(teile.isNotEmpty() && teile.none(String::isBlank)) {
        "Die Strukturform muss kommaseparierte positive Ganzzahlen oder Variablennamen enthalten."
    }
    return teile.map { teil ->
        teil.toLongOrNull()?.let(RationaleZahl::von)
            ?: teil.takeIf { it.matches(Regex("[A-Za-z][A-Za-z0-9_]*")) }?.let(::Variable)
            ?: error("'$teil' ist keine positive Ganzzahl oder Variable.")
    }.also { form ->
        require(form.all { it is Variable || (it as? RationaleZahl)?.let { zahl ->
            zahl.nenner == java.math.BigInteger.ONE && zahl.zähler.signum() > 0
        } == true }) { "Dimensionen müssen positive ganze Zahlen oder Variablen sein." }
    }
}

private fun validiereStrukturForm(form: List<ZahlAusdruck>, art: AnschlussArtId) {
    require(form.isNotEmpty()) { "Eine bekannte Strukturform benötigt mindestens eine Achse." }
    when (art.wert) {
        "mathematik.tupel", "mathematik.vektor.spalte", "mathematik.vektor.zeile" ->
            require(form.size == 1) { "Tupel und Vektoren besitzen genau eine Achse." }
        "mathematik.matrix" -> require(form.size == 2) { "Matrizen besitzen genau zwei Achsen." }
    }
}

private fun AnschlussArtId.istFormArt(): Boolean = wert in setOf(
    "mathematik.tupel",
    "mathematik.vektor.spalte",
    "mathematik.vektor.zeile",
    "mathematik.matrix",
    "mathematik.tensor",
)
