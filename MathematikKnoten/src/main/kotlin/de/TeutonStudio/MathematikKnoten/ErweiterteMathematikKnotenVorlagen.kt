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

    val ReelleMethodenSumme = KnotenVorlage(
        art = "mathematik.reelleMethodenSumme",
        name = "Reelle Methodensumme",
        kategorie = "Analysis: Approximation",
        beschreibung = "Berechnet Ober- oder Untersumme einer reellen einstelligen Methode über einem Intervall.",
        standardGröße = GraphGröße(520f, 330f),
        anschlüsse = listOf(
            AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Funktion.id, reihenfolge = 0),
            AnschlussDaten(name = "partitionen", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 1),
            AnschlussDaten(name = "minimum", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 2),
            AnschlussDaten(name = "maximum", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 3),
            AnschlussDaten(name = "intervall", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Menge.id, reihenfolge = 4),
            AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id),
        ),
        parameter = mapOf("summenArt" to "untersumme", "bereichsArt" to "grenzen"),
    )

    val alle: List<KnotenVorlage> = listOf(Subtraktion, ReelleMethodenSumme)
}
