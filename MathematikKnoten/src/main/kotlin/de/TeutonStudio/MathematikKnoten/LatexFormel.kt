package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

private const val PMATRIX_BEGINN = "\\begin{pmatrix}"
private const val PMATRIX_ENDE = "\\end{pmatrix}"

internal data class LatexMatrixFormel(
    val vorher: String,
    val zeilen: List<List<String>>,
    val nachher: String,
)

internal sealed interface LatexMatrixAnalyse {
    data object KeineMatrix : LatexMatrixAnalyse
    data class Erfolg(val formel: LatexMatrixFormel) : LatexMatrixAnalyse
    data class Fehler(val diagnose: String, val fallback: String) : LatexMatrixAnalyse
}

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

internal fun analysiereLatexMatrix(latex: String): LatexMatrixAnalyse {
    val bereinigt = entferneLatexDisplayBegrenzer(latex)
    val beginn = bereinigt.indexOf(PMATRIX_BEGINN)
    if (beginn < 0) return LatexMatrixAnalyse.KeineMatrix
    val inhaltStart = beginn + PMATRIX_BEGINN.length
    val ende = findePassendesPmatrixEnde(bereinigt, inhaltStart)
    if (ende < 0) {
        return LatexMatrixAnalyse.Fehler(
            diagnose = "Die Matrix besitzt kein \\end{pmatrix}.",
            fallback = bereinigt,
        )
    }
    val inhalt = bereinigt.substring(inhaltStart, ende)
    val zerlegt = zerlegeLatexMatrixInhalt(inhalt)
    if (zerlegt.fehler != null) {
        return LatexMatrixAnalyse.Fehler(zerlegt.fehler, bereinigt)
    }
    val zeilen = zerlegt.zeilen
    if (zeilen.isEmpty() || zeilen.all { zeile -> zeile.all(String::isBlank) }) {
        return LatexMatrixAnalyse.Fehler("Die Matrix ist leer.", bereinigt)
    }
    val spalten = zeilen.first().size
    if (spalten == 0 || zeilen.any { it.size != spalten }) {
        val längen = zeilen.joinToString(", ") { it.size.toString() }
        return LatexMatrixAnalyse.Fehler(
            diagnose = "Matrixzeilen besitzen unterschiedliche Spaltenzahlen: $längen.",
            fallback = bereinigt,
        )
    }
    val nachherStart = ende + PMATRIX_ENDE.length
    if (bereinigt.indexOf(PMATRIX_BEGINN, startIndex = nachherStart) >= 0) {
        return LatexMatrixAnalyse.Fehler(
            diagnose = "Mehrere Matrixumfelder in einer Display-Formel werden noch nicht unterstützt.",
            fallback = bereinigt,
        )
    }
    return LatexMatrixAnalyse.Erfolg(
        LatexMatrixFormel(
            vorher = bereinigt.substring(0, beginn).trimEnd(),
            zeilen = zeilen,
            nachher = bereinigt.substring(nachherStart).trimStart(),
        ),
    )
}

/** Findet das Ende des äußeren pmatrix-Umfelds, nicht das erste Ende einer inneren Matrix. */
private fun findePassendesPmatrixEnde(text: String, inhaltStart: Int): Int {
    var tiefe = 1
    var position = inhaltStart
    while (position < text.length) {
        val nächsterBeginn = text.indexOf(PMATRIX_BEGINN, startIndex = position)
        val nächstesEnde = text.indexOf(PMATRIX_ENDE, startIndex = position)
        if (nächstesEnde < 0) return -1
        if (nächsterBeginn >= 0 && nächsterBeginn < nächstesEnde) {
            tiefe++
            position = nächsterBeginn + PMATRIX_BEGINN.length
        } else {
            tiefe--
            if (tiefe == 0) return nächstesEnde
            position = nächstesEnde + PMATRIX_ENDE.length
        }
    }
    return -1
}

