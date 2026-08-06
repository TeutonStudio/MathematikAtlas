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
    val MengenEigenschaftenTopologie = FachPfad.von("mengenlehre", "eigenschaften", "topologie")
    val MengenEigenschaftenKonvexität = FachPfad.von("mengenlehre", "eigenschaften", "konvexitaet")

    val LogikAussagen = FachPfad.von("logik", "aussagen")
    val LogikPrädikate = FachPfad.von("logik", "praedikate")
    val LogikQuantoren = FachPfad.von("logik", "quantoren")

    val AlgebraZahlen = FachPfad.von("algebra", "zahlen")
    val AlgebraOperationen = FachPfad.von("algebra", "operationen")
    val AlgebraMethoden = FachPfad.von("algebra", "methoden")

    val TopologieGrundbegriffe = FachPfad.von("topologie", "grundbegriffe")
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
        MengenEigenschaftenTopologie,
        MengenEigenschaftenKonvexität,
        LogikAussagen,
        LogikPrädikate,
        LogikQuantoren,
        AlgebraZahlen,
        AlgebraOperationen,
        AlgebraMethoden,
        TopologieGrundbegriffe,
        StochastikGrundbegriffe,
        EigeneKarten,
    )

    fun fürVorlage(
        art: String,
        name: String,
        kategorie: String,
        besitztKartenVerweis: Boolean,
    ): Set<FachPfad> {
        val artKlein = art.lowercase()
        val nameKlein = name.lowercase()
        val kategorieKlein = kategorie.lowercase()
        return buildSet {
            if (besitztKartenVerweis || kategorieKlein in setOf("eigene karten", "gespeicherte karten")) {
                add(EigeneKarten)
            }

            when {
                "methodeneigenschaft" in artKlein -> {
                    add(AnalysisEigenschaftenRegularität)
                    add(AnalysisEigenschaftenIntegrabilität)
                    add(AnalysisEigenschaftenFunktionsgeometrie)
                    add(AlgebraMethoden)
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
                "mengeneigenschaft" in artKlein -> {
                    add(MengenEigenschaftenTopologie)
                    add(MengenEigenschaftenKonvexität)
                    add(TopologieGrundbegriffe)
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
