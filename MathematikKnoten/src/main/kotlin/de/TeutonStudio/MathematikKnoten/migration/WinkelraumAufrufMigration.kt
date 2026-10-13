package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*

/** Alte Komponentennamen bestimmen explizite Packung; Anschlussreihenfolge allein ist kein Nachweis. */
fun KartenDaten.migriereWinkelraumAufrufe(): KartenDaten {
    val nachId = knoten.associateBy { it.id }
    fun vorfahren(id: KnotenId, besucht: MutableSet<KnotenId>): List<KnotenDaten> {
        if (!besucht.add(id)) return emptyList()
        val k = nachId[id] ?: return emptyList()
        return listOf(k) + verbindungen.filter { it.zu.knotenId == id }.flatMap { vorfahren(it.von.knotenId, besucht) }
    }
    return copy(knoten = knoten.map { k ->
        if (k.art != METHODEN_AUFRUF_ART || METHODEN_STRUKTUR_PACKUNG in k.eigenschaften || METHODEN_STRUKTUR_MIGRATIONSFEHLER in k.parameter) return@map k
        val methodenPort = k.anschlüsse.firstOrNull { it.name == "methode" && it.richtung == AnschlussRichtung.Eingang } ?: return@map k
        val verbindung = verbindungen.singleOrNull { it.zu == AnschlussVerweis(k.id, methodenPort.id) } ?: return@map k
        val quellen = vorfahren(verbindung.von.knotenId, mutableSetOf()).filter {
            it.art == "mathematik.variable" && variablenStruktur(it) == VARIABLE_STRUKTUR_WINKEL
        }.distinctBy { it.id }
        if (quellen.isEmpty()) return@map k
        val ports = k.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }.sortedBy { it.reihenfolge }
        val namen = ports.mapIndexed { i, p -> p to k.parameter["$METHODEN_AUFRUF_PARAMETER_PREFIX$i.name"] }
        // Ein vorhandener einzelner Anschluss bezeichnet schon das ganze Tupelargument.
        if (ports.size <= 1) return@map k
        fun diagnose(text: String) = k.copy(parameter = k.parameter + (METHODEN_STRUKTUR_MIGRATIONSFEHLER to text))
        if (quellen.size != 1) return@map diagnose("Mehrere Winkelraumquellen: Die historischen Komponentenanschlüsse müssen ausdrücklich zugeordnet werden.")
        val quelle = quellen.single()
        val basisName = quelle.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
        val erwartet = (1..variablenDimension(quelle) + 1).map { "${basisName}_$it" }
        val komponenten = erwartet.map { name -> namen.singleOrNull { it.second == name }?.first }
        if (komponenten.any { it == null } || namen.any { it.second == null }) {
            // Bereits neue strukturierte Signaturen besitzen keinen alten Komponentennamensatz.
            if (namen.any { it.second == basisName } && namen.none { it.second in erwartet }) return@map k
            return@map diagnose("Die historischen Winkelkomponenten sind nicht eindeutig benannt. Anschlüsse und Verbindungen bleiben zur manuellen Zuordnung erhalten.")
        }
        fun verweis(p: AnschlussDaten) = KnotenEigenschaft.Objekt(mapOf("id" to KnotenEigenschaft.Text(p.id.wert), "name" to KnotenEigenschaft.Text(p.name)))
        val scalar = namen.filter { it.second !in erwartet }.associate { (p, name) ->
            p.name to KnotenEigenschaft.Text(name!!)
        }
        k.copy(eigenschaften = k.eigenschaften + (METHODEN_STRUKTUR_PACKUNG to KnotenEigenschaft.Objekt(mapOf(
            "komponenten" to KnotenEigenschaft.Liste(komponenten.filterNotNull().map(::verweis)),
            "skalare" to KnotenEigenschaft.Objekt(scalar),
        ))))
    })
}
