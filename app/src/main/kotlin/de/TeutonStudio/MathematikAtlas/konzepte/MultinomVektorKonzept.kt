package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

/** Dynamische, strukturelle Definitionskarte des Multinomvektors. */
internal fun multinomVektorKonzept(knoten: KnotenDaten): KonzeptDefinition {
    val form = knoten.parameter[MULTINOM_AUSGABEFORM_PARAMETER] ?: MULTINOM_AUSGABE_VEKTOR
    val orient = knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: VEKTOR_ORIENTIERUNG_SPALTE
    return KonzeptDefinition(
        id = KonzeptId("multinomvektor-${form}-${orient}"),
        name = "Multinomvektor",
        beschreibung = "Erzeugt die Monome (x^k) für k=0,…,dim. Die Definitionskarte benutzt denselben Dimension-und-Indexmethode-Vertrag wie Tupel und Vektor.",
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
        position = GraphPunkt(30f, 65f),
        größe = GraphGröße(180f, 90f),
        anschlüsse = listOf(ausgang("multinom-definition-x-wert", "wert", MathematikAnschlussArten.Zahl.id)),
        parameter = mapOf("typ" to MathematikAnschlussArten.Zahl.id.wert, "rolle" to "x"),
    )
    val dim = KnotenDaten(
        id = KnotenId("multinom-definition-dim"),
        art = KonzeptKnotenArten.EINGANG,
        name = "dim",
        position = GraphPunkt(30f, 320f),
        größe = GraphGröße(180f, 90f),
        anschlüsse = listOf(ausgang("multinom-definition-dim-wert", "wert", MathematikAnschlussArten.Zahl.id)),
        parameter = mapOf("typ" to MathematikAnschlussArten.Zahl.id.wert, "rolle" to "dim"),
    )
    val eins = KnotenDaten(
        id = KnotenId("multinom-definition-eins"),
        art = "mathematik.zahl",
        name = "1",
        position = GraphPunkt(250f, 420f),
        größe = GraphGröße(150f, 85f),
        anschlüsse = listOf(ausgang("multinom-definition-eins-wert", "wert", MathematikAnschlussArten.Zahl.id)),
        parameter = mapOf("wert" to "1"),
    )
    val dimPlusEins = KnotenDaten(
        id = KnotenId("multinom-definition-dimension"),
        art = "mathematik.addition",
        name = "dim + 1",
        position = GraphPunkt(455f, 330f),
        größe = GraphGröße(220f, 120f),
        anschlüsse = listOf(
            eingang("multinom-definition-plus-a", "a", MathematikAnschlussArten.Zahl.id, 0),
            eingang("multinom-definition-plus-b", "b", MathematikAnschlussArten.Zahl.id, 1),
            ausgang("multinom-definition-plus-wert", "wert", MathematikAnschlussArten.Zahl.id),
        ),
        parameter = mapOf("festeEingänge" to "2", "operatorAnzeige" to "wert"),
    )
    val indexMethode = KnotenDaten(
        id = KnotenId("multinom-definition-indexmethode"),
        art = KonzeptKnotenArten.REGEL,
        name = "Indexmethode",
        position = GraphPunkt(440f, 70f),
        größe = GraphGröße(300f, 145f),
        anschlüsse = listOf(
            eingang("multinom-definition-regel-x", "x", MathematikAnschlussArten.Zahl.id),
            ausgang("multinom-definition-regel-methode", "methode", MathematikAnschlussArten.Methode.id),
        ),
        parameter = mapOf(
            "regel" to "Die Konstruktoren verwenden die mathematischen Indizes idx=1,…,dim+1.",
            "definition" to "idx\\mapsto x^{idx-1}",
        ),
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
        position = GraphPunkt(790f, 185f),
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
        position = GraphPunkt(1110f, 205f),
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
        knoten = listOf(x, dim, eins, dimPlusEins, indexMethode, konstruktor, ziel),
        verbindungen = listOf(
            verbindung("multinom-v-x-regel", x, "wert", indexMethode, "x"),
            verbindung("multinom-v-dim-plus", dim, "wert", dimPlusEins, "a"),
            verbindung("multinom-v-eins-plus", eins, "wert", dimPlusEins, "b"),
            verbindung("multinom-v-plus-konstruktor", dimPlusEins, "wert", konstruktor, "dimension"),
            verbindung("multinom-v-regel-konstruktor", indexMethode, "methode", konstruktor, "methode"),
            verbindung("multinom-v-konstruktor-ziel", konstruktor, konstruktorAusgangName, ziel, "wert"),
        ),
    )
}
