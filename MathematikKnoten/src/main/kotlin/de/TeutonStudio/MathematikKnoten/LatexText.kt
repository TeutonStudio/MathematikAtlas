package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import de.TeutonStudio.MathematikKnoten.mathematikschrift.integral.AtlasIntegralGlyph

private const val ATLAS_SET_MACRO = "\\newcommand{\\Set}[1]{\\left\\{#1\\right\\}}"
private const val ATLAS_IMPLIES_MACRO = "\\newcommand{\\implies}{\\Rightarrow}"
private const val ATLAS_IFF_MACRO = "\\newcommand{\\iff}{\\Leftrightarrow}"
private const val ATLAS_LONGTO_MACRO = "\\newcommand{\\longto}{\\longrightarrow}"
private const val GROSSES_KARTESISCHES_PRODUKT = "\\mathop{\\Large\\times}"
private val ATLAS_MACROS = listOf(
    ATLAS_SET_MACRO,
    ATLAS_IMPLIES_MACRO,
    ATLAS_IFF_MACRO,
    ATLAS_LONGTO_MACRO,
).joinToString(separator = "")

private val STANDARD_WAHR_FARBE = Color(0xFF2E7D32)
private val STANDARD_LÜGE_FARBE = Color(0xFFC62828)

/**
 * Zentrale LaTeX-Fassade des Atlas.
 *
 * Gewöhnliche mathematische Layoutarbeit wird vollständig an den nativen
 * Compose-Multiplatform-Renderer delegiert. Zwei Renderer-Lücken werden explizit
 * überbrückt: die Atlas-Integralglyphe und das iterierte kartesische Produkt.
 * Historische nicht-bedingte `cases`-Blöcke bleiben ebenfalls kompatibel.
 */
@Composable
fun LatexText(
    latex: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val normalisiert = normalisiereLatexQuelltext(latex)
    val grossesProdukt = normalisiert
        .takeUnless { "\\begin{" in it }
        ?.let(::zerlegeGrossesKartesischesProdukt)

    if (grossesProdukt != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (grossesProdukt.vorher.isNotBlank()) {
                EchterLatexText(grossesProdukt.vorher, style = style)
            }
            AtlasGrossesKartesischesProdukt(
                untereAnnotation = grossesProdukt.untereAnnotation,
                style = style,
            )
            if (grossesProdukt.nachher.isNotBlank()) {
                LatexText(latex = grossesProdukt.nachher, style = style)
            }
        }
        return
    }

    val integral = normalisiert
        .takeUnless { "\\begin{" in it }
        ?.let(::zerlegeIntegralOperator)

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
 * Kanonisiert ausschließlich Schreibweisen, die mathematisch äquivalent sind,
 * aber vom eingebetteten Renderer unterschiedlich gut verstanden werden.
 */
internal fun kanonisiereAtlasLatex(latex: String): String {
    val linkeMengenKlammer = "\u0001"
    val rechteMengenKlammer = "\u0002"
    return latex
        .replace("\\mathbb N_0", "\\mathbb{N}_0")
        .replace("\\mathbb N", "\\mathbb{N}")
        .replace("\\mathbb Z", "\\mathbb{Z}")
        .replace("\\mathbb Q", "\\mathbb{Q}")
        .replace("\\mathbb R", "\\mathbb{R}")
        .replace("\\mathbb C", "\\mathbb{C}")
        .replace("\\mathbb H", "\\mathbb{H}")
        .replace("\\operatorname{Re}", "\\mathcal{Re}")
        .replace("\\operatorname{Im}", "\\mathcal{Im}")
        .replace("\\left\\{", linkeMengenKlammer)
        .replace("\\right\\}", rechteMengenKlammer)
        .replace("\\{", "\\left\\{")
        .replace("\\}", "\\right\\}")
        .replace(linkeMengenKlammer, "\\left\\{")
        .replace(rechteMengenKlammer, "\\right\\}")
}

/**
 * Ergänzt nur Atlas-eigene Kompatibilitätsmakros und die bisherigen farbigen
 * Wahrheitswerte. Mathematisches Layout wird ansonsten nicht mehr umgeschrieben.
 */
internal fun atlasLatexQuelltext(latex: String, dunklesSchema: Boolean): String {
    val wahr = if (dunklesSchema) "#81C784" else "#1B5E20"
    val lüge = if (dunklesSchema) "#EF9A9A" else "#B71C1C"
    val rendererKompatibel = kanonisiereAtlasLatex(ersetzeNichtBedingteCases(latex))
    val farbig = rendererKompatibel
        .replace("\\mathcal{Wahr}", "\\textcolor{$wahr}{\\mathcal{Wahr}}")
        .replace("\\mathcal{Lüge}", "\\textcolor{$lüge}{\\mathcal{Lüge}}")
        .replace("\\top", "\\textcolor{$wahr}{\\mathcal{Wahr}}")
        .replace("\\bot", "\\textcolor{$lüge}{\\mathcal{Lüge}}")
    return ATLAS_MACROS + farbig
}

