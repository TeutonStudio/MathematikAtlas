package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** Skalare Variablenbelegungen für plattformneutrale numerische Auswertungen. */
data class NumerischeUmgebung(
    val skalare: Map<String, Double> = emptyMap(),
) {
    operator fun get(name: String): Double? = skalare[name]
    operator fun get(variable: Variable): Double? = skalare[variable.name]

    fun mit(name: String, wert: Double): NumerischeUmgebung = copy(skalare = skalare + (name to wert))
}

/** Gemeinsame Genauigkeits- und Definitionsschwellen aller numerischen Verbraucher. */
data class NumerischeOptionen(
    val toleranz: Double = 1e-9,
    val nullToleranz: Double = 1e-12,
) {
    init {
        require(toleranz >= 0.0 && toleranz.isFinite()) { "Die numerische Toleranz muss endlich und nichtnegativ sein." }
        require(nullToleranz >= 0.0 && nullToleranz.isFinite()) { "Die Nulltoleranz muss endlich und nichtnegativ sein." }
    }
}

/** Explizites Ergebnis statt stiller NaN-, Infinity- oder null-Sentinels. */
sealed interface NumerischesErgebnis<out T> {
    data class Wert<T>(val wert: T) : NumerischesErgebnis<T>

    sealed interface Fehler : NumerischesErgebnis<Nothing> {
        val beschreibung: String
    }

    data class Undefiniert(override val beschreibung: String) : Fehler
    data class Definitionsbereich(override val beschreibung: String) : Fehler
    data class NichtUnterstützt(override val beschreibung: String) : Fehler
    data class BindungFehlt(val variable: String) : Fehler {
        override val beschreibung: String = "Für die Variable '$variable' fehlt eine numerische Bindung."
    }
}

/**
 * Einziger öffentlicher Einstieg für skalare Terme und numerisch entscheidbare Aussagen.
 * Der Dienst ist Android- und Compose-frei und kann daher von Knoten, Visualisierern und Tests geteilt werden.
 */
object NumerischerAuswerter {
    fun wert(
        ausdruck: ZahlAusdruck,
        umgebung: NumerischeUmgebung = NumerischeUmgebung(),
        optionen: NumerischeOptionen = NumerischeOptionen(),
    ): NumerischesErgebnis<Double> = wertIntern(ausdruck, umgebung, optionen)
        .endlichenWertSicherstellen(ausdruck)

    fun aussage(
        aussage: Aussage,
        umgebung: NumerischeUmgebung = NumerischeUmgebung(),
        optionen: NumerischeOptionen = NumerischeOptionen(),
    ): NumerischesErgebnis<Boolean> = aussageIntern(aussage, umgebung, optionen)
}

