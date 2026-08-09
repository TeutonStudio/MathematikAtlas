package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

/**
 * Vollständige, argumenttragende Vorschau für den Rechner-Operatorauswahldialog.
 * `symbolLatex` bleibt bewusst die kompakte Glyphe für Inspector, Definitionen und Ergebnisnamen.
 */
val UniversellerZahlenOperator.vorschauLatex: String
    get() = when (this) {
        UniversellerZahlenOperator.ADDITION -> "(\\dots)+(\\dots)"
        UniversellerZahlenOperator.SUBTRAKTION -> "(\\dots)-(\\dots)"
        UniversellerZahlenOperator.MULTIPLIKATION -> "(\\dots)\\cdot(\\dots)"
        UniversellerZahlenOperator.DIVISION -> "(\\dots)\\div(\\dots)"
        UniversellerZahlenOperator.KEHRWERT -> "(\\dots)^{-1}"
        UniversellerZahlenOperator.POTENZ -> "(\\dots)^{p}"
        UniversellerZahlenOperator.QUADRAT -> "(\\dots)^2"
        UniversellerZahlenOperator.KUBIK -> "(\\dots)^3"
        UniversellerZahlenOperator.WURZEL -> "\\sqrt[p]{(\\dots)}"
        UniversellerZahlenOperator.QUADRATWURZEL -> "\\sqrt{(\\dots)}"
        UniversellerZahlenOperator.KUBIKWURZEL -> "\\sqrt[3]{(\\dots)}"
        UniversellerZahlenOperator.LOGARITHMUS -> "\\log_{b}(\\dots)"
        UniversellerZahlenOperator.LOGARITHMUS_BASIS_2 -> "\\operatorname{lb}(\\dots)"
        UniversellerZahlenOperator.NATUERLICHER_LOGARITHMUS -> "\\ln(\\dots)"
        UniversellerZahlenOperator.LOGARITHMUS_BASIS_10 -> "\\log(\\dots)"
        UniversellerZahlenOperator.ITERIERTE_SUMME -> "\\sum\\limits_{idx\\in\\dots}(\\dots)(idx)"
        UniversellerZahlenOperator.ITERIERTES_PRODUKT -> "\\prod\\limits_{idx\\in\\dots}(\\dots)(idx)"
        UniversellerZahlenOperator.INTEGRAL -> "\\int\\limits_{idx\\in\\dots}(\\dots)(idx)\\,\\mathrm d idx"
        UniversellerZahlenOperator.DIFFERENTIAL -> "\\frac{\\mathrm{d}}{\\mathrm{d}x}(\\dots)"
        UniversellerZahlenOperator.MINIMUM -> "\\min(\\dots,\\dots)"
        UniversellerZahlenOperator.MAXIMUM -> "\\max(\\dots,\\dots)"
        UniversellerZahlenOperator.ABRUNDUNG -> "\\lfloor\\dots\\rfloor"
        UniversellerZahlenOperator.AUFRUNDUNG -> "\\lceil\\dots\\rceil"
        UniversellerZahlenOperator.RUNDUNG -> "\\lfloor\\dots\\rceil"
        UniversellerZahlenOperator.KONJUGIERTE -> "\\overline{(\\dots)}"
        UniversellerZahlenOperator.REALTEIL -> "\\operatorname{Re}(\\dots)"
        UniversellerZahlenOperator.IMAGINAERTEIL -> "\\operatorname{Im}(\\dots)"
        UniversellerZahlenOperator.KOMPLEXER_WINKEL -> "\\arg(\\dots)"
        UniversellerZahlenOperator.KOMPLEXER_RADIUS -> "|\\dots|"
        UniversellerZahlenOperator.KOMPLEX_AUS_POLAR -> "(r,\\varphi)\\mapsto r e^{i\\varphi}"
        UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH -> "(a,b)\\mapsto a+b i"
        UniversellerZahlenOperator.MODULO -> "(\\dots)\\bmod(\\dots)"
        UniversellerZahlenOperator.BETRAG -> "|\\dots|"
        UniversellerZahlenOperator.EXPONENTIALFUNKTION -> "\\exp(\\dots)"
        UniversellerZahlenOperator.SINUS -> "\\sin(\\dots)"
        UniversellerZahlenOperator.COSINUS -> "\\cos(\\dots)"
        UniversellerZahlenOperator.ARCSINUS -> "\\arcsin(\\dots)"
        UniversellerZahlenOperator.ARCCOSINUS -> "\\arccos(\\dots)"
        UniversellerZahlenOperator.LIMES_HYPERREELL_ZU_REELL -> "\\lim\\limits_{idx\\to\\dots}(\\dots)(idx)"
    }

val ErweiterterZahlenOperator.vorschauLatex: String
    get() = when (this) {
        ErweiterterZahlenOperator.TANGENS -> "\\tan(\\dots)"
        ErweiterterZahlenOperator.COTANGENS -> "\\cot(\\dots)"
        ErweiterterZahlenOperator.SEKANS -> "\\sec(\\dots)"
        ErweiterterZahlenOperator.KOSEKANS -> "\\csc(\\dots)"
        ErweiterterZahlenOperator.ARCTANGENS -> "\\arctan(\\dots)"
        ErweiterterZahlenOperator.SINUS_HYPERBOLICUS -> "\\sinh(\\dots)"
        ErweiterterZahlenOperator.COSINUS_HYPERBOLICUS -> "\\cosh(\\dots)"
        ErweiterterZahlenOperator.TANGENS_HYPERBOLICUS -> "\\tanh(\\dots)"
        ErweiterterZahlenOperator.COTANGENS_HYPERBOLICUS -> "\\coth(\\dots)"
        ErweiterterZahlenOperator.SEKANS_HYPERBOLICUS -> "\\operatorname{sech}(\\dots)"
        ErweiterterZahlenOperator.KOSEKANS_HYPERBOLICUS -> "\\operatorname{csch}(\\dots)"
        ErweiterterZahlenOperator.POLYNOM -> "(c_i)_i\\cdot\\vec{x}"
    }
