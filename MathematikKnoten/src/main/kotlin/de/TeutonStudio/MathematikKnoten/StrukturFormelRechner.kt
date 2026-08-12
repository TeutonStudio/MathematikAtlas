package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val STRUKTUR_FORMEL_KNOTEN_ART = "mathematik.strukturFormel"
const val STRUKTUR_FORMEL_PARAMETER = "struktur.formel"
const val STRUKTUR_OPERATOR_PARAMETER = "struktur.operator"

/**
 * Kompakte, strukturierte Formelbeschreibung für Rechneroperationen.
 *
 * Die Darstellung ist absichtlich kein freier LaTeX-Text. Variablen werden mit
 * stabilen Namen referenziert und jeder Operator besitzt eine feste Signatur.
 */
sealed interface FormelAusdruck {
    data class Variable(
        val name: String,
        val typ: StrukturFormelTyp,
    ) : FormelAusdruck

    data class Literal(
        val wert: MathematischesObjekt,
        val typ: StrukturFormelTyp,
    ) : FormelAusdruck

    data class Operation(
        val operator: String,
        val argumente: List<FormelAusdruck>,
        val typ: StrukturFormelTyp,
    ) : FormelAusdruck
}

enum class StrukturFormelTyp {
    ZAHL,
    AUSSAGE,
    MENGE,
    TUPEL,
    VEKTOR,
    MATRIX,
    TENSOR,
    METHODE,
    OBJEKT,
}

data class StrukturOperatorDefinition(
    val stabileId: String,
    val titel: String,
    val argumentTypen: List<StrukturFormelTyp>,
    val ergebnisTyp: StrukturFormelTyp,
    val parameter: Map<String, String> = emptyMap(),
)

/**
 * Registry aller im strukturierten Formelrechner sichtbaren Operationen.
 * Rechnerknoten und Formelausdrücke greifen auf dieselben Definitionen zu.
 */
object StrukturOperatorRegister {
    private fun operator(
        id: String,
        titel: String,
        argumente: List<StrukturFormelTyp>,
        ergebnis: StrukturFormelTyp,
        parameter: Map<String, String> = emptyMap(),
    ) = StrukturOperatorDefinition(id, titel, argumente, ergebnis, parameter)

