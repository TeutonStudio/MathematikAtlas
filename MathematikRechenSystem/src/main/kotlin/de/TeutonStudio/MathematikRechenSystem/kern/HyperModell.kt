package de.TeutonStudio.MathematikRechenSystem.kern

@JvmInline
value class HyperModellId(val wert: String) {
    init { require(wert.isNotBlank()) }
}

data class HyperModellBeispiel(
    val name: String,
    val beschreibung: String,
)

data class HyperModell(
    val id: HyperModellId,
    val indexMengeLatex: String,
    val filterAxiome: Set<String>,
    val frei: Boolean,
    val beispiele: List<HyperModellBeispiel> = emptyList(),
) {
    init {
        require(indexMengeLatex.isNotBlank())
        require(filterAxiome.isNotEmpty())
    }
}

object KanonischesHyperModell {
    val modell = HyperModell(
        id = HyperModellId("hyper.standard.ultrapotenz.N"),
        indexMengeLatex = "\\mathbb N",
        filterAxiome = linkedSetOf(
            "\\varnothing\\notin\\mathcal U",
            "A\\in\\mathcal U\\land A\\subseteq B\\Rightarrow B\\in\\mathcal U",
            "A,B\\in\\mathcal U\\Rightarrow A\\cap B\\in\\mathcal U",
            "A\\subseteq\\mathbb N\\Rightarrow A\\in\\mathcal U\\lor\\mathbb N\\setminus A\\in\\mathcal U",
            "\\forall F\\subseteq\\mathbb N\\text{ endlich}:F\\notin\\mathcal U",
        ),
        frei = true,
        beispiele = listOf(
            HyperModellBeispiel("Kofinite Mengen", "Jede kofinite Teilmenge von ℕ liegt in jedem freien Ultrafilter."),
        ),
    )
}

enum class HyperErweiterungsArt {
    MENGE,
    METHODE,
    RELATION,
    OPERATION,
    AUSSAGE,
    WERT,
}

data class SymbolischeHyperErweiterung(
    val grundobjekt: MathematischesObjekt,
    val art: HyperErweiterungsArt,
    val modellId: HyperModellId = KanonischesHyperModell.modell.id,
    val voraussetzungen: Set<String> = emptySet(),
) : MathematischesObjekt {
    override fun zuLatex(): String = "{}^*\\left(${grundobjekt.zuLatex()}\\right)"
}

data class ModellgebundeneHyperIndexStruktur(
    val struktur: HyperErweiterteIndexStruktur,
    val modellId: HyperModellId = KanonischesHyperModell.modell.id,
) {
    fun zuLatex(): String = struktur.zuLatex()
}

fun UnendlicheIndexStruktur.hyperErweiterung(
    modell: HyperModell,
): ModellgebundeneHyperIndexStruktur = ModellgebundeneHyperIndexStruktur(
    struktur = hyperErweiterung(),
    modellId = modell.id,
)

enum class ExternesHyperPraedikat {
    STANDARD,
    ENDLICH,
    UNENDLICH,
    INFINITESIMAL,
    STANDARDTEIL,
}

enum class TransferStatus {
    UEBERTRAGEN,
    EXTERNE_BESTANDTEILE,
    NICHT_REGISTRIERTE_SYMBOLE,
}

data class HyperUebertrageneAussage(
    val ursprung: Aussage,
    val modellId: HyperModellId,
) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = AussageErgebnis(
        wahrheitswert = null,
        status = EntscheidungsStatus.Unbekannt,
        begründung = "Die übertragene Aussage bleibt bis zu einem strukturellen Nachweis symbolisch.",
    )

    override fun zuLatex(): String = "{}^*\\left(${ursprung.zuLatex()}\\right)"
}

data class TransferErgebnis(
    val status: TransferStatus,
    val ursprung: Aussage,
    val uebertragen: HyperUebertrageneAussage? = null,
    val externeBestandteile: Set<ExternesHyperPraedikat> = emptySet(),
    val nichtRegistrierteSymbole: Set<String> = emptySet(),
)

