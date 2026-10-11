package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten

/** Reine, fachneutrale Vorschau derselben Anschlussregel wie [KartenAktion.KnotenErsetzen]. */
data class KnotenErsetzungsAuswirkung(
    val erhalteneAnschlüsse: List<AnschlussDaten>,
    val hinzugefügteAnschlüsse: List<AnschlussDaten>,
    val entfallendeAnschlüsse: List<AnschlussDaten>,
    val entfallendeVerbindungen: List<VerbindungDaten>,
) {
    val trenntVerbindungen: Boolean
        get() = entfallendeVerbindungen.isNotEmpty()
}

fun KartenDaten.vorschauKnotenErsetzen(neuerKnoten: KnotenDaten, prüfung: GraphPrüfung? = null): KnotenErsetzungsAuswirkung {
    val bisherigerKnoten = knoten.firstOrNull { it.id == neuerKnoten.id }
        ?: return KnotenErsetzungsAuswirkung(
            erhalteneAnschlüsse = emptyList(),
            hinzugefügteAnschlüsse = neuerKnoten.anschlüsse,
            entfallendeAnschlüsse = emptyList(),
            entfallendeVerbindungen = emptyList(),
        )
    val bisherigeIds = bisherigerKnoten.anschlüsse.mapTo(linkedSetOf(), AnschlussDaten::id)
    val neueIds = neuerKnoten.anschlüsse.mapTo(linkedSetOf(), AnschlussDaten::id)
    val entfallendeIds = bisherigeIds - neueIds

    val entfernte = verbindungen.filter { it.verweistAuf(neuerKnoten, entfallendeIds) }
    val kandidat = copy(
        knoten = knoten.map { if (it.id == neuerKnoten.id) neuerKnoten else it },
        verbindungen = verbindungen - entfernte.toSet(),
    )
    val vorherUngültig = prüfung?.inkompatibleVerbindungen(this)?.map { it.id }.orEmpty().toSet()
    val neuUngültig = prüfung?.inkompatibleVerbindungen(kandidat).orEmpty().filter { it.id !in vorherUngültig }
    return KnotenErsetzungsAuswirkung(
        erhalteneAnschlüsse = neuerKnoten.anschlüsse.filter { it.id in bisherigeIds },
        hinzugefügteAnschlüsse = neuerKnoten.anschlüsse.filter { it.id !in bisherigeIds },
        entfallendeAnschlüsse = bisherigerKnoten.anschlüsse.filter { it.id !in neueIds },
        entfallendeVerbindungen = (entfernte + neuUngültig).distinctBy { it.id },
    )
}

private fun VerbindungDaten.verweistAuf(
    knoten: KnotenDaten,
    anschlussIds: Set<AnschlussId>,
): Boolean =
    (von.knotenId == knoten.id && von.anschlussId in anschlussIds) ||
        (zu.knotenId == knoten.id && zu.anschlussId in anschlussIds)
