package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Registriert die arithmetische Subtraktion als eigene Knotensemantik. */
internal fun MathematikAuswerterRegister.registriereSubtraktion() {
    registriere("mathematik.subtraktion") { k ->
        val minuendWert = k.eingänge["minuend"] ?: error("Minuend fehlt.")
        val subtrahendWert = k.eingänge["subtrahend"] ?: error("Subtrahend fehlt.")
        val minuend = minuendWert.objekt as? ZahlAusdruck ?: error("Der Minuend muss eine Zahl sein.")
        val subtrahend = subtrahendWert.objekt as? ZahlAusdruck ?: error("Der Subtrahend muss eine Zahl sein.")
        val eingänge = listOf(minuendWert, subtrahendWert)
        val annahmen = eingänge.flatMap { it.annahmen }.toSet()
        val kontext = k.rechenKontext.copy(annahmen = k.rechenKontext.annahmen + annahmen)
        val differenz = vereinfache(subtraktion(minuend, subtrahend), kontext)
        KnotenAuswertungsErgebnis(mapOf(
            "wert" to BedingterWert(
                objekt = differenz,
                annahmen = annahmen,
                reelleVariablen = reelleVariablen(eingänge),
                variablenQuellen = eingänge.flatMap { it.variablenQuellen }.distinctBy {
                    Pair(Triple(it.knotenId, it.name, it.werteVorrat), it.alsMethodenParameter)
                },
                latexDarstellung = "${minuendWert.anzeigeLatex()} - ${subtrahendWert.anzeigeLatex()}",
            ),
        ))
    }
}

/** Ersetzt Intervallkonjunktionen wie `x < max ∧ min < x` durch die äquivalente Kettendarstellung. */
internal fun MathematikAuswerterRegister.registriereOptimierteKonjunktion() {
    registriere("mathematik.konjunktion") { k ->
        val werte = k.knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
            .mapNotNull { k.eingänge[it.name] }
        val aussagen = werte.map { it.objekt as? Aussage ?: error("Konjunktion benötigt Aussagen.") }
        require(aussagen.size >= 2) { "Mindestens zwei Aussagen müssen verbunden sein." }
        val annahmen = werte.flatMap { it.annahmen }.toSet()
        KnotenAuswertungsErgebnis(mapOf(
            "aussage" to BedingterWert(
                objekt = Konjunktion(aussagen),
                annahmen = annahmen,
                reelleVariablen = reelleVariablen(werte),
                latexDarstellung = intervallKette(aussagen),
            ),
        ))
    }
}

private fun intervallKette(aussagen: List<Aussage>): String? {
    if (aussagen.size != 2) return null
    val a = aussagen[0] as? Vergleich ?: return null
    val b = aussagen[1] as? Vergleich ?: return null
    if (a.art !in ordnungsVergleiche || b.art !in ordnungsVergleiche) return null
    return when {
        a.links == b.rechts -> "${b.links.zuLatex()} ${b.art.latex} ${a.links.zuLatex()} ${a.art.latex} ${a.rechts.zuLatex()}"
        a.rechts == b.links -> "${a.links.zuLatex()} ${a.art.latex} ${a.rechts.zuLatex()} ${b.art.latex} ${b.rechts.zuLatex()}"
        else -> null
    }
}

private val ordnungsVergleiche = setOf(VergleichsArt.Kleiner, VergleichsArt.KleinerGleich)
