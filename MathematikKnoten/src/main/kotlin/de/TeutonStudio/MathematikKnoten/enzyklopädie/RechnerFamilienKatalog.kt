package de.TeutonStudio.MathematikKnoten.enzyklopädie

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikKnoten.ZahlenRechnerKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

@JvmInline
value class RechnerFamilienId(val wert: String) {
    init { require(wert.isNotBlank()) }
    override fun toString(): String = wert
}

data class OperatorStelligkeit(
    val mindestens: Int,
    val höchstens: Int? = mindestens,
) {
    init {
        require(mindestens >= 0)
        require(höchstens == null || höchstens >= mindestens)
    }

    fun erlaubt(anzahl: Int): Boolean = anzahl >= mindestens && (höchstens == null || anzahl <= höchstens)
}

data class RechnerOperatorSignatur(
    val argumentRollen: List<String>,
    val eingangsArten: List<AnschlussArtId>,
    val stelligkeit: OperatorStelligkeit = OperatorStelligkeit(argumentRollen.size),
    val beschreibung: String? = null,
) {
    init {
        require(argumentRollen.isNotEmpty() || stelligkeit.mindestens == 0)
        require(argumentRollen.size == eingangsArten.size) {
            "Argumentrollen und Anschlussarten einer Operatorsignatur müssen gleich lang sein."
        }
        require(argumentRollen.distinct().size == argumentRollen.size) {
            "Argumentrollen einer Operatorsignatur müssen eindeutig sein."
        }
        require(stelligkeit.mindestens <= argumentRollen.size) {
            "Die Mindeststelligkeit darf die beschriebenen Grundrollen nicht überschreiten."
        }
    }
}

data class RechnerOperatorEintrag(
    val stabileId: String,
    val familie: RechnerFamilienId,
    val knotenArt: KnotenArtId,
    val titel: String,
    val kategorie: String,
    val signaturen: List<RechnerOperatorSignatur>,
    val ausgangsArt: AnschlussArtId,
    val casVerfügbar: Boolean,
    val wissensId: WissensId,
    val varianten: Set<VariantenId> = setOf(VariantenId(stabileId)),
) {
    init {
        require(stabileId.isNotBlank())
        require(titel.isNotBlank())
        require(kategorie.isNotBlank())
        require(signaturen.isNotEmpty()) { "$stabileId benötigt mindestens eine Operatorsignatur." }
    }

    val argumentRollen: List<String>
        get() = signaturen.flatMap(RechnerOperatorSignatur::argumentRollen).distinct()

    val eingangsArten: List<AnschlussArtId>
        get() = signaturen.flatMap(RechnerOperatorSignatur::eingangsArten).distinct()

    val stelligkeit: OperatorStelligkeit
        get() = OperatorStelligkeit(
            mindestens = signaturen.minOf { it.stelligkeit.mindestens },
            höchstens = signaturen.map { it.stelligkeit.höchstens }.let { maxima ->
                if (maxima.any { it == null }) null else maxima.filterNotNull().maxOrNull()
            },
        )
}

object RechnerFamilienKatalog {
    val Zahlenrechner = RechnerFamilienId("rechner.zahlen")
    val Vektorrechner = RechnerFamilienId("rechner.vektor")
    val Matrixrechner = RechnerFamilienId("rechner.matrix")
    val Tensorrechner = RechnerFamilienId("rechner.tensor")

    private val projektionen: List<RechnerOperatorEintrag> = RechnerOperatorRegistry.alle.filterNot { it.operatorId.endsWith(".formel") }.map { vertrag ->
        RechnerOperatorEintrag(
            stabileId = vertrag.operatorId,
            familie = when (vertrag.familie) { RechnerFamilie.ZAHL -> Zahlenrechner; RechnerFamilie.VEKTOR -> Vektorrechner; RechnerFamilie.MATRIX -> Matrixrechner; RechnerFamilie.TENSOR -> Tensorrechner },
            knotenArt = vertrag.familie.knotenArt, titel = vertrag.titel, kategorie = vertrag.kategorie,
            signaturen = vertrag.signaturen.map { signatur -> RechnerOperatorSignatur(
                argumentRollen = signatur.eingänge.map { it.name }, eingangsArten = signatur.eingänge.map { it.art },
                stelligkeit = OperatorStelligkeit(signatur.eingänge.size, if (signatur.eingänge.any { it.variadisch }) null else signatur.eingänge.size),
                beschreibung = vertrag.definitionsLatex,
            ) },
            ausgangsArt = vertrag.signaturen.first().ausgänge.first().art,
            casVerfügbar = vertrag.unterstützung != RechnerUnterstützung.REGISTRIERT,
            wissensId = WissensId("operator.${vertrag.operatorId}"),
        )
    }
    val alle = projektionen.distinctBy { it.stabileId }
    val zahlenOperatoren get() = fürFamilie(Zahlenrechner)
    val tensorOperatoren get() = fürFamilie(Tensorrechner)
    val vektorOperatoren get() = fürFamilie(Vektorrechner)
    val matrixOperatoren get() = fürFamilie(Matrixrechner)
    private val nachId = alle.associateBy(RechnerOperatorEintrag::stabileId)
    fun fürOperatorId(id: String): RechnerOperatorEintrag? = nachId[id]
    fun fürFamilie(familie: RechnerFamilienId): List<RechnerOperatorEintrag> = projektionen.filter { it.familie == familie }
    fun validierungsFehler(): List<String> = alle.groupBy { it.stabileId }.filterValues { it.size > 1 }.keys.map { "Doppelte Operator-ID: $it" }
}
