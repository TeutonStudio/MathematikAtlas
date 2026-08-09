package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage

/** Kanonische produktive Vorlagen des nächsten Konsolidierungsblocks. */
object V230KnotenVorlagen {
    private fun eingang(
        name: String,
        art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
        reihenfolge: Int = 0,
        erweiterbar: Boolean = false,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = reihenfolge,
        kannSichErweitern = erweiterbar,
    )

    private fun ausgang(
        name: String,
        art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )

    val AussageZuPrädikat = KnotenVorlage(
        art = "mathematik.termZuMethode",
        name = "Aussage zu Prädikat",
        kategorie = "Methoden",
        beschreibung = "Erzeugt aus einer Aussage ein typisiertes Prädikat.",
        standardGröße = GraphGröße(265f, 135f),
        anschlüsse = listOf(
            eingang("term", MathematikAnschlussArten.Aussage.id),
            ausgang("methode", MathematikAnschlussArten.AussageMethode.id),
        ),
        standardParameter = mapOf(
            "name" to "P",
            "argumentReihenfolge" to "",
            "suchAlias" to "Aussage zu Methode",
        ),
    )

    val NullDistanz = KnotenVorlage(
        art = NULL_DISTANZ_ART,
        name = "0-Distanz",
        kategorie = "Zahlen",
        beschreibung = "Bestimmt den kanonischen Abstand eines Zahlwerts zum Nullelement; über ℝ, ℂ und ℍ entspricht dies dem jeweiligen Betrag/Radius.",
        standardGröße = GraphGröße(225f, 105f),
        anschlüsse = listOf(
            eingang("zahl", MathematikAnschlussArten.Zahl.id),
            ausgang("wert", MathematikAnschlussArten.Zahl.id),
        ),
        standardParameter = mapOf("operator" to "zahl.betrag"),
    )

    val Vektor = KnotenVorlage(
        art = VEKTOR_ART,
        name = "Vektor",
        kategorie = "Vektoren",
        beschreibung = "Erzeugt einen Zeilen- oder Spaltenvektor aus Einzelwerten oder aus Dimension und Indexmethode.",
        standardGröße = GraphGröße(230f, 125f),
        anschlüsse = listOf(
            eingang("a", MathematikAnschlussArten.Zahl.id, 0, true),
            eingang("b", MathematikAnschlussArten.Zahl.id, 1, true),
            ausgang("vektor", MathematikAnschlussArten.SpaltenVektor.id),
        ),
        standardParameter = mapOf(
            "festeEingänge" to "2",
            "operatorAnzeige" to "wert",
            VEKTOR_ERZEUGUNGS_ART to VEKTOR_EINZEL_EINGABEN,
            VEKTOR_ORIENTIERUNG to VEKTOR_SPALTE,
        ),
    )

    val MultinomVektor = KnotenVorlage(
        art = MULTINOM_VEKTOR_ART,
        name = "Multinomvektor",
        kategorie = "Vektoren",
        beschreibung = "Erzeugt Mₙ(x)=(xᵏ)₀≤k≤n und gibt die Monomfolge als Spaltenvektor, Zeilenvektor oder Tupel aus.",
        standardGröße = GraphGröße(250f, 120f),
        anschlüsse = listOf(
            eingang("x", MathematikAnschlussArten.Zahl.id, 0),
            eingang("dim", MathematikAnschlussArten.Zahl.id, 1),
            ausgang("wert", MathematikAnschlussArten.SpaltenVektor.id),
        ),
        standardParameter = mapOf(
            MULTINOM_AUSGABE_FORM to MULTINOM_AUSGABE_VEKTOR,
            VEKTOR_ORIENTIERUNG to VEKTOR_SPALTE,
        ),
    )

    val alle = listOf(NullDistanz, Vektor, MultinomVektor, AussageZuPrädikat)
}