    val alle: List<StrukturOperatorDefinition> = buildList {
        add(operator("aussage.negation", "Negation", listOf(StrukturFormelTyp.AUSSAGE), StrukturFormelTyp.AUSSAGE))
        add(operator("aussage.konjunktion", "Konjunktion", listOf(StrukturFormelTyp.AUSSAGE, StrukturFormelTyp.AUSSAGE), StrukturFormelTyp.AUSSAGE))
        add(operator("aussage.disjunktion", "Disjunktion", listOf(StrukturFormelTyp.AUSSAGE, StrukturFormelTyp.AUSSAGE), StrukturFormelTyp.AUSSAGE))
        add(operator("aussage.implikation", "Implikation", listOf(StrukturFormelTyp.AUSSAGE, StrukturFormelTyp.AUSSAGE), StrukturFormelTyp.AUSSAGE))
        add(operator("aussage.aequivalenz", "Äquivalenz", listOf(StrukturFormelTyp.AUSSAGE, StrukturFormelTyp.AUSSAGE), StrukturFormelTyp.AUSSAGE))
        add(operator("aussage.adjunktion", "Adjunktion", listOf(StrukturFormelTyp.AUSSAGE, StrukturFormelTyp.AUSSAGE), StrukturFormelTyp.AUSSAGE))

        add(operator("vektor.radius", "Radius", listOf(StrukturFormelTyp.VEKTOR), StrukturFormelTyp.ZAHL))
        add(operator("vektor.norm", "Norm", listOf(StrukturFormelTyp.VEKTOR), StrukturFormelTyp.ZAHL))
        add(operator("vektor.skalarprodukt", "Skalarprodukt", listOf(StrukturFormelTyp.VEKTOR, StrukturFormelTyp.VEKTOR), StrukturFormelTyp.ZAHL))
        add(operator("vektor.kreuzprodukt", "Kreuzprodukt", listOf(StrukturFormelTyp.VEKTOR, StrukturFormelTyp.VEKTOR), StrukturFormelTyp.VEKTOR))
        add(operator("vektor.transponieren", "Transponieren", listOf(StrukturFormelTyp.VEKTOR), StrukturFormelTyp.VEKTOR))
        add(operator("vektor.addition", "Addition", listOf(StrukturFormelTyp.VEKTOR, StrukturFormelTyp.VEKTOR), StrukturFormelTyp.VEKTOR))
        add(operator("vektor.subtraktion", "Subtraktion", listOf(StrukturFormelTyp.VEKTOR, StrukturFormelTyp.VEKTOR), StrukturFormelTyp.VEKTOR))
        add(operator("vektor.skalarmultiplikation", "Skalarmultiplikation", listOf(StrukturFormelTyp.ZAHL, StrukturFormelTyp.VEKTOR), StrukturFormelTyp.VEKTOR))
        add(operator("vektor.dyadisch", "Dyadisches Produkt", listOf(StrukturFormelTyp.VEKTOR, StrukturFormelTyp.VEKTOR), StrukturFormelTyp.MATRIX))
        add(operator("vektor.normieren", "Normieren", listOf(StrukturFormelTyp.VEKTOR), StrukturFormelTyp.VEKTOR))

        add(operator("matrix.addition", "Addition", listOf(StrukturFormelTyp.MATRIX, StrukturFormelTyp.MATRIX), StrukturFormelTyp.MATRIX))
        add(operator("matrix.subtraktion", "Subtraktion", listOf(StrukturFormelTyp.MATRIX, StrukturFormelTyp.MATRIX), StrukturFormelTyp.MATRIX))
        add(operator("matrix.multiplikation", "Multiplikation", listOf(StrukturFormelTyp.MATRIX, StrukturFormelTyp.MATRIX), StrukturFormelTyp.MATRIX))
        add(operator("matrix.skalarmultiplikation", "Skalarmultiplikation", listOf(StrukturFormelTyp.ZAHL, StrukturFormelTyp.MATRIX), StrukturFormelTyp.MATRIX))
        add(operator("matrix.transponieren", "Transponieren", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.MATRIX))
        add(operator("matrix.invertieren", "Invertieren", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.MATRIX))
        add(operator("matrix.determinante", "Determinante", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.ZAHL))
        add(operator("matrix.spur", "Spur", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.ZAHL))
        add(operator("matrix.rang", "Rang", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.ZAHL))
        add(operator("matrix.charpolynom", "Charakteristisches Polynom", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.METHODE))
        add(operator("matrix.minpolynom", "Minimalpolynom", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.METHODE))
        add(operator("matrix.kern", "Kern", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.MENGE))
        add(operator("matrix.bild", "Bild", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.MENGE))
        add(operator("matrix.loesen", "Lineares Gleichungssystem", listOf(StrukturFormelTyp.MATRIX, StrukturFormelTyp.VEKTOR), StrukturFormelTyp.OBJEKT))
        add(operator("matrix.qr", "QR-Zerlegung", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.OBJEKT))
        add(operator("matrix.lu", "LU-Zerlegung", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.OBJEKT))
        add(operator("matrix.cholesky", "Cholesky-Zerlegung", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.OBJEKT))
        add(operator("matrix.svd", "Singulärwertzerlegung", listOf(StrukturFormelTyp.MATRIX), StrukturFormelTyp.OBJEKT))

        add(operator("tensor.addition", "Addition", listOf(StrukturFormelTyp.TENSOR, StrukturFormelTyp.TENSOR), StrukturFormelTyp.TENSOR))
        add(operator("tensor.subtraktion", "Subtraktion", listOf(StrukturFormelTyp.TENSOR, StrukturFormelTyp.TENSOR), StrukturFormelTyp.TENSOR))
        add(operator("tensor.skalarmultiplikation", "Skalarmultiplikation", listOf(StrukturFormelTyp.ZAHL, StrukturFormelTyp.TENSOR), StrukturFormelTyp.TENSOR))
        add(operator("tensor.tensorprodukt", "Tensorprodukt", listOf(StrukturFormelTyp.TENSOR, StrukturFormelTyp.TENSOR), StrukturFormelTyp.TENSOR))
        add(operator("tensor.transponieren", "Transponieren", listOf(StrukturFormelTyp.TENSOR), StrukturFormelTyp.TENSOR))
        add(operator("tensor.kontraktion", "Kontraktion", listOf(StrukturFormelTyp.TENSOR), StrukturFormelTyp.TENSOR))
        add(operator("tensor.norm", "Norm", listOf(StrukturFormelTyp.TENSOR), StrukturFormelTyp.ZAHL))
    }

