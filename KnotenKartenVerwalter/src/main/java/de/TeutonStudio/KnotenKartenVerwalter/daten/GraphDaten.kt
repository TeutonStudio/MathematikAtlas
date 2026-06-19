package de.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.Rechteck
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKonstruktor
//import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten


typealias GraphDatenId = String

interface GraphDaten{
    public val id: GraphDatenId
    public var klasse: String?

    interface benanntesGD: GraphDaten {
        val name: String
    }
    interface bewegbareGD: GraphDaten {
        public var position: KartenPosition

        public fun verschiebeKnoten(delta: Offset) { position += delta }
    }
    interface orthogoneGD: GraphDaten {
        public var breite: Float
        public var tiefe: Float
        public val dimension get() = Rect(if (this is bewegbareGD) position else Offset.Zero,Size(breite,tiefe))
    }
}