private fun wertIntern(
    ausdruck: ZahlAusdruck,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
): NumerischesErgebnis<Double> = when (ausdruck) {
    is RationaleZahl -> NumerischesErgebnis.Wert(ausdruck.zuDezimal(34).toDouble())
    is Variable -> umgebung[ausdruck]?.let { NumerischesErgebnis.Wert(it) }
        ?: NumerischesErgebnis.BindungFehlt(ausdruck.name)
    is MathematischeKonstante -> when (ausdruck) {
        Pi -> NumerischesErgebnis.Wert(Math.PI)
        EulerscheZahl -> NumerischesErgebnis.Wert(Math.E)
        else -> NumerischesErgebnis.NichtUnterstützt("Die Konstante '${ausdruck.name}' besitzt keinen numerischen Wert.")
    }
    is Addition -> werte(ausdruck.summanden, umgebung, optionen).map { it.sum() }
    is Multiplikation -> werte(ausdruck.faktoren, umgebung, optionen).map { faktoren ->
        faktoren.fold(1.0) { produkt, faktor -> produkt * faktor }
    }
    is Maximum -> werte(ausdruck.operanden, umgebung, optionen).map { it.reduce(::max) }
    is Minimum -> werte(ausdruck.operanden, umgebung, optionen).map { it.reduce(::min) }
    is Division -> wertIntern(ausdruck.dividend, umgebung, optionen).flatMap { dividend ->
        wertIntern(ausdruck.divisor, umgebung, optionen).flatMap { divisor ->
            if (abs(divisor) <= optionen.nullToleranz) {
                NumerischesErgebnis.Undefiniert("Division durch null ist nicht definiert.")
            } else NumerischesErgebnis.Wert(dividend / divisor)
        }
    }
    is Potenz -> wertIntern(ausdruck.basis, umgebung, optionen).flatMap { basis ->
        wertIntern(ausdruck.exponent, umgebung, optionen).flatMap { exponent ->
            when {
                abs(basis) <= optionen.nullToleranz && exponent < 0.0 ->
                    NumerischesErgebnis.Undefiniert("Null kann nicht mit einem negativen Exponenten potenziert werden.")
                basis < 0.0 && !istNaheGanzzahl(exponent, optionen.toleranz) ->
                    NumerischesErgebnis.Definitionsbereich("Eine negative Basis mit nichtganzzahligem Exponenten ist im Reellen nicht definiert.")
                else -> NumerischesErgebnis.Wert(basis.pow(exponent))
            }
        }
    }
    is Betrag -> wertIntern(ausdruck.argument, umgebung, optionen).map(::abs)
    is Sinus -> wertIntern(ausdruck.argument, umgebung, optionen).map(::sin)
    is Cosinus -> wertIntern(ausdruck.argument, umgebung, optionen).map(::cos)
    is Exponentialfunktion -> wertIntern(ausdruck.argument, umgebung, optionen).map(::exp)
    is NatürlicherLogarithmus -> wertIntern(ausdruck.argument, umgebung, optionen).flatMap { argument ->
        if (argument <= 0.0) NumerischesErgebnis.Definitionsbereich("Der natürliche Logarithmus ist im Reellen nur für positive Argumente definiert.")
        else NumerischesErgebnis.Wert(ln(argument))
    }
    is Logarithmus -> wertIntern(ausdruck.basis, umgebung, optionen).flatMap { basis ->
        wertIntern(ausdruck.argument, umgebung, optionen).flatMap { argument ->
            when {
                basis <= 0.0 -> NumerischesErgebnis.Definitionsbereich("Die Logarithmusbasis muss positiv sein.")
                abs(basis - 1.0) <= optionen.toleranz -> NumerischesErgebnis.Definitionsbereich("Die Logarithmusbasis darf nicht eins sein.")
                argument <= 0.0 -> NumerischesErgebnis.Definitionsbereich("Das Logarithmusargument muss positiv sein.")
                else -> NumerischesErgebnis.Wert(ln(argument) / ln(basis))
            }
        }
    }
    is Wurzel -> wertIntern(ausdruck.argument, umgebung, optionen).flatMap { argument ->
        when {
            argument < -optionen.toleranz -> NumerischesErgebnis.Definitionsbereich("Die Quadratwurzel einer negativen reellen Zahl ist nicht reell definiert.")
            else -> NumerischesErgebnis.Wert(sqrt(argument.coerceAtLeast(0.0)))
        }
    }
    is ZahlFallAusdruck -> aussageIntern(ausdruck.aussage, umgebung, optionen).flatMap { bedingung ->
        wertIntern(if (bedingung) ausdruck.wahr else ausdruck.lüge, umgebung, optionen)
    }
    else -> NumerischesErgebnis.NichtUnterstützt(
        "Der Zahlterm ${ausdruck.zuLatex()} wird numerisch noch nicht unterstützt.",
    )
}