    private val nachId = alle.associateBy(StrukturOperatorDefinition::stabileId)

    fun finde(id: String?): StrukturOperatorDefinition? = nachId[id]
}

object StrukturFormelKnotenVorlagen {
    val Formel = KnotenVorlage(
        art = STRUKTUR_FORMEL_KNOTEN_ART,
        name = "Strukturformel",
        kategorie = "Rechner: Formeln",
        beschreibung = "Verknüpft typisierte Aussagen-, Vektor-, Matrix- und Tensoroperationen zu einer wiederverwendbaren strukturierten Formel.",
        standardGröße = GraphGröße(300f, 135f),
        anschlüsse = listOf(
            strukturEingang("a", StrukturFormelTyp.OBJEKT, 0),
            strukturEingang("b", StrukturFormelTyp.OBJEKT, 1),
            strukturAusgang("wert", StrukturFormelTyp.OBJEKT),
        ),
        standardParameter = mapOf(
            STRUKTUR_FORMEL_PARAMETER to "a",
        ),
    )

    fun operatorVorlage(definition: StrukturOperatorDefinition): KnotenVorlage = KnotenVorlage(
        art = STRUKTUR_FORMEL_KNOTEN_ART,
        name = definition.titel,
        kategorie = "Rechner: Formeln",
        beschreibung = "Strukturierte Formeloperation ${definition.titel}.",
        standardGröße = GraphGröße(275f, 125f),
        anschlüsse = definition.argumentTypen.mapIndexed { index, typ ->
            strukturEingang("argument-${index + 1}", typ, index)
        } + strukturAusgang("wert", definition.ergebnisTyp),
        standardParameter = mapOf(
            STRUKTUR_OPERATOR_PARAMETER to definition.stabileId,
            STRUKTUR_FORMEL_PARAMETER to definition.stabileId,
        ) + definition.parameter,
    )
}

fun konfiguriereStrukturFormelOperator(
    knoten: KnotenDaten,
    operatorId: String,
): KnotenDaten {
    val definition = StrukturOperatorRegister.finde(operatorId)
        ?: error("Unbekannter Strukturformel-Operator '$operatorId'.")
    val bestehend = knoten.anschlüsse.associateBy { it.richtung to it.name }
    val gewuenscht = definition.argumentTypen.mapIndexed { index, typ ->
        strukturEingang("argument-${index + 1}", typ, index)
    } + strukturAusgang("wert", definition.ergebnisTyp)
    return knoten.copy(
        name = definition.titel,
        anschlüsse = gewuenscht.map { neu ->
            bestehend[neu.richtung to neu.name]?.let { alt -> neu.copy(id = alt.id) } ?: neu
        },
        parameter = knoten.parameter + mapOf(
            STRUKTUR_OPERATOR_PARAMETER to definition.stabileId,
            STRUKTUR_FORMEL_PARAMETER to definition.stabileId,
        ),
    )
}

