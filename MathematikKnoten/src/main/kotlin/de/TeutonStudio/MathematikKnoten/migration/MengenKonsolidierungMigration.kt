package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal val mengenKonsolidierungAltarten = setOf("mathematik.einzelmenge", "mathematik.potenzmenge", "mathematik.abbildungsmenge")

/** Eindeutige Rollen werden vor jeder allgemeinen Anschlussnormalisierung umgesetzt. */
fun KartenDaten.migriereMengenKonsolidierung(): KartenDaten {
    val rollen = mutableMapOf<KnotenId, Map<String, String>>()
    val register = AnschlussArtRegister(MathematikAnschlussArten.alle + GeometrieAnschlussArten.alle)
    val neu = knoten.map { alt ->
        if (alt.art !in mengenKonsolidierungAltarten || RECHNER_MIGRATION_DIAGNOSE in alt.parameter) return@map alt
        runCatching {
            val einzel = alt.art == "mathematik.einzelmenge"
            val eingänge = when(alt.art) {
                "mathematik.einzelmenge" -> mapOf("element" to "element1")
                "mathematik.potenzmenge" -> mapOf("grundmenge" to "a")
                else -> mapOf("zielmenge" to "zielmenge", "definitionsmenge" to "argumentmenge")
            }
            val ausgänge = mapOf("menge" to "menge")
            val inputs = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
            val outputs = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }
            require(inputs.map {it.name}.toSet()==eingänge.keys && inputs.size==eingänge.size && outputs.size==1 && outputs.single().name=="menge") { "Die gespeicherten Anschlussrollen sind nicht eindeutig." }
            require(alt.anschlüsse.size==inputs.size+1 && alt.anschlüsse.map {it.id}.distinct().size==alt.anschlüsse.size && knoten.count {it.id==alt.id}==1) { "Knoten- oder Anschlussidentitäten sind mehrdeutig." }
            require(alt.anschlüsse.all { it.belegung==AnschlussBelegung.Einzeln && !it.kannSichErweitern && it.sammelAnschluss==null } && inputs.all {p -> verbindungen.count {it.zu==AnschlussVerweis(alt.id,p.id)}<=1}) { "Die Einzelbelegung ist nicht erhaltbar." }
            for(p in alt.anschlüsse) {
                val art=if(einzel && p.richtung==AnschlussRichtung.Eingang) MathematikAnschlussArten.Objekt.id else MathematikAnschlussArten.Menge.id
                val singletonEingang=einzel && p.richtung==AnschlussRichtung.Eingang
                fun erlaubt(a:AnschlussArtId)=register.istUnterart(a,art) || singletonEingang && a==MathematikAnschlussArten.Methode.id
                val gültig=if(singletonEingang && p.art==MathematikAnschlussArten.AtlasWert.id)
                    p.zulässigeArten.isNotEmpty() && p.zulässigeArten.all(::erlaubt)
                else (setOf(p.art)+p.zulässigeArten).all(::erlaubt)
                require(gültig) { "Die Anschlussart von '${p.name}' widerspricht der Mengenoperation." }
            }
            val basis=alt.copy(art=if(einzel) ENDLICHE_MENGE_ART else MengenRechner.KNOTEN_ART,
                anschlüsse=alt.anschlüsse.map {p -> p.remappeStrukturRollen(eingänge).copy(name=if(p.richtung==AnschlussRichtung.Eingang) eingänge.getValue(p.name) else p.name)},
                parameter=remappeStrukturParameter(alt.parameter,eingänge,ausgänge),
                eingangsKartenVerweise=remappeEindeutig(alt.eingangsKartenVerweise) {remappeFallbackRolle(it,eingänge)})
            val ziel=if(einzel) konfiguriereEndlicheMenge(basis,ENDLICHE_MENGE_GRAPH,1) else konfiguriereMengenRechner(basis,
                if(alt.art=="mathematik.potenzmenge") MengenRechnerOperator.POTENZMENGE else MengenRechnerOperator.ABBILDUNGSMENGE)
            require(ziel.anschlüsse.map {it.id}.toSet()==alt.anschlüsse.map {it.id}.toSet()) { "Die Anschlussidentitäten sind nicht vollständig erhaltbar." }
            val erhalten = ziel.copy(name=alt.name, anschlüsse=basis.anschlüsse.map {if(einzel && it.richtung==AnschlussRichtung.Eingang) it.alsGraphElementEingang() else it})
            val normalisiert = if(einzel) erhalten else normalisiereRechnerMethodenAnschluesse(erhalten)
            val alterAusgang = basis.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
            require(alterAusgang.artPriorisiertEingänge == null || alterAusgang.artPriorisiertEingänge == normalisiert.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}.artPriorisiertEingänge) {
                "Die gespeicherte Ausgangspriorisierung ist nicht eindeutig im Methodenvertrag erhaltbar."
            }
            rollen[alt.id]=eingänge
            normalisiert
        }.getOrElse {fehler -> alt.copy(parameter=alt.parameter+(RECHNER_MIGRATION_DIAGNOSE to "${alt.art} bleibt erhalten: ${fehler.message}"))}
    }
    return copy(knoten=neu,verbindungen=verbindungen.map {v ->
        val map=rollen[v.zu.knotenId]
        if(map==null || v.eingangsFallback==null) v else v.copy(eingangsFallback=remappeFallbackRolle(requireNotNull(v.eingangsFallback),map))
    })
}
