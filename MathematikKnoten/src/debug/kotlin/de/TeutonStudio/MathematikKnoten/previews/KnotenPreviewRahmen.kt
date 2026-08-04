package de.TeutonStudio.MathematikKnoten.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKnoten.MathematikKnotenRenderer

internal data class KnotenArtPreviewGruppe(
    val art: String,
    val varianten: List<KnotenVorlage>,
) {
    override fun toString(): String = art.substringAfterLast('.')
}

internal object PreviewAktionen : KnotenRendererAktionen {
    override fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
}

@Composable
internal fun KnotenVariantenPreview(
    gruppe: KnotenArtPreviewGruppe,
    modifier: Modifier = Modifier,
) {
    val renderer = MathematikKnotenRenderer()
    MaterialTheme {
        Column(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(gruppe.art, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${gruppe.varianten.size} registrierte Variante${if (gruppe.varianten.size == 1) "" else "n"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            gruppe.varianten.forEachIndexed { index, vorlage ->
                val knoten = vorlage.alsDeterministischenPreviewKnoten(index)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        vorlage.name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (vorlage.standardParameter.isNotEmpty()) {
                        Text(
                            vorlage.standardParameter.toSortedMap().entries.joinToString(" · ") { (key, value) -> "$key=$value" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .width(vorlage.standardGröße.breite.dp)
                            .height(vorlage.standardGröße.höhe.dp),
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp,
                    ) {
                        renderer.Inhalt(
                            knoten = knoten,
                            ausgewählt = index == 0,
                            aktionen = PreviewAktionen,
                        )
                    }
                }
            }
        }
    }
}

internal fun KnotenVorlage.alsDeterministischenPreviewKnoten(index: Int): KnotenDaten {
    val idTeil = art.lowercase()
        .map { zeichen -> if (zeichen.isLetterOrDigit()) zeichen else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
    return KnotenDaten(
        id = KnotenId("preview-$idTeil-$index"),
        art = art,
        name = name,
        position = GraphPunkt.Zero,
        größe = standardGröße,
        anschlüsse = anschlüsse.mapIndexed { anschlussIndex, anschluss ->
            anschluss.copy(id = AnschlussId("preview-$idTeil-$index-anschluss-$anschlussIndex"))
        },
        parameter = standardParameter,
        eigenschaften = standardEigenschaften,
        kartenVerweis = kartenVerweis,
    )
}
