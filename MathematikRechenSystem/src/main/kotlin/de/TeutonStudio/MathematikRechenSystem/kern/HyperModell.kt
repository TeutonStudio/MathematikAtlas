package de.TeutonStudio.MathematikRechenSystem.kern

@JvmInline
value class HyperModellId(val wert: String) {
    init { require(wert.isNotBlank()) }
}

enum class HyperAxiomArt {
    FILTER,
    ULTRAFILTER,
    FREIHEIT,
}

data class HyperAxiom(
    val id: String,
    val art: HyperAxiomArt,
    val latex: String,
    val beschreibung: String,
) {
    init {
        require(id.isNotBlank())
        require(latex.isNotBlank())
        require(beschreibung.isNotBlank())
    }

    val aussage: Aussage = UnentscheidbareAussage(
        bezeichnung = id,
        system = "Axiom des symbolischen Hypermodells",
    )
}

data class HyperModellBeispiel(
    val name: String,
    val beschreibung: String,
    val beweisStatus: NachweisStatus = NachweisStatus.Unvollstaendig,
)

data class HyperModell(
    val id: HyperModellId,
    val indexMenge: MengenAusdruck,
    val filterAxiome: Set<HyperAxiom>,
    val frei: HyperAxiom,
    val beispiele: List<HyperModellBeispiel> = emptyList(),
) {
    init {
        require(filterAxiome.isNotEmpty())
        require(frei.art == HyperAxiomArt.FREIHEIT)
        require(filterAxiome.map { it.id }.distinct().size == filterAxiome.size)
        require(frei.id !in filterAxiome.map { it.id })
    }

    val alleAxiome: Set<HyperAxiom> get() = filterAxiome + frei
}

object KanonischesHyperModell {
    private val leerNichtImFilter = HyperAxiom(
        id = "hyper.filter.leerNichtEnthalten",
        art = HyperAxiomArt.FILTER,
        latex = "\\varnothing\\notin\\mathcal U",
        beschreibung = "Der Filter enthält nicht die leere Menge.",
    )
    private val obermengenAbschluss = HyperAxiom(
        id = "hyper.filter.obermengenAbschluss",
        art = HyperAxiomArt.FILTER,
        latex = "A\\in\\mathcal U\\land A\\subseteq B\\Rightarrow B\\in\\mathcal U",
        beschreibung = "Der Filter ist unter Obermengen abgeschlossen.",
    )
    private val schnittAbschluss = HyperAxiom(
        id = "hyper.filter.schnittAbschluss",
        art = HyperAxiomArt.FILTER,
        latex = "A,B\\in\\mathcal U\\Rightarrow A\\cap B\\in\\mathcal U",
        beschreibung = "Der Filter ist unter endlichen Schnitten abgeschlossen.",
    )
    private val ultrafilterEntscheidung = HyperAxiom(
        id = "hyper.ultrafilter.entscheidung",
        art = HyperAxiomArt.ULTRAFILTER,
        latex = "A\\subseteq\\mathbb N\\Rightarrow A\\in\\mathcal U\\lor\\mathbb N\\setminus A\\in\\mathcal U",
        beschreibung = "Für jede Teilmenge liegt sie selbst oder ihr Komplement im Ultrafilter.",
    )
    private val freiheit = HyperAxiom(
        id = "hyper.filter.frei",
        art = HyperAxiomArt.FREIHEIT,
        latex = "\\forall F\\subseteq\\mathbb N\\text{ endlich}:F\\notin\\mathcal U",
        beschreibung = "Keine endliche Teilmenge der Indexmenge liegt im freien Ultrafilter.",
    )

