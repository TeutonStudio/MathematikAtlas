package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Erzeugt die Definitionskarte eines Formelzustands aus dessen tatsächlichem Ausdrucksgraphen. */
internal fun zahlenRechnerFormelKonzept(knoten: KnotenDaten): KonzeptDefinition {
    require(istZahlenRechnerFormel(knoten))
    val latex = knoten.parameter[ZAHLENRECHNER_FORMEL_LATEX].orEmpty().ifBlank { "x" }
    val import = FormelLatexCodec.importiere(latex)
    val ausdruck = (import as? FormelLatexImportErgebnis.Erfolg)?.ausdruck
        ?: return fehlerhaftesFormelKonzept(
            knoten = knoten,
            latex = latex,
            nachricht = (import as FormelLatexImportErgebnis.Fehler).nachricht,
        )
    val graph = FormelZuGraph.konvertiere(ausdruck)
    val karte = formelDefinitionsKarte(knoten, latex, graph)
    return KonzeptDefinition(
        id = KonzeptId("zahlenrechner-formel-${latex.hashCode().toUInt()}"),
        name = "Formel",
        beschreibung = "Die gespeicherte Formel wird als Karte aus ihren freien Variablen, Literalen und Zahlenoperatoren aufgebaut.",
        pfad = listOf("Analysis", "Zahlen", "Formeln"),
        tags = setOf("Formel", "CAS", latex, ZAHLENRECHNER_FORMEL_ID),
        knotenArten = setOf(ZAHLENRECHNER_ART),
        knotenParameter = mapOf(
            ZAHLENRECHNER_OPERATOR to ZAHLENRECHNER_FORMEL_ID,
            ZAHLENRECHNER_FORMEL_LATEX to latex,
        ),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = latex,
                rolle = KonzeptReiterRolle.Definition,
                karte = karte,
            ),
        ),
    )
}

