package com.TeutonStudio.KnotenKartenVerwalter.daten.karte

import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.BasisKarte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.KartenArt

open class KarteDaten(
    override val id: String,
    public val name: String,
    public val größe: Rechteck? = null, // TODO größe des Graphs
    public val initialKnoten: List<KnotenDaten> = emptyList(),
    public val initialVerbindungen: List<VerbindungDaten> = emptyList(),
): GraphDaten(id) {
    override val klasse: KartenArt? = BasisKarte.KARTEN_ART

//    public val cache: KartenCacheDaten = KartenCacheDaten()
    public val knoten: MutableList<KnotenDaten> = initialKnoten.toMutableList()
    public val verbindungen: MutableList<VerbindungDaten> = initialVerbindungen.toMutableList()
//    public var ansicht: AnsichtsfensterDaten = StandardAnsicht()

    constructor(
        daten: KarteDaten,
        id: String? = null,
        name: String? = null,
        größe: Rechteck? = null,
        initialKnoten: List<KnotenDaten>? = null,
        initialVerbindungen: List<VerbindungDaten>? = null,
//        ansichtsfenster: AnsichtsfensterDaten? = null,
//        cache: KartenCacheDaten? = null,
    ): this(
        id ?: daten.id,
        name ?: daten.name,
        größe ?: daten.größe,
        initialKnoten ?: daten.initialKnoten,
        initialVerbindungen ?: daten.initialVerbindungen,
    ) {
//        this.ansicht = ansichtsfenster ?: daten.ansicht
//        this.cache = cache ?: daten.cache // TODO

    }

}