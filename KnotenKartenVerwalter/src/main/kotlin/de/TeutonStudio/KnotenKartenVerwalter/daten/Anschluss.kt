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

/**
 * Wählt die effektive Ausgangsart aus mehreren Eingängen anhand einer geordneten
 * Prioritätenliste. Die erste Art, zu der mindestens eine verbundene Quellart
 * eine Unterart ist, gewinnt; ohne Treffer bleibt die deklarierte Anschlussart.
 */
data class AnschlussArtPriorisierung(
    val eingänge: List<String>,
    val prioritäten: List<AnschlussArtId>,
) {
    init {
        require(eingänge.isNotEmpty()) { "Eine Anschlussart-Priorisierung benötigt Eingänge." }
        require(prioritäten.isNotEmpty()) { "Eine Anschlussart-Priorisierung benötigt Prioritäten." }
    }
}

data class AnschlussDaten(
    val id: AnschlussId = neueAnschlussId(),
    val name: String,
    val richtung: AnschlussRichtung = AnschlussRichtung.Neutral,
    val kante: AnschlussKante,
    /**
     * Grobe Anschlusskategorie für Editor, Fallback und Legacykarten. Der
     * konkrete fachliche Werttyp liegt in [vertrag].
     */
    val art: AnschlussArtId,
    val reihenfolge: Int = 0,
    /** Semantischer Typ und orthogonale Struktur-/Eigenschaftsanforderungen. */
    val vertrag: AnschlussVertrag = AnschlussVertrag(),
    /** Optionale semantische Typinferenz aus anderen Anschlüssen desselben Knotens. */
    val typInferenz: TypInferenzRegel? = null,
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
    /** Typabhängige Ausgangsart, die eine Quellart über mehrere Eingänge priorisiert. */
    val artPriorisiertEingänge: AnschlussArtPriorisierung? = null,
)

data class AnschlussVerweis(val knotenId: KnotenId, val anschlussId: AnschlussId)
