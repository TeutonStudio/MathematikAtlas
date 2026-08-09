package de.TeutonStudio.MathematikAtlas

import kotlin.math.ceil
import kotlin.math.floor

/** Einmal pro Textrevision erzeugte Positionsquelle des JSON-Editors. */
internal class JsonZeilenIndex private constructor(
    private val anfänge: IntArray,
    val maximaleZeilenLänge: Int,
    private val textLänge: Int,
) {
    val zeilenAnzahl: Int get() = anfänge.size

    fun position(offset: Int): JsonPosition {
        val begrenzt = offset.coerceIn(0, textLänge)
        val treffer = anfänge.binarySearch(begrenzt)
        val index = if (treffer >= 0) treffer else (-treffer - 2).coerceAtLeast(0)
        return JsonPosition(
            zeile = index + 1,
            spalte = begrenzt - anfänge[index] + 1,
        )
    }

    fun offset(zeile: Int, spalte: Int): Int {
        val index = (zeile - 1).coerceIn(0, anfänge.lastIndex)
        val start = anfänge[index]
        val endeExklusiv = if (index == anfänge.lastIndex) textLänge else anfänge[index + 1] - 1
        return (start + spalte.coerceAtLeast(1) - 1).coerceIn(start, endeExklusiv)
    }

    companion object {
        fun erzeuge(text: String): JsonZeilenIndex {
            val anfänge = IntArray(text.count { it == '\n' } + 1)
            var zeilenIndex = 1
            var zeilenStart = 0
            var maximaleLänge = 0
            text.forEachIndexed { index, zeichen ->
                if (zeichen == '\n') {
                    maximaleLänge = maxOf(maximaleLänge, index - zeilenStart)
                    anfänge[zeilenIndex++] = index + 1
                    zeilenStart = index + 1
                }
            }
            maximaleLänge = maxOf(maximaleLänge, text.length - zeilenStart)
            return JsonZeilenIndex(anfänge, maximaleLänge, text.length)
        }
    }
}

internal data class JsonSichtbarerZeilenbereich(
    val ersteZeile: Int,
    val letzteZeile: Int,
) {
    val anzahl: Int get() = (letzteZeile - ersteZeile + 1).coerceAtLeast(0)
}

/** Bestimmt nur die zu komponierenden Zeilennummern einschließlich einer Pufferzeile. */
internal fun sichtbarerJsonZeilenbereich(
    scrollYpx: Int,
    viewportHöhePx: Int,
    zeilenHöhePx: Float,
    zeilenAnzahl: Int,
    innenabstandPx: Float = 0f,
): JsonSichtbarerZeilenbereich {
    require(zeilenHöhePx > 0f)
    if (zeilenAnzahl <= 0) return JsonSichtbarerZeilenbereich(1, 0)
    val inhaltStart = (scrollYpx - innenabstandPx).coerceAtLeast(0f)
    val erste = (floor(inhaltStart / zeilenHöhePx).toInt() + 1 - 1).coerceIn(1, zeilenAnzahl)
    val sichtbaresEnde = (scrollYpx + viewportHöhePx - innenabstandPx).coerceAtLeast(0f)
    val letzte = (ceil(sichtbaresEnde / zeilenHöhePx).toInt() + 1).coerceIn(erste, zeilenAnzahl)
    return JsonSichtbarerZeilenbereich(erste, letzte)
}

internal enum class JsonAnalyseModus(
    val anzeigeName: String,
    val automatischeVerzögerungMillis: Long?,
) {
    Vollständig("Vollständige Live-Analyse", 150L),
    Verzögert("Verzögerte Live-Analyse", 650L),
    Reduziert("Reduzierte Analyse", null),
}

internal const val JSON_VOLLSTAENDIG_MAX_BYTES = 250 * 1024
internal const val JSON_VERZOEGER_MAX_BYTES = 1024 * 1024
internal const val JSON_VOLLSTAENDIG_MAX_ZEILEN = 5_000
internal const val JSON_VERZOEGER_MAX_ZEILEN = 20_000
internal const val JSON_ZEILENHOEHE_SP = 20f

/** Wandelt die Textzeilenhöhe so um, dass der Zeilenrand dieselbe Font-Skalierung erhält. */
internal fun jsonZeilenHöheDp(fontScale: Float): Float {
    require(fontScale > 0f)
    return JSON_ZEILENHOEHE_SP * fontScale
}

internal fun jsonAnalyseModus(textGrößeBytes: Int, zeilenAnzahl: Int): JsonAnalyseModus = when {
    textGrößeBytes <= JSON_VOLLSTAENDIG_MAX_BYTES && zeilenAnzahl <= JSON_VOLLSTAENDIG_MAX_ZEILEN ->
        JsonAnalyseModus.Vollständig
    textGrößeBytes <= JSON_VERZOEGER_MAX_BYTES && zeilenAnzahl <= JSON_VERZOEGER_MAX_ZEILEN ->
        JsonAnalyseModus.Verzögert
    else -> JsonAnalyseModus.Reduziert
}

internal fun reduzierteJsonPrüfungV2311(
    text: String,
    zeilenIndex: JsonZeilenIndex = JsonZeilenIndex.erzeuge(text),
): JsonPrüfungV2311 = JsonPrüfungV2311(
    analysierterText = text,
    syntaxFehler = null,
    schemaFehler = null,
    zeilenAnzahl = zeilenIndex.zeilenAnzahl,
    listen = emptyList(),
    idBereiche = emptyList(),
    tokens = emptyList(),
    vollständig = false,
)
