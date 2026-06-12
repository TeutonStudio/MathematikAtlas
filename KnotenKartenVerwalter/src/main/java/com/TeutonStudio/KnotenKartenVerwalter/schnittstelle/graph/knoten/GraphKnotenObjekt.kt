package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import android.graphics.RectF
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.EinzelAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import com.TeutonStudio.KnotenKartenVerwalter.erhalteSize
import com.TeutonStudio.KnotenKartenVerwalter.overlaps
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss.Companion.findeNachId
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

interface GraphKnotenObjekt<K: KnotenAnschlussDaten<out AnschlussDaten>>: GraphDatenObjekt<K> {
    public abstract val besitzer: Karte
    public abstract val anschlussFabrik: AnschlussFabrik
    public val dimension get() = daten.erhalteSize()

    abstract fun definiereVerbindung()

    @Composable
    override fun Modifier.vorher(): Modifier = offset { daten.position.round() }.size(with(LocalDensity.current) { dimension.toDpSize() })


    override fun beiKlick(klickPos: Offset) {
        besitzer.wähle(EinzelAuswahl(this))
        besitzer.keinKontext()
    }

    override fun beiHalten(klickPos: Offset) {
        besitzer.ctx = daten.id to klickPos.zuBildAusKnoten()
        besitzer.wähle(EinzelAuswahl(this))
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        if (daten.beweglich) besitzer.verschiebeKnoten(daten.id,panDelta)
        besitzer.wähle(EinzelAuswahl(this))
        besitzer.keinKontext()
    }

    fun istImViewport(viewport: RectF = besitzer.zustand.erhalteViewportRect()): Boolean = RectF(
        daten.position.x,
        daten.position.y,
        daten.position.x + daten.breite,
        daten.position.y + daten.tiefe,
    ).overlaps(viewport)

    public fun KartenPosition.zuBildAusKnoten(): BildschirmPosition = (this + daten.position).round()

    public companion object {

        public fun Iterable<Knoten>.sichtbar() = filter { it.istImViewport() }

        public fun Iterable<Knoten>.findeNachId(id:String) = find { it.daten.id == id }
        public fun Iterable<Knoten>.anschlussNachId(idKnoten:String,idAnschluss:String) = findeNachId(idKnoten)?.anschlussNachId(idAnschluss)
        public fun Knoten.anschlussNachId(id:String) = anschlüsse.findeNachId(id)
        public fun Iterable<Knoten>.anschlüsseNachIDEhe(ids: IDEhe) =
            anschlussNachId(ids.knotenIdMann,ids.anschlussIdMann)?.let { aM ->
                anschlussNachId(ids.knotenIdWeib,ids.anschlussIdWeib)?.let { aW ->
                    aM to aW
                }
            }
    }
}