package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "mathematik.iterierterSchnitt", showBackground = true, widthDp = 760, heightDp = 1200)
@Composable
private fun MathematikIterierterSchnittPreview() {
    KnotenVariantenPreview(KnotenPreviewDaten.für("mathematik.iterierterSchnitt"))
}
