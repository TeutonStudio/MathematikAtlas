package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable

@HelleVorschau
@Composable
private fun EndlicheMengeInspektorBefülltVorschau() {
    KnotenInspektorVorschau(VorschauDaten.EndlicheMenge)
}

@DunkleVorschau
@Composable
private fun EndlicheMengeInspektorDunkelVorschau() {
    KnotenInspektorVorschau(VorschauDaten.EndlicheMenge, dunkel = true)
}
