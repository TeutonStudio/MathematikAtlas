package com.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Fachliche Beschreibung einer Verbindung zwischen zwei Anschlüssen.
 *
 * Quelle und Ziel werden über Knoten-ID und Anschluss-ID referenziert. Dadurch
 * bleiben Verbindungen stabil, auch wenn Knoten visuell verschoben oder
 * Anschlüsse neu gerendert werden.
 */
data class VerbindungDaten(
    val id: String,
    val quellKnotenId: String,
    val quellAnschlussId: String,
    val zielKnotenId: String,
    val zielAnschlussId: String,
    val label: String? = null,
    val typ: String = "default",
    val ausgewaehlt: Boolean = false,
)
