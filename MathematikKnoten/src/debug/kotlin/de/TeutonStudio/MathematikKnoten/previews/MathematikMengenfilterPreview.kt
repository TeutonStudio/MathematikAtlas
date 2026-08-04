package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "mathematik.mengenfilter", showBackground = true, widthDp = 760, heightDp = 1200)
@Composable
private fun MathematikMengenfilterPreview() {
    KnotenVariantenPreview(KnotenPreviewDaten.für("mathematik.mengenfilter"))
}
