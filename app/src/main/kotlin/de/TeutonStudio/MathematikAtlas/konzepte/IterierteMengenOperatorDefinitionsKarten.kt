package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*

private data class IterierteMengenDefinitionsKonfiguration(
    val operator: String,
    val iteration: KnotenVorlage,
    val mengenName: String,
)

internal fun iterierteDefinitionsKarte(
    vorlage: KnotenVorlage,
    variantenIndex: Int,
): KartenDaten = when (vorlage.art) {
    MathematikKnotenVorlagen.IterierteVereinigung.art,
    MathematikKnotenVorlagen.IterierterSchnitt.art ->
        iterierteMengenOperatorDefinitionsKarte(vorlage, variantenIndex)
    else -> iterierteOperatorDefinitionsKarte(vorlage, variantenIndex)
}

internal fun iterierteMengenOperatorDefinitionsKarte(
    vorlage: KnotenVorlage,
    variantenIndex: Int,
): KartenDaten {
    val konfiguration = when (vorlage.art) {
        MathematikKnotenVorlagen.IterierteVereinigung.art -> IterierteMengenDefinitionsKonfiguration(
            operator = "disjunktion",
            iteration = MathematikKnotenVorlagen.IterierteDisjunktion,
            mengenName = "\\bigcup_{i\\in I}A(i)",
        )
        MathematikKnotenVorlagen.IterierterSchnitt.art -> IterierteMengenDefinitionsKonfiguration(
            operator = "konjunktion",
            iteration = MathematikKnotenVorlagen.IterierteKonjunktion,
            mengenName = "\\bigcap_{i\\in I}A(i)",
        )
        else -> error("${vorlage.art} ist keine iterierte Mengenfunktion.")
    }
    val prefix = "definition-${vorlage.art.removePrefix("mathematik.")}-${konfiguration.operator}-$variantenIndex"
    val paarId = "$prefix-mengendefinition"

    val methode = iterierterMengenEingang(
        prefix = prefix,
        kennung = "methode",
        name = "A",
        art = MathematikAnschlussArten.MengenFunktion.id,
        position = GraphPunkt(20f, 80f),
        index = 0,
    )
    val indexMenge = iterierterMengenEingang(
        prefix = prefix,
        kennung = "indexmenge",
        name = "I",
        art = MathematikAnschlussArten.Menge.id,
        position = GraphPunkt(20f, 520f),
        index = 1,
    )
    val index = iterierterMengenVorlagenKnoten(
        prefix = prefix,
        kennung = "index-i",
        vorlage = MathematikKnotenVorlagen.AllgemeinerParameter,
        position = GraphPunkt(350f, 350f),
        parameter = mapOf("name" to "i"),
    )
    val methodenAufruf = iterierterMengenMethodenAufruf(
        prefix = prefix,
        position = GraphPunkt(650f, 110f),
    )
    val mengenKonstruktor = iterierterMengenVorlagenKnoten(
        prefix = prefix,
        kennung = "mengenkonstruktor",
        vorlage = MengendefinitionKnotenVorlagen.Mengenkonstruktor,
        position = GraphPunkt(650f, 520f),
        parameter = mapOf(
            MENGENDEFINITION_PAAR to paarId,
            MENGENDEFINITION_MENGENNAME to konfiguration.mengenName,
            MENGENDEFINITION_ELEMENTNAME to "x",
            MENGENDEFINITION_ELEMENTART to MathematikAnschlussArten.Objekt.id.wert,
        ),
        anschlussArten = mapOf("element" to MathematikAnschlussArten.Objekt.id),
    )
    val element = iterierterMengenVorlagenKnoten(
        prefix = prefix,
        kennung = "element",
        vorlage = MathematikKnotenVorlagen.Element,
        position = GraphPunkt(1000f, 290f),
    )
    val aussageZuMethode = iterierterMengenVorlagenKnoten(
        prefix = prefix,
        kennung = "aussage-zu-methode",
        vorlage = MathematikKnotenVorlagen.AussageZuMethode,
        position = GraphPunkt(1320f, 290f),
        parameter = mapOf(
            "name" to "P",
            "argumentReihenfolge" to "i",
        ),
    )
    val iteration = iterierterMengenVorlagenKnoten(
        prefix = prefix,
        kennung = "iterierte-aussage",
        vorlage = konfiguration.iteration,
        position = GraphPunkt(1660f, 300f),
    )
    val mengenDefinator = iterierterMengenVorlagenKnoten(
        prefix = prefix,
        kennung = "mengendefinator",
        vorlage = MengendefinitionKnotenVorlagen.Mengendefinator,
        position = GraphPunkt(1990f, 300f),
        parameter = mapOf(MENGENDEFINITION_PAAR to paarId),
    )
    val ausgang = iterierterMengenVorlagenKnoten(
        prefix = prefix,
        kennung = "karten-ausgang",
        vorlage = MathematikKnotenVorlagen.KartenAusgang,
        position = GraphPunkt(2320f, 300f),
        parameter = mapOf("name" to "menge"),
        anschlussArten = mapOf("wert" to MathematikAnschlussArten.Menge.id),
    )

    val verbindungen = buildList {
        fun verbinde(von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String, kennung: String) {
            add(iterierteMengenVerbindung(prefix, von, vonName, zu, zuName, kennung))
        }
        verbinde(methode, "wert", methodenAufruf, "methode", "methode-aufruf")
        verbinde(index, "wert", methodenAufruf, "argument-0", "index-aufruf")
        verbinde(mengenKonstruktor, "element", element, "links", "element-links")
        verbinde(methodenAufruf, "wert", element, "rechts", "mengenwert-element")
        verbinde(element, "aussage", aussageZuMethode, "term", "element-praedikat")
        verbinde(aussageZuMethode, "methode", iteration, "methode", "praedikat-iteration")
        verbinde(indexMenge, "wert", iteration, "indexmenge", "indexmenge-iteration")
        verbinde(iteration, "aussage", mengenDefinator, "aussage", "iteration-definator")
        verbinde(mengenDefinator, "menge", ausgang, "wert", "definator-ausgang")
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition von ${vorlage.name}",
        knoten = listOf(
            methode,
            indexMenge,
            index,
            methodenAufruf,
            mengenKonstruktor,
            element,
            aussageZuMethode,
            iteration,
            mengenDefinator,
            ausgang,
        ),
        verbindungen = verbindungen,
    )
}

