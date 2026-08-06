package de.TeutonStudio.MathematikRechenSystem.kern

/** Maschinenlesbare Definitionskarten der iterierten Selbstkomposition. */
object SelbstkompositionsDefinitionsKatalog {
    val operator = InduktiveDefinition(
        id = "selbstkomposition.operator",
        name = "Iterierte Selbstkomposition",
        ziel = DefinitionsZiel.Operation(
            "selbstkomposition.operator",
            IterationsArt.SELBSTKOMPOSITION.operatorId,
        ),
        basisRegeln = listOf(
            DefinitionsRegel(
                id = "selbstkomposition.null",
                name = "Nullfall",
                folgerungLatex = "f^{\\langle0\\rangle}=\\operatorname{id}\\vert_W",
            ),
            DefinitionsRegel(
                id = "selbstkomposition.eins",
                name = "Einsfall",
                folgerungLatex = "f^{\\langle1\\rangle}=f",
            ),
        ),
        abschlussRegeln = listOf(
            DefinitionsRegel(
                id = "selbstkomposition.rekursion",
                name = "Rekursion",
                folgerungLatex = "f^{\\langle n+1\\rangle}=f\\circ f^{\\langle n\\rangle}",
                rekursiveReferenzen = listOf(
                    DefinitionsReferenz(
                        definitionsId = "selbstkomposition.operator",
                        position = RekursionsPosition.ABSCHLUSS_VORAUSSETZUNG,
                        monotonie = NachweisStatus.Nachgewiesen,
                    ),
                ),
            ),
        ),
        voraussetzungen = listOf(
            UnentscheidbareAussage("n\\in\\mathbb N_0", "Iterationsordnung"),
            UnentscheidbareAussage("kompatibler erneuter Aufrufvertrag", "Selbstkomposition"),
        ),
    )

