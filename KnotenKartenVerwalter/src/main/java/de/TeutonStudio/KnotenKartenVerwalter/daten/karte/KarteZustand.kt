package de.TeutonStudio.KnotenKartenVerwalter.daten.karte

import android.graphics.RectF
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphHintergrund.RasterArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphHintergrund.RasterTesselation

open class KarteZustand(
    zeigeÜbersicht: Boolean = false,
    zeigeKontrollLeiste: Boolean = false,
    val auswahl: MutableState<AuswahlDaten> = mutableStateOf(AuswahlDaten.LEER),
    rasterArt: RasterArt = RasterArt.Punkte,
    rasterTesselation: RasterTesselation = RasterTesselation.Quadgon,
) {
    public var zeigeÜbersicht by mutableStateOf(zeigeÜbersicht)

    public var zeigeKontrollLeiste by mutableStateOf(zeigeKontrollLeiste)
    var rasterEinstellung = Pair(rasterArt,rasterTesselation)

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

    fun setzeZoom(neuerZoom: Float, fokus: Offset) {
        val begrenzt = neuerZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val alterZoom = zoom.coerceAtLeast(0.0001f)
        val faktor = begrenzt / alterZoom

        pos = fokus - (fokus - pos) * faktor
        zoom = begrenzt
    }

    public fun setzeAnsicht(
        neuerZoom: Float,
        neuePosition: Offset,
    ) {
        zoom = neuerZoom.coerceIn(MIN_ZOOM,MAX_ZOOM)
        pos = neuePosition
    }

    public fun zentriereAuf(weltPosition: Offset) {
        pos = Offset(
            x = dimension.width / 2f - weltPosition.x * zoom,
            y = dimension.height / 2f - weltPosition.y * zoom,
        )
    }

    public fun verschiebe(delta: Offset) { pos += delta }
    public fun zoome(delta: Float) { zoom = (zoom * delta).coerceIn(MIN_ZOOM,MAX_ZOOM) }
    public fun transformiere(verschiebung: Offset,zoom: Float) { verschiebe(verschiebung); zoome(zoom) }

    public fun zuBild(kartePos: KartenPosition): BildschirmPosition = (pos + kartePos * zoom).round()
    public fun zuKarte(bildPos: BildschirmPosition): KartenPosition = (bildPos.toOffset() - pos) / zoom

//    public fun erhalteTransformiert(von: KartenPosition): BildschirmPosition = (von * zoom + pos).round()

//    public fun erhalteUntransformiert(von: BildschirmPosition): KartenPosition = (von.toOffset() - pos) / zoom

//    public fun erhalteDeltaUntransformiert(delta: Offset): Offset = delta / zoom

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

    public companion object {
        public const val MIN_ZOOM = 0.05f
        public const val MAX_ZOOM = 5f
    }
}