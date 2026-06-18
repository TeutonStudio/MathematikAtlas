package de.TeutonStudio.KnotenKartenVerwalter.daten.karte

import androidx.compose.runtime.mutableStateListOf
import de.TeutonStudio.KnotenKartenVerwalter.Rechteck
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.BasisKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenArt

open class KarteDaten(
    override val id: GraphDatenId,
    public val name: String,
    /* TODO größe des Graphs */
    public val größe: Rechteck? = null,
    initialKnoten: List<AnschlussKnotenDaten> = emptyList(),
    initialVerbindungen: List<VerbindungDaten> = emptyList(),
): GraphDaten {
    override var klasse: KartenArt? = BasisKarte.KARTEN_ART

    public val knoten = mutableStateListOf<AnschlussKnotenDaten>().apply { addAll(initialKnoten) }
    public val verbindungen = mutableStateListOf<VerbindungDaten>().apply { addAll(initialVerbindungen) }

    constructor(
        daten: KarteDaten,
        id: String? = null,
        name: String? = null,
        größe: Rechteck? = null,
        initialKnoten: List<AnschlussKnotenDaten>? = null,
        initialVerbindungen: List<VerbindungDaten>? = null,
    ): this(
        id ?: daten.id,
        name ?: daten.name,
        größe ?: daten.größe,
    ) {

    }

}
