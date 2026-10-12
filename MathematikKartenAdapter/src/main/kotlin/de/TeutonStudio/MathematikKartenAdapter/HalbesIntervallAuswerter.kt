package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Einseitig unbeschränkte reelle Menge mit unabhängig offenem oder geschlossenem Rand. */
internal object HalbesIntervallAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val eingang = kontext.eingänge["grenze"] ?: error("Grenze fehlt.")
        val grenze = eingang.objekt as? ZahlAusdruck ?: error("Grenze ist keine Zahl.")
        require(eingang.istNachweisbarReell()) { "Die Grenze muss nachweisbar reell sein." }
        val offen = kontext.eingänge["offen"]?.let { wert ->
            val aussage = wert.objekt as? Aussage ?: error("Offenheit ist keine Aussage.")
            when (aussage.entscheide(kontext.rechenKontext).wahrheitswert) {
                Wahrheitswert.Wahr -> true
                Wahrheitswert.Lüge -> false
                null -> error("Die Offenheit konnte nicht entschieden werden.")
            }
        } ?: false
        val richtung = kontext.knoten.parameter["richtung"] ?: "aufwaerts"
        require(richtung == "aufwaerts" || richtung == "abwaerts") { "Ungültige Richtung: $richtung" }
        val variable = Variable("x")
        val relation = if (offen) VergleichsArt.Kleiner else VergleichsArt.KleinerGleich
        val bedingung = if (richtung == "aufwaerts") Vergleich(grenze, relation, variable)
                        else Vergleich(variable, relation, grenze)
        val menge = DefinierteMenge(listOf(GebundeneMengenVariable(variable, ReelleZahlen)), bedingung)
        val werte = kontext.eingänge.values
        return KnotenAuswertungsErgebnis(mapOf("menge" to BedingterWert(
            objekt = menge,
            annahmen = werte.flatMap { it.annahmen }.toSet(),
            reelleVariablen = reelleVariablen(werte),
            variablenQuellen = werte.flatMap { it.variablenQuellen }
                .distinctBy { Pair(Triple(it.knotenId, it.name, it.werteVorrat), it.alsMethodenParameter) },
        )))
    }
}
