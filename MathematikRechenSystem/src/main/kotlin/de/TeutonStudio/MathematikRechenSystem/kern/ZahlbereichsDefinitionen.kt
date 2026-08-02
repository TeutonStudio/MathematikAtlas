package de.TeutonStudio.MathematikRechenSystem.kern

/** Projektweit verbindliche Zahlbereichskonvention. */
enum class FundamentalerZahlbereich(
    val id: String,
    val latex: String,
    val enthaeltNull: Boolean,
    val istGeordnet: Boolean,
) {
    NATUERLICH_POSITIV("N", "\\mathbb N", false, true),
    NATUERLICH_MIT_NULL("N0", "\\mathbb N_0", true, true),
    GANZ("Z", "\\mathbb Z", true, true),
    RATIONAL("Q", "\\mathbb Q", true, true),
    REELL("R", "\\mathbb R", true, true),
    KOMPLEX("C", "\\mathbb C", true, false),
    QUATERNION("H", "\\mathbb H", true, false),
}

object FundamentaleZahlbereiche {
    private val direkteEinbettungen = mapOf(
        FundamentalerZahlbereich.NATUERLICH_POSITIV to FundamentalerZahlbereich.NATUERLICH_MIT_NULL,
        FundamentalerZahlbereich.NATUERLICH_MIT_NULL to FundamentalerZahlbereich.GANZ,
        FundamentalerZahlbereich.GANZ to FundamentalerZahlbereich.RATIONAL,
        FundamentalerZahlbereich.RATIONAL to FundamentalerZahlbereich.REELL,
        FundamentalerZahlbereich.REELL to FundamentalerZahlbereich.KOMPLEX,
        FundamentalerZahlbereich.KOMPLEX to FundamentalerZahlbereich.QUATERNION,
    )

    fun istTeilbereich(
        teil: FundamentalerZahlbereich,
        ober: FundamentalerZahlbereich,
    ): Boolean {
        if (teil == ober) return true
        var aktuell = direkteEinbettungen[teil]
        while (aktuell != null) {
            if (aktuell == ober) return true
            aktuell = direkteEinbettungen[aktuell]
        }
        return false
    }

    fun kleinsterGemeinsamerBereich(
        bereiche: Iterable<FundamentalerZahlbereich>,
    ): FundamentalerZahlbereich {
        val liste = bereiche.toList()
        require(liste.isNotEmpty())
        return FundamentalerZahlbereich.entries.first { kandidat ->
            liste.all { istTeilbereich(it, kandidat) }
        }
    }
}

/** Null bleibt ein vorhandener Zahlterm; ihre Definition wird getrennt referenziert. */
data object DefinierteNullSemantik {
    val wert: RationaleZahl = RationaleZahl.Null
    val definitionLatex: String = "0:=|\\varnothing|"
    val bereich: FundamentalerZahlbereich = FundamentalerZahlbereich.GANZ
}

/** N₀ ist ein benannter Filter über Z und keine zweite Peano-Ursprungsstruktur. */
object NichtnegativeGanzeZahlenSemantik {
    val menge: MengenAusdruck = BenannteMenge("N0", "\\mathbb N_0")
    val filterLatex: String = "\\{z\\in\\mathbb Z\\mid z\\ge 0\\}"
}

/**
 * Bewahrt die Quellsyntax z/n, ohne einen weiteren direkten ZahlAusdruck-Untertyp
 * in das versiegelte Ausdrucksmodell einzuführen.
 */
data class SymbolischerRationalerTerm(
    val zaehler: ZahlAusdruck,
    val nenner: ZahlAusdruck,
    val nennerBedingung: Aussage = Ungleichheit(nenner, RationaleZahl.Null),
) {
    val ausdruck: Division = Division(zaehler, nenner)
    fun zuLatex(): String = ausdruck.zuLatex()
}

fun rationaleGleichheitsBedingung(
    links: SymbolischerRationalerTerm,
    rechts: SymbolischerRationalerTerm,
): Aussage = Konjunktion(
    listOf(
        links.nennerBedingung,
        rechts.nennerBedingung,
        Gleichheit(
            multiplikation(links.zaehler, rechts.nenner),
            multiplikation(rechts.zaehler, links.nenner),
        ),
    ),
)

/** Definitionslücken bleiben Ergebniszustände und sind keine mathematischen Werte. */
data class DefinitionsLuecke(
    val operatorId: String,
    val ursache: StrukturierterAuswertungsGrund,
    val eingabenLatex: List<String> = emptyList(),
    val erwarteterTyp: String = "Objekt",
    val fehlgeschlageneDefinitionen: List<String> = emptyList(),
) {
    init { require(operatorId.isNotBlank()) }
    fun zuLatex(): String = "\\operatorname{undef}_{${operatorId.replace("_", "\\_")}}"
}

