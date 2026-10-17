package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal val matrixDiagonalLeser = setOf(MatrixRechnerOperator.HAUPTDIAGONALE.stabileId,MatrixRechnerOperator.NEBENDIAGONALE.stabileId)
internal fun istMatrixDiagonalLeser(k:KnotenDaten)=k.art==MatrixRechner.KNOTEN_ART && k.parameter[RECHNER_OPERATOR_PARAMETER] in matrixDiagonalLeser
internal fun matrixRechnerDiagonalAusgabe(k:KnotenDaten):VektorStrukturAusgabe =
    k.parameter[MATRIXDIAGONALE_AUSGABE]?.let { gespeichert -> VektorStrukturAusgabe.entries.singleOrNull {it.stabileId==gespeichert}
        ?: error("Unbekannte gespeicherte Diagonalprojektion '$gespeichert'.") } ?: VektorStrukturAusgabe.TUPEL
internal fun matrixRechnerDiagonalArt(k:KnotenDaten):MatrixDiagonalArt =
    k.parameter[MATRIXDIAGONALE_ART_PARAMETER]?.let { gespeichert -> MatrixDiagonalArt.ausParameterOderNull(gespeichert)
        ?: error("Unbekannte gespeicherte Diagonalart '$gespeichert'.") } ?: MatrixDiagonalArt.HAUPTDIAGONALE

internal fun matrixDiagonalAnschlüsse(k:KnotenDaten,operator:String,ports:List<AnschlussDaten>):List<AnschlussDaten> = ports.map { p ->
    when {
        p.richtung==AnschlussRichtung.Eingang && operator in matrixDiagonalLeser+MatrixRechnerOperator.SPUR.stabileId ->
            p.copy(art=MathematikAnschlussArten.Objekt.id,zulässigeArten=setOf(MathematikAnschlussArten.Matrix.id,MathematikAnschlussArten.Tensor.id))
        p.richtung==AnschlussRichtung.Ausgang && operator in matrixDiagonalLeser -> p.copy(art=when(matrixRechnerDiagonalAusgabe(k)) {
            VektorStrukturAusgabe.TUPEL -> MathematikAnschlussArten.Tupel.id
            VektorStrukturAusgabe.ZEILE -> MathematikAnschlussArten.ZeilenVektor.id
            VektorStrukturAusgabe.SPALTE -> MathematikAnschlussArten.SpaltenVektor.id
        })
        else -> p
    }
}

fun konfiguriereMatrixRechnerDiagonalAusgabe(k:KnotenDaten,ausgabe:String):KnotenDaten {
    require(istMatrixDiagonalLeser(k))
    require(VektorStrukturAusgabe.entries.any {it.stabileId==ausgabe}) {"Unbekannte Diagonalprojektion."}
    return konfiguriereStrukturRechner(k.copy(parameter=k.parameter+(MATRIXDIAGONALE_AUSGABE to ausgabe)),StrukturRechnerKnotenFamilie.MATRIX,k.parameter.getValue(RECHNER_OPERATOR_PARAMETER))
}

fun konfiguriereMatrixRechnerDiagonalArt(k: KnotenDaten, art: String): KnotenDaten {
    require(k.art==MatrixRechner.KNOTEN_ART && k.parameter[RECHNER_OPERATOR_PARAMETER]==MatrixRechnerOperator.DIAGONALMATRIX_ERZEUGEN.stabileId)
    require(MatrixDiagonalArt.ausParameterOderNull(art)!=null) { "Unbekannte Diagonalart." }
    return konfiguriereStrukturRechner(k.copy(parameter=k.parameter+(MATRIXDIAGONALE_ART_PARAMETER to art)),StrukturRechnerKnotenFamilie.MATRIX,MatrixRechnerOperator.DIAGONALMATRIX_ERZEUGEN.stabileId)
}

internal fun matrixDiagonalVerlustWarnung(matrix: Tensorartig, art: MatrixDiagonalArt): String? {
    require(matrix.tensorStufe==2)
    val (höhe,breite)=matrix.tensorForm
    val verworfen=(0 until höhe).any { i -> (0 until breite).any { j ->
        val aufDiagonale=if(art==MatrixDiagonalArt.HAUPTDIAGONALE) i==j else j==breite-1-i
        !aufDiagonale && vereinfache(matrix.tensorKomponente(listOf(i,j)))!=RationaleZahl.Null
    } }
    return if(verworfen || höhe!=breite) "Auslesen verwirft Nichtdiagonaleinträge oder die rechteckige Form; Rückführung erzeugt eine quadratische Diagonalmatrix." else null
}
