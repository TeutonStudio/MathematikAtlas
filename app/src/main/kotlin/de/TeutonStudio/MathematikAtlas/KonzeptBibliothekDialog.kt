package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage

@Composable
internal fun KonzeptBibliothekDialog(
    zustand: AtlasZustand,
    position: GraphPunkt,
    vorlagen: List<KnotenVorlage>,
    onStandardWähler: () -> Unit,
) {
    Dialog(
        onDismissRequest = zustand::schließeKnotenAuswahl,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f).widthIn(max = 1440.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Konzeptbibliothek", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Dreistufige fachliche Navigation mit adaptiven Rastern",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onStandardWähler) { Text("Standardliste") }
                        TextButton(onClick = zustand::schließeKnotenAuswahl) { Text("Schließen") }
                    }
                    HorizontalDivider()
                    KonzeptBibliothekInhalt(
                        zustand = zustand,
                        position = position,
                        vorlagen = vorlagen,
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                    )
                }
            }
        }
    }
}
