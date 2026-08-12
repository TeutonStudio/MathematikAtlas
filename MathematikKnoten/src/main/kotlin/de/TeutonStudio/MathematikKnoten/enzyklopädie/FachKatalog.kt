package de.TeutonStudio.MathematikKnoten.enzyklopädie

/** Kanonische Fachpfade der Konzeptbibliothek. */
object FachKatalog {
    val AnalysisFunktionen = FachPfad.von("analysis", "funktionen")
    val AnalysisFolgenReihen = FachPfad.von("analysis", "folgen-reihen")
    val AnalysisDifferentialIntegral = FachPfad.von("analysis", "differential-integral")
    val AnalysisEigenschaftenRegularität = FachPfad.von("analysis", "eigenschaften", "regularitaet")
    val AnalysisEigenschaftenIntegrabilität = FachPfad.von("analysis", "eigenschaften", "integrabilitaet")
    val AnalysisEigenschaftenFunktionsgeometrie = FachPfad.von("analysis", "eigenschaften", "funktionsgeometrie")

    val MethodenSignatur = FachPfad.von("methoden", "signatur")
    val MethodenFolgen = FachPfad.von("methoden", "folgen")
    val MethodenWertarten = FachPfad.von("methoden", "wertarten")

    val LineareAlgebraVektoren = FachPfad.von("lineare-algebra", "vektoren")
    val LineareAlgebraMatrizen = FachPfad.von("lineare-algebra", "matrizen")
    val LineareAlgebraTensoren = FachPfad.von("lineare-algebra", "tensoren")
    val LineareAlgebraSkalarprodukte = FachPfad.von("lineare-algebra", "skalarprodukte")

    val GeometrieGrundobjekte = FachPfad.von("geometrie", "grundobjekte")
    val GeometrieKonstruktionen = FachPfad.von("geometrie", "konstruktionen")
    val GeometrieTransformationen = FachPfad.von("geometrie", "transformationen")
    val GeometrieDarstellung = FachPfad.von("geometrie", "visualisierung")

    val MengenlehreMengen = FachPfad.von("mengenlehre", "mengen")
    val MengenlehreOperationen = FachPfad.von("mengenlehre", "mengenoperationen")
    val MengenlehreDefinitionen = FachPfad.von("mengenlehre", "mengendefinitionen")
    val MengenlehreKonstruktionen = FachPfad.von("mengenlehre", "konstruktionen")
    val MengenlehreAxiome = FachPfad.von("mengenlehre", "axiome")
    val MengenEigenschaftenKardinalität = FachPfad.von("mengenlehre", "eigenschaften", "kardinalitaet")
    val MengenEigenschaftenTopologie = FachPfad.von("mengenlehre", "eigenschaften", "topologie")
    val MengenEigenschaftenKonvexität = FachPfad.von("mengenlehre", "eigenschaften", "konvexitaet")
    val MengenlehreTopologieRäume = FachPfad.von("mengenlehre", "topologie", "raeume")

    val LogikAussagen = FachPfad.von("logik", "aussagen")
    val LogikPrädikate = FachPfad.von("logik", "praedikate")
    val LogikAxiome = FachPfad.von("logik", "praedikate", "axiome")
    val LogikQuantoren = FachPfad.von("logik", "quantoren")

    val ArithmetikNatürlicheZahlen = FachPfad.von("arithmetik", "natuerliche-zahlen")

    val AlgebraZahlen = FachPfad.von("algebra", "zahlen")
    val AlgebraOperationen = FachPfad.von("algebra", "operationen")
    val AlgebraMethoden = FachPfad.von("algebra", "methoden")
    val AlgebraStrukturen = FachPfad.von("algebra", "strukturen")
    val AlgebraStrukturenGruppen = FachPfad.von("algebra", "strukturen", "gruppen")
    val AlgebraStrukturenRingeKörper = FachPfad.von("algebra", "strukturen", "ringe-koerper")

    val TopologieGrundbegriffe = FachPfad.von("topologie", "grundbegriffe")
    val TopologieAbbildungen = FachPfad.von("topologie", "abbildungen")
    val StochastikGrundbegriffe = FachPfad.von("stochastik", "grundbegriffe")
    val EigeneKarten = FachPfad.von("eigene-karten")

