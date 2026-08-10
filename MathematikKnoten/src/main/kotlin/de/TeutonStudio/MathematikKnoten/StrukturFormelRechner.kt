package de.TeutonStudio.MathematikKnoten

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

data class StrukturRechnerEingang(
    val name: String,
    val typ: FormelTyp,
)

data class StrukturRechnerOperatorDefinition(
    val id: String,
    val titel: String,
    val symbolLatex: String,
    val eingänge: List<StrukturRechnerEingang>,
    val ergebnisTyp: FormelTyp,
    val definitionsLatex: String,
)

enum class StrukturRechnerKnotenFamilie(
    val knotenArt: String,
    val titel: String,
    val kategorie: String,
    val formelFamilie: StrukturFormelFamilie,
    val formelOperatorId: String,
) {
    AUSSAGESATZ(
        AussagenSatzRechner.KNOTEN_ART,
        "Aussagesatz",
        "Aussagen: Aussagenlogik",
        StrukturFormelFamilie.AUSSAGESATZ,
        AUSSAGESATZ_FORMEL_ID,
    ),
    VEKTOR(
        VektorRechner.KNOTEN_ART,
        "Vektorrechner",
        "Lineare Algebra",
        StrukturFormelFamilie.VEKTOR,
        VEKTOR_FORMEL_ID,
    ),
    MATRIX(
        MatrixRechner.KNOTEN_ART,
        "Matrixrechner",
        "Lineare Algebra: Matrizen",
        StrukturFormelFamilie.MATRIX,
        MATRIX_FORMEL_ID,
    ),
    TENSOR(
        TensorRechner.KNOTEN_ART,
        "Tensorrechner",
        "Tensoren",
        StrukturFormelFamilie.TENSOR,
        TENSOR_FORMEL_ID,
    ),
    ;

    companion object {
        fun fuerKnotenArt(art: String): StrukturRechnerKnotenFamilie? = entries.firstOrNull { it.knotenArt == art }
    }
}

object StrukturRechnerOperatoren {
    private fun e(name: String, typ: FormelTyp) = StrukturRechnerEingang(name, typ)
    private fun d(
        id: String,
        titel: String,
        symbol: String,
        ergebnis: FormelTyp,
        definition: String,
        vararg eingänge: StrukturRechnerEingang,
    ) = StrukturRechnerOperatorDefinition(id, titel, symbol, eingänge.toList(), ergebnis, definition)

    private val aussage = listOf(
        d("aussage.negation", "Negation", "\\neg A", FormelTyp.AUSSAGE, "\\neg A", e("aussage", FormelTyp.AUSSAGE)),
        d("aussage.konjunktion", "Konjunktion", "A\\land B", FormelTyp.AUSSAGE, "A\\land B", e("a", FormelTyp.AUSSAGE), e("b", FormelTyp.AUSSAGE)),
        d("aussage.disjunktion", "Disjunktion", "A\\lor B", FormelTyp.AUSSAGE, "A\\lor B", e("a", FormelTyp.AUSSAGE), e("b", FormelTyp.AUSSAGE)),
        d("aussage.adjunktion", "Adjunktion", "A\\mathbin{\\dot\\lor}B", FormelTyp.AUSSAGE, "(A\\lor B)\\land\\neg(A\\land B)", e("a", FormelTyp.AUSSAGE), e("b", FormelTyp.AUSSAGE)),
        d("aussage.implikation", "Implikation", "A\\Rightarrow B", FormelTyp.AUSSAGE, "A\\Rightarrow B", e("voraussetzung", FormelTyp.AUSSAGE), e("folgerung", FormelTyp.AUSSAGE)),
        d("aussage.aequivalenz", "Äquivalenz", "A\\Leftrightarrow B", FormelTyp.AUSSAGE, "A\\Leftrightarrow B", e("links", FormelTyp.AUSSAGE), e("rechts", FormelTyp.AUSSAGE)),
        d("aussage.allquantor", "Allquantor", "\\forall x", FormelTyp.AUSSAGE, "\\forall x\\in M:\\;A(x)", e("aussage", FormelTyp.AUSSAGE), e("bereich", FormelTyp.MENGE)),
        d("aussage.existenzquantor", "Existenzquantor", "\\exists x", FormelTyp.AUSSAGE, "\\exists x\\in M:\\;A(x)", e("aussage", FormelTyp.AUSSAGE), e("bereich", FormelTyp.MENGE)),
        d("aussage.eindeutigerExistenzquantor", "Eindeutiger Existenzquantor", "\\exists! x", FormelTyp.AUSSAGE, "\\exists! x\\in M:\\;A(x)", e("aussage", FormelTyp.AUSSAGE), e("bereich", FormelTyp.MENGE)),
    )

