package com.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Ziel eines Kontextaufrufs auf der Karte.
 *
 * Diese Struktur trennt Hit-Testing von konkreten Menueeintraegen. Das Modul meldet nur, worauf
 * der Benutzer geklickt hat; die App kann daraus eigene Aktionen und Menues ableiten.
 */
sealed class KartenKontextZiel {
    /** Kontextaktion auf dem freien Kartenhintergrund. */
    data object Hintergrund : KartenKontextZiel()

    /** Kontextaktion auf einem Knoten. */
    data class Knoten(val knotenId: String) : KartenKontextZiel()

    /** Kontextaktion auf einem Anschluss eines Knotens. */
    data class Anschluss(
        val knotenId: String,
        val anschlussId: String,
        val richtung: AnschlussRichtung,
    ) : KartenKontextZiel()

    /** Kontextaktion auf einer Verbindung. */
    data class Verbindung(val verbindungId: String) : KartenKontextZiel()
}
