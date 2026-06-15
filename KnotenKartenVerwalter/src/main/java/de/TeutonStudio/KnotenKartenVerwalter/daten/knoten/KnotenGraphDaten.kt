package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.Rechteck
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten

/** Datenvertrag für Knotenposition, Knotenmaße und freie Knotendaten. */
interface KnotenGraphDaten: GraphDaten {
    val name: String
    var breite: Float
    var tiefe: Float
    open val dimension: Rechteck get() = Rect(position,position + Offset(breite,tiefe))

    var position: KartenPosition
    var beweglich: Boolean
    val anschlussIdx: MutableMap<String, Int>
    val data: MutableMap<String, Any>
}
