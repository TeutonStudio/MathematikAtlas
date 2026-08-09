package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import de.TeutonStudio.MathematikKnoten.mathematikschrift.integral.AtlasIntegralGlyph

private const val ATLAS_SET_MACRO = "\\newcommand{\\Set}[1]{\\left\\{#1\\right\\}}"
private const val ATLAS_IMPLIES_MACRO = "\\newcommand{\\implies}{\\Rightarrow}"
private const val ATLAS_IFF_MACRO = "\\newcommand{\\iff}{\\Leftrightarrow}"
private const val ATLAS_LONGTO_MACRO = "\\newcommand{\\longto}{\\longrightarrow}"
private val ATLAS_MACROS = listOf(
    ATLAS_SET_MACRO,
    ATLAS_IMPLIES_MACRO,
    ATLAS_IFF_MACRO,
    ATLAS_LONGTO_MACRO,
).joinToString(separator = "")

/**
 * Zentrale LaTeX-Fassade des Atlas.
 *
 * Gewöhnliche mathematische Layoutarbeit wird vollständig an den nativen
 * Compose-Multiplatform-Renderer delegiert. Der Atlas interpretiert insbesondere
 * keine Brüche, Matrizen, cases-Umgebungen, skalierenden Klammern oder großen
 * Operatoren mehr selbst.
 *
 * Einzige bewusste Darstellungs-Sonderregel bleibt die Atlas-Integralglyphe. Sie
 * ersetzt ausschließlich das erste echte `\\int`; Grenzen und Restformel werden
 * wiederum durch denselben LaTeX-Renderer dargestellt.
 */
@Composable
fun LatexText(
    latex: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val normalisiert = normalisiereLatexQuelltext(latex)
    val integral = zerlegeIntegralOperator(normalisiert)

    if (integral != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (integral.vorher.isNotBlank()) {
                EchterLatexText(integral.vorher, style = style)
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

    EchterLatexText(
        latex = normalisiert,
        modifier = modifier,
        style = style,
    )
}

@Composable
private fun EchterLatexText(
    latex: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
) {
    val dunklesSchema = isSystemInDarkTheme()
    val fontSize = style.fontSize.value
        .takeIf { it.isFinite() && it > 0f }
        ?.sp
        ?: 18.sp
    Latex(
        latex = atlasLatexQuelltext(latex, dunklesSchema),
        modifier = modifier,
        config = LatexConfig(
            fontSize = fontSize,
            accessibilityEnabled = true,
        ),
        isDarkTheme = dunklesSchema,
    )
}

/**
 * Ergänzt nur Atlas-eigene Kompatibilitätsmakros und die bisherigen farbigen
 * Wahrheitswerte. Mathematisches Layout wird ausdrücklich nicht mehr umgeschrieben.
 */
internal fun atlasLatexQuelltext(latex: String, dunklesSchema: Boolean): String {
    val wahr = if (dunklesSchema) "#81C784" else "#1B5E20"
    val lüge = if (dunklesSchema) "#EF9A9A" else "#B71C1C"
    val farbig = latex
        .replace("\\mathcal{Wahr}", "\\textcolor{$wahr}{\\mathcal{Wahr}}")
        .replace("\\mathcal{Lüge}", "\\textcolor{$lüge}{\\mathcal{Lüge}}")
        .replace("\\top", "\\textcolor{$wahr}{\\mathcal{Wahr}}")
        .replace("\\bot", "\\textcolor{$lüge}{\\mathcal{Lüge}}")
    return ATLAS_MACROS + farbig
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
            EchterLatexText(
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
            EchterLatexText(
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

/** Entfernt ausschließlich vollständige äußere Formelbegrenzer. */
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

/**
 * Kompatible Klartextdarstellung für nicht-visuelle Diagnosepfade. Sie ist kein
 * Formelrenderer; visuelle Oberflächen müssen [LatexText] oder [LatexFormel] verwenden.
 */
fun vereinfacheLatexAnzeige(latex: String): String {
    var text = normalisiereLatexQuelltext(latex)
    val befehle = linkedMapOf(
        "\\longrightarrow" to "→",
        "\\rightarrow" to "→",
        "\\mapsto" to "↦",
        "\\Leftrightarrow" to "⇔",
        "\\Rightarrow" to "⇒",
        "\\subseteq" to "⊆",
        "\\subset" to "⊂",
        "\\setminus" to "∖",
        "\\neq" to "≠",
        "\\ne" to "≠",
        "\\leq" to "≤",
        "\\geq" to "≥",
        "\\in" to "∈",
        "\\cup" to "∪",
        "\\cap" to "∩",
        "\\cdot" to "·",
        "\\times" to "×",
        "\\div" to "÷",
        "\\sum" to "∑",
        "\\prod" to "∏",
        "\\bigcup" to "⋃",
        "\\bigcap" to "⋂",
        "\\bigwedge" to "⋀",
        "\\bigvee" to "⋁",
        "\\int" to "∫",
        "\\varnothing" to "∅",
        "\\dots" to "…",
        "\\ldots" to "…",
        "\\circ" to "∘",
        "\\forall" to "∀",
        "\\exists" to "∃",
    )
    befehle.forEach { (quelle, ziel) -> text = text.replace(quelle, ziel) }
    text = text
        .replace("\\begin{cases}", "")
        .replace("\\end{cases}", "")
        .replace("\\begin{pmatrix}", "[")
        .replace("\\end{pmatrix}", "]")
        .replace("\\limits", "")
        .replace("\\left", "")
        .replace("\\right", "")
        .replace("\\mathopen", "")
        .replace("\\mathclose", "")
        .replace("\\mathrm", "")
        .replace("\\mathbf", "")
        .replace("\\operatorname", "")
        .replace("\\text", "")
        .replace("\\mathcal", "")
        .replace("\\mathbb{N}", "ℕ")
        .replace("\\mathbb{Z}", "ℤ")
        .replace("\\mathbb{Q}", "ℚ")
        .replace("\\mathbb{R}", "ℝ")
        .replace("\\mathbb{C}", "ℂ")
        .replace("\\mathbb{H}", "ℍ")
        .replace("\\\\", "\n")
        .replace(Regex("[{}]"), "")
    return text
}

fun latexZuAnnotiertemText(
    latex: String,
    wahrFarbe: Color = Color(0xFF2E7D32),
    lügeFarbe: Color = Color(0xFFC62828),
): AnnotatedString {
    @Suppress("UNUSED_VARIABLE") val farben = wahrFarbe to lügeFarbe
    return AnnotatedString(vereinfacheLatexAnzeige(latex))
}
