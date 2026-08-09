package de.TeutonStudio.MathematikKnoten

import androidx.compose.runtime.Composable
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenAuswertungszeitFußzeile
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis

/** Ergänzt jeden Renderer um dieselbe Laufzeitdiagnose, ohne seine Knotendarstellung zu verändern. */
fun KnotenRenderer.mitAuswertungszeit(
    ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis?,
): KnotenRenderer {
    val basis = this
    return object : KnotenRenderer {
        override val interaktionsModus: KnotenInteraktionsModus get() = basis.interaktionsModus

        @Composable
        override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
            basis.Inhalt(knoten, ausgewählt, aktionen)
        }

        @Composable
        override fun Fußzeile(knoten: KnotenDaten, ausgewählt: Boolean) {
            KnotenAuswertungszeitFußzeile(ergebnisFür(knoten)?.auswertungsDauerNanos)
        }
    }
}
