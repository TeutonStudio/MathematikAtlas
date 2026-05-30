package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset

/**
 * Beschreibt Bearbeitungsaktionen, die durch Tastatur oder Kontextmenue entstehen koennen.
 *
 * Die Aktionen enthalten nur Absicht und Ziel. Die eigentliche Aenderung an `KarteDaten` bleibt im
 * aufrufenden Code, damit Undo/Redo und Persistenz dort sauber kontrolliert werden koennen.
 */
sealed class KartenBearbeitungsAktion {
    /** Entfernt die aktuell ausgewaehlten Elemente. */
    data class AuswahlLoeschen(val auswahl: AuswahlDaten) : KartenBearbeitungsAktion()

    /** Kopiert die aktuell ausgewaehlten Elemente in eine fachliche Zwischenablage. */
    data class AuswahlKopieren(val auswahl: AuswahlDaten) : KartenBearbeitungsAktion()

    /** Fuegt eine vorher kopierte Auswahl an einer Zielposition wieder ein. */
    data class Einfuegen(val zielPosition: Offset) : KartenBearbeitungsAktion()

    /** Bricht die laufende Interaktion ab, zum Beispiel Verbindungserstellung oder Auswahlrechteck. */
    data object Abbrechen : KartenBearbeitungsAktion()

    /** Fordert den aufrufenden Code auf, den letzten Schritt rueckgaengig zu machen. */
    data object Rueckgaengig : KartenBearbeitungsAktion()

    /** Fordert den aufrufenden Code auf, einen rueckgaengig gemachten Schritt erneut auszufuehren. */
    data object Wiederholen : KartenBearbeitungsAktion()
}
