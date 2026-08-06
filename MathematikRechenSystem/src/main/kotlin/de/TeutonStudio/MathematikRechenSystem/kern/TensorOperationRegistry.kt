package de.TeutonStudio.MathematikRechenSystem.kern

@JvmInline
value class TensorOperationId(val wert: String) {
    init { require(wert.isNotBlank()) }
}

@JvmInline
value class TensorHandleRolle(val wert: String) {
    init { require(wert.isNotBlank()) }
}

@JvmInline
value class TensorAchsenId(val wert: String) {
    init { require(wert.isNotBlank()) }
}

enum class TensorSignaturFamilie {
    UNAER,
    BINAER,
    VARIADISCH,
    ACHSENABHAENGIG,
    INDEXIERUNG,
    KONSTRUKTION,
    ZERLEGUNG,
    MEHRFACHAUSGANG,
}

enum class AchsenEingabeModus {
    TUPEL,
    DYNAMISCHE_EINZELHANDLES,
}

data class NormalisierteTensorAchse(
    val sichtbarerIndex: Int,
    val position: Int,
)

fun normalisiereTensorAchse(sichtbarerIndex: Int, stufe: Int): NormalisierteTensorAchse {
    require(stufe > 0) { "Die Tensorstufe muss positiv sein." }
    require(sichtbarerIndex != 0) { "Der sichtbare Achsenindex 0 ist ungültig." }
    val position = if (sichtbarerIndex > 0) sichtbarerIndex - 1 else stufe + sichtbarerIndex
    require(position in 0 until stufe) {
        "Achsenindex $sichtbarerIndex liegt außerhalb eines Tensors der Stufe $stufe."
    }
    return NormalisierteTensorAchse(sichtbarerIndex, position)
}

fun normalisiereTensorAchsen(
    sichtbareIndizes: Iterable<Int>,
    stufe: Int,
): List<NormalisierteTensorAchse> = sichtbareIndizes.map { normalisiereTensorAchse(it, stufe) }

sealed interface TensorAchsenSpezifikation {
    val modus: AchsenEingabeModus

    data class Tupel(
        val sichtbareIndizes: List<Int>,
    ) : TensorAchsenSpezifikation {
        override val modus: AchsenEingabeModus = AchsenEingabeModus.TUPEL
    }

    data class Dynamisch(
        val sichtbareIndizesNachRolle: Map<TensorHandleRolle, Int>,
    ) : TensorAchsenSpezifikation {
        override val modus: AchsenEingabeModus = AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES
    }
}

data class TensorOperationDefinition(
    val id: TensorOperationId,
    val familie: TensorSignaturFamilie,
    val eingangsRollen: List<TensorHandleRolle>,
    val ausgangsRollen: List<TensorHandleRolle>,
    val minimaleAchsenAnzahl: Int = 0,
    val maximaleAchsenAnzahl: Int? = minimaleAchsenAnzahl,
    val erlaubtSkalar: Boolean = false,
    val symbolischUnterstuetzt: Boolean = true,
) {
    init {
        require(eingangsRollen.isNotEmpty()) { "Eine Tensoroperation benötigt mindestens einen Eingang." }
        require(ausgangsRollen.isNotEmpty()) { "Eine Tensoroperation benötigt mindestens einen Ausgang." }
        require(eingangsRollen.distinct().size == eingangsRollen.size) { "Eingangsrollen müssen eindeutig sein." }
        require(ausgangsRollen.distinct().size == ausgangsRollen.size) { "Ausgangsrollen müssen eindeutig sein." }
        require(minimaleAchsenAnzahl >= 0)
        require(maximaleAchsenAnzahl == null || maximaleAchsenAnzahl >= minimaleAchsenAnzahl)
    }

    fun pruefeAchsenAnzahl(anzahl: Int): Boolean =
        anzahl >= minimaleAchsenAnzahl && (maximaleAchsenAnzahl == null || anzahl <= maximaleAchsenAnzahl)
}

class TensorOperationRegistry(
    definitionen: Iterable<TensorOperationDefinition>,
) {
    private val nachId: Map<TensorOperationId, TensorOperationDefinition>

    init {
        val liste = definitionen.toList()
        nachId = liste.associateBy { it.id }
        require(nachId.size == liste.size) { "Tensoroperation-IDs müssen eindeutig sein." }
    }

    fun definition(id: TensorOperationId): TensorOperationDefinition? = nachId[id]

    fun alle(): List<TensorOperationDefinition> = nachId.values.sortedBy { it.id.wert }

    fun familie(familie: TensorSignaturFamilie): List<TensorOperationDefinition> =
        alle().filter { it.familie == familie }
}

