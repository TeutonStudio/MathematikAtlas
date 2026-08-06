package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

object ZahlenPotenzAdapter {
    /**
     * Liefert nur für natürliche konkrete Exponenten ein Ergebnis. Negative,
     * rationale und allgemeine symbolische Exponenten bleiben beim bestehenden
     * Potenzvertrag des Zahlenrechners.
     */
    fun versuche(
        basis: ZahlenRechnerEingabe,
        exponent: ZahlenRechnerEingabe,
    ): ZahlenRechnerErgebnis? {
        val rational = exponent.ausdruck as? RationaleZahl ?: return null
        if (rational.nenner != BigInteger.ONE || rational.zähler.signum() < 0) return null
        val struktur = StandardPotenzStrukturen.zahlbereich(basis.bereich)
        return when (
            val ergebnis = PotenzDienst.werteAus(
                basis = basis.ausdruck,
                ordnung = IterationsOrdnung.Konkret(rational.zähler),
                expliziteStruktur = struktur,
            )
        ) {
            is PotenzDienstErgebnis.ObjektWert -> {
                val wert = ergebnis.wert as? ZahlAusdruck ?: return ZahlenRechnerErgebnis.Ungueltig(
                    "potenz_ergebnistyp",
                    "Die natürliche Zahlenpotenz lieferte keinen Zahlenausdruck.",
                    listOf("basis", "exponent"),
                )
                ZahlenRechnerErgebnis.Wert(
                    ausdruck = wert,
                    bereich = basis.bereich,
                    definitionsId = "potenz|${basis.bereich.id}",
                    bedingungen = ergebnis.voraussetzungen.toList(),
                )
            }
            is PotenzDienstErgebnis.Symbolisch -> ZahlenRechnerErgebnis.Bedingt(
                ausdruck = Potenz(basis.ausdruck, exponent.ausdruck),
                moeglicheBereiche = setOf(basis.bereich),
                definitionsIds = setOf("potenz|${basis.bereich.id}"),
                bedingungen = ergebnis.voraussetzungen.toList(),
            )
            is PotenzDienstErgebnis.Ungueltig -> ZahlenRechnerErgebnis.Ungueltig(
                ergebnis.code,
                ergebnis.grund,
                listOf("basis", "exponent"),
            )
            is PotenzDienstErgebnis.MethodenWert -> ZahlenRechnerErgebnis.Ungueltig(
                "potenz_ergebnistyp",
                "Der Zahlenrechner akzeptiert keine Methodenpotenz.",
                listOf("basis"),
            )
        }
    }
}

object MatrixPotenzAdapter {
    fun werteNatuerlichAus(
        operand: MatrixOperand,
        exponent: BigInteger,
    ): MatrixRechnerErgebnis {
        require(exponent.signum() >= 0)
        return when (
            val ergebnis = PotenzDienst.werteAus(
                basis = operand.matrix,
                ordnung = IterationsOrdnung.Konkret(exponent),
                expliziteStruktur = StandardPotenzStrukturen.matrix(operand.matrix),
            )
        ) {
            is PotenzDienstErgebnis.ObjektWert -> {
                val matrix = ergebnis.wert as? Matrix ?: return MatrixRechnerErgebnis.Ungueltig(
                    "potenz_ergebnistyp",
                    "Die natürliche Matrixpotenz lieferte keine Matrix.",
                )
                MatrixRechnerErgebnis.MatrixWert(
                    wert = matrix,
                    zahlbereich = operand.zahlbereich,
                    bedingungen = ergebnis.voraussetzungen.toList(),
                )
            }
            is PotenzDienstErgebnis.Symbolisch -> MatrixRechnerErgebnis.Bedingt(
                latex = ergebnis.wert.zuLatex(),
                ergebnisTyp = FormelTyp.MATRIX,
                bedingungen = ergebnis.voraussetzungen.map(Aussage::zuLatex),
            )
            is PotenzDienstErgebnis.Ungueltig -> MatrixRechnerErgebnis.Ungueltig(
                ergebnis.code,
                ergebnis.grund,
            )
            is PotenzDienstErgebnis.MethodenWert -> MatrixRechnerErgebnis.Ungueltig(
                "potenz_ergebnistyp",
                "Der Matrixrechner akzeptiert keine Methodenpotenz.",
            )
        }
    }
}
