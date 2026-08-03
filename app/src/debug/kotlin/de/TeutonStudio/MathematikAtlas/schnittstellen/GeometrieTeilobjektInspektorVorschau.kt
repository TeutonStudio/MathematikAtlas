package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable

@HelleVorschau
@Composable
private fun GeometrieTeilobjektInspektorOhneVerbindungVorschau() {
    KnotenInspektorVorschau(VorschauDaten.GeometrischeKante)
}

@DunkleVorschau
@Composable
private fun GeometrieTeilobjektInspektorDunkelVorschau() {
    KnotenInspektorVorschau(VorschauDaten.GeometrischeKante, dunkel = true)
}
