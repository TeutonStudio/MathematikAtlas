package de.TeutonStudio.MathematikKnoten

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em

/** Stellt größere mathematische Ausgaben als Display-LaTeX dar. */
@Composable
fun LatexText(
    latex: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val displayLatex = alsDisplayLatex(latex)
    Text(latexZuAnnotiertemText(displayLatex), modifier = modifier, style = style, color = LocalContentColor.current)
}

/** Normalisiert Inline- oder Display-Begrenzer zu genau einem `\[...\]`-Block. */
fun alsDisplayLatex(latex: String): String = "\\[${entferneMatheBegrenzer(latex)}\\]"

fun latexZuAnnotiertemText(latex: String): AnnotatedString = buildAnnotatedString {
    LatexParser(entferneMatheBegrenzer(latex), this).schreibe()
}

/** Kompakte Klartextvariante für Stellen, an denen kein Compose-Text verfügbar ist. */
fun vereinfacheLatexAnzeige(latex: String): String = latexZuAnnotiertemText(latex).text

private fun entferneMatheBegrenzer(latex: String): String {
    var text = latex.trim()
    var geändert: Boolean
    do {
        geändert = false
        text = when {
            text.startsWith("\\[") && text.endsWith("\\]") && text.length >= 4 -> text.substring(2, text.length - 2).trim().also { geändert = true }
            text.startsWith("$$") && text.endsWith("$$") && text.length >= 4 -> text.substring(2, text.length - 2).trim().also { geändert = true }
            text.startsWith('$') && text.endsWith('$') && text.length >= 2 -> text.substring(1, text.length - 1).trim().also { geändert = true }
            else -> text
        }
    } while (geändert)
    return text
}

private class LatexParser(private val quelltext: String, private val ausgabe: AnnotatedString.Builder) {
    private var position = 0
    private var casesTiefe = 0

