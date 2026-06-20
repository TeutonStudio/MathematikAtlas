package de.TeutonStudio.KnotenKartenVerwalter.daten.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size


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
    }
}
