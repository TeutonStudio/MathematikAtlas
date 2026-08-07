package de.TeutonStudio.MathematikRechenSystem.kern

/** Maschinenlesbare Definitionskarten des gemeinsamen Differentialknotens. */
object DifferentialDefinitionsKatalog {
    val operator = ExpliziteDefinition(
        id = "differential.operator",
        name = "Differentialoperator",
        ziel = DefinitionsZiel.Operation("differential.operator", "analysis.differential"),
        wert = AllgemeinerParameter(name = "d", latex = "d:f\\mapsto df"),
    )
    val methodenForm = ExpliziteDefinition(
        id = "differential.methodenForm", name = "Differential einer Methode",
        ziel = DefinitionsZiel.Operation("differential.methodenForm", "analysis.differential.methode"),
        wert = AllgemeinerParameter(name = "methodendifferential", latex = "df=f^{\\mathrm I}\\cdot d\\left(\\operatorname{id}\\vert_W\\right)"),
        referenzen = setOf(operator.id),
    )
    val termForm = ExpliziteDefinition(
        id = "differential.termForm", name = "Differential eines Terms",
        ziel = DefinitionsZiel.Operation("differential.termForm", "analysis.differential.term"),
        wert = AllgemeinerParameter(name = "termdifferential", latex = "d_x(f(x))=f^{\\mathrm I}(x)\\cdot dx"),
        referenzen = setOf(operator.id, methodenForm.id),
    )
    val partielleForm = ExpliziteDefinition(
        id = "differential.partiell", name = "Partielle Ableitung nach formalem Argument",
        ziel = DefinitionsZiel.Operation("differential.partiell", DifferentialOperator.Partiell(1).operatorId),
        wert = AllgemeinerParameter(name = "partiell", latex = "\\partial_i f"),
        voraussetzungen = listOf(UnentscheidbareAussage("1\\le i\\le\\operatorname{Stelligkeit}(f)", "Methodensignatur")),
        referenzen = setOf(operator.id),
    )
    val ordnung = ImpliziteDefinition(
        id = "differential.ordnung", name = "n-fache Differentiation",
        ziel = DefinitionsZiel.Operation("differential.ordnung", "iteration.differentiation"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel("differential.ordnung.null", "Nullfall", folgerungLatex = "f^{(0)}=f"),
            DefinitionsRegel("differential.ordnung.rekursion", "Rekursion", folgerungLatex = "f^{(n+1)}=D\\left(f^{(n)}\\right)"),
            DefinitionsRegel("differential.ordnung.renderer", "Konkrete Ordnung", folgerungLatex = "f^{(4)}\\mapsto f^{\\mathrm{IV}}"),
        ),
        existenzStatus = NachweisStatus.Bedingt(listOf(UnentscheidbareAussage("n\\in\\mathbb N_0", "Iterationsordnung"))),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(operator.id),
    )
    val werteVorrat = ImpliziteDefinition(
        id = "differential.werteVorrat", name = "Differenzierbarkeitsbereich höherer Ordnung",
        ziel = DefinitionsZiel.Struktur("differential.werteVorrat", "analysis.differential.domain"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel("differential.domain.null", "Ausgangsbereich", folgerungLatex = "D_0=D"),
            DefinitionsRegel("differential.domain.rekursion", "Maximaler Differenzierbarkeitsbereich", folgerungLatex = "D_{k+1}=\\{x\\in D_k\\mid f^{(k)}\\text{ ist an }x\\text{ differenzierbar}\\}"),
        ),
        existenzStatus = NachweisStatus.Bedingt(listOf(UnentscheidbareAussage("Differenzierbarkeit", "gewählter Differentialbegriff"))),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(ordnung.id),
    )
    val zielRaum = ImpliziteDefinition(
        id = "differential.zielRaum", name = "Zielräume höherer Ableitungen",
        ziel = DefinitionsZiel.Struktur("differential.zielRaum", "analysis.differential.codomain"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel("differential.codomain.null", "Ausgangsziel", folgerungLatex = "Z_0=Z"),
            DefinitionsRegel("differential.codomain.eins", "Lineare Abbildungen", folgerungLatex = "Z_1=\\mathcal L(V,Z)"),
            DefinitionsRegel("differential.codomain.n", "Multilineare Abbildungen", folgerungLatex = "Z_n=\\mathcal L^n(V,Z)"),
            DefinitionsRegel("differential.codomain.scalar", "Eindimensionale skalare Identifikation", folgerungLatex = "f:D\\subseteq\\mathbb R\\to\\mathbb R\\Rightarrow f^{(n)}:D_n\\to\\mathbb R"),
        ),
        existenzStatus = NachweisStatus.Bedingt(listOf(UnentscheidbareAussage("passende lineare lokale Struktur", "Differentialvertrag"))),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(ordnung.id),
    )
    val differentialBegriffe = ExpliziteDefinition(
        id = "differential.begriffe", name = "Differentialbegriffe",
        ziel = DefinitionsZiel.Struktur("differential.begriffe", "analysis.differential.model"),
        wert = AllgemeinerParameter(name = "modelle", latex = "\\operatorname{reell/Fr\\acute echet},\\;\\operatorname{komplex}"),
        referenzen = setOf(werteVorrat.id, zielRaum.id),
    )
    val alle: List<MathematischeDefinition> = listOf(operator, methodenForm, termForm, partielleForm, ordnung, werteVorrat, zielRaum, differentialBegriffe)
    val register = DefinitionsRegister(alle)
}
