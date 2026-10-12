package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Fachneutral: gleich typisierte erweiterbare Eingänge bilden eine sortierbare Gruppe. */
fun dynamischeOperandengruppen(karte: KartenDaten, knoten: KnotenDaten, zusätzlichBelegteEingänge: Set<AnschlussId> = emptySet()): List<List<AnschlussDaten>> =
    knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.kannSichErweitern }
        .groupBy { Triple(it.art, it.zulässigeArten, it.kante) }.values.map { gruppe ->
            val geordnet = gruppe.sortedBy { it.reihenfolge }
            val belegt = karte.verbindungen.filter { it.zu.knotenId == knoten.id }.map { it.zu.anschlussId }.toSet()
            geordnet.dropLastWhile { it.dynamischErzeugt && it.id !in belegt && it.id !in zusätzlichBelegteEingänge && it.name !in knoten.eingangsKartenVerweise }
        }.filter { it.size > 1 }

/** Validiert gegen den aktuellen Snapshot; IDs, Rollen und Verbindungspartner bleiben erhalten. */
fun ordneDynamischeOperanden(
    karte: KartenDaten,
    knotenId: KnotenId,
    ids: List<AnschlussId>,
    zusätzlichBelegteEingänge: Set<AnschlussId> = emptySet(),
): KartenAktion.KnotenAnschlüsseÄndern? {
    val knoten = karte.knoten.singleOrNull { it.id == knotenId } ?: return null
    if (ids.distinct().size != ids.size) return null
    val gruppe = dynamischeOperandengruppen(karte, knoten, zusätzlichBelegteEingänge).singleOrNull { g -> g.map { it.id }.toSet() == ids.toSet() }
        ?: return null
    if (gruppe.map { it.id } == ids) return null
    val positionen = gruppe.map { it.reihenfolge }.sorted()
    val neu = ids.mapIndexed { index, id -> id to positionen[index] }.toMap()
    return KartenAktion.KnotenAnschlüsseÄndern(knotenId, knoten.anschlüsse.map { port ->
        neu[port.id]?.let { port.copy(reihenfolge = it) } ?: port
    })
}

/** Eine reine Umordnung darf weder persistierte Quellen noch eine freie Schlussreserve bereinigen. */
fun KartenDaten.istReineAnschlussUmordnung(neu: KartenDaten): Boolean {
    if (this == neu) return false
    val bisher = knoten.associateBy { it.id }
    val ohneReihenfolgeÄnderung = neu.copy(knoten = neu.knoten.map { neuerKnoten ->
        val alt = bisher[neuerKnoten.id] ?: return false
        val ports = alt.anschlüsse.associateBy { it.id }
        neuerKnoten.copy(anschlüsse = neuerKnoten.anschlüsse.map { port ->
            val alterPort = ports[port.id] ?: return false
            port.copy(reihenfolge = alterPort.reihenfolge)
        })
    })
    return this == ohneReihenfolgeÄnderung
}
