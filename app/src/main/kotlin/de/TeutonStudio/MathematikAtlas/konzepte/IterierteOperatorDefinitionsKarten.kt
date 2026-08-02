package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*

private data class IterierteDefinitionsKonfiguration(
    val operator: String,
    val methodenName: String,
    val methodenArt: AnschlussArtId,
    val ergebnisArt: AnschlussArtId,
    val anwendung: KnotenVorlage,
    val verknüpfung: KnotenVorlage,
    val neutralVorlage: KnotenVorlage?,
    val neutralParameter: Map<String, String> = emptyMap(),
    val verknüpfungsEingangA: String = "a",
    val verknüpfungsEingangB: String = "b",
    val verknüpfungsAusgang: String,
)

internal fun iterierteOperatorDefinitionsKarte(
    vorlage: KnotenVorlage,
    variantenIndex: Int,
): KartenDaten {
    if (vorlage.art == "mathematik.iteriertesKartesischesProdukt") {
        return iteriertesKartesischesProduktDefinitionsKarte(vorlage, variantenIndex)
    }
    val konfiguration = iterierteKonfiguration(vorlage)
    val prefix = "definition-${vorlage.art.removePrefix("mathematik.")}-${konfiguration.operator}-$variantenIndex"
    val paarId = "$prefix-faltung"

    val methode = konzeptEingang(
        prefix = prefix,
        kennung = "methode",
        name = konfiguration.methodenName,
        art = konfiguration.methodenArt,
        position = GraphPunkt(20f, 70f),
        index = 0,
    )
    val indexMenge = konzeptEingang(
        prefix = prefix,
        kennung = "indexmenge",
        name = "I",
        art = MathematikAnschlussArten.Menge.id,
        position = GraphPunkt(20f, 430f),
        index = 1,
    )
    val neutral = konfiguration.neutralVorlage?.let { neutralVorlage ->
        definitionsVorlagenKnoten(
            prefix = prefix,
            kennung = "neutral",
            vorlage = neutralVorlage,
            position = GraphPunkt(310f, 610f),
            parameter = konfiguration.neutralParameter,
        )
    }
    val zielMenge = if (konfiguration.operator == "schnitt") {
        definitionsVorlagenKnoten(
            prefix,
            "methoden-zielmenge",
            FaltungsKnotenVorlagen.MethodenZielmenge,
            GraphPunkt(310f, 610f),
            anschlussArten = mapOf("methode" to konfiguration.methodenArt),
        )
    } else null
    val konstruktor = definitionsVorlagenKnoten(
        prefix = prefix,
        kennung = "faltungskonstruktor",
        vorlage = FaltungsKnotenVorlagen.Faltungskonstruktor,
        position = GraphPunkt(590f, 330f),
        parameter = mapOf(
            FALTUNG_PAAR to paarId,
            FALTUNG_OPERATOR to konfiguration.operator,
            FALTUNG_INDEXNAME to "i",
            FALTUNG_AKKUMULATORNAME to "a",
        ),
        anschlussArten = mapOf(
            "neutral" to konfiguration.ergebnisArt,
            "akkumulator" to konfiguration.ergebnisArt,
        ),
    )
    val anwendung = definitionsVorlagenKnoten(
        prefix,
        "methoden-anwendung",
        konfiguration.anwendung,
        GraphPunkt(920f, 95f),
        anschlussArten = mapOf(
            "methode" to konfiguration.methodenArt,
            "argument" to MathematikAnschlussArten.Zahl.id,
            "wert" to konfiguration.ergebnisArt,
        ),
    )
    val verknüpfung = definitionsVorlagenKnoten(
        prefix,
        "verknuepfung",
        konfiguration.verknüpfung,
        GraphPunkt(1260f, 315f),
    )
    val definator = definitionsVorlagenKnoten(
        prefix = prefix,
        kennung = "faltungsdefinator",
        vorlage = FaltungsKnotenVorlagen.Faltungsdefinator,
        position = GraphPunkt(1590f, 315f),
        parameter = mapOf(
            FALTUNG_PAAR to paarId,
            FALTUNG_OPERATOR to konfiguration.operator,
        ),
        anschlussArten = mapOf(
            "nächsterAkkumulator" to konfiguration.ergebnisArt,
            "wert" to konfiguration.ergebnisArt,
        ),
    )
    val ausgang = definitionsVorlagenKnoten(
        prefix,
        "karten-ausgang",
        MathematikKnotenVorlagen.KartenAusgang,
        GraphPunkt(1920f, 315f),
        parameter = mapOf("name" to if (konfiguration.ergebnisArt == MathematikAnschlussArten.Menge.id) "menge" else if (konfiguration.ergebnisArt == MathematikAnschlussArten.Aussage.id) "aussage" else "wert"),
        anschlussArten = mapOf("wert" to konfiguration.ergebnisArt),
    )

    val knoten = listOfNotNull(methode, indexMenge, neutral, zielMenge, konstruktor, anwendung, verknüpfung, definator, ausgang)
    val verbindungen = buildList {
        fun verbinde(von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String, kennung: String) {
            add(definitionsVerbindung(prefix, von, vonName, zu, zuName, kennung))
        }
        verbinde(methode, "wert", anwendung, "methode", "methode-anwendung")
        verbinde(indexMenge, "wert", konstruktor, "indexmenge", "indexmenge-konstruktor")
        verbinde(konstruktor, "index", anwendung, "argument", "index-anwendung")
        if (neutral != null) verbinde(neutral, neutral.ausgangsName(), konstruktor, "neutral", "neutral-konstruktor")
        if (zielMenge != null) {
            verbinde(methode, "wert", zielMenge, "methode", "methode-zielmenge")
            verbinde(zielMenge, "menge", konstruktor, "neutral", "zielmenge-konstruktor")
        }
        verbinde(konstruktor, "akkumulator", verknüpfung, konfiguration.verknüpfungsEingangA, "akkumulator-verknuepfung")
        verbinde(anwendung, "wert", verknüpfung, konfiguration.verknüpfungsEingangB, "anwendung-verknuepfung")
        verbinde(verknüpfung, konfiguration.verknüpfungsAusgang, definator, "nächsterAkkumulator", "verknuepfung-definator")
        verbinde(definator, "wert", ausgang, "wert", "definator-ausgang")
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition von ${vorlage.name}",
        knoten = knoten,
        verbindungen = verbindungen,
    )
}

