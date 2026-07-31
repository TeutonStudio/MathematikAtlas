package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten

@Composable
internal fun KartenTabellenKopf(
    knoten: KnotenDaten,
    quelle: KartenWahrheitstabellenQuelle,
    schließen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(knoten.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "Wahrheitstabelle · feste Kartenversion ${quelle.verweis.version}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = schließen) { Text("Schließen") }
    }
}

@Composable
internal fun KartenTabellenAktionen(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    quelle: KartenWahrheitstabellenQuelle,
    schließen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = {
            zustand.öffne(quelle.verweis)
            schließen()
        }) { Text("Karte öffnen") }
        OutlinedButton(onClick = {
            zustand.editor.wähleKnoten(knoten.id)
            zustand.editor.dupliziereAuswahl()
            schließen()
        }) { Text("Knoten duplizieren") }
        OutlinedButton(onClick = {
            zustand.editor.wähleKnoten(knoten.id)
            zustand.editor.isoliereAusgewähltenKnoten()
            schließen()
        }) { Text("Knoten isolieren") }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                zustand.editor.wähleKnoten(knoten.id)
                zustand.editor.löscheAuswahl()
                schließen()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("Knoten löschen") }
    }
}
