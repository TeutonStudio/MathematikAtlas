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
        registriereMethodenEinschraenkungKnoten()
        registriereMathematischeEigenschaften()
        registriereExakteEigenschaftsAuswertung()
        registriereFolgenUndSignaturEigenschaften()
        registriereMengenEigenschaftsAuswertung()
        // Bewusst zuletzt: ersetzt historische Spezialauswerter derselben Knotenarten.
        registriereKonsolidierteKnoten()
        // Verfeinert ausschließlich das Skalarprodukt und delegiert alle übrigen Vektoroperationen.
        registriereSkalarproduktErweiterungen()
        // Muss zuletzt registriert werden: typisierte CAS-Formeln und dynamische Rechnerverträge.
        registriereStrukturFormelRechner()
        // Bewahrt gespeicherte Karten mit dem historischen Ausgang `skalar`.
        registriereStrukturRechnerKompatibilitaet()
        // Umschließt den finalen Analysis-Auswerter und ergänzt exakte Monom-Nullstellen.
        registriereErweiterteAnalysisNullstellen()
    }
}
