package de.TeutonStudio.KnotenKartenVerwalter.daten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BezierVerbindung

class BasisDatenVerbindung(
    override val id: GraphDatenId,
    override val ids: GraphDatenVerbindung.IDEhe,
    override var label: String,
    override val fehler: String?
): GraphDatenVerbindung {
    override var klasse: AnschlussArt? = BezierVerbindung.VERBINDUNG_ART
}