private fun iterierteKonfiguration(vorlage: KnotenVorlage): IterierteDefinitionsKonfiguration = when (vorlage.art) {
    "mathematik.iterierteSumme" -> IterierteDefinitionsKonfiguration(
        operator = "summe",
        methodenName = "f",
        methodenArt = MathematikAnschlussArten.ZahlMethode.id,
        ergebnisArt = MathematikAnschlussArten.Zahl.id,
        anwendung = FaltungsKnotenVorlagen.MethodenAnwendungZahl,
        verknüpfung = MathematikKnotenVorlagen.Addition,
        neutralVorlage = MathematikKnotenVorlagen.Zahl,
        neutralParameter = mapOf("wert" to "0"),
        verknüpfungsAusgang = "wert",
    )
    "mathematik.iteriertesProdukt" -> IterierteDefinitionsKonfiguration(
        operator = "produkt",
        methodenName = "f",
        methodenArt = MathematikAnschlussArten.ZahlMethode.id,
        ergebnisArt = MathematikAnschlussArten.Zahl.id,
        anwendung = FaltungsKnotenVorlagen.MethodenAnwendungZahl,
        verknüpfung = MathematikKnotenVorlagen.Multiplikation,
        neutralVorlage = MathematikKnotenVorlagen.Zahl,
        neutralParameter = mapOf("wert" to "1"),
        verknüpfungsAusgang = "wert",
    )
    MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART -> when (vorlage.standardParameter["operator"]) {
        "konjunktion" -> IterierteDefinitionsKonfiguration(
            "konjunktion", "P", MathematikAnschlussArten.AussageMethode.id,
            MathematikAnschlussArten.Aussage.id, FaltungsKnotenVorlagen.MethodenAnwendungAussage,
            MathematikKnotenVorlagen.Konjunktion, MathematikKnotenVorlagen.Wahr,
            verknüpfungsAusgang = "aussage",
        )
        "disjunktion" -> IterierteDefinitionsKonfiguration(
            "disjunktion", "P", MathematikAnschlussArten.AussageMethode.id,
            MathematikAnschlussArten.Aussage.id, FaltungsKnotenVorlagen.MethodenAnwendungAussage,
            MathematikKnotenVorlagen.Disjunktion, MathematikKnotenVorlagen.Lüge,
            verknüpfungsAusgang = "aussage",
        )
        "adjunktion" -> IterierteDefinitionsKonfiguration(
            "adjunktion", "P", MathematikAnschlussArten.AussageMethode.id,
            MathematikAnschlussArten.Aussage.id, FaltungsKnotenVorlagen.MethodenAnwendungAussage,
            AussagenLogikKnotenVorlagen.Adjunktion, MathematikKnotenVorlagen.Lüge,
            verknüpfungsAusgang = "aussage",
        )
        else -> error("Unbekannte iterierte Aussagenverknüpfung.")
    }
    "mathematik.iterierteVereinigung" -> IterierteDefinitionsKonfiguration(
        operator = "vereinigung",
        methodenName = "A",
        methodenArt = MathematikAnschlussArten.MengenMethode.id,
        ergebnisArt = MathematikAnschlussArten.Menge.id,
        anwendung = FaltungsKnotenVorlagen.MethodenAnwendungMenge,
        verknüpfung = MathematikKnotenVorlagen.Vereinigung,
        neutralVorlage = MengenraumKnotenVorlagen.LeereMenge,
        verknüpfungsAusgang = "menge",
    )
    "mathematik.iterierterSchnitt" -> IterierteDefinitionsKonfiguration(
        operator = "schnitt",
        methodenName = "A",
        methodenArt = MathematikAnschlussArten.MengenMethode.id,
        ergebnisArt = MathematikAnschlussArten.Menge.id,
        anwendung = FaltungsKnotenVorlagen.MethodenAnwendungMenge,
        verknüpfung = MathematikKnotenVorlagen.Schnitt,
        neutralVorlage = null,
        verknüpfungsAusgang = "menge",
    )
    else -> error("Für ${vorlage.art} existiert keine iterierte Definitionskonfiguration.")
}

