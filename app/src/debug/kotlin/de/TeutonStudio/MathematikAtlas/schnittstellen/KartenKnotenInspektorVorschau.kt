package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenVerweis
import de.TeutonStudio.MathematikAtlas.KartenKnotenInspektor

@Preview(
    name = "KartenKnoten · Methodenmodus",
    widthDp = 440,
    heightDp = 760,
    showBackground = true,
)
@Composable
private fun KartenKnotenInspektorVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    val referenz = zustand.karten.first()
    val knoten = VorschauDaten.KartenKnoten.copy(
        kartenVerweis = KartenVerweis(referenz.id, referenz.version),
        parameter = VorschauDaten.KartenKnoten.parameter + ("zustand" to "methode"),
    )
    MathematikAtlasVorschauRahmen {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Gauß-Verfahren als KartenKnoten", style = MaterialTheme.typography.headlineSmall)
                KartenKnotenInspektor(knoten, zustand)
            }
        }
    }
}
