package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.runtime.retain.retain
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

public fun KnotenDaten.zuIntOffset(zustand: KarteZustand): IntOffset = IntOffset(
    x = (this.position.waagrecht * zustand.zoom + zustand.verschiebung.x).roundToInt(),
    y = (this.position.senkrecht * zustand.zoom + zustand.verschiebung.y).roundToInt(),
)

public fun KnotenDaten.zuIntOffset(verschiebung: Offset): IntOffset = zuIntOffset(
    KarteZustand(verschiebung = verschiebung),
)

public operator fun Offset.div(other: KarteZustand): PositionDaten {
    val zoom = other.zoom.takeIf { it > 0f } ?: 1f
    return PositionDaten(this.x / zoom, this.y / zoom)
}

data class PositionDaten(
    val waagrecht: Float,
    val senkrecht: Float,
) {
    operator fun plus(other: PositionDaten) = PositionDaten(waagrecht+other.waagrecht,senkrecht+other.senkrecht)
    operator fun minus(other: PositionDaten) = PositionDaten(waagrecht-other.waagrecht,senkrecht-other.senkrecht)
}

data class FlächeDaten(
    val waagrecht: Float,
    val senkrecht: Float,
) {

}

data class KnotenDaten(
    val id: String,
    val name: String,
    val position: PositionDaten = PositionDaten(0f, 0f),
    val fläche: FlächeDaten = FlächeDaten(180f, 96f),
    val typ: String = "default",
    val eingänge: List<EingangDaten> = emptyList(),
    val ausgänge: List<AusgangDaten> = emptyList(),
    val ausgewaehlt: Boolean = false,
    val beweglich: Boolean = true,
//    val bewegt: Boolean = false, // Eher für schnittstelle relevant.
) {
    public fun erhalteAnschlüsseGeordnet(richtung: AnschlussRichtung): List<AnschlussDaten> = when(richtung) {
        AnschlussRichtung.Eingang -> eingängeGeordnet
        AnschlussRichtung.Ausgang -> ausgängeGeordnet
    }

    val eingängeGeordnet: List<EingangDaten>
        get() = eingänge

    val ausgängeGeordnet: List<AusgangDaten>
        get() = ausgänge
}