/**
 * Sehr kleiner Parser für gespeicherte Formelausdrücke. Der Parser versteht
 * Variablen und verschachtelte Operatoraufrufe der Form op(a,b). Er ist bewusst
 * deterministisch und typisiert die Variablen erst über die Anschlussverträge.
 */
fun parseStrukturFormel(
    text: String,
    variablenTypen: Map<String, StrukturFormelTyp>,
): FormelAusdruck {
    val quelle = text.trim()
    require(quelle.isNotEmpty()) { "Die Strukturformel darf nicht leer sein." }

    fun parseTeil(ausdruck: String): FormelAusdruck {
        val teil = ausdruck.trim()
        variablenTypen[teil]?.let { return FormelAusdruck.Variable(teil, it) }

        val klammer = teil.indexOf('(')
        require(klammer > 0 && teil.endsWith(')')) { "Unbekannter Formelausdruck '$teil'." }
        val id = teil.substring(0, klammer).trim()
        val definition = StrukturOperatorRegister.finde(id)
            ?: error("Unbekannter Strukturformel-Operator '$id'.")
        val innen = teil.substring(klammer + 1, teil.length - 1)
        val argumentTexte = teileArgumente(innen)
        require(argumentTexte.size == definition.argumentTypen.size) {
            "${definition.titel} erwartet ${definition.argumentTypen.size} Argumente, erhielt aber ${argumentTexte.size}."
        }
        val argumente = argumentTexte.mapIndexed { index, argumentText ->
            val argument = parseTeil(argumentText)
            val erwartet = definition.argumentTypen[index]
            require(argument.typ() == erwartet || erwartet == StrukturFormelTyp.OBJEKT) {
                "Argument ${index + 1} von ${definition.titel} erwartet $erwartet, erhielt ${argument.typ()}."
            }
            argument
        }
        return FormelAusdruck.Operation(definition.stabileId, argumente, definition.ergebnisTyp)
    }

    return parseTeil(quelle)
}

private fun teileArgumente(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val teile = mutableListOf<String>()
    var tiefe = 0
    var start = 0
    text.forEachIndexed { index, zeichen ->
        when (zeichen) {
            '(' -> tiefe++
            ')' -> tiefe--
            ',' -> if (tiefe == 0) {
                teile += text.substring(start, index).trim()
                start = index + 1
            }
        }
        require(tiefe >= 0) { "Ungültige Klammerung in '$text'." }
    }
    require(tiefe == 0) { "Ungültige Klammerung in '$text'." }
    teile += text.substring(start).trim()
    return teile
}

private fun FormelAusdruck.typ(): StrukturFormelTyp = when (this) {
    is FormelAusdruck.Variable -> typ
    is FormelAusdruck.Literal -> typ
    is FormelAusdruck.Operation -> typ
}

internal fun MathematikAuswerterRegister.registriereStrukturFormelRechner() {
    registriere(STRUKTUR_FORMEL_KNOTEN_ART) { kontext ->
        val operator = kontext.knoten.parameter[STRUKTUR_OPERATOR_PARAMETER]
        if (operator != null) {
            werteStrukturOperatorAus(kontext, operator)
        } else {
            werteStrukturFormelAus(kontext)
        }
    }
}

private fun werteStrukturFormelAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val eingangsAnschlüsse = kontext.knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val typen = eingangsAnschlüsse.associate { it.name to strukturTyp(it.art) }
    val formel = parseStrukturFormel(
        kontext.knoten.parameter[STRUKTUR_FORMEL_PARAMETER].orEmpty(),
        typen,
    )
    val variablen: Map<String, MathematischesObjekt> = eingangsAnschlüsse.associate { anschluss ->
        anschluss.name to (
            kontext.eingänge[anschluss.name]?.mathematischesObjekt("Formeleingang '${anschluss.name}'")
                ?: error("Formeleingang '${anschluss.name}' fehlt.")
            )
    }
    val wert = werteFormelAusdruckAus(formel, variablen)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("wert" to BedingterWert(wert, kontext.annahmen())),
        eingänge = kontext.eingänge,
    )
}

