package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis

class MathematikKnotenRenderer(
    private val ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis? = { null },
) : KnotenRenderer {
    @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean) {
        val ergebnis = ergebnisFür(knoten)
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(knoten.name, style = MaterialTheme.typography.titleMedium)
            val objekt = ergebnis?.ausgaben?.values?.firstOrNull()?.objekt
            when {
                knoten.art == "mathematik.addition" -> LatexText(operatorFormel(knoten, " + "), style = MaterialTheme.typography.bodyLarge)
                knoten.art == "mathematik.vereinigung" -> LatexText(operatorFormel(knoten, " \\cup "), style = MaterialTheme.typography.bodyLarge)
                objekt != null -> LatexText(objekt.zuLatex(), style = MaterialTheme.typography.bodyLarge)
                knoten.parameter.isNotEmpty() -> Text(knoten.parameter.values.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                else -> Text(knoten.art.substringAfterLast('.'), style = MaterialTheme.typography.bodySmall)
            }
            ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, maxLines = 2) }
        }
    }

    private fun operatorFormel(knoten: KnotenDaten, zeichen: String): String = knoten.anschlüsse
        .filter { it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
        .mapIndexed { index, _ -> "input_{${index + 1}}" }
        .joinToString(zeichen)
}
