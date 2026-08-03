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
import de.TeutonStudio.MathematikAtlas.IterierteMethodenKartenInspektor

@Preview(
    name = "Iterierte Summe · Kartenmethode",
    widthDp = 460,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun IterierteMethodenKartenInspektorVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    val methode = zustand.karten.first()
    val knoten = VorschauDaten.knoten(
        art = "mathematik.iterierteSumme",
        name = "Summe der Quadratzahlen",
    ).copy(
        eingangsKartenVerweise = mapOf(
            "methode" to KartenVerweis(methode.id, methode.version),
        ),
    )
    MathematikAtlasVorschauRahmen(dunkel = true) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Iterierte Methode", style = MaterialTheme.typography.headlineSmall)
                IterierteMethodenKartenInspektor(knoten, zustand)
            }
        }
    }
}
