package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAbstände
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    AtlasDialog(
        onDismissRequest = zustand::schließeKnotenAuswahl,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.fillMaxSize()) {
                AtlasDialogKopf(
                    titel = { Text("Konzeptbibliothek") },
                    untertitel = { Text(
                            "Dreistufige fachliche Navigation mit adaptiven Rastern",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) },
                    aktionen = { TextButton(onClick = onStandardWähler) { Text("Standardliste") }
                    TextButton(onClick = zustand::schließeKnotenAuswahl) { Text("Schließen") } },
                )
                HorizontalDivider()
                KonzeptBibliothekInhalt(
                    zustand = zustand,
                    position = position,
                    vorlagen = vorlagen,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(AtlasDialogAbstände.inhalt),
                )
            }

        }
    }
}