    fun schreibe(bisGruppenEnde: Boolean = false) {
        while (position < quelltext.length) {
            when (val zeichen = quelltext[position++]) {
                '}' -> if (bisGruppenEnde) return else ausgabe.append(zeichen)
                '{' -> schreibe(bisGruppenEnde = true)
                '^' -> mitStil(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = .78.em)) { schreibeArgument() }
                '_' -> mitStil(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = .78.em)) { schreibeArgument() }
                '\\' -> schreibeBefehl()
                '$' -> Unit
                else -> ausgabe.append(zeichen)
            }
        }
    }

    private fun schreibeArgument() {
        if (position >= quelltext.length) return
        if (quelltext[position] == '{') {
            position++
            schreibe(bisGruppenEnde = true)
        } else {
            val einzelnes = quelltext[position++]
            if (einzelnes == '\\') schreibeBefehl() else ausgabe.append(einzelnes)
        }
    }

    private fun schreibeBefehl() {
        if (position >= quelltext.length) return
        if (!quelltext[position].isLetter()) {
            when (val zeichen = quelltext[position++]) {
                '\\' -> {
                    ausgabe.append(if (casesTiefe > 0) "\n" else ";\n")
                    if (casesTiefe > 0) while (position < quelltext.length && quelltext[position] == ' ') position++
                }
                '{', '}' -> ausgabe.append(zeichen)
                ' ' -> ausgabe.append(";\n")
                '[', ']' -> Unit
                else -> ausgabe.append(zeichen)
            }
            return
        }
        val start = position
        while (position < quelltext.length && quelltext[position].isLetter()) position++
        when (val befehl = quelltext.substring(start, position)) {
            "frac" -> schreibeBruch()
            "mathbb" -> ausgabe.append(zahlbereich(liesGruppenText()))
            "begin" -> when (liesGruppenText()) {
                "pmatrix" -> ausgabe.append('[')
                "cases" -> { casesTiefe++; ausgabe.append("{\n") }
            }
            "end" -> when (liesGruppenText()) {
                "pmatrix" -> ausgabe.append(']')
                "cases" -> { casesTiefe = (casesTiefe - 1).coerceAtLeast(0); ausgabe.append('}') }
            }
            "left", "right", "!", ",", ";", "quad", "qquad", "displaystyle" -> Unit
            "operatorname", "text", "mathrm", "mathbf" -> ausgabe.append(liesGruppenText().replace("\\ ", " "))
            else -> ausgabe.append(zeichenFürBefehl(befehl))
        }
    }

    /** Rendert einen echten typografischen Bruch ohne die bisherigen Klammerpaare. */
    private fun schreibeBruch() {
        val zählerQuelle = liesArgumentQuelltext()
        val nennerQuelle = liesArgumentQuelltext()
        val zähler = latexZuAnnotiertemText(zählerQuelle)
        val nenner = latexZuAnnotiertemText(nennerQuelle)
        val zählerVerschachtelt = "\\frac" in zählerQuelle
        val nennerVerschachtelt = "\\frac" in nennerQuelle
        if (zählerVerschachtelt) ausgabe.append('(')
        mitStil(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = .84.em)) { ausgabe.append(zähler) }
        if (zählerVerschachtelt) ausgabe.append(')')
        ausgabe.append('⁄')
        if (nennerVerschachtelt) ausgabe.append('(')
        mitStil(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = .84.em)) { ausgabe.append(nenner) }
        if (nennerVerschachtelt) ausgabe.append(')')
    }

    private fun liesArgumentQuelltext(): String {
        if (position >= quelltext.length) return ""
        if (quelltext[position] == '{') {
            position++
            val start = position
            var tiefe = 1
            var maskiert = false
            while (position < quelltext.length && tiefe > 0) {
                val zeichen = quelltext[position++]
                when {
                    maskiert -> maskiert = false
                    zeichen == '\\' -> maskiert = true
                    zeichen == '{' -> tiefe++
                    zeichen == '}' -> tiefe--
                }
            }
            return quelltext.substring(start, (position - 1).coerceAtLeast(start))
        }
        if (quelltext[position] == '\\') {
            val start = position++
            while (position < quelltext.length && quelltext[position].isLetter()) position++
            return quelltext.substring(start, position)
        }
        return quelltext[position++].toString()
    }

    private fun liesGruppenText(): String {
        if (position >= quelltext.length || quelltext[position] != '{') return ""
        position++
        val start = position
        var tiefe = 1
        while (position < quelltext.length && tiefe > 0) {
            when (quelltext[position++]) {
                '{' -> tiefe++
                '}' -> tiefe--
            }
        }
        return quelltext.substring(start, (position - 1).coerceAtLeast(start))
    }

    private fun mitStil(stil: SpanStyle, block: () -> Unit) {
        ausgabe.pushStyle(stil)
        block()
        ausgabe.pop()
    }

    private fun zahlbereich(text: String) = when (text) {
        "R" -> "ℝ"
        "Q" -> "ℚ"
        "Z" -> "ℤ"
        "N" -> "ℕ"
        "C" -> "ℂ"
        else -> text
    }

    private fun zeichenFürBefehl(befehl: String) = mapOf(
        "cdot" to "·", "times" to "×", "pi" to "π", "in" to "∈", "cup" to "∪", "cap" to "∩",
        "subseteq" to "⊆", "subset" to "⊂", "supseteq" to "⊇", "supset" to "⊃", "setminus" to "∖", "neq" to "≠", "le" to "≤", "ge" to "≥",
        "varnothing" to "∅", "top" to "wahr", "bot" to "falsch", "neg" to "¬", "land" to "∧", "lor" to "∨",
        "sum" to "∑", "prod" to "∏", "bigcup" to "⋃", "bigcap" to "⋂",
        "forall" to "∀", "exists" to "∃", "rightarrow" to "→", "longrightarrow" to "→", "longto" to "→", "to" to "→", "mapsto" to "↦", "implies" to "⇒", "iff" to "⇔",
        "pm" to "±", "mp" to "∓", "sin" to "sin", "cos" to "cos", "ln" to "ln",
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε", "theta" to "θ",
        "lambda" to "λ", "mu" to "μ", "rho" to "ρ", "sigma" to "σ", "phi" to "φ", "omega" to "ω",
    )[befehl] ?: befehl
}
