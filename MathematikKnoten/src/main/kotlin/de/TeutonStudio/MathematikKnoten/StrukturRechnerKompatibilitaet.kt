package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.knoten.MathematikAuswerterRegister

/**
 * Hält den historischen Vektorrechner-Ausgang `skalar` kompatibel, während
 * Strukturformeln den einheitlichen Ausgang `wert` verwenden.
 */
internal fun MathematikAuswerterRegister.registriereStrukturRechnerKompatibilitaet() {
    val basis = requireNotNull(finde(VektorRechner.KNOTEN_ART)) {
        "Der Vektorrechner muss vor seiner Kompatibilitätsschicht registriert sein."
    }

    registriere(VektorRechner.KNOTEN_ART) { kontext ->
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
}
