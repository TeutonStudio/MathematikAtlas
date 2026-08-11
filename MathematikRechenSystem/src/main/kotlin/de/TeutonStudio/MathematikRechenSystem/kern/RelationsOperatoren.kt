package de.TeutonStudio.MathematikRechenSystem.kern

enum class RelationsArgumentArt {
    OBJEKT,
    ZAHL,
    MENGE,
    GEOMETRIE_OBJEKT,
    PUNKT,
    GERADE,
    STRECKE,
    WINKEL,
}

data class RelationsArgument(
    val rolle: String,
    val art: RelationsArgumentArt,
)

enum class RelationsAxiom {
    REFLEXIV,
    IRREFLEXIV,
    SYMMETRISCH,
    ANTISYMMETRISCH,
    ASYMMETRISCH,
    TRANSITIV,
    TOTAL,
}

data class RelationsAxiomNachweis(
    val axiom: RelationsAxiom,
    val status: NachweisStatus,
    val traeger: MengenAusdruck? = null,
    val bedingungen: List<Aussage> = emptyList(),
)

enum class RelationsKlasse(val titel: String) {
    AEQUIVALENZRELATION("Äquivalenzrelation"),
    PRAEORDNUNG("Präordnung"),
    HALBORDNUNG("Halbordnung"),
    TOTALORDNUNG("Totalordnung"),
    STRIKTE_HALBORDNUNG("strikte Halbordnung"),
    STRIKTE_TOTALORDNUNG("strikte Totalordnung"),
}

data class RelationsStruktur(
    val nachweise: List<RelationsAxiomNachweis>,
) {
    fun status(axiom: RelationsAxiom): NachweisStatus =
        nachweise.firstOrNull { it.axiom == axiom }?.status ?: NachweisStatus.Unvollstaendig

    fun istNachgewiesen(axiom: RelationsAxiom): Boolean = status(axiom) == NachweisStatus.Nachgewiesen

    fun klassen(): Set<RelationsKlasse> = buildSet {
        val reflexiv = istNachgewiesen(RelationsAxiom.REFLEXIV)
        val irreflexiv = istNachgewiesen(RelationsAxiom.IRREFLEXIV)
        val symmetrisch = istNachgewiesen(RelationsAxiom.SYMMETRISCH)
        val antisymmetrisch = istNachgewiesen(RelationsAxiom.ANTISYMMETRISCH)
        val transitiv = istNachgewiesen(RelationsAxiom.TRANSITIV)
        val total = istNachgewiesen(RelationsAxiom.TOTAL)

        if (reflexiv && symmetrisch && transitiv) add(RelationsKlasse.AEQUIVALENZRELATION)
        if (reflexiv && transitiv) add(RelationsKlasse.PRAEORDNUNG)
        if (reflexiv && antisymmetrisch && transitiv) add(RelationsKlasse.HALBORDNUNG)
        if (reflexiv && antisymmetrisch && transitiv && total) add(RelationsKlasse.TOTALORDNUNG)
        if (irreflexiv && transitiv) add(RelationsKlasse.STRIKTE_HALBORDNUNG)
        if (irreflexiv && transitiv && total) add(RelationsKlasse.STRIKTE_TOTALORDNUNG)
    }

    fun kompakteKlassen(): List<RelationsKlasse> {
        val klassen = klassen()
        return when {
            RelationsKlasse.TOTALORDNUNG in klassen -> listOf(RelationsKlasse.TOTALORDNUNG)
            RelationsKlasse.STRIKTE_TOTALORDNUNG in klassen -> listOf(RelationsKlasse.STRIKTE_TOTALORDNUNG)
            RelationsKlasse.AEQUIVALENZRELATION in klassen -> listOf(RelationsKlasse.AEQUIVALENZRELATION)
            RelationsKlasse.HALBORDNUNG in klassen -> listOf(RelationsKlasse.HALBORDNUNG)
            RelationsKlasse.STRIKTE_HALBORDNUNG in klassen -> listOf(RelationsKlasse.STRIKTE_HALBORDNUNG)
            RelationsKlasse.PRAEORDNUNG in klassen -> listOf(RelationsKlasse.PRAEORDNUNG)
            else -> emptyList()
        }
    }
}

