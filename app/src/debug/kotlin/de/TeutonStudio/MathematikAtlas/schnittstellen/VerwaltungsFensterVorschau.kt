package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikAtlas.VerwaltungsBereich
import de.TeutonStudio.MathematikAtlas.VerwaltungsFenster

@Preview(
    name = "Verwaltung · Kartenbibliothek",
    widthDp = 340,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun VerwaltungsFensterKartenVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    LaunchedEffect(zustand) { zustand.linkerBereich = VerwaltungsBereich.Karten }
    MathematikAtlasVorschauRahmen {
        VerwaltungsFenster(
            zustand = zustand,
            modifier = Modifier.width(300.dp).fillMaxHeight(),
        )
    }
}

@Preview(
    name = "Verwaltung · Auswertung · Dunkel",
    widthDp = 340,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun VerwaltungsFensterAuswertungVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    LaunchedEffect(zustand) { zustand.linkerBereich = VerwaltungsBereich.Auswertung }
    MathematikAtlasVorschauRahmen(dunkel = true) {
        VerwaltungsFenster(
            zustand = zustand,
            modifier = Modifier.width(300.dp).fillMaxHeight(),
        )
    }
}
