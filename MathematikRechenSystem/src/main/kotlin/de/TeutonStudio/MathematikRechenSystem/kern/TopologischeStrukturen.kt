package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Die zwei von #386 geforderten, voneinander unabhängigen Kardinalitätsachsen.
 * `ABZAEHLBAR` umfasst dabei wie üblich auch endliche Mengen.
 */
enum class EndlichkeitsStatus { ENDLICH, UNENDLICH, UNENTSCHEIDBAR }
enum class AbzaehlbarkeitsStatus { ABZAEHLBAR, UEBERABZAEHLBAR, UNENTSCHEIDBAR }

data class KardinalitaetsVertrag(
    val endlichkeit: EndlichkeitsStatus,
    val abzaehlbarkeit: AbzaehlbarkeitsStatus,
    val begruendung: String,
)

/**
 * Zentraler, rein struktureller Kardinalitätsvertrag. Sichtbare Namen oder LaTeX
 * werden absichtlich niemals zur Inferenz verwendet.
 */
fun kardinalitaetsVertrag(menge: MengenAusdruck): KardinalitaetsVertrag = when (menge) {
    LeereMenge -> endlichAbzaehlbar("Die leere Menge besitzt endlich viele Elemente.")
    is EndlicheMenge -> endlichAbzaehlbar("Die Menge ist explizit endlich materialisiert.")
    is ModuloZahlenraum -> endlichAbzaehlbar("Ein Restklassenring modulo n besitzt genau n Klassen.")

    NatürlicheZahlen,
    GanzeZahlen,
    RationaleZahlen,
    Primzahlen,
    GaußscheGanzeZahlen,
    GaußschePrimzahlen,
    -> unendlichAbzaehlbar("Die Standardmenge besitzt einen registrierten abzählbar-unendlichen Kardinalitätsvertrag.")

    ReelleZahlen,
    KomplexeZahlen,
    -> unendlichUeberabzaehlbar("Die Standardmenge besitzt einen registrierten überabzählbaren Kardinalitätsvertrag.")

    is ReellesIntervall -> intervallKardinalitaet(menge)
    is Vereinigung -> vereinigungsKardinalitaet(menge.mengen)
    is Schnitt -> schnittKardinalitaet(menge.mengen)
    is MengenDifferenz -> differenzKardinalitaet(menge)
    is KartesischesProdukt -> produktKardinalitaet(menge.mengen)
    is Tupelraum -> produktKardinalitaet(menge.komponenten)
    is Vektorraum -> potenzKardinalitaet(menge.skalarMenge, menge.dimension)
    is Matrizenraum -> potenzKardinalitaet(menge.skalarMenge, menge.zeilen * menge.spalten)
    is Tensorraum -> tensorKardinalitaet(menge)
    is Potenzmenge -> potenzmengenKardinalitaet(menge.grundMenge)
    is GefilterteMenge -> teilMengenKardinalitaet(menge.menge)
    is DefinierteMenge -> definierteMengenKardinalitaet(menge)
    is Folgenraum -> folgenraumKardinalitaet(menge.elementMenge)
    else -> KardinalitaetsVertrag(
        EndlichkeitsStatus.UNENTSCHEIDBAR,
        AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
        "Für diese symbolische Mengenstruktur existiert kein ausreichender Kardinalitätsnachweis.",
    )
}

private fun endlichAbzaehlbar(begruendung: String) = KardinalitaetsVertrag(
    EndlichkeitsStatus.ENDLICH,
    AbzaehlbarkeitsStatus.ABZAEHLBAR,
    begruendung,
)

private fun unendlichAbzaehlbar(begruendung: String) = KardinalitaetsVertrag(
    EndlichkeitsStatus.UNENDLICH,
    AbzaehlbarkeitsStatus.ABZAEHLBAR,
    begruendung,
)

private fun unendlichUeberabzaehlbar(begruendung: String) = KardinalitaetsVertrag(
    EndlichkeitsStatus.UNENDLICH,
    AbzaehlbarkeitsStatus.UEBERABZAEHLBAR,
    begruendung,
)

