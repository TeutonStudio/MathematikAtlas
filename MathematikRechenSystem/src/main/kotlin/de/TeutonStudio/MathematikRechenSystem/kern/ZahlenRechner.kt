package de.TeutonStudio.MathematikRechenSystem.kern

/** Einziger erzeugbarer numerischer Rechenknoten. */
enum class ZahlenRechnerOperator(val stabileId: String) {
    ADDITION("zahl.addition"),
    SUBTRAKTION("zahl.subtraktion"),
    MULTIPLIKATION("zahl.multiplikation"),
    DIVISION("zahl.division"),
    POTENZ("zahl.potenz"),
    WURZEL("zahl.wurzel"),
    BETRAG("zahl.betrag"),
    MINIMUM("zahl.minimum"),
    MAXIMUM("zahl.maximum"),
    LOGARITHMUS("zahl.logarithmus"),
    NATUERLICHER_LOGARITHMUS("zahl.ln"),
    EXPONENTIALFUNKTION("zahl.exp"),
    SINUS("zahl.sin"),
    COSINUS("zahl.cos"),
    ARCSINUS("zahl.arcsin"),
    ARCCOSINUS("zahl.arccos"),
    LIMES_HYPERREELL_ZU_REELL("zahl.limes"),
}

data class ZahlenArgumentRolle(
    val id: String,
    val name: String,
    val mindestens: Int = 1,
    val hoechstens: Int = 1,
) {
    init {
        require(id.isNotBlank())
        require(mindestens >= 0)
        require(hoechstens >= mindestens)
    }
}

data class ZahlenOperatorSpezifikation(
    val operator: ZahlenRechnerOperator,
    val rollen: List<ZahlenArgumentRolle>,
    val kommutativ: Boolean,
    val assoziativ: Boolean,
    val benoetigtGeordnetenBereich: Boolean = false,
    val definitionsIds: Map<FundamentalerZahlbereich, String>,
)

data class ZahlenRechnerEingabe(
    val rollenId: String,
    val ausdruck: ZahlAusdruck,
    val bereich: FundamentalerZahlbereich,
)

sealed interface ZahlenRechnerErgebnis {
    data class Wert(
        val ausdruck: ZahlAusdruck,
        val bereich: FundamentalerZahlbereich,
        val definitionsId: String,
        val einbettungen: List<String> = emptyList(),
        val bedingungen: List<Aussage> = emptyList(),
    ) : ZahlenRechnerErgebnis

    data class Bedingt(
        val ausdruck: ZahlAusdruck,
        val moeglicheBereiche: Set<FundamentalerZahlbereich>,
        val definitionsIds: Set<String>,
        val bedingungen: List<Aussage>,
    ) : ZahlenRechnerErgebnis

    data class Luecke(val luecke: DefinitionsLuecke) : ZahlenRechnerErgebnis

    data class Ungueltig(
        val code: String,
        val nachricht: String,
        val rollenIds: List<String> = emptyList(),
    ) : ZahlenRechnerErgebnis
}

object ZahlenRechnerKatalog {
    const val KNOTEN_ART = "mathematik.zahlenrechner"

    private fun definitionen(prefix: String): Map<FundamentalerZahlbereich, String> =
        FundamentalerZahlbereich.entries.associateWith { bereich -> "$prefix|${bereich.id}" }

