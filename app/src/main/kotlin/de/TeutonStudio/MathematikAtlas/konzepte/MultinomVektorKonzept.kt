package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

/** Dynamische, ausführbare Definitionskarte des Multinomvektors. */
internal fun multinomVektorKonzept(knoten: KnotenDaten): KonzeptDefinition {
    val form = knoten.parameter[MULTINOM_AUSGABEFORM_PARAMETER] ?: MULTINOM_AUSGABE_VEKTOR
    val orient = knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: VEKTOR_ORIENTIERUNG_SPALTE
    return KonzeptDefinition(
        id = KonzeptId("multinomvektor-${form}-${orient}"),
        name = "Multinomvektor",
        beschreibung = "Erzeugt die Monome (x^k) für k=0,…,dim. Die Definitionskarte verwendet dieselben produktiven Rechen-, Methoden- und Strukturknoten wie eine normale Karte.",
        pfad = listOf("Lineare Algebra", "Vektoren"),
        tags = setOf("Multinomvektor", "Monom", "Polynom", "Indexmethode", "Tupel", "Vektor"),
        knotenArten = setOf(MULTINOMVEKTOR_ART),
        knotenParameter = mapOf(
            MULTINOM_AUSGABEFORM_PARAMETER to form,
            VEKTOR_ORIENTIERUNG_PARAMETER to orient,
        ),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = "(x^k)_{0\\le k\\le dim}",
                rolle = KonzeptReiterRolle.Definition,
                karte = multinomVektorDefinitionsKarte(form, orient),
            ),
        ),
    )
}

