package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.istJoinRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl

internal data class JoinWechsel(val karte: KartenDaten, val kandidat: KnotenDaten, val entfernteLokale: List<String> = emptyList())
private data class WechselBeitrag(val position: Int, val verbindung: VerbindungDaten? = null, val lokal: String? = null, val fallback: KartenVerweis? = null)

/** Graphbewusste Übernahme: Beiträge bleiben in ihrer bisherigen Reihenfolge, Verluste werden vorab gemeldet. */
internal fun bereiteJoinWechselVor(bisher: KartenDaten, kandidat: KnotenDaten): JoinWechsel {
    val alt = bisher.knoten.firstOrNull { it.id == kandidat.id } ?: return JoinWechsel(bisher, kandidat)
    if (istJoinRechnerOperator(joinOperatorId(kandidat).orEmpty())) {
        val normalisiert = bisher.copy(knoten = bisher.knoten.map { if (it.id == kandidat.id) kandidat else it }).normalisiereJoinEingänge(überführeEinzelAnschlüsse = (alt.anschlüsse+kandidat.anschlüsse).any {it.belegung==AnschlussBelegung.Mehrfach})
        return JoinWechsel(bisher.copy(verbindungen = normalisiert.verbindungen), normalisiert.knoten.single { it.id == kandidat.id })
    }
    val join = alt.anschlüsse.singleOrNull { it.belegung == AnschlussBelegung.Mehrfach } ?: return JoinWechsel(bisher, kandidat)
    val ref = AnschlussVerweis(alt.id, join.id)
    val quellen = bisher.verbindungen.filter { it.zu == ref }
    val lokale = alt.parameter.filterKeys { it.startsWith("join.lokal.") }
    val beiträge = (quellen.map { WechselBeitrag(it.eingangsReihenfolge, verbindung = it,
        fallback = it.eingangsFallback?.let(alt.eingangsKartenVerweise::get)) } +
        lokale.map { (key, wert) -> WechselBeitrag(alt.parameter["join.position." + key.removePrefix("join.lokal.")]?.toIntOrNull() ?: Int.MAX_VALUE,
            lokal = wert, fallback = alt.eingangsKartenVerweise.entries.firstOrNull { it.key.endsWith("@karte:" + key.removePrefix("join.lokal.")) }?.value) } +
        alt.eingangsKartenVerweise.filterKeys { name ->
            quellen.none { it.eingangsFallback == name } && !lokale.containsKey("join.lokal." + name.substringAfter("@karte:", ""))
        }.map { (name, verweis) -> WechselBeitrag(alt.parameter["join.position." + name.substringAfter("@")]?.toIntOrNull() ?: Int.MAX_VALUE, fallback = verweis) })
        .sortedBy { it.position }
    val ports = kandidat.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
    val parameter = kandidat.parameter.filterKeys { !it.startsWith("join.") }.toMutableMap()
    val fallback = kandidat.eingangsKartenVerweise.filterKeys { "@karte:" !in it && it != join.name }.toMutableMap()
    val verdrahtet = mutableMapOf<VerbindungsId, VerbindungDaten>()
    val entfernt = mutableListOf<String>()
    beiträge.forEachIndexed { index, beitrag ->
        val port = ports.getOrNull(index)
        if (port == null) {
            beitrag.lokal?.let(entfernt::add)
            if (beitrag.verbindung == null && beitrag.fallback != null) entfernt += "Kartenmethode ${beitrag.fallback.kartenId.wert}"
            return@forEachIndexed
        }
        beitrag.verbindung?.let { v -> verdrahtet[v.id] = v.copy(zu = AnschlussVerweis(alt.id, port.id), eingangsFallback = port.name.takeIf { beitrag.fallback != null }) }
        beitrag.lokal?.let { wert ->
            val zahl = port.art == MathematikAnschlussArten.Zahl.id || MathematikAnschlussArten.Zahl.id in port.zulässigeArten
            val aussage = port.art == MathematikAnschlussArten.Aussage.id || MathematikAnschlussArten.Aussage.id in port.zulässigeArten
            if (zahl && runCatching { RationaleZahl.parse(wert) }.isSuccess || aussage && wert.toBooleanStrictOrNull() != null)
                parameter["standardwert.${port.name}"] = wert else entfernt += wert
        }
        beitrag.fallback?.let { fallback[port.name] = it }
    }
    val neu = kandidat.copy(parameter = parameter, eingangsKartenVerweise = fallback)
    return JoinWechsel(bisher.copy(verbindungen = bisher.verbindungen.mapNotNull { v ->
        if (v.zu == ref) verdrahtet[v.id] else v
    }), neu, entfernt)
}