private fun aussageIntern(
    aussage: Aussage,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
): NumerischesErgebnis<Boolean> = when (aussage) {
    is WahrheitsKonstante -> NumerischesErgebnis.Wert(aussage.wert)
    is Gleichheit -> numerischerVergleich(aussage.links, aussage.rechts, umgebung, optionen) { links, rechts ->
        abs(links - rechts) <= optionen.toleranz
    }
    is Ungleichheit -> numerischerVergleich(aussage.links, aussage.rechts, umgebung, optionen) { links, rechts ->
        abs(links - rechts) > optionen.toleranz
    }
    is Vergleich -> wertIntern(aussage.links, umgebung, optionen).flatMap { links ->
        wertIntern(aussage.rechts, umgebung, optionen).map { rechts ->
            when (aussage.art) {
                VergleichsArt.Kleiner -> links < rechts - optionen.toleranz
                VergleichsArt.KleinerGleich -> links <= rechts + optionen.toleranz
                VergleichsArt.Größer -> links > rechts + optionen.toleranz
                VergleichsArt.GrößerGleich -> links >= rechts - optionen.toleranz
            }
        }
    }
    is Negation -> aussageIntern(aussage.aussage, umgebung, optionen).map { !it }
    is Konjunktion -> logischeListe(aussage.aussagen, umgebung, optionen, und = true)
    is Disjunktion -> logischeListe(aussage.aussagen, umgebung, optionen, und = false)
    is Implikation -> aussageIntern(aussage.voraussetzung, umgebung, optionen).flatMap { voraussetzung ->
        if (!voraussetzung) NumerischesErgebnis.Wert(true)
        else aussageIntern(aussage.folgerung, umgebung, optionen)
    }
    is Äquivalenz -> aussageIntern(aussage.links, umgebung, optionen).flatMap { links ->
        aussageIntern(aussage.rechts, umgebung, optionen).map { rechts -> links == rechts }
    }
    is Adjunktion -> aussageIntern(aussage.links, umgebung, optionen).flatMap { links ->
        aussageIntern(aussage.rechts, umgebung, optionen).map { rechts -> links != rechts }
    }
    is ElementBeziehung -> elementBeziehung(aussage, umgebung, optionen)
    is AussagenFallAusdruck -> aussageIntern(aussage.aussage, umgebung, optionen).flatMap { bedingung ->
        aussageIntern(if (bedingung) aussage.wahr else aussage.lüge, umgebung, optionen)
    }
    else -> NumerischesErgebnis.NichtUnterstützt(
        "Die Aussage ${aussage.zuLatex()} wird numerisch noch nicht unterstützt.",
    )
}

private fun elementBeziehung(
    beziehung: ElementBeziehung,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
): NumerischesErgebnis<Boolean> {
    val element = beziehung.element as? ZahlAusdruck
        ?: return NumerischesErgebnis.NichtUnterstützt("Nur skalare numerische Elementbeziehungen werden unterstützt.")
    return wertIntern(element, umgebung, optionen).flatMap { wert ->
        enthältNumerisch(beziehung.menge, wert, umgebung, optionen)
    }
}

private fun enthältNumerisch(
    menge: MengenAusdruck,
    wert: Double,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
): NumerischesErgebnis<Boolean> = when (menge) {
    LeereMenge -> NumerischesErgebnis.Wert(false)
    ReelleZahlen, RationaleZahlen, KomplexeZahlen -> NumerischesErgebnis.Wert(wert.isFinite())
    GanzeZahlen -> NumerischesErgebnis.Wert(istNaheGanzzahl(wert, optionen.toleranz))
    NatürlicheZahlen -> NumerischesErgebnis.Wert(
        wert >= -optionen.toleranz && istNaheGanzzahl(wert, optionen.toleranz),
    )
    is ReellesIntervall -> wertIntern(menge.links, umgebung, optionen).flatMap { links ->
        wertIntern(menge.rechts, umgebung, optionen).map { rechts ->
            val linksErfüllt = if (menge.linksOffen) wert > links + optionen.toleranz else wert >= links - optionen.toleranz
            val rechtsErfüllt = if (menge.rechtsOffen) wert < rechts - optionen.toleranz else wert <= rechts + optionen.toleranz
            linksErfüllt && rechtsErfüllt
        }
    }
    is EndlicheMenge -> {
        val elemente = menge.elemente.map { element ->
            val zahl = element as? ZahlAusdruck
                ?: return NumerischesErgebnis.NichtUnterstützt("Die endliche Menge enthält nichtskalare Elemente.")
            when (val ergebnis = wertIntern(zahl, umgebung, optionen)) {
                is NumerischesErgebnis.Wert -> ergebnis.wert
                is NumerischesErgebnis.Fehler -> return ergebnis
            }
        }
        NumerischesErgebnis.Wert(elemente.any { abs(it - wert) <= optionen.toleranz })
    }
    is Vereinigung -> enthältMindestensEine(menge.mengen, wert, umgebung, optionen)
    is Schnitt -> enthältAlle(menge.mengen, wert, umgebung, optionen)
    is MengenDifferenz -> enthältNumerisch(menge.links, wert, umgebung, optionen).flatMap { links ->
        if (!links) NumerischesErgebnis.Wert(false)
        else enthältNumerisch(menge.rechts, wert, umgebung, optionen).map { !it }
    }
    else -> NumerischesErgebnis.NichtUnterstützt(
        "Die Menge ${menge.zuLatex()} wird für numerische Elementbeziehungen noch nicht unterstützt.",
    )
}

