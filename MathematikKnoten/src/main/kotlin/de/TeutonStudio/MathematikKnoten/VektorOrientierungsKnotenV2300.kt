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

fun konfiguriereOrientierungsKnoten(
    knoten: KnotenDaten,
    orientierung: String,
): KnotenDaten {
    val orient = if (orientierung == VEKTOR_ORIENTIERUNG_ZEILE) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE
    val zeile = orient == VEKTOR_ORIENTIERUNG_ZEILE
    val vorhandene = knoten.anschlüsse.associateBy { it.name }
    fun eingang(name: String, art: AnschlussArtId, index: Int): AnschlussDaten = vorhandene[name]?.takeIf { it.art == art }?.copy(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = index,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art, reihenfolge = index)
    fun ausgang(name: String, art: AnschlussArtId): AnschlussDaten = vorhandene[name]?.takeIf { it.art == art }?.copy(
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
        else -> error("Knoten '${knoten.art}' besitzt keinen reinen Orientierungsvertrag.")
    }
    return knoten.copy(
        art = art,
        name = name,
        anschlüsse = anschlüsse,
        parameter = knoten.parameter + (VEKTOR_ORIENTIERUNG_PARAMETER to orient),
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
    )

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

fun MathematikAuswerterRegister.registriereVektorOrientierungsKnotenV2300() {
    val diffSpalte = finde("mathematik.spaltenMethodeDifferentieren")
    val diffZeile = finde("mathematik.zeilenMethodeDifferentieren")
    val intSpalte = finde("mathematik.spaltenMethodeIntegrieren")
    val intZeile = finde("mathematik.zeilenMethodeIntegrieren")

    registriere("mathematik.tupelZuSpalte") { k -> tupelZuVektorErgebnis(k, VEKTOR_ORIENTIERUNG_SPALTE) }
    registriere("mathematik.tupelZuZeile") { k -> tupelZuVektorErgebnis(k, VEKTOR_ORIENTIERUNG_ZEILE) }
    registriere("mathematik.einheitsSpalte") { k -> einheitsVektorErgebnisV2300(k, VEKTOR_ORIENTIERUNG_SPALTE) }
    registriere("mathematik.einheitsZeile") { k -> einheitsVektorErgebnisV2300(k, VEKTOR_ORIENTIERUNG_ZEILE) }
    registriere("mathematik.vektorRadiusSpalte") { k -> vektorRadiusErgebnis(k) }
    registriere("mathematik.vektorRadiusZeile") { k -> vektorRadiusErgebnis(k) }
    registriere("mathematik.kreuzproduktSpalte") { k -> kreuzproduktErgebnisV2300(k, VEKTOR_ORIENTIERUNG_SPALTE) }
    registriere("mathematik.kreuzproduktZeile") { k -> kreuzproduktErgebnisV2300(k, VEKTOR_ORIENTIERUNG_ZEILE) }

    if (diffSpalte != null && diffZeile != null) {
        registriere("mathematik.spaltenMethodeDifferentieren") { k ->
            if (orientierungFürOrientierungsKnoten(k.knoten) == VEKTOR_ORIENTIERUNG_ZEILE) diffZeile.auswerten(k) else diffSpalte.auswerten(k)
        }
        registriere("mathematik.zeilenMethodeDifferentieren") { k -> diffZeile.auswerten(k) }
    }
    if (intSpalte != null && intZeile != null) {
        registriere("mathematik.spaltenMethodeIntegrieren") { k ->
            if (orientierungFürOrientierungsKnoten(k.knoten) == VEKTOR_ORIENTIERUNG_ZEILE) intZeile.auswerten(k) else intSpalte.auswerten(k)
        }
        registriere("mathematik.zeilenMethodeIntegrieren") { k -> intZeile.auswerten(k) }
    }
}

private fun tatsächlicheOrientierung(k: KnotenAuswertungsKontext, historisch: String): String =
    k.knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER]
        ?: if (k.knoten.art in ZEILEN_ALIAS_ARTEN) VEKTOR_ORIENTIERUNG_ZEILE else historisch

