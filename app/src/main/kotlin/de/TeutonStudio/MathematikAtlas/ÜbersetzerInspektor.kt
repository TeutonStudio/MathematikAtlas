package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*

object ÜbersetzerInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        Text("Übersetzungsziel", style = MaterialTheme.typography.titleSmall)
        if (übersetzerZiel(knoten) in setOf("polar", "kartesisch")) {
            Row { Text("Radius und Winkel getrennt"); Switch(checked = übersetzerPolarGetrennt(knoten), onCheckedChange = { getrennt -> aktionen.knoten(konfiguriereÜbersetzerPolarAnschlüsse(knoten, getrennt)) }) }
        }
        Column {
            ÜBERSETZER_ZIELE.forEach { ziel ->
                TextButton(onClick = { aktionen.knoten(konfiguriereÜbersetzer(knoten, ziel)) }) {
                    Text(if (ziel == übersetzerZiel(knoten)) "✓ ${übersetzerZielTitel(ziel)}" else übersetzerZielTitel(ziel))
                }
            }
        }
    }
}
