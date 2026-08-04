package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen

/**
 * Android Studio erzeugt für jeden Parameterwert eine eigene Preview.
 * Jeder Wert entspricht genau einer stabilen Knotenart und zeigt in einer
 * vertikalen Column sämtliche registrierten Varianten dieser Art.
 */
internal class KnotenArtPreviewProvider : PreviewParameterProvider<KnotenArtPreviewGruppe> {
    override val values: Sequence<KnotenArtPreviewGruppe> =
        alleMathematikDefinitionsVorlagen()
            .groupBy { it.art }
            .toSortedMap()
            .map { (art, varianten) ->
                KnotenArtPreviewGruppe(
                    art = art,
                    varianten = varianten.sortedWith(
                        compareBy({ it.name }, { it.standardParameter.toSortedMap().toString() }),
                    ),
                )
            }
            .asSequence()
}

@Preview(
    name = "Mathematische Knotenart",
    showBackground = true,
    widthDp = 760,
    heightDp = 1200,
)
@Composable
private fun MathematikKnotenArtPreview(
    @PreviewParameter(KnotenArtPreviewProvider::class)
    gruppe: KnotenArtPreviewGruppe,
) {
    KnotenVariantenPreview(gruppe)
}
