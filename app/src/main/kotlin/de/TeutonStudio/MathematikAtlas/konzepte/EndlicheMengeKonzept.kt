package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

internal fun endlicheMengeKonzept(ursprung: KnotenDaten): KonzeptDefinition {
    val gelesen = leseEndlicheMengeKonfiguration(ursprung)
    if (gelesen.fehler != null) return endlicheMengeFehlerKonzept(ursprung, gelesen.fehler)
    val normalisiert = normalisiereEndlicheMengeKonfiguration(gelesen.konfiguration)
    return KonzeptDefinition(
        id = KonzeptId("endliche-menge-${ursprung.id.wert}"),
        name = "Endliche Menge",
        beschreibung = "Konstruiert die konfigurierte endliche Menge als Vereinigung ihrer Einzelmengen.",
        pfad = listOf("Mengenlehre", "Mengen"),
        tags = setOf("Endliche Menge", "Einzelmenge", "Vereinigung") + normalisiert.warnungen,
        knotenArten = setOf("mathematik.endlicheMenge"),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = "Definition",
                rolle = KonzeptReiterRolle.Definition,
                karte = endlicheMengeDefinitionsKarte(ursprung, normalisiert.konfiguration),
            ),
        ),
    )
}

private fun endlicheMengeDefinitionsKarte(
    ursprung: KnotenDaten,
    konfiguration: EndlicheMengeKonfiguration,
): KartenDaten {
    val prefix = "definition-endliche-menge-${ursprung.id.wert}"
    val eintraege = konfiguration.einträge
    val knoten = mutableListOf<KnotenDaten>()
    val verbindungen = mutableListOf<VerbindungDaten>()

    val ausgangId = KnotenId("$prefix-ausgang")
    val ausgangAnschluss = AnschlussId("$prefix-ausgang-menge")
    val ausgang = KnotenDaten(
        id = ausgangId,
        art = KonzeptKnotenArten.AUSGANG,
        name = "Menge",
        position = GraphPunkt(if (eintraege.size <= 1) 650f else 980f, 100f),
        größe = GraphGröße(210f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = ausgangAnschluss,
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
        parameter = mapOf("typ" to MathematikAnschlussArten.Menge.id.wert, "rolle" to "menge"),
    )

    if (eintraege.isEmpty()) {
        val leerId = KnotenId("$prefix-leer")
        val leerAusgang = AnschlussId("$prefix-leer-menge")
        knoten += KnotenDaten(
            id = leerId,
            art = "mathematik.leereMenge",
            name = "Leere Menge",
            position = GraphPunkt(320f, 100f),
            größe = GraphGröße(210f, 92f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = leerAusgang,
                    name = "menge",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Menge.id,
                ),
            ),
        )
        knoten += ausgang
        verbindungen += VerbindungDaten(
            id = VerbindungsId("$prefix-leer-ausgang"),
            von = AnschlussVerweis(leerId, leerAusgang),
            zu = AnschlussVerweis(ausgangId, ausgangAnschluss),
        )
        return KartenDaten(
            id = KartenId(prefix),
            name = "Definition: Endliche Menge",
            knoten = knoten,
            verbindungen = verbindungen,
        )
    }

    val einzelAusgaenge = mutableListOf<Pair<KnotenId, AnschlussId>>()
    eintraege.forEachIndexed { index, eintrag ->
        val safe = eintrag.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val y = 45f + index * 145f
        val inputId = KnotenId("$prefix-$safe-eingang")
        val inputOut = AnschlussId("$prefix-$safe-eingang-wert")
        val art = AnschlussArtId(eintrag.art)
        knoten += KnotenDaten(
            id = inputId,
            art = KonzeptKnotenArten.EINGANG,
            name = "Element ${index + 1}",
            position = GraphPunkt(35f, y),
            größe = GraphGröße(210f, 92f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = inputOut,
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = art,
                ),
            ),
            parameter = mapOf("typ" to art.wert, "rolle" to "element-${eintrag.id}"),
        )

        val einzelId = KnotenId("$prefix-$safe-einzelmenge")
        val einzelIn = AnschlussId("$prefix-$safe-einzelmenge-element")
        val einzelOut = AnschlussId("$prefix-$safe-einzelmenge-menge")
        knoten += KnotenDaten(
            id = einzelId,
            art = "mathematik.einzelmenge",
            name = "Einzelmenge",
            position = GraphPunkt(330f, y),
            größe = GraphGröße(220f, 105f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = einzelIn,
                    name = "element",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Objekt.id,
                ),
                AnschlussDaten(
                    id = einzelOut,
                    name = "menge",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Menge.id,
                ),
            ),
        )
        verbindungen += VerbindungDaten(
            id = VerbindungsId("$prefix-$safe-element"),
            von = AnschlussVerweis(inputId, inputOut),
            zu = AnschlussVerweis(einzelId, einzelIn),
        )
        einzelAusgaenge += einzelId to einzelOut
    }

    if (einzelAusgaenge.size == 1) {
        knoten += ausgang
        verbindungen += VerbindungDaten(
            id = VerbindungsId("$prefix-einzel-ausgang"),
            von = AnschlussVerweis(einzelAusgaenge.single().first, einzelAusgaenge.single().second),
            zu = AnschlussVerweis(ausgangId, ausgangAnschluss),
        )
    } else {
        val unionId = KnotenId("$prefix-vereinigung")
        val unionOut = AnschlussId("$prefix-vereinigung-menge")
        val unionInputs = einzelAusgaenge.indices.map { index ->
            AnschlussDaten(
                id = AnschlussId("$prefix-vereinigung-${index + 1}"),
                name = if (index == 0) "a" else if (index == 1) "b" else "input${index + 1}",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
                reihenfolge = index,
                kannSichErweitern = true,
            )
        }
        knoten += KnotenDaten(
            id = unionId,
            art = "mathematik.vereinigung",
            name = "Vereinigung",
            position = GraphPunkt(650f, 45f + ((einzelAusgaenge.size - 1) * 72f)),
            größe = GraphGröße(250f, 96f + einzelAusgaenge.size * 24f),
            anschlüsse = unionInputs + AnschlussDaten(
                id = unionOut,
                name = "menge",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Menge.id,
            ),
            parameter = mapOf("festeEingänge" to einzelAusgaenge.size.toString(), "operatorAnzeige" to "wert"),
        )
        einzelAusgaenge.forEachIndexed { index, quelle ->
            verbindungen += VerbindungDaten(
                id = VerbindungsId("$prefix-vereinigung-kante-${index + 1}"),
                von = AnschlussVerweis(quelle.first, quelle.second),
                zu = AnschlussVerweis(unionId, unionInputs[index].id),
            )
        }
        knoten += ausgang
        verbindungen += VerbindungDaten(
            id = VerbindungsId("$prefix-vereinigung-ausgang"),
            von = AnschlussVerweis(unionId, unionOut),
            zu = AnschlussVerweis(ausgangId, ausgangAnschluss),
        )
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition: Endliche Menge",
        knoten = knoten,
        verbindungen = verbindungen,
    )
}

