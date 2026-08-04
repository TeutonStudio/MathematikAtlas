package de.TeutonStudio.MathematikKnoten.enzyklopädie

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
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
    val Tensorrechner = RechnerFamilienId("rechner.tensor")

    val zahlenOperatoren: List<RechnerOperatorEintrag> = UniversellerZahlenOperator.entries.map { operator ->
        val vorlage = zahlenVorlage(operator)
        RechnerOperatorEintrag(
            stabileId = operator.stabileId,
            familie = Zahlenrechner,
            knotenArt = ZAHLENRECHNER_ART,
            titel = operator.titel,
            kategorie = zahlenKategorie(operator),
            signaturen = zahlenSignaturen(operator, vorlage),
            ausgangsArt = vorlage.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.art,
            casVerfügbar = true,
            wissensId = WissensId("operator.${operator.stabileId}"),
        )
    }

    val tensorOperatoren: List<RechnerOperatorEintrag> = TensorRechnerOperator.entries.map { operator ->
        RechnerOperatorEintrag(
            stabileId = operator.stabileId,
            familie = Tensorrechner,
            knotenArt = TensorRechner.KNOTEN_ART,
            titel = tensorTitel(operator),
            kategorie = tensorKategorie(operator),
            signaturen = listOf(tensorSignatur(operator)),
            ausgangsArt = if (operator == TensorRechnerOperator.NORM) {
                MathematikAnschlussArten.Zahl.id
            } else {
                MathematikAnschlussArten.Tensor.id
            },
            casVerfügbar = operator in setOf(
                TensorRechnerOperator.ADDITION,
                TensorRechnerOperator.SUBTRAKTION,
                TensorRechnerOperator.SKALARMULTIPLIKATION,
                TensorRechnerOperator.HADAMARD_PRODUKT,
                TensorRechnerOperator.TENSORPRODUKT,
                TensorRechnerOperator.TRANSPONIEREN,
                TensorRechnerOperator.NORM,
            ),
            wissensId = WissensId("operator.${operator.stabileId}"),
        )
    }

    val alle: List<RechnerOperatorEintrag> = (zahlenOperatoren + tensorOperatoren)
        .sortedWith(compareBy<RechnerOperatorEintrag> { it.familie.wert }.thenBy { it.stabileId })

    private val nachId = alle.associateBy(RechnerOperatorEintrag::stabileId)
    private val nachFamilie = alle.groupBy(RechnerOperatorEintrag::familie)

    fun fürOperatorId(id: String): RechnerOperatorEintrag? = nachId[id]
    fun fürFamilie(familie: RechnerFamilienId): List<RechnerOperatorEintrag> = nachFamilie[familie].orEmpty()

    fun validierungsFehler(): List<String> = buildList {
        alle.groupBy(RechnerOperatorEintrag::stabileId).filterValues { it.size > 1 }.keys.forEach {
            add("Doppelte Operator-ID: $it")
        }
        val zahlenIds = zahlenOperatoren.map(RechnerOperatorEintrag::stabileId).toSet()
        val erwarteteZahlenIds = UniversellerZahlenOperator.entries.map(UniversellerZahlenOperator::stabileId).toSet()
        (erwarteteZahlenIds - zahlenIds).forEach { add("Fehlender Zahlenoperator: $it") }
        (zahlenIds - erwarteteZahlenIds).forEach { add("Unbekannter Zahlenoperator: $it") }

        zahlenOperatoren.forEach { eintrag ->
            val operator = UniversellerZahlenOperator.entries.single { it.stabileId == eintrag.stabileId }
            val vorlage = zahlenVorlage(operator)
            val eingänge = vorlage.anschlüsse
                .filter { it.richtung == AnschlussRichtung.Eingang }
                .associate { it.name to it.art }
            eintrag.signaturen.forEach { signatur ->
                signatur.argumentRollen.zip(signatur.eingangsArten).forEach { (rolle, art) ->
                    if (eingänge[rolle] != art) {
                        add("${eintrag.stabileId}: Signaturrolle $rolle/$art stimmt nicht mit der Knotenvorlage überein.")
                    }
                }
            }
            val ausgang = vorlage.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.art
            if (eintrag.ausgangsArt != ausgang) {
                add("${eintrag.stabileId}: Ausgang ${eintrag.ausgangsArt} stimmt nicht mit $ausgang überein.")
            }
        }

        val tensorIds = tensorOperatoren.map(RechnerOperatorEintrag::stabileId).toSet()
        val erwarteteTensorIds = TensorRechnerOperator.entries.map(TensorRechnerOperator::stabileId).toSet()
        (erwarteteTensorIds - tensorIds).forEach { add("Fehlender Tensoroperator: $it") }
        (tensorIds - erwarteteTensorIds).forEach { add("Unbekannter Tensoroperator: $it") }
    }
}

