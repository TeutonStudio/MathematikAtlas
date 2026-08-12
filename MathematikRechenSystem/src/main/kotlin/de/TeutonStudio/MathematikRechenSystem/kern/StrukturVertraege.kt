package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Nachweisbarer Strukturvertrag eines homogenen numerischen Tupels.
 *
 * Der Vertrag ist eine abgeleitete Ansicht. Das zugrunde liegende Objekt bleibt
 * ein [Tupel] und wird weder in einen Vektor noch in einen Tensor umgeschrieben.
 */
data class KartesischerTupelVertrag(
    val laenge: Int,
    val zahlBereich: MengenAusdruck,
) {
    init { require(laenge > 0) { "Ein kartesisches Tupel benötigt mindestens eine Komponente." } }
}

/** Gemeinsame orientierungslose Sicht auf eine geordnete numerische Komponentenfolge. */
data class NumerischeKomponentenAnsicht(
    val komponenten: List<ZahlAusdruck>,
    val zahlBereich: MengenAusdruck,
    val orientierung: VektorOrientierung? = null,
    val quelle: MathematischesObjekt,
) {
    init { require(komponenten.isNotEmpty()) { "Eine Komponentenansicht darf nicht leer sein." } }
    val laenge: Int get() = komponenten.size
    fun komponente(index: Int): ZahlAusdruck = komponenten.getOrElse(index) {
        throw IndexOutOfBoundsException("Komponentenindex $index liegt außerhalb der Länge $laenge.")
    }
}

sealed interface StrukturPruefung<out T> {
    data class Gueltig<T>(val wert: T) : StrukturPruefung<T>
    data class Bedingt<T>(val wert: T?, val bedingungen: List<String>) : StrukturPruefung<T>
    data class Ungueltig(val grund: String) : StrukturPruefung<Nothing>
    data class Unentscheidbar(val grund: String) : StrukturPruefung<Nothing>
}

/** Gemeinsamer Laufzeitvertrag für echte Vektor-, Matrix- und Tensorwerte. */
interface Tensorartig : MathematischesObjekt {
    val tensorForm: List<Int>
    val tensorStufe: Int get() = tensorForm.size
    val tensorZahlBereich: MengenAusdruck
    fun tensorKomponente(indizes: List<Int>): ZahlAusdruck
}

/**
 * Explizite tensorielle Ansicht, auch für Zahlen und kartesische Tupel.
 *
 * Das leere Tupel `()` besitzt die leere Form und ist damit ein Tensor der
 * Stufe 0. Es besitzt keine Komponenten; der Zahlbereich ist für diesen
 * Grenzfall lediglich neutrale Metadaten und wird kanonisch als C geführt.
 */
data class TensorielleAnsicht(
    val form: List<Int>,
    val zahlBereich: MengenAusdruck,
    val komponenten: List<ZahlAusdruck>,
    val quelle: MathematischesObjekt,
) {
    init {
        if (form.isEmpty()) {
            require(komponenten.isEmpty()) { "Ein Tensor der Stufe 0 besitzt keine Komponenten." }
        } else {
            require(form.all { it > 0 }) { "Tensorformen müssen positive Achsenlängen besitzen." }
            require(komponenten.size == form.sicheresProdukt()) {
                "Die Komponentenanzahl muss dem Produkt der Tensorform entsprechen."
            }
        }
    }
    val stufe: Int get() = form.size
    fun komponente(indizes: List<Int>): ZahlAusdruck {
        require(form.isNotEmpty()) { "Ein Tensor der Stufe 0 besitzt keine Komponente." }
        return komponenten[tensorLinearerIndex(indizes, form)]
    }
}

fun Tupel.kartesischerTupelVertrag(
    werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
): StrukturPruefung<KartesischerTupelVertrag> {
    if (elemente.isEmpty()) return StrukturPruefung.Ungueltig("Ein leeres Tupel besitzt keinen kartesischen Zahlbereich.")
    val zahlen = elemente.mapIndexed { index, element ->
        element as? ZahlAusdruck ?: return StrukturPruefung.Ungueltig(
            "Die ${index + 1}. Tupelkomponente ist keine Zahl; das Tupel ist daher nicht kartesisch.",
        )
    }
    val bereiche = runCatching { zahlen.map { inferiereZahlenWertevorrat(it, werteVorraete) } }
        .getOrElse { return StrukturPruefung.Unentscheidbar(it.message ?: "Der Zahlbereich ist nicht bestimmbar.") }
    val gemeinsam = runCatching { maximaleZahlenGrundmenge(bereiche) }
        .getOrElse { return StrukturPruefung.Ungueltig(it.message ?: "Die Zahlbereiche sind nicht kompatibel.") }
    return StrukturPruefung.Gueltig(KartesischerTupelVertrag(zahlen.size, gemeinsam))
}

