package de.TeutonStudio.MathematikRechenSystem.kern

data class EndlicheMenge(val elemente: Set<MathematischesObjekt>) : MengenAusdruck {
    override fun zuLatex() = elemente.sortedBy(::strukturellerSchlüssel).joinToString(prefix = "\\{", postfix = "\\}") { it.zuLatex() }
}

data object LeereMenge : MengenAusdruck { override fun zuLatex() = "\\varnothing" }

data class BenannteMenge(val name: String, val latex: String = name) : MengenAusdruck { override fun zuLatex() = latex }

/** Reelles Intervall mit unabhängig offenen oder geschlossenen Grenzen. */
data class ReellesIntervall(
    val links: ZahlAusdruck,
    val linksOffen: Boolean,
    val rechts: ZahlAusdruck,
    val rechtsOffen: Boolean,
) : MengenAusdruck {
    override fun zuLatex(): String {
        val linkeRelation = if (linksOffen) "<" else "\\leq"
        val rechteRelation = if (rechtsOffen) "<" else "\\leq"
        return "{}^{${links.zuLatex()}$linkeRelation}\\mathbb{R}^{${rechteRelation}${rechts.zuLatex()}}"
    }
}

/**
 * Erzeugt ein reelles Intervall und normalisiert exakt entscheidbare rationale
 * Randfälle zu einer Menge mit derselben Bedeutung.
 */
fun reellesIntervall(
    links: ZahlAusdruck,
    linksOffen: Boolean,
    rechts: ZahlAusdruck,
    rechtsOffen: Boolean,
    kontext: RechenKontext = RechenKontext(),
): MengenAusdruck {
    val linkeGrenze = vereinfache(links, kontext)
    val rechteGrenze = vereinfache(rechts, kontext)
    if (linkeGrenze is RationaleZahl && rechteGrenze is RationaleZahl) {
        return when {
            linkeGrenze > rechteGrenze -> LeereMenge
            linkeGrenze == rechteGrenze && (linksOffen || rechtsOffen) -> LeereMenge
            linkeGrenze == rechteGrenze -> EndlicheMenge(setOf(linkeGrenze))
            else -> ReellesIntervall(linkeGrenze, linksOffen, rechteGrenze, rechtsOffen)
        }
    }
    return ReellesIntervall(linkeGrenze, linksOffen, rechteGrenze, rechtsOffen)
}

data class Vereinigung(val mengen: List<MengenAusdruck>) : MengenAusdruck {
    override fun zuLatex() = mengen.joinToString(" \\cup ") { it.zuLatex() }
}

data class Schnitt(val mengen: List<MengenAusdruck>, val grundMenge: MengenAusdruck? = null) : MengenAusdruck {
    override fun zuLatex() = mengen.joinToString(" \\cap ") { it.zuLatex() }
}

data class MengenDifferenz(val links: MengenAusdruck, val rechts: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex() = "${links.zuLatex()} \\setminus ${rechts.zuLatex()}"
}

fun mengenDifferenz(links: MengenAusdruck, rechts: MengenAusdruck): MengenAusdruck = when {
    links == LeereMenge -> LeereMenge
    rechts == LeereMenge -> links
    links is EndlicheMenge && rechts is EndlicheMenge -> {
        val entscheidungen = links.elemente.associateWith { ElementBeziehung(it, rechts).entscheide().wahrheitswert }
        if (entscheidungen.values.any { it == null }) MengenDifferenz(links, rechts)
        else EndlicheMenge(entscheidungen.filterValues { it == Wahrheitswert.Lüge }.keys)
    }
    else -> MengenDifferenz(links, rechts)
}

/** Ein geordnetes Tupel ist ein Mengenelement, etwa für kartesische Produkte. */
data class Tupel(val elemente: List<MathematischesObjekt>) : MathematischesObjekt {
    override fun zuLatex() = elemente.joinToString(prefix = "\\left(", postfix = "\\right)") { it.zuLatex() }
}

