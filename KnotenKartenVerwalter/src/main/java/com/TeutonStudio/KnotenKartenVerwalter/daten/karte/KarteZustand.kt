package com.TeutonStudio.KnotenKartenVerwalter.daten.karte

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten

open class KarteZustand(
//    ansicht: AnsichtsfensterDaten = StandardAnsicht(),
    val zeigeÜbersicht: Boolean = false,
    val zeigeKontrollLeiste: Boolean = false,
    val auswahl: MutableState<AuswahlDaten> = mutableStateOf(AuswahlDaten())
) {
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
    public fun verschiebe(delta: Offset) { pos += delta }
    public fun zoome(delta: Float) { zoom += delta }
    public fun transformiere(verschiebung: Offset,zoom: Float) { verschiebe(verschiebung); zoome(zoom) }

    public fun erhalteTransformiert(von: KartenPosition): BildschirmPosition = (von + pos * zoom).round()
    public fun erhalteUntransformiert(von: BildschirmPosition): KartenPosition = von.toOffset() - pos * zoom
}