fun MathematischesObjekt.numerischeKomponentenAnsicht(
    zahlAlsSingleton: Boolean = false,
    werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
): StrukturPruefung<NumerischeKomponentenAnsicht> = when (this) {
    is ZeilenVektor -> StrukturPruefung.Gueltig(
        NumerischeKomponentenAnsicht(werte, gemeinsamerZahlBereich(werte, werteVorraete), VektorOrientierung.Zeile, this),
    )
    is SpaltenVektor -> StrukturPruefung.Gueltig(
        NumerischeKomponentenAnsicht(werte, gemeinsamerZahlBereich(werte, werteVorraete), VektorOrientierung.Spalte, this),
    )
    is Tupel -> when (val vertrag = kartesischerTupelVertrag(werteVorraete)) {
        is StrukturPruefung.Gueltig -> StrukturPruefung.Gueltig(
            NumerischeKomponentenAnsicht(elemente.map { it as ZahlAusdruck }, vertrag.wert.zahlBereich, null, this),
        )
        is StrukturPruefung.Bedingt -> StrukturPruefung.Bedingt(null, vertrag.bedingungen)
        is StrukturPruefung.Ungueltig -> vertrag
        is StrukturPruefung.Unentscheidbar -> vertrag
    }
    is ZahlAusdruck -> if (zahlAlsSingleton) StrukturPruefung.Gueltig(
        NumerischeKomponentenAnsicht(listOf(this), inferiereZahlenWertevorrat(this, werteVorraete), null, this),
    ) else StrukturPruefung.Ungueltig("Eine Zahl ist nur in ausdrücklich tensoriellem Kontext eine Singleton-Komponentenfolge.")
    else -> StrukturPruefung.Ungueltig("${this::class.simpleName} ist keine numerische Komponentenfolge.")
}

fun MathematischesObjekt.tensorielleAnsicht(
    werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
): StrukturPruefung<TensorielleAnsicht> = when (this) {
    is Tensorartig -> StrukturPruefung.Gueltig(
        TensorielleAnsicht(tensorForm, tensorZahlBereich, tensorForm.indizesFolge().map(::tensorKomponente), this),
    )
    is Tupel -> if (elemente.isEmpty()) {
        StrukturPruefung.Gueltig(
            TensorielleAnsicht(emptyList(), KomplexeZahlen, emptyList(), this),
        )
    } else when (val komponenten = numerischeKomponentenAnsicht(werteVorraete = werteVorraete)) {
        is StrukturPruefung.Gueltig -> StrukturPruefung.Gueltig(
            TensorielleAnsicht(listOf(komponenten.wert.laenge), komponenten.wert.zahlBereich, komponenten.wert.komponenten, this),
        )
        is StrukturPruefung.Bedingt -> StrukturPruefung.Bedingt(null, komponenten.bedingungen)
        is StrukturPruefung.Ungueltig -> komponenten
        is StrukturPruefung.Unentscheidbar -> komponenten
    }
    is ZahlAusdruck -> StrukturPruefung.Gueltig(
        TensorielleAnsicht(listOf(1), inferiereZahlenWertevorrat(this, werteVorraete), listOf(this), this),
    )
    else -> StrukturPruefung.Ungueltig("${this::class.simpleName} besitzt keine tensorielle Ansicht.")
}

fun pruefeGleicheLaenge(
    links: NumerischeKomponentenAnsicht,
    rechts: NumerischeKomponentenAnsicht,
): StrukturPruefung<Int> = if (links.laenge == rechts.laenge) {
    StrukturPruefung.Gueltig(links.laenge)
} else {
    StrukturPruefung.Ungueltig("Die Komponentenfolgen besitzen unterschiedliche Längen: ${links.laenge} und ${rechts.laenge}.")
}

private fun gemeinsamerZahlBereich(
    komponenten: List<ZahlAusdruck>,
    werteVorraete: Map<String, MengenAusdruck>,
): MengenAusdruck = maximaleZahlenGrundmenge(komponenten.map { inferiereZahlenWertevorrat(it, werteVorraete) })

internal fun List<Int>.sicheresProdukt(): Int = fold(1) { produkt, faktor -> Math.multiplyExact(produkt, faktor) }

internal fun tensorLinearerIndex(indizes: List<Int>, form: List<Int>): Int {
    require(indizes.size == form.size) { "Die Indexanzahl muss der Tensorstufe entsprechen." }
    var linear = 0
    indizes.zip(form).forEach { (index, dimension) ->
        require(index in 0 until dimension) { "Tensorindex $index liegt außerhalb der Dimension $dimension." }
        linear = Math.addExact(Math.multiplyExact(linear, dimension), index)
    }
    return linear
}

internal fun List<Int>.indizesFolge(): List<List<Int>> = List(sicheresProdukt()) { linear ->
    var rest = linear
    MutableList(size) { 0 }.also { indizes ->
        for (achse in indices.reversed()) {
            indizes[achse] = rest % this[achse]
            rest /= this[achse]
        }
    }
}
