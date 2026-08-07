package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.TeutonStudio.MathematikKnoten.mathematikschrift.integral.AtlasIntegralGlyph

private val STANDARD_WAHR_FARBE = Color(0xFF2E7D32)
private val STANDARD_LÜGE_FARBE = Color(0xFFC62828)

/**
 * Stellt den vom Rechenkern erzeugten LaTeX-Teilumfang nativ dar. Es bleibt bewusst
 * ohne WebView und ohne externen TeX-Renderer, unterstützt aber Gruppen, Hoch- und
 * Tiefstellungen, Brüche, Matrizen und die verwendeten mathematischen Befehle.
 * `\\int` wird zentral mit der Atlas-Integralglyphe (Variante 4) dargestellt.
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
    val normalisiert = normalisiereLatexQuelltext(latex)
    val integral = zerlegeIntegralOperator(normalisiert)
    val großerOperator = zerlegeGroßenOperator(normalisiert)

    if (integral != null && (großerOperator == null || integral.position <= großerOperator.position)) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (integral.vorher.isNotBlank()) {
                Text(
                    latexZuAnnotiertemText(integral.vorher, wahrFarbe, lügeFarbe),
                    style = style,
                    color = LocalContentColor.current,
                )
            }
            AtlasIntegralOperator(
                untereAnnotation = integral.untereAnnotation,
                obereAnnotation = integral.obereAnnotation,
                style = style,
            )
            if (integral.nachher.isNotBlank()) {
                LatexText(
                    latex = integral.nachher,
                    style = style,
                )
            }
        }
        return
    }

    if (großerOperator != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (großerOperator.vorher.isNotBlank()) {
                Text(
                    latexZuAnnotiertemText(großerOperator.vorher, wahrFarbe, lügeFarbe),
                    style = style,
                    color = LocalContentColor.current,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    latexZuAnnotiertemText(großerOperator.operator, wahrFarbe, lügeFarbe),
                    style = style,
                    color = LocalContentColor.current,
                )
                LatexText(
                    latex = großerOperator.index,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (großerOperator.nachher.isNotBlank()) {
                LatexText(
                    latex = großerOperator.nachher,
                    style = style,
                )
            }
        }
        return
    }
    Text(
        latexZuAnnotiertemText(normalisiert, wahrFarbe, lügeFarbe),
        modifier = modifier,
        style = style,
        color = LocalContentColor.current,
    )
}

@Composable
private fun AtlasIntegralOperator(
    untereAnnotation: String?,
    obereAnnotation: String?,
    style: TextStyle,
) {
    val schriftgroesse = style.fontSize.value.takeIf { it.isFinite() && it > 0f } ?: 18f
    val glyphHoehe = (schriftgroesse * 2.15f).dp
    val glyphBreite = (schriftgroesse * 1.72f).dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!obereAnnotation.isNullOrBlank()) {
            LatexText(
                latex = obereAnnotation,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        AtlasIntegralGlyph(
            modifier = Modifier
                .width(glyphBreite)
                .height(glyphHoehe),
        )
        if (!untereAnnotation.isNullOrBlank()) {
            LatexText(
                latex = untereAnnotation,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

internal data class IntegralOperatorTeile(
    val position: Int,
    val vorher: String,
    val untereAnnotation: String?,
    val obereAnnotation: String?,
    val nachher: String,
)

private data class LatexArgument(
    val text: String,
    val ende: Int,
)

/** Zerlegt den ersten echten `\\int`-Befehl samt optionalem `_` und `^`. */
internal fun zerlegeIntegralOperator(latex: String): IntegralOperatorTeile? {
    var sucheAb = 0
    var start = -1
    while (sucheAb < latex.length) {
        val kandidat = latex.indexOf("\\int", startIndex = sucheAb)
        if (kandidat < 0) return null
        val danach = kandidat + 4
        if (danach >= latex.length || !latex[danach].isLetter()) {
            start = kandidat
            break
        }
        sucheAb = danach
    }
    if (start < 0) return null

    var position = start + 4
    while (position < latex.length && latex[position].isWhitespace()) position++
    if (latex.startsWith("\\limits", position)) {
        position += "\\limits".length
        while (position < latex.length && latex[position].isWhitespace()) position++
    }

    var unten: String? = null
    var oben: String? = null
    while (position < latex.length) {
        var markerPosition = position
        while (markerPosition < latex.length && latex[markerPosition].isWhitespace()) markerPosition++
        val marker = latex.getOrNull(markerPosition)
        if (marker != '_' && marker != '^') break
        val argument = liesLatexArgument(latex, markerPosition + 1) ?: return null
        if (marker == '_') unten = argument.text else oben = argument.text
        position = argument.ende
    }

    return IntegralOperatorTeile(
        position = start,
        vorher = latex.substring(0, start).trimEnd(),
        untereAnnotation = unten,
        obereAnnotation = oben,
        nachher = latex.substring(position).trimStart(),
    )
}

private fun liesLatexArgument(latex: String, start: Int): LatexArgument? {
    var position = start
    while (position < latex.length && latex[position].isWhitespace()) position++
    if (position >= latex.length) return null

    if (latex[position] == '{') {
        val inhaltStart = position + 1
        position++
        var tiefe = 1
        while (position < latex.length && tiefe > 0) {
            when (latex[position++]) {
                '{' -> tiefe++
                '}' -> tiefe--
            }
        }
        if (tiefe != 0) return null
        return LatexArgument(
            text = latex.substring(inhaltStart, position - 1),
            ende = position,
        )
    }

    if (latex[position] == '\\') {
        val befehlStart = position
        position++
        while (position < latex.length && latex[position].isLetter()) position++
        if (position < latex.length && latex[position] == '{') {
            position++
            var tiefe = 1
            while (position < latex.length && tiefe > 0) {
                when (latex[position++]) {
                    '{' -> tiefe++
                    '}' -> tiefe--
                }
            }
            if (tiefe != 0) return null
        }
        return LatexArgument(latex.substring(befehlStart, position), position)
    }

    return LatexArgument(latex[position].toString(), position + 1)
}

