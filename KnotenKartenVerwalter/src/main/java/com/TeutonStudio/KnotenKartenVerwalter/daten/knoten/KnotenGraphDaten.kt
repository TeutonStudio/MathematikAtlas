package com.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten

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