private fun intervallKardinalitaet(intervall: ReellesIntervall): KardinalitaetsVertrag {
    val links = intervall.links as? RationaleZahl
    val rechts = intervall.rechts as? RationaleZahl
    return when {
        links != null && rechts != null && links > rechts -> endlichAbzaehlbar("Das Intervall ist leer.")
        links != null && rechts != null && links == rechts && (intervall.linksOffen || intervall.rechtsOffen) ->
            endlichAbzaehlbar("Das offene degenerierte Intervall ist leer.")
        links != null && rechts != null && links == rechts -> endlichAbzaehlbar("Das degenerierte abgeschlossene Intervall ist eine einelementige Menge.")
        links != null && rechts != null -> unendlichUeberabzaehlbar("Jedes nichtdegenerierte reelle Intervall ist überabzählbar.")
        else -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
            "Die symbolischen Intervallgrenzen erlauben noch keinen Nichtentartungsnachweis.",
        )
    }
}

private fun vereinigungsKardinalitaet(mengen: List<MengenAusdruck>): KardinalitaetsVertrag {
    if (mengen.isEmpty()) return endlichAbzaehlbar("Die leere endliche Vereinigung ist leer.")
    val vertraege = mengen.map(::kardinalitaetsVertrag)
    if (vertraege.all { it.endlichkeit == EndlichkeitsStatus.ENDLICH }) {
        return endlichAbzaehlbar("Eine endliche Vereinigung endlicher Mengen ist endlich.")
    }
    if (vertraege.any { it.abzaehlbarkeit == AbzaehlbarkeitsStatus.UEBERABZAEHLBAR }) {
        return unendlichUeberabzaehlbar("Die Vereinigung enthält eine nachweisbar überabzählbare Teilmenge.")
    }
    if (vertraege.all { it.abzaehlbarkeit == AbzaehlbarkeitsStatus.ABZAEHLBAR }) {
        return KardinalitaetsVertrag(
            if (vertraege.any { it.endlichkeit == EndlichkeitsStatus.UNENDLICH }) EndlichkeitsStatus.UNENDLICH
            else EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.ABZAEHLBAR,
            "Eine endliche Vereinigung abzählbarer Mengen ist abzählbar.",
        )
    }
    return KardinalitaetsVertrag(
        EndlichkeitsStatus.UNENTSCHEIDBAR,
        AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
        "Mindestens ein Vereinigungsfaktor besitzt unbekannte Kardinalität.",
    )
}

private fun schnittKardinalitaet(mengen: List<MengenAusdruck>): KardinalitaetsVertrag {
    val vertraege = mengen.map(::kardinalitaetsVertrag)
    if (vertraege.any { it.endlichkeit == EndlichkeitsStatus.ENDLICH }) {
        return endlichAbzaehlbar("Der Schnitt ist Teilmenge eines endlichen Faktors.")
    }
    if (vertraege.any { it.abzaehlbarkeit == AbzaehlbarkeitsStatus.ABZAEHLBAR }) {
        return KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.ABZAEHLBAR,
            "Der Schnitt ist Teilmenge eines abzählbaren Faktors.",
        )
    }
    return KardinalitaetsVertrag(
        EndlichkeitsStatus.UNENTSCHEIDBAR,
        AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
        "Ein Schnitt überabzählbarer oder unbekannter Mengen kann wesentlich kleiner sein.",
    )
}

private fun differenzKardinalitaet(differenz: MengenDifferenz): KardinalitaetsVertrag {
    val links = kardinalitaetsVertrag(differenz.links)
    return when {
        links.endlichkeit == EndlichkeitsStatus.ENDLICH -> endlichAbzaehlbar("Eine Teilmenge einer endlichen Menge ist endlich.")
        links.abzaehlbarkeit == AbzaehlbarkeitsStatus.ABZAEHLBAR -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.ABZAEHLBAR,
            "Eine Teilmenge einer abzählbaren Menge ist abzählbar.",
        )
        else -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
            "Aus einer Mengendifferenz folgt ohne Information über den entfernten Anteil keine exakte Kardinalitätsklasse.",
        )
    }
}

