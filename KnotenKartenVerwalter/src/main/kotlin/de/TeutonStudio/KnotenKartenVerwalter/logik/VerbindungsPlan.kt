package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Rein berechneter Vorschlag. Die UI muss vor Übernahme noch den Ausgangsstand prüfen. */
data class VerbindungsVorschlag(val titel: String, val karte: KartenDaten)

data class VerbindungsAuswahl(val basis: KartenDaten, val vorschläge: List<VerbindungsVorschlag>)

fun verbindeGeprüft(karte: KartenDaten, von: AnschlussVerweis, zu: AnschlussVerweis, prüfung: GraphPrüfung): KartenDaten? {
    if (prüfung.prüfe(karte, von, zu) !is VerbindungsPrüfung.Erlaubt) return null
    val (quelle, ziel) = prüfung.normalisiere(karte, von, zu) ?: return null
    return karte.wendeAn(KartenAktion.VerbindungEinfügen(VerbindungDaten(von = quelle, zu = ziel)))
}

fun istGültigeVerdrahtung(karte: KartenDaten, prüfung: GraphPrüfung): Boolean {
    if (karte.knoten.map { it.id }.distinct().size != karte.knoten.size) return false
    val ports = karte.knoten.flatMap { it.anschlüsse.map { port -> port.id } }
    if (ports.distinct().size != ports.size) return false
    if (karte.verbindungen.map { it.id }.distinct().size != karte.verbindungen.size) return false
    if (karte.verbindungen.groupBy { it.zu }.any { (ziel, kanten) ->
            kanten.size > 1 && karte.findeAnschluss(ziel)?.let {
                it.richtung == AnschlussRichtung.Eingang && it.belegung == AnschlussBelegung.Einzeln
            } == true
        }) return false
    val knotenIds = karte.knoten.map { it.id }.toSet()
    if (karte.verbindungen.any { kante ->
            kante.von.knotenId !in knotenIds || kante.zu.knotenId !in knotenIds ||
                prüfung.normalisiere(karte, kante.von, kante.zu) != (kante.von to kante.zu)
        }) return false
    // Den fertigen Graph einmal prüfen, statt jede Kante erneut als hypothetische
    // Einzeländerung zu testen (diese würde ihrerseits alle Folgekanten durchsuchen).
    val nachfolger = karte.verbindungen.groupBy { it.von.knotenId }
    val grad = knotenIds.associateWith { 0 }.toMutableMap()
    karte.verbindungen.forEach { grad[it.zu.knotenId] = grad.getValue(it.zu.knotenId) + 1 }
    val offen = ArrayDeque(grad.filterValues { it == 0 }.keys)
    var besucht = 0
    while (offen.isNotEmpty()) {
        val id = offen.removeFirst()
        besucht++
        nachfolger[id].orEmpty().forEach {
            val rest = grad.getValue(it.zu.knotenId) - 1
            grad[it.zu.knotenId] = rest
            if (rest == 0) offen.add(it.zu.knotenId)
        }
    }
    return besucht == knotenIds.size && prüfung.inkompatibleVerbindungen(karte).isEmpty()
}

fun planeKnotenInVerbindung(karte: KartenDaten, knotenId: KnotenId, verbindungsId: VerbindungsId, prüfung: GraphPrüfung): List<VerbindungsVorschlag> {
    val knoten = karte.knoten.firstOrNull { it.id == knotenId } ?: return emptyList()
    val kante = karte.verbindungen.firstOrNull { it.id == verbindungsId } ?: return emptyList()
    if (knotenId in setOf(kante.von.knotenId, kante.zu.knotenId)) return emptyList()
    // Die alte Kante hält abhängige Typen während der Zwischenprüfung stabil.
    // Im fertigen Graph wird genau ihr Quellende ersetzt, auch bei Mehrfachbelegung.
    val basis = karte
    return knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.flatMap { eingang ->
        val eingangsRef = AnschlussVerweis(knotenId, eingang.id)
        if (basis.verbindungen.any { it.zu == eingangsRef }) return@flatMap emptyList()
        val mitEingang = verbindeGeprüft(basis, kante.von, eingangsRef, prüfung) ?: return@flatMap emptyList()
        knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.mapNotNull { ausgang ->
            val fertig = mitEingang.copy(verbindungen = mitEingang.verbindungen.map {
                if (it.id == kante.id) it.copy(von = AnschlussVerweis(knotenId, ausgang.id)) else it
            })
            if (!istGültigeVerdrahtung(fertig, prüfung)) return@mapNotNull null
            VerbindungsVorschlag("${knoten.name}: ${eingang.name} → ${ausgang.name}", fertig)
        }
    }
}

/** Nur eine eindeutige benutzte Eingangsquelle/Ausgangsgruppe darf überbrückt werden. */
fun planeKnotenHerausziehen(karte: KartenDaten, id: KnotenId, prüfung: GraphPrüfung): VerbindungsVorschlag? {
    val eingang = karte.verbindungen.filter { it.zu.knotenId == id }.singleOrNull() ?: return null
    val ausgänge = karte.verbindungen.filter { it.von.knotenId == id }
    if (ausgänge.isEmpty() || ausgänge.map { it.von }.distinct().size != 1) return null
    val probe = karte.copy(verbindungen = karte.verbindungen.mapNotNull { kante ->
        when {
            kante.id == eingang.id -> null
            kante.von.knotenId == id -> kante.copy(von = eingang.von)
            else -> kante
        }
    })
    if (!istGültigeVerdrahtung(probe, prüfung)) return null
    return VerbindungsVorschlag("Knoten aus Verbindung herausziehen", probe)
}