sealed interface EndlicheMaechtigkeitsInferenz {
    data object LeereMengeNullInZ : EndlicheMaechtigkeitsInferenz
    data object PositiveNatuerlicheZahl : EndlicheMaechtigkeitsInferenz
    data class Bedingt(
        val leerheitsBedingung: Aussage,
        val fallsLeer: FundamentalerZahlbereich = FundamentalerZahlbereich.GANZ,
        val fallsNichtLeer: FundamentalerZahlbereich = FundamentalerZahlbereich.NATUERLICH_POSITIV,
    ) : EndlicheMaechtigkeitsInferenz
    data object NichtEndlich : EndlicheMaechtigkeitsInferenz
}

fun inferiereEndlicheMaechtigkeit(menge: MengenAusdruck): EndlicheMaechtigkeitsInferenz = when (menge) {
    LeereMenge -> EndlicheMaechtigkeitsInferenz.LeereMengeNullInZ
    is EndlicheMenge -> if (menge.elemente.isEmpty()) {
        EndlicheMaechtigkeitsInferenz.LeereMengeNullInZ
    } else {
        EndlicheMaechtigkeitsInferenz.PositiveNatuerlicheZahl
    }
    else -> EndlicheMaechtigkeitsInferenz.Bedingt(
        leerheitsBedingung = Gleichheit(menge, LeereMenge),
    )
}

/** Kernobjekte der vier grundlegenden Definitionskarten. */
object ZahlbereichsDefinitionsKatalog {
    val natuerlicheZahlen = InduktiveDefinition(
        id = "zahlbereich.N",
        name = "Positive natürliche Zahlen",
        ziel = DefinitionsZiel.Menge("zahlbereich.N", NatürlicheZahlen),
        basisRegeln = listOf(
            DefinitionsRegel(
                id = "N.basis.1",
                name = "Basis Eins",
                folgerungLatex = "1\\in\\mathbb N",
            ),
        ),
        abschlussRegeln = listOf(
            DefinitionsRegel(
                id = "N.nachfolger",
                name = "Von-Neumann-Nachfolger",
                folgerungLatex = "n\\in\\mathbb N\\Rightarrow n\\cup\\{n\\}\\in\\mathbb N",
                rekursiveReferenzen = listOf(
                    DefinitionsReferenz(
                        definitionsId = "zahlbereich.N",
                        position = RekursionsPosition.ABSCHLUSS_VORAUSSETZUNG,
                        monotonie = NachweisStatus.Nachgewiesen,
                    ),
                ),
            ),
        ),
    )

    val nullDefinition = ExpliziteDefinition(
        id = "zahl.Null",
        name = "Null als Mächtigkeit der leeren Menge",
        ziel = DefinitionsZiel.Struktur("zahl.Null", "zahl.0"),
        wert = RationaleZahl.Null,
    )

    val ganzeZahlen = ImpliziteDefinition(
        id = "zahlbereich.Z",
        name = "Ganze Zahlen",
        ziel = DefinitionsZiel.Menge("zahlbereich.Z", GanzeZahlen),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel("Z.null", "Null", folgerungLatex = "0\\in\\mathbb Z"),
            DefinitionsRegel("Z.N", "Natürliche Einbettung", folgerungLatex = "\\mathbb N\\subset\\mathbb Z"),
            DefinitionsRegel(
                "Z.nachfolgerVorgaenger",
                "Nachfolger und Vorgänger",
                folgerungLatex = "z\\in\\mathbb Z\\Rightarrow z+1,z-1\\in\\mathbb Z",
            ),
        ),
        existenzStatus = NachweisStatus.Nachgewiesen,
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(natuerlicheZahlen.id, nullDefinition.id),
    )

    val nichtnegativeGanze = PraedikativeDefinition(
        id = "zahlbereich.N0",
        name = "Nichtnegative ganze Zahlen",
        ziel = DefinitionsZiel.Menge("zahlbereich.N0", NichtnegativeGanzeZahlenSemantik.menge),
        grundmenge = GanzeZahlen,
        praedikat = Vergleich(
            Variable("z"),
            VergleichsArt.GrößerGleich,
            RationaleZahl.Null,
        ),
        referenzen = setOf(ganzeZahlen.id),
    )

    val rationaleZahlen = ImpliziteDefinition(
        id = "zahlbereich.Q",
        name = "Rationale Divisionsterme",
        ziel = DefinitionsZiel.Menge("zahlbereich.Q", RationaleZahlen),
        charakterisierendeRegeln = listOf(
            DefinitionsRegel(
                id = "Q.division",
                name = "Definierter Divisionsterm",
                folgerungLatex = "z\\in\\mathbb Z,n\\in\\mathbb N\\Rightarrow z/n\\in\\mathbb Q",
            ),
            DefinitionsRegel(
                id = "Q.gleichheit",
                name = "Kreuzmultiplikation",
                folgerungLatex = "a/b=c/d\\Leftrightarrow ad=bc",
            ),
        ),
        existenzStatus = NachweisStatus.Nachgewiesen,
        eindeutigkeitsStatus = NachweisStatus.Nachgewiesen,
        referenzen = setOf(ganzeZahlen.id, natuerlicheZahlen.id),
    )

    val alle: List<MathematischeDefinition> = listOf(
        natuerlicheZahlen,
        nullDefinition,
        ganzeZahlen,
        nichtnegativeGanze,
        rationaleZahlen,
    )
}
