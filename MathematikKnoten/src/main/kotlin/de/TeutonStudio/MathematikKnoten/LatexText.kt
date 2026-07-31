package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em

private val STANDARD_WAHR_FARBE = Color(0xFF2E7D32)
private val STANDARD_LÜGE_FARBE = Color(0xFFC62828)

/**
 * Stellt den vom Rechenkern erzeugten LaTeX-Teilumfang nativ dar. Es bleibt bewusst
 * ohne WebView und ohne externen TeX-Renderer, unterstützt aber Gruppen, Hoch- und
 * Tiefstellungen, Brüche, Matrizen und die verwendeten mathematischen Befehle.
 */
@Composable
fun LatexText(
    latex: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val dunklesSchema = isSystemInDarkTheme()
    val wahrFarbe = if (dunklesSchema) Color(0xFF81C784) else Color(0xFF1B5E20)
    val lügeFarbe = if (dunklesSchema) Color(0xFFEF9A9A) else Color(0xFFB71C1C)
    Text(
        latexZuAnnotiertemText(latex, wahrFarbe, lügeFarbe),
        modifier = modifier,
        style = style,
        color = LocalContentColor.current,
    )
}

fun latexZuAnnotiertemText(
    latex: String,
    wahrFarbe: Color = STANDARD_WAHR_FARBE,
    lügeFarbe: Color = STANDARD_LÜGE_FARBE,
): AnnotatedString = buildAnnotatedString {
    LatexParser(latex, this, wahrFarbe, lügeFarbe).schreibe()
}

/** Kompakte Klartextvariante für Stellen, an denen kein Compose-Text verfügbar ist. */
fun vereinfacheLatexAnzeige(latex: String): String = latexZuAnnotiertemText(latex).text

private class LatexParser(
    private val quelltext: String,
    private val ausgabe: AnnotatedString.Builder,
    private val wahrFarbe: Color,
    private val lügeFarbe: Color,
) {
    private var position = 0
    private var casesTiefe = 0

    fun schreibe(bisGruppenEnde: Boolean = false) {
        while (position < quelltext.length) {
            when (val zeichen = quelltext[position++]) {
                '}' -> if (bisGruppenEnde) return else ausgabe.append(zeichen)
                '{' -> schreibe(bisGruppenEnde = true)
                '^' -> mitStil(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.78.em)) { schreibeArgument() }
                '_' -> mitStil(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.78.em)) { schreibeArgument() }
                '\\' -> schreibeBefehl()
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
                // Der Rechenkern verwendet " \\ " als Matrizen-Zeilentrenner.
                ' ' -> ausgabe.append(";\n")
                else -> ausgabe.append(zeichen)
            }
            return
        }
        val start = position
        while (position < quelltext.length && quelltext[position].isLetter()) position++
        when (val befehl = quelltext.substring(start, position)) {
            "frac" -> schreibeBruch()
            "stackrel" -> schreibeStackrel()
            "Set" -> {
                ausgabe.append('{')
                schreibeArgument()
                ausgabe.append('}')
            }
            "mathcal" -> schreibeMathcal()
            "top" -> schreibeWahrheitswert("Wahr", wahrFarbe)
            "bot" -> schreibeWahrheitswert("Lüge", lügeFarbe)
            "mathop", "mathbin" -> schreibeArgument()
            "mathbb" -> ausgabe.append(zahlbereich(liesGruppenText()))
            "begin" -> when (liesGruppenText()) {
                "pmatrix" -> ausgabe.append('[')
                "cases" -> { casesTiefe++; ausgabe.append("{\n") }
            }
            "end" -> when (liesGruppenText()) {
                "pmatrix" -> ausgabe.append(']')
                "cases" -> { casesTiefe = (casesTiefe - 1).coerceAtLeast(0); ausgabe.append('}') }
            }
            "left", "right", "!", ",", ";", "quad", "qquad" -> Unit
            "operatorname", "text", "mathrm", "mathbf" -> ausgabe.append(liesGruppenText().replace("\\ ", " "))
            else -> ausgabe.append(zeichenFürBefehl(befehl))
        }
    }

    private fun schreibeMathcal() {
        val inhalt = liesGruppenText()
        when (inhalt) {
            "Wahr" -> schreibeWahrheitswert(inhalt, wahrFarbe)
            "Lüge" -> schreibeWahrheitswert(inhalt, lügeFarbe)
            else -> mitStil(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)) {
                LatexParser(inhalt, ausgabe, wahrFarbe, lügeFarbe).schreibe()
            }
        }
    }

    private fun schreibeWahrheitswert(text: String, farbe: Color) {
        mitStil(
            SpanStyle(
                color = farbe,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
            ),
        ) {
            ausgabe.append(text)
        }
    }

    private fun schreibeBruch() {
        ausgabe.append('(')
        schreibeArgument()
        ausgabe.append(")⁄(")
        mitStil(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.86.em)) { schreibeArgument() }
        ausgabe.append(')')
    }

    private fun schreibeStackrel() {
        val oben = liesGruppenText()
        val unten = liesGruppenText()
        when (oben to unten) {
            "\\circ" to "\\lor" -> ausgabe.append("∨̊")
            "\\circ" to "\\bigvee" -> ausgabe.append("⋁̊")
            else -> {
                mitStil(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.66.em)) {
                    LatexParser(oben, ausgabe, wahrFarbe, lügeFarbe).schreibe()
                }
                LatexParser(unten, ausgabe, wahrFarbe, lügeFarbe).schreibe()
            }
        }
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
        "subseteq" to "⊆", "subset" to "⊂", "setminus" to "∖", "ne" to "≠", "neq" to "≠", "le" to "≤", "ge" to "≥",
        "varnothing" to "∅", "neg" to "¬", "land" to "∧", "lor" to "∨",
        "sum" to "∑", "prod" to "∏", "bigcup" to "⋃", "bigcap" to "⋂", "bigwedge" to "⋀", "bigvee" to "⋁",
        "circ" to "∘", "forall" to "∀", "exists" to "∃", "rightarrow" to "→", "longrightarrow" to "→", "longto" to "→", "to" to "→", "mapsto" to "↦",
        "Rightarrow" to "⇒", "Leftrightarrow" to "⇔", "implies" to "⇒", "iff" to "⇔",
        "pm" to "±", "mp" to "∓", "sin" to "sin", "cos" to "cos", "ln" to "ln",
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε", "theta" to "θ",
        "lambda" to "λ", "mu" to "μ", "rho" to "ρ", "sigma" to "σ", "phi" to "φ", "omega" to "ω",
    )[befehl] ?: befehl
}