private fun produktKardinalitaet(faktoren: List<MengenAusdruck>): KardinalitaetsVertrag {
    if (faktoren.isEmpty()) return endlichAbzaehlbar("Das leere Produkt besitzt genau ein leeres Tupel.")
    if (faktoren.any { it == LeereMenge }) return endlichAbzaehlbar("Ein kartesisches Produkt mit leerem Faktor ist leer.")
    val vertraege = faktoren.map(::kardinalitaetsVertrag)
    if (vertraege.all { it.endlichkeit == EndlichkeitsStatus.ENDLICH }) {
        return endlichAbzaehlbar("Ein endliches Produkt endlicher Mengen ist endlich.")
    }
    if (vertraege.all { it.abzaehlbarkeit == AbzaehlbarkeitsStatus.ABZAEHLBAR }) {
        return unendlichAbzaehlbar("Ein endliches Produkt abzählbarer, nichtleerer Mengen ist abzählbar; mindestens ein Faktor ist unendlich.")
    }
    if (
        vertraege.any { it.abzaehlbarkeit == AbzaehlbarkeitsStatus.UEBERABZAEHLBAR } &&
        faktoren.all(::istNachweisbarNichtLeer)
    ) {
        return unendlichUeberabzaehlbar("Das Produkt enthält einen überabzählbaren Faktor und alle übrigen Faktoren sind nachweisbar nichtleer.")
    }
    return KardinalitaetsVertrag(
        EndlichkeitsStatus.UNENTSCHEIDBAR,
        AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
        "Die Kardinalität mindestens eines Produktfaktors oder dessen Nichtleerheit ist unbestimmt.",
    )
}

private fun potenzKardinalitaet(basis: MengenAusdruck, exponent: Int): KardinalitaetsVertrag {
    require(exponent > 0)
    return produktKardinalitaet(List(exponent) { basis })
}

private fun tensorKardinalitaet(raum: Tensorraum): KardinalitaetsVertrag {
    val dimensionen = raum.dimensionen.map { it as? RationaleZahl }
    if (dimensionen.any { it == null || it.nenner != java.math.BigInteger.ONE }) {
        return KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
            "Die Tensorform ist nicht vollständig als endliche natürliche Dimension nachgewiesen.",
        )
    }
    val exponent = dimensionen.filterNotNull().fold(1) { produkt, wert ->
        Math.multiplyExact(produkt, wert.zähler.intValueExact())
    }
    return potenzKardinalitaet(raum.elementMenge, exponent)
}

private fun potenzmengenKardinalitaet(grundMenge: MengenAusdruck): KardinalitaetsVertrag {
    val basis = kardinalitaetsVertrag(grundMenge)
    return when (basis.endlichkeit) {
        EndlichkeitsStatus.ENDLICH -> endlichAbzaehlbar("Die Potenzmenge einer endlichen Menge ist endlich.")
        EndlichkeitsStatus.UNENDLICH -> unendlichUeberabzaehlbar("Nach Cantor ist die Potenzmenge einer unendlichen Menge überabzählbar.")
        EndlichkeitsStatus.UNENTSCHEIDBAR -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
            "Die Kardinalität der Grundmenge ist nicht ausreichend bestimmt.",
        )
    }
}

private fun teilMengenKardinalitaet(oberMenge: MengenAusdruck): KardinalitaetsVertrag {
    val ober = kardinalitaetsVertrag(oberMenge)
    return when {
        ober.endlichkeit == EndlichkeitsStatus.ENDLICH -> endlichAbzaehlbar("Eine Teilmenge einer endlichen Menge ist endlich.")
        ober.abzaehlbarkeit == AbzaehlbarkeitsStatus.ABZAEHLBAR -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.ABZAEHLBAR,
            "Eine Teilmenge einer abzählbaren Menge ist abzählbar.",
        )
        else -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
            "Eine Teilmenge einer überabzählbaren Menge kann endlich, abzählbar oder überabzählbar sein.",
        )
    }
}

