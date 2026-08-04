package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MENGENKONSTRUKTOR_ART
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikRechenSystem.kern.*

private const val CASES_ANFANG = "\\begin{cases}"
private const val CASES_ENDE = "\\end{cases}"

internal data class LatexFallFormel(
    val vorher: String,
    val zeilen: List<String>,
    val nachher: String,
)

/** Entfernt ausschließlich äußere Display-Begrenzer; der Zeilenrenderer erhält sie nie. */
internal fun entferneLatexDisplayBegrenzer(latex: String): String {
    val getrimmt = latex.trim()
    return when {
        getrimmt.length >= 4 && getrimmt.startsWith("\$\$") && getrimmt.endsWith("\$\$") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        getrimmt.length >= 4 && getrimmt.startsWith("\\[") && getrimmt.endsWith("\\]") ->
            getrimmt.substring(2, getrimmt.length - 2).trim()
        else -> getrimmt
    }
}

/** Zerlegt eine Display-Formel mit cases in Präfix, echte Zeilen und Suffix. */
internal fun zerlegeLatexFallFormel(latex: String): LatexFallFormel? {
    val inhalt = entferneLatexDisplayBegrenzer(latex)
    val anfang = inhalt.indexOf(CASES_ANFANG)
    if (anfang < 0) return null
    val ende = inhalt.indexOf(CASES_ENDE, anfang + CASES_ANFANG.length)
    if (ende < 0) return null
    val zeilen = teileLatexZeilen(
        inhalt.substring(anfang + CASES_ANFANG.length, ende),
    )
    if (zeilen.isEmpty()) return null
    return LatexFallFormel(
        vorher = inhalt.substring(0, anfang).trimEnd(),
        zeilen = zeilen,
        nachher = inhalt.substring(ende + CASES_ENDE.length).trimStart(),
    )
}

private fun teileLatexZeilen(inhalt: String): List<String> {
    val zeilen = mutableListOf<String>()
    var gruppenTiefe = 0
    var zeilenAnfang = 0
    var position = 0
    while (position < inhalt.length) {
        when (inhalt[position]) {
            '{' -> gruppenTiefe++
            '}' -> gruppenTiefe = (gruppenTiefe - 1).coerceAtLeast(0)
        }
        if (
            gruppenTiefe == 0 &&
            inhalt[position] == '\\' &&
            position + 1 < inhalt.length &&
            inhalt[position + 1] == '\\'
        ) {
            inhalt.substring(zeilenAnfang, position).trim().takeIf(String::isNotEmpty)?.let(zeilen::add)
            position += 2
            while (position < inhalt.length && inhalt[position].isWhitespace()) position++
            zeilenAnfang = position
            continue
        }
        position++
    }
    inhalt.substring(zeilenAnfang).trim().takeIf(String::isNotEmpty)?.let(zeilen::add)
    return zeilen
}

/**
 * Display-Renderer für große Formeln. Anders als [LatexText] behandelt er
 * mehrzeilige cases strukturell und reicht nur einzelne Zeilen an den Zeilenrenderer weiter.
 */
