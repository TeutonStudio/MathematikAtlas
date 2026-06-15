package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.Rechteck
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.EingabeKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt

open class KnotenEingabeDaten(
    override val id: String,
    override val name: String,
): KnotenAnschlussDaten<AusgangDaten>, KnotenRichtung<AusgangDaten> {
    override var klasse: KnotenArt? = EingabeKnoten.KNOTEN_ART
    override val dimension: Rechteck get() = Rect(position,position + Offset(breite,tiefe))
    override var breite: Float = 180f
    override var tiefe: Float = 96f

    override var beweglich: Boolean = true
    override var position: KartenPosition by mutableStateOf(KartenPosition.Zero)
    override val anschlussIdx = mutableStateMapOf<String, Int>()
    override val data: MutableMap<String, Any> = mutableMapOf()
    override val anschlussLabel = mutableMapOf<AnschlussKante, Pair<String, Int>>()

    public override fun erzeugeAnschluss(
        id: String,
        kante: AnschlussKante,
        label: String
    ) = AusgangDaten(id,kante,label)

    public override fun erzeugeAnschlussId(knotenId: String, idx: Int): String = "${knotenId}-ausgang-${idx}"

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
        public fun id(id: String, idx: Int): String = "${id} out ${idx}"
    }
}
