package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Registriert die mit v2.3.2 eingeführte Kehrwert- und Divisionssemantik. */
internal fun MathematikAuswerterRegister.registriereDivisionUndKehrwert() {
    registriere("mathematik.kehrwert") { k ->
        val eingang = k.eingänge["zahl"] ?: error("Für den Kehrwert muss eine Zahl verbunden sein.")
        val zahl = eingang.objekt as? ZahlAusdruck ?: error("Der Kehrwert ist nur für Zahlen definiert.")
        val nichtNull = Ungleichheit(zahl, RationaleZahl.Null)
        val kontext = k.rechenKontext.copy(annahmen = k.rechenKontext.annahmen + eingang.annahmen + nichtNull)
        val kehrwert = vereinfache(Potenz(zahl, RationaleZahl.von(-1)), kontext)
        KnotenAuswertungsErgebnis(mapOf(
            "wert" to eingang.copy(
                objekt = kehrwert,
                annahmen = eingang.annahmen + nichtNull,
                latexDarstellung = "\\left(${eingang.anzeigeLatex()}\\right)^{-1}",
            ),
        ))
    }

    registriere("mathematik.division") { k ->
        val dividendWert = k.eingänge["dividend"] ?: error("Dividend fehlt.")
        val divisorWert = k.eingänge["divisor"] ?: error("Divisor fehlt.")
        val dividend = dividendWert.objekt as? ZahlAusdruck ?: error("Der Dividend muss eine Zahl sein.")
        val divisor = divisorWert.objekt as? ZahlAusdruck ?: error("Der Divisor muss eine Zahl sein.")
        val nullErsatz = k.eingänge["fallsNennerNull"]
        require(nullErsatz == null || nullErsatz.objekt is ZahlAusdruck) { "Der Ersatzwert für Nenner 0 muss eine Zahl sein." }

        val eingangsWerte = listOfNotNull(dividendWert, divisorWert, nullErsatz)
        val annahmen = eingangsWerte.flatMap { it.annahmen }.toSet()
        val reelle = reelleVariablen(eingangsWerte)
        val quellen = eingangsWerte.flatMap { it.variablenQuellen }
            .distinctBy { quelle -> Pair(Triple(quelle.knotenId, quelle.name, quelle.werteVorrat), quelle.alsMethodenParameter) }
        val divisorNull = Gleichheit(divisor, RationaleZahl.Null)
        val divisorNichtNull = Ungleichheit(divisor, RationaleZahl.Null)
        val kontext = k.rechenKontext.copy(annahmen = k.rechenKontext.annahmen + annahmen)
        val quotient = vereinfache(
            Division(dividend, divisor),
            kontext.copy(annahmen = kontext.annahmen + divisorNichtNull),
        )
        val quotientWert = BedingterWert(
            objekt = quotient,
            annahmen = annahmen + divisorNichtNull,
            reelleVariablen = reelle,
            variablenQuellen = quellen,
        )

        val basis = when (divisorNull.entscheide(kontext).wahrheitswert) {
            Wahrheitswert.Wahr -> nullErsatz ?: error("Für Nenner 0 muss der Eingang „falls Nenner null“ verbunden sein.")
            Wahrheitswert.Lüge -> quotientWert
            null -> if (nullErsatz == null) quotientWert else BedingterWert(
                objekt = FallAusdruck(nullErsatz.objekt, divisorNull, quotient),
                annahmen = annahmen,
                reelleVariablen = reelle,
                variablenQuellen = quellen,
            )
        }

        val latex = if (nullErsatz == null) {
            "\\frac{${dividendWert.anzeigeLatex()}}{${divisorWert.anzeigeLatex()}}"
        } else {
            "\\begin{cases}${nullErsatz.anzeigeLatex()},&${divisorWert.anzeigeLatex()}=0\\\\\\frac{${dividendWert.anzeigeLatex()}}{${divisorWert.anzeigeLatex()}},&${divisorWert.anzeigeLatex()}\\ne0\\end{cases}"
        }
        KnotenAuswertungsErgebnis(mapOf(
            "wert" to basis.copy(
                annahmen = if (basis === quotientWert) quotientWert.annahmen else annahmen,
                reelleVariablen = reelle,
                variablenQuellen = quellen,
                latexDarstellung = latex,
            ),
        ))
    }
}
