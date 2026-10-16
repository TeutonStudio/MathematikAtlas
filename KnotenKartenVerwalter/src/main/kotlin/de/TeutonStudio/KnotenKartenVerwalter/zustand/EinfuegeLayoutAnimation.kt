package de.TeutonStudio.KnotenKartenVerwalter.zustand

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Momentaufnahme der vorherigen Projektion, keine zweite persistente Karte. */
internal class EinfuegeLayoutAnimation(vorher: KartenDaten) {
    private val knoten = vorher.knoten.associateBy { it.id }
    private val gruppen = vorher.visuelleGruppen.associateBy { it.id }

    fun darstellung(ziel: KartenDaten, fortschritt: Float): KartenDaten {
        val rest = 1f - fortschritt.coerceIn(0f, 1f)
        return ziel.copy(
            knoten = ziel.knoten.map { k -> knoten[k.id]?.let { alt ->
                k.copy(position = k.position + (alt.position - k.position) * rest)
            } ?: k },
            visuelleGruppen = ziel.visuelleGruppen.map { g -> gruppen[g.id]?.let { alt ->
                g.copy(position = g.position + (alt.position - g.position) * rest,
                    größe = GraphGröße(g.größe.breite + (alt.größe.breite - g.größe.breite) * rest,
                        g.größe.höhe + (alt.größe.höhe - g.größe.höhe) * rest))
            } ?: g },
        )
    }
}
