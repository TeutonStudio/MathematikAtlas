package de.TeutonStudio.MathematikKnoten.enzyklopädie

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenArtId
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
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

data class RechnerOperatorEintrag(
    val stabileId: String,
    val familie: RechnerFamilienId,
    val knotenArt: KnotenArtId,
    val titel: String,
    val kategorie: String,
    val argumentRollen: List<String>,
    val stelligkeit: OperatorStelligkeit,
    val eingangsArten: List<AnschlussArtId>,
    val ausgangsArt: AnschlussArtId,
    val casVerfügbar: Boolean,
    val wissensId: WissensId,
    val varianten: Set<VariantenId> = setOf(VariantenId(stabileId)),
) {
    init {
        require(stabileId.isNotBlank())
        require(titel.isNotBlank())
        require(kategorie.isNotBlank())
        require(argumentRollen.size >= stelligkeit.mindestens || stelligkeit.höchstens == null)
        require(eingangsArten.isNotEmpty() || stelligkeit.mindestens == 0)
    }
}

object RechnerFamilienKatalog {
    val Zahlenrechner = RechnerFamilienId("rechner.zahlen")
    val Tensorrechner = RechnerFamilienId("rechner.tensor")

    val zahlenOperatoren: List<RechnerOperatorEintrag> = UniversellerZahlenOperator.entries.map { operator ->
        RechnerOperatorEintrag(
            stabileId = operator.stabileId,
            familie = Zahlenrechner,
            knotenArt = ZAHLENRECHNER_ART,
            titel = operator.titel,
            kategorie = zahlenKategorie(operator),
            argumentRollen = zahlenRollen(operator),
            stelligkeit = zahlenStelligkeit(operator),
            eingangsArten = zahlenEingangsArten(operator),
            ausgangsArt = MathematikAnschlussArten.Zahl.id,
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
            argumentRollen = tensorRollen(operator),
            stelligkeit = tensorStelligkeit(operator),
            eingangsArten = tensorEingangsArten(operator),
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

        val tensorIds = tensorOperatoren.map(RechnerOperatorEintrag::stabileId).toSet()
        val erwarteteTensorIds = TensorRechnerOperator.entries.map(TensorRechnerOperator::stabileId).toSet()
        (erwarteteTensorIds - tensorIds).forEach { add("Fehlender Tensoroperator: $it") }
        (tensorIds - erwarteteTensorIds).forEach { add("Unbekannter Tensoroperator: $it") }
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

private fun zahlenRollen(operator: UniversellerZahlenOperator): List<String> = when (operator) {
    UniversellerZahlenOperator.ADDITION,
    UniversellerZahlenOperator.SUBTRAKTION,
    UniversellerZahlenOperator.MULTIPLIKATION,
    UniversellerZahlenOperator.MINIMUM,
    UniversellerZahlenOperator.MAXIMUM,
    -> listOf("a", "b")

    UniversellerZahlenOperator.DIVISION -> listOf("zähler", "nenner")
    UniversellerZahlenOperator.POTENZ -> listOf("basis", "exponent")
    UniversellerZahlenOperator.WURZEL -> listOf("radikand", "exponent")
    UniversellerZahlenOperator.LOGARITHMUS -> listOf("basis", "argument")
    UniversellerZahlenOperator.ITERIERTE_SUMME,
    UniversellerZahlenOperator.ITERIERTES_PRODUKT,
    -> listOf("methode", "indexmenge")

    UniversellerZahlenOperator.INTEGRAL -> listOf("methode", "untereGrenze", "obereGrenze")
    UniversellerZahlenOperator.DIFFERENTIAL -> listOf("methode", "variable")
    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR -> listOf("radius", "winkel")
    UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH -> listOf("realteil", "imaginärteil")
    UniversellerZahlenOperator.MODULO -> listOf("dividend", "modul")
    else -> listOf("argument")
}

private fun zahlenStelligkeit(operator: UniversellerZahlenOperator): OperatorStelligkeit = when (operator) {
    UniversellerZahlenOperator.ADDITION,
    UniversellerZahlenOperator.MULTIPLIKATION,
    UniversellerZahlenOperator.MINIMUM,
    UniversellerZahlenOperator.MAXIMUM,
    -> OperatorStelligkeit(2, null)

    UniversellerZahlenOperator.INTEGRAL -> OperatorStelligkeit(3)
    else -> OperatorStelligkeit(zahlenRollen(operator).size)
}

private fun zahlenEingangsArten(operator: UniversellerZahlenOperator): List<AnschlussArtId> = when (operator) {
    UniversellerZahlenOperator.ITERIERTE_SUMME,
    UniversellerZahlenOperator.ITERIERTES_PRODUKT,
    -> listOf(MathematikAnschlussArten.Methode.id, MathematikAnschlussArten.Menge.id)

    UniversellerZahlenOperator.INTEGRAL -> listOf(
        MathematikAnschlussArten.Methode.id,
        MathematikAnschlussArten.Zahl.id,
        MathematikAnschlussArten.Zahl.id,
    )

    UniversellerZahlenOperator.DIFFERENTIAL -> listOf(
        MathematikAnschlussArten.Methode.id,
        MathematikAnschlussArten.Zahl.id,
    )

    else -> List(zahlenRollen(operator).size.coerceAtLeast(1)) { MathematikAnschlussArten.Zahl.id }
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

private fun tensorRollen(operator: TensorRechnerOperator): List<String> = when (operator) {
    TensorRechnerOperator.ADDITION,
    TensorRechnerOperator.SUBTRAKTION,
    TensorRechnerOperator.HADAMARD_PRODUKT,
    TensorRechnerOperator.TENSORPRODUKT,
    -> listOf("links", "rechts")

    TensorRechnerOperator.SKALARMULTIPLIKATION -> listOf("skalar", "tensor")
    else -> listOf("tensor")
}

private fun tensorStelligkeit(operator: TensorRechnerOperator): OperatorStelligkeit =
    OperatorStelligkeit(tensorRollen(operator).size)

private fun tensorEingangsArten(operator: TensorRechnerOperator): List<AnschlussArtId> = when (operator) {
    TensorRechnerOperator.SKALARMULTIPLIKATION -> listOf(
        MathematikAnschlussArten.Zahl.id,
        MathematikAnschlussArten.Tensor.id,
    )

    else -> List(tensorRollen(operator).size) { MathematikAnschlussArten.Tensor.id }
}