private fun iteriertesKartesischesProduktDefinitionsKarte(
    vorlage: KnotenVorlage,
    variantenIndex: Int,
): KartenDaten {
    val prefix = "definition-iteriertes-kartesisches-produkt-$variantenIndex"
    val faltungsId = "$prefix-faltung"
    val mengenPaarId = "$prefix-mengendefinition"
    val methode = konzeptEingang(prefix, "methode", "A", MathematikAnschlussArten.MengenMethode.id, GraphPunkt(20f, 80f), 0)
    val indexMenge = konzeptEingang(prefix, "indexmenge", "I", MathematikAnschlussArten.Menge.id, GraphPunkt(20f, 510f), 1)
    val vereinigung = definitionsVorlagenKnoten(prefix, "iterierte-vereinigung", MathematikKnotenVorlagen.IterierteVereinigung, GraphPunkt(330f, 60f))
    val funktionsRaum = definitionsVorlagenKnoten(prefix, "abbildungsmenge", MengenraumKnotenVorlagen.Abbildungsmenge, GraphPunkt(670f, 100f))
    val mengenKonstruktor = definitionsVorlagenKnoten(
        prefix, "mengenkonstruktor", MengendefinitionKnotenVorlagen.Mengenkonstruktor, GraphPunkt(1000f, 235f),
        parameter = mapOf(
            MENGENDEFINITION_PAAR to mengenPaarId,
            MENGENDEFINITION_MENGENNAME to "\\mathop{\\times}\\limits_{i\\in I}A(i)",
            MENGENDEFINITION_ELEMENTNAME to "g",
            MENGENDEFINITION_ELEMENTART to MathematikAnschlussArten.Methode.id.wert,
        ),
        anschlussArten = mapOf("element" to MathematikAnschlussArten.Methode.id),
    )
    val gImRaum = definitionsVorlagenKnoten(prefix, "g-im-funktionsraum", MathematikKnotenVorlagen.Element, GraphPunkt(1330f, 90f))
    val wahr = definitionsVorlagenKnoten(prefix, "wahr", MathematikKnotenVorlagen.Wahr, GraphPunkt(1040f, 650f))
    val faltungsKonstruktor = definitionsVorlagenKnoten(
        prefix, "faltungskonstruktor", FaltungsKnotenVorlagen.Faltungskonstruktor, GraphPunkt(1300f, 545f),
        parameter = mapOf(FALTUNG_PAAR to faltungsId, FALTUNG_OPERATOR to "konjunktion", FALTUNG_INDEXNAME to "i", FALTUNG_AKKUMULATORNAME to "a"),
        anschlussArten = mapOf("neutral" to MathematikAnschlussArten.Aussage.id, "akkumulator" to MathematikAnschlussArten.Aussage.id),
    )
    val gAnwendung = definitionsVorlagenKnoten(prefix, "g-anwendung", FaltungsKnotenVorlagen.MethodenAnwendungObjekt, GraphPunkt(1640f, 380f))
    val aAnwendung = definitionsVorlagenKnoten(prefix, "a-anwendung", FaltungsKnotenVorlagen.MethodenAnwendungMenge, GraphPunkt(1640f, 660f))
    val element = definitionsVorlagenKnoten(prefix, "auswahl-element", MathematikKnotenVorlagen.Element, GraphPunkt(1980f, 515f))
    val innereKonjunktion = definitionsVorlagenKnoten(prefix, "innere-konjunktion", MathematikKnotenVorlagen.Konjunktion, GraphPunkt(2310f, 555f))
    val faltungsDefinator = definitionsVorlagenKnoten(
        prefix, "faltungsdefinator", FaltungsKnotenVorlagen.Faltungsdefinator, GraphPunkt(2640f, 555f),
        parameter = mapOf(FALTUNG_PAAR to faltungsId, FALTUNG_OPERATOR to "konjunktion"),
        anschlussArten = mapOf("nächsterAkkumulator" to MathematikAnschlussArten.Aussage.id, "wert" to MathematikAnschlussArten.Aussage.id),
    )
    val äußereKonjunktion = definitionsVorlagenKnoten(prefix, "aeussere-konjunktion", MathematikKnotenVorlagen.Konjunktion, GraphPunkt(2970f, 310f))
    val mengenDefinator = definitionsVorlagenKnoten(
        prefix, "mengendefinator", MengendefinitionKnotenVorlagen.Mengendefinator, GraphPunkt(3300f, 310f),
        parameter = mapOf(MENGENDEFINITION_PAAR to mengenPaarId),
    )
    val ausgang = definitionsVorlagenKnoten(
        prefix, "karten-ausgang", MathematikKnotenVorlagen.KartenAusgang, GraphPunkt(3630f, 310f),
        parameter = mapOf("name" to "menge"), anschlussArten = mapOf("wert" to MathematikAnschlussArten.Menge.id),
    )
    val knoten = listOf(
        methode, indexMenge, vereinigung, funktionsRaum, mengenKonstruktor, gImRaum, wahr,
        faltungsKonstruktor, gAnwendung, aAnwendung, element, innereKonjunktion,
        faltungsDefinator, äußereKonjunktion, mengenDefinator, ausgang,
    )
    val verbindungen = buildList {
        fun v(von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String, id: String) {
            add(definitionsVerbindung(prefix, von, vonName, zu, zuName, id))
        }
        v(methode, "wert", vereinigung, "methode", "a-vereinigung")
        v(indexMenge, "wert", vereinigung, "indexmenge", "i-vereinigung")
        v(vereinigung, "menge", funktionsRaum, "zielmenge", "u-funktionsraum")
        v(indexMenge, "wert", funktionsRaum, "definitionsmenge", "i-funktionsraum")
        v(mengenKonstruktor, "element", gImRaum, "links", "g-element")
        v(funktionsRaum, "menge", gImRaum, "rechts", "raum-element")
        v(indexMenge, "wert", faltungsKonstruktor, "indexmenge", "i-faltung")
        v(wahr, "aussage", faltungsKonstruktor, "neutral", "wahr-faltung")
        v(mengenKonstruktor, "element", gAnwendung, "methode", "g-anwendung-methode")
        v(faltungsKonstruktor, "index", gAnwendung, "argument", "i-g-anwendung")
        v(methode, "wert", aAnwendung, "methode", "a-anwendung-methode")
        v(faltungsKonstruktor, "index", aAnwendung, "argument", "i-a-anwendung")
        v(gAnwendung, "wert", element, "links", "g-i-element")
        v(aAnwendung, "wert", element, "rechts", "a-i-element")
        v(faltungsKonstruktor, "akkumulator", innereKonjunktion, "a", "akkumulator-konjunktion")
        v(element, "aussage", innereKonjunktion, "b", "element-konjunktion")
        v(innereKonjunktion, "aussage", faltungsDefinator, "nächsterAkkumulator", "konjunktion-faltungsdefinator")
        v(gImRaum, "aussage", äußereKonjunktion, "a", "raum-aeussere-konjunktion")
        v(faltungsDefinator, "wert", äußereKonjunktion, "b", "faltung-aeussere-konjunktion")
        v(äußereKonjunktion, "aussage", mengenDefinator, "aussage", "aeussere-konjunktion-mengendefinator")
        v(mengenDefinator, "menge", ausgang, "wert", "mengendefinator-ausgang")
    }
    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition von ${vorlage.name}",
        knoten = knoten,
        verbindungen = verbindungen,
    )
}

private fun konzeptEingang(
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

private fun definitionsVorlagenKnoten(
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

private fun definitionsVerbindung(
    prefix: String,
    von: KnotenDaten,
    vonName: String,
    zu: KnotenDaten,
    zuName: String,
    kennung: String,
): VerbindungDaten = VerbindungDaten(
    id = VerbindungsId("$prefix-$kennung"),
    von = AnschlussVerweis(von.id, von.anschluss(vonName, AnschlussRichtung.Ausgang).id),
    zu = AnschlussVerweis(zu.id, zu.anschluss(zuName, AnschlussRichtung.Eingang).id),
)

private fun KnotenDaten.anschluss(name: String, richtung: AnschlussRichtung): AnschlussDaten =
    anschlüsse.single { it.name == name && it.richtung == richtung }

private fun KnotenDaten.ausgangsName(): String =
    anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.name