/**
 * Der verwendete Renderer interpretiert `cases` als echte Fallunterscheidung und
 * ergänzt dabei ein „if“. Historische Methodendarstellungen nutzen dieselbe Umgebung
 * jedoch nur für die große linke Klammer. Solche Blöcke besitzen keine `&`-Spalte
 * und werden deshalb verlustfrei in eine einspaltige Matrix mit linker Klammer
 * übersetzt. Echte Fallunterscheidungen mit Bedingungsspalte bleiben unangetastet.
 */
internal fun ersetzeNichtBedingteCases(latex: String): String {
    val startMarke = "\\begin{cases}"
    val endeMarke = "\\end{cases}"
    var text = latex
    var sucheAb = 0

    while (sucheAb < text.length) {
        val start = text.indexOf(startMarke, sucheAb)
        if (start < 0) break
        val inhaltStart = start + startMarke.length
        val ende = text.indexOf(endeMarke, inhaltStart)
        if (ende < 0) break
        val inhalt = text.substring(inhaltStart, ende)
        if ('&' !in inhalt) {
            val ersatz = "\\left\\{\\begin{matrix}$inhalt\\end{matrix}\\right."
            text = text.substring(0, start) + ersatz + text.substring(ende + endeMarke.length)
            sucheAb = start + ersatz.length
        } else {
            sucheAb = ende + endeMarke.length
        }
    }
    return text
}

@Composable
private fun AtlasGrossesKartesischesProdukt(
    untereAnnotation: String?,
    style: TextStyle,
) {
    val schriftgroesse = style.fontSize.value.takeIf { it.isFinite() && it > 0f } ?: 18f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "×",
            style = style.copy(fontSize = (schriftgroesse * 1.55f).sp),
        )
        if (!untereAnnotation.isNullOrBlank()) {
            EchterLatexText(
                latex = untereAnnotation,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

internal data class GrossesKartesischesProduktTeile(
    val position: Int,
    val vorher: String,
    val untereAnnotation: String?,
    val nachher: String,
)

/** Zerlegt exakt den Atlas-Operator `\\mathop{\\Large\\times}\\limits_{...}`. */
internal fun zerlegeGrossesKartesischesProdukt(latex: String): GrossesKartesischesProduktTeile? {
    val start = latex.indexOf(GROSSES_KARTESISCHES_PRODUKT)
    if (start < 0) return null
    var position = start + GROSSES_KARTESISCHES_PRODUKT.length
    while (position < latex.length && latex[position].isWhitespace()) position++
    if (latex.startsWith("\\limits", position)) {
        position += "\\limits".length
        while (position < latex.length && latex[position].isWhitespace()) position++
    }
    if (latex.getOrNull(position) != '_') return null
    val argument = liesLatexArgument(latex, position + 1) ?: return null
    return GrossesKartesischesProduktTeile(
        position = start,
        vorher = latex.substring(0, start).trimEnd(),
        untereAnnotation = argument.text,
        nachher = latex.substring(argument.ende).trimStart(),
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

fun latexZuAnnotiertemText(
    latex: String,
    wahrFarbe: Color = STANDARD_WAHR_FARBE,
    lügeFarbe: Color = STANDARD_LÜGE_FARBE,
): AnnotatedString = buildAnnotatedString {
    LatexParser(normalisiereLatexQuelltext(latex), this, wahrFarbe, lügeFarbe).schreibe()
}

/** Kompakte Klartextvariante für Stellen, an denen kein Compose-Text verfügbar ist. */
fun vereinfacheLatexAnzeige(latex: String): String = latexZuAnnotiertemText(latex).text

/**
 * Leichtgewichtiger Parser ausschließlich für Text-/Diagnosepfade, die einen
 * [AnnotatedString] statt eines echten Formel-Layouts benötigen. Die visuelle
 * Darstellung läuft weiterhin vollständig über [LatexText].
 */
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
                "cases" -> {
                    casesTiefe++
                    ausgabe.append("{\n")
                }
            }
            "end" -> when (liesGruppenText()) {
                "pmatrix" -> ausgabe.append(']')
                "cases" -> {
                    casesTiefe = (casesTiefe - 1).coerceAtLeast(0)
                    ausgabe.append('}')
                }
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
        "cdot" to "·", "times" to "×", "div" to "÷", "pi" to "π", "dots" to "…", "ldots" to "…", "in" to "∈", "cup" to "∪", "cap" to "∩", "triangle" to "△",
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