private fun zahlenVorlage(operator: UniversellerZahlenOperator): KnotenVorlage =
    ZahlenRechnerKnotenVorlagen.alle.single {
        it.standardParameter[ZAHLENRECHNER_OPERATOR] == operator.stabileId
    }

private fun zahlenSignaturen(
    operator: UniversellerZahlenOperator,
    vorlage: KnotenVorlage,
): List<RechnerOperatorSignatur> {
    val eingänge = vorlage.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }

    fun signatur(
        namen: List<String>,
        stelligkeit: OperatorStelligkeit = OperatorStelligkeit(namen.size),
        beschreibung: String? = null,
    ): RechnerOperatorSignatur {
        val anschlüsse = namen.map { name -> eingänge.single { it.name == name } }
        return RechnerOperatorSignatur(
            argumentRollen = namen,
            eingangsArten = anschlüsse.map { it.art },
            stelligkeit = stelligkeit,
            beschreibung = beschreibung,
        )
    }

    return when (operator) {
        UniversellerZahlenOperator.ADDITION,
        UniversellerZahlenOperator.MULTIPLIKATION,
        UniversellerZahlenOperator.MINIMUM,
        UniversellerZahlenOperator.MAXIMUM,
        -> listOf(
            signatur(
                namen = eingänge.take(2).map { it.name },
                stelligkeit = OperatorStelligkeit(2, null),
                beschreibung = "Mindestens zwei, über dynamische Anschlüsse beliebig viele Argumente.",
            ),
        )

        UniversellerZahlenOperator.DIVISION -> listOf(
            signatur(listOf("a", "b"), beschreibung = "Zähler und Nenner."),
            signatur(listOf("a", "b", "c"), beschreibung = "Optionaler Ersatzwert für einen verschwindenden Nenner."),
        )

        UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
        UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH,
        -> listOf(
            signatur(listOf("a", "b"), beschreibung = "Getrennte Komponenten."),
            signatur(listOf("tupel"), beschreibung = "Gemeinsame Tupelkomponente."),
        )

        else -> listOf(
            RechnerOperatorSignatur(
                argumentRollen = eingänge.map { it.name },
                eingangsArten = eingänge.map { it.art },
            ),
        )
    }
}

