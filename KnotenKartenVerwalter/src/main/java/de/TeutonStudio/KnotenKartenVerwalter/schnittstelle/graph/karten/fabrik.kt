package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import de.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import de.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import de.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKarte
//import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektKarte
//import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Zustand

/** Schlüssel einer Kartenimplementierung in der [KartenFabrik]. */
typealias KartenArt = String

/** Fabrikvertrag für Kartenimplementierungen. */
typealias KartenFabrik = Map<KartenArt,KartenKonstruktor>

/** Konstruktorfunktion einer konkreten [Karte]. */
typealias KartenKonstruktor = (
    graph: Graph,
    daten: GraphDatenKarte,
    zustand: Zustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
    onAuswahlÄndern: AuswahlÄndern,
) -> GraphDatenObjektKarte<*>

/**
 * Erzeugt die zur Datenklasse passende Karte.
 *
 * @receiver Fabrikzuordnung der bekannten Kartenarten
 * @throws IllegalStateException wenn keine passende Kartenklasse registriert ist
 */
public fun KartenFabrik.erzeugeKarte(
    graph: Graph, daten: GraphDatenKarte,
    zustand: Zustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
    onAuswahlÄndern: AuswahlÄndern,
): GraphDatenObjektKarte<*> {
    val klasse = daten.klasse ?: BasisObjektKarte.KARTEN_ART
    val konstruktor = this[klasse] ?: error("Keine Kartenklasse '$klasse'. Bekannte Klassen: ${keys.joinToString()}")

    return konstruktor(
        graph,
        daten,
        zustand,
        aktualisierung,
        onVerbindungErstellen,
        onAuswahlÄndern,
    )
}
@Suppress("UNCHECKED_CAST")
val BasisKartenFabrik: KartenFabrik = mapOf(
    BasisObjektKarte.KARTEN_ART to ::BasisObjektKarte as KartenKonstruktor,
)