private fun iterierterMengenMethodenAufruf(
    prefix: String,
    position: GraphPunkt,
): KnotenDaten {
    val id = KnotenId("$prefix-methoden-aufruf")
    val basis = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(position)
    val methodenEingang = basis.anschlüsse.single {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    }
    val ausgang = basis.anschlüsse.single {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "wert"
    }
    return basis.copy(
        id = id,
        anschlüsse = listOf(
            methodenEingang.copy(
                id = AnschlussId("${id.wert}-methode"),
                art = MathematikAnschlussArten.MengenFunktion.id,
                reihenfolge = 0,
            ),
            AnschlussDaten(
                id = AnschlussId("${id.wert}:methodenAufruf:argument:0"),
                name = "argument-0",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Objekt.id,
                reihenfolge = 1,
            ),
            ausgang.copy(
                id = AnschlussId("${id.wert}-wert"),
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
        parameter = basis.parameter + mapOf(
            METHODEN_ANWENDUNG_ERGEBNIS_ART to MathematikAnschlussArten.Menge.id.wert,
            "festeEingänge" to "1",
        ),
    )
}

private fun iterierterMengenEingang(
    prefix: String,
    kennung: String,
    name: String,
    art: AnschlussArtId,
    position: GraphPunkt,
    index: Int,
): KnotenDaten {
    val id = KnotenId("$prefix-eingang-$kennung")
    return KnotenDaten(
        id = id,
        art = TestDefinitionsKarten.KONZEPT_EINGANG_ART,
        name = name,
        position = position,
        größe = GraphGröße(250f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("${id.wert}-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = art,
                reihenfolge = index,
            ),
        ),
        parameter = mapOf("typ" to art.wert, "variabel" to "false", "folgtEingang" to ""),
    )
}

private fun iterierterMengenVorlagenKnoten(
    prefix: String,
    kennung: String,
    vorlage: KnotenVorlage,
    position: GraphPunkt,
    parameter: Map<String, String> = emptyMap(),
    anschlussArten: Map<String, AnschlussArtId> = emptyMap(),
): KnotenDaten {
    val id = KnotenId("$prefix-$kennung")
    return vorlage.erzeuge(position).copy(
        id = id,
        anschlüsse = vorlage.anschlüsse.mapIndexed { index, anschluss ->
            anschluss.copy(
                id = AnschlussId("${id.wert}-anschluss-$index"),
                art = anschlussArten[anschluss.name] ?: anschluss.art,
            )
        },
        parameter = vorlage.standardParameter + parameter,
    )
}

private fun iterierteMengenVerbindung(
    prefix: String,
    von: KnotenDaten,
    vonName: String,
    zu: KnotenDaten,
    zuName: String,
    kennung: String,
): VerbindungDaten = VerbindungDaten(
    id = VerbindungsId("$prefix-$kennung"),
    von = AnschlussVerweis(von.id, von.iterierterMengenAnschluss(vonName, AnschlussRichtung.Ausgang).id),
    zu = AnschlussVerweis(zu.id, zu.iterierterMengenAnschluss(zuName, AnschlussRichtung.Eingang).id),
)

private fun KnotenDaten.iterierterMengenAnschluss(
    name: String,
    richtung: AnschlussRichtung,
): AnschlussDaten = anschlüsse.single { it.name == name && it.richtung == richtung }