    private val vektor = listOf(
        d("vektor.addition", "Addition", "u+v", FormelTyp.VEKTOR, "(u_i)+(v_i)=(u_i+v_i)", e("links", FormelTyp.VEKTOR), e("rechts", FormelTyp.VEKTOR)),
        d("vektor.subtraktion", "Subtraktion", "u-v", FormelTyp.VEKTOR, "(u_i)-(v_i)=(u_i-v_i)", e("links", FormelTyp.VEKTOR), e("rechts", FormelTyp.VEKTOR)),
        d("vektor.skalarmultiplikation", "Skalarmultiplikation", "s\\cdot v", FormelTyp.VEKTOR, "s(v_i)=(sv_i)", e("skalar", FormelTyp.ZAHL), e("vektor", FormelTyp.VEKTOR)),
        d("vektor.negation", "Negation", "-v", FormelTyp.VEKTOR, "-(v_i)=(-v_i)", e("vektor", FormelTyp.VEKTOR)),
        d("vektor.skalarprodukt", "Skalarprodukt", "\\langle u,v\\rangle", FormelTyp.ZAHL, "\\langle u,v\\rangle=\\sum_i u_i v_i", e("links", FormelTyp.VEKTOR), e("rechts", FormelTyp.VEKTOR)),
        d("vektor.kreuzprodukt", "Kreuzprodukt", "u\\times v", FormelTyp.VEKTOR, "u\\times v", e("links", FormelTyp.VEKTOR), e("rechts", FormelTyp.VEKTOR)),
        d("vektor.norm", "Norm", "\\lVert v\\rVert", FormelTyp.ZAHL, "\\lVert v\\rVert=\\sqrt{\\sum_i v_i^2}", e("vektor", FormelTyp.VEKTOR)),
        d("vektor.normalisierung", "Normalisierung", "v/\\lVert v\\rVert", FormelTyp.VEKTOR, "\\widehat v=v/\\lVert v\\rVert", e("vektor", FormelTyp.VEKTOR)),
        d("vektor.hadamard", "Hadamard-Produkt", "u\\circ v", FormelTyp.VEKTOR, "(u_i)\\circ(v_i)=(u_iv_i)", e("links", FormelTyp.VEKTOR), e("rechts", FormelTyp.VEKTOR)),
        d("vektor.projektion", "Projektion", "\\operatorname{proj}_v(u)", FormelTyp.VEKTOR, "\\operatorname{proj}_v(u)=\\frac{\\langle u,v\\rangle}{\\langle v,v\\rangle}v", e("vektor", FormelTyp.VEKTOR), e("ziel", FormelTyp.VEKTOR)),
        d("vektor.winkel", "Winkel", "\\angle(u,v)", FormelTyp.ZAHL, "\\angle(u,v)=\\arccos\\frac{\\langle u,v\\rangle}{\\lVert u\\rVert\\lVert v\\rVert}", e("links", FormelTyp.VEKTOR), e("rechts", FormelTyp.VEKTOR)),
    )

