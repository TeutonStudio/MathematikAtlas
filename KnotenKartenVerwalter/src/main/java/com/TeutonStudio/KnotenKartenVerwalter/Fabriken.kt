package com.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.runtime.State
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisKarte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Karte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Verbindung
import kotlin.collections.get


// Verbindung

typealias VerbindungArt = String
typealias VerbindungFabrik = Map<VerbindungArt,VerbindungKonstruktor>
typealias VerbindungKonstruktor = (graph: Graph, daten: VerbindungDaten, start: State<KartenPosition>, ende: State<KartenPosition>) -> Verbindung

public fun VerbindungFabrik.erzeugeVerbindung(
    graph: Graph, daten: VerbindungDaten,
    start: State<KartenPosition>, ende: State<KartenPosition>,
): Verbindung? = this[daten.klasse]?.invoke(graph,daten,start,ende)

// Anschluss

typealias AnschlussArt = String
typealias AnschlussFabrik = Map<AnschlussArt,AnschlussKonstruktor>
typealias AnschlussKonstruktor = (graph: Graph, daten: AnschlussDaten, besitzer: Knoten) -> Anschluss

public fun AnschlussFabrik.erzeugeAnschluss(graph: Graph, daten: AnschlussDaten, besitzer: Knoten): Anschluss? = this[daten.klasse]?.invoke(graph,daten,besitzer)

// Knoten

typealias KnotenArt = String
typealias KnotenFabrik = Map<KnotenArt,KnotenKonstruktor>
typealias KnotenKonstruktor = (graph: Graph, daten: KnotenDaten,besitzer: Karte) -> Knoten

public fun KnotenFabrik.erzeugeKnoten(graph: Graph, daten: KnotenDaten,besitzer: Karte): Knoten? = this[daten.klasse]?.invoke(graph,daten,besitzer)

// Karten

typealias KartenArt = String
typealias KartenFabrik = Map<KartenArt,KartenKonstruktor>
typealias KartenKonstruktor = (
    graph: Graph,
    daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
    onKontextAktion: KontextAktionAusführen,
    onAuswahlÄndern: AuswahlÄndern,
) -> Karte

fun KartenFabrik.erzeugeKarte(
    graph: Graph, daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
    onKontextAktion: KontextAktionAusführen,
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
        onKontextAktion,
        onAuswahlÄndern,
    )
}
