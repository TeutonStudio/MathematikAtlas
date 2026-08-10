package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.BenannteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.TopologieArt
import de.TeutonStudio.MathematikRechenSystem.kern.TopologischerKontext
import de.TeutonStudio.MathematikRechenSystem.kern.topologischerRand

const val RAND_KNOTEN_ART = "mathematik.rand"

object RandKnotenVorlagen {
    val Rand = KnotenVorlage(
        art = RAND_KNOTEN_ART,
        name = "Rand",
        kategorie = "Mengenlehre: Topologie",
        beschreibung = "Topologischer Rand ∂_X A einer Menge A in einem gewählten Umgebungsraum X.",
        standardGröße = GraphGröße(230f, 112f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "menge",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
            ),
            AnschlussDaten(
                name = "rand",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
        standardParameter = mapOf(
            "topologie" to "kanonisch",
            "umgebungsraum" to "R",
            "relativ" to "false",
        ),
    )

    val alle = listOf(Rand)
}

internal fun MathematikAuswerterRegister.registriereRandKnoten() {
    registriere(RAND_KNOTEN_ART) { kontext ->
        val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
            ?: error("Für den Rand fehlt die Menge.")
        val parameter = kontext.knoten.parameter
        val umgebungsraum = parseRandUmgebungsraum(parameter["umgebungsraum"])
        val topologie = TopologieArt.ausPersistenz(parameter["topologie"])
        val relativ = parameter["relativ"].toBoolean()
        val rand = topologischerRand(
            menge,
            TopologischerKontext(
                umgebungsraum = umgebungsraum,
                topologie = topologie,
                relativ = relativ,
            ),
        )
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "rand" to BedingterWert(
                    objekt = rand,
                    annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet(),
                    latexDarstellung = rand.zuLatex(),
                ),
            ),
        )
    }
}

private fun parseRandUmgebungsraum(wert: String?): MengenAusdruck = when (wert?.trim()?.lowercase()) {
    null, "", "r", "reell", "\\mathbb r", "\\mathbb{r}" -> ReelleZahlen
    else -> BenannteMenge(wert.trim(), wert.trim())
}
