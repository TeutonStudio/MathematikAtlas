package de.TeutonStudio.MathematikRechenSystem.kern

data class EndlicheMenge(val elemente: Set<MathematischesObjekt>) : MengenAusdruck {
    override fun zuLatex() = elemente.sortedBy(::strukturellerSchlüssel).joinToString(prefix = "\\{", postfix = "\\}") { it.zuLatex() }
}

data object LeereMenge : MengenAusdruck { override fun zuLatex() = "\\varnothing" }

data class BenannteMenge(val name: String, val latex: String = name) : MengenAusdruck { override fun zuLatex() = latex }

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
    links is EndlicheMenge && rechts is EndlicheMenge -> EndlicheMenge(links.elemente - rechts.elemente)
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

sealed interface Mächtigkeit : MathematischesObjekt
data class EndlicheMächtigkeit(val wert: RationaleZahl) : Mächtigkeit { override fun zuLatex() = "|M| = ${wert.zuLatex()}" }
data object AbzählbarUnendlich : Mächtigkeit { override fun zuLatex() = "|M| = \\aleph_0" }
data object Überabzählbar : Mächtigkeit { override fun zuLatex() = "|M| > \\aleph_0" }

fun mächtigkeit(menge: MengenAusdruck): Mächtigkeit = when (menge) {
    is EndlicheMenge -> EndlicheMächtigkeit(RationaleZahl.von(menge.elemente.size.toLong()))
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
        val elemente = flach.filterIsInstance<EndlicheMenge>().map { it.elemente }.reduce { links, rechts -> links.intersect(rechts) }
        return EndlicheMenge(elemente)
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
        is EndlicheMenge -> if (element in menge.elemente) AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen) else AussageErgebnis(Wahrheitswert.Falsch, EntscheidungsStatus.Widerlegt)
        LeereMenge -> AussageErgebnis(Wahrheitswert.Falsch, EntscheidungsStatus.Widerlegt)
        RationaleZahlen, ReelleZahlen -> if (element is RationaleZahl) AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen) else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        GanzeZahlen -> if (element is RationaleZahl) {
            val wahr = element.nenner == java.math.BigInteger.ONE
            AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Falsch, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
        } else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        NatürlicheZahlen -> if (element is RationaleZahl) {
            val wahr = element.nenner == java.math.BigInteger.ONE && element.zähler.signum() >= 0
            AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Falsch, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
        } else AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
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
    teilMenge is EndlicheMenge && grundMenge is EndlicheMenge -> {
        val wahr = grundMenge.elemente.containsAll(teilMenge.elemente)
        AussageErgebnis(
            if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Falsch,
            if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt,
        )
    }
    teilMenge is EndlicheMenge -> {
        val elementErgebnisse = teilMenge.elemente.map { ElementBeziehung(it, grundMenge).entscheide(kontext) }
        when {
            elementErgebnisse.any { it.wahrheitswert == Wahrheitswert.Falsch } ->
                AussageErgebnis(Wahrheitswert.Falsch, EntscheidungsStatus.Widerlegt)
            elementErgebnisse.all { it.wahrheitswert == Wahrheitswert.Wahr } ->
                AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
            else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
        }
    }
    else -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
}

data class EchteTeilmengeBeziehung(val links: MengenAusdruck, val rechts: MengenAusdruck) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        if (links is EndlicheMenge && rechts is EndlicheMenge) {
            val wahr = rechts.elemente.containsAll(links.elemente) && links.elemente != rechts.elemente
            return AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Falsch, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
        }
        return AussageErgebnis(null, EntscheidungsStatus.Unbekannt)
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
            val wahr = links.elemente.intersect(rechts.elemente).isEmpty()
            return AussageErgebnis(if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Falsch, if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt)
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
