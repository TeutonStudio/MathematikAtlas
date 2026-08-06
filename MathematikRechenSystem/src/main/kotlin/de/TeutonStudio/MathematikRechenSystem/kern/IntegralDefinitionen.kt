package de.TeutonStudio.MathematikRechenSystem.kern

/** Maschinenlesbare Definitionskarten des gemeinsamen Integralknotens. */
object IntegralDefinitionsKatalog {
    val operator = ExpliziteDefinition(
        id = "integral.operator",
        name = "Integraloperator",
        ziel = DefinitionsZiel.Operation("integral.operator", "analysis.integral"),
        wert = AllgemeinerParameter(
            name = "integral",
            latex = "(M,f,\\mu)\\mapsto\\int_M f\\,d\\mu",
        ),
    )

    val methodenForm = ExpliziteDefinition(
        id = "integral.methodenForm",
        name = "Integral einer Methode",
        ziel = DefinitionsZiel.Operation("integral.methodenForm", "analysis.integral.methode"),
        wert = AllgemeinerParameter(
            name = "methodenIntegral",
            latex = "\\int_M f\\cdot d\\left(\\operatorname{id}\\vert_M\\right)",
        ),
        referenzen = setOf(operator.id),
    )

    val termForm = ExpliziteDefinition(
        id = "integral.termForm",
        name = "Integral eines Terms",
        ziel = DefinitionsZiel.Operation("integral.termForm", "analysis.integral.term"),
        wert = AllgemeinerParameter(
            name = "termIntegral",
            latex = "\\int_{x\\in M}t\\cdot dx",
        ),
        referenzen = setOf(operator.id, methodenForm.id),
    )

    val bindung = ImpliziteDefinition(
        id = "integral.bindung",
        name = "Bindung von Variablen und Differentialen",
        ziel = DefinitionsZiel.Struktur("integral.bindung", "analysis.integral.binding"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "integral.bindung.eindeutig",
                name = "Eindeutige Quellenidentität",
                folgerungLatex = "\\operatorname{Quelle}(x_i)=\\operatorname{Quelle}(dx_i)",
            ),
            DefinitionsRegel(
                id = "integral.bindung.dimension",
                name = "Eine Bindung je Bereichskomponente",
                folgerungLatex = "\\#\\operatorname{Bindungen}=\\dim(M)",
            ),
            DefinitionsRegel(
                id = "integral.bindung.frei",
                name = "Weitere Variablen bleiben frei",
                folgerungLatex = "y\\notin\\{x_1,\\ldots,x_n\\}\\Rightarrow y\\text{ bleibt Parameter}",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(
                UnentscheidbareAussage(
                    "passende Variablenbindung",
                    "Integralsignatur",
                ),
            ),
        ),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(termForm.id),
    )

    val bereich = ExpliziteDefinition(
        id = "integral.bereich",
        name = "Integrationsbereich",
        ziel = DefinitionsZiel.Struktur("integral.bereich", "analysis.integral.domain"),
        wert = AllgemeinerParameter(
            name = "bereich",
            latex = "M=M_1\\times\\cdots\\times M_n",
        ),
        referenzen = setOf(operator.id),
    )

    val mass = ImpliziteDefinition(
        id = "integral.mass",
        name = "Maßvertrag",
        ziel = DefinitionsZiel.Struktur("integral.mass", "analysis.integral.measure"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "integral.mass.reell",
                name = "Reeller Standardbereich",
                folgerungLatex = "M_i\\text{ Intervall}\\Rightarrow\\mu=\\lambda",
            ),
            DefinitionsRegel(
                id = "integral.mass.diskret",
                name = "Endlicher diskreter Bereich",
                folgerungLatex = "M_i\\text{ endlich}\\Rightarrow\\mu=\\#",
            ),
            DefinitionsRegel(
                id = "integral.mass.explizit",
                name = "Nicht eindeutiger Bereich",
                folgerungLatex = "\\mu\\text{ nicht eindeutig ableitbar}\\Rightarrow\\mu\\text{ muss explizit sein}",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(
                UnentscheidbareAussage(
                    "geeignetes Maß",
                    "Integralvertrag",
                ),
            ),
        ),
        eindeutigkeitsStatus = NachweisStatus.Bedingt(
            listOf(
                UnentscheidbareAussage(
                    "Maß eindeutig aus Bereich ableitbar",
                    "Integralvertrag",
                ),
            ),
        ),
        referenzen = setOf(bereich.id),
    )

