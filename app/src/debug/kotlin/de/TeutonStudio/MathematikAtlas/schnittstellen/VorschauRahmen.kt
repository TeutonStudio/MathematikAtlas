package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikAtlas.DarstellungsModus
import de.TeutonStudio.MathematikAtlas.MathematikAtlasTheme
import de.TeutonStudio.MathematikAtlas.speicher.ProfilFarbe

internal val VorschauProfilFarbe: ProfilFarbe =
    requireNotNull(ProfilFarbe.parse("#0F766E"))

@Composable
internal fun MathematikAtlasVorschauRahmen(
    dunkel: Boolean = false,
    profilFarbe: ProfilFarbe = VorschauProfilFarbe,
    inhalt: @Composable BoxScope.() -> Unit,
) {
    MathematikAtlasTheme(
        modus = if (dunkel) DarstellungsModus.Dunkel else DarstellungsModus.Hell,
        profilFarbe = profilFarbe,
        onModusÄndern = {},
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
                content = inhalt,
            )
        }
    }
}

@Preview(
    name = "Hell · Telefon",
    group = "Darstellung",
    widthDp = 420,
    heightDp = 860,
    showBackground = true,
)
@Preview(
    name = "Hell · Tablet",
    group = "Darstellung",
    widthDp = 1100,
    heightDp = 760,
    showBackground = true,
)
internal annotation class HelleVorschau

@Preview(
    name = "Dunkel · Telefon",
    group = "Darstellung",
    widthDp = 420,
    heightDp = 860,
    showBackground = true,
)
@Preview(
    name = "Dunkel · Tablet",
    group = "Darstellung",
    widthDp = 1100,
    heightDp = 760,
    showBackground = true,
)
internal annotation class DunkleVorschau