private fun enthältMindestensEine(
    mengen: List<MengenAusdruck>,
    wert: Double,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
): NumerischesErgebnis<Boolean> {
    for (menge in mengen) {
        when (val ergebnis = enthältNumerisch(menge, wert, umgebung, optionen)) {
            is NumerischesErgebnis.Wert -> if (ergebnis.wert) return ergebnis
            is NumerischesErgebnis.Fehler -> return ergebnis
        }
    }
    return NumerischesErgebnis.Wert(false)
}

private fun enthältAlle(
    mengen: List<MengenAusdruck>,
    wert: Double,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
): NumerischesErgebnis<Boolean> {
    for (menge in mengen) {
        when (val ergebnis = enthältNumerisch(menge, wert, umgebung, optionen)) {
            is NumerischesErgebnis.Wert -> if (!ergebnis.wert) return ergebnis
            is NumerischesErgebnis.Fehler -> return ergebnis
        }
    }
    return NumerischesErgebnis.Wert(true)
}

private fun numerischerVergleich(
    links: MathematischesObjekt,
    rechts: MathematischesObjekt,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
    vergleiche: (Double, Double) -> Boolean,
): NumerischesErgebnis<Boolean> {
    if (links !is ZahlAusdruck || rechts !is ZahlAusdruck) {
        return if (links == rechts) NumerischesErgebnis.Wert(vergleiche(0.0, 0.0))
        else NumerischesErgebnis.NichtUnterstützt("Nur Zahlgleichungen werden numerisch ausgewertet.")
    }
    return wertIntern(links, umgebung, optionen).flatMap { linkerWert ->
        wertIntern(rechts, umgebung, optionen).map { rechterWert -> vergleiche(linkerWert, rechterWert) }
    }
}

private fun logischeListe(
    aussagen: List<Aussage>,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
    und: Boolean,
): NumerischesErgebnis<Boolean> {
    for (aussage in aussagen) {
        when (val wert = aussageIntern(aussage, umgebung, optionen)) {
            is NumerischesErgebnis.Wert -> {
                if (und && !wert.wert) return NumerischesErgebnis.Wert(false)
                if (!und && wert.wert) return NumerischesErgebnis.Wert(true)
            }
            is NumerischesErgebnis.Fehler -> return wert
        }
    }
    return NumerischesErgebnis.Wert(und)
}

private fun werte(
    ausdrücke: List<ZahlAusdruck>,
    umgebung: NumerischeUmgebung,
    optionen: NumerischeOptionen,
): NumerischesErgebnis<List<Double>> {
    val ergebnisse = ArrayList<Double>(ausdrücke.size)
    for (ausdruck in ausdrücke) {
        when (val ergebnis = wertIntern(ausdruck, umgebung, optionen)) {
            is NumerischesErgebnis.Wert -> ergebnisse += ergebnis.wert
            is NumerischesErgebnis.Fehler -> return ergebnis
        }
    }
    return NumerischesErgebnis.Wert(ergebnisse)
}

private fun istNaheGanzzahl(wert: Double, toleranz: Double): Boolean =
    wert.isFinite() && abs(wert - round(wert)) <= toleranz

private fun NumerischesErgebnis<Double>.endlichenWertSicherstellen(
    ausdruck: ZahlAusdruck,
): NumerischesErgebnis<Double> = when (this) {
    is NumerischesErgebnis.Wert -> if (wert.isFinite()) this
        else NumerischesErgebnis.Undefiniert("Die Auswertung von ${ausdruck.zuLatex()} ergibt keinen endlichen reellen Wert.")
    is NumerischesErgebnis.Fehler -> this
}

private inline fun <T, R> NumerischesErgebnis<T>.map(
    transformiere: (T) -> R,
): NumerischesErgebnis<R> = when (this) {
    is NumerischesErgebnis.Wert -> NumerischesErgebnis.Wert(transformiere(wert))
    is NumerischesErgebnis.Fehler -> this
}

private inline fun <T, R> NumerischesErgebnis<T>.flatMap(
    transformiere: (T) -> NumerischesErgebnis<R>,
): NumerischesErgebnis<R> = when (this) {
    is NumerischesErgebnis.Wert -> transformiere(wert)
    is NumerischesErgebnis.Fehler -> this
}
