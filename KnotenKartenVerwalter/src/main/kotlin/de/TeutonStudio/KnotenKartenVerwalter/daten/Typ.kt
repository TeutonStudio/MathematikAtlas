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
 * Fachneutraler semantischer Typausdruck eines Anschlusses.
 *
 * [Unbekannt] bedeutet, dass der Typ noch nicht hergeleitet werden konnte.
 * [Beliebig] bedeutet dagegen explizit, dass jeder semantische Typ zulässig ist.
 * Diese beiden Fälle dürfen bei der Typprüfung nicht zusammenfallen.
 */
sealed interface TypAusdruck {
    data object Beliebig : TypAusdruck
    data object Unbekannt : TypAusdruck

    data class Atom(val id: TypId) : TypAusdruck

    data class Parameterisiert(
        val konstruktor: TypId,
        val argumente: List<TypAusdruck>,
    ) : TypAusdruck

    data class Vereinigung(val alternativen: List<TypAusdruck>) : TypAusdruck {
        init { require(alternativen.isNotEmpty()) { "Eine Typvereinigung benötigt mindestens eine Alternative." } }
    }

    data class Variable(val id: TypVariablenId) : TypAusdruck
}

/**
 * Zusätzliche fachliche Forderung an einen Wert. Strukturen, Eigenschaften und
 * Axiome bleiben bewusst orthogonal zum eigentlichen Typausdruck.
 */
sealed interface TypAnforderung {
    val id: String

    data class Struktur(override val id: String) : TypAnforderung
    data class Eigenschaft(override val id: String) : TypAnforderung
    data class Axiom(override val id: String) : TypAnforderung
}

data class AnschlussVertrag(
    val typ: TypAusdruck = TypAusdruck.Unbekannt,
    val anforderungen: List<TypAnforderung> = emptyList(),
)

/**
 * Semantische Typinferenz eines Anschlusses. Sie läuft parallel zu den
 * historischen AnschlussArt-Regeln, damit bestehende Karten migrationsfähig
 * bleiben, ohne den neuen Typkern auf String-Kategorien zu reduzieren.
 */
sealed interface TypInferenzRegel {
    data class Fest(val typ: TypAusdruck) : TypInferenzRegel
    data class FolgtEingang(val eingang: String) : TypInferenzRegel
    data class GemeinsameOberart(val eingänge: List<String>) : TypInferenzRegel
    data class Priorisierung(
        val eingänge: List<String>,
        val prioritäten: List<TypAusdruck>,
    ) : TypInferenzRegel
    data class TupelAus(val eingänge: List<String>) : TypInferenzRegel
    data class KomponenteVonTupel(val eingang: String, val index: Int) : TypInferenzRegel {
        init { require(index >= 0) { "Ein Tupelindex darf nicht negativ sein." } }
    }
}
