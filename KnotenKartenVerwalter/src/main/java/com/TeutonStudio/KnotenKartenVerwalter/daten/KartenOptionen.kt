package com.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Konfiguriert das Verhalten der Kartenoberflaeche.
 *
 * `KarteZustand` beschreibt den aktuellen Viewport. Diese Optionen beschreiben dagegen, welche
 * Interaktionen grundsaetzlich erlaubt sind und welche Grenzen fuer Zoom oder Raster gelten.
 */
data class KartenOptionen(
    /** Untere Zoomgrenze fuer Gesten und Kontrollleiste. */
    val minZoom: Float = 0.25f,

    /** Obere Zoomgrenze fuer Gesten und Kontrollleiste. */
    val maxZoom: Float = 3f,

    /** Rasterweite in Weltkoordinaten, falls Grid oder Snap-to-Grid aktiv sind. */
    val rasterGroesse: Float = 24f,

    /** Aktiviert oder deaktiviert alle direkten Benutzerinteraktionen auf der Karte. */
    val interaktiv: Boolean = true,

    /** Erlaubt das Verschieben des Viewports. */
    val panAktiv: Boolean = true,

    /** Erlaubt das Vergroessern und Verkleinern des Viewports. */
    val zoomAktiv: Boolean = true,

    /** Erlaubt das Ziehen von Knoten, sofern der einzelne Knoten ebenfalls beweglich ist. */
    val knotenDragAktiv: Boolean = true,

    /** Erlaubt das Erstellen neuer Verbindungen ueber Anschluss-Drag. */
    val verbindungsDragAktiv: Boolean = true,
)
