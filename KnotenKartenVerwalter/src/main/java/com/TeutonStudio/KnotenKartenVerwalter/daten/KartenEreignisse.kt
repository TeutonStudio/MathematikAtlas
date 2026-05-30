package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset

/**
 * Buendelt alle Rueckmeldungen, die die Kartenoberflaeche an den aufrufenden Code senden kann.
 *
 * Die Datenmutation bleibt bewusst ausserhalb des Moduls. Das Modul rendert den aktuellen Zustand
 * und meldet Benutzeraktionen, waehrend die App entscheidet, wie daraus ein neuer `KarteDaten`-
 * Zustand entsteht.
 */
data class KartenEreignisse(
    /** Meldet, dass sich der sichtbare Kartenausschnitt durch Pan, Zoom oder Minimap-Drag geaendert hat. */
    val onAnsichtAendern: (KarteZustand) -> Unit = {},

    /** Meldet eine neue Knotenposition waehrend oder nach einem Drag-Vorgang. */
    val onKnotenPositionAendern: (knotenId: String, position: Offset) -> Unit = { _, _ -> },

    /** Meldet, dass der Benutzer eine neue Verbindung zwischen zwei Anschluessen erstellt hat. */
    val onVerbindungErstellen: (VerbindungDaten) -> Unit = {},

    /** Meldet, dass sich die kontrollierte Auswahl von Knoten oder Verbindungen aendern soll. */
    val onAuswahlAendern: (AuswahlDaten) -> Unit = {},

    /** Meldet eine kontextbezogene Aktion, zum Beispiel durch Rechtsklick oder langes Druecken. */
    val onKontextAktion: (KartenKontextZiel, Offset) -> Unit = { _, _ -> },

    /** Meldet allgemeine Bearbeitungsaktionen wie Loeschen, Kopieren oder Rueckgaengig. */
    val onBearbeitungsAktion: (KartenBearbeitungsAktion) -> Unit = {},
)