    val spezifikationen: Map<ZahlenRechnerOperator, ZahlenOperatorSpezifikation> = listOf(
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.ADDITION,
            listOf(ZahlenArgumentRolle("summand", "Summand", 2, Int.MAX_VALUE)),
            kommutativ = true,
            assoziativ = true,
            definitionsIds = definitionen("addition"),
        ),
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.SUBTRAKTION,
            listOf(
                ZahlenArgumentRolle("minuend", "Minuend"),
                ZahlenArgumentRolle("subtrahend", "Subtrahend"),
            ),
            kommutativ = false,
            assoziativ = false,
            definitionsIds = definitionen("subtraktion"),
        ),
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.MULTIPLIKATION,
            listOf(ZahlenArgumentRolle("faktor", "Faktor", 2, Int.MAX_VALUE)),
            kommutativ = false,
            assoziativ = true,
            definitionsIds = definitionen("multiplikation"),
        ),
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.DIVISION,
            listOf(
                ZahlenArgumentRolle("zaehler", "Zähler"),
                ZahlenArgumentRolle("nenner", "Nenner"),
            ),
            kommutativ = false,
            assoziativ = false,
            definitionsIds = definitionen("division"),
        ),
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.POTENZ,
            listOf(
                ZahlenArgumentRolle("basis", "Basis"),
                ZahlenArgumentRolle("exponent", "Exponent"),
            ),
            kommutativ = false,
            assoziativ = false,
            definitionsIds = definitionen("potenz"),
        ),
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.WURZEL,
            listOf(ZahlenArgumentRolle("radikand", "Radikand")),
            kommutativ = false,
            assoziativ = false,
            definitionsIds = definitionen("wurzel"),
        ),
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.BETRAG,
            listOf(ZahlenArgumentRolle("argument", "Argument")),
            kommutativ = false,
            assoziativ = false,
            definitionsIds = definitionen("betrag"),
        ),
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.MINIMUM,
            listOf(ZahlenArgumentRolle("operand", "Operand", 2, Int.MAX_VALUE)),
            kommutativ = true,
            assoziativ = true,
            benoetigtGeordnetenBereich = true,
            definitionsIds = definitionen("minimum"),
        ),
        ZahlenOperatorSpezifikation(
            ZahlenRechnerOperator.MAXIMUM,
            listOf(ZahlenArgumentRolle("operand", "Operand", 2, Int.MAX_VALUE)),
            kommutativ = true,
            assoziativ = true,
            benoetigtGeordnetenBereich = true,
            definitionsIds = definitionen("maximum"),
        ),
    ).associateBy { it.operator } + listOf(
        ZahlenRechnerOperator.LOGARITHMUS,
        ZahlenRechnerOperator.NATUERLICHER_LOGARITHMUS,
        ZahlenRechnerOperator.EXPONENTIALFUNKTION,
        ZahlenRechnerOperator.SINUS,
        ZahlenRechnerOperator.COSINUS,
        ZahlenRechnerOperator.ARCSINUS,
        ZahlenRechnerOperator.ARCCOSINUS,
        ZahlenRechnerOperator.LIMES_HYPERREELL_ZU_REELL,
    ).associateWith { operator ->
        val rollen = if (operator == ZahlenRechnerOperator.LOGARITHMUS) {
            listOf(ZahlenArgumentRolle("basis", "Basis"), ZahlenArgumentRolle("argument", "Argument"))
        } else {
            listOf(ZahlenArgumentRolle("argument", "Argument"))
        }
        ZahlenOperatorSpezifikation(
            operator,
            rollen,
            kommutativ = false,
            assoziativ = false,
            definitionsIds = definitionen(operator.stabileId),
        )
    }

    fun spezifikation(operator: ZahlenRechnerOperator): ZahlenOperatorSpezifikation =
        requireNotNull(spezifikationen[operator])
}

object ZahlenRechner {
    fun erzeuge(
        operator: ZahlenRechnerOperator,
        eingaben: List<ZahlenRechnerEingabe>,
    ): ZahlenRechnerErgebnis {
        val spezifikation = ZahlenRechnerKatalog.spezifikation(operator)
        pruefeRollen(spezifikation, eingaben)?.let { return it }

        val gemeinsamerBereich = FundamentaleZahlbereiche.kleinsterGemeinsamerBereich(
            eingaben.map { it.bereich },
        )
        if (spezifikation.benoetigtGeordnetenBereich && !gemeinsamerBereich.istGeordnet) {
            return ZahlenRechnerErgebnis.Ungueltig(
                "bereich_nicht_geordnet",
                "${operator.name} benötigt einen geordneten Zahlbereich; ${gemeinsamerBereich.latex} ist nicht geordnet.",
                eingaben.map { it.rollenId },
            )
        }

        val ausdruck = when (operator) {
            ZahlenRechnerOperator.ADDITION -> addition(eingaben.map { it.ausdruck })
            ZahlenRechnerOperator.SUBTRAKTION -> subtraktion(
                eingaben.einzeln("minuend").ausdruck,
                eingaben.einzeln("subtrahend").ausdruck,
            )
            ZahlenRechnerOperator.MULTIPLIKATION -> multiplikation(eingaben.map { it.ausdruck })
            ZahlenRechnerOperator.DIVISION -> {
                val zaehler = eingaben.einzeln("zaehler").ausdruck
                val nenner = eingaben.einzeln("nenner").ausdruck
                if (nenner == RationaleZahl.Null) {
                    return luecke(operator, eingaben, "division_durch_null", "Division durch null ist nicht definiert.")
                }
                Division(zaehler, nenner)
            }
            ZahlenRechnerOperator.POTENZ -> Potenz(
                eingaben.einzeln("basis").ausdruck,
                eingaben.einzeln("exponent").ausdruck,
            )
            ZahlenRechnerOperator.WURZEL -> Wurzel(eingaben.einzeln("radikand").ausdruck)
            ZahlenRechnerOperator.BETRAG -> Betrag(eingaben.einzeln("argument").ausdruck)
            ZahlenRechnerOperator.MINIMUM -> minimum(eingaben.map { it.ausdruck })
            ZahlenRechnerOperator.MAXIMUM -> maximum(eingaben.map { it.ausdruck })
            ZahlenRechnerOperator.LOGARITHMUS -> Logarithmus(
                eingaben.einzeln("basis").ausdruck,
                eingaben.einzeln("argument").ausdruck,
            )
            ZahlenRechnerOperator.NATUERLICHER_LOGARITHMUS ->
                NatürlicherLogarithmus(eingaben.einzeln("argument").ausdruck)
            ZahlenRechnerOperator.EXPONENTIALFUNKTION ->
                Exponentialfunktion(eingaben.einzeln("argument").ausdruck)
            ZahlenRechnerOperator.SINUS -> Sinus(eingaben.einzeln("argument").ausdruck)
            ZahlenRechnerOperator.COSINUS -> Cosinus(eingaben.einzeln("argument").ausdruck)
            ZahlenRechnerOperator.ARCSINUS -> ArcSinus(eingaben.einzeln("argument").ausdruck)
            ZahlenRechnerOperator.ARCCOSINUS -> ArcCosinus(eingaben.einzeln("argument").ausdruck)
            ZahlenRechnerOperator.LIMES_HYPERREELL_ZU_REELL -> return ZahlenRechnerErgebnis.Bedingt(
                ausdruck = eingaben.einzeln("argument").ausdruck,
                moeglicheBereiche = setOf(FundamentalerZahlbereich.REELL),
                definitionsIds = setOf("zahl.limes|R"),
                bedingungen = emptyList(),
            )
        }

        val ergebnisBereich = inferiereErgebnisBereich(operator, eingaben, gemeinsamerBereich)
        val definition = requireNotNull(spezifikation.definitionsIds[ergebnisBereich])
        val bedingungen = definitionsBedingungen(operator, eingaben)
        return ZahlenRechnerErgebnis.Wert(
            ausdruck = ausdruck,
            bereich = ergebnisBereich,
            definitionsId = definition,
            einbettungen = eingaben
                .filter { it.bereich != ergebnisBereich }
                .map { "${it.bereich.id}→${ergebnisBereich.id}" },
            bedingungen = bedingungen,
        )
    }

