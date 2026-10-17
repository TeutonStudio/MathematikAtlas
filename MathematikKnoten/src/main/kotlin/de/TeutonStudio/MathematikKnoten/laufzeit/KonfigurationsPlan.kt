package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.synchronisiereRestriktionsAnschlüsse

data class KonfigurationsPlan(val karte: KartenDaten, val auswirkung: KnotenErsetzungsAuswirkung)

/** Reine Vorschau inklusive aller durch Signatur-/Formänderung abhängigen Anschlüsse. */
fun planeKonfiguration(
    bisher: KartenDaten,
    kandidat: KnotenDaten,
    prüfung: GraphPrüfung,
    auswerten: (KartenDaten) -> KartenAuswertungsErgebnis,
): KonfigurationsPlan {
    val wechsel = bereiteEingangsRollenWechselVor(bereiteJoinWechselVor(bisher, kandidat))
    val direkte = wechsel.karte.vorschauKnotenErsetzen(wechsel.kandidat, prüfung).copy(entfallendeLokaleWerte = wechsel.entfernteLokale)
    var neu = wechsel.karte.wendeAn(KartenAktion.KnotenErsetzen(wechsel.kandidat, direkte.entfallendeVerbindungen.map { it.id }.toSet()))
    // Signaturen können weitere Signaturen bestimmen. Jede Runde wird aus dem
    // tatsächlich vorgeschlagenen Stand ausgewertet, nie aus dem sichtbaren Altstand.
    repeat(bisher.knoten.size + 1) {
        val ergebnis = auswerten(neu)
        val restriktion = synchronisiereRestriktionsAnschlüsse(neu, ergebnis)
        val bildmenge = synchronisiereBildmengenAnschlüsse(restriktion, ergebnis)
        val zerleger = synchronisiereStrukturZerleger(bildmenge, ergebnis, prüfung)
        val synchronisiert = synchronisiereMethodenAufrufe(zerleger, ergebnis, prüfung)
        if (synchronisiert == neu) {
            val neueIds = neu.verbindungen.map { it.id }.toSet()
            return KonfigurationsPlan(neu, direkte.copy(entfallendeVerbindungen = bisher.verbindungen.filter { it.id !in neueIds }))
        }
        neu = synchronisiert
    }
    error("Die abhängigen Anschlussverträge stabilisieren sich nicht; die Konfiguration wurde nicht übernommen.")
}
