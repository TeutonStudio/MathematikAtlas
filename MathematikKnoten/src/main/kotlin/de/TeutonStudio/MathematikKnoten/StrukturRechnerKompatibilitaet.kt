package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner

/**
 * Hält den historischen Vektorrechner-Ausgang `skalar` kompatibel, während
 * Strukturformeln den einheitlichen Ausgang `wert` verwenden.
 */
internal fun mitVektorAusgangKompatibilität(basis: de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter): de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter =
    de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter { kontext ->
        val ergebnis = basis.auswerten(kontext)
        val wert = ergebnis.ausgaben["wert"]
            ?: ergebnis.ausgaben[VEKTOR_RECHNER_AUSGANG]
            ?: ergebnis.ausgaben.values.singleOrNull()

        if (wert == null) {
            ergebnis
        } else {
            ergebnis.copy(
                ausgaben = ergebnis.ausgaben + mapOf(
                    "wert" to wert,
                    VEKTOR_RECHNER_AUSGANG to wert,
                ),
            )
        }
}

internal fun MathematikAuswerterRegister.registriereStrukturRechnerKompatibilitaet() { registriere(VektorRechner.KNOTEN_ART, mitVektorAusgangKompatibilität(requireNotNull(finde(VektorRechner.KNOTEN_ART)))) }
