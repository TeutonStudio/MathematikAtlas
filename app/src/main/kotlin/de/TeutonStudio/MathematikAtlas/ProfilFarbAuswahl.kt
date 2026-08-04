package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikAtlas.speicher.ProfilFarbe
import de.TeutonStudio.MathematikAtlas.speicher.RgbFarbe
import de.TeutonStudio.MathematikAtlas.speicher.rgbHex
import de.TeutonStudio.MathematikAtlas.speicher.zuProfilFarbe

/** Kompakter Profilaufrufer; die vollständige Bearbeitung lebt im eigenständigen Dialog. */
@Composable
internal fun ProfilFarbAuswahl(
    startFarbe: ProfilFarbe,
    farbeGeaendert: (ProfilFarbe) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogOffen by remember { mutableStateOf(false) }
    val rgb = remember(startFarbe) { RgbFarbe.aus(startFarbe) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Profilfarbe", style = MaterialTheme.typography.titleMedium)
        Text(
            "Die Farbe wird im Dialog bearbeitet. Erst „Übernehmen“ ändert den Profilentwurf; dauerhaft gespeichert wird weiterhin mit „Profil speichern“.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .semantics { contentDescription = "Aktuelle Profilfarbe ${rgb.rgbHex}" },
            color = Color(rgb.argbLong),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {}
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                rgb.rgbHex,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
            )
            Button(onClick = { dialogOffen = true }) { Text("Farbe auswählen") }
        }
    }

    FarbAuswahlDialog(
        offen = dialogOffen,
        ausgangsFarbe = rgb,
        standardFarbe = RgbFarbe.aus(ProfilFarbe.Standard),
        titel = "Profilfarbe auswählen",
        onAbbrechen = { dialogOffen = false },
        onBestaetigen = { farbe ->
            farbeGeaendert(farbe.zuProfilFarbe())
            dialogOffen = false
        },
    )
}
