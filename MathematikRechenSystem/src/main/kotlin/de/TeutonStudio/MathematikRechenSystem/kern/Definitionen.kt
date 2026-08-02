package de.TeutonStudio.MathematikRechenSystem.kern

/** Gemeinsamer Status für Nachweise, Definitionen und bedingte Typverträge. */
sealed interface NachweisStatus {
    data object Nachgewiesen : NachweisStatus
    data object Widerlegt : NachweisStatus
    data class Bedingt(val bedingungen: List<Aussage>) : NachweisStatus
    data object Unvollstaendig : NachweisStatus
    data object Unentscheidbar : NachweisStatus
}

sealed interface DefinitionsZiel {
    val stabileId: String

    data class Menge(override val stabileId: String, val symbol: MengenAusdruck) : DefinitionsZiel
    data class Operation(override val stabileId: String, val operatorId: String) : DefinitionsZiel
    data class Struktur(override val stabileId: String, val strukturId: String) : DefinitionsZiel
    data class Eigenschaft(override val stabileId: String, val eigenschaftId: String) : DefinitionsZiel
}

/**
 * Maschinenlesbare Definition statt bloßer Erklärungstexte auf einer Karte.
 *
 * Definitionen sind absichtlich kein direkter Untertyp des versiegelten
 * MathematischesObjekt-Modells. So erweitern Metadaten nicht unbemerkt jede
 * exhaustive Auswertung des mathematischen Laufzeitkerns.
 */
sealed interface MathematischeDefinition {
    val id: String
    val name: String
    val ziel: DefinitionsZiel
    val voraussetzungen: List<Aussage>
    val referenzen: Set<String>

    fun zuLatex(): String = "\\operatorname{Def}_{${name.latexText()}}"
}

data class GebundeneDefinitionsVariable(
    val id: String,
    val name: String,
    val wertebereich: MengenAusdruck? = null,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
    }
}

/** Eine Regel trägt ihre Semantik unabhängig von der Definitionskarten-Geometrie. */
data class DefinitionsRegel(
    val id: String,
    val name: String,
    val variablen: List<GebundeneDefinitionsVariable> = emptyList(),
    val voraussetzungen: List<Aussage> = emptyList(),
    val folgerungLatex: String,
    val referenzen: Set<String> = emptySet(),
    val rekursiveReferenzen: List<DefinitionsReferenz> = emptyList(),
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(folgerungLatex.isNotBlank())
        require(variablen.map { it.id }.distinct().size == variablen.size) {
            "Gebundene Variablen einer Definitionsregel benötigen eindeutige IDs."
        }
    }
}

enum class RekursionsPosition {
    BASIS,
    ABSCHLUSS_VORAUSSETZUNG,
    ABSCHLUSS_FOLGERUNG,
    IMPLIZITE_REGEL,
}

/** Bewachte Selbstreferenz; sie ist ausdrücklich keine beliebige Graphschleife. */
data class DefinitionsReferenz(
    val definitionsId: String,
    val position: RekursionsPosition,
    val monotonie: NachweisStatus = NachweisStatus.Unentscheidbar,
) {
    init { require(definitionsId.isNotBlank()) }
}

data class ExpliziteDefinition(
    override val id: String,
    override val name: String,
    override val ziel: DefinitionsZiel,
    val wert: MathematischesObjekt,
    override val voraussetzungen: List<Aussage> = emptyList(),
    override val referenzen: Set<String> = emptySet(),
) : MathematischeDefinition

data class PraedikativeDefinition(
    override val id: String,
    override val name: String,
    override val ziel: DefinitionsZiel,
    val grundmenge: MengenAusdruck,
    val praedikat: Aussage,
    override val voraussetzungen: List<Aussage> = emptyList(),
    override val referenzen: Set<String> = emptySet(),
) : MathematischeDefinition

data class InduktiveDefinition(
    override val id: String,
    override val name: String,
    override val ziel: DefinitionsZiel,
    val basisRegeln: List<DefinitionsRegel>,
    val abschlussRegeln: List<DefinitionsRegel>,
    val minimal: Boolean = true,
    override val voraussetzungen: List<Aussage> = emptyList(),
    override val referenzen: Set<String> = emptySet(),
) : MathematischeDefinition {
    init {
        require(basisRegeln.isNotEmpty()) { "Eine induktive Definition benötigt mindestens eine Basisregel." }
        require(abschlussRegeln.isNotEmpty()) { "Eine induktive Definition benötigt mindestens eine Abschlussregel." }
        require(minimal) { "Ohne Minimalität ist die induktiv beschriebene Zielstruktur nicht eindeutig bestimmt." }
    }
}

