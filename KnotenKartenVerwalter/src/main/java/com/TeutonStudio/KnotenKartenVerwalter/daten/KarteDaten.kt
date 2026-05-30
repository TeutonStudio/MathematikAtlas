package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset

/**
 * Persistierter Viewport einer Karte.
 *
 * `x` und `y` beschreiben die Verschiebung des Weltkoordinatensystems in
 * Bildschirmkoordinaten. `zoom` beschreibt den Skalierungsfaktor zwischen Welt-
 * und Bildschirmkoordinaten.
 */
data class AnsichtsfensterDaten(
    val x: Float = 0f,
    val y: Float = 0f,
    val zoom: Float = 1f,
)

/**
 * Vollständiger fachlicher Zustand einer Knotenkarte.
 *
 * Die Klasse ist bewusst immutable gehalten: Änderungen an Knoten, Verbindungen
 * oder dem Viewport werden durch `copy(...)` erzeugt und vom aufrufenden Code
 * kontrolliert.
 */
data class KarteDaten(
    val id: String,
    val name: String,
    val knoten: List<KnotenDaten> = emptyList(),
    val verbindungen: List<VerbindungDaten> = emptyList(),
    val ansichtsfenster: AnsichtsfensterDaten = AnsichtsfensterDaten(),
)

/**
 * Laufzeit-Zustand der Kartenansicht.
 *
 * Anders als [AnsichtsfensterDaten] enthält dieser Zustand zusätzlich UI-Flags
 * für Hilfselemente. Die Verschiebung liegt in Bildschirmkoordinaten, während
 * Knotenpositionen weiterhin in Weltkoordinaten gespeichert werden.
 */
data class KarteZustand(
    val verschiebung: Offset = Offset.Zero,
    val zoom: Float = 1f,
    val zeigeÜbersicht: Boolean = false,
    val zeigeKontrollLeiste: Boolean = false,
)
