package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Verlorene Graphrollen werden erst in der gemeinsamen, bestätigbaren Vorschau bereinigt. */
internal fun bereiteEndlicheMengeWechselVor(wechsel: JoinWechsel): JoinWechsel {
    val kandidat=wechsel.kandidat
    if(kandidat.art!=ENDLICHE_MENGE_ART) return wechsel
    val alt=wechsel.karte.knoten.singleOrNull {it.id==kandidat.id} ?: return wechsel
    val neuRollen=kandidat.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.map {it.name}.toSet()
    val entfernt=alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang && it.name !in neuRollen}.map {it.name}.toSet()
    if(entfernt.isEmpty()) return wechsel
    fun standard(key:String)=key.startsWith("standardwert.") && key.removePrefix("standardwert.").substringBefore('.') in entfernt
    fun fallback(key:String)=key.substringBefore("@karte:") in entfernt
    val verluste=(alt.parameter+kandidat.parameter).filterKeys(::standard).map {(key,value)->"Standardwert ${key.removePrefix("standardwert.")}: $value"}+
        (alt.eingangsKartenVerweise+kandidat.eingangsKartenVerweise).filterKeys(::fallback).map {(key,value)->"Kartenfallback $key: ${value.kartenId.wert} v${value.version}"}
    return wechsel.copy(kandidat=kandidat.copy(parameter=kandidat.parameter.filterKeys {!standard(it)},
        eingangsKartenVerweise=kandidat.eingangsKartenVerweise.filterKeys {!fallback(it)}),
        entfernteLokale=wechsel.entfernteLokale+verluste)
}