    private fun pruefeRollen(
        spezifikation: ZahlenOperatorSpezifikation,
        eingaben: List<ZahlenRechnerEingabe>,
    ): ZahlenRechnerErgebnis.Ungueltig? {
        val gruppiert = eingaben.groupBy { it.rollenId }
        spezifikation.rollen.forEach { rolle ->
            val anzahl = gruppiert[rolle.id].orEmpty().size
            if (anzahl !in rolle.mindestens..rolle.hoechstens) {
                return ZahlenRechnerErgebnis.Ungueltig(
                    "argumentanzahl",
                    "Rolle ${rolle.name} erwartet ${rolle.mindestens}..${rolle.hoechstens} Argumente, erhalten: $anzahl.",
                    listOf(rolle.id),
                )
            }
        }
        val unbekannt = gruppiert.keys - spezifikation.rollen.map { it.id }.toSet()
        return if (unbekannt.isEmpty()) null else ZahlenRechnerErgebnis.Ungueltig(
            "unbekannte_rolle",
            "Unbekannte Argumentrollen: ${unbekannt.sorted().joinToString()}.",
            unbekannt.sorted(),
        )
    }

    private fun inferiereErgebnisBereich(
        operator: ZahlenRechnerOperator,
        eingaben: List<ZahlenRechnerEingabe>,
        gemeinsam: FundamentalerZahlbereich,
    ): FundamentalerZahlbereich = when (operator) {
        ZahlenRechnerOperator.SUBTRAKTION -> {
            val links = eingaben.einzeln("minuend").ausdruck as? RationaleZahl
            val rechts = eingaben.einzeln("subtrahend").ausdruck as? RationaleZahl
            if (links != null && rechts != null) bereichFuerRational(links - rechts)
            else if (gemeinsam == FundamentalerZahlbereich.NATUERLICH_POSITIV) {
                FundamentalerZahlbereich.GANZ
            } else gemeinsam
        }
        ZahlenRechnerOperator.DIVISION -> if (
            FundamentaleZahlbereiche.istTeilbereich(gemeinsam, FundamentalerZahlbereich.RATIONAL)
        ) FundamentalerZahlbereich.RATIONAL else gemeinsam
        ZahlenRechnerOperator.WURZEL -> {
            val radikand = eingaben.einzeln("radikand").ausdruck as? RationaleZahl
            if (radikand != null && radikand < RationaleZahl.Null) FundamentalerZahlbereich.KOMPLEX
            else maxBereich(gemeinsam, FundamentalerZahlbereich.REELL)
        }
        ZahlenRechnerOperator.BETRAG -> if (gemeinsam == FundamentalerZahlbereich.KOMPLEX ||
            gemeinsam == FundamentalerZahlbereich.QUATERNION
        ) FundamentalerZahlbereich.REELL else gemeinsam
        ZahlenRechnerOperator.LOGARITHMUS,
        ZahlenRechnerOperator.NATUERLICHER_LOGARITHMUS,
        ZahlenRechnerOperator.EXPONENTIALFUNKTION,
        ZahlenRechnerOperator.SINUS,
        ZahlenRechnerOperator.COSINUS,
        ZahlenRechnerOperator.ARCSINUS,
        ZahlenRechnerOperator.ARCCOSINUS,
        ZahlenRechnerOperator.LIMES_HYPERREELL_ZU_REELL,
        -> maxBereich(gemeinsam, FundamentalerZahlbereich.REELL)
        else -> gemeinsam
    }

