package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft

enum class KnotenInteraktionsModus { GanzeFlächeZiehbar, NurKopfzeileZiehbar }

/** Vom Editor bereitgestellte, undo-fähige Schreibschnittstelle für Renderer. */
interface KnotenRendererAktionen {
    fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>)
    fun inhaltsminimumMelden(minimum: GraphGröße) {}
}

interface KnotenRenderer {
    val interaktionsModus: KnotenInteraktionsModus get() = KnotenInteraktionsModus.GanzeFlächeZiehbar
    val misstNatürlichenInhalt: Boolean get() = interaktionsModus == KnotenInteraktionsModus.GanzeFlächeZiehbar
    @Composable fun Mindestgröße(knoten: KnotenDaten): GraphGröße {
        val skala = LocalDensity.current.fontScale.coerceAtLeast(1f)
        return if (misstNatürlichenInhalt) GraphGröße(180f * skala, 96f * skala)
        else GraphGröße(360f * skala, 280f * skala)
    }
    @Composable fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen)

    /**
     * Optionale Diagnosezeile außerhalb des logischen Knotenrechtecks.
     * Sie verändert weder gespeicherte Knotengröße noch Anschluss- oder Treffergeometrie.
     */
    @Composable fun Fußzeile(knoten: KnotenDaten, ausgewählt: Boolean) {}
}

object StandardKnotenRenderer : KnotenRenderer {
    @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(knoten.name, style = MaterialTheme.typography.titleMedium)
            Text(knoten.art, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            knoten.parameter.entries.take(3).forEach { (schlüssel, wert) ->
                Text("$schlüssel: $wert", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
