package de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt


open class BasisKnotenDaten(
    override val id: GraphDatenId,
    override val name: String = "",
): GraphDatenKnoten {
    override var klasse: KnotenArt? = BasisObjektKnoten.KNOTEN_ART

    override val dimension: Rect get() = Rect(position,position + Offset(breite,tiefe))
    override var breite: Float = 180f
    override var tiefe: Float = 96f
    override var beweglich: Boolean = true
    override var position: GraphPosition by mutableStateOf(GraphPosition.Zero)
    override val anschlüsse = mutableStateListOf<GraphDatenAnschluss>()
    override val anschlussIdx = mutableStateMapOf<String, Int>()
    override val data: MutableMap<String, Any> = mutableMapOf()

    constructor(
        id: String,
        name: String,
        anschlüsse: MutableMap<GraphDatenAnschluss,Int>? = null,
        position: GraphPosition? = null,
        breite: Float? = null,
        tiefe: Float? = null,
        beweglich: Boolean? = null,
        data: MutableMap<String, Any>? = null,
    ): this(id,name) {
        this.position = position ?: this.position
        this.breite = breite ?: this.breite
        this.tiefe = tiefe ?: this.tiefe
        this.beweglich = beweglich ?: this.beweglich
        this.anschlüsse.addAll(anschlüsse?.keys ?: this.anschlüsse)
        this.data.clear()
        this.data.putAll(data ?: this.data)
    }
}