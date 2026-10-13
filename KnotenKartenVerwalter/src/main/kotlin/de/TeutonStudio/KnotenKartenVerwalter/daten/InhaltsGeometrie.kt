package de.TeutonStudio.KnotenKartenVerwalter.daten

/** Abgeleitete Geometrie: Benutzermaße bleiben gespeichert, Inhaltsminima nicht. */
fun KnotenDaten.mitInhaltsminimum(minimum: GraphGröße?): KnotenDaten = if (minimum == null) this else copy(
    größe = GraphGröße(maxOf(größe.breite, minimum.breite), maxOf(größe.höhe, minimum.höhe)),
)

fun KartenDaten.mitInhaltsminima(minima: Map<KnotenId, GraphGröße>): KartenDaten = copy(
    knoten = knoten.map { it.mitInhaltsminimum(minima[it.id]) },
).mitGruppenInhaltsminimum()

/** Von innen nach außen erweitern; insbesondere leere Untergruppen mit umfassen. */
fun KartenDaten.mitGruppenInhaltsminimum(kopfMinima: Map<VisuelleGruppenId, GraphGröße> = emptyMap()): KartenDaten {
    if (visuelleGruppen.isEmpty()) return this
    val index = GruppenIndex(this)
    val nachId = knoten.associateBy { it.id }
    val gruppen = visuelleGruppen.associateBy { it.id }.toMutableMap()
    visuelleGruppen.sortedByDescending { index.vorfahren(it.id).size }.forEach { original ->
        val kopf = kopfMinima[original.id] ?: GraphGröße(VISUELLE_GRUPPE_MINDEST_BREITE, VISUELLE_GRUPPE_KOPFZEILE_HÖHE)
        val g = original.copy(größe = GraphGröße(maxOf(original.größe.breite, kopf.breite),
            maxOf(original.größe.höhe, kopf.höhe + VISUELLE_GRUPPE_INHALT_ABSTAND)))
        gruppen[g.id] = g
        if (!g.eingeklappt) {
            val inhalt = g.knotenIds.mapNotNull(nachId::get).map { it.position to it.größe } +
                index.kinder[g.id].orEmpty().mapNotNull { kind -> gruppen[kind.id]?.let { it.position to it.größe } }
            if (inhalt.isNotEmpty()) {
                val links = minOf(g.position.x, inhalt.minOf { it.first.x } - VISUELLE_GRUPPE_INHALT_ABSTAND)
                val oben = minOf(g.position.y, inhalt.minOf { it.first.y } - maxOf(VISUELLE_GRUPPE_KOPFZEILE_HÖHE, kopf.höhe) - VISUELLE_GRUPPE_INHALT_ABSTAND)
                val rechts = maxOf(g.position.x + g.größe.breite, inhalt.maxOf { it.first.x + it.second.breite } + VISUELLE_GRUPPE_INHALT_ABSTAND)
                val unten = maxOf(g.position.y + g.größe.höhe, inhalt.maxOf { it.first.y + it.second.höhe } + VISUELLE_GRUPPE_INHALT_ABSTAND)
                gruppen[g.id] = g.copy(position = GraphPunkt(links, oben), größe = GraphGröße(rechts - links, unten - oben).alsGültigeVisuelleGruppenGröße())
            }
        }
    }
    return copy(visuelleGruppen = visuelleGruppen.map { gruppen.getValue(it.id) })
}
