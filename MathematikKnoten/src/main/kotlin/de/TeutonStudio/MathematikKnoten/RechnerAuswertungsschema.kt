package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialog
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Abgeleitete, nicht persistierte Sicht auf das tatsächliche Auswertungsergebnis. */
data class RechnerSchemaModell(
    val titel: String,
    val eingänge: List<Pair<String, String>>,
    val ausgänge: List<Pair<String, String>>,
    val fehler: String?,
)
fun rechnerSchemaModell(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?): RechnerSchemaModell =
    RechnerSchemaModell(
        knoten.name,
        ergebnis?.eingänge.orEmpty().map { it.key to it.value.anzeigeLatex() },
        ergebnis?.ausgaben.orEmpty().map { it.key to it.value.anzeigeLatex() },
        ergebnis?.fehler ?: if (ergebnis == null) "Noch kein Auswertungsergebnis vorhanden." else null,
    )

/** Liest Komponenten ausschließlich aus der bestehenden Tensor-Schnittstelle. */
fun schemaKomponente(objekt: AtlasWert, index: List<Int>): String? =
    (objekt as? Tensorartig)?.takeIf { tensor ->
        index.size == tensor.tensorForm.size &&
            index.indices.all { index[it] in 0 until tensor.tensorForm[it] }
    }?.tensorKomponente(index)?.zuLatex()

@Composable
fun RechnerAuswertungsschemaInhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, modifier: Modifier = Modifier) {
    val modell = remember(knoten, ergebnis) { rechnerSchemaModell(knoten, ergebnis) }
    val tensor = ergebnis?.ausgaben?.values?.firstOrNull()?.objekt as? Tensorartig
    var index by remember(knoten.id, ergebnis) { mutableStateOf(tensor?.tensorForm?.map { 0 }.orEmpty()) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(modell.titel, style = MaterialTheme.typography.titleMedium)
        modell.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Eingaben", style = MaterialTheme.typography.titleSmall)
        modell.eingänge.forEach { (name, latex) -> Column(Modifier.padding(8.dp)) { Text(name); LatexText(latex) } }
        if (tensor != null && modell.fehler == null && tensor.tensorForm.all { it > 0 }) {
            Text("Ergebniskomponente auswählen", style = MaterialTheme.typography.titleSmall)
            tensor.tensorForm.forEachIndexed { achse, länge ->
                Row {
                    Text("Achse ${achse + 1}: ${index[achse] + 1}")
                    TextButton(enabled = index[achse] > 0, onClick = {
                        index = index.toMutableList().also { it[achse]-- }
                    }) { Text("−") }
                    TextButton(enabled = index[achse] + 1 < länge, onClick = {
                        index = index.toMutableList().also { it[achse]++ }
                    }) { Text("+") }
                }
            }
            val komponenten = ergebnis.eingänge.mapNotNull { (name, wert) ->
                schemaKomponente(wert.objekt, index)?.let { name to it }
            }
            komponenten.forEach { (name, latex) ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.padding(8.dp)) { Text(name); LatexText(latex) }
                }
            }
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(Modifier.padding(8.dp)) {
                    Text("Ergebnis [${index.joinToString { (it + 1).toString() }}]")
                    LatexText(tensor.tensorKomponente(index).zuLatex())
                }
            }
        }
        Text("Ergebnis", style = MaterialTheme.typography.titleSmall)
        modell.ausgänge.forEach { (name, latex) -> Column(Modifier.padding(8.dp)) { Text(name); LatexText(latex) } }
        ergebnis?.warnungen?.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun RechnerSchemaSchaltfläche(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?) {
    var offen by remember(knoten.id) { mutableStateOf(false) }
    OutlinedButton(onClick = { offen = true }) { Text("Auswertungsschema") }
    if (offen) AtlasDialog(onDismissRequest = { offen = false }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 840.dp)) {
            AtlasDialogKopf(
                titel = { Text("${knoten.name} · Auswertungsschema") },
                aktionen = { TextButton(onClick = { offen = false }) { Text("Schließen") } },
            )
            RechnerAuswertungsschemaInhalt(knoten, ergebnis, Modifier.weight(1f, fill = false).fillMaxWidth())
        }
    }
}