    private fun definitionsBedingungen(
        operator: ZahlenRechnerOperator,
        eingaben: List<ZahlenRechnerEingabe>,
    ): List<Aussage> = when (operator) {
        ZahlenRechnerOperator.DIVISION -> listOf(
            Ungleichheit(eingaben.einzeln("nenner").ausdruck, RationaleZahl.Null),
        )
        ZahlenRechnerOperator.LOGARITHMUS -> listOf(
            Vergleich(eingaben.einzeln("basis").ausdruck, VergleichsArt.Größer, RationaleZahl.Null),
            Ungleichheit(eingaben.einzeln("basis").ausdruck, RationaleZahl.Eins),
            Vergleich(eingaben.einzeln("argument").ausdruck, VergleichsArt.Größer, RationaleZahl.Null),
        )
        ZahlenRechnerOperator.NATUERLICHER_LOGARITHMUS -> listOf(
            Vergleich(eingaben.einzeln("argument").ausdruck, VergleichsArt.Größer, RationaleZahl.Null),
        )
        ZahlenRechnerOperator.ARCSINUS,
        ZahlenRechnerOperator.ARCCOSINUS,
        -> listOf(inverseTrigonometrischeDefinitionsBedingung(eingaben.einzeln("argument").ausdruck))
        else -> emptyList()
    }

    private fun luecke(
        operator: ZahlenRechnerOperator,
        eingaben: List<ZahlenRechnerEingabe>,
        code: String,
        nachricht: String,
    ) = ZahlenRechnerErgebnis.Luecke(
        DefinitionsLuecke(
            operatorId = operator.stabileId,
            ursache = StrukturierterAuswertungsGrund(code, nachricht),
            eingabenLatex = eingaben.map { it.ausdruck.zuLatex() },
            erwarteterTyp = "Zahl",
            fehlgeschlageneDefinitionen = FundamentalerZahlbereich.entries.map { bereich ->
                "${operator.stabileId}|${bereich.id}"
            },
        ),
    )
}

object ZahlenRechnerMigration {
    val alteKnotenArten: Map<String, ZahlenRechnerOperator> = mapOf(
        "mathematik.addition" to ZahlenRechnerOperator.ADDITION,
        "mathematik.multiplikation" to ZahlenRechnerOperator.MULTIPLIKATION,
        "mathematik.division" to ZahlenRechnerOperator.DIVISION,
        "mathematik.potenz" to ZahlenRechnerOperator.POTENZ,
        "mathematik.wurzel" to ZahlenRechnerOperator.WURZEL,
        "mathematik.logarithmus" to ZahlenRechnerOperator.LOGARITHMUS,
        "mathematik.extremwert.maximum" to ZahlenRechnerOperator.MAXIMUM,
        "mathematik.extremwert.minimum" to ZahlenRechnerOperator.MINIMUM,
    )
}

private fun List<ZahlenRechnerEingabe>.einzeln(rolle: String): ZahlenRechnerEingabe =
    singleOrNull { it.rollenId == rolle }
        ?: error("Für die Rolle '$rolle' wird genau eine Eingabe benötigt.")

private fun bereichFuerRational(wert: RationaleZahl): FundamentalerZahlbereich = when {
    wert.nenner != java.math.BigInteger.ONE -> FundamentalerZahlbereich.RATIONAL
    wert > RationaleZahl.Null -> FundamentalerZahlbereich.NATUERLICH_POSITIV
    wert == RationaleZahl.Null -> FundamentalerZahlbereich.NATUERLICH_MIT_NULL
    else -> FundamentalerZahlbereich.GANZ
}

private fun maxBereich(
    links: FundamentalerZahlbereich,
    rechts: FundamentalerZahlbereich,
): FundamentalerZahlbereich = FundamentaleZahlbereiche.kleinsterGemeinsamerBereich(listOf(links, rechts))
