package com.TeutonStudio.KnotenKartenVerwalter.daten.karte

import androidx.compose.runtime.mutableStateListOf
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.BasisKarte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenArt

open class KarteDaten(
    override val id: String,
    public val name: String,
    public val größe: Rechteck? = null, // TODO größe des Graphs
    initialKnoten: List<AnschlussKnotenDaten> = emptyList(),
    initialVerbindungen: List<VerbindungDaten> = emptyList(),
): GraphDaten {
    override val klasse: KartenArt? = BasisKarte.KARTEN_ART

//    public val cache: KartenCacheDaten = KartenCacheDaten()
    public val knoten = mutableStateListOf<AnschlussKnotenDaten>().apply { addAll(initialKnoten) }
    public val verbindungen = mutableStateListOf<VerbindungDaten>().apply { addAll(initialVerbindungen) }
//    public var ansicht: AnsichtsfensterDaten = StandardAnsicht()

    constructor(
        daten: KarteDaten,
        id: String? = null,
        name: String? = null,
        größe: Rechteck? = null,
        initialKnoten: List<AnschlussKnotenDaten>? = null,
        initialVerbindungen: List<VerbindungDaten>? = null,
//        ansichtsfenster: AnsichtsfensterDaten? = null,
//        cache: KartenCacheDaten? = null,
    ): this(
        id ?: daten.id,
        name ?: daten.name,
        größe ?: daten.größe,
//        initialKnoten ?: daten.initialKnoten,
//        initialVerbindungen ?: daten.initialVerbindungen,
    ) {
//        this.ansicht = ansichtsfenster ?: daten.ansicht
//        this.cache = cache ?: daten.cache // TODO

    }

}