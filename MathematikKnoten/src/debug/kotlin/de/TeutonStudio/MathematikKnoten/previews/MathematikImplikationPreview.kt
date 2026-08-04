package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "mathematik.implikation", showBackground = true, widthDp = 760, heightDp = 1200)
@Composable
private fun MathematikImplikationPreview() {
    KnotenVariantenPreview(KnotenPreviewDaten.für("mathematik.implikation"))
}
