package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikAtlas.KnotenInspektorRegister

@Composable
internal fun KnotenInspektorVorschau(
    knoten: KnotenDaten,
    dunkel: Boolean = false,
) {
    MathematikAtlasVorschauRahmen(dunkel = dunkel) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(knoten.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    knoten.art,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                val inspektor = KnotenInspektorRegister.finde(knoten.art)
                if (inspektor == null) {
                    Text(
                        "Für diese Knotenart ist kein spezialisierter Inspector registriert.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    inspektor.Inhalt(
                        knoten = knoten,
                        ergebnis = null,
                        aktionen = VorschauInspektorAktionen,
                    )
                }
            }
        }
    }
}
