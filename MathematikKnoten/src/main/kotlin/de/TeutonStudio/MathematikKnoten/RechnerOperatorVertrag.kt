package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
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

/** Deklarative Projektion ohne instanzbezogene IDs. */
data class RechnerPortVertrag(
    val name: String,
    val richtung: AnschlussRichtung,
    val art: AnschlussArtId,
    val zulässigeArten: Set<AnschlussArtId> = emptySet(),
    val reihenfolge: Int = 0,
    val variadisch: Boolean = false,
    val aliases: Set<String> = emptySet(),
)

data class RechnerSignaturVertrag(
    val id: String,
    val anschlüsse: List<RechnerPortVertrag>,
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
    val symbolLatex: String = titel,
    val definitionsLatex: String = symbolLatex,
    val aliases: Set<String> = emptySet(),
    val kategorie: String = familie.name.lowercase(),
)

sealed interface RechnerOperatorAuflösung {
    data class Bekannt(val vertrag: RechnerOperatorVertrag) : RechnerOperatorAuflösung
    data class Unbekannt(val familie: RechnerFamilie, val operatorId: String?) : RechnerOperatorAuflösung
}

object RechnerOperatorRegistry {
    val alle: List<RechnerOperatorVertrag> = RechnerOperatorDefinitionen.alle.map { definition ->
        RechnerOperatorVertrag(
            familie = RechnerFamilie.valueOf(definition.familie.name), operatorId = definition.id, titel = definition.titel,
            signaturen = definition.signaturen.map { signatur ->
                RechnerSignaturVertrag(signatur.id,
                    signatur.eingänge.mapIndexed { i, r -> r.alsPort(AnschlussRichtung.Eingang, i) } +
                        signatur.ausgänge.mapIndexed { i, r -> r.alsPort(AnschlussRichtung.Ausgang, i) })
            },
            parameter = definition.parameter.map { RechnerParameterVertrag(it.id, it.erforderlich, it.standardwert) },
            fähigkeiten = buildSet {
                if (definition.signaturen.any { it.eingänge.any(OperatorRolle::variadisch) }) add(RechnerFähigkeit.VARIADISCHE_EINGÄNGE)
                if (definition.signaturen.size > 1) add(RechnerFähigkeit.ALTERNATIVE_SIGNATUREN)
                if (definition.signaturen.any { it.eingänge.any(OperatorRolle::methodenfähig) }) add(RechnerFähigkeit.METHODENHEBUNG)
                if (definition.familie == OperatorFamilie.TENSOR && definition.signaturen.size > 1) add(RechnerFähigkeit.DYNAMISCHE_ACHSEN)
                if (definition.signaturen.any { it.ausgänge.size > 1 }) add(RechnerFähigkeit.MEHRFACHAUSGÄNGE)
            },
            unterstützung = RechnerUnterstützung.valueOf(definition.unterstützung.name),
            symbolLatex = definition.symbolLatex, definitionsLatex = definition.definitionsLatex, aliases = definition.aliases, kategorie = definition.kategorie,
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

private fun OperatorRolle.alsPort(richtung: AnschlussRichtung, index: Int): RechnerPortVertrag {
    val erlaubt = zulässigeTypen.flatMap { typ -> if (typ == FormelTyp.VEKTOR) listOf(
        MathematikAnschlussArten.SpaltenVektor.id, MathematikAnschlussArten.ZeilenVektor.id,
    ) else listOf(typ.anschlussArt()) }.toSet()
    return RechnerPortVertrag(name, richtung,
        if (methodenfähig) MathematikAnschlussArten.AtlasWert.id else if (erlaubt.isNotEmpty()) MathematikAnschlussArten.Objekt.id else typ.anschlussArt(),
        if (methodenfähig) erlaubt.ifEmpty { setOf(typ.anschlussArt()) } + MathematikAnschlussArten.Methode.id else erlaubt,
        index, variadisch, aliases)
}

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

/** Instanz-IDs entstehen erst an dieser expliziten Graphgrenze. */
internal fun rechnerAnschlüsse(familie: RechnerFamilie, id: String, signaturId: String = "standard"): List<AnschlussDaten> {
    val vertrag = (RechnerOperatorRegistry.finde(familie, id) as? RechnerOperatorAuflösung.Bekannt)?.vertrag
        ?: error("Unbekannter Operator $id")
    val signatur = vertrag.signaturen.single { it.id == signaturId }
    return signatur.anschlüsse.map { port -> AnschlussDaten(
        name = port.name, richtung = port.richtung,
        kante = if (port.richtung == AnschlussRichtung.Eingang) AnschlussKante.Links else AnschlussKante.Rechts,
        art = port.art, zulässigeArten = port.zulässigeArten, reihenfolge = port.reihenfolge,
        kannSichErweitern = port.variadisch,
    ) }
}
