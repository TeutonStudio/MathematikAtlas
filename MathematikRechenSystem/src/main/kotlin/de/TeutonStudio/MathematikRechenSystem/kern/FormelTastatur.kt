package de.TeutonStudio.MathematikRechenSystem.kern

enum class FormelTastenKategorie {
    GRUNDRECHNUNG,
    POTENZEN,
    FUNKTIONEN,
    TRIGONOMETRIE,
    HYPERBOLISCH,
    KONSTANTEN,
}

data class FormelTastaturTaste(
    val id: String,
    val beschriftung: String,
    val kategorie: FormelTastenKategorie,
    val operatorId: String? = null,
    val argumentRollen: List<String> = emptyList(),
    val literal: MathematischesObjekt? = null,
) {
    init {
        require(id.isNotBlank())
        require(operatorId != null || literal != null) {
            "Eine Formeltaste benötigt einen Operator oder ein Literal."
        }
    }
}

/** UI-neutrale Tastenbelegung. Tasten erzeugen Ausdrucksobjekte, niemals Roh-LaTeX. */
object FormelTastatur {
    /**
     * Der Standardkatalog wird aus dem stabilen Operatorregister des universellen
     * Zahlenrechners erzeugt. Neue Standardoperatoren können dadurch nicht mehr
     * unbemerkt im Rechner existieren, aber im CAS-Formelbauer fehlen.
     */
    val standard: List<FormelTastaturTaste> =
        UniversellerZahlenOperator.entries.map(::standardTaste) +
            listOf(
                operator("tan", "tan", FormelTastenKategorie.TRIGONOMETRIE, "zahl.tan", "argument"),
                operator("cot", "cot", FormelTastenKategorie.TRIGONOMETRIE, "zahl.cot", "argument"),
                operator("sec", "sec", FormelTastenKategorie.TRIGONOMETRIE, "zahl.sec", "argument"),
                operator("csc", "csc", FormelTastenKategorie.TRIGONOMETRIE, "zahl.csc", "argument"),
                operator("arctan", "arctan", FormelTastenKategorie.TRIGONOMETRIE, "zahl.arctan", "argument"),
                operator("sinh", "sinh", FormelTastenKategorie.HYPERBOLISCH, "zahl.sinh", "argument"),
                operator("cosh", "cosh", FormelTastenKategorie.HYPERBOLISCH, "zahl.cosh", "argument"),
                operator("tanh", "tanh", FormelTastenKategorie.HYPERBOLISCH, "zahl.tanh", "argument"),
                operator("coth", "coth", FormelTastenKategorie.HYPERBOLISCH, "zahl.coth", "argument"),
                operator("sech", "sech", FormelTastenKategorie.HYPERBOLISCH, "zahl.sech", "argument"),
                operator("csch", "csch", FormelTastenKategorie.HYPERBOLISCH, "zahl.csch", "argument"),
                FormelTastaturTaste("pi", "π", FormelTastenKategorie.KONSTANTEN, literal = Pi),
                FormelTastaturTaste("e", "e", FormelTastenKategorie.KONSTANTEN, literal = EulerscheZahl),
            )

    private fun standardTaste(operator: UniversellerZahlenOperator): FormelTastaturTaste =
        FormelTastaturTaste(
            id = operator.stabileId.substringAfterLast('.'),
            beschriftung = operator.tastenBeschriftung(),
            kategorie = operator.tastenKategorie(),
            operatorId = operator.stabileId,
            argumentRollen = operator.formelRollen(),
        )

    private fun UniversellerZahlenOperator.tastenKategorie(): FormelTastenKategorie = when (this) {
        UniversellerZahlenOperator.ADDITION,
        UniversellerZahlenOperator.SUBTRAKTION,
        UniversellerZahlenOperator.MULTIPLIKATION,
        UniversellerZahlenOperator.DIVISION,
        UniversellerZahlenOperator.MODULO,
        UniversellerZahlenOperator.MINIMUM,
        UniversellerZahlenOperator.MAXIMUM,
        -> FormelTastenKategorie.GRUNDRECHNUNG

        UniversellerZahlenOperator.KEHRWERT,
        UniversellerZahlenOperator.POTENZ,
        UniversellerZahlenOperator.QUADRAT,
        UniversellerZahlenOperator.KUBIK,
        UniversellerZahlenOperator.WURZEL,
        UniversellerZahlenOperator.QUADRATWURZEL,
        UniversellerZahlenOperator.KUBIKWURZEL,
        -> FormelTastenKategorie.POTENZEN

        UniversellerZahlenOperator.SINUS,
        UniversellerZahlenOperator.COSINUS,
        UniversellerZahlenOperator.ARCSINUS,
        UniversellerZahlenOperator.ARCCOSINUS,
        -> FormelTastenKategorie.TRIGONOMETRIE

        else -> FormelTastenKategorie.FUNKTIONEN
    }

