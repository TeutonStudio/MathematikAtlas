package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Nur Struktur, keine veränderlichen Parameter oder mathematischen Ergebnisse. */
internal class GraphAuswertungsPlan(karte: KartenDaten) {
    private val knotenIds = karte.knoten.map { it.id }
    private val verbindungen = karte.verbindungen.toList()
    val eingehend: Map<KnotenId, List<VerbindungDaten>> = verbindungen.groupBy { it.zu.knotenId }
    val reihenfolge: List<KnotenId>

    init {
        val nachfolger = verbindungen.groupBy { it.von.knotenId }
            .mapValues { (_, kanten) -> kanten.map { it.zu.knotenId }.distinct() }
        val grad = knotenIds.associateWith { id ->
            eingehend[id].orEmpty().map { it.von.knotenId }.distinct().size
        }.toMutableMap()
        val offen = java.util.PriorityQueue<KnotenId>(compareBy { it.wert })
        grad.filterValues { it == 0 }.keys.forEach(offen::add)
        reihenfolge = buildList {
            while (offen.isNotEmpty()) {
                val id = offen.remove()
                // Eine Kante auf einen fehlenden Zielknoten erzeugt keinen Phantomknoten.
                if (id !in grad) continue
                add(id)
                nachfolger[id].orEmpty().forEach { ziel ->
                    if (ziel !in grad) return@forEach
                    val rest = grad.getValue(ziel) - 1
                    grad[ziel] = rest
                    if (rest == 0) offen.add(ziel)
                }
            }
        }
    }

    fun passtZu(karte: KartenDaten): Boolean =
        knotenIds.size == karte.knoten.size &&
            knotenIds.indices.all { knotenIds[it] == karte.knoten[it].id } &&
            verbindungen == karte.verbindungen
}

/** Aktuelle Daten pro Durchlauf: ein Strukturtreffer darf keine alten Portnamen liefern. */
internal class GraphAuswertungsIndex(karte: KartenDaten) {
    val knoten: Map<KnotenId, KnotenDaten> = karte.knoten.associateBy { it.id }
    private val anschlüsse = knoten.mapValues { (_, knoten) -> knoten.anschlüsse.associateBy { it.id } }

    fun anschluss(ref: AnschlussVerweis): AnschlussDaten? = anschlüsse[ref.knotenId]?.get(ref.anschlussId)
}
