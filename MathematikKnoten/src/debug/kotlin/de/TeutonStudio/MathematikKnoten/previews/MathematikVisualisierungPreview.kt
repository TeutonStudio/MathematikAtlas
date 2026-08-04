package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "mathematik.visualisierung", showBackground = true, widthDp = 760, heightDp = 1200)
@Composable
private fun MathematikVisualisierungPreview() {
    KnotenVariantenPreview(KnotenPreviewDaten.für("mathematik.visualisierung"))
}
