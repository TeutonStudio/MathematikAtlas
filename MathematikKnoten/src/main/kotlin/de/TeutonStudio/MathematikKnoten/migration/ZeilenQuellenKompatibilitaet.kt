package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Historische Zeilenquellen verwenden dieselben konfigurierten Quellen wie Spalten. */
fun KartenDaten.migriereZeilenQuellen(): KartenDaten = migriereHistorischeKnotenRollen { alt ->
    if (alt.art !in setOf("mathematik.zeilenVektor", "mathematik.einheitsZeile")) return@migriereHistorischeKnotenRollen alt
    prüfeHistorischeAnschlussForm(alt)
    val orientierung=alt.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: VEKTOR_ORIENTIERUNG_ZEILE
    require(orientierung in setOf(VEKTOR_ORIENTIERUNG_ZEILE,VEKTOR_ORIENTIERUNG_SPALTE)) { "Die historische Zeilenquelle besitzt eine widersprüchliche Orientierung." }
    val ausgang=alt.anschlüsse.single { it.richtung==AnschlussRichtung.Ausgang }
    require(ausgang.name=="vektor") { "Unbekannter Ausgang der Zeilenquelle." }
    prüfeHistorischenPortTyp(ausgang,setOf(if(orientierung==VEKTOR_ORIENTIERUNG_ZEILE) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id))
    val eingänge=alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}
    val ziel=if(alt.art=="mathematik.einheitsZeile") {
        // Der vorangegangene historische Loader hat index bereits nullbasiert in position überführt.
        require(eingänge.map {it.name}.toSet()==setOf(EINHEITSVEKTOR_POSITION,EINHEITSVEKTOR_DIMENSION)) { "Unvollständige Basisvektorrollen." }
        eingänge.forEach { prüfeHistorischenPortTyp(it,setOf(MathematikAnschlussArten.Zahl.id)) }
        "mathematik.einheitsSpalte"
    } else {
        val modus=alt.parameter[VEKTOR_ERZEUGUNGSART_PARAMETER] ?: VEKTOR_EINZEL_EINGABEN
        require(modus in setOf(VEKTOR_EINZEL_EINGABEN,VEKTOR_METHODE)) { "Unbekannte Erzeugungsart der Zeilenquelle." }
        if(modus==VEKTOR_METHODE) {
            require(eingänge.map {it.name}.toSet()==setOf("dimension","methode")) { "Unvollständige Methodenrollen der Zeilenquelle." }
            eingänge.forEach { prüfeHistorischenPortTyp(it,setOf(if(it.name=="methode") MathematikAnschlussArten.Methode.id else MathematikAnschlussArten.Zahl.id)) }
        } else {
            val anzahl=alt.parameter["festeEingänge"]?.let {it.toIntOrNull() ?: error("Ungültige Anzahl der Zeilenkomponenten.")} ?: 2
            require(anzahl>=2 && eingänge.size>=anzahl) { "Anzahl und Komponentenrollen der Zeilenquelle widersprechen einander." }
            require(eingänge.map {it.name}.toSet()==eingänge.indices.map {when(it){0->"a";1->"b";else->"input${it+1}"}}.toSet()) { "Unbekannte Komponentenrollen der Zeilenquelle." }
            eingänge.sortedBy {it.reihenfolge}.forEachIndexed {index,p ->
                require(index<anzahl || p.dynamischErzeugt) { "Zusätzliche Komponenten müssen als dynamische Slots gekennzeichnet sein." }
                prüfeHistorischenPortTyp(p,setOf(MathematikAnschlussArten.Zahl.id),variadisch=true)
            }
        }
        VEKTOR_KONSTRUKTOR_ART
    }
    alt.copy(art=ziel,parameter=alt.parameter+(VEKTOR_ORIENTIERUNG_PARAMETER to orientierung))
}
