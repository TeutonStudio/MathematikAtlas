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
            eingang("minuend", MathematikAnschlussArten.Zahl.id, 0),
            eingang("subtrahend", MathematikAnschlussArten.Zahl.id, 1),
            ausgang("wert", MathematikAnschlussArten.Zahl.id),
        ),
    )

    val ReelleMethodenSumme = KnotenVorlage(
        art = "mathematik.reelleMethodenSumme",
        name = "Reelle Methodensumme",
        kategorie = "Analysis: Approximation",
        beschreibung = "Berechnet Ober- oder Untersumme einer reellen einstelligen Methode über einem Intervall.",
        standardGröße = GraphGröße(520f, 330f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Funktion.id, 0),
            eingang("partitionen", MathematikAnschlussArten.Zahl.id, 1),
            eingang("minimum", MathematikAnschlussArten.Zahl.id, 2),
            eingang("maximum", MathematikAnschlussArten.Zahl.id, 3),
            eingang("intervall", MathematikAnschlussArten.Menge.id, 4),
            ausgang("wert", MathematikAnschlussArten.Zahl.id),
        ),
        standardParameter = mapOf("summenArt" to "untersumme", "bereichsArt" to "grenzen"),
    )

    val DreieckRechner = KnotenVorlage(
        art = "mathematik.geometrie.dreieckRechner",
        name = "Dreieckrechner",
        kategorie = "Geometrie: Dreiecke",
        beschreibung = "Bestimmt Seiten und Winkel eines Dreiecks aus jeder hinreichenden konsistenten Wertekombination.",
        standardGröße = GraphGröße(540f, 360f),
        anschlüsse = listOf(
            eingang("a", MathematikAnschlussArten.Zahl.id, 0),
            eingang("b", MathematikAnschlussArten.Zahl.id, 1),
            eingang("c", MathematikAnschlussArten.Zahl.id, 2),
            eingang("alpha", MathematikAnschlussArten.Zahl.id, 3),
            eingang("beta", MathematikAnschlussArten.Zahl.id, 4),
            eingang("gamma", MathematikAnschlussArten.Zahl.id, 5),
            ausgang("aWert", MathematikAnschlussArten.Zahl.id, 0),
            ausgang("bWert", MathematikAnschlussArten.Zahl.id, 1),
            ausgang("cWert", MathematikAnschlussArten.Zahl.id, 2),
            ausgang("alphaWert", MathematikAnschlussArten.Zahl.id, 3),
            ausgang("betaWert", MathematikAnschlussArten.Zahl.id, 4),
            ausgang("gammaWert", MathematikAnschlussArten.Zahl.id, 5),
            ausgang("gültig", MathematikAnschlussArten.Aussage.id, 6),
            ausgang("bestimmt", MathematikAnschlussArten.Aussage.id, 7),
        ),
    )

    val alle: List<KnotenVorlage> = listOf(Subtraktion, ReelleMethodenSumme, DreieckRechner)

    private fun eingang(name: String, art: AnschlussArtId, reihenfolge: Int) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = reihenfolge,
    )

    private fun ausgang(name: String, art: AnschlussArtId, reihenfolge: Int = 0) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
        reihenfolge = reihenfolge,
    )
}
