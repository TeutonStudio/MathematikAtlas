package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
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

const val STRUKTUR_RECHNER_FORMEL_AUSDRUCK = "strukturFormelAusdruck"
const val STRUKTUR_RECHNER_FORMEL_LATEX = "strukturFormelLatex"
const val STRUKTUR_RECHNER_FORMEL_VARIABLEN = "strukturFormelVariablen"
const val AUSSAGESATZ_FORMEL_ID = "aussage.formel"
const val VEKTOR_FORMEL_ID = "vektor.formel"
const val MATRIX_FORMEL_ID = "matrix.formel"
const val TENSOR_FORMEL_ID = "tensor.formel"
const val MATRIX_ZERLEGEN_RICHTUNG = "matrixZerlegen.richtung"

typealias StrukturRechnerEingang = de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerEingang
typealias StrukturRechnerOperatorDefinition = de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerOperatorDefinition
typealias StrukturRechnerKnotenFamilie = de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie

/** Kompatible Fassade; die fachliche Quelle liegt im Rechenkern. */
object StrukturRechnerOperatoren {
    fun fuer(familie: StrukturRechnerKnotenFamilie) = de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerOperatoren.fuer(familie)
    fun finde(familie: StrukturRechnerKnotenFamilie, id: String?) = de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerOperatoren.finde(familie, id)
}

object StrukturFormelRechnerVorlagen {
    private fun standard(familie: StrukturRechnerKnotenFamilie): KnotenVorlage {
        val operator = StrukturRechnerOperatoren.fuer(familie).first()
        return methodenfaehigeRechnerVorlage(
            KnotenVorlage(
                art = familie.knotenArt,
                name = familie.titel,
                kategorie = familie.kategorie,
                beschreibung = "Einheitlicher ${familie.titel} mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.",
                standardGröße = GraphGröße(300f, 145f),
                anschlüsse = operator.eingänge.mapIndexed { index, eingang ->
                    neuerEingang(eingang.name, eingang.typ, index)
                } + neuerAusgang(operator.ergebnisTyp),
                standardParameter = mapOf(RECHNER_OPERATOR_PARAMETER to operator.id),
            ),
        )
    }

    val Aussagesatz = standard(StrukturRechnerKnotenFamilie.AUSSAGESATZ)
    val Vektor = standard(StrukturRechnerKnotenFamilie.VEKTOR)
    val Matrix = standard(StrukturRechnerKnotenFamilie.MATRIX)
    val Tensor = standard(StrukturRechnerKnotenFamilie.TENSOR)
    val alle = listOf(Aussagesatz, Vektor, Matrix, Tensor)
}

fun strukturOperatorAlsFormel(definition: StrukturRechnerOperatorDefinition): FormelAusdruck.Operation =
    FormelAusdruck.Operation(
        id = "struktur-formel-${definition.id}-${System.nanoTime().toUInt()}",
        operatorId = definition.id,
        argumente = definition.eingänge.mapIndexed { index, eingang ->
            FormelArgument(
                rollenId = eingang.name,
                position = index,
                ausdruck = FormelAusdruck.Variable(
                    id = "struktur-variable-${eingang.name}-${System.nanoTime().toUInt()}",
                    name = eingang.name,
                    latex = eingang.name,
                    typ = eingang.typ,
                ),
            )
        },
        typ = definition.ergebnisTyp,
    )

