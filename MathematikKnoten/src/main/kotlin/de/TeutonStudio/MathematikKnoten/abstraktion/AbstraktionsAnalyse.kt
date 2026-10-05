package de.TeutonStudio.MathematikKnoten.abstraktion

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.TeilgraphErsetzungsPlan
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis

enum class AbstraktionsSicherheit { STRUKTURELL_BEWIESEN, IDENTISCH }

data class AbstraktionsVorschlag(
    val id: String,
    val titel: String,
    val beschreibung: String,
    val wurzelKnoten: KnotenId,
    val vorherLatex: String?,
    val nachherLatex: String?,
    val vorherSchnittstelle: String,
    val nachherSchnittstelle: String,
    val sicherheit: AbstraktionsSicherheit,
    val ersetzungsPlan: TeilgraphErsetzungsPlan,
) {
    val vorherKnoten: Set<KnotenId> get() = ersetzungsPlan.entfernteKnoten
    val ersparnis: Int get() = ersetzungsPlan.ersparnis
}

data class AbstraktionsKontext(
    val karte: KartenDaten,
    val auswertung: KartenAuswertungsErgebnis,
    val graph: TeilgraphIndex = TeilgraphIndex.aus(karte, auswertung),
    val graphPrüfung: GraphPrüfung,
    val auswerten: (KartenDaten) -> KartenAuswertungsErgebnis,
)

fun interface AbstraktionsRegel {
    fun finde(kontext: AbstraktionsKontext): List<AbstraktionsVorschlag>
}

class AbstraktionsRegister(
    private val regeln: List<AbstraktionsRegel> = listOf(PolynomAbstraktionsRegel),
) {
    fun analysiere(kontext: AbstraktionsKontext): List<AbstraktionsVorschlag> = regeln
        .flatMap { it.finde(kontext) }
        .filter { it.ersparnis > 0 }
        .distinctBy { it.id }
        .sortedWith(
            compareByDescending<AbstraktionsVorschlag> { it.ersparnis }
                .thenByDescending { it.sicherheit }
                .thenByDescending { it.vorherKnoten.size }
                .thenBy { kontext.graph.topologischeReihenfolge[it.wurzelKnoten] ?: Int.MAX_VALUE }
                .thenBy { it.id },
        )
}

data class TeilgraphIndex(
    val ausgangZuWert: Map<AnschlussVerweis, BedingterWert>,
    val eingangZuQuelle: Map<AnschlussVerweis, AnschlussVerweis>,
    val vorgänger: Map<KnotenId, Set<KnotenId>>,
    val nachfolger: Map<KnotenId, Set<KnotenId>>,
    val topologischeReihenfolge: Map<KnotenId, Int>,
) {
    fun vorgängerAb(wurzel: KnotenId): Set<KnotenId> {
        val ergebnis = linkedSetOf<KnotenId>()
        val offen = ArrayDeque<KnotenId>().apply { add(wurzel) }
        while (offen.isNotEmpty()) {
            val aktuell = offen.removeFirst()
            if (!ergebnis.add(aktuell)) continue
            vorgänger[aktuell].orEmpty().forEach(offen::add)
        }
        return ergebnis
    }

    companion object {
        fun aus(karte: KartenDaten, auswertung: KartenAuswertungsErgebnis): TeilgraphIndex {
            val ausgänge = buildMap {
                karte.knoten.forEach { knoten ->
                    val ergebnis = auswertung.knoten[knoten.id] ?: return@forEach
                    knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.forEach { anschluss ->
                        ergebnis.ausgaben[anschluss.name]?.let { put(AnschlussVerweis(knoten.id, anschluss.id), it) }
                    }
                }
            }
            val vorgänger = karte.verbindungen.groupBy { it.zu.knotenId }
                .mapValues { (_, kanten) -> kanten.mapTo(linkedSetOf()) { it.von.knotenId } }
            val nachfolger = karte.verbindungen.groupBy { it.von.knotenId }
                .mapValues { (_, kanten) -> kanten.mapTo(linkedSetOf()) { it.zu.knotenId } }
            return TeilgraphIndex(
                ausgangZuWert = ausgänge,
                eingangZuQuelle = karte.verbindungen.associate { it.zu to it.von },
                vorgänger = vorgänger,
                nachfolger = nachfolger,
                topologischeReihenfolge = topologischeSortierung(karte),
            )
        }

        private fun topologischeSortierung(karte: KartenDaten): Map<KnotenId, Int> {
            val grad = karte.knoten.associate { knoten ->
                knoten.id to karte.verbindungen.count { it.zu.knotenId == knoten.id }
            }.toMutableMap()
            val offen = java.util.PriorityQueue<KnotenId>(compareBy { it.wert })
            grad.filterValues { it == 0 }.keys.forEach(offen::add)
            val ergebnis = linkedMapOf<KnotenId, Int>()
            while (offen.isNotEmpty()) {
                val id = offen.remove()
                ergebnis[id] = ergebnis.size
                karte.verbindungen.filter { it.von.knotenId == id }.forEach { kante ->
                    val ziel = kante.zu.knotenId
                    grad[ziel] = (grad[ziel] ?: 1) - 1
                    if (grad[ziel] == 0) offen.add(ziel)
                }
            }
            return ergebnis
        }
    }
}
