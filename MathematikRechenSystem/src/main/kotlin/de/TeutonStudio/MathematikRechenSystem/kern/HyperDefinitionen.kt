package de.TeutonStudio.MathematikRechenSystem.kern

/** Maschinenlesbare Definitionskarten der symbolischen Nichtstandardanalysis. */
object HyperDefinitionsKatalog {
    val filterAxiome = ImpliziteDefinition(
        id = "hyper.filteraxiome",
        name = "Filter- und Ultrafilteraxiome",
        ziel = DefinitionsZiel.Struktur("hyper.filteraxiome", "hyper.ultrafilter"),
        charakterisierendeRegeln = KanonischesHyperModell.modell.alleAxiome.map { axiom ->
            DefinitionsRegel(
                id = axiom.id,
                name = axiom.beschreibung,
                folgerungLatex = axiom.latex,
            )
        },
        existenzStatus = NachweisStatus.Bedingt(
            listOf(KanonischesHyperModell.modell.frei.aussage),
        ),
        eindeutigkeitsStatus = NachweisStatus.Unentscheidbar,
    )

    val ultrapotenz = ExpliziteDefinition(
        id = "hyper.ultrapotenz",
        name = "Ultrapotenz",
        ziel = DefinitionsZiel.Struktur("hyper.ultrapotenz", "hyper.modell"),
        wert = AllgemeinerParameter(
            name = "ultrapotenz",
            latex = "{}^*A:=A^{\\mathbb N}/\\mathcal U",
        ),
        voraussetzungen = listOf(KanonischesHyperModell.modell.frei.aussage),
        referenzen = setOf(filterAxiome.id),
    )

    val hyperErweiterung = ExpliziteDefinition(
        id = "hyper.erweiterung",
        name = "Hypererweiterung",
        ziel = DefinitionsZiel.Operation("hyper.erweiterung", "analysis.hyperErweiterung"),
        wert = AllgemeinerParameter(
            name = "hypererweiterung",
            latex = "A\\mapsto{}^*A",
        ),
        referenzen = setOf(ultrapotenz.id),
    )

    val interneAussagen = PraedikativeDefinition(
        id = "hyper.interneAussagen",
        name = "Interne erststufige Aussagen",
        ziel = DefinitionsZiel.Eigenschaft("hyper.interneAussagen", "analysis.hyper.intern"),
        grundmenge = WahrheitsMenge,
        praedikat = UnentscheidbareAussage(
            bezeichnung = "intern_1",
            system = "Syntaxprüfung des registrierten Aussage-AST",
        ),
        referenzen = setOf(hyperErweiterung.id),
    )

    val externeBegriffe = ExpliziteDefinition(
        id = "hyper.externeBegriffe",
        name = "Externe Begriffe",
        ziel = DefinitionsZiel.Eigenschaft("hyper.externeBegriffe", "analysis.hyper.extern"),
        wert = AllgemeinerParameter(
            name = "extern",
            latex = "\\operatorname{standard},\\operatorname{endlich},\\operatorname{infinitesimal},\\operatorname{st}",
        ),
        referenzen = setOf(hyperErweiterung.id),
    )

    val transfer = ImpliziteDefinition(
        id = "hyper.transfer",
        name = "Transferprinzip",
        ziel = DefinitionsZiel.Operation("hyper.transfer", "analysis.hyper.transfer"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "hyper.transfer.intern",
                name = "Interner Transfer",
                folgerungLatex = "\\varphi\\text{ intern und erststufig}\\Rightarrow{}^*\\varphi",
                referenzen = setOf(interneAussagen.id),
            ),
            DefinitionsRegel(
                id = "hyper.transfer.extern",
                name = "Externe Grenze",
                folgerungLatex = "\\varphi\\text{ extern}\\Rightarrow\\operatorname{TransferFehler}(\\varphi)",
                referenzen = setOf(externeBegriffe.id),
            ),
        ),
        existenzStatus = NachweisStatus.Nachgewiesen,
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(interneAussagen.id, externeBegriffe.id),
    )

    val endlichUndUnendlich = ExpliziteDefinition(
        id = "hyper.endlichUnendlich",
        name = "Endliche und unendliche Hyperwerte",
        ziel = DefinitionsZiel.Eigenschaft("hyper.endlichUnendlich", "analysis.hyper.groessenklasse"),
        wert = AllgemeinerParameter(
            name = "hyperklasse",
            latex = "h\\in{}^*\\mathbb R:\\;\\operatorname{endlich}(h)\\lor h\\gg0\\lor h\\ll0",
        ),
        referenzen = setOf(externeBegriffe.id),
    )

    val infinitesimal = ExpliziteDefinition(
        id = "hyper.infinitesimal",
        name = "Infinitesimalität",
        ziel = DefinitionsZiel.Eigenschaft("hyper.infinitesimal", "analysis.hyper.infinitesimal"),
        wert = AllgemeinerParameter(
            name = "infinitesimal",
            latex = "h\\approx0\\Leftrightarrow\\forall n\\in\\mathbb N:\\lvert h\\rvert<1/n",
        ),
        referenzen = setOf(externeBegriffe.id, endlichUndUnendlich.id),
    )

    val standardteilUndLimes = ExpliziteDefinition(
        id = "hyper.standardteilLimes",
        name = "Standardteil und Hyper-Limes",
        ziel = DefinitionsZiel.Operation("hyper.standardteilLimes", "analysis.hyperLimes"),
        wert = AllgemeinerParameter(
            name = "hyperLimes",
            latex = "\\operatorname{limes}:{}^*\\mathbb R\\to\\overline{\\mathbb R}",
        ),
        voraussetzungen = listOf(
            UnentscheidbareAussage(
                bezeichnung = "eindeutiger_Standardteil",
                system = "symbolisches Hypermodell",
            ),
        ),
        referenzen = setOf(endlichUndUnendlich.id, infinitesimal.id),
    )

    val symbolischeGrenzen = ExpliziteDefinition(
        id = "hyper.symbolischeGrenzen",
        name = "Symbolische Grenzen",
        ziel = DefinitionsZiel.Operation("hyper.symbolischeGrenzen", "analysis.grenzwert"),
        wert = AllgemeinerParameter(
            name = "grenzwert",
            latex = "\\operatorname{grenzwert}(f)\\rightsquigarrow\\operatorname{limes}({}^*f(H))",
        ),
        referenzen = setOf(hyperErweiterung.id, standardteilUndLimes.id),
    )

    val alle: List<MathematischeDefinition> = listOf(
        filterAxiome,
        ultrapotenz,
        hyperErweiterung,
        interneAussagen,
        transfer,
        externeBegriffe,
        endlichUndUnendlich,
        infinitesimal,
        standardteilUndLimes,
        symbolischeGrenzen,
    )

    val register = DefinitionsRegister(alle)
}
