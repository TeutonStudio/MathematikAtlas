package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikKartenAdapter.registriereMethodenArgumente

/** Bestehender Standardauswerter plus additive Mathematikdomänen. */
object GesamterMathematikAuswerter {
    fun erzeugeRegister(): MathematikAuswerterRegister = StandardMathematikAuswerter.erzeugeRegister().apply {
        registriereUniversellenZahlenRechner()
        registriereZahlenRechnerErweiterungen()
        registriereMatrixdiagonale()
        registriereSpurUndTupelsumme()
        registriereTupelOperationKnoten()
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
        registriereHyperAnalysisKnoten()
        registriereDifferentialKnoten()
        registriereIntegralKnoten()
        registriereRestriktionsKnoten()
        registriereMethodenGraphKnoten()
        registriereMethodenArgumente()
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
        // Normalisiert anschließend triviale definierte Stellenmengen auf Grundmenge oder leere Menge.
        registriereAnalysisMengenNormalisierung()
        // Finaler Tensorrechner-Wrapper: Registryrollen, sichtbare Achsen und symbolische Ausgänge.
        registriereTensorOperationRegistry()
        // Finaler Zahlenrechner-Wrapper: übernimmt ausschließlich den Divisionszustand.
        registriereStrukturierteDivision()
        // Darauf aufbauend übernimmt der Analysisadapter ausschließlich strukturierte Differentialzustände.
        registriereZahlenRechnerDifferential()
        // Vereinheitlicht Betrag und historischen Radius.
        registriereNullDistanz()
        // v2.30.0: kanonischer Vektorkonstruktor mit Orientierung und Indexmethode.
        registriereVektorKonstruktorV2300()
        // v2.30.0: historische Zeile-/Spalte-Paare werden auf parametrierte Verträge abgebildet.
        registriereVektorOrientierungsKnotenV2300()
        // Neuer erzeugbarer Typ mit gemeinsamer Monomfolge.
        registriereMultinomVektor()
        // Historischer Komfortknoten und Zahlenrechner teilen dieselbe Multinomfolge.
        registrierePolynomMultinomVertragV2300()
    }
}