    val alle: Set<FachPfad> = setOf(
        AnalysisFunktionen,
        AnalysisFolgenReihen,
        AnalysisDifferentialIntegral,
        AnalysisEigenschaftenRegularität,
        AnalysisEigenschaftenIntegrabilität,
        AnalysisEigenschaftenFunktionsgeometrie,
        MethodenSignatur,
        MethodenFolgen,
        MethodenWertarten,
        LineareAlgebraVektoren,
        LineareAlgebraMatrizen,
        LineareAlgebraTensoren,
        LineareAlgebraSkalarprodukte,
        GeometrieGrundobjekte,
        GeometrieKonstruktionen,
        GeometrieTransformationen,
        GeometrieDarstellung,
        MengenlehreMengen,
        MengenlehreOperationen,
        MengenlehreDefinitionen,
        MengenlehreKonstruktionen,
        MengenlehreAxiome,
        MengenEigenschaftenKardinalität,
        MengenEigenschaftenTopologie,
        MengenEigenschaftenKonvexität,
        MengenlehreTopologieRäume,
        LogikAussagen,
        LogikPrädikate,
        LogikAxiome,
        LogikQuantoren,
        ArithmetikNatürlicheZahlen,
        AlgebraZahlen,
        AlgebraOperationen,
        AlgebraMethoden,
        AlgebraStrukturen,
        AlgebraStrukturenGruppen,
        AlgebraStrukturenRingeKörper,
        TopologieGrundbegriffe,
        TopologieAbbildungen,
        StochastikGrundbegriffe,
        EigeneKarten,
    )

    fun fürAxiomId(axiomId: String): Set<FachPfad> = buildSet {
        if (!axiomId.startsWith("axiom.")) return@buildSet
        add(LogikAxiome)
        when {
            axiomId.startsWith("axiom.peano.") -> add(ArithmetikNatürlicheZahlen)
            axiomId.startsWith("axiom.zf.") || axiomId.startsWith("axiom.zfc.") -> add(MengenlehreAxiome)
            axiomId.startsWith("axiom.relation.") -> add(LogikPrädikate)
            axiomId.startsWith("axiom.algebra.") -> {
                add(AlgebraStrukturen)
                if (listOf("halbgruppe", "monoid", "gruppe", "abelscheGruppe").any(axiomId::endsWith)) {
                    add(AlgebraStrukturenGruppen)
                }
                if (listOf("halbring", "ringOhneEins", "ring", "kommutativerRing", "integritaetsbereich", "schiefkoerper", "koerper").any(axiomId::endsWith)) {
                    add(AlgebraStrukturenRingeKörper)
                }
            }
        }
    }

