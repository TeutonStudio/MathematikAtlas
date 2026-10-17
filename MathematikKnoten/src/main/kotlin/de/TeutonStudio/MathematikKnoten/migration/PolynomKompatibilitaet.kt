package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Der alte intrinsische Indeterminatenvertrag wird ausdrücklich im bestehenden Operator konfiguriert. */
fun KartenDaten.migrierePolynomKompatibilität(): KartenDaten = migriereHistorischeKnotenRollen { alt ->
    if(alt.art==ZAHLENRECHNER_ART && alt.parameter[ZAHLENRECHNER_OPERATOR]==ErweiterterZahlenOperator.POLYNOM.stabileId) {
        prüfeHistorischeAnschlussForm(alt)
        val modus=polynomArgumentModus(alt)
        if(modus==POLYNOM_ARGUMENT_VARIABLE) polynomVariablenName(alt)
        val rollen=alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.map {it.name}.toSet()
        require(rollen==if(modus==POLYNOM_ARGUMENT_VARIABLE) setOf("koeffizienten") else setOf("koeffizienten","argument")) {"$POLYNOM_KONFIG_DIAGNOSE Die gespeicherten Eingangsrollen widersprechen dem Argumentmodus."}
        alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.forEach {p ->
            prüfeHistorischenPortTyp(p,if(p.name=="koeffizienten") setOf(MathematikAnschlussArten.Tupel.id,MathematikAnschlussArten.Vektor.id) else setOf(MathematikAnschlussArten.Zahl.id,MathematikAnschlussArten.Methode.id))
        }
        val ausgang=alt.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
        prüfeHistorischenPortTyp(ausgang,if(modus==POLYNOM_ARGUMENT_VARIABLE) setOf(MathematikAnschlussArten.Zahl.id) else setOf(MathematikAnschlussArten.Zahl.id,MathematikAnschlussArten.Methode.id))
        require(ausgang.name=="wert") {"$POLYNOM_KONFIG_DIAGNOSE Unbekannte Ausgangsrolle."}
        return@migriereHistorischeKnotenRollen alt
    }
    if(alt.art!="mathematik.vektorZuPolynom") return@migriereHistorischeKnotenRollen alt
    prüfeHistorischeAnschlussForm(alt)
    require(alt.anschlüsse.size==2) {"Historische Polynomrollen sind nicht eindeutig."}
    val eingang=alt.anschlüsse.single {it.richtung==AnschlussRichtung.Eingang}
    val ausgang=alt.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
    require(eingang.name=="vektor" && ausgang.name=="wert") {"Unbekannte historische Polynomrollen."}
    prüfeHistorischenPortTyp(eingang,setOf(MathematikAnschlussArten.Vektor.id))
    prüfeHistorischenPortTyp(ausgang,setOf(MathematikAnschlussArten.Zahl.id))
    val name=polynomVariablenName(alt)
    require(alt.parameter[POLYNOM_ARGUMENT_MODUS] in setOf(null,POLYNOM_ARGUMENT_VARIABLE)) {"$POLYNOM_KONFIG_DIAGNOSE Widersprüchlicher historischer Argumentmodus."}
    alt.copy(art=ZAHLENRECHNER_ART,anschlüsse=listOf(eingang.copy(name="koeffizienten"),ausgang),
        parameter=alt.parameter+mapOf(ZAHLENRECHNER_OPERATOR to ErweiterterZahlenOperator.POLYNOM.stabileId,
            POLYNOM_ARGUMENT_MODUS to POLYNOM_ARGUMENT_VARIABLE,ZAHLENRECHNER_VARIABLE to name))
}
