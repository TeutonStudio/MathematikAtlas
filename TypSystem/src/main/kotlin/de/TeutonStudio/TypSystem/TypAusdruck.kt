package de.TeutonStudio.TypSystem

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
 * Neutraler semantischer Typausdruck. Er kennt weder Mathematik- noch Godot-Klassen
 * und kann deshalb von Rechenkern, Karteneditor und späteren Engine-Adaptern gemeinsam
 * verwendet werden.
 */
sealed interface TypAusdruck {
    /** Jeder konkrete Typ ist zulässig. */
    data object Beliebig : TypAusdruck

    /** Der Typ ist derzeit nicht bekannt. Das ist ausdrücklich nicht dasselbe wie [Beliebig]. */
    data object Unbekannt : TypAusdruck

    /** Nominaler atomarer Typ. */
    data class Atom(val id: TypId) : TypAusdruck

    /** Strukturierter Typ mit explizitem Konstruktor, z. B. Tupel<A,B> oder Methode<W,Z>. */
    data class Parameterisiert(
        val konstruktor: TypId,
        val argumente: List<TypAusdruck>,
    ) : TypAusdruck

    /** Echte Typalternative. */
    data class Vereinigung(val alternativen: List<TypAusdruck>) : TypAusdruck {
        init { require(alternativen.isNotEmpty()) { "Eine Typvereinigung benötigt mindestens eine Alternative." } }
    }

    /** Frei auflösbare Typvariable für spätere Unifikation. */
    data class Variable(val id: TypVariablenId) : TypAusdruck

    /** Neutrales Literal für Dimensionen, Formen und andere Typparameter. */
    data class Literal(val wert: String) : TypAusdruck {
        init { require(wert.isNotBlank()) { "Ein Typliteral darf nicht leer sein." } }
    }
}

enum class TypVarianz { Kovariant, Kontravariant, Invariant }

data class TypKonstruktorDefinition(
    val id: TypId,
    /**
     * Explizite Varianz je Typparameter. Ist die Liste kürzer als der tatsächlich
     * verwendete parameterisierte Typ, gilt für die restlichen Parameter
     * [standardVarianz]. Das ist insbesondere für variadische Tupel erforderlich.
     */
    val varianzen: List<TypVarianz> = emptyList(),
    val standardVarianz: TypVarianz = TypVarianz.Invariant,
)

/** Erweiterbarer Anforderungshaken. G0.3 registriert konkrete Struktur- und Axiomprüfer. */
data class TypAnforderung(
    val id: String,
    val parameter: Map<String, String> = emptyMap(),
) {
    init { require(id.isNotBlank()) { "Eine Typanforderung benötigt eine ID." } }
}

/**
 * Kanonische Namensräume für Anforderungen. G0.2 legt nur die stabilen IDs fest;
 * die fachlichen Prüfer werden in den jeweiligen Domänen registriert.
 */
object TypAnforderungen {
    fun struktur(id: String, parameter: Map<String, String> = emptyMap()): TypAnforderung =
        TypAnforderung("struktur.${id.trim()}", parameter).also {
            require(id.isNotBlank()) { "Eine Struktur-Anforderung benötigt eine ID." }
        }

    fun eigenschaft(id: String, parameter: Map<String, String> = emptyMap()): TypAnforderung =
        TypAnforderung("eigenschaft.${id.trim()}", parameter).also {
            require(id.isNotBlank()) { "Eine Eigenschafts-Anforderung benötigt eine ID." }
        }

    fun axiom(id: String, parameter: Map<String, String> = emptyMap()): TypAnforderung =
        TypAnforderung("axiom.${id.trim()}", parameter).also {
            require(id.isNotBlank()) { "Eine Axiom-Anforderung benötigt eine ID." }
        }
}

data class AnschlussVertrag(
    val typ: TypAusdruck = TypAusdruck.Unbekannt,
    val anforderungen: List<TypAnforderung> = emptyList(),
)

/**
 * Semantische Typinferenz eines Anschlusses. Die Regeln referenzieren absichtlich
 * stabile Anschlussnamen innerhalb desselben Knotens, analog zu den bestehenden
 * AnschlussArt-Regeln.
 */
sealed interface TypInferenzRegel {
    data class FolgtEingang(val eingang: String) : TypInferenzRegel
    data class GemeinsameOberart(val eingänge: List<String>) : TypInferenzRegel
    data class Vereinigung(val eingänge: List<String>) : TypInferenzRegel
    data class TupelAus(val eingänge: List<String>) : TypInferenzRegel
    data class KomponenteVonTupel(val eingang: String, val index: Int) : TypInferenzRegel {
        init { require(index >= 0) { "Der Tupelindex darf nicht negativ sein." } }
    }
    data class AbbildungVonEingang(
        val eingang: String,
        val abbildung: Map<TypAusdruck, TypAusdruck>,
    ) : TypInferenzRegel
    data class Priorisierung(
        val eingänge: List<String>,
        val prioritäten: List<TypAusdruck>,
    ) : TypInferenzRegel
}

/** Optionale Capability für Werte, die ihren semantischen Typ selbst bereitstellen. */
interface TypTragend {
    val typAusdruck: TypAusdruck
}
