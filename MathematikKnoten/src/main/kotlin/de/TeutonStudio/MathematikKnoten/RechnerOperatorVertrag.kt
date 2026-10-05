package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Kanonischer, UI-unabhängiger Vertrag aller bestehenden Rechnerfamilien. */
enum class RechnerFamilie(
    val knotenArt: String,
    val operatorParameter: String,
) {
    ZAHL(ZAHLENRECHNER_ART, ZAHLENRECHNER_OPERATOR),
    VEKTOR(VektorRechner.KNOTEN_ART, VEKTOR_RECHNER_OPERATOR),
    MATRIX(MatrixRechner.KNOTEN_ART, RECHNER_OPERATOR_PARAMETER),
    TENSOR(TensorRechner.KNOTEN_ART, TENSOR_OPERATION_ID),
}

enum class RechnerFähigkeit {
    VARIADISCHE_EINGÄNGE,
    ALTERNATIVE_SIGNATUREN,
    METHODENHEBUNG,
    DYNAMISCHE_ACHSEN,
    MEHRFACHAUSGÄNGE,
}

enum class RechnerUnterstützung { KONKRET, SYMBOLISCH, REGISTRIERT }

data class RechnerParameterVertrag(
    val id: String,
    val erforderlich: Boolean = false,
    val standardwert: String? = null,
)

data class RechnerSignaturVertrag(
    val id: String,
    val anschlüsse: List<AnschlussDaten>,
) {
    val eingänge get() = anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    val ausgänge get() = anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }
}

data class RechnerOperatorVertrag(
    val familie: RechnerFamilie,
    val operatorId: String,
    val titel: String,
    val signaturen: List<RechnerSignaturVertrag>,
    val parameter: List<RechnerParameterVertrag> = emptyList(),
    val fähigkeiten: Set<RechnerFähigkeit> = emptySet(),
    val unterstützung: RechnerUnterstützung = RechnerUnterstützung.KONKRET,
)

sealed interface RechnerOperatorAuflösung {
    data class Bekannt(val vertrag: RechnerOperatorVertrag) : RechnerOperatorAuflösung
    data class Unbekannt(val familie: RechnerFamilie, val operatorId: String?) : RechnerOperatorAuflösung
}