internal fun multinomVektorDefinitionsKarte(
    ausgabeForm: String,
    orientierung: String,
): KartenDaten {
    fun ausgang(id: String, name: String, art: AnschlussArtId) = AnschlussDaten(
        id = AnschlussId(id),
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )
    fun eingang(id: String, name: String, art: AnschlussArtId, index: Int = 0) = AnschlussDaten(
        id = AnschlussId(id),
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = index,
    )

    val x = KnotenDaten(
        id = KnotenId("multinom-definition-x"),
        art = KonzeptKnotenArten.EINGANG,
        name = "x",
        position = GraphPunkt(30f, 75f),
        größe = GraphGröße(180f, 90f),
        anschlüsse = listOf(ausgang("multinom-definition-x-wert", "wert", MathematikAnschlussArten.Zahl.id)),
        parameter = mapOf("typ" to MathematikAnschlussArten.Zahl.id.wert, "rolle" to "x"),
    )
    val dim = KnotenDaten(
        id = KnotenId("multinom-definition-dim"),
        art = KonzeptKnotenArten.EINGANG,
        name = "dim",
        position = GraphPunkt(30f, 390f),
        größe = GraphGröße(180f, 90f),
        anschlüsse = listOf(ausgang("multinom-definition-dim-wert", "wert", MathematikAnschlussArten.Zahl.id)),
        parameter = mapOf("typ" to MathematikAnschlussArten.Zahl.id.wert, "rolle" to "dim"),
    )
    val idx = KnotenDaten(
        id = KnotenId("multinom-definition-idx"),
        art = "mathematik.variable",
        name = "idx",
        position = GraphPunkt(30f, 225f),
        größe = GraphGröße(180f, 95f),
        anschlüsse = listOf(ausgang("multinom-definition-idx-wert", "wert", MathematikAnschlussArten.Zahl.id)),
        parameter = mapOf("name" to "idx", "werteVorrat" to "N"),
    )
    val eins = KnotenDaten(
        id = KnotenId("multinom-definition-eins"),
        art = "mathematik.zahl",
        name = "1",
        position = GraphPunkt(250f, 315f),
        größe = GraphGröße(145f, 85f),
        anschlüsse = listOf(ausgang("multinom-definition-eins-wert", "wert", MathematikAnschlussArten.Zahl.id)),
        parameter = mapOf("wert" to "1"),
    )
    val exponent = KnotenDaten(
        id = KnotenId("multinom-definition-exponent"),
        art = "mathematik.subtraktion",
        name = "idx − 1",
        position = GraphPunkt(445f, 205f),
        größe = GraphGröße(220f, 115f),
        anschlüsse = listOf(
            eingang("multinom-definition-sub-a", "a", MathematikAnschlussArten.Zahl.id, 0),
            eingang("multinom-definition-sub-b", "b", MathematikAnschlussArten.Zahl.id, 1),
            ausgang("multinom-definition-sub-wert", "wert", MathematikAnschlussArten.Zahl.id),
        ),
    )
    val potenz = KnotenDaten(
        id = KnotenId("multinom-definition-potenz"),
        art = "mathematik.potenz",
        name = "x^(idx−1)",
        position = GraphPunkt(705f, 90f),
        größe = GraphGröße(225f, 115f),
        anschlüsse = listOf(
            eingang("multinom-definition-potenz-basis", "basis", MathematikAnschlussArten.Zahl.id, 0),
            eingang("multinom-definition-potenz-exponent", "exponent", MathematikAnschlussArten.Zahl.id, 1),
            ausgang("multinom-definition-potenz-wert", "wert", MathematikAnschlussArten.Zahl.id),
        ),
    )
    val indexMethode = KnotenDaten(
        id = KnotenId("multinom-definition-indexmethode"),
        art = "mathematik.termZuMethode",
        name = "Indexmethode",
        position = GraphPunkt(970f, 95f),
        größe = GraphGröße(235f, 105f),
        anschlüsse = listOf(
            eingang("multinom-definition-methode-term", "term", MathematikAnschlussArten.Zahl.id),
            ausgang("multinom-definition-methode-ausgang", "methode", MathematikAnschlussArten.Methode.id),
        ),
        parameter = mapOf("name" to "m", "argumentReihenfolge" to "idx"),
    )
    val dimPlusEins = KnotenDaten(
        id = KnotenId("multinom-definition-dimension"),
        art = "mathematik.addition",
        name = "dim + 1",
        position = GraphPunkt(455f, 405f),
        größe = GraphGröße(220f, 120f),
        anschlüsse = listOf(
            eingang("multinom-definition-plus-a", "a", MathematikAnschlussArten.Zahl.id, 0),
            eingang("multinom-definition-plus-b", "b", MathematikAnschlussArten.Zahl.id, 1),
            ausgang("multinom-definition-plus-wert", "wert", MathematikAnschlussArten.Zahl.id),
        ),
        parameter = mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )

    val tuple = ausgabeForm == MULTINOM_AUSGABE_TUPEL
    val zeile = orientierung == VEKTOR_ORIENTIERUNG_ZEILE
    val konstruktorArt = if (tuple) "mathematik.tupel" else VEKTOR_KONSTRUKTOR_ART
    val konstruktorAusgangName = if (tuple) "tupel" else "vektor"
    val konstruktorAusgangArt = when {
        tuple -> MathematikAnschlussArten.Tupel.id
        zeile -> MathematikAnschlussArten.ZeilenVektor.id
        else -> MathematikAnschlussArten.SpaltenVektor.id
    }
    val konstruktor = KnotenDaten(
        id = KnotenId("multinom-definition-konstruktor"),
        art = konstruktorArt,
        name = if (tuple) "Tupel" else "Vektor",
        position = GraphPunkt(1245f, 250f),
        größe = GraphGröße(260f, 135f),
        anschlüsse = listOf(
            eingang("multinom-definition-konstruktor-dimension", "dimension", MathematikAnschlussArten.Zahl.id, 0),
            eingang("multinom-definition-konstruktor-methode", "methode", MathematikAnschlussArten.Methode.id, 1),
            ausgang("multinom-definition-konstruktor-wert", konstruktorAusgangName, konstruktorAusgangArt),
        ),
        parameter = if (tuple) {
            mapOf("erzeugungsArt" to TUPEL_METHODE)
        } else {
            mapOf(
                VEKTOR_ERZEUGUNGSART_PARAMETER to VEKTOR_METHODE,
                VEKTOR_ORIENTIERUNG_PARAMETER to if (zeile) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE,
            )
        },
    )
    val ziel = KnotenDaten(
        id = KnotenId("multinom-definition-ausgang"),
        art = KonzeptKnotenArten.AUSGANG,
        name = "wert",
        position = GraphPunkt(1560f, 270f),
        größe = GraphGröße(190f, 90f),
        anschlüsse = listOf(eingang("multinom-definition-ausgang-wert", "wert", konstruktorAusgangArt)),
        parameter = mapOf("typ" to konstruktorAusgangArt.wert, "rolle" to "wert"),
    )

    fun verbindung(id: String, von: KnotenDaten, vonAnschluss: String, zu: KnotenDaten, zuAnschluss: String) = VerbindungDaten(
        id = VerbindungsId(id),
        von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == vonAnschluss }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == zuAnschluss }.id),
    )

    return KartenDaten(
        id = KartenId("multinom-definition-${if (tuple) "tupel" else if (zeile) "zeile" else "spalte"}"),
        name = "Multinomvektor: Definition",
        knoten = listOf(x, dim, idx, eins, exponent, potenz, indexMethode, dimPlusEins, konstruktor, ziel),
        verbindungen = listOf(
            verbindung("multinom-v-idx-sub", idx, "wert", exponent, "a"),
            verbindung("multinom-v-eins-sub", eins, "wert", exponent, "b"),
            verbindung("multinom-v-x-potenz", x, "wert", potenz, "basis"),
            verbindung("multinom-v-sub-potenz", exponent, "wert", potenz, "exponent"),
            verbindung("multinom-v-potenz-methode", potenz, "wert", indexMethode, "term"),
            verbindung("multinom-v-dim-plus", dim, "wert", dimPlusEins, "a"),
            verbindung("multinom-v-eins-plus", eins, "wert", dimPlusEins, "b"),
            verbindung("multinom-v-plus-konstruktor", dimPlusEins, "wert", konstruktor, "dimension"),
            verbindung("multinom-v-methode-konstruktor", indexMethode, "methode", konstruktor, "methode"),
            verbindung("multinom-v-konstruktor-ziel", konstruktor, konstruktorAusgangName, ziel, "wert"),
        ),
    )
}