fun konfiguriereStrukturRechner(
    knoten: KnotenDaten,
    familie: StrukturRechnerKnotenFamilie,
    operatorId: String,
): KnotenDaten {
    require(knoten.art == familie.knotenArt) { "Der Knoten gehört nicht zur Rechnerfamilie ${familie.name}." }
    val operator = StrukturRechnerOperatoren.finde(familie, operatorId)
    val gewünschte = if (familie == StrukturRechnerKnotenFamilie.MATRIX) {
        rechnerAnschlüsse(RechnerFamilie.MATRIX, operator.id)
    } else operator.eingänge.mapIndexed { index, eingang -> neuerEingang(eingang.name, eingang.typ, index) } + neuerAusgang(operator.ergebnisTyp)
    val produkt = familie == StrukturRechnerKnotenFamilie.MATRIX && operatorId == MatrixRechnerOperator.MATRIXPRODUKT.stabileId
    val produktEingänge = if (produkt) {
        val feste = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
        val vorhanden = if (knoten.parameter[RECHNER_OPERATOR_PARAMETER] == operatorId) knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.dynamischErzeugt }.maxOfOrNull { it.reihenfolge + 1 } ?: 0 else 0
        val basis = gewünschte.filter { it.richtung == AnschlussRichtung.Eingang }
        List(maxOf(feste, vorhanden)) { index -> basis[minOf(index, basis.lastIndex)].copy(
            name = if (index < 2) basis[index].name else "input${index + 1}", reihenfolge = index,
            dynamischErzeugt = index >= feste, kannSichErweitern = true) } + gewünschte.filter { it.richtung == AnschlussRichtung.Ausgang }
    } else gewünschte
    val anschlüsse = if (familie == StrukturRechnerKnotenFamilie.MATRIX)
        erhalteRechnerAnschlussIds(knoten.anschlüsse, produktEingänge, RechnerFamilie.MATRIX, operator.id)
    else erhalteRechnerAnschlussIds(knoten.anschlüsse, gewünschte)
    return normalisiereRechnerMethodenAnschluesse(
        knoten.copy(
            anschlüsse = anschlüsse,
            parameter = knoten.parameter
                .minus(STRUKTUR_RECHNER_FORMEL_AUSDRUCK)
                .minus(STRUKTUR_RECHNER_FORMEL_LATEX)
                .minus(STRUKTUR_RECHNER_FORMEL_VARIABLEN) +
                (RECHNER_OPERATOR_PARAMETER to operator.id),
        ),
    )
}

fun konfiguriereStrukturRechnerFormel(
    knoten: KnotenDaten,
    familie: StrukturRechnerKnotenFamilie,
    wurzel: FormelAusdruck,
    quantorVariable: String = "x",
): KnotenDaten {
    require(knoten.art == familie.knotenArt) { "Der Knoten gehört nicht zur Rechnerfamilie ${familie.name}." }
    require(FormelAusdruckPruefer.pruefe(wurzel) == FormelPruefung.Gueltig) { "Die Formel ist unvollständig." }
    val variablen = strukturFormelVariablen(wurzel)
    require(variablen.map { it.name }.distinct().size == variablen.size)
    val anschlüsse = erhalteRechnerAnschlussIds(knoten.anschlüsse, variablen.mapIndexed { index, variable ->
        neuerEingang(variable.name, variable.typ, index)
    } + neuerAusgang(wurzel.typ))
    return normalisiereRechnerMethodenAnschluesse(
        knoten.copy(
            anschlüsse = anschlüsse,
            eigenschaften = knoten.eigenschaften + mapOf(
                FORMEL_AST_EIGENSCHAFT to KnotenEigenschaft.Text(StrukturFormelCodec.kodieren(wurzel)),
                FORMEL_INTERPRETATION_EIGENSCHAFT to KnotenEigenschaft.Text(StrukturFormelDarstellung.latex(wurzel, quantorVariable))),
            parameter = knoten.parameter + mapOf(
                RECHNER_OPERATOR_PARAMETER to familie.formelOperatorId,
                STRUKTUR_RECHNER_FORMEL_AUSDRUCK to StrukturFormelCodec.kodieren(wurzel),
                STRUKTUR_RECHNER_FORMEL_LATEX to StrukturFormelDarstellung.latex(wurzel, quantorVariable),
                STRUKTUR_RECHNER_FORMEL_VARIABLEN to variablen.joinToString(",") { "${it.name}:${it.typ.name}" },
            ),
        ),
    )
}

fun ladeStrukturRechnerFormel(knoten: KnotenDaten): FormelAusdruck? =
    knoten.parameter[STRUKTUR_RECHNER_FORMEL_AUSDRUCK]
        ?.takeIf(String::isNotBlank)
        ?.let { runCatching { StrukturFormelCodec.dekodieren(it) }.getOrNull() }

