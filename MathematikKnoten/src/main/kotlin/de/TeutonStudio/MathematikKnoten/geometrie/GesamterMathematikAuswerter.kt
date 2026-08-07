package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister

/** Bestehender Standardauswerter plus additive Mathematikdomänen. */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereUniversellenZahlenRechner()
        registriereZahlenRechnerErweiterungen()
        registriereMatrixdiagonale()
        registriereSpurUndTupelsumme()
        registriereTransponieren()
        registriereGeometrieGrundobjekte()
        registriereGeometrieTeilobjekte()
        registriereGeometrieRelationen()
        registriereGeometrieTransformationen()
        registriereMengenraumKnoten()
        registriereAussagenLogikKnoten()
        registriereStrukturRechnerKnoten()
        registriereLineareStrukturErweiterungen()
        registriereLineareAlgebraGrundlagen()
        registriereMathematischeEigenschaften()
        registriereExakteEigenschaftsAuswertung()
        registriereFolgenUndSignaturEigenschaften()
        registriereMengenEigenschaftsAuswertung()
        // Bewusst zuletzt: ersetzt historische Spezialauswerter derselben Knotenarten.
        registriereKonsolidierteKnoten()
        // Verfeinert ausschließlich das Skalarprodukt und delegiert alle übrigen Vektoroperationen.
        registriereSkalarproduktErweiterungen()
        // Muss nach den allgemeinen Rechnern registriert werden: typisierte CAS-Formeln und dynamische Verträge.
        registriereStrukturFormelRechner()
        // Bewahrt gespeicherte Karten mit dem historischen Ausgang `skalar`.
        registriereStrukturRechnerKompatibilitaet()
        // Umschließt den finalen Analysis-Auswerter und ergänzt exakte Monom-Nullstellen.
        registriereErweiterteAnalysisNullstellen()
        // Finaler Tensorrechner-Wrapper: Registryrollen, sichtbare Achsen und symbolische Ausgänge.
        registriereTensorOperationRegistry()
        // Finaler Zahlenrechner-Wrapper: übernimmt ausschließlich den Divisionszustand.
        registriereStrukturierteDivision()
    }
}
