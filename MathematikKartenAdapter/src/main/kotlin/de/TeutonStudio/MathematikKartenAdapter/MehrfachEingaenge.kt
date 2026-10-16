package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

internal fun KnotenAuswertungsKontext.mehrfachWerte(): Map<String, List<BedingterWert>> =
    knoten.anschlüsse.filter { it.belegung == AnschlussBelegung.Mehrfach }.associate { port ->
        port.name to (mehrfachEingänge[port.name] ?: eingänge.filterKeys { it.startsWith(port.name + "@") }.values.toList())
    }

/** Entfaltung am Adapterrand; bestehende Fachauswerter behalten ihre Operandenverträge. */
internal fun KnotenAuswertungsKontext.entfalteMehrfachEingänge(): KnotenAuswertungsKontext {
    val gruppen = mehrfachWerte()
    if (gruppen.isEmpty()) return this
    val werte = linkedMapOf<String, BedingterWert>()
    val ports = knoten.anschlüsse.flatMap { port ->
        if (port.name !in gruppen || port.richtung != AnschlussRichtung.Eingang) {
            eingänge[port.name]?.let { werte[port.name] = it }
            listOf(port)
        } else {
            val beiträge = gruppen.getValue(port.name)
            require(beiträge.isNotEmpty()) { "Mindestens ein Operand muss verbunden oder lokal eingetragen sein." }
            beiträge.mapIndexed { index, wert ->
                val name = "join.$index"
                werte[name] = wert
                port.copy(id = AnschlussId(port.id.wert + ":" + index), name = name,
                    reihenfolge = index, kannSichErweitern = true, dynamischErzeugt = false,
                    belegung = AnschlussBelegung.Einzeln)
            }
        }
    }
    return copy(knoten = knoten.copy(anschlüsse = ports), eingänge = werte, mehrfachEingänge = gruppen)
}
