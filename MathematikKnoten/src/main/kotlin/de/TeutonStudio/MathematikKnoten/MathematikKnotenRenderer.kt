package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MENGENKONSTRUKTOR_ART
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikKartenAdapter.prädikatsArgumente
import de.TeutonStudio.MathematikRechenSystem.kern.*

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
                knoten.art == "mathematik.termZuMethode" -> LatexText(
                    termZuMethodeFormel(ergebnis),
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

    private fun termZuMethodeFormel(ergebnis: KnotenAuswertungsErgebnis?): String {
        val ausgewertet = ergebnis
            ?: return "f:\\begin{cases}? \\longrightarrow ?\\end{cases}"
        val methode = ausgewertet.ausgaben["methode"]?.objekt as? Methode
            ?: return "f:\\begin{cases}? \\longrightarrow ?\\end{cases}"
        if (methode.istPrädikat()) {
            val termEingang = ausgewertet.eingänge["term"]
            val argumentQuellen = termEingang?.let { it.prädikatsArgumente() }.orEmpty()
            return runCatching { methode.kompaktePrädikatsDarstellung(argumentQuellen = argumentQuellen) }
                .getOrElse { methode.zuLatex() }
        }

        val signatur = runCatching { methode.methodenSignatur() }.getOrNull()
        val argumente = methode.parameter.joinToString(",") { it.zuLatex() }
        val wertevorrat = signatur?.werteVorrat?.zuLatex() ?: "?"
        val zielmenge = signatur?.zielMenge?.zuLatex() ?: "?"
        val bild = runCatching { methode.vorschrift.zuLatex() }.getOrDefault("?")
        val tupel = when (methode.parameter.size) {
            0 -> "\\left\\right"
            1 -> argumente
            else -> "\\left($argumente\\right)"
        }
        return "${methode.name}:\\begin{cases}$wertevorrat \\longrightarrow $zielmenge\\\\$tupel \\mapsto $bild\\end{cases}"
    }

    private companion object {
        val iterativeArten = setOf("mathematik.iterierteSumme", "mathematik.iteriertesProdukt", "mathematik.iterierteVereinigung", "mathematik.iterierterSchnitt", "mathematik.iteriertesKartesischesProdukt", MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART)
        val mengenIterationsArten = setOf("mathematik.iterierteVereinigung", "mathematik.iterierterSchnitt")
    }
}