private fun definierteMengenKardinalitaet(menge: DefinierteMenge): KardinalitaetsVertrag {
    val produkt = produktKardinalitaet(menge.variablen.map { it.grundMenge })
    return when {
        produkt.endlichkeit == EndlichkeitsStatus.ENDLICH -> endlichAbzaehlbar("Die definierte Menge liegt in einem endlichen Grundraum.")
        produkt.abzaehlbarkeit == AbzaehlbarkeitsStatus.ABZAEHLBAR -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.ABZAEHLBAR,
            "Die definierte Menge ist Teilmenge eines abzählbaren endlichen Produktraums.",
        )
        else -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
            "Die Prädikatsbedingung kann die Kardinalität des Grundraums wesentlich reduzieren.",
        )
    }
}

private fun folgenraumKardinalitaet(elementMenge: MengenAusdruck): KardinalitaetsVertrag {
    val elemente = kardinalitaetsVertrag(elementMenge)
    return when (elemente.abzaehlbarkeit) {
        AbzaehlbarkeitsStatus.ABZAEHLBAR -> unendlichAbzaehlbar(
            "Die Menge endlicher Folgen über einer nichtleeren abzählbaren Menge ist abzählbar.",
        )
        AbzaehlbarkeitsStatus.UEBERABZAEHLBAR -> unendlichUeberabzaehlbar(
            "Bereits die Folgen der Länge eins bilden eine überabzählbare Teilmenge.",
        )
        AbzaehlbarkeitsStatus.UNENTSCHEIDBAR -> KardinalitaetsVertrag(
            EndlichkeitsStatus.UNENTSCHEIDBAR,
            AbzaehlbarkeitsStatus.UNENTSCHEIDBAR,
            "Die Elementmenge besitzt keinen ausreichenden Kardinalitätsvertrag.",
        )
    }
}

private fun istNachweisbarNichtLeer(menge: MengenAusdruck): Boolean = when (menge) {
    LeereMenge -> false
    is EndlicheMenge -> menge.elemente.isNotEmpty()
    NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen, KomplexeZahlen,
    Primzahlen, GaußscheGanzeZahlen, GaußschePrimzahlen,
    -> true
    is ModuloZahlenraum -> true
    else -> kardinalitaetsVertrag(menge).endlichkeit == EndlichkeitsStatus.UNENDLICH
}

/** Mathematischer Topologievertrag; die Topologie trägt ihren Träger selbst. */
sealed interface Topologie : MathematischesObjekt {
    val traeger: MengenAusdruck
}

data class DiskreteTopologie(override val traeger: MengenAusdruck) : Topologie {
    override fun zuLatex(): String = "\\mathcal{P}\\left(${traeger.zuLatex()}\\right)"
}

data class IndiskreteTopologie(override val traeger: MengenAusdruck) : Topologie {
    override fun zuLatex(): String = "\\{\\varnothing,${traeger.zuLatex()}\\}"
}

enum class StandardTopologieKennung(val persistenzWert: String) {
    REELL("standard-reell"),
    KOMPLEX("standard-komplex"),
    EUKLIDISCH("standard-euklidisch"),
}

data class StandardTopologie(
    override val traeger: MengenAusdruck,
    val kennung: StandardTopologieKennung,
) : Topologie {
    override fun zuLatex(): String = "\\tau_{\\mathrm{std}}(${traeger.zuLatex()})"
}

data class SymbolischeTopologie(
    override val traeger: MengenAusdruck,
    val symbol: String = "\\tau",
) : Topologie {
    init { require(symbol.isNotBlank()) }
    override fun zuLatex(): String = symbol
}

data class TeilraumTopologie(
    override val traeger: MengenAusdruck,
    val oberRaum: TopologischerRaum,
) : Topologie {
    override fun zuLatex(): String = "${oberRaum.topologie.zuLatex()}|_{${traeger.zuLatex()}}"
}

