package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahl
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

/**
 * Konsolidiert Betrag und historischen komplexen Radius auf den Begriff Abstand zu 0.
 * Die historische Operator-ID bleibt ausführbar; neue Auswahl verwendet `zahl.betrag` als kanonischen Persistenzzustand.
 */
fun MathematikAuswerterRegister.registriereNullDistanz() {
    val basis = requireNotNull(finde(ZAHLENRECHNER_ART))
    registriere(ZAHLENRECHNER_ART) { kontext ->
        val operatorId = kontext.knoten.parameter[ZAHLENRECHNER_OPERATOR]
        if (
            operatorId == UniversellerZahlenOperator.BETRAG.stabileId &&
            kontext.eingänge["a"]?.objekt is KomplexeZahl
        ) {
            // Der historische Radiuspfad besitzt bereits die korrekte komplexe Betragssemantik.
            // Wir delegieren dorthin, ohne den gespeicherten Knoten umzuschreiben.
            val alias = kontext.copy(
                knoten = kontext.knoten.copy(
                    parameter = kontext.knoten.parameter +
                        (ZAHLENRECHNER_OPERATOR to UniversellerZahlenOperator.KOMPLEXER_RADIUS.stabileId),
                ),
            )
            basis.auswerten(alias)
        } else {
            basis.auswerten(kontext)
        }
    }
}