    val mehrstellig = ImpliziteDefinition(
        id = "selbstkomposition.mehrstellig",
        name = "Mehrstellige Selbstkomposition",
        ziel = DefinitionsZiel.Struktur(
            "selbstkomposition.mehrstellig",
            "methode.selbstkomposition.signature",
        ),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "selbstkomposition.mehrstellig.anzahl",
                name = "Komponentenanzahl",
                folgerungLatex = "\\#\\operatorname{Komponenten}(f(x_1,\\ldots,x_k))=k",
            ),
            DefinitionsRegel(
                id = "selbstkomposition.mehrstellig.bereiche",
                name = "Komponentenbereiche",
                folgerungLatex = "\\operatorname{Bild}_i(f)\\subseteq W_i\\quad(1\\le i\\le k)",
            ),
            DefinitionsRegel(
                id = "selbstkomposition.mehrstellig.signatur",
                name = "Äußere Signatur bleibt k-stellig",
                folgerungLatex = "f^{\\langle n\\rangle}:W_n\\subseteq W_1\\times\\cdots\\times W_k\\to Z",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("komponentenweise Bildverträglichkeit", "Selbstkomposition")),
        ),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(operator.id),
    )

    val einleistigesPacken = ExpliziteDefinition(
        id = "selbstkomposition.packen",
        name = "Einleistiges Packen und Entpacken",
        ziel = DefinitionsZiel.Operation(
            "selbstkomposition.packen",
            "methode.selbstkomposition.pack",
        ),
        wert = AllgemeinerParameter(
            name = "packen",
            latex = "(x_1,\\ldots,x_k)\\leftrightarrow\\operatorname{Tupel}(x_1,\\ldots,x_k)",
        ),
        voraussetzungen = listOf(
            UnentscheidbareAussage("nur eine Tupel-, Zeilen- oder Spaltenebene", "Packvertrag"),
        ),
        referenzen = setOf(mehrstellig.id),
    )

    val keineRekursiveAbflachung = ExpliziteDefinition(
        id = "selbstkomposition.keineRekursiveAbflachung",
        name = "Keine rekursive Abflachung",
        ziel = DefinitionsZiel.Struktur(
            "selbstkomposition.keineRekursiveAbflachung",
            "methode.selbstkomposition.noFlatten",
        ),
        wert = AllgemeinerParameter(
            name = "keineAbflachung",
            latex = "((a,b),c)\\not\\mapsto(a,b,c)",
        ),
        referenzen = setOf(einleistigesPacken.id),
    )

    val maximalerWertevorrat = ImpliziteDefinition(
        id = "selbstkomposition.wertevorrat",
        name = "Maximaler Wertevorrat der Iteration",
        ziel = DefinitionsZiel.Struktur(
            "selbstkomposition.wertevorrat",
            "methode.selbstkomposition.domain",
        ),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "selbstkomposition.wertevorrat.null",
                name = "Ausgangsbereich",
                folgerungLatex = "W_0=W",
            ),
            DefinitionsRegel(
                id = "selbstkomposition.wertevorrat.eins",
                name = "Erste Stufe",
                folgerungLatex = "W_1=W",
            ),
            DefinitionsRegel(
                id = "selbstkomposition.wertevorrat.rekursion",
                name = "Rekursiver maximaler Bereich",
                folgerungLatex = "W_{n+1}=W_n\\cap\\left(f^{\\langle n\\rangle}\\right)^{-1}(W)",
            ),
        ),
        existenzStatus = NachweisStatus.Nachgewiesen,
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(operator.id),
    )

    val bereichsModi = ExpliziteDefinition(
        id = "selbstkomposition.bereichsModi",
        name = "Bereichsmodi",
        ziel = DefinitionsZiel.Struktur(
            "selbstkomposition.bereichsModi",
            "methode.selbstkomposition.domainMode",
        ),
        wert = AllgemeinerParameter(
            name = "bereichsModi",
            latex = "\\operatorname{maximal}:W_n;\\qquad\\operatorname{vollstaendig}:W_n=W",
        ),
        referenzen = setOf(maximalerWertevorrat.id),
    )

    val status = ExpliziteDefinition(
        id = "selbstkomposition.status",
        name = "Status der Selbstkomposition",
        ziel = DefinitionsZiel.Struktur(
            "selbstkomposition.status",
            "methode.selbstkomposition.status",
        ),
        wert = AllgemeinerParameter(
            name = "status",
            latex = "\\{\\operatorname{total},\\operatorname{eingeschraenkt},\\operatorname{bedingt}," +
                "\\operatorname{leer},\\operatorname{unmoeglich},\\operatorname{nichtImplementiert}\\}",
        ),
        referenzen = setOf(maximalerWertevorrat.id, mehrstellig.id),
    )

    val korrelierteBereiche = ImpliziteDefinition(
        id = "selbstkomposition.korrelierteBereiche",
        name = "Korrelierte mehrstellige Teilbereiche",
        ziel = DefinitionsZiel.Struktur(
            "selbstkomposition.korrelierteBereiche",
            "methode.selbstkomposition.correlatedDomain",
        ),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "selbstkomposition.korrelierteBereiche.produkt",
                name = "Nicht jedes Teilgebiet ist ein Produkt",
                folgerungLatex = "W_n\\subseteq W_1\\times\\cdots\\times W_k\\not\\Rightarrow W_n=A_1\\times\\cdots\\times A_k",
            ),
            DefinitionsRegel(
                id = "selbstkomposition.korrelierteBereiche.keinVerlust",
                name = "Keine verlustreiche Zerlegung",
                folgerungLatex = "\\operatorname{Korrelation}(W_n)\\text{ muss erhalten bleiben}",
            ),
        ),
        existenzStatus = NachweisStatus.Nachgewiesen,
        eindeutigkeitsStatus = NachweisStatus.Unentscheidbar,
        referenzen = setOf(maximalerWertevorrat.id, mehrstellig.id),
    )

    val persistenz = ExpliziteDefinition(
        id = "selbstkomposition.persistenz",
        name = "Semantische Persistenz",
        ziel = DefinitionsZiel.Struktur(
            "selbstkomposition.persistenz",
            "methode.selbstkomposition.persistence",
        ),
        wert = AllgemeinerParameter(
            name = "persistenz",
            latex = "(\\operatorname{operatorId},n,\\operatorname{eingang},\\operatorname{ausgang},\\operatorname{bereich})",
        ),
        referenzen = setOf(operator.id, einleistigesPacken.id, bereichsModi.id),
    )

    val alle: List<MathematischeDefinition> = listOf(
        operator,
        mehrstellig,
        einleistigesPacken,
        keineRekursiveAbflachung,
        maximalerWertevorrat,
        bereichsModi,
        status,
        korrelierteBereiche,
        persistenz,
    )

    val register = DefinitionsRegister(alle)
}
