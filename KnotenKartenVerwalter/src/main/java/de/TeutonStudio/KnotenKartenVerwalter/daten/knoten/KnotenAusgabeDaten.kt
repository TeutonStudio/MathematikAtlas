package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.Rechteck
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.AusgabeKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt

open class KnotenAusgabeDaten(
    override val id: String,
    override val name: String,
): KnotenAnschlussDaten<EingangDaten>, KnotenRichtung<EingangDaten> {
    override var klasse: KnotenArt? = AusgabeKnoten.KNOTEN_ART
    override val dimension: Rechteck get() = Rect(position,position + Offset(breite,tiefe))
    override var breite: Float = 180f
    override var tiefe: Float = 96f

    override var beweglich: Boolean = true
    override var position: KartenPosition by mutableStateOf(KartenPosition.Zero)
    override val anschlussIdx = mutableStateMapOf<String, Int>()
    override val data: MutableMap<String, Any> = mutableMapOf()
    override val anschlussLabel: MutableMap<AnschlussKante, Pair<String, Int>> = mutableMapOf()
//    override val anschlüsse get() = erhalteAnschlüsse()
    override fun erzeugeAnschluss(
        id: String,
        kante: AnschlussKante,
        label: String
    ) = EingangDaten(id,kante,label)
    override fun erzeugeAnschlussId(knotenId: String, idx: Int) = id(knotenId,idx)
//    override val anschlüsse: SnapshotStateList<EingangDaten>
//    override val anschlüsse get() = erhalteAnschlüsse()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        breite: Float? = null,
        tiefe: Float? = null,
        beweglich: Boolean? = null,
        anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>>? = null,
        data: MutableMap<String, Any>? = null,
    ): this(
        id,
        name,
    ) {
        this.position = position ?: this.position
        this.breite = breite ?: this.breite
        this.tiefe = tiefe ?: this.tiefe
        this.beweglich = beweglich ?: this.beweglich
        this.anschlussLabel.clear()
        this.anschlussLabel.putAll(anschlussLabel ?: this.anschlussLabel)
        this.data.clear()
        this.data.putAll(data ?: this.data)
    }

    public companion object {
        public fun id(id: String, idx: Int): String = "${id}-eingang-${idx}"
    }
}