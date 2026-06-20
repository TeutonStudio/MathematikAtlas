package de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert

import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BezierObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.VerbindungArt

class BasisDatenVerbindung(
    override val id: GraphDatenId,
    override val ids: GraphDatenVerbindung.IDEhe,
    override var label: String,
    override val fehler: String?
): GraphDatenVerbindung {
    override var klasse: VerbindungArt? = BezierObjektVerbindung.VERBINDUNG_ART
}