package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Prüft Mehrdeutigkeiten vor fachlicher Normalisierung, ohne Graphdaten zu reparieren. */
fun KartenDaten.diagnostiziereHistorischeGraphMehrdeutigkeiten(): KartenDaten {
    val doppelteKnoten=knoten.groupBy {it.id}.filterValues {it.size>1}.keys
    val doppelteKanten=verbindungen.groupBy {it.id}.filterValues {it.size>1}.values.flatten()
        .flatMap {listOf(it.von.knotenId,it.zu.knotenId)}.toSet()
    val eingänge=verbindungen.groupBy {it.zu}
    return copy(knoten=knoten.map {k ->
        if(RECHNER_MIGRATION_DIAGNOSE in k.parameter || !(k.art.startsWith("mathematik.") || k.art==ÜBERSETZER_ART)) return@map k
        val grund=when {
            k.id in doppelteKnoten -> "Die Knoten-ID ist mehrfach vorhanden."
            k.id in doppelteKanten -> "Eine beteiligte Verbindungs-ID ist mehrfach vorhanden."
            k.anschlüsse.map {it.id}.distinct().size!=k.anschlüsse.size -> "Anschluss-IDs sind innerhalb des Knotens mehrfach vorhanden."
            k.anschlüsse.groupBy {it.richtung to it.name}.any {it.value.size>1} -> "Anschlussrollen sind innerhalb derselben Richtung mehrdeutig."
            k.anschlüsse.any {p -> p.richtung==AnschlussRichtung.Eingang && p.belegung==AnschlussBelegung.Einzeln && eingänge[AnschlussVerweis(k.id,p.id)].orEmpty().size>1} -> "Ein Einzelanschluss besitzt mehrere eingehende Verbindungen."
            else -> null
        }
        if(grund==null) k else k.copy(parameter=k.parameter+(RECHNER_MIGRATION_DIAGNOSE to "${k.art} bleibt erhalten: $grund"))
    })
}

/** Eine Ablehnung darf auch durch nachgelagerte Plattformnormalisierung keine Originaldaten verlieren. */
fun KartenDaten.bewahreDiagnostizierteOriginale(original: KartenDaten): KartenDaten {
    val diagnosen = (original.knoten + knoten).mapNotNull { k ->
        k.parameter[RECHNER_MIGRATION_DIAGNOSE]?.let { k.id to it }
    }.toMap()
    if (diagnosen.isEmpty()) return this
    val ursprüngliche = original.knoten.groupBy { it.id }.mapValues { it.value.iterator() }
    val erhalteneKnoten = knoten.map { k ->
        val diagnose = diagnosen[k.id] ?: return@map k
        val iterator = ursprüngliche[k.id]
        if (iterator != null && iterator.hasNext()) iterator.next().let { alt ->
            alt.copy(parameter = alt.parameter + (RECHNER_MIGRATION_DIAGNOSE to (alt.parameter[RECHNER_MIGRATION_DIAGNOSE] ?: diagnose)))
        } else k
    } + ursprüngliche.filterKeys { it in diagnosen }.values.flatMap { iterator ->
        iterator.asSequence().map { alt -> alt.copy(parameter = alt.parameter + (RECHNER_MIGRATION_DIAGNOSE to diagnosen.getValue(alt.id))) }.toList()
    }
    fun geschützt(v: VerbindungDaten) = v.von.knotenId in diagnosen || v.zu.knotenId in diagnosen
    val geschütztIds = original.verbindungen.filter(::geschützt).mapTo(hashSetOf()) { it.id }
    val umgesetzte = verbindungen.filterNot { geschützt(it) || it.id in geschütztIds }.groupBy { it.id }.mapValues { it.value.iterator() }
    val erhalteneKanten = original.verbindungen.flatMap { alt ->
        if (geschützt(alt) || alt.id in geschütztIds) listOf(alt) else umgesetzte[alt.id]?.let { if (it.hasNext()) listOf(it.next()) else emptyList() }.orEmpty()
    } + umgesetzte.values.flatMap { it.asSequence().toList() }
    return copy(knoten = erhalteneKnoten, verbindungen = erhalteneKanten)
}
