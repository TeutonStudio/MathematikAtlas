package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.times
import kotlin.math.pow

open class BasisVerbindung(
    _graph: Graph,
    override val daten: VerbindungDaten,
    override val start: State<KartenPosition>,
    override val ende: State<KartenPosition>,
): Verbindung(_graph) {
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
        val line = { p: IntOffset -> lineTo(p.x.toFloat(), p.y.toFloat()) }
        val move = { p: IntOffset -> moveTo(p.x.toFloat(), p.y.toFloat()) }
        val _start = start.value.zuBild()
        val _ende = ende.value.zuBild()
        move(_start)
        line(_ende)
    }

    public fun aufPfad(pos: KartenPosition): Boolean {
        return TODO()
    }

    public companion object {
        public const val VERBINDUNG_ART: VerbindungArt = "default"
    }
}