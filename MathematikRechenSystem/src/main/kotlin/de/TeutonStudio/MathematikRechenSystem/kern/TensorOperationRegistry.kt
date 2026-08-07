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

enum class TensorUnterstuetzungsStatus {
    KONKRET_IMPLEMENTIERT,
    SYMBOLISCH_IMPLEMENTIERT,
    REGISTRIERT,
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

fun normalisiereTensorPermutation(
    sichtbareIndizes: List<Int>,
    stufe: Int,
): List<Int> {
    require(sichtbareIndizes.size == stufe) {
        "Eine Achsenpermutation benötigt genau $stufe sichtbare Indizes."
    }
    val positionen = normalisiereTensorAchsen(sichtbareIndizes, stufe).map { it.position }
    require(positionen.distinct().size == stufe) {
        "Eine Achsenpermutation darf jede Achse genau einmal enthalten."
    }
    return positionen
}

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

data class TensorOperatorParameter(
    val id: String,
    val erforderlich: Boolean = false,
    val standardWert: String? = null,
) {
    init { require(id.isNotBlank()) }
}

data class TensorOperationDefinition(
    val id: TensorOperationId,
    val titel: String,
    val familie: TensorSignaturFamilie,
    val eingangsRollen: List<TensorHandleRolle>,
    val ausgangsRollen: List<TensorHandleRolle>,
    val parameter: List<TensorOperatorParameter> = emptyList(),
    val minimaleAchsenAnzahl: Int = 0,
    val maximaleAchsenAnzahl: Int? = minimaleAchsenAnzahl,
    val erlaubtSkalar: Boolean = false,
    val symbolischUnterstuetzt: Boolean = true,
    val unterstuetzungsStatus: TensorUnterstuetzungsStatus = TensorUnterstuetzungsStatus.REGISTRIERT,
) {
    init {
        require(titel.isNotBlank())
        require(eingangsRollen.isNotEmpty()) { "Eine Tensoroperation benötigt mindestens einen Eingang." }
        require(ausgangsRollen.isNotEmpty()) { "Eine Tensoroperation benötigt mindestens einen Ausgang." }
        require(eingangsRollen.distinct().size == eingangsRollen.size) { "Eingangsrollen müssen eindeutig sein." }
        require(ausgangsRollen.distinct().size == ausgangsRollen.size) { "Ausgangsrollen müssen eindeutig sein." }
        require(parameter.map { it.id }.distinct().size == parameter.size) { "Operatorparameter müssen eindeutig sein." }
        require(minimaleAchsenAnzahl >= 0)
        require(maximaleAchsenAnzahl == null || maximaleAchsenAnzahl >= minimaleAchsenAnzahl)
    }

    fun pruefeAchsenAnzahl(anzahl: Int): Boolean =
        anzahl >= minimaleAchsenAnzahl && (maximaleAchsenAnzahl == null || anzahl <= maximaleAchsenAnzahl)

    fun besitztRolle(rolle: TensorHandleRolle): Boolean =
        rolle in eingangsRollen || rolle in ausgangsRollen
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

    fun definition(id: String?): TensorOperationDefinition? =
        id?.let(::TensorOperationId)?.let(nachId::get)

    fun alle(): List<TensorOperationDefinition> = nachId.values.sortedBy { it.id.wert }

    fun familie(familie: TensorSignaturFamilie): List<TensorOperationDefinition> =
        alle().filter { it.familie == familie }
}

data class TensorOperation(
    val operationId: TensorOperationId,
    val operanden: Map<TensorHandleRolle, MathematischesObjekt>,
    val achsen: TensorAchsenSpezifikation? = null,
    val parameter: Map<String, MathematischesObjekt> = emptyMap(),
    val voraussetzungen: Set<Aussage> = emptySet(),
    val unterstuetzungsStatus: TensorUnterstuetzungsStatus,
) : MathematischesObjekt {
    init {
        require(operanden.isNotEmpty()) { "Eine Tensoroperation benötigt mindestens einen Operanden." }
        require(parameter.keys.all { it.isNotBlank() })
    }

    override fun zuLatex(): String = buildString {
        append("\\operatorname{")
        append(operationId.wert.replace("_", "\\_"))
        append("}\\left(")
        append(operanden.entries.sortedBy { it.key.wert }.joinToString(",") { it.value.zuLatex() })
        achsen?.let {
            append(";")
            append(
                when (it) {
                    is TensorAchsenSpezifikation.Tupel -> it.sichtbareIndizes.joinToString(",")
                    is TensorAchsenSpezifikation.Dynamisch -> it.sichtbareIndizesNachRolle.entries
                        .sortedBy { eintrag -> eintrag.key.wert }
                        .joinToString(",") { eintrag -> "${eintrag.key.wert}=${eintrag.value}" }
                },
            )
        }
        append("\\right)")
    }
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
    private val tensor = TensorHandleRolle("tensor")
    private val ergebnis = TensorHandleRolle("ergebnis")
    private val links = TensorHandleRolle("links")
    private val rechts = TensorHandleRolle("rechts")
    private val skalar = TensorHandleRolle("skalar")

    private fun d(
        id: String,
        titel: String,
        familie: TensorSignaturFamilie,
        eingang: List<TensorHandleRolle>,
        ausgang: List<TensorHandleRolle> = listOf(ergebnis),
        parameter: List<TensorOperatorParameter> = emptyList(),
        minimaleAchsenAnzahl: Int = 0,
        maximaleAchsenAnzahl: Int? = minimaleAchsenAnzahl,
        erlaubtSkalar: Boolean = false,
        status: TensorUnterstuetzungsStatus = TensorUnterstuetzungsStatus.REGISTRIERT,
    ) = TensorOperationDefinition(
        id = TensorOperationId(id),
        titel = titel,
        familie = familie,
        eingangsRollen = eingang,
        ausgangsRollen = ausgang,
        parameter = parameter,
        minimaleAchsenAnzahl = minimaleAchsenAnzahl,
        maximaleAchsenAnzahl = maximaleAchsenAnzahl,
        erlaubtSkalar = erlaubtSkalar,
        unterstuetzungsStatus = status,
    )

    val definitionen: List<TensorOperationDefinition> = listOf(
        d("tensor.addition", "Addition", TensorSignaturFamilie.BINAER, listOf(links, rechts), status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.subtraktion", "Subtraktion", TensorSignaturFamilie.BINAER, listOf(links, rechts), status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.skalarmultiplikation", "Skalierung", TensorSignaturFamilie.BINAER, listOf(skalar, tensor), erlaubtSkalar = true, status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.hadamard", "Hadamard-Produkt", TensorSignaturFamilie.BINAER, listOf(links, rechts), status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.tensorprodukt", "Tensorprodukt", TensorSignaturFamilie.BINAER, listOf(links, rechts), erlaubtSkalar = true, status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.kontraktion", "Kontraktion", TensorSignaturFamilie.ACHSENABHAENGIG, listOf(tensor), minimaleAchsenAnzahl = 2, maximaleAchsenAnzahl = 2, status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.spur", "Spur", TensorSignaturFamilie.ACHSENABHAENGIG, listOf(tensor), ausgang = listOf(TensorHandleRolle("wert")), minimaleAchsenAnzahl = 2, maximaleAchsenAnzahl = 2, status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("tensor.achsenpermutation", "Achsenpermutation", TensorSignaturFamilie.ACHSENABHAENGIG, listOf(tensor), minimaleAchsenAnzahl = 1, maximaleAchsenAnzahl = null, status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.transponieren", "Transponieren", TensorSignaturFamilie.UNAER, listOf(tensor), status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.achsenschnitt", "Achsenschnitt", TensorSignaturFamilie.INDEXIERUNG, listOf(tensor), parameter = listOf(TensorOperatorParameter("index", true)), minimaleAchsenAnzahl = 1, maximaleAchsenAnzahl = 1, status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.indexauswertung", "Indexauswertung", TensorSignaturFamilie.INDEXIERUNG, listOf(tensor), ausgang = listOf(TensorHandleRolle("wert")), parameter = listOf(TensorOperatorParameter("indizes", true)), status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.norm", "Norm", TensorSignaturFamilie.UNAER, listOf(tensor), ausgang = listOf(TensorHandleRolle("wert")), status = TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT),
        d("tensor.reshape", "Reshape", TensorSignaturFamilie.KONSTRUKTION, listOf(tensor), parameter = listOf(TensorOperatorParameter("form", true)), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("tensor.konstruktion", "Tensor-Konstruktion", TensorSignaturFamilie.KONSTRUKTION, listOf(TensorHandleRolle("komponenten")), parameter = listOf(TensorOperatorParameter("form", true)), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("matrix.determinante", "Determinante", TensorSignaturFamilie.UNAER, listOf(tensor), ausgang = listOf(TensorHandleRolle("wert")), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("matrix.inverse", "Inverse", TensorSignaturFamilie.UNAER, listOf(tensor), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("matrix.rang", "Rang", TensorSignaturFamilie.UNAER, listOf(tensor), ausgang = listOf(TensorHandleRolle("rang")), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("matrix.eigen", "Eigenzerlegung", TensorSignaturFamilie.MEHRFACHAUSGANG, listOf(tensor), listOf(TensorHandleRolle("eigen.werte"), TensorHandleRolle("eigen.vektoren")), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("matrix.qr", "QR-Zerlegung", TensorSignaturFamilie.ZERLEGUNG, listOf(tensor), listOf(TensorHandleRolle("qr.q"), TensorHandleRolle("qr.r")), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("matrix.svd", "Singulärwertzerlegung", TensorSignaturFamilie.ZERLEGUNG, listOf(tensor), listOf(TensorHandleRolle("svd.u"), TensorHandleRolle("svd.s"), TensorHandleRolle("svd.vAdjungiert")), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("matrix.spektral", "Spektralzerlegung", TensorSignaturFamilie.ZERLEGUNG, listOf(tensor), listOf(TensorHandleRolle("spektral.projektoren"), TensorHandleRolle("spektral.werte")), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
        d("matrix.jordan", "Jordan-Zerlegung", TensorSignaturFamilie.ZERLEGUNG, listOf(tensor), listOf(TensorHandleRolle("jordan.basis"), TensorHandleRolle("jordan.form")), status = TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT),
    )

    val registry = TensorOperationRegistry(definitionen)
}

fun TensorRechnerOperator.alsTensorOperationId(): TensorOperationId = TensorOperationId(stabileId)

fun TensorOperationDefinition.alsBestehenderTensorOperatorOderNull(): TensorRechnerOperator? =
    TensorRechnerOperator.entries.firstOrNull { it.stabileId == id.wert }
