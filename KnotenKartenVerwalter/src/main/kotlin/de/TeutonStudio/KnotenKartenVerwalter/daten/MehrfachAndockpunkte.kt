package de.TeutonStudio.KnotenKartenVerwalter.daten

/** Reine Projektion: jede Leitung bekommt ein eigenes, dauerhaft aus ihrer ID abgeleitetes Ende. */
fun GruppenProjektion.mitMehrfachAndockpunkten(
    kapsel: (KartenDaten, KnotenDaten) -> Boolean = { _, _ -> true },
): GruppenProjektion {
    if (karte.knoten.none { k -> k.anschlüsse.any { it.belegung == AnschlussBelegung.Mehrfach } }) return this
    val originale = originalAnschlüsse.toMutableMap()
    val verbindungsZiele = mutableMapOf<VerbindungsId, AnschlussVerweis>()
    val knoten = karte.knoten.map { k ->
        val ports = k.anschlüsse.flatMap { p ->
            if (p.belegung != AnschlussBelegung.Mehrfach) listOf(p) else {
                val ref = AnschlussVerweis(k.id, p.id)
                val beiträge = karte.verbindungen.filter { it.zu == ref }.sortedBy { it.eingangsReihenfolge }
                val sammlung = kapsel(karte, k)
                beiträge.mapIndexed { index, v ->
                    val id = AnschlussId("join:" + p.id.wert + ":" + v.id.wert)
                    val sichtbar = AnschlussVerweis(k.id, id)
                    originale[sichtbar] = original(ref)
                    verbindungsZiele[v.id] = sichtbar
                    p.copy(id = id, name = "${p.name} · ${index + 1}", reihenfolge = index,
                        beitragsVerbindung = v.id, sammelAnschluss = p.id, sammelDarstellung = sammlung)
                } + p.copy(reihenfolge = beiträge.size, sammelAnschluss = p.id, sammelDarstellung = sammlung)
            }
        }
        k.copy(anschlüsse = ports, größe = k.größe.copy(höhe = maxOf(k.größe.höhe,
            ports.groupBy { it.kante }.values.maxOfOrNull { it.size + 1 }?.times(48f) ?: 0f)))
    }
    return copy(karte = karte.copy(knoten = knoten, verbindungen = karte.verbindungen.map { v ->
        verbindungsZiele[v.id]?.let { v.copy(zu = it) } ?: v
    }), originalAnschlüsse = originale)
}

/** Die Körpergröße ist abgeleitet; dieselbe Mindestgeometrie gilt für Layout und Projektion. */
fun KartenDaten.mitMehrfachMinima(): KartenDaten {
    val anzahl = verbindungen.groupingBy { it.zu }.eachCount()
    return copy(knoten = knoten.map { k ->
        if (k.anschlüsse.none { it.belegung == AnschlussBelegung.Mehrfach }) k else {
            val minimum = k.anschlüsse.groupBy { it.kante }.values.maxOf { ports ->
                ports.sumOf { p -> if (p.belegung == AnschlussBelegung.Mehrfach) (anzahl[AnschlussVerweis(k.id, p.id)] ?: 0) + 1 else 1 } + 1
            } * 48f
            k.copy(größe = k.größe.copy(höhe = maxOf(k.größe.höhe, minimum)))
        }
    }).mitGruppenInhaltsminimum()
}
