package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
//import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungArt
import de.TeutonStudio.KnotenKartenVerwalter.times
import kotlin.math.pow

open class BasisObjektVerbindung(
    override val graph: Graph,
    override val daten: GraphDatenVerbindung,
    override val start: State<KartenPosition>,
    override val ende: State<KartenPosition>,
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
    override fun BoxScope.KontextFenster(pos: BildschirmPosition) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }

    public override var startKante: Kante = Kante.Rechts
    public override var endeKante: Kante = Kante.Links


    public fun abstand(pos: KartenPosition): Offset {
        val delta = ende.value - start.value
        val distSq = delta.x.pow(2) + delta.y.pow(2)
        val t = (if (distSq != 0f) ((pos - start.value)* delta) / distSq else 0f).coerceIn(0f,1f)
        val nearest = start.value + delta * t
        val diff = pos - nearest
        return diff
    }

    public override fun erhaltePfad(): Path = Path().apply {
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