private fun werteStrukturOperatorAus(
    kontext: KnotenAuswertungsKontext,
    operatorId: String,
): KnotenAuswertungsErgebnis {
    val definition = StrukturOperatorRegister.finde(operatorId)
        ?: return kontext.fehler("Unbekannter Strukturformel-Operator '$operatorId'.")
    val argumente: Map<String, MathematischesObjekt> = definition.argumentTypen.indices.associate { index ->
        val name = "argument-${index + 1}"
        name to (
            kontext.eingänge[name]?.mathematischesObjekt("Strukturoperator-Argument ${index + 1}")
                ?: error("Argument ${index + 1} fehlt.")
            )
    }
    val wert = werteFormelOperationAus(definition, argumente)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("wert" to BedingterWert(wert, kontext.annahmen())),
        eingänge = kontext.eingänge,
    )
}

private fun werteFormelAusdruckAus(
    ausdruck: FormelAusdruck,
    variablen: Map<String, MathematischesObjekt>,
): MathematischesObjekt = when (ausdruck) {
    is FormelAusdruck.Variable -> variablen[ausdruck.name]
        ?: error("Formelvariable '${ausdruck.name}' fehlt.")
    is FormelAusdruck.Literal -> ausdruck.wert
    is FormelAusdruck.Operation -> {
        val definition = StrukturOperatorRegister.finde(ausdruck.operator)
            ?: error("Unbekannter Formeloperator '${ausdruck.operator}'.")
        val argumente = ausdruck.argumente.mapIndexed { index, argument ->
            "argument-${index + 1}" to werteFormelAusdruckAus(argument, variablen)
        }.toMap()
        werteFormelOperationAus(definition, argumente)
    }
}

private fun werteFormelOperationAus(
    definition: StrukturOperatorDefinition,
    argumente: Map<String, MathematischesObjekt>,
): MathematischesObjekt = when {
    definition.stabileId.startsWith("aussage.") -> werteAussagenFormelAus(definition, argumente)
    definition.stabileId.startsWith("vektor.") -> werteVektorFormelAus(definition, argumente)
    definition.stabileId.startsWith("matrix.") -> werteMatrixFormelAus(definition, argumente)
    definition.stabileId.startsWith("tensor.") -> werteTensorFormelAus(definition, argumente)
    else -> error("Nicht unterstützter Formeloperator '${definition.stabileId}'.")
}

private fun werteAussagenFormelAus(
    definition: StrukturOperatorDefinition,
    argumente: Map<String, MathematischesObjekt>,
): MathematischesObjekt {
    fun aussage(index: Int) = argumente.getValue("argument-$index") as? Aussage
        ?: error("Argument $index muss eine Aussage sein.")
    return when (definition.stabileId) {
        "aussage.negation" -> Negation(aussage(1))
        "aussage.konjunktion" -> Konjunktion(listOf(aussage(1), aussage(2)))
        "aussage.disjunktion" -> Disjunktion(listOf(aussage(1), aussage(2)))
        "aussage.implikation" -> Implikation(aussage(1), aussage(2))
        "aussage.aequivalenz" -> Äquivalenz(aussage(1), aussage(2))
        "aussage.adjunktion" -> Adjunktion(aussage(1), aussage(2))
        else -> error("Nicht unterstützter Aussagenoperator '${definition.stabileId}'.")
    }
}

