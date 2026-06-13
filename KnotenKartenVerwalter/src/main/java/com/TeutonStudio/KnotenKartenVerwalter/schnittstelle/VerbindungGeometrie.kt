package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.geometry.Offset

/**
 * Berechnete Geometrie einer Verbindung.
 *
 * Diese Struktur trennt fachliche Edge-Daten von der Darstellung. Dadurch koennen Hit-Tests,
 * Labels und verschiedene Edge-Typen dieselbe berechnete Geometrie wiederverwenden.
 */
/*data class VerbindungGeometrie(
    *//** Bildschirmposition des Quellanschlusses. *//*
    val start: Offset,

    *//** Bildschirmposition des Zielanschlusses. *//*
    val ende: Offset,

    *//** Bildschirmposition, an der ein Label gerendert werden soll. *//*
    val labelPosition: Offset = Offset(
        x = (start.x + ende.x) / 2f,
        y = (start.y + ende.y) / 2f,
    ),

    *//** Interaktionsbreite fuer Klicks und Kontextmenues auf der Verbindung. *//*
    val interaktionsBreite: Float = 12f,

    *//** Darstellungsart des Pfads, zum Beispiel Bezier, Gerade oder Stufe. *//*
    val pfadTyp: VerbindungPfadTyp = VerbindungPfadTyp.Bezier,
)*/

/** Unterstuetzte Pfadtypen fuer Verbindungen. */
/*enum class VerbindungPfadTyp {
    *//** Kubische Bezierkurve wie in der aktuellen Verbindungskomponente. *//*
    Bezier,

    *//** Direkte Linie zwischen Quelle und Ziel. *//*
    Gerade,

    *//** Rechtwinklige Stufenverbindung. *//*
    Stufe,

    *//** Weich abgerundete Stufenverbindung. *//*
    WeicheStufe,
}*/