data class KartesischesProdukt(val mengen: List<MengenAusdruck>) : MengenAusdruck {
    init { require(mengen.size >= 2) }
    override fun zuLatex() = mengen.joinToString(" \\times ") { it.zuLatex() }
}

/** Trägermenge geordneter Tupel, auch für einstellige Tupel. */
data class Tupelraum(val komponenten: List<MengenAusdruck>) : MengenAusdruck {
    init { require(komponenten.isNotEmpty()) }
    override fun zuLatex() = komponenten.joinToString(" \\times ") { it.zuLatex() }
}

/** Trägermenge endlicher Tupel variabler Länge über einer Elementmenge. */
data class Folgenraum(val elementMenge: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex() = "${elementMenge.zuLatex()}^{<\\omega}"
}

enum class VektorOrientierung { Spalte, Zeile }

/** Raum gleichdimensionierter orientierter Vektoren über einer Zahlgrundmenge. */
data class Vektorraum(
    val orientierung: VektorOrientierung,
    val dimension: Int,
    val skalarMenge: MengenAusdruck,
) : MengenAusdruck {
    init { require(dimension > 0) }
    override fun zuLatex() = when (orientierung) {
        VektorOrientierung.Spalte -> "${skalarMenge.zuLatex()}^{${dimension}\\times 1}"
        VektorOrientierung.Zeile -> "${skalarMenge.zuLatex()}^{1\\times ${dimension}}"
    }
}

/** Raum rechteckiger Matrizen über einer Zahlgrundmenge. */
data class Matrizenraum(
    val zeilen: Int,
    val spalten: Int,
    val skalarMenge: MengenAusdruck,
) : MengenAusdruck {
    init { require(zeilen > 0 && spalten > 0) }
    override fun zuLatex() = "${skalarMenge.zuLatex()}^{${zeilen}\\times ${spalten}}"
}

/** Eine Variable mit ihrer Grundmenge innerhalb einer [DefinierteMenge]. */
data class GebundeneMengenVariable(
    val variable: Variable,
    val grundMenge: MengenAusdruck,
)

/**
 * Symbolische Menge in Mengenschreibweise. Die Bedingung bleibt absichtlich
 * unverändert im CAS und wird erst von Darstellern gegebenenfalls angenähert.
 */
data class DefinierteMenge(
    val variablen: List<GebundeneMengenVariable>,
    val bedingung: Aussage,
) : MengenAusdruck {
    init {
        require(variablen.isNotEmpty()) { "Eine definierte Menge benötigt mindestens eine Variable." }
        require(variablen.map { it.variable.name }.distinct().size == variablen.size) {
            "Die Variablen einer definierten Menge müssen eindeutige Namen haben."
        }
    }

    override fun zuLatex(): String {
        val links = if (variablen.size == 1) variablen.single().variable.zuLatex()
        else variablen.joinToString(prefix = "\\left(", postfix = "\\right)") { it.variable.zuLatex() }
        val gleicheGrundmenge = variablen.map { it.grundMenge }.distinct().singleOrNull()
        val grundmenge = when {
            gleicheGrundmenge != null && variablen.size == 1 -> gleicheGrundmenge.zuLatex()
            gleicheGrundmenge != null -> "${gleicheGrundmenge.zuLatex()}^${variablen.size}"
            else -> KartesischesProdukt(variablen.map { it.grundMenge }).zuLatex()
        }
        return "\\left\\{$links\\in$grundmenge\\mid ${bedingung.zuLatex()}\\right\\}"
    }
}

