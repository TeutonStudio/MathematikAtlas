package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "mathematik.disjunkt", showBackground = true, widthDp = 760, heightDp = 1200)
@Composable
private fun MathematikDisjunktPreview() {
    KnotenVariantenPreview(KnotenPreviewDaten.für("mathematik.disjunkt"))
}
