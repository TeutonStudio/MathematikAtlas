package de.TeutonStudio.MathematikRechenSystem.kern

/** Maschinenlesbare Definitionskarten der natürlichen algebraischen Potenz. */
object PotenzDefinitionsKatalog {
    val struktur = ImpliziteDefinition(
        id = "potenz.struktur",
        name = "Multiplikative Potenzstruktur",
        ziel = DefinitionsZiel.Struktur("potenz.struktur", "algebra.potenzstruktur"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "potenz.struktur.inner",
                name = "Innere Multiplikation",
                folgerungLatex = "\\cdot:M\\times M\\longrightarrow M",
            ),
            DefinitionsRegel(
                id = "potenz.struktur.assoziativ",
                name = "Assoziativität",
                folgerungLatex = "(a\\cdot b)\\cdot c=a\\cdot(b\\cdot c)",
            ),
            DefinitionsRegel(
                id = "potenz.struktur.neutral",
                name = "Neutrales Element",
                folgerungLatex = "1_M\\cdot a=a=a\\cdot1_M",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("registrierte innere Multiplikation", "Potenzstruktur")),
        ),
        eindeutigkeitsStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("eindeutige Produktwahl", "Potenzstruktur")),
        ),
    )

    val rekursion = InduktiveDefinition(
        id = "potenz.natuerlich",
        name = "Natürliche Potenz",
        ziel = DefinitionsZiel.Operation("potenz.natuerlich", IterationsArt.MULTIPLIKATION.operatorId),
        basisRegeln = listOf(
            DefinitionsRegel(
                id = "potenz.null",
                name = "Nullfall",
                folgerungLatex = "a^0=1_M",
            ),
            DefinitionsRegel(
                id = "potenz.eins",
                name = "Einsfall",
                folgerungLatex = "a^1=a",
            ),
        ),
        abschlussRegeln = listOf(
            DefinitionsRegel(
                id = "potenz.rekursion",
                name = "Rekursion",
                folgerungLatex = "a^{n+1}=a^n\\cdot a",
                rekursiveReferenzen = listOf(
                    DefinitionsReferenz(
                        definitionsId = "potenz.natuerlich",
                        position = RekursionsPosition.ABSCHLUSS_VORAUSSETZUNG,
                        monotonie = NachweisStatus.Nachgewiesen,
                    ),
                ),
            ),
        ),
        voraussetzungen = listOf(
            UnentscheidbareAussage("a\\in M", "Potenzstruktur"),
            UnentscheidbareAussage("n\\in\\mathbb N_0", "Iterationsordnung"),
        ),
        referenzen = setOf(struktur.id),
    )

    val ordnungsFaelle = ExpliziteDefinition(
        id = "potenz.ordnungsFaelle",
        name = "Voraussetzungen nach Potenzordnung",
        ziel = DefinitionsZiel.Struktur("potenz.ordnungsFaelle", "algebra.potenz.orderCases"),
        wert = AllgemeinerParameter(
            name = "ordnungsFaelle",
            latex = "n=0:\\ 1_M;\\quad n=1:\\ a;\\quad n\\ge2:\\ \\text{Abgeschlossenheit und Assoziativität}",
        ),
        referenzen = setOf(rekursion.id),
    )

    val methodenPotenz = ExpliziteDefinition(
        id = "potenz.methode.punktweise",
        name = "Punktweise Potenz einer Methode",
        ziel = DefinitionsZiel.Operation(
            "potenz.methode.punktweise",
            "iteration.multiplikation.punktweise",
        ),
        wert = AllgemeinerParameter(
            name = "methodenPotenz",
            latex = "a:W\\to M\\Rightarrow a^n:W\\to M,\\quad a^n(x)=a(x)^n",
        ),
        referenzen = setOf(rekursion.id),
    )

    val matrixPotenz = ImpliziteDefinition(
        id = "potenz.matrix",
        name = "Natürliche Matrixpotenz",
        ziel = DefinitionsZiel.Operation("potenz.matrix", "matrix.potenz.natuerlich"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "potenz.matrix.eins",
                name = "Rechteckiger Einsfall",
                folgerungLatex = "A\\in K^{m\\times n}\\Rightarrow A^1=A",
            ),
            DefinitionsRegel(
                id = "potenz.matrix.null",
                name = "Quadratischer Nullfall",
                folgerungLatex = "A\\in K^{n\\times n}\\Rightarrow A^0=I_n",
            ),
            DefinitionsRegel(
                id = "potenz.matrix.hoeher",
                name = "Quadratische höhere Potenz",
                folgerungLatex = "A\\in K^{n\\times n},r\\ge2\\Rightarrow A^r\\in K^{n\\times n}",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("quadratische Matrix für Ordnung 0 oder mindestens 2", "Matrixpotenz")),
        ),
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(rekursion.id),
    )

    val nichtKommutativ = ExpliziteDefinition(
        id = "potenz.nichtkommutativ",
        name = "Nichtkommutative Potenz",
        ziel = DefinitionsZiel.Struktur("potenz.nichtkommutativ", "algebra.potenz.noncommutative"),
        wert = AllgemeinerParameter(
            name = "reihenfolge",
            latex = "a^{n+1}=a^n\\cdot a\\quad\\text{ohne Umordnung der Faktoren}",
        ),
        referenzen = setOf(rekursion.id),
    )

    val mehrdeutigeProdukte = ImpliziteDefinition(
        id = "potenz.mehrdeutigeProdukte",
        name = "Mehrdeutige Vektor- und Tensorpotenzen",
        ziel = DefinitionsZiel.Struktur("potenz.mehrdeutigeProdukte", "algebra.potenz.ambiguousProducts"),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "potenz.mehrdeutig.vektor",
                name = "Kein kanonisches Vektorprodukt",
                folgerungLatex = "v^n\\text{ ist ohne gewählten inneren Produktvertrag nicht definiert}",
            ),
            DefinitionsRegel(
                id = "potenz.mehrdeutig.tensor",
                name = "Tensorprodukte ändern typischerweise die Stufe",
                folgerungLatex = "T\\otimes T\\notin\\operatorname{Traeger}(T)\\text{ im Allgemeinen}",
            ),
            DefinitionsRegel(
                id = "potenz.mehrdeutig.explizit",
                name = "Explizite Produktwahl",
                folgerungLatex = "\\text{Hadamard-, kontrahiertes, Clifford- und Tensorprodukt bleiben getrennte Operatoren}",
            ),
        ),
        existenzStatus = NachweisStatus.Bedingt(
            listOf(UnentscheidbareAussage("explizite innere Produktstruktur", "Potenzstruktur")),
        ),
        eindeutigkeitsStatus = NachweisStatus.Unentscheidbar,
        referenzen = setOf(struktur.id),
    )

    val persistenz = ExpliziteDefinition(
        id = "potenz.persistenz",
        name = "Semantische Potenzpersistenz",
        ziel = DefinitionsZiel.Struktur("potenz.persistenz", "algebra.potenz.persistence"),
        wert = AllgemeinerParameter(
            name = "persistenz",
            latex = "(\\operatorname{operatorId},\\operatorname{basisRef},n,\\operatorname{strukturId})",
        ),
        referenzen = setOf(rekursion.id, struktur.id),
    )

    val alle: List<MathematischeDefinition> = listOf(
        struktur,
        rekursion,
        ordnungsFaelle,
        methodenPotenz,
        matrixPotenz,
        nichtKommutativ,
        mehrdeutigeProdukte,
        persistenz,
    )

    val register = DefinitionsRegister(alle)
}