/** Symbolischer Filter einer Menge durch eine einstellige aussagewertige Methode. */
data class GefilterteMenge(
    val menge: MengenAusdruck,
    val methode: Methode,
) : MengenAusdruck {
    init {
        require(methode.parameter.size == 1) { "Eine Filtermethode benötigt genau einen Elementparameter." }
        require(methode.ausgabeNamen.size == 1 && methode.vorschrift is Aussage) {
            "Eine Filtermethode muss genau eine Aussage ausgeben."
        }
    }

    override fun zuLatex(): String {
        val parameter = methode.parameter.single()
        val bedingung = methode.vorschrift as Aussage
        return "\\left\\{${parameter.zuLatex()}\\in${menge.zuLatex()}\\mid ${bedingung.zuLatex()}\\right\\}"
    }
}

/** Filtert endliche Mengen exakt und bewahrt andernfalls die symbolische Filterdefinition. */
fun filtereMenge(
    menge: MengenAusdruck,
    methode: Methode,
    kontext: RechenKontext = RechenKontext(),
): MengenAusdruck {
    require(methode.parameter.size == 1) { "Eine Filtermethode benötigt genau einen Elementparameter." }
    val (_, ausgabe) = methode.einzigeAusgabe()
    require(ausgabe is Aussage) { "Eine Filtermethode muss eine Aussage ausgeben." }
    if (menge == LeereMenge) return LeereMenge
    if (menge is EndlicheMenge) {
        val behalten = linkedSetOf<MathematischesObjekt>()
        for (element in menge.elemente.sortedBy(::strukturellerSchlüssel)) {
            val bedingung = methode.wendeAn(listOf(element)) as Aussage
            when (bedingung.entscheide(kontext).wahrheitswert) {
                Wahrheitswert.Wahr -> behalten += element
                Wahrheitswert.Lüge -> Unit
                null -> return GefilterteMenge(menge, methode)
            }
        }
        return if (behalten.isEmpty()) LeereMenge else EndlicheMenge(behalten)
    }
    return GefilterteMenge(menge, methode)
}

sealed interface Mächtigkeit : MathematischesObjekt
data class EndlicheMächtigkeit(val wert: RationaleZahl) : Mächtigkeit { override fun zuLatex() = "|M| = ${wert.zuLatex()}" }
data object AbzählbarUnendlich : Mächtigkeit { override fun zuLatex() = "|M| = \\aleph_0" }
data object Überabzählbar : Mächtigkeit { override fun zuLatex() = "|M| > \\aleph_0" }

fun mächtigkeit(menge: MengenAusdruck): Mächtigkeit = when (menge) {
    is EndlicheMenge -> EndlicheMächtigkeit(RationaleZahl.von(
        (eindeutigeEndlicheElemente(menge) ?: error("Die Mächtigkeit dieser Menge ist noch nicht entscheidbar.")).size.toLong(),
    ))
    NatürlicheZahlen, GanzeZahlen, RationaleZahlen -> AbzählbarUnendlich
    ReelleZahlen, KomplexeZahlen -> Überabzählbar
    else -> error("Die Mächtigkeit dieser Menge ist noch nicht entscheidbar.")
}

/** Kanonische Mengenvereinigung mit Abflachung und konkreter Auswertung endlicher Mengen. */
fun vereinige(mengen: Iterable<MengenAusdruck>): MengenAusdruck {
    val flach = mengen.flatMap { if (it is Vereinigung) it.mengen else listOf(it) }.filterNot { it == LeereMenge }
    if (flach.isEmpty()) return LeereMenge
    if (flach.all { it is EndlicheMenge }) return EndlicheMenge(flach.filterIsInstance<EndlicheMenge>().flatMap { it.elemente }.toSet())
    val eindeutig = flach.distinct().sortedBy(::strukturellerSchlüssel)
    return if (eindeutig.size == 1) eindeutig.single() else Vereinigung(eindeutig)
}