data class RelationsOperatorDefinition(
    val stabileId: String,
    val titel: String,
    val kategorie: String,
    val symbolLatex: String,
    val argumente: List<RelationsArgument>,
    val suchbegriffe: Set<String> = emptySet(),
    val relationsStruktur: RelationsStruktur? = null,
    val auswerter: (List<MathematischesObjekt>) -> Aussage,
) {
    init {
        require(stabileId.isNotBlank())
        require(titel.isNotBlank())
        require(argumente.isNotEmpty())
        require(argumente.map { it.rolle }.distinct().size == argumente.size)
    }

    fun werteAus(argumenteNachRolle: Map<String, MathematischesObjekt>): Aussage = auswerter(
        argumente.map { argument ->
            argumenteNachRolle[argument.rolle]
                ?: error("Für die Relation '$titel' fehlt das Argument '${argument.rolle}'.")
        },
    )
}

object RelationsOperatoren {
    const val KNOTEN_ART = "mathematik.praedikat"
    const val OPERATOR_PARAMETER = "operator"

    private fun a(rolle: String, art: RelationsArgumentArt) = RelationsArgument(rolle, art)
    private fun n(axiom: RelationsAxiom) = RelationsAxiomNachweis(axiom, NachweisStatus.Nachgewiesen)
    private fun w(axiom: RelationsAxiom) = RelationsAxiomNachweis(axiom, NachweisStatus.Widerlegt)

    private val gleichheitsStruktur = RelationsStruktur(
        listOf(
            n(RelationsAxiom.REFLEXIV),
            n(RelationsAxiom.SYMMETRISCH),
            n(RelationsAxiom.TRANSITIV),
        ),
    )
    private val ungleichheitsStruktur = RelationsStruktur(
        listOf(
            n(RelationsAxiom.IRREFLEXIV),
            n(RelationsAxiom.SYMMETRISCH),
            w(RelationsAxiom.TRANSITIV),
        ),
    )
    private val echteMengenordnung = RelationsStruktur(
        listOf(
            n(RelationsAxiom.IRREFLEXIV),
            n(RelationsAxiom.ASYMMETRISCH),
            n(RelationsAxiom.TRANSITIV),
        ),
    )
    private val mengenHalbordnung = RelationsStruktur(
        listOf(
            n(RelationsAxiom.REFLEXIV),
            n(RelationsAxiom.ANTISYMMETRISCH),
            n(RelationsAxiom.TRANSITIV),
        ),
    )
    private val symmetrisch = RelationsStruktur(listOf(n(RelationsAxiom.SYMMETRISCH)))