private fun formelDefinitionsKarte(
    ursprung: KnotenDaten,
    latex: String,
    graph: FormelGraph,
): KartenDaten {
    val prefix = "definition-formel-${ursprung.id.wert}-${latex.hashCode().toUInt()}"
    val eingehendeKanten = graph.kanten.groupBy(FormelGraphKante::zielKnotenId)
    val tiefen = mutableMapOf<String, Int>()

    fun tiefe(graphKnotenId: String): Int = tiefen.getOrPut(graphKnotenId) {
        val kinder = eingehendeKanten[graphKnotenId].orEmpty()
        if (kinder.isEmpty()) 0 else 1 + kinder.maxOf { tiefe(it.quelleKnotenId) }
    }

    val graphZuKartenKnoten = mutableMapOf<String, KnotenId>()
    val ebenen = sortedMapOf<Int, MutableList<KnotenDaten>>()
    val variablenKnoten = linkedMapOf<String, KnotenDaten>()

    fun fügeEin(ebene: Int, node: KnotenDaten) {
        ebenen.getOrPut(ebene) { mutableListOf() } += node
    }

    graph.knoten
        .filter { it.art == "formel.variable" }
        .sortedBy { it.parameter["name"].orEmpty() }
        .forEach { graphKnoten ->
            val name = graphKnoten.parameter["name"].orEmpty().ifBlank { "x" }
            val node = variablenKnoten.getOrPut(name) {
                formelEingang(prefix, name)
            }
            graphZuKartenKnoten[graphKnoten.id] = node.id
        }
    variablenKnoten.values.forEach { fügeEin(0, it) }

    graph.knoten
        .filter { it.art == "formel.literal" }
        .sortedBy(FormelGraphKnoten::id)
        .forEach { graphKnoten ->
            val node = formelLiteral(prefix, graphKnoten)
            graphZuKartenKnoten[graphKnoten.id] = node.id
            fügeEin(0, node)
        }

    graph.knoten
        .filter { it.art !in setOf("formel.variable", "formel.literal", "formel.platzhalter") }
        .sortedWith(compareBy<FormelGraphKnoten> { tiefe(it.id) }.thenBy { it.id })
        .forEach { graphKnoten ->
            val argumentAnzahl = eingehendeKanten[graphKnoten.id].orEmpty().size
            val node = formelOperator(prefix, graphKnoten, argumentAnzahl)
            graphZuKartenKnoten[graphKnoten.id] = node.id
            fügeEin(tiefe(graphKnoten.id), node)
        }

    graph.knoten
        .filter { it.art == "formel.platzhalter" }
        .forEach { graphKnoten ->
            val node = formelHinweisKnoten(
                prefix = prefix,
                idTeil = "platzhalter-${graphKnoten.ausdrucksId}",
                name = graphKnoten.parameter["beschriftung"].orEmpty().ifBlank { "Platzhalter" },
                eingänge = 0,
            )
            graphZuKartenKnoten[graphKnoten.id] = node.id
            fügeEin(0, node)
        }

    val positionierteKnoten = ebenen.flatMap { (ebene, knoten) ->
        knoten.distinctBy(KnotenDaten::id).mapIndexed { index, node ->
            node.copy(position = GraphPunkt(40f + ebene * 330f, 50f + index * 150f))
        }
    }
    val nachId = positionierteKnoten.associateBy(KnotenDaten::id)
    val wurzelId = requireNotNull(graphZuKartenKnoten[graph.wurzelKnotenId])
    val wurzel = requireNotNull(nachId[wurzelId])
    val maximaleTiefe = graph.knoten.maxOfOrNull { tiefe(it.id) } ?: 0
    val ausgang = formelAusgang(prefix).copy(
        position = GraphPunkt(40f + (maximaleTiefe + 1) * 330f, wurzel.position.y),
    )

    val verbindungen = buildList {
        graph.kanten.sortedWith(compareBy<FormelGraphKante> { it.zielKnotenId }.thenBy { it.position })
            .forEachIndexed { index, kante ->
                val quelle = nachId[graphZuKartenKnoten[kante.quelleKnotenId]] ?: return@forEachIndexed
                val ziel = nachId[graphZuKartenKnoten[kante.zielKnotenId]] ?: return@forEachIndexed
                val quelleAnschluss = quelle.anschlüsse
                    .firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
                    ?: return@forEachIndexed
                val zielAnschluss = ziel.anschlüsse
                    .filter { it.richtung == AnschlussRichtung.Eingang }
                    .sortedBy { it.reihenfolge }
                    .getOrNull(kante.position)
                    ?: return@forEachIndexed
                add(
                    VerbindungDaten(
                        id = VerbindungsId("$prefix-kante-$index"),
                        von = AnschlussVerweis(quelle.id, quelleAnschluss.id),
                        zu = AnschlussVerweis(ziel.id, zielAnschluss.id),
                    ),
                )
            }
        val wurzelAusgang = wurzel.anschlüsse.first { it.richtung == AnschlussRichtung.Ausgang }
        val zielEingang = ausgang.anschlüsse.single()
        add(
            VerbindungDaten(
                id = VerbindungsId("$prefix-ergebnis"),
                von = AnschlussVerweis(wurzel.id, wurzelAusgang.id),
                zu = AnschlussVerweis(ausgang.id, zielEingang.id),
            ),
        )
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definitionskarte: $latex",
        knoten = positionierteKnoten + ausgang,
        verbindungen = verbindungen,
    )
}

private fun formelOperator(
    prefix: String,
    graphKnoten: FormelGraphKnoten,
    argumentAnzahl: Int,
): KnotenDaten {
    val operatorId = graphKnoten.art
    val standard = UniversellerZahlenOperator.entries.firstOrNull { operator ->
        operatorId == operator.stabileId || operatorId.equals(operator.name, ignoreCase = true)
    }
    val erweitert = ErweiterterZahlenOperator.vonId(operatorId)
    val basis = KnotenDaten(
        id = KnotenId("$prefix-operator-${sichererIdTeil(graphKnoten.ausdrucksId)}"),
        art = ZAHLENRECHNER_ART,
        name = standard?.titel ?: erweitert?.titel ?: operatorId.substringAfterLast('.'),
        position = GraphPunkt.Zero,
        größe = GraphGröße(260f, maxOf(110f, 76f + argumentAnzahl * 28f)),
    )
    return when {
        standard != null -> konfiguriereZahlenRechner(
            knoten = basis,
            operator = standard,
            festeEingänge = argumentAnzahl.coerceAtLeast(2),
        )
        erweitert != null -> konfiguriereErweitertenZahlenRechner(basis, erweitert)
        else -> formelHinweisKnoten(
            prefix = prefix,
            idTeil = "operator-${graphKnoten.ausdrucksId}",
            name = operatorId,
            eingänge = argumentAnzahl,
        )
    }
}