/** Kanonischer Schnitt. Der leere Schnitt ist nur mit expliziter Grundmenge definiert. */
fun schneide(mengen: Iterable<MengenAusdruck>, grundMenge: MengenAusdruck? = null): MengenAusdruck {
    val flach = mengen.flatMap { if (it is Schnitt) it.mengen else listOf(it) }
    if (flach.isEmpty()) return grundMenge ?: error("Ein leerer Schnitt benötigt eine Grundmenge.")
    if (flach.any { it == LeereMenge }) return LeereMenge
    if (flach.all { it is EndlicheMenge }) {
        val erste = (flach.first() as EndlicheMenge).elemente
        val entscheidungen = erste.associateWith { element ->
            Konjunktion(flach.drop(1).map { ElementBeziehung(element, it) }).entscheide().wahrheitswert
        }
        if (entscheidungen.values.none { it == null }) {
            return EndlicheMenge(entscheidungen.filterValues { it == Wahrheitswert.Wahr }.keys)
        }
    }
    val eindeutig = flach.distinct().sortedBy(::strukturellerSchlüssel)
    return if (eindeutig.size == 1) eindeutig.single() else Schnitt(eindeutig, grundMenge)
}

fun kartesischesProdukt(mengen: Iterable<MengenAusdruck>): MengenAusdruck {
    val faktoren = mengen.toList()
    require(faktoren.size >= 2) { "Ein kartesisches Produkt benötigt mindestens zwei Mengen." }
    if (faktoren.any { it == LeereMenge }) return LeereMenge
    if (faktoren.all { it is EndlicheMenge }) {
        val tupel = faktoren.filterIsInstance<EndlicheMenge>().fold(listOf(emptyList<MathematischesObjekt>())) { bisher, menge ->
            bisher.flatMap { präfix -> menge.elemente.sortedBy(::strukturellerSchlüssel).map { präfix + it } }
        }
        return EndlicheMenge(tupel.map(::Tupel).toSet())
    }
    return KartesischesProdukt(faktoren)
}

internal fun strukturellerSchlüssel(objekt: MathematischesObjekt): String = "${objekt::class.qualifiedName}:${objekt.zuLatex()}"

