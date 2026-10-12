package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAktionen
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAbstände
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/** Ein Rahmen für alle Fachpaletten und beide Plattformen. Übernahme betrifft den gesamten Entwurf. */
@Composable
internal fun FormelEditorDialogRahmen(
    titel: String,
    beschreibung: String,
    zeichnung: FormelZeichnungsEntwurf,
    gültig: Boolean,
    schließen: () -> Unit,
    übernehmen: () -> Unit,
    vorschau: @Composable () -> Unit,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    AtlasDialog(onDismissRequest = schließen, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize()) {
            AtlasDialogKopf(
                titel = { Text(titel) },
                untertitel = beschreibung.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                aktionen = { TextButton(onClick = schließen) { Text("Schließen") } },
            )
            // Endliche sichtbare Bodyhöhe; die Fachpaletten begrenzen ihre eigenen Lazy-Grids.
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(AtlasDialogAbstände.inhalt),
                verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.felder)) {
                FormelEingabeModi(zeichnung)
                vorschau()
                inhalt()
            }
            AtlasDialogAktionen {
                TextButton(onClick = schließen) { Text("Abbrechen") }
                Button(onClick = übernehmen, enabled = gültig) { Text("Formel übernehmen") }
            }
        }

    }
}
