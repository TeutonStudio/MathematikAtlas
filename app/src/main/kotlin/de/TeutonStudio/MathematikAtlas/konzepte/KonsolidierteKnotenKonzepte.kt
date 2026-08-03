package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_AUSWAHL
import de.TeutonStudio.MathematikKnoten.MengenKnotenAuswahl
import de.TeutonStudio.MathematikKnoten.MengenKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_OPERATOR
import de.TeutonStudio.MathematikKnoten.ZahlenRechnerKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

internal fun konzeptFürKonsolidiertenKnoten(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
): KonzeptDefinition? = when (knoten.art) {
    MENGEN_KNOTEN_ART -> mengenKnotenKonzept(zustand, knoten)
    ZAHLENRECHNER_ART -> zahlenRechnerKonzept(knoten)
    else -> null
}

private fun mengenKnotenKonzept(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
): KonzeptDefinition {
    knoten.kartenVerweis?.let { verweis ->
        val karte = zustand.speicher.lade(verweis)
            ?: return fehlendesKartenKonzept(knoten, "Die referenzierte Kartenversion ${verweis.version} ist nicht mehr vorhanden.")
        return KonzeptDefinition(
            id = KonzeptId("menge-karte-${verweis.kartenId.wert}-${verweis.version}"),
            name = karte.name,
            beschreibung = "Eigene Mengendefinition als versionsfeste Kartenreferenz ${verweis.kartenId.wert}, Version ${verweis.version}.",
            pfad = listOf("Mengen", "Eigene Karten"),
            tags = setOf("Menge", "Eigene Karte", "Version ${verweis.version}"),
            knotenArten = setOf(MENGEN_KNOTEN_ART),
            knotenParameter = mapOf(MENGEN_KNOTEN_AUSWAHL to knoten.parameter[MENGEN_KNOTEN_AUSWAHL].orEmpty()),
            reiter = listOf(
                KonzeptReiter(
                    id = "definition",
                    titel = "Definition · v${verweis.version}",
                    rolle = KonzeptReiterRolle.Definition,
                    karte = karte,
                ),
            ),
        )
    }

    val auswahl = MengenKnotenAuswahl.vonId(knoten.parameter[MENGEN_KNOTEN_AUSWAHL])
    val vorlage = MengenKnotenVorlagen.vorlage(auswahl)
    return KonzeptDefinition(
        id = KonzeptId("menge-${auswahl.stabileId.lowercase()}"),
        name = auswahl.titel,
        beschreibung = auswahl.beschreibung,
        pfad = listOf("Mengen", "Grundmengen"),
        tags = setOf("Menge", auswahl.titel, auswahl.stabileId),
        knotenArten = setOf(MENGEN_KNOTEN_ART),
        knotenParameter = mapOf(MENGEN_KNOTEN_AUSWAHL to auswahl.stabileId),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = "Definition",
                rolle = KonzeptReiterRolle.Definition,
                karte = mengenDefinitionsKarte(auswahl, vorlage),
            ),
            KonzeptReiter(
                id = "knotenvertrag",
                titel = "Knotenvertrag",
                rolle = KonzeptReiterRolle.Äquivalenz,
                karte = TestDefinitionsKarten.definitionsKarte(vorlage, 1),
            ),
        ),
    )
}

private fun mengenDefinitionsKarte(
    auswahl: MengenKnotenAuswahl,
    vorlage: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage,
): KartenDaten {
    val prefix = "definition-menge-${auswahl.stabileId.lowercase()}"
    val regel = KnotenDaten(
        id = KnotenId("$prefix-regel"),
        art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
        name = auswahl.menge().zuLatex(),
        position = GraphPunkt(80f, 70f),
        größe = GraphGröße(520f, 180f),
        parameter = mapOf(
            "regel" to auswahl.beschreibung,
            "definition" to auswahl.menge().zuLatex(),
            "knotenArt" to vorlage.art,
        ),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("$prefix-ausgang"),
                name = "menge",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten.Menge.id,
            ),
        ),
    )
    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition ${auswahl.titel}",
        knoten = listOf(regel),
    )
}

private fun fehlendesKartenKonzept(
    knoten: KnotenDaten,
    fehler: String,
): KonzeptDefinition = KonzeptDefinition(
    id = KonzeptId("menge-fehlende-karte-${knoten.id.wert}"),
    name = knoten.name,
    beschreibung = fehler,
    pfad = listOf("Mengen", "Eigene Karten"),
    knotenArten = setOf(MENGEN_KNOTEN_ART),
    reiter = listOf(
        KonzeptReiter(
            id = "fehler",
            titel = "Fehlende Version",
            rolle = KonzeptReiterRolle.Spezialfall,
            karte = hinweisKarte(
                id = "fehlende-mengenkarte-${knoten.id.wert}",
                name = "Fehlende Mengendefinition",
                titel = "Referenz nicht auflösbar",
                text = fehler,
            ),
        ),
    ),
)