private data class MatrixZerlegung(
    val zeilen: List<List<String>>,
    val fehler: String? = null,
)

private fun zerlegeLatexMatrixInhalt(inhalt: String): MatrixZerlegung {
    val zeilen = mutableListOf<MutableList<String>>()
    var zeile = mutableListOf<String>()
    val zelle = StringBuilder()
    var gruppenTiefe = 0
    var matrixTiefe = 0
    var position = 0

    fun zelleAbschließen() {
        zeile += zelle.toString().trim()
        zelle.clear()
    }

    fun zeileAbschließen() {
        zelleAbschließen()
        zeilen += zeile
        zeile = mutableListOf()
    }

    while (position < inhalt.length) {
        val zeichen = inhalt[position]
        when {
            inhalt.startsWith(PMATRIX_BEGINN, position) -> {
                matrixTiefe++
                zelle.append(PMATRIX_BEGINN)
                position += PMATRIX_BEGINN.length
            }
            inhalt.startsWith(PMATRIX_ENDE, position) -> {
                if (matrixTiefe == 0) {
                    return MatrixZerlegung(emptyList(), "Unerwartetes \\end{pmatrix} innerhalb einer Matrixzelle.")
                }
                matrixTiefe--
                zelle.append(PMATRIX_ENDE)
                position += PMATRIX_ENDE.length
            }
            zeichen == '{' -> {
                gruppenTiefe++
                zelle.append(zeichen)
                position++
            }
            zeichen == '}' -> {
                if (gruppenTiefe == 0) return MatrixZerlegung(emptyList(), "Unerwartete schließende Gruppe in der Matrix.")
                gruppenTiefe--
                zelle.append(zeichen)
                position++
            }
            zeichen == '&' && gruppenTiefe == 0 && matrixTiefe == 0 -> {
                zelleAbschließen()
                position++
            }
            zeichen == '\\' &&
                position + 1 < inhalt.length &&
                inhalt[position + 1] == '\\' &&
                gruppenTiefe == 0 &&
                matrixTiefe == 0 -> {
                zeileAbschließen()
                position += 2
                while (position < inhalt.length && inhalt[position].isWhitespace()) position++
            }
            else -> {
                zelle.append(zeichen)
                position++
            }
        }
    }
    if (gruppenTiefe != 0) return MatrixZerlegung(emptyList(), "Eine Gruppe innerhalb der Matrix ist nicht geschlossen.")
    if (matrixTiefe != 0) return MatrixZerlegung(emptyList(), "Eine verschachtelte Matrix ist nicht geschlossen.")
    zeileAbschließen()
    return MatrixZerlegung(zeilen)
}

/**
 * Display-Renderer für mehrzeilige mathematische Strukturen.
 * Gewöhnliche Formeln und `cases` werden weiterhin an den einzeiligen Teilparser delegiert;
 * `pmatrix` erhält dagegen ein echtes Raster aus getrennten, rekursiv renderbaren Zellen.
 */
@Composable
fun LatexFormel(
    latex: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    when (val analyse = remember(latex) { analysiereLatexMatrix(latex) }) {
        LatexMatrixAnalyse.KeineMatrix -> LatexText(
            latex = entferneLatexDisplayBegrenzer(latex),
            modifier = modifier,
            style = style,
        )
        is LatexMatrixAnalyse.Fehler -> Column(modifier) {
            LatexText(analyse.fallback, style = style)
            Text(
                analyse.diagnose,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        is LatexMatrixAnalyse.Erfolg -> LatexMatrixDarstellung(
            formel = analyse.formel,
            modifier = modifier,
            style = style,
        )
    }
}

@Composable
private fun LatexMatrixDarstellung(
    formel: LatexMatrixFormel,
    modifier: Modifier,
    style: TextStyle,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (formel.vorher.isNotBlank()) LatexText(formel.vorher, style = style)
        MatrixKlammer(links = true, zeilenAnzahl = formel.zeilen.size, style = style)
        LatexMatrixRaster(formel.zeilen, style)
        MatrixKlammer(links = false, zeilenAnzahl = formel.zeilen.size, style = style)
        if (formel.nachher.isNotBlank()) LatexText(formel.nachher, style = style)
    }
}

@Composable
private fun MatrixKlammer(links: Boolean, zeilenAnzahl: Int, style: TextStyle) {
    if (zeilenAnzahl <= 1) {
        Text(if (links) "(" else ")", style = style, color = LocalContentColor.current)
        return
    }
    val oben = if (links) "⎛" else "⎞"
    val mitte = if (links) "⎜" else "⎟"
    val unten = if (links) "⎝" else "⎠"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(oben, style = style, color = LocalContentColor.current)
        repeat((zeilenAnzahl - 2).coerceAtLeast(0)) {
            Text(mitte, style = style, color = LocalContentColor.current)
        }
        Text(unten, style = style, color = LocalContentColor.current)
    }
}