private fun erhalteOderErzeugeEingang(
    knoten: KnotenDaten,
    name: String,
    typ: FormelTyp,
    reihenfolge: Int,
): AnschlussDaten {
    val art = anschlussArt(typ)
    val zulässige = zulässigeAnschlussArten(typ)
    val vorhanden = knoten.anschlüsse.firstOrNull { anschluss ->
        anschluss.name == name &&
            anschluss.richtung == AnschlussRichtung.Eingang &&
            (
                anschluss.art == art && anschluss.zulässigeArten == zulässige ||
                    anschluss.art in setOf(MathematikAnschlussArten.Objekt.id, MathematikAnschlussArten.AtlasWert.id) &&
                    art in anschluss.zulässigeArten &&
                    MathematikAnschlussArten.Methode.id in anschluss.zulässigeArten
                )
    }
    return vorhanden?.copy(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        zulässigeArten = zulässige,
        reihenfolge = reihenfolge,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: neuerEingang(name, typ, reihenfolge)
}

private fun erhalteOderErzeugeAusgang(knoten: KnotenDaten, typ: FormelTyp): AnschlussDaten {
    val art = anschlussArt(typ)
    val vorhanden = knoten.anschlüsse.firstOrNull {
        it.name == "wert" && it.richtung == AnschlussRichtung.Ausgang && it.art == art
    }
    return vorhanden?.copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
        zulässigeArten = emptySet(),
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: neuerAusgang(typ)
}

private fun neuerEingang(name: String, typ: FormelTyp, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = anschlussArt(typ),
    zulässigeArten = zulässigeAnschlussArten(typ),
    reihenfolge = reihenfolge,
)

private fun neuerAusgang(typ: FormelTyp) = AnschlussDaten(
    name = "wert",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = anschlussArt(typ),
)

fun anschlussArt(typ: FormelTyp): AnschlussArtId = when (typ) {
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

private fun zulässigeAnschlussArten(typ: FormelTyp): Set<AnschlussArtId> = when (typ) {
    FormelTyp.VEKTOR -> setOf(
        MathematikAnschlussArten.Vektor.id,
        MathematikAnschlussArten.SpaltenVektor.id,
        MathematikAnschlussArten.ZeilenVektor.id,
        MathematikAnschlussArten.Tupel.id,
    )
    FormelTyp.TENSOR -> setOf(
        MathematikAnschlussArten.Tensor.id,
        MathematikAnschlussArten.Matrix.id,
        MathematikAnschlussArten.Vektor.id,
        MathematikAnschlussArten.SpaltenVektor.id,
        MathematikAnschlussArten.ZeilenVektor.id,
        MathematikAnschlussArten.Tupel.id,
        MathematikAnschlussArten.Zahl.id,
    )
    else -> emptySet()
}

/** Registriert alle vier Rechner zuletzt, damit Formelmodus und dynamische Verträge maßgeblich bleiben. */
internal fun strukturFormelFachauswerter(familie: StrukturRechnerKnotenFamilie, basis: de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter? = null): de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter =
    de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter { kontext ->
        val operatorId = kontext.knoten.parameter[RECHNER_OPERATOR_PARAMETER]
            ?: kontext.knoten.parameter[TENSOR_OPERATION_ID]
        if (operatorId == familie.formelOperatorId || familie == StrukturRechnerKnotenFamilie.MATRIX || basis == null) {
            kontext.werteStrukturRechnerSicher(familie, operatorId)
        } else basis.auswerten(kontext)
    }

internal fun MathematikAuswerterRegister.registriereStrukturFormelRechner() {
    StrukturRechnerKnotenFamilie.entries.forEach { familie ->
        registriere(familie.knotenArt, strukturFormelFachauswerter(familie, finde(familie.knotenArt)))
    }
}

private fun KnotenAuswertungsKontext.werteStrukturRechnerSicher(
    familie: StrukturRechnerKnotenFamilie,
    operatorId: String?,
): KnotenAuswertungsErgebnis = runCatching {
    if (familie == StrukturRechnerKnotenFamilie.MATRIX && operatorId == MatrixRechnerOperator.ZERLEGEN.stabileId) {
        return werteMatrixZerlegenAus(this)
    }
    val wert = if (operatorId == familie.formelOperatorId) {
        werteStrukturFormelAus(this)
    } else {
        werteStrukturOperatorAus(this, familie, StrukturRechnerOperatoren.finde(familie, operatorId))
    }
    KnotenAuswertungsErgebnis(
        ausgaben = mapOf("wert" to BedingterWert(wert, annahmenFuerStrukturFormel())),
        eingänge = eingänge,
    )
}.getOrElse { ursache ->
    KnotenAuswertungsErgebnis(
        ausgaben = emptyMap(),
        eingänge = eingänge,
        fehler = ursache.message ?: "Der Strukturrechner konnte nicht ausgewertet werden.",
    )
}

private fun werteMatrixZerlegenAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis = runCatching {
    val matrix = kontext.mathematischerEingang("matrix", "Zerlegen")
    val richtung = runCatching {
        MatrixZerlegeRichtung.valueOf(kontext.knoten.parameter[MATRIX_ZERLEGEN_RICHTUNG] ?: MatrixZerlegeRichtung.ZEILEN.name)
    }.getOrDefault(MatrixZerlegeRichtung.ZEILEN)
    val form = when (matrix) {
        is Matrix -> listOf(RationaleZahl.von(matrix.zeilenAnzahl.toLong()), RationaleZahl.von(matrix.spaltenAnzahl.toLong()))
        is TypisiertesElement -> matrix.strukturForm
        else -> error("Zerlegen akzeptiert ausschließlich Matrizen.")
    }
    require(form == null || form.size == 2) { "Eine Matrixform benötigt genau zwei Achsen." }
    val relevanteAchse = if (richtung == MatrixZerlegeRichtung.ZEILEN) 0 else 1
    val laenge = form?.get(relevanteAchse)
    val konkret = laenge?.positiveStrukturDimensionOderNull()
    val ausgaben = if (konkret != null) {
        List(konkret) { index ->
            val name = if (richtung == MatrixZerlegeRichtung.ZEILEN) "zeile${index + 1}" else "spalte${index + 1}"
            name to BedingterWert(matrixStrukturSchnitt(matrix, richtung, index), kontext.annahmenFuerStrukturFormel())
        }.toMap()
    } else {
        mapOf("methode" to BedingterWert(matrixZerlegeMethode(matrix, richtung), kontext.annahmenFuerStrukturFormel()))
    }
    KnotenAuswertungsErgebnis(ausgaben = ausgaben, eingänge = kontext.eingänge)
}.getOrElse { fehler ->
    KnotenAuswertungsErgebnis(ausgaben = emptyMap(), eingänge = kontext.eingänge, fehler = fehler.message)
}

fun matrixZerlegeMethode(matrix: MathematischesObjekt, richtung: MatrixZerlegeRichtung): MathematischeMethode {
    val index = Variable(if (richtung == MatrixZerlegeRichtung.ZEILEN) "i" else "j")
    val achse = if (richtung == MatrixZerlegeRichtung.ZEILEN) 0 else 1
    val form = (matrix as? TypisiertesElement)?.strukturForm
    val ergebnisForm = form?.getOrNull(1 - achse)?.let(::listOf)
    val art = if (richtung == MatrixZerlegeRichtung.ZEILEN) StrukturZugriffsArt.Zeile else StrukturZugriffsArt.Spalte
    val anschlussArt = if (richtung == MatrixZerlegeRichtung.ZEILEN) "mathematik.vektor.zeile" else "mathematik.vektor.spalte"
    return MathematischeMethode(
        name = "zerlegen",
        parameter = listOf(index),
        vorschrift = strukturSchnitt(matrix, index, art, achse, anschlussArt, ergebnisForm),
        zielMenge = StrukturErgebnisMenge(anschlussArt, ergebnisForm, "\\operatorname{${if (richtung == MatrixZerlegeRichtung.ZEILEN) "Zeilen" else "Spalten"}}(A)"),
        werteVorräte = mapOf(index.name to EndlicheIndexMenge(strukturAchsenLaenge(matrix, achse))),
    )
}

private fun matrixStrukturSchnitt(
    matrix: MathematischesObjekt,
    richtung: MatrixZerlegeRichtung,
    index: Int,
): MathematischesObjekt = strukturSchnitt(
    matrix,
    RationaleZahl.von((index + 1).toLong()),
    if (richtung == MatrixZerlegeRichtung.ZEILEN) StrukturZugriffsArt.Zeile else StrukturZugriffsArt.Spalte,
    if (richtung == MatrixZerlegeRichtung.ZEILEN) 0 else 1,
    if (richtung == MatrixZerlegeRichtung.ZEILEN) "mathematik.vektor.zeile" else "mathematik.vektor.spalte",
)

private fun ZahlAusdruck.positiveStrukturDimensionOderNull(): Int? = (this as? RationaleZahl)?.let { zahl ->
    if (zahl.nenner == java.math.BigInteger.ONE && zahl.zähler.signum() > 0 && zahl.zähler.bitLength() < 31) zahl.zähler.toInt() else null
}

private fun werteStrukturFormelAus(kontext: KnotenAuswertungsKontext): AtlasWert {
    val wurzel = ladeStrukturRechnerFormel(kontext.knoten) ?: error("Die gespeicherte Strukturformel fehlt oder ist beschädigt.")
    require(FormelAusdruckPruefer.pruefe(wurzel) == FormelPruefung.Gueltig) { "Die gespeicherte Strukturformel ist unvollständig." }
    val variablen = strukturFormelVariablen(wurzel).associate { variable ->
        variable.name to (
            kontext.eingänge[variable.name]?.mathematischesObjekt("Formeleingang '${variable.name}'")
                ?: error("Formeleingang '${variable.name}' fehlt.")
            )
    }
    return werteFormelAusdruckAus(wurzel, variablen, kontext.knoten)
}

private fun werteFormelAusdruckAus(
    ausdruck: FormelAusdruck,
    variablen: Map<String, MathematischesObjekt>,
    knoten: KnotenDaten,
): AtlasWert = when (ausdruck) {
    is FormelAusdruck.Literal -> ausdruck.wert
    is FormelAusdruck.Variable -> variablen[ausdruck.name] ?: error("Variable ${ausdruck.name} ist nicht belegt.")
    is FormelAusdruck.Platzhalter -> error("Die Formel enthält den offenen Platzhalter ${ausdruck.beschriftung}.")
    is FormelAusdruck.Operation -> {
        val argumente = ausdruck.argumente.sortedBy { it.position }.associate { argument ->
            argument.rollenId to werteFormelAusdruckAus(argument.ausdruck, variablen, knoten)
        }
        werteFormelOperationAus(ausdruck.operatorId, argumente, knoten)
    }
}

private fun werteFormelOperationAus(
    operatorId: String,
    argumente: Map<String, AtlasWert>,
    knoten: KnotenDaten,
): AtlasWert {
    val mathematischeArgumente = argumente.mapValues { (rolle, wert) ->
        wert.alsMathematischesObjekt("Formelargument '$rolle'")
    }
    val formelFamilie = RechnerOperatorDefinitionen.alle.firstOrNull { it.id == operatorId && it.formelFamilie != null }?.formelFamilie
    return when {
        operatorId.startsWith("zahl.") -> werteZahlFormelAus(operatorId, mathematischeArgumente.values.toList())
        operatorId.startsWith("aussage.") -> werteAussagenFormelAus(operatorId, mathematischeArgumente, knoten)
        formelFamilie == OperatorFamilie.VEKTOR -> werteVektorFormelAus(operatorId, mathematischeArgumente)
        formelFamilie == OperatorFamilie.MATRIX -> werteMatrixFormelAus(operatorId, mathematischeArgumente)
        formelFamilie == OperatorFamilie.TENSOR -> werteTensorFormelAus(operatorId, mathematischeArgumente, knoten)
        else -> error("Unbekannter Formeloperator $operatorId.")
    }
}

private fun werteZahlFormelAus(operatorId: String, argumente: List<MathematischesObjekt>): ZahlAusdruck {
    val zahlen = argumente.map { it as? ZahlAusdruck ?: error("$operatorId benötigt Zahlargumente.") }
    return when (operatorId) {
        "zahl.addition" -> addition(zahlen)
        "zahl.subtraktion" -> subtraktion(zahlen[0], zahlen[1])
        "zahl.multiplikation" -> multiplikation(zahlen[0], zahlen[1])
        "zahl.division" -> Division(zahlen[0], zahlen[1])
        "zahl.potenz" -> Potenz(zahlen[0], zahlen[1])
        "zahl.wurzel" -> Wurzel(zahlen[0])
        else -> error("Nicht unterstützter Zahloperator $operatorId.")
    }
}

private fun werteAussagenFormelAus(
    operatorId: String,
    argumente: Map<String, MathematischesObjekt>,
    knoten: KnotenDaten,
): MathematischesObjekt {
    val operator = AussagenSatzOperator.entries.firstOrNull { it.stabileId == operatorId }
        ?: error("Unbekannter Aussagenoperator $operatorId.")
    val aussagen = argumente.values.filterIsInstance<Aussage>().map(::LogischesAtom)
    val bereich = argumente["bereich"] as? MengenAusdruck ?: NatürlicheZahlen
    val variable = LogischeVariable(
        knoten.parameter["variablenId"].orEmpty().ifBlank { "x" },
        knoten.parameter["variablenName"].orEmpty().ifBlank { "x" },
        bereich,
    )
    return when (val ergebnis = AussagenSatzRechner.erzeuge(operator, aussagen, variable, knoten.parameter["praedikatName"])) {
        is AussagenSatzErgebnis.AussageWert -> ergebnis.aussage
        is AussagenSatzErgebnis.PraedikatWert -> ergebnis.praedikat
        is AussagenSatzErgebnis.Ungueltig -> error(ergebnis.nachricht)
    }
}

private fun werteVektorFormelAus(
    operatorId: String,
    argumente: Map<String, MathematischesObjekt>,
): MathematischesObjekt {
    val operator = VektorRechnerOperator.entries.firstOrNull { it.stabileId == operatorId }
        ?: error("Unbekannter Vektoroperator $operatorId.")
    val quellen = argumente.values.mapNotNull(::vektorQuelleOderNull)
    val skalare = argumente.values.filterIsInstance<ZahlAusdruck>()
    return when (val ergebnis = VektorRechner.erzeuge(VektorRechnerAnfrage(operator, quellen, skalare))) {
        is VektorRechnerErgebnis.VektorWert -> ergebnis.wert
        is VektorRechnerErgebnis.ZahlWert -> ergebnis.wert
        is VektorRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
    }
}

private fun werteMatrixFormelAus(
    operatorId: String,
    argumente: Map<String, MathematischesObjekt>,
): AtlasWert {
    val operator = MatrixRechnerOperator.entries.firstOrNull { it.stabileId == operatorId }
        ?: error("Unbekannter Matrixoperator $operatorId.")
    val matrizen = argumente.mapNotNull { (rolle, objekt) ->
        (if (operator == MatrixRechnerOperator.MATRIXPRODUKT) objekt.alsMatrixFaktor() else objekt as? Matrix)?.let { MatrixOperand(rolle, it, FundamentalerZahlbereich.REELL) }
    }
    val skalare = argumente.values.filterIsInstance<ZahlAusdruck>()
    val vektoren = argumente.values.mapNotNull(::orientierterVektorOderNull)
    return matrixErgebnisWert(MatrixRechner.erzeuge(MatrixRechnerAnfrage(operator, matrizen, skalare, vektoren)))
}

private fun werteTensorFormelAus(
    operatorId: String,
    argumente: Map<String, MathematischesObjekt>,
    knoten: KnotenDaten,
): MathematischesObjekt {
    val operator = TensorRechnerOperator.entries.firstOrNull { it.stabileId == operatorId }
        ?: error("Unbekannter Tensoroperator $operatorId.")
    val eingaben = argumente.map { (rolle, objekt) -> TensorRechnerEingabe(rolle, objekt) }
    return tensorErgebnisWert(
        TensorRechner.erzeuge(operator, eingaben, tensorKonfiguration(knoten)),
    )
}

private fun werteStrukturOperatorAus(
    kontext: KnotenAuswertungsKontext,
    familie: StrukturRechnerKnotenFamilie,
    definition: StrukturRechnerOperatorDefinition,
): AtlasWert {
    val produkt = definition.id == MatrixRechnerOperator.MATRIXPRODUKT.stabileId
    val argumente = if (produkt) matrixProduktFaktorWerte(kontext)
    else definition.eingänge.associate { eingang ->
        eingang.name to (
            kontext.eingänge[eingang.name]?.mathematischesObjekt("Strukturoperator-Eingang '${eingang.name}'")
                ?: error("Eingang '${eingang.name}' fehlt.")
            )
    }
    return when (familie) {
        StrukturRechnerKnotenFamilie.AUSSAGESATZ -> werteAussagenFormelAus(definition.id, argumente, kontext.knoten)
        StrukturRechnerKnotenFamilie.VEKTOR -> werteVektorFormelAus(definition.id, argumente)
        StrukturRechnerKnotenFamilie.MATRIX -> werteMatrixFormelAus(definition.id, argumente)
        StrukturRechnerKnotenFamilie.TENSOR -> werteTensorFormelAus(definition.id, argumente, kontext.knoten)
    }
}

private fun matrixErgebnisWert(ergebnis: MatrixRechnerErgebnis): AtlasWert = when (ergebnis) {
    is MatrixRechnerErgebnis.MatrixWert -> ergebnis.wert
    is MatrixRechnerErgebnis.VektorWert -> ergebnis.wert
    is MatrixRechnerErgebnis.ZahlWert -> ergebnis.wert
    is MatrixRechnerErgebnis.MethodeWert -> ergebnis.wert
    is MatrixRechnerErgebnis.TupelWert -> ergebnis.wert
    is MatrixRechnerErgebnis.Bedingt -> error(ergebnis.bedingungen.joinToString())
    is MatrixRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
}

private fun tensorErgebnisWert(ergebnis: TensorRechnerErgebnis): MathematischesObjekt = when (ergebnis) {
    is TensorRechnerErgebnis.Wert -> ergebnis.objekt
    is TensorRechnerErgebnis.Bedingt -> error(ergebnis.bedingungen.joinToString())
    is TensorRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
}

private fun tensorKonfiguration(knoten: KnotenDaten) = TensorRechnerKonfiguration(
    achsen = parseStrukturIntListe(knoten.parameter["achsen"]),
    indizes = parseStrukturIntListe(knoten.parameter["indizes"]),
    permutation = parseStrukturIntListe(knoten.parameter["permutation"]),
)

private fun parseStrukturIntListe(wert: String?): List<Int> =
    wert.orEmpty().split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }

private fun vektorQuelleOderNull(objekt: MathematischesObjekt): VektorQuelle? {
    val vektor = orientierterVektorOderNull(objekt) ?: return null
    val vertrag = KartesischerKoordinatenVertrag(
        dimension = vektor.werte.size,
        zahlbereich = FundamentalerZahlbereich.REELL,
        basisId = "standard",
        koordinatensystemId = "kartesisch",
        standardBasis = true,
    )
    return if (objekt is Tupel) VektorQuelle.Koordinaten(objekt, vertrag)
    else VektorQuelle.Vektor(vektor, vertrag)
}

private fun orientierterVektorOderNull(objekt: MathematischesObjekt): OrientierterVektor? {
    return when (objekt) {
        is OrientierterVektor -> objekt
        is Tupel -> {
            val werte = objekt.elemente.map { it as? ZahlAusdruck ?: return null }
            SpaltenVektor(werte)
        }
        else -> null
    }
}

private fun KnotenAuswertungsKontext.annahmenFuerStrukturFormel() =
    eingänge.values.flatMap { it.annahmen }.toSet()
