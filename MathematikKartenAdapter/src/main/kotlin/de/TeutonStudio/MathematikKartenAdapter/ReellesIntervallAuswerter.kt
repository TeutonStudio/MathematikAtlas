package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * Priorisierte Auswertung des überarbeiteten Intervallknotens.
 *
 * Sie liegt vorläufig im Adapter, damit parallele Arbeiten am großen
 * Standardauswerter keinen unnötigen Mergekonflikt erzeugen. Nach der
 * Integration der laufenden Releasebranches kann sie ohne Verhaltensänderung
 * in dessen Registryblock verschoben werden.
 */
internal object ReellesIntervallAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val linksWert = kontext.eingänge["links"] ?: error("Zahleingang links fehlt.")
        val rechtsWert = kontext.eingänge["rechts"] ?: error("Zahleingang rechts fehlt.")
        val links = linksWert.objekt as? ZahlAusdruck ?: error("Der Eingang links enthält keine Zahl.")
        val rechts = rechtsWert.objekt as? ZahlAusdruck ?: error("Der Eingang rechts enthält keine Zahl.")

        require(linksWert.istNachweisbarReell() && rechtsWert.istNachweisbarReell()) {
            "Ein reelles Intervall benötigt zwei nachweisbar reelle Grenzen."
        }

        val linksOffen = kontext.entscheideOffenheit("linksOffen", "links offen?")
        val rechtsOffen = kontext.entscheideOffenheit("rechtsOffen", "rechts offen?")
        val eingangswerte = kontext.eingänge.values

        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "menge" to BedingterWert(
                    objekt = reellesIntervall(
                        links = links,
                        linksOffen = linksOffen,
                        rechts = rechts,
                        rechtsOffen = rechtsOffen,
                        kontext = kontext.rechenKontext,
                    ),
                    annahmen = eingangswerte.flatMap { it.annahmen }.toSet(),
                    reelleVariablen = reelleVariablen(eingangswerte),
                    variablenQuellen = eingangswerte.flatMap { it.variablenQuellen }
                        .distinctBy { quelle -> Pair(Triple(quelle.knotenId, quelle.name, quelle.werteVorrat), quelle.alsMethodenParameter) },
                ),
            ),
        )
    }

    private fun KnotenAuswertungsKontext.entscheideOffenheit(
        anschluss: String,
        bezeichnung: String,
    ): Boolean {
        val wert = eingänge[anschluss] ?: return false
        val aussage = wert.objekt as? Aussage
            ?: error("Der Eingang „$bezeichnung“ enthält keine Aussage.")
        val ergebnis = aussage.entscheide(rechenKontext)
        return when (ergebnis.wahrheitswert) {
            Wahrheitswert.Wahr -> true
            Wahrheitswert.Lüge -> false
            null -> {
                val begründung = ergebnis.begründung.trim()
                error(
                    "Die Aussage am Eingang „$bezeichnung“ konnte nicht entschieden werden." +
                        if (begründung.isBlank()) "" else " $begründung",
                )
            }
        }
    }
}
