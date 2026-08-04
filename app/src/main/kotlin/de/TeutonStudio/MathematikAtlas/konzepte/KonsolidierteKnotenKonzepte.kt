package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

internal fun dynamischesKonzeptFürKnoten(zustand: AtlasZustand, knoten: KnotenDaten): KonzeptDefinition? {
    if (knoten.art == MENGEN_KNOTEN_ART) {
        val verweis = knoten.kartenVerweis ?: return null
        val karte = zustand.speicher.lade(verweis)
            ?: return fehlendesKartenKonzept(knoten, "Die referenzierte Kartenversion ${verweis.version} ist nicht mehr vorhanden.")
        return KonzeptDefinition(
            id = KonzeptId("menge-karte-${verweis.kartenId.wert}-${verweis.version}"),
            name = karte.name,
            beschreibung = "Eigene Mengendefinition als versionsfeste Kartenreferenz ${verweis.kartenId.wert}, Version ${verweis.version}.",
            pfad = listOf("Mengen", "Eigene Karten"),
            tags = setOf("Menge", "Eigene Karte", "Version ${verweis.version}"),
            knotenArten = setOf(MENGEN_KNOTEN_ART),
            reiter = listOf(KonzeptReiter("definition", "Definition · v${verweis.version}", KonzeptReiterRolle.Definition, karte)),
        )
    }
    if (istZahlenRechnerFormel(knoten)) return zahlenRechnerFormelKonzept(knoten)
    val familie = StrukturRechnerKnotenFamilie.fuerKnotenArt(knoten.art)
    if (familie != null && knoten.parameter[RECHNER_OPERATOR_PARAMETER] == familie.formelOperatorId) {
        return strukturRechnerKonzept(knoten, familie)
    }
    return null
}

private fun fehlendesKartenKonzept(knoten: KnotenDaten, fehler: String): KonzeptDefinition = KonzeptDefinition(
    id = KonzeptId("menge-fehlende-karte-${knoten.id.wert}"),
    name = knoten.name,
    beschreibung = fehler,
    pfad = listOf("Mengen", "Eigene Karten"),
    tags = setOf("Menge", "Eigene Karte", "Fehlende Version"),
    knotenArten = setOf(MENGEN_KNOTEN_ART),
    reiter = listOf(
        KonzeptReiter(
            id = "fehler",
            titel = "Fehlende Version",
            rolle = KonzeptReiterRolle.Definition,
            karte = KartenDaten(
                id = KartenId("fehlende-mengenkarte-${knoten.id.wert}"),
                name = "Fehlende Mengendefinition",
                knoten = listOf(
                    KnotenDaten(
                        id = KnotenId("fehlende-mengenkarte-${knoten.id.wert}-regel"),
                        art = KonzeptKnotenArten.REGEL,
                        name = "Referenz nicht auflösbar",
                        position = GraphPunkt(70f, 65f),
                        größe = GraphGröße(620f, 210f),
                        parameter = mapOf("regel" to fehler, "knotenArt" to MENGEN_KNOTEN_ART),
                    ),
                ),
            ),
        ),
    ),
)
