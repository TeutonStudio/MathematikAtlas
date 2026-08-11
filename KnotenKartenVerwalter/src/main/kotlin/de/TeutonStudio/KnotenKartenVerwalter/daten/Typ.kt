package de.TeutonStudio.KnotenKartenVerwalter.daten

@JvmInline
value class TypId(val wert: String) {
    init { require(wert.isNotBlank()) { "Eine Typ-ID darf nicht leer sein." } }
    override fun toString(): String = wert
}

@JvmInline
value class TypVariablenId(val wert: String) {
    init { require(wert.isNotBlank()) { "Eine Typvariablen-ID darf nicht leer sein." } }
    override fun toString(): String = wert
}

/**
 * Neutraler, serialisierbarer Typausdruck des Graphkerns.
 *
 * Der Ausdruck ist absichtlich nicht auf Mathematik beschränkt. Mathematik,
 * Godot und spätere Domänen registrieren ihre atomaren und parametrisierten
 * Typen im selben [de.TeutonStudio.KnotenKartenVerwalter.logik.TypSystem].
 */
sealed interface TypAusdruck {
    /** Jeder konkrete Typ ist zulässig. Nicht mit [Unbekannt] verwechseln. */
    data object Beliebig : TypAusdruck

    /** Der Typ ist noch nicht bekannt bzw. kann aktuell nicht inferiert werden. */
    data object Unbekannt : TypAusdruck

    data class Atom(val id: TypId) : TypAusdruck

    data class Parameterisiert(
        val konstruktor: TypId,
        val argumente: List<TypAusdruck>,
    ) : TypAusdruck {
        init { require(argumente.isNotEmpty()) { "Ein parametrisierter Typ benötigt Argumente." } }
    }

    /** Echte Typalternative (Oder-Typ), nicht bloß eine Liste von UI-Farben. */
    data class Vereinigung(val alternativen: List<TypAusdruck>) : TypAusdruck {
        init { require(alternativen.isNotEmpty()) { "Eine Typvereinigung benötigt Alternativen." } }
    }

    data class Variable(val id: TypVariablenId) : TypAusdruck
}

enum class TypAnforderungsArt {
    Struktur,
    Eigenschaft,
    Axiom,
}

/**
 * Zusätzliche fachliche Forderung an einen Wert. G0.2 transportiert und
 * persistiert diese Anforderungen; die fachliche Auswertung folgt in G0.3.
 */
data class TypAnforderung(
    val art: TypAnforderungsArt,
    val id: TypId,
)

data class AnschlussVertrag(
    val typ: TypAusdruck = TypAusdruck.Unbekannt,
    val anforderungen: Set<TypAnforderung> = emptySet(),
)

data class TypAbbildungsFall(
    val von: TypAusdruck,
    val zu: TypAusdruck,
)

/**
 * Semantische Typinferenz eines Anschlusses. Sie ergänzt die historischen
 * AnschlussArt-Regeln, damit alte Karten während der Migration unverändert
 * funktionieren.
 */
sealed interface TypInferenzRegel {
    data class FolgtEingang(val eingang: String) : TypInferenzRegel

    data class GemeinsameOberart(val eingänge: List<String>) : TypInferenzRegel {
        init { require(eingänge.isNotEmpty()) { "Die Typinferenz benötigt Eingänge." } }
    }

    data class VereinigungAusEingängen(val eingänge: List<String>) : TypInferenzRegel {
        init { require(eingänge.isNotEmpty()) { "Die Typvereinigung benötigt Eingänge." } }
    }

    data class AbbildungVonEingang(
        val eingang: String,
        val fälle: List<TypAbbildungsFall>,
    ) : TypInferenzRegel {
        init { require(fälle.isNotEmpty()) { "Eine Typabbildung benötigt mindestens einen Fall." } }
    }

    data class Priorisierung(
        val eingänge: List<String>,
        val prioritäten: List<TypAusdruck>,
    ) : TypInferenzRegel {
        init {
            require(eingänge.isNotEmpty()) { "Eine Typpriorisierung benötigt Eingänge." }
            require(prioritäten.isNotEmpty()) { "Eine Typpriorisierung benötigt Prioritäten." }
        }
    }

    data class TupelAusEingängen(
        val eingänge: List<String>,
        val konstruktor: TypId = TypId("mathematik.tupel"),
    ) : TypInferenzRegel {
        init { require(eingänge.isNotEmpty()) { "Ein Tupeltyp benötigt Eingänge." } }
    }

    data class KomponenteAusEingang(
        val eingang: String,
        val index: Int,
        val konstruktor: TypId? = null,
    ) : TypInferenzRegel {
        init { require(index >= 0) { "Ein Typkomponentenindex darf nicht negativ sein." } }
    }
}

/** Konservativer semantischer Fallback aus der bestehenden Anschlusskategorie. */
fun AnschlussDaten.deklarierterTyp(): TypAusdruck =
    vertrag.typ.takeUnless { it == TypAusdruck.Unbekannt }
        ?: TypAusdruck.Atom(TypId(art.wert))