private fun formelEingang(prefix: String, name: String): KnotenDaten {
    val id = "$prefix-eingang-${sichererIdTeil(name)}-${name.hashCode().toUInt()}"
    return KnotenDaten(
        id = KnotenId(id),
        art = TestDefinitionsKarten.KONZEPT_EINGANG_ART,
        name = name,
        position = GraphPunkt.Zero,
        größe = GraphGröße(230f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("$id-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        parameter = mapOf(
            "typ" to MathematikAnschlussArten.Zahl.id.wert,
            "rolle" to name,
        ),
    )
}

private fun formelLiteral(prefix: String, graphKnoten: FormelGraphKnoten): KnotenDaten {
    val latex = graphKnoten.parameter["latex"].orEmpty()
    val id = "$prefix-literal-${sichererIdTeil(graphKnoten.ausdrucksId)}"
    return KnotenDaten(
        id = KnotenId(id),
        art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
        name = latex,
        position = GraphPunkt.Zero,
        größe = GraphGröße(230f, 110f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("$id-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        parameter = mapOf(
            "regel" to "Konstante $latex",
            "definition" to latex,
        ),
    )
}

private fun formelHinweisKnoten(
    prefix: String,
    idTeil: String,
    name: String,
    eingänge: Int,
): KnotenDaten {
    val id = "$prefix-${sichererIdTeil(idTeil)}"
    return KnotenDaten(
        id = KnotenId(id),
        art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
        name = name,
        position = GraphPunkt.Zero,
        größe = GraphGröße(260f, maxOf(110f, 76f + eingänge * 28f)),
        anschlüsse = List(eingänge) { index ->
            AnschlussDaten(
                id = AnschlussId("$id-eingang-$index"),
                name = "argument${index + 1}",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Zahl.id,
                reihenfolge = index,
            )
        } + AnschlussDaten(
            id = AnschlussId("$id-wert"),
            name = "wert",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Zahl.id,
        ),
        parameter = mapOf(
            "regel" to name,
            "operator" to name,
        ),
    )
}

private fun formelAusgang(prefix: String): KnotenDaten {
    val id = "$prefix-ausgang"
    return KnotenDaten(
        id = KnotenId(id),
        art = TestDefinitionsKarten.KONZEPT_AUSGANG_ART,
        name = "wert",
        position = GraphPunkt.Zero,
        größe = GraphGröße(230f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("$id-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        parameter = mapOf("typ" to MathematikAnschlussArten.Zahl.id.wert),
    )
}

private fun fehlerhaftesFormelKonzept(
    knoten: KnotenDaten,
    latex: String,
    nachricht: String,
): KonzeptDefinition {
    val prefix = "definition-formel-fehler-${knoten.id.wert}"
    val karte = KartenDaten(
        id = KartenId(prefix),
        name = "Ungültige Formel",
        knoten = listOf(
            KnotenDaten(
                id = KnotenId("$prefix-regel"),
                art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
                name = latex,
                position = GraphPunkt(60f, 60f),
                größe = GraphGröße(620f, 210f),
                parameter = mapOf("regel" to nachricht, "definition" to latex),
            ),
        ),
    )
    return KonzeptDefinition(
        id = KonzeptId("zahlenrechner-formel-fehler-${knoten.id.wert}"),
        name = "Formel",
        beschreibung = "Die gespeicherte Formel konnte nicht in einen Ausdrucksgraphen übersetzt werden.",
        pfad = listOf("Analysis", "Zahlen", "Formeln"),
        tags = setOf("Formel", "Fehler"),
        knotenArten = setOf(ZAHLENRECHNER_ART),
        knotenParameter = mapOf(ZAHLENRECHNER_OPERATOR to ZAHLENRECHNER_FORMEL_ID),
        reiter = listOf(
            KonzeptReiter("definition", "Fehler", KonzeptReiterRolle.Definition, karte),
        ),
    )
}

private fun sichererIdTeil(text: String): String = text
    .map { zeichen -> if (zeichen.isLetterOrDigit()) zeichen else '-' }
    .joinToString("")
    .replace(Regex("-+"), "-")
    .trim('-')
    .ifBlank { "ausdruck" }