data class ElementBeziehung(val element: MathematischesObjekt, val menge: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = when (menge) {
        is EndlicheMenge -> Disjunktion(menge.elemente.map { Gleichheit(element, it) }).entscheide(kontext)
        is KartesischesProdukt -> entscheideTupelMitgliedschaft(element, menge.mengen, kontext)
        is Tupelraum -> entscheideTupelMitgliedschaft(element, menge.komponenten, kontext)
        is Vereinigung -> Disjunktion(menge.mengen.map { ElementBeziehung(element, it) }).entscheide(kontext)
        is Schnitt -> if (menge.mengen.isEmpty()) {
            menge.grundMenge?.let { ElementBeziehung(element, it).entscheide(kontext) }
                ?: AussageErgebnis(null, EntscheidungsStatus.NichtAuswertbar, "Ein leerer Schnitt benötigt eine Grundmenge.")
        } else Konjunktion(menge.mengen.map { ElementBeziehung(element, it) }).entscheide(kontext)
        is MengenDifferenz -> Konjunktion(listOf(ElementBeziehung(element, menge.links), Negation(ElementBeziehung(element, menge.rechts)))).entscheide(kontext)
        is SymmetrischeDifferenz -> Negation(Äquivalenz(ElementBeziehung(element, menge.links), ElementBeziehung(element, menge.rechts))).entscheide(kontext)
        is ReellesIntervall -> {
            val wert = (element as? ZahlAusdruck)?.let { vereinfache(it, kontext) } as? RationaleZahl
            val links = vereinfache(menge.links, kontext) as? RationaleZahl
            val rechts = vereinfache(menge.rechts, kontext) as? RationaleZahl
            if (wert != null && links != null && rechts != null) {
                val linkeBedingung = if (menge.linksOffen) links < wert else links <= wert
                val rechteBedingung = if (menge.rechtsOffen) wert < rechts else wert <= rechts
                val enthalten = linkeBedingung && rechteBedingung
                AussageErgebnis(
                    if (enthalten) Wahrheitswert.Wahr else Wahrheitswert.Lüge,
                    if (enthalten) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt,
                )
            } else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
        LeereMenge -> AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
        RationaleZahlen, ReelleZahlen -> if (element is RationaleZahl) AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen) else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        KomplexeZahlen -> if (
            element is RationaleZahl ||
            element is KomplexeZahl && element.realteil is RationaleZahl && element.imaginärteil is RationaleZahl
        ) AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
        else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        GanzeZahlen -> if (element is RationaleZahl) {
            val wahr = element.nenner == java.math.BigInteger.ONE
            AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
        } else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        NatürlicheZahlen -> if (element is RationaleZahl) {
            val wahr = element.nenner == java.math.BigInteger.ONE && element.zähler.signum() > 0
            AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
        } else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        is OrbitBeschraenktheitsMenge -> when (val entscheidung = entscheideOrbitBeschraenktheit(menge, element)) {
            is OrbitEntscheidung.Enthalten -> AussageErgebnis(
                Wahrheitswert.Wahr,
                EntscheidungsStatus.Bewiesen,
                entscheidung.grund,
            )
            is OrbitEntscheidung.Ausgeschlossen -> AussageErgebnis(
                Wahrheitswert.Lüge,
                EntscheidungsStatus.Widerlegt,
                entscheidung.grund,
            )
            is OrbitEntscheidung.Unbekannt -> AussageErgebnis(
                null,
                EntscheidungsStatus.Unbekannt,
                entscheidung.grund,
            )
        }
        is GefilterteMenge -> {
            val grundErgebnis = ElementBeziehung(element, menge.menge).entscheide(kontext)
            if (grundErgebnis.wahrheitswert == Wahrheitswert.Lüge) grundErgebnis else {
                val bedingung = menge.methode.wendeAn(listOf(element)) as Aussage
                val filterErgebnis = bedingung.entscheide(kontext)
                when {
                    filterErgebnis.wahrheitswert == Wahrheitswert.Lüge -> filterErgebnis
                    grundErgebnis.wahrheitswert == Wahrheitswert.Wahr && filterErgebnis.wahrheitswert == Wahrheitswert.Wahr ->
                        AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
                    else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
                }
            }
        }
        else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
    }
    override fun zuLatex() = "${element.zuLatex()} \\in ${menge.zuLatex()}"
}

data class TeilmengenBeziehung(val links: MengenAusdruck, val rechts: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = prüfeTeilmenge(links, rechts, kontext)
    override fun zuLatex() = "${links.zuLatex()} \\subseteq ${rechts.zuLatex()}"
}

/**
 * Entscheidet eine Teilmengenbeziehung nur dann abschließend, wenn sie aus dem
 * vorhandenen Mengen- und Elementwissen beweisbar oder widerlegbar ist.
 */
