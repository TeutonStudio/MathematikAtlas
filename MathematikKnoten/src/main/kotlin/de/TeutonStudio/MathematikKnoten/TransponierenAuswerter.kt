package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Registriert den typabhängigen Universalknoten getrennt von den historischen Varianten. */
internal fun MathematikAuswerterRegister.registriereTransponieren() {
    registriere("mathematik.transponieren") { k ->
        val eingang = k.eingänge["wert"] ?: error("Transponierbarer Wert fehlt.")
        val transponiert = when (val wert = eingang.objekt) {
            is SpaltenVektor -> wert.transponiert()
            is ZeilenVektor -> wert.transponiert()
            is Matrix -> wert.transponiert()
            is Tensor -> wert.permutiereAchsen(
                parseTensorPermutation(k.knoten.parameter["achsenPermutation"], wert.rang),
            )
            else -> error("Nur Vektoren, Matrizen und Tensoren können transponiert werden.")
        }
        KnotenAuswertungsErgebnis(mapOf("wert" to eingang.copy(objekt = transponiert)))
    }
}
