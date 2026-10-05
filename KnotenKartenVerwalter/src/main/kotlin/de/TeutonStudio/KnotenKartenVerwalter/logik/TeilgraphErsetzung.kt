package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsId

/**
 * Vollständiger, gegen seinen Ursprungsstand prüfbarer Plan für eine atomare Teilgraphersetzung.
 * Die Ursprungslisten enthalten alle zu entfernenden Knoten und sämtliche sie berührenden Kanten.
 */
data class TeilgraphErsetzungsPlan(
    val ursprungsKnoten: List<KnotenDaten>,
    val ursprungsVerbindungen: List<VerbindungDaten>,
    val neueKnoten: List<KnotenDaten>,
    val neueVerbindungen: List<VerbindungDaten>,
) {
    val entfernteKnoten: Set<KnotenId> get() = ursprungsKnoten.mapTo(linkedSetOf()) { it.id }
    val entfernteVerbindungen: Set<VerbindungsId> get() = ursprungsVerbindungen.mapTo(linkedSetOf()) { it.id }
    val ersparnis: Int get() = entfernteKnoten.size - neueKnoten.size

    companion object {
        fun aus(
            karte: KartenDaten,
            entfernteKnoten: Set<KnotenId>,
            neueKnoten: List<KnotenDaten>,
            neueVerbindungen: List<VerbindungDaten>,
        ): TeilgraphErsetzungsPlan {
            val ursprungsKnoten = karte.knoten.filter { it.id in entfernteKnoten }
            val ursprungsVerbindungen = karte.verbindungen.filter {
                it.von.knotenId in entfernteKnoten || it.zu.knotenId in entfernteKnoten
            }
            return TeilgraphErsetzungsPlan(ursprungsKnoten, ursprungsVerbindungen, neueKnoten, neueVerbindungen)
        }
    }
}

data class TeilgraphErsetzungsVorschau(
    val karte: KartenDaten?,
    val fehler: List<String>,
) {
    val istGültig: Boolean get() = karte != null && fehler.isEmpty()
}

fun KartenDaten.vorschauTeilgraphErsetzen(
    plan: TeilgraphErsetzungsPlan,
    prüfung: GraphPrüfung,
): TeilgraphErsetzungsVorschau {
    val fehler = mutableListOf<String>()
    val entfernteIds = plan.entfernteKnoten
    if (entfernteIds.isEmpty()) fehler += "Der Ersetzungsplan enthält keine Ursprungsknoten."
    if (plan.ursprungsKnoten.map { it.id }.toSet().size != plan.ursprungsKnoten.size) {
        fehler += "Der Ersetzungsplan enthält doppelte Ursprungsknoten."
    }
    if (plan.neueKnoten.map { it.id }.toSet().size != plan.neueKnoten.size) {
        fehler += "Der Ersetzungsplan enthält doppelte neue Knoten-IDs."
    }

    val aktuelleUrsprungsknoten = knoten.filter { it.id in entfernteIds }
    if (aktuelleUrsprungsknoten != plan.ursprungsKnoten) {
        fehler += "Die betroffenen Knoten wurden seit der Analyse verändert."
    }
    val aktuelleUrsprungsverbindungen = verbindungen.filter {
        it.von.knotenId in entfernteIds || it.zu.knotenId in entfernteIds
    }
    if (aktuelleUrsprungsverbindungen != plan.ursprungsVerbindungen) {
        fehler += "Die Grenz- oder Innenverbindungen wurden seit der Analyse verändert."
    }

    val verbleibendeKnotenIds = knoten.asSequence().map { it.id }.filterNot { it in entfernteIds }.toSet()
    val kollidierendeKnoten = plan.neueKnoten.map { it.id }.filter { it in verbleibendeKnotenIds }
    if (kollidierendeKnoten.isNotEmpty()) fehler += "Neue Knoten-IDs kollidieren mit vorhandenen Knoten."

    val entfernteVerbindungsIds = plan.entfernteVerbindungen
    val verbleibendeVerbindungsIds = verbindungen.asSequence().map { it.id }
        .filterNot { it in entfernteVerbindungsIds }.toSet()
    if (plan.neueVerbindungen.map { it.id }.toSet().size != plan.neueVerbindungen.size) {
        fehler += "Der Ersetzungsplan enthält doppelte Verbindungs-IDs."
    }
    if (plan.neueVerbindungen.any { it.id in verbleibendeVerbindungsIds }) {
        fehler += "Neue Verbindungs-IDs kollidieren mit unbeteiligten Verbindungen."
    }
    if (fehler.isNotEmpty()) return TeilgraphErsetzungsVorschau(null, fehler.distinct())

    var probe = copy(
        knoten = knoten.filterNot { it.id in entfernteIds } + plan.neueKnoten,
        verbindungen = verbindungen.filterNot { it.id in entfernteVerbindungsIds },
        visuelleGruppen = visuelleGruppen.map { gruppe ->
            gruppe.copy(knotenIds = gruppe.knotenIds - entfernteIds)
        },
    )
    plan.neueVerbindungen.forEach { verbindung ->
        when (val ergebnis = prüfung.prüfe(probe, verbindung.von, verbindung.zu)) {
            VerbindungsPrüfung.Erlaubt -> probe = probe.copy(verbindungen = probe.verbindungen + verbindung)
            is VerbindungsPrüfung.Abgelehnt -> fehler += "Verbindung ${verbindung.id.wert}: ${ergebnis.grund}"
        }
    }
    return if (fehler.isEmpty()) TeilgraphErsetzungsVorschau(probe, emptyList())
    else TeilgraphErsetzungsVorschau(null, fehler)
}
