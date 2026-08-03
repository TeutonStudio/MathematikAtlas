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
    val standard: List<FormelTastaturTaste> = listOf(
        operator("plus", "+", FormelTastenKategorie.GRUNDRECHNUNG, "zahl.addition", "a", "b"),
        operator("minus", "−", FormelTastenKategorie.GRUNDRECHNUNG, "zahl.subtraktion", "a", "b"),
        operator("mal", "×", FormelTastenKategorie.GRUNDRECHNUNG, "zahl.multiplikation", "a", "b"),
        operator("geteilt", "÷", FormelTastenKategorie.GRUNDRECHNUNG, "zahl.division", "zaehler", "nenner"),
        operator("potenz", "xʸ", FormelTastenKategorie.POTENZEN, "zahl.potenz", "basis", "exponent"),
        operator("wurzel", "√", FormelTastenKategorie.POTENZEN, "zahl.wurzel", "radikand"),
        operator("betrag", "|x|", FormelTastenKategorie.FUNKTIONEN, "zahl.betrag", "argument"),
        operator("ln", "ln", FormelTastenKategorie.FUNKTIONEN, "zahl.ln", "argument"),
        operator("log", "logₐ", FormelTastenKategorie.FUNKTIONEN, "zahl.logarithmus", "basis", "argument"),
        operator("exp", "exp", FormelTastenKategorie.FUNKTIONEN, "zahl.exp", "argument"),
        operator("sin", "sin", FormelTastenKategorie.TRIGONOMETRIE, "zahl.sin", "argument"),
        operator("cos", "cos", FormelTastenKategorie.TRIGONOMETRIE, "zahl.cos", "argument"),
        operator("tan", "tan", FormelTastenKategorie.TRIGONOMETRIE, "zahl.tan", "argument"),
        operator("cot", "cot", FormelTastenKategorie.TRIGONOMETRIE, "zahl.cot", "argument"),
        operator("sec", "sec", FormelTastenKategorie.TRIGONOMETRIE, "zahl.sec", "argument"),
        operator("csc", "csc", FormelTastenKategorie.TRIGONOMETRIE, "zahl.csc", "argument"),
        operator("arcsin", "arcsin", FormelTastenKategorie.TRIGONOMETRIE, "zahl.arcsin", "argument"),
        operator("arccos", "arccos", FormelTastenKategorie.TRIGONOMETRIE, "zahl.arccos", "argument"),
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

