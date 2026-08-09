package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.Betrag
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.vereinfache

const val NULL_DISTANZ_ART = "mathematik.nullDistanz"

/**
 * Gemeinsamer produktiver Knoten für d(0,x). Der sichtbare Begriff ist
 * unabhängig davon, ob der Zahlwert reell, komplex oder quaternionisch ist.
 * Die eigentliche Bereichssemantik bleibt im Rechenkern.
 */
fun MathematikAuswerterRegister.registriereNullDistanz() {
    registriere(NULL_DISTANZ_ART) { kontext ->
        val eingang = kontext.eingänge["zahl"]
            ?: error("0-Distanz benötigt einen Zahlwert.")
        val zahl = eingang.objekt as? ZahlAusdruck
            ?: error("0-Distanz kann nur auf Zahlwerte angewendet werden.")
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to eingang.copy(
                    objekt = vereinfache(Betrag(zahl), kontext.rechenKontext),
                    latexDarstellung = null,
                ),
            ),
            eingänge = kontext.eingänge,
        )
    }
}