    val alle: List<RelationsOperatorDefinition> = listOf(
        RelationsOperatorDefinition(
            stabileId = "relation.gleichheit",
            titel = "Gleichheit",
            kategorie = "Allgemein",
            symbolLatex = "A=B",
            argumente = listOf(a("links", RelationsArgumentArt.OBJEKT), a("rechts", RelationsArgumentArt.OBJEKT)),
            suchbegriffe = setOf("gleich", "=", "equivalent"),
            relationsStruktur = gleichheitsStruktur,
        ) { werte -> Gleichheit(werte[0], werte[1]) },
        RelationsOperatorDefinition(
            stabileId = "relation.ungleichheit",
            titel = "Ungleichheit",
            kategorie = "Allgemein",
            symbolLatex = "A\\neq B",
            argumente = listOf(a("links", RelationsArgumentArt.OBJEKT), a("rechts", RelationsArgumentArt.OBJEKT)),
            suchbegriffe = setOf("ungleich", "!=", "≠"),
            relationsStruktur = ungleichheitsStruktur,
        ) { werte -> Ungleichheit(werte[0], werte[1]) },
        vergleich(
            id = "relation.kleiner",
            titel = "Kleiner",
            latex = "a<b",
            art = VergleichsArt.Kleiner,
            suchbegriffe = setOf("kleiner als", "<"),
        ),
        vergleich(
            id = "relation.groesser",
            titel = "Größer",
            latex = "a>b",
            art = VergleichsArt.Größer,
            suchbegriffe = setOf("größer als", ">", "groesser"),
        ),
        vergleich(
            id = "relation.kleinerGleich",
            titel = "Kleiner oder gleich",
            latex = "a\\le b",
            art = VergleichsArt.KleinerGleich,
            suchbegriffe = setOf("kleiner gleich", "<=", "≤"),
        ),
        vergleich(
            id = "relation.groesserGleich",
            titel = "Größer oder gleich",
            latex = "a\\ge b",
            art = VergleichsArt.GrößerGleich,
            suchbegriffe = setOf("größer gleich", ">=", "≥", "groesser gleich"),
        ),
        RelationsOperatorDefinition(
            stabileId = MengenRelationsOperator.ELEMENT.stabileId,
            titel = "Element",
            kategorie = "Mengen",
            symbolLatex = "x\\in A",
            argumente = listOf(a("element", RelationsArgumentArt.OBJEKT), a("menge", RelationsArgumentArt.MENGE)),
            suchbegriffe = setOf("element von", "in", "∈"),
        ) { werte -> MengenRelationRechner.erzeuge(MengenRelationsOperator.ELEMENT, werte[0], werte[1]) },
        mengenRelation(MengenRelationsOperator.TEILMENGE, echteMengenordnung, setOf("subset", "⊂", "echte teilmenge")),
        mengenRelation(MengenRelationsOperator.UEBERMENGE, echteMengenordnung, setOf("superset", "⊃", "echte obermenge")),
        mengenRelation(MengenRelationsOperator.TEIL_ODER_GLEICHMENGE, mengenHalbordnung, setOf("subseteq", "⊆", "teil oder gleichmenge")),
        mengenRelation(MengenRelationsOperator.UEBER_ODER_GLEICHMENGE, mengenHalbordnung, setOf("superseteq", "⊇", "ober oder gleichmenge")),
        mengenRelation(MengenRelationsOperator.DISJUNKT, symmetrisch, setOf("disjoint", "schnitt leer")),
        RelationsOperatorDefinition(
            stabileId = "geometrie.relation.inzidenz",
            titel = "Inzidenz",
            kategorie = "Geometrie",
            symbolLatex = "P\\mathrel{\\mathbf I}g",
            argumente = listOf(a("punkt", RelationsArgumentArt.PUNKT), a("objekt", RelationsArgumentArt.GEOMETRIE_OBJEKT)),
            suchbegriffe = setOf("liegt auf", "inzident"),
        ) { werte -> GeometrischeInzidenz(werte[0] as GeometriePunkt, werte[1] as GeometrischerAusdruck) },
        RelationsOperatorDefinition(
            stabileId = "geometrie.relation.zwischenlage",
            titel = "Zwischenlage",
            kategorie = "Geometrie",
            symbolLatex = "\\operatorname{Zwischen}(A,B,C)",
            argumente = listOf(a("a", RelationsArgumentArt.PUNKT), a("b", RelationsArgumentArt.PUNKT), a("c", RelationsArgumentArt.PUNKT)),
            suchbegriffe = setOf("zwischen", "betweenness"),
        ) { werte -> Zwischenlage(werte[0] as GeometriePunkt, werte[1] as GeometriePunkt, werte[2] as GeometriePunkt) },
        RelationsOperatorDefinition(
            stabileId = "geometrie.relation.kollinear",
            titel = "Kollinearität",
            kategorie = "Geometrie",
            symbolLatex = "\\operatorname{kollinear}(A,B,C)",
            argumente = listOf(a("a", RelationsArgumentArt.PUNKT), a("b", RelationsArgumentArt.PUNKT), a("c", RelationsArgumentArt.PUNKT)),
            suchbegriffe = setOf("kollinear", "auf einer geraden"),
        ) { werte -> Kollinearität(werte.map { it as GeometriePunkt }) },
        RelationsOperatorDefinition(
            stabileId = "geometrie.relation.parallel",
            titel = "Parallelität",
            kategorie = "Geometrie",
            symbolLatex = "g\\parallel h",
            argumente = listOf(a("links", RelationsArgumentArt.GERADE), a("rechts", RelationsArgumentArt.GERADE)),
            suchbegriffe = setOf("parallel"),
        ) { werte -> GeometrischeParallelität(werte[0] as GeometrieGerade, werte[1] as GeometrieGerade) },
        RelationsOperatorDefinition(
            stabileId = "geometrie.relation.orthogonal",
            titel = "Orthogonalität",
            kategorie = "Geometrie",
            symbolLatex = "g\\perp h",
            argumente = listOf(a("links", RelationsArgumentArt.GERADE), a("rechts", RelationsArgumentArt.GERADE)),
            suchbegriffe = setOf("orthogonal", "senkrecht", "perpendicular"),
            relationsStruktur = symmetrisch,
        ) { werte -> GeometrischeOrthogonalität(werte[0] as GeometrieGerade, werte[1] as GeometrieGerade) },
        RelationsOperatorDefinition(
            stabileId = "geometrie.relation.gleichheit",
            titel = "Geometrische Gleichheit",
            kategorie = "Geometrie",
            symbolLatex = "A\\equiv_G B",
            argumente = listOf(a("links", RelationsArgumentArt.GEOMETRIE_OBJEKT), a("rechts", RelationsArgumentArt.GEOMETRIE_OBJEKT)),
            suchbegriffe = setOf("geometrisch gleich", "koinzidenz"),
            relationsStruktur = gleichheitsStruktur,
        ) { werte -> GeometrischeGleichheit(werte[0] as GeometrischerAusdruck, werte[1] as GeometrischerAusdruck) },
        RelationsOperatorDefinition(
            stabileId = "geometrie.relation.streckenkongruenz",
            titel = "Streckenkongruenz",
            kategorie = "Geometrie",
            symbolLatex = "s_1\\cong s_2",
            argumente = listOf(a("links", RelationsArgumentArt.STRECKE), a("rechts", RelationsArgumentArt.STRECKE)),
            suchbegriffe = setOf("kongruente strecken", "gleiche länge"),
            relationsStruktur = gleichheitsStruktur,
        ) { werte -> StreckenKongruenz(werte[0] as GeometrieStrecke, werte[1] as GeometrieStrecke) },
        RelationsOperatorDefinition(
            stabileId = "geometrie.relation.winkelkongruenz",
            titel = "Winkelkongruenz",
            kategorie = "Geometrie",
            symbolLatex = "\\alpha\\cong\\beta",
            argumente = listOf(a("links", RelationsArgumentArt.WINKEL), a("rechts", RelationsArgumentArt.WINKEL)),
            suchbegriffe = setOf("kongruente winkel", "gleicher winkel"),
            relationsStruktur = gleichheitsStruktur,
        ) { werte -> WinkelKongruenz(werte[0] as GeometrieWinkel, werte[1] as GeometrieWinkel) },
    )

