package de.TeutonStudio.MathematikRechenSystem.kern

/** Maschinenlesbare Definitionskarten der gemeinsamen Iterationsordnung. */
object IterationsDefinitionsKatalog {
    val ordnung = ImpliziteDefinition(
        id = "iteration.ordnung",
        name = "Gemeinsame Iterationsordnung",
        ziel = DefinitionsZiel.Struktur("iteration.ordnung", "iteration.order"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "iteration.ordnung.domain",
                name = "Nichtnegative ganzzahlige Ordnung",
                folgerungLatex = "n\\in\\mathbb N_0",
            ),
            DefinitionsRegel(
                id = "iteration.ordnung.konkret",
                name = "Konkrete Ordnung",
                folgerungLatex = "n\\in\\{0,1,2,\\ldots\\}",
            ),
            DefinitionsRegel(
                id = "iteration.ordnung.symbolisch",
                name = "Symbolische Ordnung",
                folgerungLatex = "n\\text{ symbolisch}\\Rightarrow[n\\in\\mathbb N_0]\\text{ bleibt Voraussetzung}",
            ),
        ),
        existenzStatus = NachweisStatus.Nachgewiesen,
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
    )

    val multiplikation = ImpliziteDefinition(
        id = "iteration.multiplikation",
        name = "Iterative Multiplikation",
        ziel = DefinitionsZiel.Operation("iteration.multiplikation", IterationsArt.MULTIPLIKATION.operatorId),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "iteration.multiplikation.null",
                name = "Neutrales Element",
                folgerungLatex = "a^0=e",
            ),
            DefinitionsRegel(
                id = "iteration.multiplikation.rekursion",
                name = "Rekursion",
                folgerungLatex = "a^{n+1}=a^n\\cdot a",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("multiplikative Struktur mit neutralem Element", "Iterationsvertrag")),
        ),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(ordnung.id),
    )

    val differentiation = ImpliziteDefinition(
        id = "iteration.differentiation",
        name = "Iterative Differentiation",
        ziel = DefinitionsZiel.Operation("iteration.differentiation", IterationsArt.DIFFERENTIATION.operatorId),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "iteration.differentiation.null",
                name = "Nullfall",
                folgerungLatex = "f^{(0)}=f",
            ),
            DefinitionsRegel(
                id = "iteration.differentiation.rekursion",
                name = "Rekursion",
                folgerungLatex = "f^{(n+1)}=D\\left(f^{(n)}\\right)",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("Differentialbegriff auf jeder Stufe", "Iterationsvertrag")),
        ),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(ordnung.id),
    )

    val selbstkomposition = ImpliziteDefinition(
        id = "iteration.selbstkomposition",
        name = "Iterative Selbstkomposition",
        ziel = DefinitionsZiel.Operation("iteration.selbstkomposition", IterationsArt.SELBSTKOMPOSITION.operatorId),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "iteration.selbstkomposition.null",
                name = "Identität auf dem Wertevorrat",
                folgerungLatex = "f^{\\langle0\\rangle}=\\operatorname{id}\\vert_W",
            ),
            DefinitionsRegel(
                id = "iteration.selbstkomposition.rekursion",
                name = "Rekursion",
                folgerungLatex = "f^{\\langle n+1\\rangle}=f\\circ f^{\\langle n\\rangle}",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("kompatible Zielmenge und Wertevorrat", "Iterationsvertrag")),
        ),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(ordnung.id),
    )

    val einschraenkung = ImpliziteDefinition(
        id = "methode.einschraenkung",
        name = "Einschränkung einer Methode",
        ziel = DefinitionsZiel.Operation("methode.einschraenkung", "methode.einschraenkung"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "methode.einschraenkung.domain",
                name = "Eingeschränkter Wertevorrat",
                folgerungLatex = "M\\subseteq D\\Rightarrow f\\vert_M:M\\to Z",
            ),
            DefinitionsRegel(
                id = "methode.einschraenkung.target",
                name = "Zielmenge bleibt erhalten",
                folgerungLatex = "\\operatorname{Ziel}(f\\vert_M)=\\operatorname{Ziel}(f)",
            ),
            DefinitionsRegel(
                id = "methode.einschraenkung.image",
                name = "Engeres Bild nur bei Nachweis",
                folgerungLatex = "f[M]\\subseteq Z'\\subseteq Z\\text{ ist zusätzliche Information}",
            ),
            DefinitionsRegel(
                id = "methode.einschraenkung.normalisierung",
                name = "Verschachtelte Restriktion",
                folgerungLatex = "(f\\vert_M)\\vert_N=f\\vert_N\\quad(N\\subseteq M)",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("M\\subseteq D", "Methodeneinschränkung")),
        ),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
    )

    val identitaet = ExpliziteDefinition(
        id = "methode.identitaet.eingeschraenkt",
        name = "Identität auf einer Menge",
        ziel = DefinitionsZiel.Operation("methode.identitaet.eingeschraenkt", "methode.identitaet.eingeschraenkt"),
        wert = AllgemeinerParameter(
            name = "identitaet",
            latex = "\\operatorname{id}\\vert_M:M\\to M,\\;x\\mapsto x",
        ),
        referenzen = setOf(selbstkomposition.id),
    )

    val renderer = ExpliziteDefinition(
        id = "iteration.renderer",
        name = "Rendererprojektion der Iteration",
        ziel = DefinitionsZiel.Struktur("iteration.renderer", "iteration.renderer"),
        wert = AllgemeinerParameter(
            name = "renderer",
            latex = "a^n,\\quad f^{(n)},\\quad f^{\\langle n\\rangle},\\quad f^{(4)}\\mapsto f^{\\mathrm{IV}}",
        ),
        referenzen = setOf(multiplikation.id, differentiation.id, selbstkomposition.id),
    )

    val alle: List<MathematischeDefinition> = listOf(
        ordnung,
        multiplikation,
        differentiation,
        selbstkomposition,
        einschraenkung,
        identitaet,
        renderer,
    )

    val register = DefinitionsRegister(alle)
}
