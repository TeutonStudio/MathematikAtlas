package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import de.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import de.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import de.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph

/** Schlüssel einer Kartenimplementierung in der [KartenFabrik]. */
typealias KartenArt = String

/** Fabrikvertrag für Kartenimplementierungen. */
typealias KartenFabrik = Map<KartenArt,KartenKonstruktor>

/** Konstruktorfunktion einer konkreten [Karte]. */
typealias KartenKonstruktor = (
    graph: Graph,
    daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
//    onKontextAktion: KontextAktionAusführen,
    onAuswahlÄndern: AuswahlÄndern,
) -> Karte

/**
 * Erzeugt die zur Datenklasse passende Karte.
 *
 * @receiver Fabrikzuordnung der bekannten Kartenarten
 * @throws IllegalStateException wenn keine passende Kartenklasse registriert ist
 */
fun KartenFabrik.erzeugeKarte(
    graph: Graph, daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
//    onKontextAktion: KontextAktionAusführen,
    onAuswahlÄndern: AuswahlÄndern,
): Karte {
    val klasse = daten.klasse ?: BasisKarte.KARTEN_ART

    val konstruktor = this[klasse]
        ?: error("Keine Kartenklasse '$klasse'. Bekannte Klassen: ${keys.joinToString()}")

    return konstruktor(
        graph,
        daten,
        zustand,
        aktualisierung,
        onVerbindungErstellen,
//        onKontextAktion,
        onAuswahlÄndern,
    )
}
@Suppress("UNCHECKED_CAST")
val BasisKartenFabrik: KartenFabrik = mapOf(
    BasisKarte.KARTEN_ART to ::BasisKarte as KartenKonstruktor,
)