    private val nachId: Map<String, RelationsOperatorDefinition> = buildMap {
        alle.forEach { definition ->
            put(definition.stabileId, definition)
            put(definition.stabileId.lowercase(), definition)
        }
        put("mathematik.gleichheit", alle.first { it.stabileId == "relation.gleichheit" })
        put("mathematik.ungleichheit", alle.first { it.stabileId == "relation.ungleichheit" })
        put("mathematik.kleiner", alle.first { it.stabileId == "relation.kleiner" })
        put("mathematik.größer", alle.first { it.stabileId == "relation.groesser" })
        put("mathematik.groesser", alle.first { it.stabileId == "relation.groesser" })
        put("mathematik.kleinerGleich", alle.first { it.stabileId == "relation.kleinerGleich" })
        put("mathematik.größerGleich", alle.first { it.stabileId == "relation.groesserGleich" })
        put("mathematik.groesserGleich", alle.first { it.stabileId == "relation.groesserGleich" })
    }

    fun vonIdOderNull(id: String?): RelationsOperatorDefinition? {
        val wert = id?.trim().orEmpty()
        if (wert.isEmpty()) return null
        return nachId[wert] ?: nachId[wert.lowercase()]
    }

    fun standard(): RelationsOperatorDefinition = alle.first { it.stabileId == "relation.gleichheit" }

    private fun vergleich(
        id: String,
        titel: String,
        latex: String,
        art: VergleichsArt,
        suchbegriffe: Set<String>,
    ) = RelationsOperatorDefinition(
        stabileId = id,
        titel = titel,
        kategorie = "Ordnung",
        symbolLatex = latex,
        argumente = listOf(a("links", RelationsArgumentArt.ZAHL), a("rechts", RelationsArgumentArt.ZAHL)),
        suchbegriffe = suchbegriffe,
        // Ob <, <=, > und >= eine (strikte) Totalordnung bilden, hängt vom tatsächlichen Träger ab.
        // Ohne zertifizierten Träger wird deshalb kein globales Ordnungs-Chip behauptet.
        relationsStruktur = RelationsStruktur(
            listOf(
                RelationsAxiomNachweis(
                    if (art == VergleichsArt.Kleiner || art == VergleichsArt.Größer) RelationsAxiom.IRREFLEXIV else RelationsAxiom.REFLEXIV,
                    NachweisStatus.Unvollstaendig,
                ),
                RelationsAxiomNachweis(RelationsAxiom.TRANSITIV, NachweisStatus.Unvollstaendig),
                RelationsAxiomNachweis(RelationsAxiom.TOTAL, NachweisStatus.Unvollstaendig),
            ),
        ),
    ) { werte -> Vergleich(werte[0] as ZahlAusdruck, art, werte[1] as ZahlAusdruck) }

    private fun mengenRelation(
        operator: MengenRelationsOperator,
        struktur: RelationsStruktur,
        suchbegriffe: Set<String>,
    ) = RelationsOperatorDefinition(
        stabileId = operator.stabileId,
        titel = operator.titel,
        kategorie = "Mengen",
        symbolLatex = operator.symbolLatex,
        argumente = listOf(a("links", RelationsArgumentArt.MENGE), a("rechts", RelationsArgumentArt.MENGE)),
        suchbegriffe = suchbegriffe,
        relationsStruktur = struktur,
    ) { werte -> MengenRelationRechner.erzeuge(operator, werte[0], werte[1]) }
}
