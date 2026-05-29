package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset

data class AnsichtsfensterDaten(
    val x: Float = 0f,
    val y: Float = 0f,
    val zoom: Float = 1f,
)

data class KarteDaten(
    val id: String,
    val name: String,
    val knoten: List<KnotenDaten> = emptyList(),
    val verbindungen: List<VerbindungDaten> = emptyList(),
    val ansichtsfenster: AnsichtsfensterDaten = AnsichtsfensterDaten(),
)

data class KarteZustand(
    val verschiebung: Offset = Offset.Zero,
    val zoom: Float = 1f,
    val zeigeÜbersicht: Boolean = false,
    val zeigeKontrollLeiste: Boolean = false,
)
