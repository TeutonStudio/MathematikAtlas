package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.Rechteck
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenKarte

class KnotenKarteDaten(
    override val id: String,
    override val name: String,
): KnotenAnschlussDaten<RichtungsAnschlussDaten> {
    override var klasse: KnotenArt? = KnotenKarte.KNOTEN_ART

    override val dimension: Rechteck get() = Rect(position,position + Offset(breite,tiefe))
    override var breite: Float = 180f
    override var tiefe: Float = 96f

    override var beweglich: Boolean = true
    override var position: KartenPosition by mutableStateOf(KartenPosition.Zero)
    override val anschlüsse = mutableStateListOf<RichtungsAnschlussDaten>()
    override val anschlussIdx = mutableStateMapOf<String, Int>()
    override val data: MutableMap<String, Any> = mutableMapOf()

    /* TODO */
}
