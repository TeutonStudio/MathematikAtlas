package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable

@HelleVorschau
@Composable
private fun ZahlenRechnerInspektorPotenzVorschau() {
    KnotenInspektorVorschau(VorschauDaten.ZahlenRechner)
}

@DunkleVorschau
@Composable
private fun ZahlenRechnerInspektorDunkelVorschau() {
    KnotenInspektorVorschau(VorschauDaten.ZahlenRechner, dunkel = true)
}
