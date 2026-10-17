package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*

/** Nur für punktweise Altvertrags-Fixtures. Graph- und Codecverträge testen stets die ganze Karte. */
internal fun MathematikAuswerterRegister.findeNachMigration(art: String): MathematikKnotenAuswerter = MathematikKnotenAuswerter { alt ->
    require(alt.knoten.art==art)
    finde(art)?.let { return@MathematikKnotenAuswerter it.auswerten(alt) }
    val karte=MathematikKartenCodec.lade(KartenDatenJson.schreibe(KartenDaten(name="Altfixture",knoten=listOf(alt.knoten))))
    val neu=karte.knoten.single()
    val diagnose=auswertungssperre(neu)
    if(diagnose!=null) return@MathematikKnotenAuswerter KnotenAuswertungsErgebnis(emptyMap(),fehler=diagnose,eingänge=alt.eingänge)
    val rollen=alt.knoten.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.associate {p -> p.name to (neu.anschlüsse.singleOrNull {it.id==p.id}?.name ?: p.name)}
    val einzeln=alt.eingänge.mapKeys {(name,_) -> rollen[name] ?: name}
    val mehrfach=alt.mehrfachEingänge.mapKeys {(name,_) -> rollen[name] ?: name}.toMutableMap()
    val join=neu.anschlüsse.singleOrNull {it.richtung==AnschlussRichtung.Eingang && it.belegung==AnschlussBelegung.Mehrfach}
    val inputs=if(join!=null && join.name !in mehrfach) {
        val werte=alt.knoten.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.sortedBy {it.reihenfolge}.mapNotNull {alt.eingänge[it.name]}
        if(werte.isNotEmpty()) mehrfach[join.name]=werte
        einzeln
    } else einzeln
    val r=GesamterMathematikAuswerter.erzeugeRegister().finde(neu.art)?.auswerten(alt.copy(knoten=neu,eingänge=inputs,mehrfachEingänge=mehrfach))
        ?: error("Kanonischer Auswerter ${neu.art} fehlt nach der Migration.")
    val ausgangsRollen=alt.knoten.anschlüsse.filter {it.richtung==AnschlussRichtung.Ausgang}.associate {p -> neu.anschlüsse.single {it.id==p.id}.name to p.name}
    r.copy(ausgaben=r.ausgaben.mapKeys {(name,_) -> ausgangsRollen[name] ?: name})
}

internal fun KartenAuswerter.auswertenNachMigration(karte: KartenDaten, vorgaben: Map<KnotenId,Map<String,BedingterWert>> = emptyMap()): KartenAuswertungsErgebnis =
    auswerten(MathematikKartenCodec.lade(KartenDatenJson.schreibe(karte)),vorgaben)

internal fun MathematikKartenLaufzeit.auswertenNachMigration(karte: KartenDaten): KartenAuswertungsErgebnis =
    auswerten(MathematikKartenCodec.lade(KartenDatenJson.schreibe(karte)))
