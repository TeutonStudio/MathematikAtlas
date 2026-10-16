package de.TeutonStudio.KnotenKartenVerwalter.daten

enum class KnotenMeldungsArt { Fehler, Warnung, Hinweis }

/** Flüchtige, fachneutrale Diagnose; kein Bestandteil des Karten-Codecs. */
data class KnotenMeldung(val text: String, val art: KnotenMeldungsArt = KnotenMeldungsArt.Hinweis)

fun Iterable<KnotenMeldung>.normalisierteMeldungen(): List<KnotenMeldung> =
    filter { it.text.isNotBlank() }.map { it.copy(text = it.text.trim()) }
        .sortedBy { it.art.ordinal }.distinctBy { it.text }

/** Unskalierte Höhen außerhalb des Körpers, ausschließlich für die Darstellung. */
data class KnotenAnhangMaße(val oben: Float = 0f, val unten: Float = 0f)

/** Abgeleitete Geometrie: Benutzermaße bleiben gespeichert, Inhaltsminima nicht. */
fun KnotenDaten.mitInhaltsminimum(minimum: GraphGröße?): KnotenDaten = if (minimum == null) this else copy(
    größe = GraphGröße(maxOf(größe.breite, minimum.breite), maxOf(größe.höhe, minimum.höhe)),
)

fun KartenDaten.mitInhaltsminima(minima: Map<KnotenId, GraphGröße>): KartenDaten = copy(
    knoten = knoten.map { it.mitInhaltsminimum(minima[it.id]) },
).mitGruppenInhaltsminimum()

/** Von innen nach außen erweitern; insbesondere leere Untergruppen mit umfassen. */
fun KartenDaten.mitGruppenInhaltsminimum(
    kopfMinima: Map<VisuelleGruppenId, GraphGröße> = emptyMap(),
    anhänge: Map<KnotenId, KnotenAnhangMaße> = emptyMap(),
): KartenDaten {
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
            val inhalt = g.knotenIds.mapNotNull(nachId::get).map {
                val anhang = anhänge[it.id] ?: KnotenAnhangMaße()
                (it.position - GraphPunkt(0f, anhang.oben)) to
                    GraphGröße(it.größe.breite, it.größe.höhe + anhang.oben + anhang.unten)
            } +
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
