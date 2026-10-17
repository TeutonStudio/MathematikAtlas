package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

private val ZEILEN_ALIAS_ARTEN = setOf(
    "mathematik.tupelZuZeile",
    "mathematik.einheitsZeile",
    "mathematik.kreuzproduktZeile",
    "mathematik.zeilenMethodeDifferentieren",
    "mathematik.zeilenMethodeIntegrieren",
)

fun orientierungFürOrientierungsKnoten(knoten: KnotenDaten): String =
    knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER]
        ?.takeIf { it in setOf(VEKTOR_ORIENTIERUNG_SPALTE, VEKTOR_ORIENTIERUNG_ZEILE) }
        ?: if (knoten.art in ZEILEN_ALIAS_ARTEN) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE

/** Nur ausdrücklich typisierte Suchvarianten besitzen eine wählbare Ergebnisorientierung. */
fun istKonfigurierteVektorAnalysis(k: KnotenDaten): Boolean =
    k.art in setOf("mathematik.methodenDifferentieren", "mathematik.methodenIntegrieren") &&
        k.parameter[methodenErgebnisArtSchlüssel("methode")] in setOf("mathematik.vektor.spalte", "mathematik.vektor.zeile")

fun konfiguriereOrientierungsKnoten(
    knoten: KnotenDaten,
    orientierung: String,
): KnotenDaten {
    require(orientierung in setOf(VEKTOR_ORIENTIERUNG_ZEILE,VEKTOR_ORIENTIERUNG_SPALTE)) { "Unbekannte Orientierung '$orientierung'." }
    val orient = orientierung
    val zeile = orient == VEKTOR_ORIENTIERUNG_ZEILE
    val vorhandene = knoten.anschlüsse.associateBy { it.richtung to it.name }
    fun eingang(name: String, art: AnschlussArtId, index: Int): AnschlussDaten = vorhandene[AnschlussRichtung.Eingang to name]?.copy(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = index,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art, reihenfolge = index)
    fun ausgang(name: String, art: AnschlussArtId): AnschlussDaten = vorhandene[AnschlussRichtung.Ausgang to name]?.copy(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(name = name, richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)

    val vektorArt = if (zeile) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id
    val methodeArt = if (zeile) MathematikAnschlussArten.ZeilenVektorMethode.id else MathematikAnschlussArten.SpaltenVektorMethode.id
    val (art, name, anschlüsse) = when (knoten.art) {
        "mathematik.tupelZuSpalte", "mathematik.tupelZuZeile" -> Triple(
            "mathematik.tupelZuSpalte",
            "Tupel zu Vektor",
            listOf(eingang("tupel", MathematikAnschlussArten.Tupel.id, 0), ausgang("vektor", vektorArt)),
        )
        "mathematik.einheitsSpalte", "mathematik.einheitsZeile" -> Triple(
            "mathematik.einheitsSpalte",
            "Einheitsvektor",
            listOf(
                eingang("position", MathematikAnschlussArten.Zahl.id, 0),
                eingang("dimension", MathematikAnschlussArten.Zahl.id, 1),
                ausgang("vektor", vektorArt),
            ),
        )
        "mathematik.kreuzproduktSpalte", "mathematik.kreuzproduktZeile" -> Triple(
            "mathematik.kreuzproduktSpalte",
            "Kreuzprodukt",
            listOf(eingang("a", vektorArt, 0), eingang("b", vektorArt, 1), ausgang("vektor", vektorArt)),
        )
        "mathematik.spaltenMethodeDifferentieren", "mathematik.zeilenMethodeDifferentieren" -> Triple(
            "mathematik.spaltenMethodeDifferentieren",
            "Vektormethode differentieren",
            listOf(eingang("methode", methodeArt, 0), ausgang("methode", methodeArt)),
        )
        "mathematik.spaltenMethodeIntegrieren", "mathematik.zeilenMethodeIntegrieren" -> Triple(
            "mathematik.spaltenMethodeIntegrieren",
            "Vektormethode integrieren",
            listOf(eingang("methode", methodeArt, 0), ausgang("methode", methodeArt)),
        )
        "mathematik.methodenDifferentieren", "mathematik.methodenIntegrieren" -> {
            require(istKonfigurierteVektorAnalysis(knoten)) { "Diese Analysis folgt der tatsächlichen Ergebnisorientierung ihrer Methode." }
            Triple(knoten.art, knoten.name, listOf(eingang("methode", methodeArt, 0), ausgang("methode", methodeArt)))
        }
        else -> error("Knoten '${knoten.art}' besitzt keinen reinen Orientierungsvertrag.")
    }
    return knoten.copy(
        art = art,
        name = name,
        anschlüsse = anschlüsse,
        parameter = knoten.parameter + (VEKTOR_ORIENTIERUNG_PARAMETER to orient) +
            if (istKonfigurierteVektorAnalysis(knoten)) mapOf(methodenErgebnisArtSchlüssel("methode") to vektorArt.wert) else emptyMap(),
    )
}

object VektorOrientierungsV2300Vorlagen {
    private fun input(name: String, art: AnschlussArtId, index: Int = 0) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = index,
    )
    private fun output(name: String, art: AnschlussArtId) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )

    val TupelZuVektor = KnotenVorlage(
        art = "mathematik.tupelZuSpalte",
        name = "Tupel zu Vektor",
        kategorie = "Vektoren",
        beschreibung = "Interpretiert ein Zahlentupel als Zeilen- oder Spaltenvektor; die Orientierung wird im Inspector gewählt.",
        standardGröße = GraphGröße(235f, 110f),
        anschlüsse = listOf(input("tupel", MathematikAnschlussArten.Tupel.id), output("vektor", MathematikAnschlussArten.SpaltenVektor.id)),
        standardParameter = mapOf(VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_SPALTE),
    ).alsAdapterVorlage()

    val EinheitsVektor = KnotenVorlage(
        art = "mathematik.einheitsSpalte",
        name = "Einheitsvektor",
        kategorie = "Vektoren",
        beschreibung = "Erzeugt einen Standardbasisvektor aus nullbasierter Position und positiver Dimension; Orientierung im Inspector.",
        standardGröße = GraphGröße(255f, 125f),
        anschlüsse = listOf(
            input("position", MathematikAnschlussArten.Zahl.id, 0),
            input("dimension", MathematikAnschlussArten.Zahl.id, 1),
            output("vektor", MathematikAnschlussArten.SpaltenVektor.id),
        ),
        standardParameter = mapOf(
            VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_SPALTE,
            "standardwert.position" to "0",
            "standardwert.dimension" to "3",
        ),
    )

    val Radius = KnotenVorlage(
        art = "mathematik.vektorRadiusSpalte",
        name = "Vektorradius",
        kategorie = "Vektoren",
        beschreibung = "Berechnet die euklidische Norm eines Zeilen- oder Spaltenvektors ohne Orientierungsduplikat.",
        standardGröße = GraphGröße(225f, 105f),
        anschlüsse = listOf(input("vektor", MathematikAnschlussArten.Vektor.id), output("wert", MathematikAnschlussArten.Zahl.id)),
    )

    val Kreuzprodukt = KnotenVorlage(
        art = "mathematik.kreuzproduktSpalte",
        name = "Kreuzprodukt",
        kategorie = "Vektoren",
        beschreibung = "Kreuzprodukt zweier gleich orientierter 3-Vektoren; Orientierung im Inspector.",
        standardGröße = GraphGröße(240f, 115f),
        anschlüsse = listOf(
            input("a", MathematikAnschlussArten.SpaltenVektor.id, 0),
            input("b", MathematikAnschlussArten.SpaltenVektor.id, 1),
            output("vektor", MathematikAnschlussArten.SpaltenVektor.id),
        ),
        standardParameter = mapOf(VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_SPALTE),
    )

    val MethodeDifferentieren = KnotenVorlage(
        art = "mathematik.spaltenMethodeDifferentieren",
        name = "Vektormethode differentieren",
        kategorie = "Abbildungen",
        beschreibung = "Differentiert eine Zeilen- oder Spaltenvektormethode komponentenweise; Orientierung im Inspector.",
        standardGröße = GraphGröße(270f, 110f),
        anschlüsse = listOf(
            input("methode", MathematikAnschlussArten.SpaltenVektorMethode.id),
            output("methode", MathematikAnschlussArten.SpaltenVektorMethode.id),
        ),
        standardParameter = mapOf(VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_SPALTE),
    )

    val MethodeIntegrieren = KnotenVorlage(
        art = "mathematik.spaltenMethodeIntegrieren",
        name = "Vektormethode integrieren",
        kategorie = "Abbildungen",
        beschreibung = "Integriert eine Zeilen- oder Spaltenvektormethode komponentenweise; Orientierung im Inspector.",
        standardGröße = GraphGröße(270f, 110f),
        anschlüsse = listOf(
            input("methode", MathematikAnschlussArten.SpaltenVektorMethode.id),
            output("methode", MathematikAnschlussArten.SpaltenVektorMethode.id),
        ),
        standardParameter = mapOf(VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_SPALTE),
    )

    val alle = listOf(TupelZuVektor, EinheitsVektor, Radius, Kreuzprodukt, MethodeDifferentieren, MethodeIntegrieren)
}
