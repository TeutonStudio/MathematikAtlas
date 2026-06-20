package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung

class AussageAusgang(
    override val id: GraphDatenId,
    override val kante: Kante,
    override val richtung: Richtung,
): AussageAnschlussDaten(id,kante, Richtung.Ausgang) {
    public companion object {
        public const val ANSCHLUSS_ART = "outputAussage"
    }
}