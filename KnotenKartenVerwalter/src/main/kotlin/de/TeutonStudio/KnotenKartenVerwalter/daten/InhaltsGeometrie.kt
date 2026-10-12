package de.TeutonStudio.KnotenKartenVerwalter.daten

/** Abgeleitete Mindestmaße; das gespeicherte Benutzermaß bleibt unverändert. */
fun KnotenDaten.mitInhaltsminimum(minimum: GraphGröße?): KnotenDaten =
    if (minimum == null) this else copy(
        größe = GraphGröße(maxOf(größe.breite, minimum.breite), maxOf(größe.höhe, minimum.höhe)),
    )

fun KartenDaten.mitInhaltsminima(minima: Map<KnotenId, GraphGröße>): KartenDaten =
    if (minima.isEmpty()) this else copy(knoten = knoten.map { it.mitInhaltsminimum(minima[it.id]) })
