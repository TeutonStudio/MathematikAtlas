package com.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Datenpaket fuer Copy/Paste innerhalb einer Knotenkarte.
 *
 * Beim Einfuegen muessen IDs in der Regel neu vergeben werden. Deshalb speichert diese Struktur
 * nur die kopierten Elemente und die Ursprungsauswahl; die konkrete ID-Neuzuordnung gehoert in die
 * spaetere Einfuege-Logik.
 */
data class KartenZwischenablage(
    /** Kopierte Knoten mit ihren urspruenglichen IDs und Positionen. */
    val knoten: List<KnotenDaten> = emptyList(),

    /** Kopierte Verbindungen, die zwischen den kopierten oder bestehenden Knoten liegen koennen. */
    val verbindungen: List<VerbindungDaten> = emptyList(),

    /** Auswahl, aus der diese Zwischenablage erzeugt wurde. */
    val ursprungsAuswahl: AuswahlDaten = AuswahlDaten(),
)