data class ImpliziteDefinition(
    override val id: String,
    override val name: String,
    override val ziel: DefinitionsZiel,
    val charakterisierendeRegeln: List<DefinitionsRegel>,
    val existenzStatus: NachweisStatus,
    val eindeutigkeitsStatus: NachweisStatus,
    override val voraussetzungen: List<Aussage> = emptyList(),
    override val referenzen: Set<String> = emptySet(),
) : MathematischeDefinition {
    init { require(charakterisierendeRegeln.isNotEmpty()) }
}

data class GruppierungsDefinition(
    override val id: String,
    override val name: String,
    override val ziel: DefinitionsZiel,
    val grundStruktur: MengenAusdruck,
    val relationId: String,
    val relationLatex: String,
    val reflexiv: NachweisStatus,
    val symmetrisch: NachweisStatus,
    val transitiv: NachweisStatus,
    val materialisiereKlassen: Boolean = false,
    override val voraussetzungen: List<Aussage> = emptyList(),
    override val referenzen: Set<String> = emptySet(),
) : MathematischeDefinition {
    init {
        require(relationId.isNotBlank())
        require(relationLatex.isNotBlank())
    }

    val istZertifizierteAequivalenzrelation: Boolean
        get() = reflexiv == NachweisStatus.Nachgewiesen &&
            symmetrisch == NachweisStatus.Nachgewiesen &&
            transitiv == NachweisStatus.Nachgewiesen
}

data class GruppierungsOperation(
    val operatorId: String,
    val gruppierungId: String,
    val repraesentantenOperatorId: String,
    val wohldefiniert: NachweisStatus,
    val bedingungen: List<Aussage> = emptyList(),
) {
    init {
        require(operatorId.isNotBlank())
        require(gruppierungId.isNotBlank())
        require(repraesentantenOperatorId.isNotBlank())
    }
}

data class VervollstaendigungsDefinition(
    override val id: String,
    override val name: String,
    override val ziel: DefinitionsZiel,
    val ausgangsRaum: MengenAusdruck,
    val repraesentantenRaum: MengenAusdruck,
    val zulaessigkeitsEigenschaftId: String,
    val gruppierung: GruppierungsDefinition,
    override val voraussetzungen: List<Aussage> = emptyList(),
    override val referenzen: Set<String> = setOf(gruppierung.id),
) : MathematischeDefinition

sealed interface DefinitionsPruefung {
    data object Gueltig : DefinitionsPruefung
    data class BedingtGueltig(val bedingungen: List<String>) : DefinitionsPruefung
    data class Ungueltig(val gruende: List<String>) : DefinitionsPruefung
    data class Unvollstaendig(val gruende: List<String>) : DefinitionsPruefung
}