private fun zahlenKategorie(operator: UniversellerZahlenOperator): String = when (operator) {
    UniversellerZahlenOperator.ADDITION,
    UniversellerZahlenOperator.SUBTRAKTION,
    UniversellerZahlenOperator.MULTIPLIKATION,
    UniversellerZahlenOperator.DIVISION,
    UniversellerZahlenOperator.MODULO,
    UniversellerZahlenOperator.MINIMUM,
    UniversellerZahlenOperator.MAXIMUM,
    -> "Grundrechenarten"

    UniversellerZahlenOperator.KEHRWERT,
    UniversellerZahlenOperator.POTENZ,
    UniversellerZahlenOperator.QUADRAT,
    UniversellerZahlenOperator.KUBIK,
    UniversellerZahlenOperator.WURZEL,
    UniversellerZahlenOperator.QUADRATWURZEL,
    UniversellerZahlenOperator.KUBIKWURZEL,
    -> "Potenzen und Wurzeln"

    UniversellerZahlenOperator.ITERIERTE_SUMME,
    UniversellerZahlenOperator.ITERIERTES_PRODUKT,
    UniversellerZahlenOperator.INTEGRAL,
    UniversellerZahlenOperator.DIFFERENTIAL,
    UniversellerZahlenOperator.LIMES_HYPERREELL_ZU_REELL,
    -> "Analysis"

    UniversellerZahlenOperator.SINUS,
    UniversellerZahlenOperator.COSINUS,
    UniversellerZahlenOperator.ARCSINUS,
    UniversellerZahlenOperator.ARCCOSINUS,
    -> "Trigonometrie"

    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
    UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH,
    UniversellerZahlenOperator.KONJUGIERTE,
    UniversellerZahlenOperator.REALTEIL,
    UniversellerZahlenOperator.IMAGINAERTEIL,
    UniversellerZahlenOperator.KOMPLEXER_WINKEL,
    UniversellerZahlenOperator.KOMPLEXER_RADIUS,
    -> "Komplexe Zahlen"

    else -> "Funktionen"
}

private fun tensorTitel(operator: TensorRechnerOperator): String = when (operator) {
    TensorRechnerOperator.ADDITION -> "Tensoraddition"
    TensorRechnerOperator.SUBTRAKTION -> "Tensorsubtraktion"
    TensorRechnerOperator.SKALARMULTIPLIKATION -> "Skalarmultiplikation"
    TensorRechnerOperator.HADAMARD_PRODUKT -> "Hadamard-Produkt"
    TensorRechnerOperator.TENSORPRODUKT -> "Tensorprodukt"
    TensorRechnerOperator.KONTRAKTION -> "Kontraktion"
    TensorRechnerOperator.ACHSENPERMUTATION -> "Achsenpermutation"
    TensorRechnerOperator.TRANSPONIEREN -> "Transponieren"
    TensorRechnerOperator.ACHSENSCHNITT -> "Achsenschnitt"
    TensorRechnerOperator.INDEXAUSWERTUNG -> "Indexauswertung"
    TensorRechnerOperator.NORM -> "Tensornorm"
}

private fun tensorKategorie(operator: TensorRechnerOperator): String = when (operator) {
    TensorRechnerOperator.ADDITION,
    TensorRechnerOperator.SUBTRAKTION,
    TensorRechnerOperator.SKALARMULTIPLIKATION,
    TensorRechnerOperator.HADAMARD_PRODUKT,
    TensorRechnerOperator.TENSORPRODUKT,
    -> "Tensoroperationen"

    TensorRechnerOperator.KONTRAKTION,
    TensorRechnerOperator.ACHSENPERMUTATION,
    TensorRechnerOperator.TRANSPONIEREN,
    TensorRechnerOperator.ACHSENSCHNITT,
    TensorRechnerOperator.INDEXAUSWERTUNG,
    -> "Tensorindizes"

    TensorRechnerOperator.NORM -> "Tensoranalyse"
}

private fun tensorSignatur(operator: TensorRechnerOperator): RechnerOperatorSignatur = when (operator) {
    TensorRechnerOperator.ADDITION,
    TensorRechnerOperator.SUBTRAKTION,
    TensorRechnerOperator.HADAMARD_PRODUKT,
    TensorRechnerOperator.TENSORPRODUKT,
    -> RechnerOperatorSignatur(
        argumentRollen = listOf("links", "rechts"),
        eingangsArten = listOf(MathematikAnschlussArten.Tensor.id, MathematikAnschlussArten.Tensor.id),
    )

    TensorRechnerOperator.SKALARMULTIPLIKATION -> RechnerOperatorSignatur(
        argumentRollen = listOf("skalar", "tensor"),
        eingangsArten = listOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Tensor.id),
    )

    else -> RechnerOperatorSignatur(
        argumentRollen = listOf("tensor"),
        eingangsArten = listOf(MathematikAnschlussArten.Tensor.id),
    )
}
