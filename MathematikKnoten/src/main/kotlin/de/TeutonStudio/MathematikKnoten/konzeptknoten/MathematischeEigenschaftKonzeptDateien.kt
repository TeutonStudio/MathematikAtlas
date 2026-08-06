package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.MathematischeEigenschaftKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.enzyklopädie.FachKatalog
import de.TeutonStudio.MathematikKnoten.enzyklopädie.VariantenId
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId

internal object MethodenEigenschaftKonzeptDateiA53F6C21 : ExpliziteKonzeptDatei {
    override val id = WissensId("konzept.methodeneigenschaft")
    override val varianten: Set<VariantenId> = setOf(
        MathematischeEigenschaftKnotenVorlagen.MethodenEigenschaft.stabileVariantenId(),
    )

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passend = vorlagen.nachVarianten(varianten)
        return gruppiertesVorlagenKonzept(
            id = id,
            titel = "Methodeneigenschaft",
            beschreibung = "Gemeinsamer Eigenschaftsknoten für Regularität, Integrabilität sowie globale Konvexität und Konkavität.",
            vorlagen = passend,
            generatorId = "konzeptkarte.eigenschaft.methode",
            zusätzlicheSuchbegriffe = setOf(
                "stetig", "differenzierbar", "C n", "D n", "Riemann-integrierbar",
                "Lebesgue-integrierbar", "konvex", "konkav", "concave",
            ),
        ).copy(
            fachPfade = setOf(
                FachKatalog.AnalysisEigenschaftenRegularität,
                FachKatalog.AnalysisEigenschaftenIntegrabilität,
                FachKatalog.AnalysisEigenschaftenFunktionsgeometrie,
            ),
            aliase = passend.mapTo(linkedSetOf(), KnotenVorlage::stabileKonzeptId) + setOf(
                "eigenschaft.stetigkeit",
                "eigenschaft.differenzierbarkeit",
                "eigenschaft.konvex",
                "eigenschaft.konkav",
            ),
        )
    }
}

internal object AnalysisEigenschaftKonzeptDateiBE88390F : ExpliziteKonzeptDatei {
    override val id = WissensId("konzept.analysis-eigenschaftsstellen")
    override val varianten: Set<VariantenId> = setOf(
        MathematischeEigenschaftKnotenVorlagen.AnalysisEigenschaft.stabileVariantenId(),
    )

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passend = vorlagen.nachVarianten(varianten)
        return gruppiertesVorlagenKonzept(
            id = id,
            titel = "Analysis-Eigenschaftsstellen",
            beschreibung = "Erzeugt symbolische oder exakte Stellenmengen für Minima, Maxima, Extrema, Sattelpunkte, Wendestellen sowie Konvexitäts- und Konkavitätsbereiche.",
            vorlagen = passend,
            generatorId = "konzeptkarte.eigenschaft.analysis-stellen",
            zusätzlicheSuchbegriffe = setOf(
                "Minimum", "Maximum", "Extremum", "Sattelpunkt", "Wendestelle",
                "Konvexitätsbereich", "Konkavitätsbereich", "Teilmenge des Wertevorrats",
            ),
        ).copy(
            fachPfade = setOf(
                FachKatalog.AnalysisEigenschaftenFunktionsgeometrie,
                FachKatalog.AnalysisDifferentialIntegral,
            ),
        )
    }
}

internal object FolgenEigenschaftKonzeptDatei5D2C79C4 : ExpliziteKonzeptDatei {
    override val id = WissensId("konzept.folgenei­genschaft")
    override val varianten: Set<VariantenId> = setOf(
        MathematischeEigenschaftKnotenVorlagen.FolgenEigenschaft.stabileVariantenId(),
    )

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passend = vorlagen.nachVarianten(varianten)
        return gruppiertesVorlagenKonzept(
            id = id,
            titel = "Folgenei­genschaft",
            beschreibung = "Prüft Folgen auf einseitige N₀- oder zweiseitige Z-Indexierung und auf ihre mathematische Wertart.",
            vorlagen = passend,
            generatorId = "konzeptkarte.eigenschaft.folge",
            zusätzlicheSuchbegriffe = setOf(
                "einseitige Folge", "zweiseitige Folge", "bi-infinite", "N0-Folge", "Z-Folge",
                "reellwertig", "komplexwertig", "polynomwertig", "vektorwertig", "unnatürliches Tupel",
            ),
        ).copy(
            fachPfade = setOf(
                FachKatalog.MethodenFolgen,
                FachKatalog.MethodenWertarten,
                FachKatalog.AnalysisFolgenReihen,
            ),
            aliase = passend.mapTo(linkedSetOf(), KnotenVorlage::stabileKonzeptId) + setOf(
                "historisch.unnatürliches-tupel",
                "folge.halbfolge",
                "folge.bi-infinite",
            ),
        )
    }
}

internal object MethodenStelligkeitKonzeptDatei426F55A9 : ExpliziteKonzeptDatei {
    override val id = WissensId("konzept.methodenstelligkeit")
    override val varianten: Set<VariantenId> = setOf(
        MathematischeEigenschaftKnotenVorlagen.MethodenStelligkeit.stabileVariantenId(),
    )

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passend = vorlagen.nachVarianten(varianten)
        return gruppiertesVorlagenKonzept(
            id = id,
            titel = "Methodenstelligkeit",
            beschreibung = "Prüft Ein- und Mehrstelligkeit auf Grundlage stabiler Argumentrollen und unterstützt Einzel-, Tupel- und Koordinatenansichten.",
            vorlagen = passend,
            generatorId = "konzeptkarte.eigenschaft.signatur",
            zusätzlicheSuchbegriffe = setOf(
                "einstellig", "mehrstellig", "univariat", "multivariat", "Argumentrolle", "Tupelansicht", "Koordinatenansicht",
            ),
        ).copy(
            fachPfade = setOf(FachKatalog.MethodenSignatur),
            aliase = passend.mapTo(linkedSetOf(), KnotenVorlage::stabileKonzeptId) + setOf(
                "eigenschaft.univariat",
                "eigenschaft.multivariat",
            ),
        )
    }
}

internal object MengenEigenschaftKonzeptDatei9041F8B7 : ExpliziteKonzeptDatei {
    override val id = WissensId("konzept.mengeneigenschaft")
    override val varianten: Set<VariantenId> = setOf(
        MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft.stabileVariantenId(),
    )

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passend = vorlagen.nachVarianten(varianten)
        return gruppiertesVorlagenKonzept(
            id = id,
            titel = "Mengeneigenschaft",
            beschreibung = "Prüft Offenheit, Abgeschlossenheit und Konvexität relativ zu Topologie, Umgebungsraum und affiner Struktur.",
            vorlagen = passend,
            generatorId = "konzeptkarte.eigenschaft.menge",
            zusätzlicheSuchbegriffe = setOf(
                "offene Menge", "abgeschlossene Menge", "konvexe Menge", "Topologie", "Umgebungsraum", "affine Struktur",
            ),
        ).copy(
            fachPfade = setOf(
                FachKatalog.MengenEigenschaftenTopologie,
                FachKatalog.MengenEigenschaftenKonvexität,
                FachKatalog.TopologieGrundbegriffe,
            ),
            aliase = passend.mapTo(linkedSetOf(), KnotenVorlage::stabileKonzeptId) + setOf(
                "eigenschaft.offen",
                "eigenschaft.abgeschlossen",
                "eigenschaft.konvexe-menge",
            ),
        )
    }
}