    val modell = HyperModell(
        id = HyperModellId("hyper.standard.ultrapotenz.N"),
        indexMenge = NatürlicheZahlen,
        filterAxiome = linkedSetOf(
            leerNichtImFilter,
            obermengenAbschluss,
            schnittAbschluss,
            ultrafilterEntscheidung,
        ),
        frei = freiheit,
        beispiele = listOf(
            HyperModellBeispiel(
                name = "Kofinite Mengen",
                beschreibung = "Jede kofinite Teilmenge von ℕ liegt in jedem freien Ultrafilter.",
                beweisStatus = NachweisStatus.Nachgewiesen,
            ),
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
    val voraussetzungen: Set<Aussage> = emptySet(),
) : Ausdruck {
    init {
        require(grundobjekt.zuLatex() !in setOf("\\mathbb H", "{}^*\\mathbb H")) {
            "Die Hypererweiterung der Quaternionen ist in der ersten Umsetzung nicht registriert."
        }
    }

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

data class SymbolischeHyperendlicheStruktur(
    val grundstruktur: MathematischesObjekt,
    val hyperIndex: ZahlAusdruck,
    val modellId: HyperModellId = KanonischesHyperModell.modell.id,
    val sichtfenster: List<MathematischesObjekt> = emptyList(),
    val beschraenktheitsStatus: NachweisStatus = NachweisStatus.Unentscheidbar,
    val cauchyStatus: NachweisStatus = NachweisStatus.Unentscheidbar,
) : Ausdruck {
    override fun zuLatex(): String =
        "{}^*\\left(${grundstruktur.zuLatex()}\\right)_{${hyperIndex.zuLatex()}}"
}

enum class ExternesHyperPraedikat(
    val operatorId: String,
    val sichtbarerName: String,
) {
    STANDARD("analysis.hyper.standard", "standard"),
    ENDLICH("analysis.hyper.endlich", "endlich"),
    UNENDLICH("analysis.hyper.unendlich", "unendlich"),
    INFINITESIMAL("analysis.hyper.infinitesimal", "infinitesimal"),
    STANDARDTEIL("analysis.hyper.standardteil", "Standardteil"),
}

private const val EXTERNES_HYPER_SYSTEM_PREFIX = "hyper.extern:"
private const val NICHT_REGISTRIERTES_HYPER_SYSTEM_PREFIX = "hyper.symbol:"

fun externesHyperPraedikat(
    art: ExternesHyperPraedikat,
    argument: MathematischesObjekt,
): Aussage = UnentscheidbareAussage(
    bezeichnung = "${art.sichtbarerName}(${argument.zuLatex()})",
    system = "$EXTERNES_HYPER_SYSTEM_PREFIX${art.name}",
)

fun nichtRegistriertesHyperSymbol(
    symbolId: String,
): Aussage {
    require(symbolId.isNotBlank())
    return UnentscheidbareAussage(
        bezeichnung = symbolId,
        system = "$NICHT_REGISTRIERTES_HYPER_SYSTEM_PREFIX$symbolId",
    )
}

data class AusdruckReferenz(
    val id: String,
    val latex: String,
) {
    init {
        require(id.isNotBlank())
        require(latex.isNotBlank())
    }
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
    val externeReferenzen: Set<AusdruckReferenz> = emptySet(),
    val nichtRegistrierteSymbole: Set<String> = emptySet(),
    val voraussetzungen: Set<Aussage> = emptySet(),
)

object TransferUebersetzer {
    fun uebertrage(
        aussage: Aussage,
        externeBestandteile: Set<ExternesHyperPraedikat> = emptySet(),
        nichtRegistrierteSymbole: Set<String> = emptySet(),
        modell: HyperModell = KanonischesHyperModell.modell,
    ): TransferErgebnis {
        val analyse = analysiere(aussage)
        val externe = analyse.externeBestandteile + externeBestandteile
        val unbekannt = analyse.nichtRegistrierteSymbole + nichtRegistrierteSymbole
        return when {
            externe.isNotEmpty() -> TransferErgebnis(
                status = TransferStatus.EXTERNE_BESTANDTEILE,
                ursprung = aussage,
                externeBestandteile = externe,
                externeReferenzen = analyse.externeReferenzen,
                voraussetzungen = analyse.voraussetzungen,
            )
            unbekannt.isNotEmpty() -> TransferErgebnis(
                status = TransferStatus.NICHT_REGISTRIERTE_SYMBOLE,
                ursprung = aussage,
                nichtRegistrierteSymbole = unbekannt,
                voraussetzungen = analyse.voraussetzungen,
            )
            else -> TransferErgebnis(
                status = TransferStatus.UEBERTRAGEN,
                ursprung = aussage,
                uebertragen = HyperUebertrageneAussage(aussage, modell.id),
                voraussetzungen = analyse.voraussetzungen,
            )
        }
    }

    private data class Analyse(
        val externeBestandteile: Set<ExternesHyperPraedikat> = emptySet(),
        val externeReferenzen: Set<AusdruckReferenz> = emptySet(),
        val nichtRegistrierteSymbole: Set<String> = emptySet(),
        val voraussetzungen: Set<Aussage> = emptySet(),
    ) {
        operator fun plus(weitere: Analyse) = Analyse(
            externeBestandteile = externeBestandteile + weitere.externeBestandteile,
            externeReferenzen = externeReferenzen + weitere.externeReferenzen,
            nichtRegistrierteSymbole = nichtRegistrierteSymbole + weitere.nichtRegistrierteSymbole,
            voraussetzungen = voraussetzungen + weitere.voraussetzungen,
        )
    }

    private fun analysiere(aussage: Aussage): Analyse {
        val lokal = if (aussage is UnentscheidbareAussage) {
            when {
                aussage.system.startsWith(EXTERNES_HYPER_SYSTEM_PREFIX) -> {
                    val name = aussage.system.removePrefix(EXTERNES_HYPER_SYSTEM_PREFIX)
                    val art = ExternesHyperPraedikat.entries.firstOrNull { it.name == name }
                    if (art == null) Analyse(nichtRegistrierteSymbole = setOf(name)) else Analyse(
                        externeBestandteile = setOf(art),
                        externeReferenzen = setOf(AusdruckReferenz(art.operatorId, aussage.zuLatex())),
                    )
                }
                aussage.system.startsWith(NICHT_REGISTRIERTES_HYPER_SYSTEM_PREFIX) -> Analyse(
                    nichtRegistrierteSymbole = setOf(
                        aussage.system.removePrefix(NICHT_REGISTRIERTES_HYPER_SYSTEM_PREFIX),
                    ),
                )
                else -> Analyse(voraussetzungen = setOf(aussage))
            }
        } else {
            Analyse()
        }
        return aussage.unterAussagen().fold(lokal) { akk, kind -> akk + analysiere(kind) }
    }

    private fun Aussage.unterAussagen(): List<Aussage> = when (this) {
        is Negation -> listOf(aussage)
        is Konjunktion -> aussagen
        is Disjunktion -> aussagen
        is Implikation -> listOf(voraussetzung, folgerung)
        is Äquivalenz -> listOf(links, rechts)
        is Adjunktion -> listOf(links, rechts)
        else -> emptyList()
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
    val voraussetzungen: Set<Aussage> = emptySet(),
) : ZahlAusdruck {
    init {
        require(name.isNotBlank())
        require(groessenKlasse == HyperGroessenKlasse.ENDLICH || standardteil == null) {
            "Nur endliche Hyperwerte dürfen einen Standardteil tragen."
        }
    }

    override fun zuLatex(): String = name
}

sealed interface ErweiterterReellerWert : Ausdruck

data class EndlicherErweiterterReellerWert(
    val wert: ZahlAusdruck,
) : ErweiterterReellerWert {
    override fun zuLatex(): String = wert.zuLatex()
}

data object PositiveUnendlichkeit : ErweiterterReellerWert {
    override fun zuLatex(): String = "+\\infty"
}

data object NegativeUnendlichkeit : ErweiterterReellerWert {
    override fun zuLatex(): String = "-\\infty"
}

data class HyperLimes(
    val argument: ZahlAusdruck,
    val modellId: HyperModellId = KanonischesHyperModell.modell.id,
) : Ausdruck {
    val operatorId: String = "analysis.hyperLimes"
    override fun zuLatex(): String = "\\operatorname{limes}\\left(${argument.zuLatex()}\\right)"
}

data class FolgenOderMethodenGrenzwert(
    val argument: MathematischesObjekt,
) : Ausdruck {
    val operatorId: String = "analysis.grenzwert"
    override fun zuLatex(): String = "\\operatorname{limes}\\left(${argument.zuLatex()}\\right)"
}

sealed interface HyperLimesErgebnis {
    data class Wert(val wert: ErweiterterReellerWert) : HyperLimesErgebnis
    data class Bedingt(
        val ausdruck: HyperLimes,
        val voraussetzungen: Set<Aussage>,
    ) : HyperLimesErgebnis
}

fun werteHyperLimes(wert: SymbolischerHyperReellerWert): HyperLimesErgebnis = when (wert.groessenKlasse) {
    HyperGroessenKlasse.ENDLICH -> wert.standardteil?.let {
        HyperLimesErgebnis.Wert(EndlicherErweiterterReellerWert(it))
    } ?: HyperLimesErgebnis.Bedingt(
        HyperLimes(wert, wert.modellId),
        wert.voraussetzungen + UnentscheidbareAussage(
            "Standardteil(${wert.zuLatex()})",
            "symbolischer Hyper-Limes",
        ),
    )
    HyperGroessenKlasse.POSITIV_UNENDLICH -> HyperLimesErgebnis.Wert(PositiveUnendlichkeit)
    HyperGroessenKlasse.NEGATIV_UNENDLICH -> HyperLimesErgebnis.Wert(NegativeUnendlichkeit)
    HyperGroessenKlasse.NICHT_ENTSCHEIDBAR -> HyperLimesErgebnis.Bedingt(
        HyperLimes(wert, wert.modellId),
        wert.voraussetzungen + UnentscheidbareAussage(
            "Klasse(${wert.zuLatex()})",
            "symbolischer Hyper-Limes",
        ),
    )
}
