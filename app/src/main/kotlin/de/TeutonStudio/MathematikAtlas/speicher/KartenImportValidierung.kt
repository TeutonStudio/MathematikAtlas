package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

internal fun prüfeKartenId(id: KartenId) {
        require(id.wert.isNotBlank() && id.wert.toByteArray(Charsets.UTF_8).size <= 200 && id.wert !in setOf(".", "..") &&
            id.wert.none { it == '/' || it == '\\' || it.isISOControl() }) { "Ungültige Karten-ID im Import." }
    }

/** Vollständige strukturelle Prüfung vor Journal/Dateimutationen; unbekannte Fachtypen bleiben ladbar. */
internal fun prüfeImportKarte(karte: KartenDaten) {
    prüfeKartenId(karte.id)
    require(karte.version >= 1 && karte.name.isNotBlank()) { "Karte benötigt einen Namen und eine positive Version." }
    require(karte.ansicht.zoom.isFinite() && karte.ansicht.zoom > 0 && karte.ansicht.verschiebung.x.isFinite() && karte.ansicht.verschiebung.y.isFinite()) { "Ungültige Kartenansicht." }
    val knoten = karte.knoten.associateBy { it.id }
    require(knoten.size == karte.knoten.size) { "Doppelte Knoten-ID im Import." }
    val ports = mutableMapOf<AnschlussVerweis, AnschlussDaten>()
    karte.knoten.forEach { k ->
        require(k.id.wert.isNotBlank() && k.art.isNotBlank() && k.name.isNotBlank()) { "Ungültiger Knoten im Import." }
        require(k.position.x.isFinite() && k.position.y.isFinite() && k.größe.breite.isFinite() && k.größe.höhe.isFinite() && k.größe.breite > 0 && k.größe.höhe > 0) { "Ungültige Knotengeometrie im Import." }
        k.anschlüsse.forEach { p ->
            require(p.id.wert.isNotBlank() && p.name.isNotBlank() && ports.put(AnschlussVerweis(k.id, p.id), p) == null) { "Ungültige oder doppelte Anschluss-ID im Import." }
        }
        (listOfNotNull(k.kartenVerweis) + k.eingangsKartenVerweise.values).forEach {
            prüfeKartenId(it.kartenId)
            require(it.version >= 1) { "Ungültiger Kartenverweis im Import." }
        }
    }
    require(karte.verbindungen.map { it.id }.distinct().size == karte.verbindungen.size) { "Doppelte Verbindungs-ID im Import." }
    val belegteEingänge = mutableSetOf<AnschlussVerweis>()
    val eingangsZahl = knoten.keys.associateWith { 0 }.toMutableMap()
    val nachfolger = mutableMapOf<KnotenId, MutableList<KnotenId>>()
    karte.verbindungen.forEach { v ->
        val von = ports[v.von]?.richtung
        val zu = ports[v.zu]?.richtung
        val gerichtet = von == AnschlussRichtung.Ausgang && zu in setOf(AnschlussRichtung.Eingang, AnschlussRichtung.Neutral)
        val neutral = von == AnschlussRichtung.Neutral && zu == AnschlussRichtung.Neutral
        require(v.id.wert.isNotBlank() && (gerichtet || neutral)) { "Verbindung verweist auf fehlende oder falsch gerichtete Anschlüsse." }
        if (zu == AnschlussRichtung.Eingang) require(belegteEingänge.add(v.zu)) { "Mehrere Verbindungen an einem Einzeleingang." }
        nachfolger.getOrPut(v.von.knotenId) { mutableListOf() }.add(v.zu.knotenId)
        eingangsZahl[v.zu.knotenId] = eingangsZahl.getValue(v.zu.knotenId) + 1
    }
    val offen = ArrayDeque(eingangsZahl.filterValues { it == 0 }.keys)
    var besucht = 0
    while (offen.isNotEmpty()) {
        val id = offen.removeFirst()
        besucht++
        nachfolger[id].orEmpty().forEach { ziel ->
            val rest = eingangsZahl.getValue(ziel) - 1
            eingangsZahl[ziel] = rest
            if (rest == 0) offen.addLast(ziel)
        }
    }
    require(besucht == knoten.size) { "Die importierte Karte enthält einen Verbindungszyklus." }
    require(karte.visuelleGruppen.map { it.id }.distinct().size == karte.visuelleGruppen.size && karte.visuelleGruppen.all { g -> g.id.wert.isNotBlank() && g.knotenIds.all { it in knoten } && g.position.x.isFinite() && g.position.y.isFinite() && g.größe.breite.isFinite() && g.größe.höhe.isFinite() }) { "Ungültige visuelle Gruppe im Import." }
}

internal fun prüfeImportPaket(karten: List<KartenDaten>) {
    karten.forEach(::prüfeImportKarte)
    val nachVerweis = karten.associateBy { KartenVerweis(it.id, it.version) }
    require(nachVerweis.size == karten.size) { "Das Freigabepaket enthält doppelte Kartenidentitäten." }
    val abhängigkeiten = nachVerweis.mapValues { (_, karte) ->
        karte.knoten.flatMap { listOfNotNull(it.kartenVerweis) + it.eingangsKartenVerweise.values }.toSet()
    }
    require(abhängigkeiten.values.flatten().all { it in nachVerweis }) { "Eine Kartenabhängigkeit fehlt im Freigabepaket." }
    val rest = abhängigkeiten.mapValues { it.value.size }.toMutableMap()
    val umgekehrt = mutableMapOf<KartenVerweis, MutableList<KartenVerweis>>()
    abhängigkeiten.forEach { (quelle, ziele) -> ziele.forEach { umgekehrt.getOrPut(it) { mutableListOf() }.add(quelle) } }
    val offen = ArrayDeque(rest.filterValues { it == 0 }.keys)
    var besucht = 0
    while (offen.isNotEmpty()) {
        val id = offen.removeFirst()
        besucht++
        umgekehrt[id].orEmpty().forEach { ziel ->
            rest[ziel] = rest.getValue(ziel) - 1
            if (rest.getValue(ziel) == 0) offen.addLast(ziel)
        }
    }
    require(besucht == karten.size) { "Das Freigabepaket enthält zyklische Kartenverweise." }
}