@Composable
internal fun LatexFormel(
    latex: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val inhalt = entferneLatexDisplayBegrenzer(latex)
    val fallFormel = zerlegeLatexFallFormel(inhalt)
    if (fallFormel == null) {
        LatexText(inhalt, modifier = modifier, style = style)
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (fallFormel.vorher.isNotBlank()) {
            LatexText(fallFormel.vorher, style = style)
        }
        Text(
            text = "{",
            style = style.copy(
                fontFamily = FontFamily.Serif,
                fontSize = (fallFormel.zeilen.size * 1.15f).em,
                lineHeight = 1.0.em,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            fallFormel.zeilen.forEach { zeile ->
                LatexText(zeile, style = style)
            }
        }
        if (fallFormel.nachher.isNotBlank()) {
            LatexText(fallFormel.nachher, style = style)
        }
    }
}

internal fun methodenFormel(methode: Methode?): String {
    if (methode == null) {
        return "\\[f:\\begin{cases}? \\longrightarrow ?\\\\x \\mapsto ?\\end{cases}\\]"
    }
    val signatur = runCatching { methode.methodenSignatur() }.getOrNull()
    val argumente = methode.parameter.joinToString(",") { it.zuLatex() }
    val wertevorrat = signatur?.werteVorrat?.zuLatex() ?: "?"
    val zielmenge = signatur?.zielMenge?.zuLatex() ?: "?"
    val bild = runCatching { methode.vorschrift.zuLatex() }.getOrDefault("?")
    val urbild = when (methode.parameter.size) {
        0 -> "\\left(\\right)"
        1 -> argumente
        else -> "\\left($argumente\\right)"
    }
    return "\\[${methode.name}:\\begin{cases}$wertevorrat \\longrightarrow $zielmenge\\\\$urbild \\mapsto $bild\\end{cases}\\]"
}

internal fun variablenFormel(knoten: KnotenDaten): String {
    val name = knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
    val werteVorratKennung = knoten.parameter["werteVorrat"]?.trim().orEmpty().ifBlank { "R" }
    val werteVorrat = when (werteVorratKennung.uppercase()) {
        "N", "ℕ" -> "\\mathbb{N}"
        "Z", "ℤ" -> "\\mathbb{Z}"
        "Q", "ℚ" -> "\\mathbb{Q}"
        "R", "ℝ" -> "\\mathbb{R}"
        "C", "ℂ" -> "\\mathbb{C}"
        else -> werteVorratKennung
    }
    return "$name \\in $werteVorrat"
}

class MathematikKnotenRenderer(
    private val ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis? = { null },
) : KnotenRenderer {
    @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        val ergebnis = ergebnisFür(knoten)
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(knoten.name, style = MaterialTheme.typography.titleMedium)
            val ausgabe = ergebnis?.ausgaben?.values?.firstOrNull()
            val objekt = ausgabe?.objekt
            (objekt as? Methode)?.let { methode ->
                Text(methode.aliasAnzeige(), style = MaterialTheme.typography.labelSmall)
            }
            when {
                knoten.art == MENGENKONSTRUKTOR_ART -> LatexText(
                    mengenkonstruktorFormel(knoten),
                    style = MaterialTheme.typography.bodyLarge,
                )
                knoten.art == "mathematik.variable" -> LatexText(
                    variablenFormel(knoten),
                    style = MaterialTheme.typography.bodyLarge,
                )
                knoten.art == "mathematik.addition" -> LatexText(operatorFormel(knoten, ergebnis, " + "), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.extremwert" -> LatexText(extremwertFormel(knoten, ergebnis), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.vereinigung" -> LatexText(operatorFormel(knoten, ergebnis, " \\cup "), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.schnitt" -> LatexText(operatorFormel(knoten, ergebnis, " \\cap "), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.kartesischesProdukt" -> LatexText(operatorFormel(knoten, ergebnis, " \\times "), style = MaterialTheme.typography.bodyLarge)
                knoten.art in iterativeArten -> LatexText(iterationsFormel(knoten, ergebnis), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.termZuMethode" -> LatexFormel(
                    methodenFormel(objekt as? Methode),
                    style = MaterialTheme.typography.bodyLarge,
                )
                objekt is Methode -> LatexFormel(
                    methodenFormel(objekt),
                    style = MaterialTheme.typography.bodyLarge,
                )
                knoten.art == "mathematik.auswerten" && objekt is WahrheitsKonstante -> LatexText(
                    objekt.zuLatex(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ausgabe != null -> LatexText(ausgabe.anzeigeLatex(), style = MaterialTheme.typography.bodyLarge)
                knoten.parameter.isNotEmpty() -> LatexText(knoten.parameter.values.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                else -> Text(knoten.art.substringAfterLast('.'), style = MaterialTheme.typography.bodySmall)
            }
            if (knoten.art in mengenIterationsArten) {
                val methodenEingang = ergebnis?.eingänge?.get("methode")
                val methode = methodenEingang?.objekt as? Methode
                runCatching { methode?.grundMengeFürMengenAusgabe()?.zuLatex() }.getOrNull()?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Grundmenge:", style = MaterialTheme.typography.labelSmall)
                        LatexText(it, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, maxLines = 2) }
        }
    }

    private fun operatorFormel(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, zeichen: String): String = knoten.anschlüsse
        .filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
        .mapIndexed { index, anschluss ->
            if (knoten.parameter["operatorAnzeige"] == "name") eingabeLatex(index + 1)
            else ergebnis?.eingänge?.get(anschluss.name)?.let { it.anzeigeLatex() }
                ?: unbekanntesOperatorLatex(knoten, index + 1)
        }
        .joinToString(zeichen)

    private fun iterationsFormel(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?): String {
        val methodenWert = ergebnis?.eingänge?.get("methode")
        val methode = methodenWert?.objekt as? Methode
        val indexWert = ergebnis?.eingänge?.get("indexmenge")
        val indexMenge = indexWert?.let { it.anzeigeLatex() } ?: "I"
        val parameter = methode?.parameter?.singleOrNull()?.zuLatex() ?: "i"
        val name = methodenWert?.let { it.latexDarstellung } ?: methode?.name ?: "f"
        val zeichen = when (knoten.art) {
            "mathematik.iterierteSumme" -> "\\sum"
            "mathematik.iteriertesProdukt" -> "\\prod"
            "mathematik.iterierteVereinigung" -> "\\bigcup"
            "mathematik.iteriertesKartesischesProdukt" -> "\\mathop{\\times}"
            MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART -> when (knoten.parameter["operator"]) {
                "konjunktion" -> "\\bigwedge"
                "disjunktion" -> "\\bigvee"
                "adjunktion" -> "\\mathop{\\stackrel{\\bullet}{\\bigvee}}"
                else -> "?"
            }
            else -> "\\bigcap"
        }
        return großerOperatorLatex(
            operator = zeichen,
            indexBedingung = "$parameter \\in $indexMenge",
            rumpf = "$name($parameter)",
        )
    }

    private fun extremwertFormel(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?): String {
        val operator = if (knoten.parameter["modus"] == "minimum") "\\min" else "\\max"
        return "$operator\\left\\{${operatorFormel(knoten, ergebnis, ",")}\\right\\}"
    }

    private companion object {
        val iterativeArten = setOf("mathematik.iterierteSumme", "mathematik.iteriertesProdukt", "mathematik.iterierteVereinigung", "mathematik.iterierterSchnitt", "mathematik.iteriertesKartesischesProdukt", MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART)
        val mengenIterationsArten = setOf("mathematik.iterierteVereinigung", "mathematik.iterierterSchnitt")
    }
}
