package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Verlorene Graphrollen werden erst in der gemeinsamen, bestätigbaren Vorschau bereinigt. */
internal fun bereiteEingangsRollenWechselVor(wechsel: JoinWechsel): JoinWechsel {
    val kandidat=wechsel.kandidat
    if(kandidat.art!=MENGEN_KNOTEN_ART && kandidat.art!=de.TeutonStudio.MathematikRechenSystem.kern.MengenRechner.KNOTEN_ART && kandidat.art!=ENDLICHE_MENGE_ART && !(kandidat.art==ZAHLENRECHNER_ART && kandidat.parameter[ZAHLENRECHNER_OPERATOR]==ErweiterterZahlenOperator.POLYNOM.stabileId)) return wechsel
    val alt=wechsel.karte.knoten.singleOrNull {it.id==kandidat.id} ?: return wechsel
    val neuRollen=kandidat.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.map {it.name}.toSet()
    val entfernt=alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang && (it.name !in neuRollen || kandidat.art in setOf(MENGEN_KNOTEN_ART, de.TeutonStudio.MathematikRechenSystem.kern.MengenRechner.KNOTEN_ART) &&
        kandidat.anschlüsse.singleOrNull { neu -> neu.richtung == AnschlussRichtung.Eingang && neu.name == it.name }?.let { neu -> neu.art != it.art || neu.zulässigeArten != it.zulässigeArten } == true)}.map {it.name}.toSet()
    if(entfernt.isEmpty()) return wechsel
    fun standard(key:String)=key.startsWith("standardwert.") && key.removePrefix("standardwert.").substringBefore('.') in entfernt
    fun fallback(key:String)=key.substringBefore("@karte:") in entfernt
    val verluste=(alt.parameter+kandidat.parameter).filterKeys(::standard).map {(key,value)->"Standardwert ${key.removePrefix("standardwert.")}: $value"}+
        (alt.eingangsKartenVerweise+kandidat.eingangsKartenVerweise).filterKeys(::fallback).map {(key,value)->"Kartenfallback $key: ${value.kartenId.wert} v${value.version}"}
    return wechsel.copy(kandidat=kandidat.copy(parameter=kandidat.parameter.filterKeys {!standard(it)},
        eingangsKartenVerweise=kandidat.eingangsKartenVerweise.filterKeys {!fallback(it)}),
        entfernteLokale=wechsel.entfernteLokale+verluste)
}
