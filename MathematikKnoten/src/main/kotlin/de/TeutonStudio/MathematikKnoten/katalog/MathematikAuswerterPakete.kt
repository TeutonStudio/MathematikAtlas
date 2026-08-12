package de.TeutonStudio.MathematikKnoten.katalog

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikKartenAdapter.registriereMethodenArgumente
import de.TeutonStudio.MathematikKnoten.*

/**
 * Benannte Registrierungseinheit für einen fachlich zusammengehörenden Teil des
 * Mathematik-Auswerterregisters.
 *
 * Die Reihenfolge ist Teil des Vertrags: spätere Verfeinerungspakete dürfen
 * bewusst zuvor registrierte Auswerter umschließen oder ersetzen. Dieser
 * Mechanismus macht diese bisher nur implizite Reihenfolge sichtbar und testbar.
 */
data class MathematikAuswerterPaket(
    val name: String,
    val registrieren: MathematikAuswerterRegister.() -> Unit,
) {
    fun installiereIn(register: MathematikAuswerterRegister) {
        registrieren.invoke(register)
    }
}

object StandardMathematikAuswerterPakete {
    /** Additive fachliche Registrierungen ohne beabsichtigte finale Wrapperrolle. */
    val basis: List<MathematikAuswerterPaket> = listOf(
        MathematikAuswerterPaket("zahlenrechner") { registriereUniversellenZahlenRechner() },
        MathematikAuswerterPaket("zahlenrechner-erweiterungen") { registriereZahlenRechnerErweiterungen() },
        MathematikAuswerterPaket("matrixdiagonale") { registriereMatrixdiagonale() },
        MathematikAuswerterPaket("spur-und-tupelsumme") { registriereSpurUndTupelsumme() },
        MathematikAuswerterPaket("tupeloperationen") { registriereTupelOperationKnoten() },
        MathematikAuswerterPaket("tupelvariable") { registriereTupelVariable() },
        MathematikAuswerterPaket("transponieren") { registriereTransponieren() },
        MathematikAuswerterPaket("geometrie-grundobjekte") { registriereGeometrieGrundobjekte() },
        MathematikAuswerterPaket("geometrie-teilobjekte") { registriereGeometrieTeilobjekte() },
        MathematikAuswerterPaket("geometrie-relationen") { registriereGeometrieRelationen() },
        MathematikAuswerterPaket("geometrie-transformationen") { registriereGeometrieTransformationen() },
        MathematikAuswerterPaket("mengenraeume") { registriereMengenraumKnoten() },
        MathematikAuswerterPaket("mengen-operatoren") { registriereMengenOperatorKnoten() },
        MathematikAuswerterPaket("praedikat") { registrierePraedikatKnoten() },
        MathematikAuswerterPaket("aussagenlogik") { registriereAussagenLogikKnoten() },
        MathematikAuswerterPaket("strukturrechner") { registriereStrukturRechnerKnoten() },
        MathematikAuswerterPaket("lineare-strukturen") { registriereLineareStrukturErweiterungen() },
        MathematikAuswerterPaket("lineare-algebra") { registriereLineareAlgebraGrundlagen() },
        MathematikAuswerterPaket("hyperanalysis") { registriereHyperAnalysisKnoten() },
        MathematikAuswerterPaket("differential") { registriereDifferentialKnoten() },
        MathematikAuswerterPaket("integral") { registriereIntegralKnoten() },
        MathematikAuswerterPaket("restriktion") { registriereRestriktionsKnoten() },
        MathematikAuswerterPaket("methodengraph") { registriereMethodenGraphKnoten() },
        MathematikAuswerterPaket("tangentialobjekt") { registriereTangentialKnoten() },
        MathematikAuswerterPaket("rand") { registriereRandKnoten() },
        MathematikAuswerterPaket("svg") { registriereSvgKnoten() },
        MathematikAuswerterPaket("methodenargumente") { registriereMethodenArgumente() },
        MathematikAuswerterPaket("mathematische-eigenschaften") { registriereMathematischeEigenschaften() },
        MathematikAuswerterPaket("exakte-eigenschaften") { registriereExakteEigenschaftsAuswertung() },
        MathematikAuswerterPaket("folgen-und-signaturen") { registriereFolgenUndSignaturEigenschaften() },
        MathematikAuswerterPaket("mengeneigenschaften") { registriereMengenEigenschaftsAuswertung() },
    )

    /**
     * Bewusst nachgelagerte Adapter. Ihre Reihenfolge darf nicht alphabetisch
     * sortiert werden, weil mehrere Pakete bestehende Auswerter verfeinern.
     */
    val verfeinerungen: List<MathematikAuswerterPaket> = listOf(
        MathematikAuswerterPaket("konsolidierte-knoten") { registriereKonsolidierteKnoten() },
        MathematikAuswerterPaket("vektorrechner-erweiterungen") { registriereVektorRechnerErweiterungen() },
        MathematikAuswerterPaket("skalarprodukt-verfeinerung") { registriereSkalarproduktErweiterungen() },
        MathematikAuswerterPaket("strukturformel-rechner") { registriereStrukturFormelRechner() },
        MathematikAuswerterPaket("strukturrechner-kompatibilitaet") { registriereStrukturRechnerKompatibilitaet() },
        MathematikAuswerterPaket("analysis-nullstellen") { registriereErweiterteAnalysisNullstellen() },
        MathematikAuswerterPaket("analysis-mengen-normalisierung") { registriereAnalysisMengenNormalisierung() },
        MathematikAuswerterPaket("tensor-operation-registry") { registriereTensorOperationRegistry() },
        MathematikAuswerterPaket("strukturierte-division") { registriereStrukturierteDivision() },
        MathematikAuswerterPaket("zahlenrechner-differential") { registriereZahlenRechnerDifferential() },
        MathematikAuswerterPaket("null-distanz") { registriereNullDistanz() },
        MathematikAuswerterPaket("vektorkonstruktor") { registriereVektorKonstruktor() },
        MathematikAuswerterPaket("vektor-orientierung") { registriereVektorOrientierungsKnoten() },
        MathematikAuswerterPaket("multinomvektor") { registriereMultinomVektor() },
        MathematikAuswerterPaket("polynom-multinom-vertrag") { registrierePolynomMultinomVertrag() },
        MathematikAuswerterPaket("rechner-methodenhebung") { registriereRechnerMethodenHebung() },
    )

    val alle: List<MathematikAuswerterPaket>
        get() = basis + verfeinerungen

    fun installiereIn(register: MathematikAuswerterRegister) {
        alle.forEach { paket -> paket.installiereIn(register) }
    }
}
