package com.TeutonStudio.KnotenKartenVerwalter.daten.karte

import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.EinzelAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.MultiAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph

open class KarteZustand(
//    ansicht: AnsichtsfensterDaten = StandardAnsicht(),
    val zeigeÜbersicht: Boolean = false,
    val zeigeKontrollLeiste: Boolean = false,
    val auswahl: MutableState<AuswahlDaten> = mutableStateOf(AuswahlDaten.LEER)
) {
    var dimension by mutableStateOf(IntSize.Zero)
    var zoom by mutableFloatStateOf(1f)
    var pos by mutableStateOf(Offset.Zero)

    constructor(
        zustand: KarteZustand,
        zeigeÜbersicht: Boolean? = null,
        zeigeKontrollLeiste: Boolean? = null,
        auswahl: AuswahlDaten? = null,
    ): this(
        zeigeÜbersicht ?: zustand.zeigeÜbersicht,
        zeigeKontrollLeiste ?: zustand.zeigeKontrollLeiste,
    ) {
        this.auswahl.value = auswahl ?: zustand.auswahl.value
        this.pos = pos ?: zustand.pos
        this.zoom = zoom ?: zustand.zoom
    }

//    fun verschiebeBildschirm(delta: Offset)

    fun setzeZoom(neuerZoom: Float, fokus: Offset) {
        val MIN_ZOOM = 0.2f
        val MAX_ZOOM = 200.0f
        val begrenzt = neuerZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val faktor = begrenzt / zoom

        pos = fokus - (fokus - pos) * faktor
        zoom = begrenzt
    }

/*    fun zentriereAuf(
        weltPosition: Offset,
    )*/

/*    fun passeInhaltEin(
        inhalt: Rect,
        rand: Float = 64f,
    )*/

    public fun verschiebe(delta: Offset) { pos += delta }
    public fun zoome(delta: Float) { zoom = (zoom * delta).coerceIn(.05f,5f) }
    public fun transformiere(verschiebung: Offset,zoom: Float) { verschiebe(verschiebung); zoome(zoom) }

    public fun erhalteTransformiert(von: KartenPosition): BildschirmPosition = (von * zoom + pos).round()

//    public fun erhalteUntransformiert(von: BildschirmPosition): KartenPosition = (von.toOffset() - pos) / zoom

    public fun erhalteDeltaUntransformiert(delta: Offset): Offset = delta / zoom

    public fun erhalteViewportRect(
        breite: Float = dimension.width.toFloat(),
        höhe: Float = dimension.height.toFloat(),
        puffer: Float = 200f,
    ): RectF {
        val links = (-pos.x) / zoom - puffer
        val oben = (-pos.y) / zoom - puffer
        val rechts = (breite - pos.x) / zoom + puffer
        val unten = (höhe - pos.y) / zoom + puffer

        return RectF(links, oben, rechts, unten)
    }

}