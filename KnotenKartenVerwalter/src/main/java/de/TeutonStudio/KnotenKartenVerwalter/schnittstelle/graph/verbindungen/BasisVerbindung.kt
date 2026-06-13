package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.times
import kotlin.math.pow

open class BasisVerbindung(
    graph: Graph,
    daten: VerbindungDaten,
    override val start: State<KartenPosition>,
    override val ende: State<KartenPosition>,
): Verbindung(graph,daten) {
    override var startKante: AnschlussKante = AnschlussKante.Rechts
    override var endeKante: AnschlussKante = AnschlussKante.Links


    override fun abstand(pos: KartenPosition): Offset {
        val delta = ende.value - start.value
        val distSq = delta.x.pow(2) + delta.y.pow(2)
        val t = (if (distSq != 0f) ((pos - start.value)* delta) / distSq else 0f).coerceIn(0f,1f)
        val nearest = start.value + delta * t
        val diff = pos - nearest
        return diff
    }

    override fun erhaltePfad(): Path = Path().apply {
        val line = { p: Offset -> lineTo(p.x, p.y) }
        val move = { p: Offset -> moveTo(p.x, p.y) }
        move(start.value); line(ende.value)
    }

    public fun aufPfad(pos: KartenPosition): Boolean {
        return TODO()
    }

    public companion object {
        public const val VERBINDUNG_ART: VerbindungArt = "default"
    }
}