package de.TeutonStudio.KnotenKartenVerwalter.daten.graph

import android.graphics.RectF
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density


typealias GraphDatenId = String
typealias GraphPosition = Offset

interface GraphDaten{
    public val id: GraphDatenId
    public var klasse: String?

    interface benanntesGD: GraphDaten {
        public val name: String
    }
    interface bewegbareGD: GraphDaten {
        public var position: GraphPosition

        public fun verschiebeKnoten(delta: Offset) { position += delta }
    }
    interface orthogoneGD: GraphDaten {
        public var breite: Float
        public var tiefe: Float
        public val dimension get() = Rect(if (this is bewegbareGD) position else Offset.Zero,Size(breite,tiefe))

        public fun dimModi(modifier: Modifier, dichte: Density, min: Offset) = modifier.width(with(dichte) { breite.coerceAtLeast(min.x).toDp() }).height(with(dichte) { tiefe.coerceAtLeast(min.y).toDp() })
    }

    public companion object {
        public fun Float.toOffset() = Offset(this,this)
        public fun List<GraphPosition>.minOffset(puffer: Float) = Offset(minOf { it.x },minOf { it.y }) - puffer.toOffset()
        public fun List<GraphPosition>.maxOffset(puffer: Float) = Offset(maxOf { it.x },maxOf { it.y }) + puffer.toOffset()
    }
}
