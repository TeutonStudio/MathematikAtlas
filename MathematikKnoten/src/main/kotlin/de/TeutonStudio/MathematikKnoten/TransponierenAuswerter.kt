package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Registriert den typabhängigen Universalknoten getrennt von den historischen Varianten. */
internal fun MathematikAuswerterRegister.registriereTransponieren() {
    registriere("mathematik.transponieren") { k ->
        val eingang = k.eingänge["wert"] ?: error("Transponierbarer Wert fehlt.")
        val wert=eingang.mathematischesObjekt("Transponierbarer Wert")
        val transponiert=transponiereStruktur(wert,if(wert is Tensor) parseTensorPermutation(k.knoten.parameter["achsenPermutation"],wert.rang) else null)
        KnotenAuswertungsErgebnis(mapOf("wert" to eingang.copy(objekt = transponiert,übersetzungen=eingang.übersetzungen.alsHerkunft())))
    }
}
