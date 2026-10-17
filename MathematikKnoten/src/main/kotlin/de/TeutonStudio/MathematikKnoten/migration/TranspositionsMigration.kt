package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Historische Vektortranspositionen erhalten sämtliche Ports und strukturierten Referenzen. */
internal fun migriereTranspositionsKnoten(karte: KartenDaten): KartenDaten = karte.migriereHistorischeKnotenRollen { alt ->
    val spalte = when (alt.art) {
        "mathematik.transponiereSpalte" -> true
        "mathematik.transponiereZeile" -> false
        "mathematik.transponiereMatrix" -> return@migriereHistorischeKnotenRollen RechnerEinzeloperationMigration.migriereGeprüft(alt)
        else -> return@migriereHistorischeKnotenRollen alt
    }
    prüfeHistorischeAnschlussForm(alt)
    require(alt.anschlüsse.size == 2 && alt.anschlüsse.all { it.name == "vektor" }) { "Historische Transpositionsrollen sind nicht eindeutig." }
    val eingang = alt.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
    val ausgang = alt.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
    prüfeHistorischenPortTyp(eingang, setOf(if (spalte) MathematikAnschlussArten.SpaltenVektor.id else MathematikAnschlussArten.ZeilenVektor.id))
    prüfeHistorischenPortTyp(ausgang, setOf(if (spalte) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id))
    require(alt.parameter["achsenPermutation"] in setOf(null, "1,0")) { "Die gespeicherte Permutation widerspricht der Vektortransposition." }
    alt.copy(art = "mathematik.transponieren", anschlüsse = alt.anschlüsse.map { it.copy(name = "wert") },
        parameter = alt.parameter + ("achsenPermutation" to "1,0"))
}
