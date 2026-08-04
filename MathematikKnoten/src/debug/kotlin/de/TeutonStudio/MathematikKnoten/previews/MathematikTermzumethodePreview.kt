package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "mathematik.termZuMethode", showBackground = true, widthDp = 760, heightDp = 1200)
@Composable
private fun MathematikTermZuMethodePreview() {
    KnotenVariantenPreview(KnotenPreviewDaten.für("mathematik.termZuMethode"))
}