/** Zentrale Prüfung kontrollierter Rekursion und referenzieller Konsistenz. */
object DefinitionsPruefer {
    fun pruefe(
        definition: MathematischeDefinition,
        bekannteDefinitionen: Set<String>,
    ): DefinitionsPruefung {
        val ungueltig = mutableListOf<String>()
        val unvollstaendig = mutableListOf<String>()
        val bedingungen = mutableListOf<String>()

        if (definition.id.isBlank()) ungueltig += "Definitions-ID fehlt."
        if (definition.name.isBlank()) ungueltig += "Definitionsname fehlt."
        val unbekannt = definition.referenzen - bekannteDefinitionen - definition.id
        if (unbekannt.isNotEmpty()) unvollstaendig += "Unbekannte Definitionen: ${unbekannt.sorted().joinToString()}."

        val regeln = when (definition) {
            is InduktiveDefinition -> definition.basisRegeln + definition.abschlussRegeln
            is ImpliziteDefinition -> definition.charakterisierendeRegeln
            else -> emptyList()
        }
        if (regeln.map { it.id }.distinct().size != regeln.size) {
            ungueltig += "Regel-IDs müssen innerhalb einer Definition eindeutig sein."
        }
        regeln.flatMap { it.rekursiveReferenzen }.forEach { referenz ->
            if (referenz.definitionsId != definition.id) {
                ungueltig += "Eine rekursive Referenz darf nur auf die eigene Definition zeigen."
            }
            if (definition is InduktiveDefinition && referenz.position !in setOf(
                    RekursionsPosition.ABSCHLUSS_VORAUSSETZUNG,
                    RekursionsPosition.ABSCHLUSS_FOLGERUNG,
                )
            ) {
                ungueltig += "Induktive Selbstreferenz ist nur in Abschlussregeln zulässig."
            }
            when (referenz.monotonie) {
                NachweisStatus.Widerlegt -> ungueltig += "Die rekursive Regel ist nachweislich nicht monoton."
                is NachweisStatus.Bedingt -> bedingungen += "Monotonie der Rekursion ist nur bedingt nachgewiesen."
                NachweisStatus.Unvollstaendig -> unvollstaendig += "Monotonienachweis der Rekursion fehlt."
                NachweisStatus.Unentscheidbar -> bedingungen += "Monotonie der Rekursion ist unentscheidbar."
                NachweisStatus.Nachgewiesen -> Unit
            }
        }

        when (definition) {
            is ImpliziteDefinition -> {
                definition.existenzStatus.alsPruefHinweis("Existenz", ungueltig, unvollstaendig, bedingungen)
                definition.eindeutigkeitsStatus.alsPruefHinweis("Eindeutigkeit", ungueltig, unvollstaendig, bedingungen)
            }
            is GruppierungsDefinition -> {
                definition.reflexiv.alsPruefHinweis("Reflexivität", ungueltig, unvollstaendig, bedingungen)
                definition.symmetrisch.alsPruefHinweis("Symmetrie", ungueltig, unvollstaendig, bedingungen)
                definition.transitiv.alsPruefHinweis("Transitivität", ungueltig, unvollstaendig, bedingungen)
            }
            else -> Unit
        }

        return when {
            ungueltig.isNotEmpty() -> DefinitionsPruefung.Ungueltig(ungueltig.distinct())
            unvollstaendig.isNotEmpty() -> DefinitionsPruefung.Unvollstaendig(unvollstaendig.distinct())
            bedingungen.isNotEmpty() -> DefinitionsPruefung.BedingtGueltig(bedingungen.distinct())
            else -> DefinitionsPruefung.Gueltig
        }
    }
}

/** Stabiles Register für Rechenkern, Typinferenz und Definitionskarten. */
class DefinitionsRegister(definitionen: Iterable<MathematischeDefinition> = emptyList()) {
    private val nachId = linkedMapOf<String, MathematischeDefinition>()

    init { definitionen.forEach(::registriere) }

    fun registriere(definition: MathematischeDefinition) {
        require(definition.id.isNotBlank())
        require(nachId[definition.id] == null || nachId[definition.id] == definition) {
            "Definitions-ID ${definition.id} ist bereits mit anderer Semantik registriert."
        }
        nachId[definition.id] = definition
    }

    operator fun get(id: String): MathematischeDefinition? = nachId[id]
    fun alle(): List<MathematischeDefinition> = nachId.values.toList()
    fun pruefeAlle(): Map<String, DefinitionsPruefung> = nachId.mapValues { (_, definition) ->
        DefinitionsPruefer.pruefe(definition, nachId.keys)
    }
}

private fun NachweisStatus.alsPruefHinweis(
    bezeichnung: String,
    ungueltig: MutableList<String>,
    unvollstaendig: MutableList<String>,
    pruefBedingungen: MutableList<String>,
) {
    when (this) {
        NachweisStatus.Nachgewiesen -> Unit
        NachweisStatus.Widerlegt -> ungueltig += "$bezeichnung ist widerlegt."
        is NachweisStatus.Bedingt -> pruefBedingungen +=
            "$bezeichnung gilt nur unter ${bedingungen.size} Bedingung(en)."
        NachweisStatus.Unvollstaendig -> unvollstaendig += "$bezeichnung ist noch nicht nachgewiesen."
        NachweisStatus.Unentscheidbar -> pruefBedingungen += "$bezeichnung ist im aktuellen System unentscheidbar."
    }
}

private fun String.latexText(): String = replace("\\", "").replace("_", "\\_").replace(" ", "\\ ")
