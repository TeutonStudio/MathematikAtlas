package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Knoten, die nach v2.3.2 ergänzend zum historischen Vorlagenobjekt registriert werden. */
object ErweiterteMathematikKnotenVorlagen {
    val Subtraktion = KnotenVorlage(
        art = "mathematik.subtraktion",
        name = "Subtraktion",
        kategorie = "Rechnen",
        beschreibung = "Bildet die Differenz aus Minuend und Subtrahend.",
        standardGröße = GraphGröße(230f, 115f),
        anschlüsse = listOf(
            AnschlussDaten(name = "minuend", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 0),
            AnschlussDaten(name = "subtrahend", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 1),
            AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id),
        ),
    )

    val alle: List<KnotenVorlage> = listOf(Subtraktion)
}
