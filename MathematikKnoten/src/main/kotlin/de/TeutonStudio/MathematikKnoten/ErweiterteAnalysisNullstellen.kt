package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.Multiplikation
import de.TeutonStudio.MathematikRechenSystem.kern.Potenz
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.ableiten
import de.TeutonStudio.MathematikRechenSystem.kern.löseLinear
import de.TeutonStudio.MathematikRechenSystem.kern.vereinfache
import java.math.BigInteger

/**
 * Ergänzt den Analysis-Eigenschaftsauswerter um exakte Nullstellen einfacher
 * Potenzen und Produkte. Dadurch werden insbesondere Wendestellen und
 * stationäre Wendestellen von Monomen wie x³ exakt erkannt, ohne einen
 * allgemeinen nichtlinearen Gleichungslöser vorzutäuschen.
 */
internal fun MathematikAuswerterRegister.registriereErweiterteAnalysisNullstellen() {
    val basisAuswerter = finde(ANALYSIS_EIGENSCHAFT_KNOTEN_ART) ?: return
    registriere(ANALYSIS_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        kontext.exaktePotenzStellen() ?: basisAuswerter.auswerten(kontext)
    }
}

private fun KnotenAuswertungsKontext.exaktePotenzStellen(): KnotenAuswertungsErgebnis? {
    val eigenschaft = knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty()
    if (eigenschaft !in setOf(
            MathematischeEigenschaftRegister.Wendestelle.id,
            MathematischeEigenschaftRegister.Sattelpunkt.id,
        )
    ) return null

    val methode = eingänge["methode"]?.objekt as? Methode ?: return null
    val variable = methode.parameter.singleOrNull() as? Variable ?: return null
    val vorschrift = methode.vorschrift as? ZahlAusdruck ?: return null
    val erste = runCatching { vereinfache(ableiten(vorschrift, variable).ergebnis) }.getOrNull() ?: return null
    val zweite = runCatching { vereinfache(ableiten(erste, variable).ergebnis) }.getOrNull() ?: return null
    val dritte = runCatching { vereinfache(ableiten(zweite, variable).ergebnis) }.getOrNull() ?: return null
    val dritteNichtNull = (dritte as? RationaleZahl)?.let { !it.istNull() } == true
    if (!dritteNichtNull) return null

    val stellen = when (eigenschaft) {
        MathematischeEigenschaftRegister.Wendestelle.id ->
            einfacheNullstellen(zweite, variable)
        MathematischeEigenschaftRegister.Sattelpunkt.id -> {
            val stationär = einfacheNullstellen(erste, variable) ?: return null
            val krümmungsNullen = einfacheNullstellen(zweite, variable) ?: return null
            stationär intersect krümmungsNullen
        }
        else -> return null
    } ?: return null

    val menge = EndlicheMenge(stellen)
    val wert = BedingterWert(
        objekt = menge,
        annahmen = eingänge.values.flatMap { it.annahmen }.toSet(),
    )
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "stellen" to wert,
            "stellenmenge" to wert,
        ),
        eingänge = eingänge,
    )
}

/**
 * Liefert nur dann eine Menge, wenn sämtliche Nullstellen durch elementare
 * Potenz-, Produkt- oder lineare Regeln vollständig bestimmt werden können.
 */
private fun einfacheNullstellen(
    ausdruck: ZahlAusdruck,
    variable: Variable,
): Set<MathematischesObjekt>? = when (val term = vereinfache(ausdruck)) {
    is RationaleZahl -> emptySet()
    is Variable -> if (term == variable) setOf(RationaleZahl.Null) else null
    is Potenz -> {
        val exponent = term.exponent as? RationaleZahl
        val positivGanzzahlig = exponent != null &&
            exponent.nenner == BigInteger.ONE &&
            exponent.zähler > BigInteger.ZERO
        if (positivGanzzahlig) einfacheNullstellen(term.basis, variable) else null
    }
    is Multiplikation -> buildSet {
        term.faktoren.forEach { faktor ->
            if (faktor is RationaleZahl) return@forEach
            addAll(einfacheNullstellen(faktor, variable) ?: return null)
        }
    }
    else -> runCatching {
        löseLinear(Gleichheit(term, RationaleZahl.Null), variable).lösungen.toSet()
    }.getOrNull()
}