private fun werteVektorFormelAus(
    definition: StrukturOperatorDefinition,
    argumente: Map<String, MathematischesObjekt>,
): MathematischesObjekt {
    val operator = when (definition.stabileId) {
        "vektor.radius" -> VektorRechnerOperator.RADIUS
        "vektor.norm" -> VektorRechnerOperator.NORM
        "vektor.skalarprodukt" -> VektorRechnerOperator.SKALARPRODUKT
        "vektor.kreuzprodukt" -> VektorRechnerOperator.KREUZPRODUKT
        "vektor.transponieren" -> VektorRechnerOperator.TRANSPONIEREN
        "vektor.addition" -> VektorRechnerOperator.ADDITION
        "vektor.subtraktion" -> VektorRechnerOperator.SUBTRAKTION
        "vektor.skalarmultiplikation" -> VektorRechnerOperator.SKALARMULTIPLIKATION
        "vektor.dyadisch" -> VektorRechnerOperator.DYADISCHES_PRODUKT
        "vektor.normieren" -> VektorRechnerOperator.NORMIEREN
        else -> error("Nicht unterstützter Vektoroperator '${definition.stabileId}'.")
    }
    val vektoren = argumente.values.filter { esVektor(it) }.map(::formelVektorQuelle)
    val skalare = argumente.values.filterIsInstance<ZahlAusdruck>()
    return when (val ergebnis = VektorRechner.erzeuge(VektorRechnerAnfrage(operator, vektoren, skalare))) {
        is VektorRechnerErgebnis.ZahlWert -> ergebnis.wert
        is VektorRechnerErgebnis.VektorWert -> ergebnis.wert
        is VektorRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
    }
}

private fun werteMatrixFormelAus(
    definition: StrukturOperatorDefinition,
    argumente: Map<String, MathematischesObjekt>,
): MathematischesObjekt {
    fun matrix(index: Int) = argumente.getValue("argument-$index") as? Matrix
        ?: error("Argument $index muss eine Matrix sein.")
    fun zahl(index: Int) = argumente.getValue("argument-$index") as? ZahlAusdruck
        ?: error("Argument $index muss eine Zahl sein.")
    val konfiguration = MatrixRechnerKonfiguration()
    val ergebnis = when (definition.stabileId) {
        "matrix.addition" -> MatrixRechner.erzeuge(MatrixRechnerOperator.ADDITION, matrix(1), matrix(2), konfiguration)
        "matrix.subtraktion" -> MatrixRechner.erzeuge(MatrixRechnerOperator.SUBTRAKTION, matrix(1), matrix(2), konfiguration)
        "matrix.multiplikation" -> MatrixRechner.erzeuge(MatrixRechnerOperator.MULTIPLIKATION, matrix(1), matrix(2), konfiguration)
        "matrix.skalarmultiplikation" -> MatrixRechner.erzeuge(MatrixRechnerOperator.SKALARMULTIPLIKATION, matrix(2), skalar = zahl(1), konfiguration = konfiguration)
        "matrix.transponieren" -> MatrixRechner.erzeuge(MatrixRechnerOperator.TRANSPONIEREN, matrix(1), konfiguration = konfiguration)
        "matrix.invertieren" -> MatrixRechner.erzeuge(MatrixRechnerOperator.INVERTIEREN, matrix(1), konfiguration = konfiguration)
        "matrix.determinante" -> MatrixRechner.erzeuge(MatrixRechnerOperator.DETERMINANTE, matrix(1), konfiguration = konfiguration)
        "matrix.spur" -> MatrixRechner.erzeuge(MatrixRechnerOperator.SPUR, matrix(1), konfiguration = konfiguration)
        "matrix.rang" -> MatrixRechner.erzeuge(MatrixRechnerOperator.RANG, matrix(1), konfiguration = konfiguration)
        "matrix.charpolynom" -> MatrixRechner.erzeuge(MatrixRechnerOperator.CHARAKTERISTISCHES_POLYNOM, matrix(1), konfiguration = konfiguration)
        "matrix.minpolynom" -> MatrixRechner.erzeuge(MatrixRechnerOperator.MINIMALPOLYNOM, matrix(1), konfiguration = konfiguration)
        "matrix.kern" -> MatrixRechner.erzeuge(MatrixRechnerOperator.KERN, matrix(1), konfiguration = konfiguration)
        "matrix.bild" -> MatrixRechner.erzeuge(MatrixRechnerOperator.BILD, matrix(1), konfiguration = konfiguration)
        "matrix.loesen" -> {
            val rechteSeite = argumente.getValue("argument-2").numerischeKomponentenAnsicht()
            val vektor = when (rechteSeite) {
                is StrukturPruefung.Gueltig -> rechteSeite.wert.komponenten
                is StrukturPruefung.Bedingt -> error(rechteSeite.bedingungen.joinToString())
                is StrukturPruefung.Ungueltig -> error(rechteSeite.grund)
                is StrukturPruefung.Unentscheidbar -> error(rechteSeite.grund)
            }
            MatrixRechner.erzeuge(MatrixRechnerOperator.LGS_LOESEN, matrix(1), rechteSeite = vektor, konfiguration = konfiguration)
        }
        "matrix.qr" -> MatrixRechner.erzeuge(MatrixRechnerOperator.QR_ZERLEGUNG, matrix(1), konfiguration = konfiguration)
        "matrix.lu" -> MatrixRechner.erzeuge(MatrixRechnerOperator.LU_ZERLEGUNG, matrix(1), konfiguration = konfiguration)
        "matrix.cholesky" -> MatrixRechner.erzeuge(MatrixRechnerOperator.CHOLESKY_ZERLEGUNG, matrix(1), konfiguration = konfiguration)
        "matrix.svd" -> MatrixRechner.erzeuge(MatrixRechnerOperator.SINGULAERWERTZERLEGUNG, matrix(1), konfiguration = konfiguration)
        else -> error("Nicht unterstützter Matrixoperator '${definition.stabileId}'.")
    }
    return matrixErgebnisWert(ergebnis)
}