private data class GroßerOperatorTeile(
    val position: Int,
    val vorher: String,
    val operator: String,
    val index: String,
    val nachher: String,
)

private fun zerlegeGroßenOperator(latex: String): GroßerOperatorTeile? {
    val operatoren = listOf(
        "\\mathop{\\stackrel{\\bullet}{\\bigvee}}" to "\\stackrel{\\bullet}{\\bigvee}",
        "\\mathop{\\times}" to "\\times",
        "\\sum" to "\\sum",
        "\\prod" to "\\prod",
        "\\bigcup" to "\\bigcup",
        "\\bigcap" to "\\bigcap",
        "\\bigwedge" to "\\bigwedge",
        "\\bigvee" to "\\bigvee",
    )
    val treffer = operatoren.mapNotNull { (quelle, anzeige) ->
        latex.indexOf(quelle).takeIf { it >= 0 }?.let { Triple(it, quelle, anzeige) }
    }.minByOrNull { it.first } ?: return null
    val limitsStart = treffer.first + treffer.second.length
    val limits = "\\limits_"
    if (!latex.startsWith(limits, limitsStart)) return null
    var position = limitsStart + limits.length
    if (position >= latex.length || latex[position] != '{') return null
    val indexStart = ++position
    var tiefe = 1
    while (position < latex.length && tiefe > 0) {
        when (latex[position++]) {
            '{' -> tiefe++
            '}' -> tiefe--
        }
    }
    if (tiefe != 0) return null
    val indexEnde = position - 1
    return GroßerOperatorTeile(
        position = treffer.first,
        vorher = latex.substring(0, treffer.first).trimEnd(),
        operator = treffer.third,
        index = latex.substring(indexStart, indexEnde),
        nachher = latex.substring(position).trimStart(),
    )
}

fun normalisiereLatexQuelltext(latex: String): String {
    val getrimmt = latex.trim()
    return when {
        getrimmt.length >= 4 && getrimmt.startsWith("$$") && getrimmt.endsWith("$$") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        getrimmt.length >= 4 && getrimmt.startsWith("\\[") && getrimmt.endsWith("\\]") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        getrimmt.length >= 4 && getrimmt.startsWith("\\(") && getrimmt.endsWith("\\)") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        getrimmt.length >= 2 && getrimmt.startsWith('$') && getrimmt.endsWith('$') ->
            getrimmt.substring(1, getrimmt.length - 1).trim()
        else -> getrimmt
    }
}

fun latexZuAnnotiertemText(
    latex: String,
    wahrFarbe: Color = STANDARD_WAHR_FARBE,
    lügeFarbe: Color = STANDARD_LÜGE_FARBE,
): AnnotatedString = buildAnnotatedString {
    LatexParser(normalisiereLatexQuelltext(latex), this, wahrFarbe, lügeFarbe).schreibe()
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
            "mathop", "mathbin", "mathopen", "mathclose" -> schreibeArgument()
            "limits" -> Unit
            "mathbb" -> schreibeDoppelstrich()
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

    private fun schreibeDoppelstrich() {
        while (position < quelltext.length && quelltext[position].isWhitespace()) position++
        val inhalt = when {
            position >= quelltext.length -> ""
            quelltext[position] == '{' -> liesGruppenText()
            else -> quelltext[position++].toString()
        }
        ausgabe.append(zahlbereich(inhalt))
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
            "\\bullet" to "\\lor" -> ausgabe.append("∨̇")
            "\\bullet" to "\\bigvee" -> ausgabe.append("⋁̇")
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
        "N" -> "ℕ"
        "Z" -> "ℤ"
        "Q" -> "ℚ"
        "R" -> "ℝ"
        "C" -> "ℂ"
        "H" -> "ℍ"
        "P" -> "ℙ"
        "F" -> "𝔽"
        "K" -> "𝕂"
        else -> text
    }

    private fun zeichenFürBefehl(befehl: String) = mapOf(
        "cdot" to "·", "times" to "×", "pi" to "π", "in" to "∈", "cup" to "∪", "cap" to "∩", "triangle" to "△",
        "subseteq" to "⊆", "subset" to "⊂", "setminus" to "∖", "ne" to "≠", "neq" to "≠", "le" to "≤", "leq" to "≤", "ge" to "≥", "geq" to "≥",
        "varnothing" to "∅", "vert" to "|", "neg" to "¬", "land" to "∧", "lor" to "∨",
        "sum" to "∑", "prod" to "∏", "bigcup" to "⋃", "bigcap" to "⋂", "bigwedge" to "⋀", "bigvee" to "⋁", "int" to "∫",
        "circ" to "∘", "bullet" to "•", "forall" to "∀", "exists" to "∃", "rightarrow" to "→", "longrightarrow" to "→", "longto" to "→", "to" to "→", "mapsto" to "↦",
        "Rightarrow" to "⇒", "Leftrightarrow" to "⇔", "implies" to "⇒", "iff" to "⇔",
        "pm" to "±", "mp" to "∓", "sin" to "sin", "cos" to "cos", "ln" to "ln",
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε", "theta" to "θ",
        "lambda" to "λ", "mu" to "μ", "rho" to "ρ", "sigma" to "σ", "phi" to "φ", "omega" to "ω",
    )[befehl] ?: befehl
}