object RechnerOperatorRegistry {
    val alle: List<RechnerOperatorVertrag> = buildList {
        ZahlenRechnerKnotenVorlagen.alle.forEach { vorlage ->
            val operator = UniversellerZahlenOperator.vonId(vorlage.standardParameter[ZAHLENRECHNER_OPERATOR])
            val fähigkeiten = buildSet {
                if (istVariadischerZahlenOperator(operator)) add(RechnerFähigkeit.VARIADISCHE_EINGÄNGE)
                if (istKomplexKonstruktor(operator) || operator == UniversellerZahlenOperator.DIVISION) {
                    add(RechnerFähigkeit.ALTERNATIVE_SIGNATUREN)
                }
                if (operator.hebungsArt == ZahlenOperatorHebungsArt.PUNKTWEISE) add(RechnerFähigkeit.METHODENHEBUNG)
            }
            add(
                RechnerOperatorVertrag(
                    RechnerFamilie.ZAHL,
                    operator.stabileId,
                    operator.titel,
                    listOf(RechnerSignaturVertrag("standard", vorlage.anschlüsse)),
                    fähigkeiten = fähigkeiten,
                ),
            )
        }
        ErweiterterZahlenOperator.entries.forEach { operator ->
            val knoten = konfiguriereErweitertenZahlenRechner(
                ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),
                operator,
            )
            add(
                RechnerOperatorVertrag(
                    RechnerFamilie.ZAHL,
                    operator.stabileId,
                    operator.titel,
                    listOf(RechnerSignaturVertrag("standard", knoten.anschlüsse)),
                ),
            )
        }
        add(
            RechnerOperatorVertrag(
                RechnerFamilie.ZAHL,
                ZAHLENRECHNER_FORMEL_ID,
                "Eigene Formel",
                listOf(RechnerSignaturVertrag("formel", ZahlenRechnerKnotenVorlagen.standard.anschlüsse)),
                unterstützung = RechnerUnterstützung.SYMBOLISCH,
            ),
        )
        VektorRechnerOperator.entries.forEach { operator ->
            add(
                RechnerOperatorVertrag(
                    RechnerFamilie.VEKTOR,
                    operator.stabileId,
                    operator.titel,
                    listOf(RechnerSignaturVertrag("standard", vektorRechnerAnschluesse(operator))),
                    fähigkeiten = setOf(RechnerFähigkeit.METHODENHEBUNG),
                ),
            )
        }
        add(
            RechnerOperatorVertrag(
                RechnerFamilie.VEKTOR,
                VEKTOR_FORMEL_ID,
                "Eigene Formel",
                listOf(RechnerSignaturVertrag("formel", vektorRechnerAnschluesse(VektorRechnerOperator.ADDITION))),
                unterstützung = RechnerUnterstützung.SYMBOLISCH,
            ),
        )
        StrukturRechnerOperatoren.fuer(StrukturRechnerKnotenFamilie.MATRIX).forEach { definition ->
            add(
                RechnerOperatorVertrag(
                    RechnerFamilie.MATRIX,
                    definition.id,
                    definition.titel,
                    listOf(
                        RechnerSignaturVertrag(
                            "standard",
                            definition.eingänge.mapIndexed { index, eingang ->
                                registryEingang(eingang.name, eingang.typ, index)
                            } + registryAusgang(definition.ergebnisTyp),
                        ),
                    ),
                    fähigkeiten = setOf(RechnerFähigkeit.METHODENHEBUNG),
                ),
            )
        }
        add(
            RechnerOperatorVertrag(
                RechnerFamilie.MATRIX,
                MATRIX_FORMEL_ID,
                "Eigene Formel",
                listOf(RechnerSignaturVertrag("formel", StrukturFormelRechnerVorlagen.Matrix.anschlüsse)),
                unterstützung = RechnerUnterstützung.SYMBOLISCH,
            ),
        )
        StandardTensorOperationen.registry.alle().forEach { definition ->
            val status = when (definition.unterstuetzungsStatus) {
                TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT -> RechnerUnterstützung.KONKRET
                TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT -> RechnerUnterstützung.SYMBOLISCH
                TensorUnterstuetzungsStatus.REGISTRIERT -> RechnerUnterstützung.REGISTRIERT
            }
            add(
                RechnerOperatorVertrag(
                    RechnerFamilie.TENSOR,
                    definition.id.wert,
                    definition.titel,
                    listOf(
                        RechnerSignaturVertrag(
                            "standard",
                            tensorOperationAnschluesse(
                                definition,
                                AchsenEingabeModus.TUPEL,
                                definition.minimaleAchsenAnzahl.coerceAtLeast(1),
                            ),
                        ),
                    ),
                    parameter = definition.parameter.map { RechnerParameterVertrag(it.id, it.erforderlich, it.standardWert) },
                    fähigkeiten = buildSet {
                        if (definition.maximaleAchsenAnzahl == null || definition.minimaleAchsenAnzahl > 0) {
                            add(RechnerFähigkeit.DYNAMISCHE_ACHSEN)
                        }
                        if (definition.ausgangsRollen.size > 1) add(RechnerFähigkeit.MEHRFACHAUSGÄNGE)
                    },
                    unterstützung = status,
                ),
            )
        }
        add(
            RechnerOperatorVertrag(
                RechnerFamilie.TENSOR,
                TENSOR_FORMEL_ID,
                "Eigene Formel",
                listOf(RechnerSignaturVertrag("formel", StrukturFormelRechnerVorlagen.Tensor.anschlüsse)),
                unterstützung = RechnerUnterstützung.SYMBOLISCH,
            ),
        )
    }.sortedWith(compareBy({ it.familie.ordinal }, RechnerOperatorVertrag::operatorId))

    private val nachSchlüssel = alle.associateBy { it.familie to it.operatorId }

    init {
        require(nachSchlüssel.size == alle.size) { "Operator-IDs müssen innerhalb ihrer Rechnerfamilie eindeutig sein." }
    }

    fun familieFür(knotenArt: String): RechnerFamilie? = RechnerFamilie.entries.firstOrNull { it.knotenArt == knotenArt }

    fun finde(familie: RechnerFamilie, operatorId: String?): RechnerOperatorAuflösung =
        nachSchlüssel[familie to operatorId]?.let(RechnerOperatorAuflösung::Bekannt)
            ?: RechnerOperatorAuflösung.Unbekannt(familie, operatorId)

    fun fürFamilie(familie: RechnerFamilie): List<RechnerOperatorVertrag> = alle.filter { it.familie == familie }

    /**
     * Einziger familienübergreifender Konfigurations-Dispatcher. Unbekannte IDs
     * werden absichtlich nicht ersetzt, damit historische Karten verlustfrei bleiben.
     */
    fun konfiguriere(knoten: KnotenDaten, operatorId: String): KnotenDaten {
        val familie = familieFür(knoten.art) ?: return knoten
        if (finde(familie, operatorId) !is RechnerOperatorAuflösung.Bekannt) return knoten
        return when (familie) {
            RechnerFamilie.ZAHL -> when {
                ErweiterterZahlenOperator.vonId(operatorId) != null -> konfiguriereErweitertenZahlenRechner(
                    knoten,
                    requireNotNull(ErweiterterZahlenOperator.vonId(operatorId)),
                )
                operatorId == ZAHLENRECHNER_FORMEL_ID -> knoten
                else -> konfiguriereZahlenRechner(knoten, UniversellerZahlenOperator.vonId(operatorId))
            }
            RechnerFamilie.VEKTOR -> if (operatorId == VEKTOR_FORMEL_ID) knoten else konfiguriereVektorRechner(
                knoten,
                requireNotNull(VektorRechnerOperator.entries.firstOrNull { it.stabileId == operatorId }),
            )
            RechnerFamilie.MATRIX -> if (operatorId == MATRIX_FORMEL_ID) knoten else {
                konfiguriereStrukturRechner(knoten, StrukturRechnerKnotenFamilie.MATRIX, operatorId)
            }
            RechnerFamilie.TENSOR -> if (operatorId == TENSOR_FORMEL_ID) knoten else konfiguriereTensorOperation(
                knoten,
                requireNotNull(StandardTensorOperationen.registry.definition(operatorId)),
            )
        }
    }
}

