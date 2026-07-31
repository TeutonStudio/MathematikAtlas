package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_ELEMENTART
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_ELEMENTNAME
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_MENGENNAME
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_PAAR
import de.TeutonStudio.MathematikKnoten.*

private data class MengenoperatorDefinitionsKonfiguration(
    val ergebnisLatex: String,
    val logikVorlage: KnotenVorlage,
    val negiertRechts: Boolean = false,
)

/**
 * Ausführbare und selbstbezugfreie Definition eines binären Mengenoperators.
 * Der erklärte Operator selbst kommt im Graphen absichtlich nicht vor.
 */
internal fun mengenoperatorDefinitionsKarte(
    vorlage: KnotenVorlage,
    variantenIndex: Int,
): KartenDaten {
    val konfiguration = when (vorlage.art) {
        MathematikKnotenVorlagen.Vereinigung.art -> MengenoperatorDefinitionsKonfiguration(
            ergebnisLatex = "A\\cup B",
            logikVorlage = MathematikKnotenVorlagen.Disjunktion,
        )
        MathematikKnotenVorlagen.Schnitt.art -> MengenoperatorDefinitionsKonfiguration(
            ergebnisLatex = "A\\cap B",
            logikVorlage = MathematikKnotenVorlagen.Konjunktion,
        )
        MathematikKnotenVorlagen.Differenz.art -> MengenoperatorDefinitionsKonfiguration(
            ergebnisLatex = "A\\setminus B",
            logikVorlage = MathematikKnotenVorlagen.Konjunktion,
            negiertRechts = true,
        )
        MengenraumKnotenVorlagen.SymmetrischeDifferenz.art -> MengenoperatorDefinitionsKonfiguration(
            ergebnisLatex = "A\\triangle B",
            logikVorlage = AussagenLogikKnotenVorlagen.Adjunktion,
        )
        else -> error("Für ${vorlage.art} ist keine Mengenoperator-Definitionskarte vorgesehen.")
    }

    val prefix = "definition-${vorlage.art.removePrefix("mathematik.")}-$variantenIndex"
    val paarId = "$prefix-paar"
    val eingangA = mengenEingang(prefix, "A", GraphPunkt(20f, 70f), 0)
    val eingangB = mengenEingang(prefix, "B", GraphPunkt(20f, 450f), 1)
    val konstruktor = definitionsKnoten(
        prefix = prefix,
        kennung = "mengenkonstruktor",
        vorlage = MengendefinitionKnotenVorlagen.Mengenkonstruktor,
        position = GraphPunkt(270f, 260f),
        parameter = mapOf(
            MENGENDEFINITION_PAAR to paarId,
            MENGENDEFINITION_MENGENNAME to konfiguration.ergebnisLatex,
            MENGENDEFINITION_ELEMENTNAME to "x",
            MENGENDEFINITION_ELEMENTART to MathematikAnschlussArten.Objekt.id.wert,
        ),
        anschlussArten = mapOf("element" to MathematikAnschlussArten.Objekt.id),
    )
    val elementA = definitionsKnoten(
        prefix,
        "element-a",
        MathematikKnotenVorlagen.Element,
        GraphPunkt(590f, 80f),
    )
    val elementB = definitionsKnoten(
        prefix,
        "element-b",
        MathematikKnotenVorlagen.Element,
        GraphPunkt(590f, 450f),
    )
    val negation = if (konfiguration.negiertRechts) {
        definitionsKnoten(
            prefix,
            "negation-b",
            AussagenLogikKnotenVorlagen.Negation,
            GraphPunkt(930f, 450f),
        )
    } else {
        null
    }
    val logik = definitionsKnoten(
        prefix,
        "praedikat",
        konfiguration.logikVorlage,
        GraphPunkt(if (negation == null) 940f else 1240f, 265f),
    )
    val definator = definitionsKnoten(
        prefix = prefix,
        kennung = "mengendefinator",
        vorlage = MengendefinitionKnotenVorlagen.Mengendefinator,
        position = GraphPunkt(if (negation == null) 1280f else 1580f, 260f),
        parameter = mapOf(MENGENDEFINITION_PAAR to paarId),
    )
    val ausgang = definitionsKnoten(
        prefix = prefix,
        kennung = "karten-ausgang",
        vorlage = MathematikKnotenVorlagen.KartenAusgang,
        position = GraphPunkt(if (negation == null) 1630f else 1930f, 265f),
        parameter = mapOf("name" to "menge"),
        anschlussArten = mapOf("wert" to MathematikAnschlussArten.Menge.id),
    )

    val verbindungen = buildList {
        fun verbinde(
            von: KnotenDaten,
            vonName: String,
            zu: KnotenDaten,
            zuName: String,
            kennung: String,
        ) {
            add(
                VerbindungDaten(
                    id = VerbindungsId("$prefix-$kennung"),
                    von = AnschlussVerweis(
                        von.id,
                        von.anschluss(vonName, AnschlussRichtung.Ausgang).id,
                    ),
                    zu = AnschlussVerweis(
                        zu.id,
                        zu.anschluss(zuName, AnschlussRichtung.Eingang).id,
                    ),
                ),
            )
        }

        verbinde(konstruktor, "element", elementA, "links", "x-element-a")
        verbinde(konstruktor, "element", elementB, "links", "x-element-b")
        verbinde(eingangA, "wert", elementA, "rechts", "a-element-a")
        verbinde(eingangB, "wert", elementB, "rechts", "b-element-b")
        verbinde(elementA, "aussage", logik, "a", "element-a-praedikat")
        if (negation == null) {
            verbinde(elementB, "aussage", logik, "b", "element-b-praedikat")
        } else {
            verbinde(elementB, "aussage", negation, "aussage", "element-b-negation")
            verbinde(negation, "aussage", logik, "b", "negation-praedikat")
        }
        verbinde(logik, "aussage", definator, "aussage", "praedikat-definator")
        verbinde(definator, "menge", ausgang, "wert", "definator-ausgang")
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition von ${vorlage.name}",
        knoten = listOfNotNull(
            eingangA,
            eingangB,
            konstruktor,
            elementA,
            elementB,
            negation,
            logik,
            definator,
            ausgang,
        ),
        verbindungen = verbindungen,
    )
}

private fun mengenEingang(
    prefix: String,
    name: String,
    position: GraphPunkt,
    index: Int,
): KnotenDaten {
    val id = KnotenId("$prefix-eingang-$index")
    return KnotenDaten(
        id = id,
        art = TestDefinitionsKarten.KONZEPT_EINGANG_ART,
        name = name,
        position = position,
        größe = GraphGröße(220f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("${id.wert}-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
        parameter = mapOf(
            "typ" to MathematikAnschlussArten.Menge.id.wert,
            "variabel" to "false",
            "folgtEingang" to "",
        ),
    )
}

private fun definitionsKnoten(
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

private fun KnotenDaten.anschluss(
    name: String,
    richtung: AnschlussRichtung,
): AnschlussDaten = anschlüsse.single { it.name == name && it.richtung == richtung }
