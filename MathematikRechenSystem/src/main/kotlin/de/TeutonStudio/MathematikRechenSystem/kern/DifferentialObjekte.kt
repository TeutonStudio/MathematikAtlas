package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

/**
 * Verwendet für partielle Ableitungen den Raum des tatsächlich gewählten
 * Arguments als linearen Quellraum. Die totale Ableitung bleibt unverändert auf
 * dem vollständigen Argumentproduktraum definiert.
 */
fun differenziereMethodeMitOperatorZielraum(
    methode: Methode,
    ordnung: DifferentialOrdnung,
    operator: DifferentialOperator = DifferentialOperator.Total,
    begriff: DifferentialBegriff = DifferentialBegriff.REELL_FRECHET,
): DifferentialMethodenErgebnis {
    val basis = differenziereMethodeStrukturiert(
        methode = methode,
        ordnung = ordnung,
        operator = operator,
        begriff = begriff,
    )
    if (operator !is DifferentialOperator.Partiell) return basis
    if (ordnung is DifferentialOrdnung.Konkret && ordnung.wert == BigInteger.ZERO) return basis

    val parameter = methode.parameter[operator.argumentIndex - 1]
    val argumentRaum = methode.werteVorräte[parameter.name]
        ?: FehlendeObermenge("differential.${methode.name}.${parameter.name}")
    val skalarIdentifiziert = argumentRaum == ReelleZahlen &&
        methode.zielMenge == ReelleZahlen &&
        begriff == DifferentialBegriff.REELL_FRECHET
    val zielRaum = if (skalarIdentifiziert) {
        ReelleZahlen
    } else {
        AbleitungsZielraum(
            argumentRaum = argumentRaum,
            ursprungsZiel = methode.zielMenge,
            ordnung = ordnung,
            eindimensionalSkalarIdentifiziert = false,
        )
    }
    return basis.copy(
        methode = basis.methode.copy(zielMenge = zielRaum),
        zielRaum = zielRaum,
    )
}

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

    fun ableitungsErgebnis(): DifferentialMethodenErgebnis = differenziereMethodeMitOperatorZielraum(
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
