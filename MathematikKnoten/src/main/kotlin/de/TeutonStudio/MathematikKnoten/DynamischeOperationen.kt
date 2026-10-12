package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

fun istDynamischeOperation(knoten: KnotenDaten): Boolean {
    val familie = RechnerOperatorRegistry.familieFür(knoten.art)
    if (familie != null) {
        val id = knoten.parameter[familie.operatorParameter] ?: knoten.parameter["operator"]
        return id != null && istAssoziativerRechnerOperator(id)
    }
    return knoten.art == MengenRechner.KNOTEN_ART ||
        knoten.art == AussagenSatzRechner.KNOTEN_ART && istAssoziativerRechnerOperator(knoten.parameter["operator"].orEmpty())
}

/** Bei unverändertem Operator bleiben existierende Operanden und ihre Reihenfolge verbindlich. */
internal fun erhalteDynamischeOperanden(knoten: KnotenDaten, neu: List<AnschlussDaten>, gleicherOperator: Boolean): List<AnschlussDaten> {
    if (!gleicherOperator || !istDynamischeOperation(knoten) || neu.none { it.kannSichErweitern }) return neu
    val alt = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.kannSichErweitern }
    if (alt.isEmpty()) return neu
    val basis = neu.first { it.kannSichErweitern }
    val alteIds = alt.map { it.id }.toSet()
    val alteNamen = alt.map { it.name }.toSet()
    val geordnet = alt.map { port ->
        val standard = neu.firstOrNull { it.id == port.id || it.richtung == port.richtung && it.name == port.name } ?: basis
        standard.copy(id = port.id, name = port.name, reihenfolge = port.reihenfolge,
            dynamischErzeugt = port.dynamischErzeugt, kannSichErweitern = true)
    }
    val zusätzliche = neu.filter { it.kannSichErweitern && it.id !in alteIds && it.name !in alteNamen }
        .mapIndexed { index, port -> port.copy(reihenfolge = (alt.maxOfOrNull { it.reihenfolge } ?: -1) + index + 1) }
    return geordnet + zusätzliche + neu.filterNot { it.id in alteIds || it.richtung == AnschlussRichtung.Eingang && it.name in alteNamen || it.kannSichErweitern }
}

/** Nur eine freie Schlussreserve darf fehlen; feste/innere Operanden werden diagnostiziert. */
internal fun geordneteOperationsWerte(kontext: KnotenAuswertungsKontext): LinkedHashMap<String, MathematischesObjekt> {
    val eingänge = kontext.knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    val ports = eingänge.filter { it.kannSichErweitern }.ifEmpty { eingänge }.sortedBy { it.reihenfolge }
    val letzter = ports.indexOfLast { it.name in kontext.eingänge }
    return linkedMapOf<String, MathematischesObjekt>().apply {
        ports.forEachIndexed { index, port ->
            val wert = kontext.eingänge[port.name]
            if (wert == null) require(port.dynamischErzeugt && index > letzter) {
                "Operand ${index + 1} ('${port.name}') fehlt oder ist nicht auswertbar."
            } else put(port.name, wert.mathematischesObjekt("Operand ${index + 1} ('${port.name}')"))
        }
        require(size >= 2) { "Die Operation benötigt mindestens zwei Operanden." }
    }
}

/** Neue Erweiterbarkeit auch für bereits gespeicherte binäre Rechner; keine Instanz wird neu erzeugt. */
fun KartenDaten.normalisiereDynamischeRechnerOperanden(): KartenDaten = copy(knoten = knoten.map { knoten ->
    val familie = RechnerOperatorRegistry.familieFür(knoten.art)
    val id = knoten.parameter[familie?.operatorParameter ?: "operator"] ?: knoten.parameter["operator"]
    if (id == null || !istAssoziativerRechnerOperator(id)) knoten else {
        val rollen = if (knoten.art == AussagenSatzRechner.KNOTEN_ART)
            StrukturRechnerOperatoren.finde(StrukturRechnerKnotenFamilie.AUSSAGESATZ, id).eingänge.map { it.name }.toSet()
        else emptySet()
        knoten.copy(anschlüsse = knoten.anschlüsse.map { port ->
            if (port.richtung == AnschlussRichtung.Eingang &&
                (rollen.isEmpty() || port.name in rollen || port.dynamischErzeugt || port.kannSichErweitern)) port.copy(kannSichErweitern = true) else port
        })
    }
})

/** Appkonventionen bleiben außerhalb des fachneutralen Editors. */
fun belegteOperationsEingänge(knoten: KnotenDaten): Set<AnschlussId> = knoten.anschlüsse.filter {
    it.name in knoten.eingangsKartenVerweise || !knoten.parameter["standardwert.${it.name}"].isNullOrBlank()
}.mapTo(mutableSetOf()) { it.id }