private fun tupelZuVektorErgebnis(k: KnotenAuswertungsKontext, historisch: String): KnotenAuswertungsErgebnis {
    val tupel = k.eingänge["tupel"]?.objekt as? Tupel ?: error("Tupel fehlt.")
    val zahlen = tupel.elemente.mapIndexed { index, element ->
        element as? ZahlAusdruck ?: error("Tupelkomponente ${index + 1} ist keine Zahl.")
    }
    val vektor: MathematischesObjekt = if (tatsächlicheOrientierung(k, historisch) == VEKTOR_ORIENTIERUNG_ZEILE) ZeilenVektor(zahlen) else SpaltenVektor(zahlen)
    return KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(vektor, k.eingänge.values.flatMap { it.annahmen }.toSet())), eingänge = k.eingänge)
}

private fun einheitsVektorErgebnisV2300(k: KnotenAuswertungsKontext, historisch: String): KnotenAuswertungsErgebnis {
    val dimensionWert = k.eingänge["dimension"]?.objekt as? RationaleZahl
    val positionWert = k.eingänge["position"]?.objekt as? RationaleZahl
    val werte = if (dimensionWert != null && positionWert != null) {
        require(dimensionWert.nenner == BigInteger.ONE && dimensionWert.zähler.signum() > 0 && dimensionWert.zähler.bitLength() < 31) { "Die Dimension muss positiv ganzzahlig sein." }
        require(positionWert.nenner == BigInteger.ONE && positionWert.zähler.signum() >= 0 && positionWert.zähler.bitLength() < 31) { "Die Position muss nichtnegativ ganzzahlig sein." }
        val dimension = dimensionWert.zähler.toInt()
        val position = positionWert.zähler.toInt()
        require(position < dimension) { "Die Position muss kleiner als die Dimension sein." }
        List<ZahlAusdruck>(dimension) { index -> if (index == position) RationaleZahl.Eins else RationaleZahl.Null }
    } else {
        val dimension = k.knoten.parameter["dimension"]?.toIntOrNull() ?: 3
        val index = k.knoten.parameter["index"]?.toIntOrNull() ?: 1
        require(dimension > 0 && index in 1..dimension) { "Historischer Einheitsvektor besitzt ungültige Dimension oder Index." }
        List<ZahlAusdruck>(dimension) { position -> if (position == index - 1) RationaleZahl.Eins else RationaleZahl.Null }
    }
    val vektor: MathematischesObjekt = if (tatsächlicheOrientierung(k, historisch) == VEKTOR_ORIENTIERUNG_ZEILE) ZeilenVektor(werte) else SpaltenVektor(werte)
    return KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(vektor, k.eingänge.values.flatMap { it.annahmen }.toSet())), eingänge = k.eingänge)
}

private fun vektorRadiusErgebnis(k: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val vektor = k.eingänge["vektor"]?.objekt as? OrientierterVektor ?: error("Vektor fehlt.")
    return KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(vektor.radius(), k.eingänge.values.flatMap { it.annahmen }.toSet())), eingänge = k.eingänge)
}

private fun kreuzproduktErgebnisV2300(k: KnotenAuswertungsKontext, historisch: String): KnotenAuswertungsErgebnis {
    val a = k.eingänge["a"]?.objekt as? OrientierterVektor ?: error("Vektor a fehlt.")
    val b = k.eingänge["b"]?.objekt as? OrientierterVektor ?: error("Vektor b fehlt.")
    val zeile = tatsächlicheOrientierung(k, historisch) == VEKTOR_ORIENTIERUNG_ZEILE
    require((a is ZeilenVektor) == zeile && (b is ZeilenVektor) == zeile) { "Beide Kreuzproduktvektoren müssen die gewählte Orientierung besitzen." }
    val ergebnis = kreuzprodukt(a, b)
    return KnotenAuswertungsErgebnis(mapOf("vektor" to BedingterWert(ergebnis, k.eingänge.values.flatMap { it.annahmen }.toSet())), eingänge = k.eingänge)
}
