package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikRechenSystem.kern.UmformungsSchritt

internal object AuswertenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        Text("Termauswertung", style = MaterialTheme.typography.titleSmall)
        Text(
  "Dieser Knoten vereinfacht mathematische Terme typ-erhaltend, darunter Zahlen, Aussagen, Matrizen, Vektoren, Tupel und Mengen. Relationen werden nicht hier gelöst, sondern im Knoten „Auflösen“.",
  style = MaterialTheme.typography.bodySmall,
  color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let { fehler ->
  Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        val hatDetails = ergebnis?.let {
  it.ausgaben.isNotEmpty() || it.schritte.isNotEmpty() || it.warnungen.isNotEmpty() || it.fehler != null
        } == true
        var dialogGeöffnet by remember(knoten.id) { mutableStateOf(false) }
        Button(
  onClick = { dialogGeöffnet = true },
  enabled = hatDetails,
  modifier = Modifier.fillMaxWidth(),
        ) {
  Text("Auswertungsdetails öffnen")
        }
        if (dialogGeöffnet && ergebnis != null) {
  AuswertungsDetailsDialog(knoten, ergebnis) { dialogGeöffnet = false }
        }
    }
}

@Composable
private fun AuswertungsDetailsDialog(
    knoten: KnotenDaten,
    ergebnis: KnotenAuswertungsErgebnis,
    schließen: () -> Unit,
) {
    var tab by remember(knoten.id) { mutableIntStateOf(0) }
    val tabs = listOf("Ergebnis", "Umformung", "Bedingungen")
    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
  modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
  shape = MaterialTheme.shapes.extraLarge,
  tonalElevation = 6.dp,
        ) {
  Column(Modifier.fillMaxSize()) {
      Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
          Text(
              "${knoten.name}: Termauswertung",
              style = MaterialTheme.typography.titleLarge,
              modifier = Modifier.weight(1f),
          )
          TextButton(onClick = schließen) { Text("Schließen") }
      }
      TabRow(selectedTabIndex = tab) {
          tabs.forEachIndexed { index, titel ->
              Tab(selected = tab == index, onClick = { tab = index }, text = { Text(titel) })
          }
      }
      when (tab) {
          0 -> ErgebnisAnsicht(ergebnis, Modifier.fillMaxSize().padding(20.dp))
          1 -> UmformungsAnsicht(ergebnis.schritte, Modifier.fillMaxSize().padding(20.dp))
          else -> BedingungenAnsicht(ergebnis, Modifier.fillMaxSize().padding(20.dp))
      }
  }
        }
    }
}

@Composable
private fun ErgebnisAnsicht(ergebnis: KnotenAuswertungsErgebnis, modifier: Modifier = Modifier) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ergebnis.ausgaben.forEach { (name, wert) ->
  item(key = "ausgabe.$name") {
      Text(name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      LatexText(wert.anzeigeLatex(), style = MaterialTheme.typography.titleMedium)
  }
        }
        if (ergebnis.warnungen.isNotEmpty()) {
  item { HorizontalDivider() }
  itemsIndexed(ergebnis.warnungen) { _, warnung ->
      Text(warnung, style = MaterialTheme.typography.bodyMedium)
  }
        }
        ergebnis.fehler?.let { fehler ->
  item { Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun UmformungsAnsicht(schritte: List<UmformungsSchritt>, modifier: Modifier = Modifier) {
    if (schritte.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
  Text("Für diesen Term sind keine Umformungsschritte erforderlich.")
        }
        return
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(schritte, key = { index, schritt -> "$index.${schritt.regelId}" }) { index, schritt ->
  Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
      Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("${index + 1}. ${schritt.titel}", fontWeight = FontWeight.SemiBold)
          LatexText(schritt.vorher.zuLatex(), style = MaterialTheme.typography.bodyMedium)
          Text("↓", style = MaterialTheme.typography.titleMedium)
          LatexText(schritt.nachher.zuLatex(), style = MaterialTheme.typography.bodyMedium)
          Text(schritt.erklärung, style = MaterialTheme.typography.bodySmall)
      }
  }
        }
    }
}

@Composable
private fun BedingungenAnsicht(ergebnis: KnotenAuswertungsErgebnis, modifier: Modifier = Modifier) {
    val bedingungen = remember(ergebnis) {
        (ergebnis.schritte.flatMap { it.bedingungen } + ergebnis.ausgaben.values.flatMap { it.annahmen })
  .distinctBy { it.zuLatex() }
    }
    if (bedingungen.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
  Text("Keine zusätzlichen Definitionsbedingungen.")
        }
        return
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(bedingungen) { index, bedingung ->
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
      Text("${index + 1}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      LatexText(bedingung.zuLatex(), style = MaterialTheme.typography.bodyLarge)
  }
        }
    }
}
