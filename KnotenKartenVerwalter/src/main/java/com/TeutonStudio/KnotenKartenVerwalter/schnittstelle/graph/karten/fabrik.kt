package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph

typealias KartenArt = String
typealias KartenFabrik = Map<KartenArt,KartenKonstruktor>
typealias KartenKonstruktor = (
    graph: Graph,
    daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
//    onKontextAktion: KontextAktionAusführen,
    onAuswahlÄndern: AuswahlÄndern,
) -> Karte

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
