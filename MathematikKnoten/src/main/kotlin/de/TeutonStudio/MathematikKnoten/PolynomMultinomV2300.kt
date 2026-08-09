package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Hält den historischen Komfortknoten auf demselben Multinomvertrag wie den Zahlenrechner. */
fun MathematikAuswerterRegister.registrierePolynomMultinomVertragV2300() {
    registriere("mathematik.vektorZuPolynom") { k ->
        val vektor = k.eingänge["vektor"]?.objekt as? OrientierterVektor
            ?: error("Vektoreingang fehlt.")
        val variablenName = (k.knoten.parameter["variable"] ?: "x").trim()
        require(variablenName.isNotEmpty()) { "Die Polynomvariable darf nicht leer sein." }
        // Explizit als ZahlAusdruck typisieren, damit dieselbe allgemeine Überladung wie
        // beim Zahlenrechner verwendet wird und damit dieselbe multinomFolge zugrunde liegt.
        val argument: ZahlAusdruck = Variable(variablenName)
        val wert = polynomAusKoeffizienten(vektor.werte, argument)
        KnotenAuswertungsErgebnis(
            mapOf(
                "wert" to BedingterWert(
                    wert,
                    annahmen = k.eingänge.values.flatMap { it.annahmen }.toSet(),
                    latexDarstellung = "(c_i)_i\\cdot\\vec{x}",
                ),
            ),
            eingänge = k.eingänge,
        )
    }
}