data class ProduktTopologie(
    override val traeger: MengenAusdruck,
    val faktoren: List<TopologischerRaum>,
) : Topologie {
    init { require(faktoren.size >= 2) }
    override fun zuLatex(): String = faktoren.joinToString("\\times") { it.topologie.zuLatex() }
}

data class MetrischInduzierteTopologie(
    override val traeger: MengenAusdruck,
    val metrik: Methode,
) : Topologie {
    override fun zuLatex(): String = "\\tau_{${metrik.name}}"
}

data class TopologischerRaum(
    val traeger: MengenAusdruck,
    val topologie: Topologie,
) : MathematischesObjekt {
    init {
        require(topologie.traeger == traeger) {
            "Die Topologie ${topologie.zuLatex()} gehört nicht zum Träger ${traeger.zuLatex()}."
        }
    }

    override fun zuLatex(): String = "\\left(${traeger.zuLatex()},${topologie.zuLatex()}\\right)"
}

data class MetrischerRaum(
    val traeger: MengenAusdruck,
    val metrik: Methode,
) : MathematischesObjekt {
    val induzierteTopologie: MetrischInduzierteTopologie
        get() = MetrischInduzierteTopologie(traeger, metrik)

    val alsTopologischerRaum: TopologischerRaum
        get() = TopologischerRaum(traeger, induzierteTopologie)

    override fun zuLatex(): String = "\\left(${traeger.zuLatex()},${metrik.name}\\right)"
}

enum class TopologieAxiomStatus { BEWIESEN, BEDINGT, UNENTSCHEIDBAR }

fun topologieAxiomStatus(topologie: Topologie): TopologieAxiomStatus = when (topologie) {
    is DiskreteTopologie,
    is IndiskreteTopologie,
    is StandardTopologie,
    is TeilraumTopologie,
    is ProduktTopologie,
    is MetrischInduzierteTopologie,
    -> TopologieAxiomStatus.BEWIESEN
    is SymbolischeTopologie -> TopologieAxiomStatus.BEDINGT
}

/** Kanonische Topologien existieren nur für ausdrücklich registrierte Träger. */
object StandardTopologieRegister {
    fun fuer(traeger: MengenAusdruck): Topologie? = when (traeger) {
        ReelleZahlen -> StandardTopologie(traeger, StandardTopologieKennung.REELL)
        KomplexeZahlen -> StandardTopologie(traeger, StandardTopologieKennung.KOMPLEX)
        is ReellesIntervall -> TeilraumTopologie(
            traeger,
            TopologischerRaum(ReelleZahlen, StandardTopologie(ReelleZahlen, StandardTopologieKennung.REELL)),
        )
        is Vektorraum -> when (traeger.skalarMenge) {
            ReelleZahlen, KomplexeZahlen -> StandardTopologie(traeger, StandardTopologieKennung.EUKLIDISCH)
            else -> null
        }
        is Matrizenraum -> when (traeger.skalarMenge) {
            ReelleZahlen, KomplexeZahlen -> StandardTopologie(traeger, StandardTopologieKennung.EUKLIDISCH)
            else -> null
        }
        else -> null
    }
}

data class MetrikVertrag(
    val traeger: MengenAusdruck,
    val metrik: Methode,
    val offeneAxiome: List<String>,
)

/**
 * Prüft die Signatur einer Metrik strukturell. Die vier Metrikaxiome bleiben als
 * explizite Bedingungen erhalten, solange der CAS sie nicht beweisen kann.
 */