data class TensorMitAchsenIdentitaet(
    val tensor: Tensor,
    val achsenIds: List<TensorAchsenId>,
) {
    init {
        require(achsenIds.size == tensor.rang) { "Jede Tensorachse benötigt genau eine stabile ID." }
        require(achsenIds.distinct().size == achsenIds.size) { "Achsen-IDs müssen eindeutig sein." }
    }

    fun permutiereAchsen(permutation: List<Int>): TensorMitAchsenIdentitaet {
        prüfePermutation(permutation, tensor.rang)
        return TensorMitAchsenIdentitaet(
            tensor = tensor.permutiereAchsen(permutation),
            achsenIds = permutation.map(achsenIds::get),
        )
    }
}

fun Tensor.mitStabilenAchsen(
    achsenIds: List<TensorAchsenId> = List(rang) { index -> TensorAchsenId("achse-${index + 1}") },
): TensorMitAchsenIdentitaet = TensorMitAchsenIdentitaet(this, achsenIds)

object StandardTensorOperationen {
    private val eingang = TensorHandleRolle("tensor")
    private val ergebnis = TensorHandleRolle("ergebnis")
    private val links = TensorHandleRolle("links")
    private val rechts = TensorHandleRolle("rechts")

    val definitionen: List<TensorOperationDefinition> = listOf(
        TensorOperationDefinition(
            TensorOperationId("tensor.produkt"),
            TensorSignaturFamilie.BINAER,
            listOf(links, rechts),
            listOf(ergebnis),
            erlaubtSkalar = true,
        ),
        TensorOperationDefinition(
            TensorOperationId("tensor.kontraktion"),
            TensorSignaturFamilie.ACHSENABHAENGIG,
            listOf(eingang),
            listOf(ergebnis),
            minimaleAchsenAnzahl = 2,
            maximaleAchsenAnzahl = 2,
        ),
        TensorOperationDefinition(
            TensorOperationId("tensor.spur"),
            TensorSignaturFamilie.ACHSENABHAENGIG,
            listOf(eingang),
            listOf(ergebnis),
            minimaleAchsenAnzahl = 2,
            maximaleAchsenAnzahl = 2,
        ),
        TensorOperationDefinition(
            TensorOperationId("tensor.achsenPermutation"),
            TensorSignaturFamilie.ACHSENABHAENGIG,
            listOf(eingang),
            listOf(ergebnis),
            minimaleAchsenAnzahl = 1,
            maximaleAchsenAnzahl = null,
        ),
        TensorOperationDefinition(
            TensorOperationId("tensor.index"),
            TensorSignaturFamilie.INDEXIERUNG,
            listOf(eingang),
            listOf(ergebnis),
            minimaleAchsenAnzahl = 1,
            maximaleAchsenAnzahl = null,
        ),
        TensorOperationDefinition(
            TensorOperationId("matrix.determinante"),
            TensorSignaturFamilie.UNAER,
            listOf(eingang),
            listOf(TensorHandleRolle("wert")),
        ),
        TensorOperationDefinition(
            TensorOperationId("matrix.inverse"),
            TensorSignaturFamilie.UNAER,
            listOf(eingang),
            listOf(ergebnis),
        ),
        TensorOperationDefinition(
            TensorOperationId("matrix.rang"),
            TensorSignaturFamilie.UNAER,
            listOf(eingang),
            listOf(TensorHandleRolle("rang")),
        ),
        TensorOperationDefinition(
            TensorOperationId("matrix.eigen"),
            TensorSignaturFamilie.MEHRFACHAUSGANG,
            listOf(eingang),
            listOf(TensorHandleRolle("eigen.werte"), TensorHandleRolle("eigen.vektoren")),
        ),
        TensorOperationDefinition(
            TensorOperationId("matrix.qr"),
            TensorSignaturFamilie.ZERLEGUNG,
            listOf(eingang),
            listOf(TensorHandleRolle("qr.q"), TensorHandleRolle("qr.r")),
        ),
        TensorOperationDefinition(
            TensorOperationId("matrix.svd"),
            TensorSignaturFamilie.ZERLEGUNG,
            listOf(eingang),
            listOf(TensorHandleRolle("svd.u"), TensorHandleRolle("svd.s"), TensorHandleRolle("svd.vAdjungiert")),
        ),
        TensorOperationDefinition(
            TensorOperationId("matrix.jordan"),
            TensorSignaturFamilie.ZERLEGUNG,
            listOf(eingang),
            listOf(TensorHandleRolle("jordan.basis"), TensorHandleRolle("jordan.form")),
        ),
    )

    val registry = TensorOperationRegistry(definitionen)
}
