package de.TeutonStudio.KnotenKartenVerwalter.daten

enum class AnschlussRichtung { Neutral, Eingang, Ausgang }
enum class AnschlussKante { Links, Rechts, Oben, Unten }

data class AnschlussArt(
    val id: AnschlussArtId,
    val name: String,
    val elternArt: AnschlussArtId? = null,
    val beschreibung: String = "",
)

/** Abbildung der effektiven Eingangsart auf die daraus folgende Ausgangsart. */
data class AnschlussArtAbbildung(
    val eingang: String,
    val abbildung: Map<AnschlussArtId, AnschlussArtId>,
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
    /**
     * Name eines Eingangs desselben Knotens, dessen effektive Art dieser Anschluss übernimmt.
     * Ohne verbundene Quelle bleibt [art] der konservative Fallback.
     */
    val artFolgtEingang: String? = null,
    /**
     * Namen mehrerer Eingänge desselben Knotens, deren effektive Arten zur kleinsten gemeinsamen
     * Oberart vereinigt werden. Ohne verbundene Quellen oder gemeinsame Oberart bleibt [art] der Fallback.
     */
    val artVereinigtEingänge: List<String> = emptyList(),
    /** Explizite zulässige Quellarten für einen Eingang; Unterarten werden ebenfalls akzeptiert. */
    val zulässigeArten: Set<AnschlussArtId> = emptySet(),
    /** Typabhängige Ausgangsart, zentral aus einem verbundenen Eingang berechnet. */
    val artAbbildungVonEingang: AnschlussArtAbbildung? = null,
)

data class AnschlussVerweis(val knotenId: KnotenId, val anschlussId: AnschlussId)
