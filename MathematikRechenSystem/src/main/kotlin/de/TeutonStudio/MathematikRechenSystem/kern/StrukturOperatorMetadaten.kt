package de.TeutonStudio.MathematikRechenSystem.kern

/** Fachliche Operatorenquelle ohne Graph-, Compose- oder Instanzabhängigkeiten. */
data class StrukturRechnerEingang(
    val name: String,
    val typ: FormelTyp,
    val variadisch: Boolean = false,
)

data class StrukturRechnerOperatorDefinition(
    val id: String,
    val titel: String,
    val symbolLatex: String,
    val eingänge: List<StrukturRechnerEingang>,
    val ergebnisTyp: FormelTyp,
    val definitionsLatex: String,
) {
    val assoziativ: Boolean get() = istAssoziativerRechnerOperator(id)
}

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
        "aussage.formel",
    ),
    VEKTOR(
        VektorRechner.KNOTEN_ART,
        "Vektorrechner",
        "Lineare Algebra",
        StrukturFormelFamilie.VEKTOR,
        "vektor.formel",
    ),
    MATRIX(
        MatrixRechner.KNOTEN_ART,
        "Matrixrechner",
        "Lineare Algebra: Matrizen",
        StrukturFormelFamilie.MATRIX,
        "matrix.formel",
    ),
    TENSOR(
        TensorRechner.KNOTEN_ART,
        "Tensorrechner",
        "Tensoren",
        StrukturFormelFamilie.TENSOR,
        "tensor.formel",
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
    ) = StrukturRechnerOperatorDefinition(id, titel, symbol,
        eingänge.map { it.copy(variadisch = istAssoziativerRechnerOperator(id)) }, ergebnis, definition)

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
        d("matrix.diagonalmatrixErzeugen", "Diagonalmatrix erzeugen", "\\operatorname{diag}(v)", FormelTyp.MATRIX, "D_{ij}=\\delta_{ij}v_i", e("werte", FormelTyp.OBJEKT)),
        d("matrix.hauptdiagonale", "Hauptdiagonale", "\\operatorname{diag}A", FormelTyp.TUPEL, "(a_{11},a_{22},\\ldots)", e("matrix", FormelTyp.MATRIX)),
        d("matrix.nebendiagonale", "Nebendiagonale", "\\operatorname{antidiag}A", FormelTyp.TUPEL, "(a_{1n},a_{2,n-1},\\ldots)", e("matrix", FormelTyp.MATRIX)),
        d("matrix.charakteristischesPolynom", "Charakteristisches Polynom", "\\chi_A", FormelTyp.METHODE, "\\chi_A(\\lambda)=\\det(A-\\lambda I)", e("matrix", FormelTyp.MATRIX)),
        d("matrix.minimalpolynom", "Minimalpolynom", "m_A", FormelTyp.METHODE, "m_A(A)=0,\\quad m_A\\mid\\chi_A", e("matrix", FormelTyp.MATRIX)),
        d("matrix.zerlegen", "Zerlegen", "A\\mapsto(A_i)", FormelTyp.METHODE, "i\\mapsto\\operatorname{Zeile}_i(A)\\;\\text{oder}\\;j\\mapsto\\operatorname{Spalte}_j(A)", e("matrix", FormelTyp.MATRIX)),
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