    val riemann = ImpliziteDefinition(
        id = "integral.riemann",
        name = "Beschränktes Riemann-Integral",
        ziel = DefinitionsZiel.Operation("integral.riemann", "analysis.integral.riemann"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "integral.riemann.domain",
                name = "Kartesisches Produkt beschränkter Intervalle",
                folgerungLatex = "M=I_1\\times\\cdots\\times I_n",
            ),
            DefinitionsRegel(
                id = "integral.riemann.hauptsatz",
                name = "Eindimensionaler Hauptsatz",
                folgerungLatex = "\\int_{[a,b]}f\\cdot dx=F(b)-F(a),\\;F^{\\mathrm I}=f",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(
                UnentscheidbareAussage(
                    "Riemann-Integrierbarkeit",
                    "Riemann-Integral",
                ),
            ),
        ),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(operator.id, mass.id),
    )

    val zaehlmass = ExpliziteDefinition(
        id = "integral.zaehlmass",
        name = "Integral bezüglich des Zählmaßes",
        ziel = DefinitionsZiel.Operation("integral.zaehlmass", "analysis.integral.zaehlmass"),
        wert = AllgemeinerParameter(
            name = "zaehlIntegral",
            latex = "\\int_M f\\,d\\#=\\sum_{x\\in M}f(x)",
        ),
        voraussetzungen = listOf(
            UnentscheidbareAussage(
                "endlicher oder summierbarer Bereich",
                "Zählmaß",
            ),
        ),
        referenzen = setOf(operator.id, mass.id),
    )

    val nichtstandard = ImpliziteDefinition(
        id = "integral.nichtstandard",
        name = "Nichtstandardmäßige Integraldarstellung",
        ziel = DefinitionsZiel.Operation("integral.nichtstandard", "analysis.integral.nichtstandard"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "integral.nichtstandard.summe",
                name = "Hyperendliche Summe",
                folgerungLatex = "\\operatorname{st}\\left(\\sum_{i=1}^{H}{}^*f(\\xi_i)\\cdot\\Delta x_i\\right)",
            ),
            DefinitionsRegel(
                id = "integral.nichtstandard.unabhaengig",
                name = "Unabhängigkeit von Stützstellen",
                folgerungLatex = "\\operatorname{st}(S_H)\\text{ ist unabhängig von der zulässigen Wahl der }\\xi_i",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(
                UnentscheidbareAussage("hyperendliche Partition", "Nichtstandardintegral"),
                UnentscheidbareAussage("Standardteil existiert", "Nichtstandardintegral"),
                UnentscheidbareAussage("Unabhängigkeit von Stützstellen", "Nichtstandardintegral"),
            ),
        ),
        eindeutigkeitsStatus = NachweisStatus.Bedingt(
            listOf(
                UnentscheidbareAussage("eindeutiger Standardteil", "Nichtstandardintegral"),
            ),
        ),
        referenzen = setOf(operator.id, mass.id),
    )

    val klassischeGrenzen = ExpliziteDefinition(
        id = "integral.klassischeGrenzen",
        name = "Normalisierung klassischer Integralgrenzen",
        ziel = DefinitionsZiel.Operation("integral.klassischeGrenzen", "analysis.integral.bounds"),
        wert = AllgemeinerParameter(
            name = "grenzen",
            latex = "\\int_a^b f(x)\\,dx\\equiv\\int_{x\\in[a,b]}f(x)\\cdot dx",
        ),
        referenzen = setOf(termForm.id, bereich.id),
    )

    val alle: List<MathematischeDefinition> = listOf(
        operator,
        methodenForm,
        termForm,
        bindung,
        bereich,
        mass,
        riemann,
        zaehlmass,
        nichtstandard,
        klassischeGrenzen,
    )

    val register = DefinitionsRegister(alle)
}
