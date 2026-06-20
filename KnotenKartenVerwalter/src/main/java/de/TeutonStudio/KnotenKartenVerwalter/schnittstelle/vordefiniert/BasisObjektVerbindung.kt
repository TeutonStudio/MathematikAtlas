package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung.Companion.dot
import kotlin.math.pow

open class BasisObjektVerbindung(
    override val graph: Graph,
    override val daten: GraphDatenVerbindung,
    override val start: State<GraphPosition>,
    override val ende: State<GraphPosition>,
): GraphDatenObjektVerbindung<GraphDatenVerbindung> {
    public override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override fun beiKlick(klickPos: Offset) {
        TODO("Not yet implemented")
    }

    override fun beiHalten(klickPos: Offset) {
        TODO("Not yet implemented")
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Darstellung() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntSize) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }

    public override var startKante: Kante = Kante.Rechts
    public override var endeKante: Kante = Kante.Links


    public fun abstand(pos: GraphPosition): Offset {
        val delta = ende.value - start.value
        val distSq = delta.x.pow(2) + delta.y.pow(2)
        val t = (if (distSq != 0f) (delta.dot(pos - start.value)) / distSq else 0f).coerceIn(0f,1f)
        val nearest = start.value + delta * t
        val diff = pos - nearest
        return diff
    }

    public override fun erhaltePfad(): Path = Path().apply {
        val line = { p: Offset -> lineTo(p.x, p.y) }
        val move = { p: Offset -> moveTo(p.x, p.y) }
        move(start.value); line(ende.value)
    }

    public fun aufPfad(pos: GraphPosition): Boolean {
        return TODO()
    }

    public companion object {
        public const val VERBINDUNG_ART: VerbindungArt = "default"
    }
}