object TransferUebersetzer {
    fun uebertrage(
        aussage: Aussage,
        externeBestandteile: Set<ExternesHyperPraedikat> = emptySet(),
        nichtRegistrierteSymbole: Set<String> = emptySet(),
        modell: HyperModell = KanonischesHyperModell.modell,
    ): TransferErgebnis = when {
        externeBestandteile.isNotEmpty() -> TransferErgebnis(
            status = TransferStatus.EXTERNE_BESTANDTEILE,
            ursprung = aussage,
            externeBestandteile = externeBestandteile,
        )
        nichtRegistrierteSymbole.isNotEmpty() -> TransferErgebnis(
            status = TransferStatus.NICHT_REGISTRIERTE_SYMBOLE,
            ursprung = aussage,
            nichtRegistrierteSymbole = nichtRegistrierteSymbole,
        )
        else -> TransferErgebnis(
            status = TransferStatus.UEBERTRAGEN,
            ursprung = aussage,
            uebertragen = HyperUebertrageneAussage(aussage, modell.id),
        )
    }
}

enum class HyperGroessenKlasse {
    ENDLICH,
    POSITIV_UNENDLICH,
    NEGATIV_UNENDLICH,
    NICHT_ENTSCHEIDBAR,
}

data class SymbolischerHyperReellerWert(
    val name: String,
    val groessenKlasse: HyperGroessenKlasse = HyperGroessenKlasse.NICHT_ENTSCHEIDBAR,
    val standardteil: ZahlAusdruck? = null,
    val modellId: HyperModellId = KanonischesHyperModell.modell.id,
) : ZahlAusdruck {
    init {
        require(name.isNotBlank())
        require(groessenKlasse == HyperGroessenKlasse.ENDLICH || standardteil == null) {
            "Nur endliche Hyperwerte dürfen einen Standardteil tragen."
        }
    }

    override fun zuLatex(): String = name
}

data object PositiveUnendlichkeit : ZahlAusdruck {
    override fun zuLatex(): String = "+\\infty"
}

data object NegativeUnendlichkeit : ZahlAusdruck {
    override fun zuLatex(): String = "-\\infty"
}

data class HyperLimes(
    val argument: ZahlAusdruck,
    val modellId: HyperModellId = KanonischesHyperModell.modell.id,
) : ZahlAusdruck {
    val operatorId: String = "analysis.hyperLimes"
    override fun zuLatex(): String = "\\operatorname{limes}\\left(${argument.zuLatex()}\\right)"
}

data class FolgenOderMethodenGrenzwert(
    val argument: MathematischesObjekt,
) : MathematischesObjekt {
    val operatorId: String = "analysis.grenzwert"
    override fun zuLatex(): String = "\\operatorname{limes}\\left(${argument.zuLatex()}\\right)"
}

sealed interface HyperLimesErgebnis {
    data class Wert(val wert: ZahlAusdruck) : HyperLimesErgebnis
    data class Bedingt(
        val ausdruck: HyperLimes,
        val voraussetzungen: Set<String>,
    ) : HyperLimesErgebnis
}

fun werteHyperLimes(wert: SymbolischerHyperReellerWert): HyperLimesErgebnis = when (wert.groessenKlasse) {
    HyperGroessenKlasse.ENDLICH -> wert.standardteil?.let(HyperLimesErgebnis::Wert)
        ?: HyperLimesErgebnis.Bedingt(
            HyperLimes(wert, wert.modellId),
            setOf("Der eindeutige Standardteil von ${wert.zuLatex()} muss bestimmt werden."),
        )
    HyperGroessenKlasse.POSITIV_UNENDLICH -> HyperLimesErgebnis.Wert(PositiveUnendlichkeit)
    HyperGroessenKlasse.NEGATIV_UNENDLICH -> HyperLimesErgebnis.Wert(NegativeUnendlichkeit)
    HyperGroessenKlasse.NICHT_ENTSCHEIDBAR -> HyperLimesErgebnis.Bedingt(
        HyperLimes(wert, wert.modellId),
        setOf("${wert.zuLatex()} muss als endlich, positiv unendlich oder negativ unendlich klassifiziert werden."),
    )
}
