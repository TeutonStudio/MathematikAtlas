package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "mathematik.spur", showBackground = true, widthDp = 760, heightDp = 1200)
@Composable
private fun MathematikSpurPreview() {
    KnotenVariantenPreview(KnotenPreviewDaten.für("mathematik.spur"))
}
