package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable

@HelleVorschau
@Composable
private fun MatrixdiagonaleInspektorHellVorschau() {
    KnotenInspektorVorschau(VorschauDaten.Matrixdiagonale)
}

@DunkleVorschau
@Composable
private fun MatrixdiagonaleInspektorDunkelVorschau() {
    KnotenInspektorVorschau(VorschauDaten.Matrixdiagonale, dunkel = true)
}
