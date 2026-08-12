package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val RAND_KNOTEN_ART = "mathematik.rand"

object RandKnotenVorlagen {
    val Rand = KnotenVorlage(
        art = RAND_KNOTEN_ART,
        name = "Rand",
        kategorie = "Mengenlehre: Topologie",
        beschreibung = "Topologischer Rand ∂_X A einer Menge A im explizit verbundenen topologischen Raum X.",
        standardGröße = GraphGröße(255f, 125f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "menge",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
            ),
            AnschlussDaten(
                name = "raum",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.TopologischerRaum.id,
            ),
            AnschlussDaten(
                name = "rand",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
    )

    val alle = listOf(Rand)
}

internal fun MathematikAuswerterRegister.registriereRandKnoten() {
    registriere(RAND_KNOTEN_ART) { kontext ->
        val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
            ?: error("Für den Rand fehlt die Menge.")
        val raum = when (val struktur = kontext.eingänge["raum"]?.objekt) {
            is TopologischerRaum -> struktur
            is MetrischerRaum -> struktur.alsTopologischerRaum
            null -> migriereHistorischenRandRaum(kontext.knoten.parameter)
                ?: return@registriere KnotenAuswertungsErgebnis(
                    ausgaben = emptyMap(),
                    fehler = "Für den Rand fehlt der topologische Raum.",
                    eingänge = kontext.eingänge,
                )
            else -> return@registriere KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                fehler = "Der Raumanschluss enthält keinen topologischen oder metrischen Raum.",
                eingänge = kontext.eingänge,
            )
        }
        val rand = runCatching { topologischerRand(menge, raum) }.getOrElse { fehler ->
            return@registriere KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                fehler = fehler.message ?: "Der Rand konnte in diesem topologischen Raum nicht gebildet werden.",
                eingänge = kontext.eingänge,
            )
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "rand" to BedingterWert(
                    objekt = rand,
                    annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet(),
                    latexDarstellung = rand.zuLatex(),
                ),
            ),
            eingänge = kontext.eingänge,
            warnungen = if (kontext.eingänge["raum"] == null) {
                listOf("Historische Randparameter wurden als strukturierter topologischer Raum interpretiert; neue Karten speichern den Raum über den Strukturanschluss.")
            } else emptyList(),
        )
    }
}

/**
 * Einziger Kompatibilitätspfad für Karten aus der Zeit vor #386. Fehlen die alten
 * Parameter vollständig, wird ausdrücklich kein ℝ-Raum mehr erfunden.
 */
private fun migriereHistorischenRandRaum(parameter: Map<String, String>): TopologischerRaum? {
    val hatAltenKontext = "topologie" in parameter || "umgebungsraum" in parameter
    if (!hatAltenKontext) return null
    val traeger = parseHistorischenRandUmgebungsraum(parameter["umgebungsraum"]) ?: return null
    val topologie = when (parameter["topologie"]?.trim()?.lowercase()) {
        null, "", "kanonisch", "automatisch", "kanonisch:r", "r", "reell" ->
            StandardTopologieRegister.fuer(traeger) ?: return null
        "diskret" -> DiskreteTopologie(traeger)
        "indiskret", "trivial" -> IndiskreteTopologie(traeger)
        "symbolisch" -> SymbolischeTopologie(traeger)
        else -> SymbolischeTopologie(traeger, parameter["topologie"]!!.trim())
    }
    return TopologischerRaum(traeger, topologie)
}

private fun parseHistorischenRandUmgebungsraum(wert: String?): MengenAusdruck? = when (wert?.trim()?.lowercase()) {
    null, "" -> null
    "r", "reell", "\\mathbb r", "\\mathbb{r}" -> ReelleZahlen
    else -> BenannteMenge(wert.trim(), wert.trim())
}