private fun matrixErgebnisWert(ergebnis: MatrixRechnerErgebnis): MathematischesObjekt = when (ergebnis) {
    is MatrixRechnerErgebnis.MatrixWert -> ergebnis.wert
    is MatrixRechnerErgebnis.ZahlWert -> ergebnis.wert
    is MatrixRechnerErgebnis.GanzzahlWert -> ergebnis.wert
    is MatrixRechnerErgebnis.MethodeWert ->
        ergebnis.wert.alsMathematischesObjekt("Matrixrechner-Methodenergebnis")
    is MatrixRechnerErgebnis.MengeWert -> ergebnis.wert
    is MatrixRechnerErgebnis.VektorWert -> ergebnis.wert
    is MatrixRechnerErgebnis.Zerlegung -> ergebnis.wert
    is MatrixRechnerErgebnis.Bedingt -> ergebnis.wert
        ?: error(ergebnis.bedingungen.joinToString().ifBlank { "Bedingtes Matrixergebnis ohne Wert." })
    is MatrixRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
}

private fun werteTensorFormelAus(
    definition: StrukturOperatorDefinition,
    argumente: Map<String, MathematischesObjekt>,
): MathematischesObjekt {
    val operator = when (definition.stabileId) {
        "tensor.addition" -> TensorRechnerOperator.ADDITION
        "tensor.subtraktion" -> TensorRechnerOperator.SUBTRAKTION
        "tensor.skalarmultiplikation" -> TensorRechnerOperator.SKALARMULTIPLIKATION
        "tensor.tensorprodukt" -> TensorRechnerOperator.TENSORPRODUKT
        "tensor.transponieren" -> TensorRechnerOperator.TRANSPONIEREN
        "tensor.kontraktion" -> TensorRechnerOperator.KONTRAKTION
        "tensor.norm" -> TensorRechnerOperator.NORM
        else -> error("Nicht unterstützter Tensoroperator '${definition.stabileId}'.")
    }
    val eingaben = argumente.entries.mapIndexed { index, eintrag ->
        TensorRechnerEingabe("argument-${index + 1}", eintrag.value)
    }
    return when (val ergebnis = TensorRechner.erzeuge(operator, eingaben, TensorRechnerKonfiguration())) {
        is TensorRechnerErgebnis.Wert -> ergebnis.objekt
        is TensorRechnerErgebnis.Bedingt -> ergebnis.objekt
            ?: error(ergebnis.bedingungen.joinToString().ifBlank { "Bedingtes Tensorergebnis ohne Wert." })
        is TensorRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
    }
}

