package de.TeutonStudio.KnotenKartenVerwalter.daten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BezierObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussArt

class BasisDatenVerbindung(
    override val id: GraphDatenId,
    override val ids: GraphDatenVerbindung.IDEhe,
    override var label: String,
    override val fehler: String?
): GraphDatenVerbindung {
    override var klasse: AnschlussArt? = BezierObjektVerbindung.VERBINDUNG_ART
}