private fun registryEingang(name: String, typ: FormelTyp, reihenfolge: Int): AnschlussDaten = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante.Links,
    art = typ.anschlussArt(),
    reihenfolge = reihenfolge,
)

private fun registryAusgang(typ: FormelTyp): AnschlussDaten = AnschlussDaten(
    name = if (typ == FormelTyp.METHODE) "methode" else "wert",
    richtung = AnschlussRichtung.Ausgang,
    kante = de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante.Rechts,
    art = typ.anschlussArt(),
)

private fun FormelTyp.anschlussArt() = when (this) {
    FormelTyp.ZAHL -> MathematikAnschlussArten.Zahl.id
    FormelTyp.MENGE -> MathematikAnschlussArten.Menge.id
    FormelTyp.AUSSAGE -> MathematikAnschlussArten.Aussage.id
    FormelTyp.TUPEL -> MathematikAnschlussArten.Tupel.id
    FormelTyp.VEKTOR -> MathematikAnschlussArten.Vektor.id
    FormelTyp.MATRIX -> MathematikAnschlussArten.Matrix.id
    FormelTyp.TENSOR -> MathematikAnschlussArten.Tensor.id
    FormelTyp.METHODE -> MathematikAnschlussArten.Methode.id
    FormelTyp.OBJEKT -> MathematikAnschlussArten.Objekt.id
}