    fun fürVorlage(
        art: String,
        name: String,
        kategorie: String,
        besitztKartenVerweis: Boolean,
        standardParameter: Map<String, String> = emptyMap(),
    ): Set<FachPfad> {
        val artKlein = art.lowercase()
        val nameKlein = name.lowercase()
        val kategorieKlein = kategorie.lowercase()
        val eigenschaft = standardParameter["eigenschaft"]?.trim()?.lowercase().orEmpty()

        if (artKlein == "mathematik.methodengraph") {
            return setOf(AnalysisFunktionen, MengenlehreKonstruktionen)
        }

        return buildSet {
            if (besitztKartenVerweis || kategorieKlein in setOf("eigene karten", "gespeicherte karten")) {
                add(EigeneKarten)
            }

            when {
                artKlein in setOf("mathematik.topologischerraum", "mathematik.metrischerraum") -> {
                    add(MengenlehreTopologieRäume)
                    add(TopologieGrundbegriffe)
                }
                "methodeneigenschaft" in artKlein -> {
                    if (eigenschaft == "stetig") {
                        add(AnalysisEigenschaftenRegularität)
                        add(TopologieAbbildungen)
                        add(AlgebraMethoden)
                    } else {
                        add(AnalysisEigenschaftenRegularität)
                        add(AnalysisEigenschaftenIntegrabilität)
                        add(AnalysisEigenschaftenFunktionsgeometrie)
                        add(AlgebraMethoden)
                    }
                }
                "analysiseigenschaft" in artKlein -> {
                    add(AnalysisEigenschaftenFunktionsgeometrie)
                    add(AnalysisDifferentialIntegral)
                }
                "folgeneigenschaft" in artKlein -> {
                    add(MethodenFolgen)
                    add(MethodenWertarten)
                    add(AnalysisFolgenReihen)
                }
                "methodenstelligkeit" in artKlein -> {
                    add(MethodenSignatur)
                    add(AlgebraMethoden)
                }
                "mengeneigenschaft" in artKlein -> when (eigenschaft) {
                    "endlich", "unendlich", "abzählbar", "abzaehlbar", "überabzählbar", "ueberabzaehlbar", "uberabzahlbar" ->
                        add(MengenEigenschaftenKardinalität)
                    "offen", "abgeschlossen", "geschlossen" -> {
                        add(MengenEigenschaftenTopologie)
                        add(TopologieGrundbegriffe)
                    }
                    "konvexe-menge" -> add(MengenEigenschaftenKonvexität)
                    else -> {
                        add(MengenEigenschaftenKardinalität)
                        add(MengenEigenschaftenTopologie)
                        add(MengenEigenschaftenKonvexität)
                    }
                }
            }

            if (kategorieKlein.startsWith("geometrie:") || "geometrie" in artKlein) {
                add(
                    when {
                        "transformation" in kategorieKlein || "transformation" in artKlein -> GeometrieTransformationen
                        "darstellung" in kategorieKlein || "visualisierung" in artKlein -> GeometrieDarstellung
                        "konstruktion" in kategorieKlein -> GeometrieKonstruktionen
                        else -> GeometrieGrundobjekte
                    },
                )
            }
            if (kategorieKlein == "vektoren" || "vektor" in artKlein) add(LineareAlgebraVektoren)
            if (kategorieKlein == "matrizen" || "matrix" in artKlein || "spur" in artKlein) add(LineareAlgebraMatrizen)
            if ("tensor" in artKlein) add(LineareAlgebraTensoren)
            if ("skalarprodukt" in artKlein || "skalarprodukt" in nameKlein) {
                add(LineareAlgebraSkalarprodukte)
                add(GeometrieGrundobjekte)
            }
            if (kategorieKlein == "mengen" || "mengen" in kategorieKlein || "menge" in artKlein) {
                add(
                    when {
                        "konstruktor" in artKlein || "definator" in artKlein -> MengenlehreDefinitionen
                        "rechnung" in kategorieKlein || listOf("schnitt", "vereinigung", "differenz", "produkt").any(artKlein::contains) -> MengenlehreOperationen
                        else -> MengenlehreMengen
                    },
                )
            }
            if (
                kategorieKlein.startsWith("aussagen") || kategorieKlein == "aussage" ||
                listOf("aussage", "praedikat", "prädikat", "quantor", "gleichheit", "ordnung").any(artKlein::contains)
            ) {
                add(
                    when {
                        "quantor" in artKlein -> LogikQuantoren
                        listOf("praedikat", "prädikat", "gleichheit", "ordnung").any(artKlein::contains) -> LogikPrädikate
                        else -> LogikAussagen
                    },
                )
            }
            if (
                kategorieKlein == "analysis" || kategorieKlein.startsWith("analysis:") ||
                listOf("ableit", "integr", "grenz", "folge", "reihe").any(artKlein::contains)
            ) {
                add(
                    when {
                        listOf("folge", "reihe", "grenz").any(artKlein::contains) -> AnalysisFolgenReihen
                        "ableit" in artKlein || "integr" in artKlein -> AnalysisDifferentialIntegral
                        else -> AnalysisFunktionen
                    },
                )
            }
            if (
                kategorieKlein in setOf("methoden", "abbildungen") || kategorieKlein.startsWith("methoden:") ||
                "methode" in artKlein || "abbild" in artKlein
            ) {
                add(AlgebraMethoden)
                add(AnalysisFunktionen)
            }
            if (
                kategorieKlein in setOf("rechnen", "algebra", "zahlen", "operatoren", "steuerung") ||
                "zahl" in artKlein || "rechner" in artKlein
            ) {
                add(if ("zahl" in artKlein && "rechner" !in artKlein) AlgebraZahlen else AlgebraOperationen)
            }
            if (isEmpty()) add(AlgebraOperationen)
        }
    }
}