fun pruefeMetrik(traeger: MengenAusdruck, metrik: Methode): StrukturPruefung<MetrikVertrag> {
    val signatur = runCatching { metrik.methodenSignatur() }.getOrElse {
        return StrukturPruefung.Ungueltig(it.message ?: "Die Metrik besitzt keine vollständige Methodensignatur.")
    }
    if (signatur.argumente.size != 2) {
        return StrukturPruefung.Ungueltig("Eine Metrik benötigt genau zwei Argumente aus demselben Träger.")
    }
    if (signatur.argumente.any { it.werteVorrat != traeger }) {
        return StrukturPruefung.Ungueltig("Beide Metrikargumente müssen den Träger ${traeger.zuLatex()} als Wertevorrat besitzen.")
    }
    if (!signatur.zielMenge.istZahlenmenge()) {
        return StrukturPruefung.Ungueltig("Eine Metrik muss reelle, nichtnegative Werte liefern.")
    }
    val axiome = listOf(
        "d(x,y) \\ge 0",
        "d(x,y)=0 \\Leftrightarrow x=y",
        "d(x,y)=d(y,x)",
        "d(x,z) \\le d(x,y)+d(y,z)",
    )
    return StrukturPruefung.Bedingt(
        MetrikVertrag(traeger, metrik, axiome),
        listOf("Nichtnegativität", "Definitheit", "Symmetrie", "Dreiecksungleichung"),
    )
}

/** Konservative Teilmengenprüfung für die derzeit strukturell bekannten Fälle. */
fun teilMengenStatus(teil: MengenAusdruck, ganz: MengenAusdruck): AussageStatus = when {
    teil == LeereMenge || teil == ganz -> AussageStatus.BEWIESEN
    ganz is EndlicheMenge && teil is EndlicheMenge ->
        if (teil.elemente.all { it in ganz.elemente }) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
    ganz == ReelleZahlen && teil.istZahlenmenge() && teil != KomplexeZahlen -> AussageStatus.BEWIESEN
    else -> AussageStatus.UNENTSCHEIDBAR
}

fun TopologischerRaum.offenheitsStatus(menge: MengenAusdruck): AussageStatus =
    topologischeEigenschaft(menge, offen = true)

fun TopologischerRaum.abgeschlossenheitsStatus(menge: MengenAusdruck): AussageStatus =
    topologischeEigenschaft(menge, offen = false)

private fun TopologischerRaum.topologischeEigenschaft(
    menge: MengenAusdruck,
    offen: Boolean,
): AussageStatus {
    val teilmenge = teilMengenStatus(menge, traeger)
    if (teilmenge == AussageStatus.WIDERLEGT) return AussageStatus.WIDERLEGT
    if (menge == LeereMenge || menge == traeger) return AussageStatus.BEWIESEN
    if (teilmenge != AussageStatus.BEWIESEN) return AussageStatus.BEDINGT

    return when (val tau = topologie) {
        is DiskreteTopologie -> AussageStatus.BEWIESEN
        is IndiskreteTopologie -> AussageStatus.WIDERLEGT
        is StandardTopologie -> standardTopologischeEigenschaft(tau, menge, offen)
        is TeilraumTopologie -> if (menge == tau.traeger) AussageStatus.BEWIESEN else AussageStatus.UNENTSCHEIDBAR
        is ProduktTopologie,
        is MetrischInduzierteTopologie,
        is SymbolischeTopologie,
        -> AussageStatus.UNENTSCHEIDBAR
    }
}

private fun standardTopologischeEigenschaft(
    topologie: StandardTopologie,
    menge: MengenAusdruck,
    offen: Boolean,
): AussageStatus {
    if (topologie.kennung != StandardTopologieKennung.REELL || topologie.traeger != ReelleZahlen) {
        return AussageStatus.UNENTSCHEIDBAR
    }
    return if (offen) {
        when (menge) {
            is ReellesIntervall -> if (menge.linksOffen && menge.rechtsOffen) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
            is EndlicheMenge -> if (menge.elemente.isEmpty()) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
            else -> AussageStatus.UNENTSCHEIDBAR
        }
    } else {
        when (menge) {
            is ReellesIntervall -> if (!menge.linksOffen && !menge.rechtsOffen) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
            is EndlicheMenge -> AussageStatus.BEWIESEN
            else -> AussageStatus.UNENTSCHEIDBAR
        }
    }
}
