package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

internal fun endlicheMengeKonzept(ursprung: KnotenDaten): KonzeptDefinition {
    val modus = runCatching { endlicheMengeEingabe(ursprung) }.getOrElse { return endlicheMengeFehlerKonzept(ursprung, it.message.orEmpty()) }
    if (modus == ENDLICHE_MENGE_GRAPH) {
        runCatching {prüfeEndlicheMengeForm(ursprung)}.exceptionOrNull()?.let { return endlicheMengeFehlerKonzept(ursprung, it.message.orEmpty()) }
        val anzahl = runCatching { endlicheMengeGraphAnzahl(ursprung) }.getOrElse { return endlicheMengeFehlerKonzept(ursprung, it.message.orEmpty()) }
        val inputs = ursprung.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        if (inputs.map {it.name} != List(anzahl) {"element${it+1}"}) return endlicheMengeFehlerKonzept(ursprung, "Die Graphanschlüsse widersprechen der Elementanzahl.")
        val dynamisch=ursprung.anschlüsse.any {it.artFolgtEingang!=null || it.artVereinigtEingänge.isNotEmpty() || it.artAbbildungVonEingang!=null || it.artPriorisiertEingänge!=null || it.typInferenz!=null}
        if(dynamisch) return endlicheMengeFehlerKonzept(ursprung, "Die dynamischen Anschlussregeln besitzen noch keine nachgewiesene Graphdefinition.")
        return endlicheMengeGraphKonzept(ursprung, inputs)
    }
    val gelesen = leseEndlicheMengeKonfiguration(ursprung)
    val fehler = gelesen.fehler
    if (fehler != null) return endlicheMengeFehlerKonzept(ursprung, fehler)
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
                karte = endlicheMengeDefinitionsKarte(ursprung, normalisiert.konfiguration.einträge.map { ElementDefinitionsEingang(it.id, AnschlussArtId(it.art), "element-${it.id}") }),
            ),
        ),
    )
}

private data class ElementDefinitionsEingang(val id: String, val art: AnschlussArtId, val rolle: String, val vorlage: AnschlussDaten? = null)

private fun endlicheMengeGraphKonzept(ursprung: KnotenDaten, inputs: List<AnschlussDaten>): KonzeptDefinition = KonzeptDefinition(
    id = KonzeptId("endliche-menge-${ursprung.id.wert}"), name = "Endliche Menge",
    beschreibung = "Konstruiert die Menge aus den vollständigen Graph-Elementen; verschachtelte Objekte bleiben jeweils ein Element.",
    pfad = listOf("Mengenlehre", "Mengen"), tags = setOf("Endliche Menge", "Graph-Elemente", "Einzelmenge", "Vereinigung"),
    knotenArten = setOf(ENDLICHE_MENGE_ART),
    reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition,
        endlicheMengeDefinitionsKarte(ursprung, inputs.map { ElementDefinitionsEingang(it.name, it.art, it.name, it) }, ursprung.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}))),
)

private fun endlicheMengeDefinitionsKarte(
    ursprung: KnotenDaten,
    eintraege: List<ElementDefinitionsEingang>,
    ergebnisVorlage: AnschlussDaten? = null,
): KartenDaten {
    val prefix = "definition-endliche-menge-${ursprung.id.wert}"
    val knoten = mutableListOf<KnotenDaten>()
    val verbindungen = mutableListOf<VerbindungDaten>()

    val ausgangId = KnotenId("$prefix-ausgang")
    val ausgangAnschluss = AnschlussId("$prefix-ausgang-menge")
    val ausgang = KnotenDaten(
        id = ausgangId,
        art = if(ergebnisVorlage != null) "mathematik.kartenAusgang" else KonzeptKnotenArten.AUSGANG,
        name = "Menge",
        position = GraphPunkt(if (eintraege.size <= 1) 650f else 980f, 100f),
        größe = GraphGröße(210f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = ausgangAnschluss,
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = ergebnisVorlage?.art ?: MathematikAnschlussArten.Menge.id,
                vertrag = ergebnisVorlage?.vertrag ?: de.TeutonStudio.TypSystem.AnschlussVertrag(),
                semantischeId = ergebnisVorlage?.semantischeId,
                zulässigeArten = ergebnisVorlage?.zulässigeArten.orEmpty(),
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
        ).let(MathematikKartenMigrationen::nachLaden)
    }

    val einzelAusgaenge = mutableListOf<Pair<KnotenId, AnschlussId>>()
    eintraege.forEachIndexed { index, eintrag ->
        val safe = eintrag.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val y = 45f + index * 145f
        val inputId = KnotenId("$prefix-$safe-eingang")
        val inputOut = AnschlussId("$prefix-$safe-eingang-wert")
        val art = eintrag.art
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
                    vertrag = eintrag.vorlage?.vertrag ?: de.TeutonStudio.TypSystem.AnschlussVertrag(),
                    semantischeId = eintrag.vorlage?.semantischeId,
                    zulässigeArten = eintrag.vorlage?.zulässigeArten.orEmpty(),
                ),
            ),
            parameter = mapOf("typ" to art.wert, "rolle" to eintrag.rolle),
        )

        val einzelId = KnotenId("$prefix-$safe-einzelmenge")
        val einzelIn = AnschlussId("$prefix-$safe-einzelmenge-element")
        val einzelOut = AnschlussId("$prefix-$safe-einzelmenge-menge")
        knoten += KnotenDaten(
            id = einzelId,
            art = ENDLICHE_MENGE_ART,
            parameter = mapOf(ENDLICHE_MENGE_EINGABE to ENDLICHE_MENGE_GRAPH, ENDLICHE_MENGE_ANZAHL to "1"),
            name = "Einzelmenge",
            position = GraphPunkt(330f, y),
            größe = GraphGröße(220f, 105f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = einzelIn,
                    name = "element1",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = eintrag.vorlage?.art ?: MathematikAnschlussArten.Objekt.id,
                    vertrag = eintrag.vorlage?.vertrag ?: de.TeutonStudio.TypSystem.AnschlussVertrag(),
                    semantischeId = eintrag.vorlage?.semantischeId,
                    zulässigeArten = eintrag.vorlage?.zulässigeArten.orEmpty(),
                ),
                AnschlussDaten(
                    id = einzelOut,
                    name = "menge",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Menge.id,
                    vertrag = de.TeutonStudio.TypSystem.AnschlussVertrag(typ = ergebnisVorlage?.vertrag?.typ ?: de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt),
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
            art = de.TeutonStudio.MathematikRechenSystem.kern.MengenRechner.KNOTEN_ART,
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
            parameter = mapOf(MENGENRECHNER_OPERATOR_PARAMETER to "menge.vereinigung", "festeEingänge" to einzelAusgaenge.size.toString(), "operatorAnzeige" to "wert"),
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
    ).let(MathematikKartenMigrationen::nachLaden)
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