    private val matrix = listOf(
        d("matrix.addition", "Addition", "A+B", FormelTyp.MATRIX, "(A+B)_{ij}=a_{ij}+b_{ij}", e("a", FormelTyp.MATRIX), e("b", FormelTyp.MATRIX)),
        d("matrix.subtraktion", "Subtraktion", "A-B", FormelTyp.MATRIX, "(A-B)_{ij}=a_{ij}-b_{ij}", e("links", FormelTyp.MATRIX), e("rechts", FormelTyp.MATRIX)),
        d("matrix.skalarmultiplikation", "Skalarmultiplikation", "sA", FormelTyp.MATRIX, "(sA)_{ij}=sa_{ij}", e("skalar", FormelTyp.ZAHL), e("matrix", FormelTyp.MATRIX)),
        d("matrix.produkt", "Matrixprodukt", "AB", FormelTyp.MATRIX, "(AB)_{ij}=\\sum_k a_{ik}b_{kj}", e("links", FormelTyp.MATRIX), e("rechts", FormelTyp.MATRIX)),
        d("matrix.hadamard", "Hadamard-Produkt", "A\\circ B", FormelTyp.MATRIX, "(A\\circ B)_{ij}=a_{ij}b_{ij}", e("links", FormelTyp.MATRIX), e("rechts", FormelTyp.MATRIX)),
        d("matrix.vektorProdukt", "Matrix-Vektor-Produkt", "Av", FormelTyp.VEKTOR, "(Av)_i=\\sum_j a_{ij}v_j", e("matrix", FormelTyp.MATRIX), e("vektor", FormelTyp.VEKTOR)),
        d("matrix.transponieren", "Transponieren", "A^{\\mathsf T}", FormelTyp.MATRIX, "(A^{\\mathsf T})_{ij}=a_{ji}", e("matrix", FormelTyp.MATRIX)),
        d("matrix.inverse", "Inverse", "A^{-1}", FormelTyp.MATRIX, "AA^{-1}=A^{-1}A=I", e("matrix", FormelTyp.MATRIX)),
        d("matrix.potenz", "Potenz", "A^n", FormelTyp.MATRIX, "A^n=\\prod_{k=1}^{n}A", e("matrix", FormelTyp.MATRIX), e("exponent", FormelTyp.ZAHL)),
        d("matrix.determinante", "Determinante", "\\det A", FormelTyp.ZAHL, "\\det(A)", e("matrix", FormelTyp.MATRIX)),
        d("matrix.spur", "Spur", "\\operatorname{tr}A", FormelTyp.ZAHL, "\\operatorname{tr}(A)=\\sum_i a_{ii}", e("matrix", FormelTyp.MATRIX)),
        d("matrix.rang", "Rang", "\\operatorname{rang}A", FormelTyp.ZAHL, "\\operatorname{rang}(A)", e("matrix", FormelTyp.MATRIX)),
        d("matrix.hauptdiagonale", "Hauptdiagonale", "\\operatorname{diag}A", FormelTyp.TUPEL, "(a_{11},a_{22},\\ldots)", e("matrix", FormelTyp.MATRIX)),
        d("matrix.nebendiagonale", "Nebendiagonale", "\\operatorname{antidiag}A", FormelTyp.TUPEL, "(a_{1n},a_{2,n-1},\\ldots)", e("matrix", FormelTyp.MATRIX)),
        d("matrix.charakteristischesPolynom", "Charakteristisches Polynom", "\\chi_A", FormelTyp.METHODE, "\\chi_A(\\lambda)=\\det(A-\\lambda I)", e("matrix", FormelTyp.MATRIX)),
        d("matrix.minimalpolynom", "Minimalpolynom", "m_A", FormelTyp.METHODE, "m_A(A)=0,\\quad m_A\\mid\\chi_A", e("matrix", FormelTyp.MATRIX)),
    )

    private val tensor = listOf(
        d("tensor.addition", "Addition", "A+B", FormelTyp.TENSOR, "(A+B)_{i_1\\ldots i_n}=a_{i_1\\ldots i_n}+b_{i_1\\ldots i_n}", e("links", FormelTyp.TENSOR), e("rechts", FormelTyp.TENSOR)),
        d("tensor.subtraktion", "Subtraktion", "A-B", FormelTyp.TENSOR, "(A-B)_I=a_I-b_I", e("links", FormelTyp.TENSOR), e("rechts", FormelTyp.TENSOR)),
        d("tensor.skalarmultiplikation", "Skalarmultiplikation", "sA", FormelTyp.TENSOR, "(sA)_I=sa_I", e("skalar", FormelTyp.ZAHL), e("tensor", FormelTyp.TENSOR)),
        d("tensor.hadamard", "Hadamard-Produkt", "A\\circ B", FormelTyp.TENSOR, "(A\\circ B)_I=a_Ib_I", e("links", FormelTyp.TENSOR), e("rechts", FormelTyp.TENSOR)),
        d("tensor.tensorprodukt", "Tensorprodukt", "A\\otimes B", FormelTyp.TENSOR, "(A\\otimes B)_{IJ}=a_Ib_J", e("links", FormelTyp.TENSOR), e("rechts", FormelTyp.TENSOR)),
        d("tensor.kontraktion", "Kontraktion", "\\operatorname{kontr}A", FormelTyp.TENSOR, "\\operatorname{kontr}_{p,q}(A)", e("tensor", FormelTyp.TENSOR)),
        d("tensor.achsenpermutation", "Achsenpermutation", "\\operatorname{perm}A", FormelTyp.TENSOR, "(\\operatorname{perm}_\\sigma A)_{i_1\\ldots i_n}=a_{i_{\\sigma(1)}\\ldots i_{\\sigma(n)}}", e("tensor", FormelTyp.TENSOR)),
        d("tensor.transponieren", "Transponieren", "A^{\\mathsf T}", FormelTyp.TENSOR, "A^{\\mathsf T}=\\operatorname{perm}_{(2,1,3,\\ldots)}A", e("tensor", FormelTyp.TENSOR)),
        d("tensor.achsenschnitt", "Achsenschnitt", "A_{i_k=c}", FormelTyp.TENSOR, "A_{i_k=c}", e("tensor", FormelTyp.TENSOR)),
        d("tensor.indexauswertung", "Indexauswertung", "A_I", FormelTyp.ZAHL, "A_{i_1\\ldots i_n}", e("tensor", FormelTyp.TENSOR)),
        d("tensor.norm", "Norm", "\\lVert A\\rVert", FormelTyp.ZAHL, "\\lVert A\\rVert=\\sqrt{\\sum_I a_I^2}", e("tensor", FormelTyp.TENSOR)),
    )