private fun esVektor(objekt: MathematischesObjekt): Boolean =
    objekt is OrientierterVektor || objekt is Tupel

private fun formelVektorQuelle(objekt: MathematischesObjekt): VektorQuelle {
    val ansicht = objekt.numerischeKomponentenAnsicht()
    val komponenten = when (ansicht) {
        is StrukturPruefung.Gueltig -> ansicht.wert.komponenten
        is StrukturPruefung.Bedingt -> error(ansicht.bedingungen.joinToString())
        is StrukturPruefung.Ungueltig -> error(ansicht.grund)
        is StrukturPruefung.Unentscheidbar -> error(ansicht.grund)
    }
    val bereich = when {
        komponenten.any { it is KomplexeZahl } -> FundamentalerZahlbereich.KOMPLEX
        komponenten.all { it is RationaleZahl } -> FundamentalerZahlbereich.RATIONAL
        else -> FundamentalerZahlbereich.REELL
    }
    val vertrag = KartesischerKoordinatenVertrag(komponenten.size, bereich)
    return when (objekt) {
        is OrientierterVektor -> VektorQuelle.Vektor(objekt, vertrag)
        is Tupel -> VektorQuelle.Koordinaten(objekt, vertrag)
        else -> error("Kein Vektorwert.")
    }
}

private fun strukturEingang(name: String, typ: StrukturFormelTyp, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = typ.anschlussArt(),
    reihenfolge = reihenfolge,
)

private fun strukturAusgang(name: String, typ: StrukturFormelTyp) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = typ.anschlussArt(),
)

private fun StrukturFormelTyp.anschlussArt() = when (this) {
    StrukturFormelTyp.ZAHL -> MathematikAnschlussArten.Zahl.id
    StrukturFormelTyp.AUSSAGE -> MathematikAnschlussArten.Aussage.id
    StrukturFormelTyp.MENGE -> MathematikAnschlussArten.Menge.id
    StrukturFormelTyp.TUPEL -> MathematikAnschlussArten.Tupel.id
    StrukturFormelTyp.VEKTOR -> MathematikAnschlussArten.Vektor.id
    StrukturFormelTyp.MATRIX -> MathematikAnschlussArten.Matrix.id
    StrukturFormelTyp.TENSOR -> MathematikAnschlussArten.Tensor.id
    StrukturFormelTyp.METHODE -> MathematikAnschlussArten.Methode.id
    StrukturFormelTyp.OBJEKT -> MathematikAnschlussArten.Objekt.id
}

private fun strukturTyp(art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId) = when (art) {
    MathematikAnschlussArten.Zahl.id -> StrukturFormelTyp.ZAHL
    MathematikAnschlussArten.Aussage.id -> StrukturFormelTyp.AUSSAGE
    MathematikAnschlussArten.Menge.id -> StrukturFormelTyp.MENGE
    MathematikAnschlussArten.Tupel.id -> StrukturFormelTyp.TUPEL
    MathematikAnschlussArten.SpaltenVektor.id,
    MathematikAnschlussArten.ZeilenVektor.id,
    MathematikAnschlussArten.Vektor.id,
    -> StrukturFormelTyp.VEKTOR
    MathematikAnschlussArten.Matrix.id -> StrukturFormelTyp.MATRIX
    MathematikAnschlussArten.Tensor.id -> StrukturFormelTyp.TENSOR
    MathematikAnschlussArten.Methode.id -> StrukturFormelTyp.METHODE
    else -> StrukturFormelTyp.OBJEKT
}

private fun KnotenAuswertungsKontext.annahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

private fun KnotenAuswertungsKontext.fehler(nachricht: String) = KnotenAuswertungsErgebnis(
    ausgaben = emptyMap(),
    fehler = nachricht,
    eingänge = eingänge,
)
