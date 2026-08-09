package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

/**
 * Koordinatenfreie Differentialdarstellung einer Methode.
 *
 * Die Ableitungsfunktion bleibt ein eigenes Methodenobjekt. Dieses Objekt hält
 * dagegen das Differential selbst und verweist auf dieselbe strukturierte
 * Differentialrechnung. Damit werden f', df, ∂ᵢf und dᵢf nicht typseitig
 * gleichgesetzt.
 */
data class MethodenDifferential(
    val methode: Methode,
    val ordnung: DifferentialOrdnung = DifferentialOrdnung.Konkret(1),
    val operator: DifferentialOperator = DifferentialOperator.Total,
    val begriff: DifferentialBegriff = DifferentialBegriff.REELL_FRECHET,
) : Ausdruck {
    init {
        operator.pruefeFuer(methode)
    }

    fun ableitungsErgebnis(): DifferentialMethodenErgebnis = differenziereMethodeStrukturiert(
        methode = methode,
        ordnung = ordnung,
        operator = operator,
        begriff = begriff,
    )

    val ableitungsFunktion: Methode
        get() = ableitungsErgebnis().methode

    override fun zuLatex(): String = when (operator) {
        DifferentialOperator.Total -> when (ordnung) {
            is DifferentialOrdnung.Konkret -> when (ordnung.wert) {
                BigInteger.ZERO -> methode.name
                BigInteger.ONE -> "d${methode.name}"
                else -> "d^{${ordnung.wert}}${methode.name}"
            }
            is DifferentialOrdnung.Symbolisch -> "d^{(${ordnung.zuLatex()})}${methode.name}"
        }
        is DifferentialOperator.Partiell -> when (ordnung) {
            is DifferentialOrdnung.Konkret -> if (ordnung.wert == BigInteger.ONE) {
                "d_{${operator.argumentIndex}}${methode.name}"
            } else {
                "d_{${operator.argumentIndex}}^{(${ordnung.wert})}${methode.name}"
            }
            is DifferentialOrdnung.Symbolisch ->
                "d_{${operator.argumentIndex}}^{(${ordnung.zuLatex()})}${methode.name}"
        }
    }

    /**
     * Sichtbare Definitionsbeziehung. Für die erste totale Ableitung gilt
     * df_x(h)=f'(x)(h); das partielle Differential wird über die Einbettung der
     * i-ten Richtung als dᵢf=df∘ιᵢ modelliert.
     */
    fun definitionsLatex(): String = when {
        operator == DifferentialOperator.Total && istErsteOrdnung() ->
            "d${methode.name}_{x}(h)=${AbleitungsMethodenAusdruck(methode).zuLatex()}(x)(h)"
        operator is DifferentialOperator.Partiell && istErsteOrdnung() ->
            "d_{${operator.argumentIndex}}${methode.name}=d${methode.name}\\circ\\iota_{${operator.argumentIndex}}"
        else ->
            "${zuLatex()}\\leftrightarrow${AbleitungsMethodenAusdruck(methode, operator, ordnung).zuLatex()}"
    }

    private fun istErsteOrdnung(): Boolean =
        ordnung is DifferentialOrdnung.Konkret && ordnung.wert == BigInteger.ONE
}