private fun zahlenRechnerKonzept(knoten: KnotenDaten): KonzeptDefinition {
    val operator = UniversellerZahlenOperator.vonId(knoten.parameter[ZAHLENRECHNER_OPERATOR])
    val vorlage = ZahlenRechnerKnotenVorlagen.alle.first {
        UniversellerZahlenOperator.vonId(it.standardParameter[ZAHLENRECHNER_OPERATOR]) == operator
    }
    val basis = ZahlenRechnerDefinitionsKarten.konzept(listOf(vorlage))
    return basis.copy(
        reiter = basis.reiter + listOf(
            KonzeptReiter(
                id = "definitionsbereich",
                titel = "Definitionsbereich",
                rolle = KonzeptReiterRolle.Spezialfall,
                karte = hinweisKarte(
                    id = "zahlenrechner-${operator.stabileId}-definitionsbereich",
                    name = "Definitionsbereich ${operator.titel}",
                    titel = "Definiert, bedingt oder undefiniert",
                    text = definitionsbereichText(operator),
                ),
            ),
            KonzeptReiter(
                id = "definitionsluecken",
                titel = "Definitionslücken",
                rolle = KonzeptReiterRolle.Spezialfall,
                karte = hinweisKarte(
                    id = "zahlenrechner-${operator.stabileId}-luecken",
                    name = "Definitionslücken ${operator.titel}",
                    titel = "Strukturierter Nichtwert",
                    text = definitionsLueckenText(operator),
                ),
            ),
            KonzeptReiter(
                id = "aequivalente",
                titel = "Äquivalente",
                rolle = KonzeptReiterRolle.Äquivalenz,
                karte = hinweisKarte(
                    id = "zahlenrechner-${operator.stabileId}-aequivalente",
                    name = "Äquivalente Darstellungen ${operator.titel}",
                    titel = "Äquivalente Darstellung",
                    text = aequivalenzText(operator),
                ),
            ),
        ),
    )
}

private fun definitionsbereichText(operator: UniversellerZahlenOperator): String = when (operator) {
    UniversellerZahlenOperator.DIVISION,
    UniversellerZahlenOperator.KEHRWERT,
    -> "Der Nenner beziehungsweise Operand muss von null verschieden sein. Ist dies nur als Annahme bekannt, bleibt die Auswertung bedingt definiert."

    UniversellerZahlenOperator.MINIMUM,
    UniversellerZahlenOperator.MAXIMUM,
    -> "Alle Eingaben benötigen einen gemeinsamen geordneten Zahlbereich. Komplexe Zahlen und Quaternionen besitzen keine projektweit kanonische Ordnung."

    UniversellerZahlenOperator.QUADRATWURZEL,
    UniversellerZahlenOperator.WURZEL,
    -> "Der Definitionsbereich folgt dem gewählten Zahlbereich und dem Wurzelexponenten. Reelle Hauptwerte dürfen nicht stillschweigend komplex fortgesetzt werden."

    else -> "Der Rechner wählt aus den Eingangsbereichen die engste nachweislich gültige Operatordefinition. Unbekannte Bereiche bleiben bedingt statt vorsorglich auf den größten Bereich erweitert zu werden."
}

private fun definitionsLueckenText(operator: UniversellerZahlenOperator): String = when (operator) {
    UniversellerZahlenOperator.DIVISION,
    UniversellerZahlenOperator.KEHRWERT,
    -> "Bei einem nachweislich verschwindenden Nenner entsteht eine Definitionslücke mit Ursache „Division durch null“; sie ist kein Element eines Zahlbereichs."

    UniversellerZahlenOperator.MINIMUM,
    UniversellerZahlenOperator.MAXIMUM,
    -> "Fehlt eine kompatible Ordnung, wird die Operation als nicht anwendbar diagnostiziert und nicht als falscher Zahlenwert ausgegeben."

    else -> "Fehlt eine gültige bereichsspezifische Definition, liefert der Rechner einen strukturierten Nichtwert mit Operator, Eingängen und Ursache."
}

private fun aequivalenzText(operator: UniversellerZahlenOperator): String = when (operator) {
    UniversellerZahlenOperator.SUBTRAKTION -> "a-b ist bei vorhandener additiver Inverser äquivalent zu a+(-b)."
    UniversellerZahlenOperator.DIVISION -> "a/b ist bei b ungleich null äquivalent zu a·b^{-1}; in nichtkommutativen Bereichen bleibt die Seitenwahl Teil der Definition."
    UniversellerZahlenOperator.POTENZ -> "Natürliche Potenzen können als iterierte Multiplikation dargestellt werden; andere Exponenten benötigen eine eigene Bereichsdefinition."
    UniversellerZahlenOperator.QUADRAT -> "a² ist die spezialisierte Potenz a^2 und äquivalent zu a·a."
    UniversellerZahlenOperator.KUBIK -> "a³ ist die spezialisierte Potenz a^3 und äquivalent zu a·a·a."
    else -> "Äquivalente Darstellungen verändern weder Eingangsbereiche noch offene Definitionsbedingungen."
}

private fun hinweisKarte(
    id: String,
    name: String,
    titel: String,
    text: String,
): KartenDaten = KartenDaten(
    id = KartenId(id.replace('.', '-')),
    name = name,
    knoten = listOf(
        KnotenDaten(
            id = KnotenId("${id.replace('.', '-')}-regel"),
            art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
            name = titel,
            position = GraphPunkt(70f, 65f),
            größe = GraphGröße(620f, 210f),
            parameter = mapOf(
                "regel" to text,
                "knotenArt" to ZAHLENRECHNER_ART,
            ),
        ),
    ),
)