    fun fuer(familie: StrukturRechnerKnotenFamilie): List<StrukturRechnerOperatorDefinition> = when (familie) {
        StrukturRechnerKnotenFamilie.AUSSAGESATZ -> aussage
        StrukturRechnerKnotenFamilie.VEKTOR -> vektor
        StrukturRechnerKnotenFamilie.MATRIX -> matrix
        StrukturRechnerKnotenFamilie.TENSOR -> tensor
    }

    fun finde(familie: StrukturRechnerKnotenFamilie, id: String?): StrukturRechnerOperatorDefinition =
        fuer(familie).firstOrNull { it.id == id } ?: fuer(familie).first()
}

object StrukturFormelRechnerVorlagen {
    private fun standard(familie: StrukturRechnerKnotenFamilie): KnotenVorlage {
        val operator = StrukturRechnerOperatoren.fuer(familie).first()
        return KnotenVorlage(
            art = familie.knotenArt,
            name = familie.titel,
            kategorie = familie.kategorie,
            beschreibung = "Einheitlicher ${familie.titel} mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.",
            standardGröße = GraphGröße(300f, 145f),
            anschlüsse = operator.eingänge.mapIndexed { index, eingang ->
                neuerEingang(eingang.name, eingang.typ, index)
            } + neuerAusgang(operator.ergebnisTyp),
            standardParameter = mapOf(RECHNER_OPERATOR_PARAMETER to operator.id),
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
    val anschlüsse = operator.eingänge.mapIndexed { index, eingang ->
        erhalteOderErzeugeEingang(knoten, eingang.name, eingang.typ, index)
    } + erhalteOderErzeugeAusgang(knoten, operator.ergebnisTyp)
    return knoten.copy(
        anschlüsse = anschlüsse,
        parameter = knoten.parameter
            .minus(STRUKTUR_RECHNER_FORMEL_AUSDRUCK)
            .minus(STRUKTUR_RECHNER_FORMEL_LATEX)
            .minus(STRUKTUR_RECHNER_FORMEL_VARIABLEN) +
            (RECHNER_OPERATOR_PARAMETER to operator.id),
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
    val anschlüsse = variablen.mapIndexed { index, variable ->
        erhalteOderErzeugeEingang(knoten, variable.name, variable.typ, index)
    } + erhalteOderErzeugeAusgang(knoten, wurzel.typ)
    return knoten.copy(
        anschlüsse = anschlüsse,
        parameter = knoten.parameter + mapOf(
            RECHNER_OPERATOR_PARAMETER to familie.formelOperatorId,
            STRUKTUR_RECHNER_FORMEL_AUSDRUCK to StrukturFormelCodec.kodieren(wurzel),
            STRUKTUR_RECHNER_FORMEL_LATEX to StrukturFormelDarstellung.latex(wurzel, quantorVariable),
            STRUKTUR_RECHNER_FORMEL_VARIABLEN to variablen.joinToString(",") { "${it.name}:${it.typ.name}" },
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
    val vorhanden = knoten.anschlüsse.firstOrNull {
        it.name == name && it.richtung == AnschlussRichtung.Eingang && it.art == art && it.zulässigeArten == zulässige
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
internal fun MathematikAuswerterRegister.registriereStrukturFormelRechner() {
    val bisherige = StrukturRechnerKnotenFamilie.entries.associateWith { familie -> finde(familie.knotenArt) }
    StrukturRechnerKnotenFamilie.entries.forEach { familie ->
        registriere(familie.knotenArt) { kontext ->
            val operatorId = kontext.knoten.parameter[RECHNER_OPERATOR_PARAMETER]
            val delegiere = operatorId != familie.formelOperatorId && when (familie) {
                StrukturRechnerKnotenFamilie.AUSSAGESATZ,
                StrukturRechnerKnotenFamilie.TENSOR,
                -> true
                StrukturRechnerKnotenFamilie.VEKTOR -> operatorId == VektorRechnerOperator.SKALARPRODUKT.stabileId
                StrukturRechnerKnotenFamilie.MATRIX -> false
            }
            if (delegiere) {
                val basis = bisherige[familie]
                if (basis != null) {
                    val ergebnis = basis.auswerten(kontext)
                    if ("wert" in ergebnis.ausgaben || ergebnis.ausgaben.size != 1) ergebnis
                    else ergebnis.copy(ausgaben = mapOf("wert" to ergebnis.ausgaben.values.single()))
                } else {
                    kontext.werteStrukturRechnerSicher(familie, operatorId)
                }
            } else {
                kontext.werteStrukturRechnerSicher(familie, operatorId)
            }
        }
    }
}

private fun KnotenAuswertungsKontext.werteStrukturRechnerSicher(
    familie: StrukturRechnerKnotenFamilie,
    operatorId: String?,
): KnotenAuswertungsErgebnis = runCatching {
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

private fun werteStrukturFormelAus(kontext: KnotenAuswertungsKontext): MathematischesObjekt {
    val wurzel = ladeStrukturRechnerFormel(kontext.knoten) ?: error("Die gespeicherte Strukturformel fehlt oder ist beschädigt.")
    require(FormelAusdruckPruefer.pruefe(wurzel) == FormelPruefung.Gueltig) { "Die gespeicherte Strukturformel ist unvollständig." }
    val variablen = strukturFormelVariablen(wurzel).associate { variable ->
        variable.name to (kontext.eingänge[variable.name]?.objekt
            ?: error("Formeleingang '${variable.name}' fehlt."))
    }
    return werteFormelAusdruckAus(wurzel, variablen, kontext.knoten)
}

private fun werteFormelAusdruckAus(
    ausdruck: FormelAusdruck,
    variablen: Map<String, MathematischesObjekt>,
    knoten: KnotenDaten,
): MathematischesObjekt = when (ausdruck) {
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
    argumente: Map<String, MathematischesObjekt>,
    knoten: KnotenDaten,
): MathematischesObjekt = when {
    operatorId.startsWith("zahl.") -> werteZahlFormelAus(operatorId, argumente.values.toList())
    operatorId.startsWith("aussage.") -> werteAussagenFormelAus(operatorId, argumente, knoten)
    operatorId.startsWith("vektor.") -> werteVektorFormelAus(operatorId, argumente)
    operatorId.startsWith("matrix.") -> werteMatrixFormelAus(operatorId, argumente)
    operatorId.startsWith("tensor.") -> werteTensorFormelAus(operatorId, argumente, knoten)
    else -> error("Unbekannter Formeloperator $operatorId.")
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
): MathematischesObjekt {
    val operator = MatrixRechnerOperator.entries.firstOrNull { it.stabileId == operatorId }
        ?: error("Unbekannter Matrixoperator $operatorId.")
    val matrizen = argumente.mapNotNull { (rolle, objekt) ->
        (objekt as? Matrix)?.let { MatrixOperand(rolle, it, FundamentalerZahlbereich.REELL) }
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
): MathematischesObjekt {
    val argumente = definition.eingänge.associate { eingang ->
        eingang.name to (kontext.eingänge[eingang.name]?.objekt
            ?: error("Eingang '${eingang.name}' fehlt."))
    }
    return when (familie) {
        StrukturRechnerKnotenFamilie.AUSSAGESATZ -> werteAussagenFormelAus(definition.id, argumente, kontext.knoten)
        StrukturRechnerKnotenFamilie.VEKTOR -> werteVektorFormelAus(definition.id, argumente)
        StrukturRechnerKnotenFamilie.MATRIX -> werteMatrixFormelAus(definition.id, argumente)
        StrukturRechnerKnotenFamilie.TENSOR -> werteTensorFormelAus(definition.id, argumente, kontext.knoten)
    }
}

private fun matrixErgebnisWert(ergebnis: MatrixRechnerErgebnis): MathematischesObjekt = when (ergebnis) {
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