@Composable
private fun LatexMatrixRaster(zeilen: List<List<String>>, style: TextStyle) {
    val spaltenAnzahl = zeilen.first().size
    val horizontalerAbstand = 12.dp
    val vertikalerAbstand = 5.dp
    Layout(
        content = {
            zeilen.forEach { zeile ->
                zeile.forEach { zelle ->
                    Box(Modifier.padding(horizontal = 1.dp)) {
                        LatexFormel(zelle, style = style)
                    }
                }
            }
        },
    ) { messbare, beschränkungen ->
        val zellenBeschränkungen = Constraints(
            minWidth = 0,
            maxWidth = beschränkungen.maxWidth,
            minHeight = 0,
            maxHeight = beschränkungen.maxHeight,
        )
        val platzierbare = messbare.map { it.measure(zellenBeschränkungen) }
        val spaltenBreiten = IntArray(spaltenAnzahl)
        val zeilenHöhen = IntArray(zeilen.size)
        platzierbare.forEachIndexed { index, platzierbar ->
            val zeilenIndex = index / spaltenAnzahl
            val spaltenIndex = index % spaltenAnzahl
            spaltenBreiten[spaltenIndex] = maxOf(spaltenBreiten[spaltenIndex], platzierbar.width)
            zeilenHöhen[zeilenIndex] = maxOf(zeilenHöhen[zeilenIndex], platzierbar.height)
        }
        val hAbstand = horizontalerAbstand.roundToPx()
        val vAbstand = vertikalerAbstand.roundToPx()
        val breite = spaltenBreiten.sum() + hAbstand * (spaltenAnzahl - 1).coerceAtLeast(0)
        val höhe = zeilenHöhen.sum() + vAbstand * (zeilen.size - 1).coerceAtLeast(0)
        layout(
            width = breite.coerceIn(beschränkungen.minWidth, beschränkungen.maxWidth),
            height = höhe.coerceIn(beschränkungen.minHeight, beschränkungen.maxHeight),
        ) {
            val xPositionen = IntArray(spaltenAnzahl)
            var x = 0
            spaltenBreiten.indices.forEach { spalte ->
                xPositionen[spalte] = x
                x += spaltenBreiten[spalte] + hAbstand
            }
            val yPositionen = IntArray(zeilen.size)
            var y = 0
            zeilenHöhen.indices.forEach { zeilenIndex ->
                yPositionen[zeilenIndex] = y
                y += zeilenHöhen[zeilenIndex] + vAbstand
            }
            platzierbare.forEachIndexed { index, platzierbar ->
                val zeilenIndex = index / spaltenAnzahl
                val spaltenIndex = index % spaltenAnzahl
                val zentriertX = xPositionen[spaltenIndex] + (spaltenBreiten[spaltenIndex] - platzierbar.width) / 2
                val zentriertY = yPositionen[zeilenIndex] + (zeilenHöhen[zeilenIndex] - platzierbar.height) / 2
                platzierbar.placeRelative(zentriertX, zentriertY)
            }
        }
    }
}
