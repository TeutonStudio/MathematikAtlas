package de.TeutonStudio.MathematikKnoten

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/** Entfernt ausschließlich vollständige äußere Display-Begrenzer. */
internal fun entferneLatexDisplayBegrenzer(latex: String): String {
    val getrimmt = latex.trim()
    return when {
        getrimmt.length >= 4 && getrimmt.startsWith("$$") && getrimmt.endsWith("$$") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        getrimmt.length >= 4 && getrimmt.startsWith("\\[") && getrimmt.endsWith("\\]") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        else -> getrimmt
    }
}

/**
 * Display-Formeln verwenden denselben nativen LaTeX-Renderer wie Inline-Formeln.
 * Matrizen, Fallunterscheidungen, Brüche und skalierende Delimiter werden nicht
 * länger durch Compose-Sonderlayouts nachgebaut.
 */
@Composable
fun LatexFormel(
    latex: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    LatexText(
        latex = entferneLatexDisplayBegrenzer(latex),
        modifier = modifier,
        style = style,
    )
}