private fun endlicheMengeFehlerKonzept(
    ursprung: KnotenDaten,
    fehler: String,
): KonzeptDefinition = KonzeptDefinition(
    id = KonzeptId("endliche-menge-fehler-${ursprung.id.wert}"),
    name = "Endliche Menge",
    beschreibung = "Die gespeicherte Elementkonfiguration ist beschädigt.",
    pfad = listOf("Mengenlehre", "Mengen"),
    tags = setOf("Endliche Menge", "Fehler"),
    knotenArten = setOf("mathematik.endlicheMenge"),
    reiter = listOf(
        KonzeptReiter(
            id = "fehler",
            titel = "Konfigurationsfehler",
            rolle = KonzeptReiterRolle.Definition,
            karte = KartenDaten(
                id = KartenId("definition-endliche-menge-fehler-${ursprung.id.wert}"),
                name = "Fehler: Endliche Menge",
                knoten = listOf(
                    KnotenDaten(
                        id = KnotenId("definition-endliche-menge-fehler-${ursprung.id.wert}-regel"),
                        art = KonzeptKnotenArten.REGEL,
                        name = "Ungültige Elementkonfiguration",
                        position = GraphPunkt(60f, 60f),
                        größe = GraphGröße(620f, 190f),
                        parameter = mapOf("regel" to fehler, "knotenArt" to "mathematik.endlicheMenge"),
                    ),
                ),
            ),
        ),
    ),
)