    private fun UniversellerZahlenOperator.formelRollen(): List<String> = when (this) {
        UniversellerZahlenOperator.ADDITION,
        UniversellerZahlenOperator.SUBTRAKTION,
        UniversellerZahlenOperator.MULTIPLIKATION,
        UniversellerZahlenOperator.MINIMUM,
        UniversellerZahlenOperator.MAXIMUM,
        -> listOf("a", "b")

        UniversellerZahlenOperator.DIVISION -> listOf("zaehler", "nenner")
        UniversellerZahlenOperator.POTENZ -> listOf("basis", "exponent")
        UniversellerZahlenOperator.WURZEL -> listOf("radikand", "grad")
        UniversellerZahlenOperator.LOGARITHMUS -> listOf("basis", "argument")
        UniversellerZahlenOperator.ITERIERTE_SUMME,
        UniversellerZahlenOperator.ITERIERTES_PRODUKT,
        -> listOf("methode", "indexmenge")

        UniversellerZahlenOperator.KOMPLEX_AUS_POLAR -> listOf("radius", "winkel")
        UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH -> listOf("realteil", "imaginaerteil")
        UniversellerZahlenOperator.MODULO -> listOf("dividend", "modul")
        else -> listOf("argument")
    }

    private fun UniversellerZahlenOperator.tastenBeschriftung(): String = when (this) {
        UniversellerZahlenOperator.ADDITION -> "+"
        UniversellerZahlenOperator.SUBTRAKTION -> "−"
        UniversellerZahlenOperator.MULTIPLIKATION -> "×"
        UniversellerZahlenOperator.DIVISION -> "÷"
        UniversellerZahlenOperator.KEHRWERT -> "x⁻¹"
        UniversellerZahlenOperator.POTENZ -> "xʸ"
        UniversellerZahlenOperator.QUADRAT -> "x²"
        UniversellerZahlenOperator.KUBIK -> "x³"
        UniversellerZahlenOperator.WURZEL -> "ⁿ√x"
        UniversellerZahlenOperator.QUADRATWURZEL -> "√x"
        UniversellerZahlenOperator.KUBIKWURZEL -> "∛x"
        UniversellerZahlenOperator.LOGARITHMUS -> "logₐ"
        UniversellerZahlenOperator.LOGARITHMUS_BASIS_2 -> "lb"
        UniversellerZahlenOperator.NATUERLICHER_LOGARITHMUS -> "ln"
        UniversellerZahlenOperator.LOGARITHMUS_BASIS_10 -> "log"
        UniversellerZahlenOperator.ITERIERTE_SUMME -> "Σ"
        UniversellerZahlenOperator.ITERIERTES_PRODUKT -> "Π"
        UniversellerZahlenOperator.INTEGRAL -> "∫"
        UniversellerZahlenOperator.DIFFERENTIAL -> "d/dx"
        UniversellerZahlenOperator.MINIMUM -> "min"
        UniversellerZahlenOperator.MAXIMUM -> "max"
        UniversellerZahlenOperator.NORM -> "‖x‖"
        UniversellerZahlenOperator.ABRUNDUNG -> "⌊x⌋"
        UniversellerZahlenOperator.AUFRUNDUNG -> "⌈x⌉"
        UniversellerZahlenOperator.RUNDUNG -> "⌊x⌉"
        UniversellerZahlenOperator.KONJUGIERTE -> "x̄"
        UniversellerZahlenOperator.REALTEIL -> "Re"
        UniversellerZahlenOperator.IMAGINAERTEIL -> "Im"
        UniversellerZahlenOperator.KOMPLEXER_WINKEL -> "arg"
        UniversellerZahlenOperator.KOMPLEXER_RADIUS -> "|z|"
        UniversellerZahlenOperator.KOMPLEX_AUS_POLAR -> "reⁱφ"
        UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH -> "a+bi"
        UniversellerZahlenOperator.MODULO -> "mod"
        UniversellerZahlenOperator.BETRAG -> "|x|"
        UniversellerZahlenOperator.EXPONENTIALFUNKTION -> "exp"
        UniversellerZahlenOperator.SINUS -> "sin"
        UniversellerZahlenOperator.COSINUS -> "cos"
        UniversellerZahlenOperator.ARCSINUS -> "arcsin"
        UniversellerZahlenOperator.ARCCOSINUS -> "arccos"
        UniversellerZahlenOperator.LIMES_HYPERREELL_ZU_REELL -> "lim"
    }

    private fun operator(
        id: String,
        beschriftung: String,
        kategorie: FormelTastenKategorie,
        operatorId: String,
        vararg rollen: String,
    ) = FormelTastaturTaste(
        id = id,
        beschriftung = beschriftung,
        kategorie = kategorie,
        operatorId = operatorId,
        argumentRollen = rollen.toList(),
    )
}