fun prüfeTeilmenge(
    teilMenge: MengenAusdruck,
    grundMenge: MengenAusdruck,
    kontext: RechenKontext = RechenKontext(),
): AussageErgebnis = when {
    teilMenge == LeereMenge || teilMenge == grundMenge ->
        AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
    zahlenmengenRang(teilMenge) != null && zahlenmengenRang(grundMenge) != null -> {
        val enthalten = zahlenmengenRang(teilMenge)!! <= zahlenmengenRang(grundMenge)!!
        AussageErgebnis(
            if (enthalten) Wahrheitswert.Wahr else Wahrheitswert.Lüge,
            if (enthalten) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt,
            if (enthalten) "Die kanonische Zahlenmengeninklusion ist erfüllt."
            else "Die kanonische Zahlenmengeninklusion ist widerlegt.",
        )
    }
    teilMenge is EndlicheMenge -> {
        val elementErgebnisse = teilMenge.elemente.map { ElementBeziehung(it, grundMenge).entscheide(kontext) }
        when {
            elementErgebnisse.any { it.wahrheitswert == Wahrheitswert.Lüge } ->
                AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
            elementErgebnisse.all { it.wahrheitswert == Wahrheitswert.Wahr } ->
                AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
            else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
    }
    else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
}

private fun entscheideTupelMitgliedschaft(
    element: MathematischesObjekt,
    komponenten: List<MengenAusdruck>,
    kontext: RechenKontext,
): AussageErgebnis {
    val tupel = element as? Tupel ?: return when (element) {
        is AllgemeinerParameter, is TypisiertesElement ->
            AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Der allgemeine Parameter ist noch nicht als Tupel belegt.")
        else -> AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, "Ein kartesischer Raum enthält nur Tupel.")
    }
    if (tupel.elemente.size != komponenten.size) {
        return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, "Die Tupeldimension stimmt nicht mit dem Raum überein.")
    }
    return Konjunktion(
        tupel.elemente.zip(komponenten).map { (wert, menge) -> ElementBeziehung(wert, menge) },
    ).entscheide(kontext)
}

private fun zahlenmengenRang(menge: MengenAusdruck): Int? = when (menge) {
    NatürlicheZahlen -> 0
    GanzeZahlen -> 1
    RationaleZahlen -> 2
    ReelleZahlen -> 3
    KomplexeZahlen -> 4
    else -> null
}

data class EchteTeilmengeBeziehung(val links: MengenAusdruck, val rechts: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        return Konjunktion(listOf(TeilmengenBeziehung(links, rechts), Negation(TeilmengenBeziehung(rechts, links)))).entscheide(kontext)
    }
    override fun zuLatex() = "${links.zuLatex()} \\subset ${rechts.zuLatex()}"
}

data class ObermengenBeziehung(val links: MengenAusdruck, val rechts: MengenAusdruck, val echt: Boolean = false) : Aussage {
    private val umgedreht: Aussage get() = if (echt) EchteTeilmengeBeziehung(rechts, links) else TeilmengenBeziehung(rechts, links)
    override fun entscheide(kontext: RechenKontext) = umgedreht.entscheide(kontext)
    override fun zuLatex() = "${links.zuLatex()} ${if (echt) "\\supset" else "\\supseteq"} ${rechts.zuLatex()}"
}

data class Disjunktheit(val links: MengenAusdruck, val rechts: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        if (links is EndlicheMenge && rechts is EndlicheMenge) {
            return Konjunktion(links.elemente.map { Negation(ElementBeziehung(it, rechts)) }).entscheide(kontext)
        }
        if (links == LeereMenge || rechts == LeereMenge) return AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
        return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
    }
    override fun zuLatex() = "${links.zuLatex()} \\cap ${rechts.zuLatex()} = \\varnothing"
}

val NatürlicheZahlen = BenannteMenge("Natürliche Zahlen", "\\mathbb{N}")
val GanzeZahlen = BenannteMenge("Ganze Zahlen", "\\mathbb{Z}")
val RationaleZahlen = BenannteMenge("Rationale Zahlen", "\\mathbb{Q}")
val ReelleZahlen = BenannteMenge("Reelle Zahlen", "\\mathbb{R}")
val KomplexeZahlen = BenannteMenge("Komplexe Zahlen", "\\mathbb{C}")

/** Representatives can be enumerated/countable only when their distinctness is proved. */
internal fun eindeutigeEndlicheElemente(menge: EndlicheMenge): List<MathematischesObjekt>? {
    val eindeutig = mutableListOf<MathematischesObjekt>()
    for (element in menge.elemente.sortedBy(::strukturellerSchlüssel)) {
        val vergleiche = eindeutig.map { Gleichheit(element, it).entscheide().wahrheitswert }
        if (Wahrheitswert.Wahr in vergleiche) continue
        if (null in vergleiche) return null
        eindeutig += element
    }
    return eindeutig
}
