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
/** Gemeinsamer Datenerhalt für bestehende historische Umsetzungen; keine Auswerterregistrierung. */
internal fun KartenDaten.migriereHistorischeKnotenRollen(umbau: (KnotenDaten) -> KnotenDaten): KartenDaten {
    val rollen = mutableMapOf<KnotenId,Map<String,String>>()
    val neu = knoten.map { alt ->
        if (RECHNER_MIGRATION_DIAGNOSE in alt.parameter) return@map alt
        runCatching {
            val kandidat = umbau(alt)
            if (kandidat == alt || RECHNER_MIGRATION_DIAGNOSE in kandidat.parameter) return@runCatching kandidat
            require(alt.anschlüsse.all {p -> kandidat.anschlüsse.any {it.id == p.id && it.richtung == p.richtung}}) { "Anschlussidentitäten würden verloren gehen." }
            val eingänge = alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.associate {p -> p.name to kandidat.anschlüsse.single {it.id==p.id}.name}
            val ausgänge = alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Ausgang}.associate {p -> p.name to kandidat.anschlüsse.single {it.id==p.id}.name}
            require(eingänge.values.distinct().size==eingänge.size && ausgänge.values.distinct().size==ausgänge.size) { "Anschlussrollen würden zusammenfallen." }
            val ergebnis=kandidat.copy(name=alt.name, kartenVerweis=alt.kartenVerweis,
                parameter=remappeStrukturParameter(kandidat.parameter,eingänge,ausgänge),
                eingangsKartenVerweise=remappeEindeutig(alt.eingangsKartenVerweise) {remappeFallbackRolle(it,eingänge)},
                anschlüsse=kandidat.anschlüsse.map {p -> p.remappeStrukturRollen(eingänge)})
            rollen[alt.id]=eingänge
            ergebnis
        }.getOrElse {fehler -> alt.copy(parameter=alt.parameter+(RECHNER_MIGRATION_DIAGNOSE to "${alt.art} bleibt erhalten: ${fehler.message}"))}
    }
    return copy(knoten=neu,verbindungen=verbindungen.map {v -> rollen[v.zu.knotenId]?.let {r ->
        v.copy(eingangsFallback=v.eingangsFallback?.let {remappeFallbackRolle(it,r)})} ?: v})
}

internal fun prüfeHistorischeAnschlussForm(k: KnotenDaten) {
    require(k.anschlüsse.none {it.richtung==AnschlussRichtung.Neutral}) {"Neutrale Anschlussrollen sind nicht eindeutig."}
    require(k.anschlüsse.map {it.id}.distinct().size==k.anschlüsse.size) {"Anschluss-IDs sind mehrfach vorhanden."}
    require(k.anschlüsse.groupBy {it.richtung to it.name}.all {it.value.size==1}) {"Anschlussrollen sind mehrfach vorhanden."}
    require(k.anschlüsse.count {it.richtung==AnschlussRichtung.Ausgang}==1) {"Genau ein historischer Ausgang ist erforderlich."}
    val arten=MathematikAnschlussArten.alle.map {it.id}.toSet()
    require(k.anschlüsse.all {it.art in arten}) {"Ein historischer Anschlusstyp ist unbekannt."}
}

internal fun prüfeHistorischenPortTyp(p: AnschlussDaten, erlaubt: Set<AnschlussArtId>, variadisch: Boolean = false) {
    val register=de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister(MathematikAnschlussArten.alle)
    fun passt(a:AnschlussArtId)=erlaubt.any {register.istUnterart(a,it)}
    val arten=if(p.art in setOf(MathematikAnschlussArten.AtlasWert.id,MathematikAnschlussArten.Objekt.id) && p.zulässigeArten.isNotEmpty()) p.zulässigeArten else setOf(p.art)+p.zulässigeArten
    require(arten.all(::passt)) {"Die Anschlussart von '${p.name}' widerspricht dem historischen Fachvertrag."}
    val ids=erlaubt.map {if(it==MathematikAnschlussArten.Tupel.id) de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen.Tupel else de.TeutonStudio.TypSystem.TypId(it.wert)}
    fun typPasst(t:de.TeutonStudio.TypSystem.TypAusdruck):Boolean=when(t) {
        de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt -> true
        is de.TeutonStudio.TypSystem.TypAusdruck.Atom -> ids.any {de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen.istAtomUntertyp(t.id,it)}
        is de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert -> ids.any {de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen.istAtomUntertyp(t.konstruktor,it)}
        is de.TeutonStudio.TypSystem.TypAusdruck.Vereinigung -> t.alternativen.all(::typPasst)
        else -> false
    }
    require(typPasst(p.vertrag.typ)) {"Der semantische Typ von '${p.name}' widerspricht dem historischen Fachvertrag."}
    require(variadisch || p.belegung==AnschlussBelegung.Einzeln && !p.kannSichErweitern && !p.dynamischErzeugt && p.sammelAnschluss==null) {"Der historische feste Anschluss '${p.name}' besitzt einen variadischen Vertrag."}
}
