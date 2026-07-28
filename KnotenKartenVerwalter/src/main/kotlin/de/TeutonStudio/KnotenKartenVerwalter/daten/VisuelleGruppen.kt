package de.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Entfernt verwaiste und mehrdeutige Gruppenmitgliedschaften.
 * Ein Knoten gehört höchstens einer visuellen Gruppe an; die zuerst gespeicherte Gruppe gewinnt.
 */
fun KartenDaten.bereinigteVisuelleGruppen(): KartenDaten {
    val vorhandeneKnoten = knoten.mapTo(mutableSetOf()) { it.id }
    val bereitsVerwendet = mutableSetOf<KnotenId>()
    val bereinigt = visuelleGruppen.mapNotNull { gruppe ->
        val ids = gruppe.knotenIds.asSequence()
            .filter { it in vorhandeneKnoten }
            .filter { bereitsVerwendet.add(it) }
            .toSet()
        ids.takeIf { it.size >= 2 }?.let { gruppe.copy(knotenIds = it) }
    }
    return if (bereinigt == visuelleGruppen) this else copy(visuelleGruppen = bereinigt)
}
