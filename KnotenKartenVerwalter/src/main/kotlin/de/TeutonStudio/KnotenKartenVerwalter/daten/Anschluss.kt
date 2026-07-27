package de.TeutonStudio.KnotenKartenVerwalter.daten

enum class AnschlussRichtung { Neutral, Eingang, Ausgang }
enum class AnschlussKante { Links, Rechts, Oben, Unten }

data class AnschlussArt(
    val id: AnschlussArtId,
    val name: String,
    val elternArt: AnschlussArtId? = null,
    val beschreibung: String = "",
)

data class AnschlussDaten(
    val id: AnschlussId = neueAnschlussId(),
    val name: String,
    val richtung: AnschlussRichtung = AnschlussRichtung.Neutral,
    val kante: AnschlussKante,
    val art: AnschlussArtId,
    val reihenfolge: Int = 0,
    /** Erlaubt dem Editor, beim Ziehen einer kompatiblen Verbindung einen weiteren Eingang anzubieten. */
    val kannSichErweitern: Boolean = false,
    /** Kennzeichnet einen vom Editor erzeugten, nur bei bestehender Verbindung erhaltenen Eingang. */
    val dynamischErzeugt: Boolean = false,
)

data class AnschlussVerweis(val knotenId: KnotenId, val anschlussId: AnschlussId)
