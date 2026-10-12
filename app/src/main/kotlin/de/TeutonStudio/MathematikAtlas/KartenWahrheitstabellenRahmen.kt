package de.TeutonStudio.MathematikAtlas
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAktionen
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogAbstände
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasDialogKopf

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
    AtlasDialogKopf(
                    titel = { Text(knoten.name) },
                    untertitel = { Text(
                "Wahrheitstabelle · feste Kartenversion ${quelle.verweis.version}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) },
                    aktionen = { TextButton(onClick = schließen) { Text("Schließen") } },
                )
}

@Composable
internal fun KartenTabellenAktionen(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    quelle: KartenWahrheitstabellenQuelle,
    schließen: () -> Unit,
) {
    AtlasDialogAktionen {
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
