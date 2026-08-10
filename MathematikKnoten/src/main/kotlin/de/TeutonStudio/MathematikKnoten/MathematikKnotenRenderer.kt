package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenAuswertungszeitFußzeile
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
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
    override val interaktionsModus: KnotenInteraktionsModus = KnotenInteraktionsModus.GanzeFlächeZiehbar

    @Composable
    override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        val ergebnis = ergebnisFür(knoten)
        val ausgabe = ergebnis?.ausgaben?.values?.firstOrNull()
        val objekt = ausgabe?.objekt
        var geöffneteDefinition by remember(knoten.id) { mutableStateOf<AutomatischesAdjektiv?>(null) }

        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(knoten.name, style = MaterialTheme.typography.titleMedium)
            (objekt as? Methode)?.let { methode ->
                Text(methode.aliasAnzeige(), style = MaterialTheme.typography.labelSmall)
            }
            when {
                knoten.art == MENGENKONSTRUKTOR_ART -> LatexFormel(
                    mengenkonstruktorFormel(knoten),
                    style = MaterialTheme.typography.bodyLarge,
                )
                knoten.art == "mathematik.variable" -> LatexFormel(
                    variablenFormel(knoten),
                    style = MaterialTheme.typography.bodyLarge,
                )
                knoten.art == "mathematik.addition" -> LatexFormel(operatorFormel(knoten, ergebnis, " + "), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.extremwert" -> LatexFormel(extremwertFormel(knoten, ergebnis), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.vereinigung" -> LatexFormel(operatorFormel(knoten, ergebnis, " \\cup "), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.schnitt" -> LatexFormel(operatorFormel(knoten, ergebnis, " \\cap "), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.kartesischesProdukt" -> LatexFormel(operatorFormel(knoten, ergebnis, " \\times "), style = MaterialTheme.typography.bodyLarge)
                knoten.art in iterativeArten -> LatexFormel(iterationsFormel(knoten, ergebnis), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.termZuMethode" -> LatexFormel(
                    termZuMethodeFormel(ergebnis),
                    style = MaterialTheme.typography.bodyLarge,
                )
                knoten.art == "mathematik.tupel" &&
                    tupelKonfiguration(knoten).erzeugungsArt == TUPEL_METHODE && ausgabe == null -> LatexFormel(
                    "\\left(f(1),\\ldots,f(n)\\right)",
                    style = MaterialTheme.typography.bodyLarge,
                )
                knoten.art == "mathematik.auswerten" && objekt is WahrheitsKonstante -> LatexFormel(
                    objekt.zuLatex(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ausgabe != null -> LatexFormel(ausgabe.anzeigeLatex(), style = MaterialTheme.typography.bodyLarge)
                knoten.parameter.isNotEmpty() -> LatexText(knoten.parameter.values.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                else -> Text(knoten.art.substringAfterLast('.'), style = MaterialTheme.typography.bodySmall)
            }

            val adjektive = objekt?.let(::automatischeAdjektive).orEmpty()
            if (adjektive.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    adjektive.take(3).forEach { adjektiv ->
                        AdjektivMarke(
                            adjektiv = adjektiv,
                            öffnen = { geöffneteDefinition = adjektiv },
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
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

            (objekt as? EigenschaftsAussage)?.let { aussage ->
                EigenschaftsStatusZeile(aussage)
            }
            ergebnis?.fehler?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
        }

        geöffneteDefinition?.let { adjektiv ->
            AdjektivDefinitionsDialog(
                adjektiv = adjektiv,
                schließen = { geöffneteDefinition = null },
            )
        }
    }

    @Composable
    override fun Fußzeile(knoten: KnotenDaten, ausgewählt: Boolean) {
        KnotenAuswertungszeitFußzeile(ergebnisFür(knoten)?.auswertungsDauerNanos)
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

        return runCatching { methode.zuFallunterscheidungsLatex() }
            .getOrElse { methode.zuLatex() }
    }

    private companion object {
        val iterativeArten = setOf("mathematik.iterierteSumme", "mathematik.iteriertesProdukt", "mathematik.iterierteVereinigung", "mathematik.iterierterSchnitt", "mathematik.iteriertesKartesischesProdukt", MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART)
        val mengenIterationsArten = setOf("mathematik.iterierteVereinigung", "mathematik.iterierterSchnitt")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdjektivMarke(
    adjektiv: AutomatischesAdjektiv,
    öffnen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = "Eigenschaft ${adjektiv.text}. Definition öffnen."
            }
            .combinedClickable(
                onClick = öffnen,
                onLongClick = öffnen,
            ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            adjektiv.text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun EigenschaftsStatusZeile(aussage: EigenschaftsAussage) {
    val farbe = when (aussage.unterstuetzung) {
        UnterstuetzungsStatus.IMPLEMENTIERT -> MaterialTheme.colorScheme.onSurfaceVariant
        UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH -> MaterialTheme.colorScheme.error
        UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT -> MaterialTheme.colorScheme.primary
    }
    val präfix = when (aussage.unterstuetzung) {
        UnterstuetzungsStatus.IMPLEMENTIERT -> aussage.aussageStatus.name.lowercase()
        UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH -> "Fehlende mathematische Struktur"
        UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT -> "Noch nicht implementiert"
    }
    Text(
        listOfNotNull(präfix, aussage.diagnose?.nachricht).joinToString(": "),
        color = farbe,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 3,
    )
}

@Composable
private fun AdjektivDefinitionsDialog(
    adjektiv: AutomatischesAdjektiv,
    schließen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("${adjektiv.text} · Pseudokarte") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Automatisch abgeleitete Eigenschaft",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelLarge,
                )
                PseudoDefinitionsKnoten(
                    rolle = "Subjekt",
                    inhalt = adjektiv.subjektLatex,
                    latex = true,
                )
                Text("↓", style = MaterialTheme.typography.titleMedium)
                PseudoDefinitionsKnoten(
                    rolle = "Begriff",
                    inhalt = adjektiv.wissensId,
                )
                Text("↓", style = MaterialTheme.typography.titleMedium)
                PseudoDefinitionsKnoten(
                    rolle = "Aussage",
                    inhalt = "${adjektiv.subjektLatex} ist ${adjektiv.text}.",
                )
                Text(
                    adjektiv.erklärung,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Die drei Knoten und ihre beiden Verbindungen existieren nur in diesem Dialog. Sie verändern weder Karte, Auswahl, Undo/Redo noch Dirty-State.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = schließen) { Text("Schließen") } },
    )
}

@Composable
private fun PseudoDefinitionsKnoten(
    rolle: String,
    inhalt: String,
    latex: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$rolle-Knoten: $inhalt"
            },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(rolle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            if (latex) {
                LatexFormel(inhalt, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(inhalt, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
