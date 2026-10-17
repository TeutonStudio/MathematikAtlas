package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.TypSystem.TypInferenzRegel

internal fun <T> remappeEindeutig(werte:Map<String,T>,name:(String)->String):Map<String,T> {
    val paare=werte.map {(k,v)->name(k) to v}
    require(paare.map {it.first}.distinct().size==paare.size) { "Gespeicherte Rollen kollidieren nach der Umsetzung." }
    return paare.toMap()
}
internal fun remappeFallbackRolle(rolle:String,rollen:Map<String,String>):String {
    val basis=rolle.substringBefore("@karte:")
    return (rollen[basis] ?: basis)+rolle.removePrefix(basis)
}
internal fun remappeStrukturParameter(parameter:Map<String,String>,eingänge:Map<String,String>,ausgänge:Map<String,String>) =
    remappeEindeutig(parameter) { key ->
        val präfix=listOf("standardwert.","methodenAnforderung.","methodenAusgang.").firstOrNull {key.startsWith(it)}
        if(präfix==null) key else {
            val rest=key.removePrefix(präfix);val rolle=rest.substringBefore('.')
            präfix+((if(präfix=="methodenAusgang.") ausgänge else eingänge)[rolle] ?: rolle)+rest.removePrefix(rolle)
        }
    }
internal fun AnschlussDaten.remappeStrukturRollen(rollen:Map<String,String>):AnschlussDaten {
    fun r(n:String)=rollen[n] ?: n
    return copy(artFolgtEingang=artFolgtEingang?.let(::r),artVereinigtEingänge=artVereinigtEingänge.map(::r),
        artAbbildungVonEingang=artAbbildungVonEingang?.let {it.copy(eingang=r(it.eingang))},
        artPriorisiertEingänge=artPriorisiertEingänge?.let {it.copy(eingänge=it.eingänge.map(::r))},
        typInferenz=when(val t=typInferenz) {
            is TypInferenzRegel.FolgtEingang -> t.copy(eingang=r(t.eingang))
            is TypInferenzRegel.GemeinsameOberart -> t.copy(eingänge=t.eingänge.map(::r))
            is TypInferenzRegel.Vereinigung -> t.copy(eingänge=t.eingänge.map(::r))
            is TypInferenzRegel.TupelAus -> t.copy(eingänge=t.eingänge.map(::r))
            is TypInferenzRegel.KomponenteVonTupel -> t.copy(eingang=r(t.eingang))
            is TypInferenzRegel.AbbildungVonEingang -> t.copy(eingang=r(t.eingang))
            is TypInferenzRegel.Priorisierung -> t.copy(eingänge=t.eingänge.map(::r))
            null -> null
        })
}
