package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten

/** Migriert ausschließlich den historischen Standardnamen der Prädikatvariante. */
fun KartenDaten.migrierePraedikatStandardname(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        val istPraedikatVariante = knoten.art == "mathematik.termZuMethode" &&
            knoten.anschlüsse.any {
                it.richtung == AnschlussRichtung.Eingang &&
                    it.name == "term" &&
                    it.art == MathematikAnschlussArten.Aussage.id
            } &&
            knoten.anschlüsse.any {
                it.richtung == AnschlussRichtung.Ausgang &&
                    it.name == "methode" &&
                    it.art == MathematikAnschlussArten.AussageMethode.id
            }
        if (istPraedikatVariante && knoten.name == "Aussage zu Methode") {
            knoten.copy(name = "Aussage zu Prädikat")
        } else knoten
    },
)
