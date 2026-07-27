package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.*

data class VisualisierungsPunkt(val x: Double, val y: Double, val z: Double? = null, val farbwert: Double? = null)

/** Ergebnis einer explizit als Approximation behandelten Mengendarstellung. */
sealed interface VisualisierungsErgebnis {
    data class Erfolgreich(val punkte: List<VisualisierungsPunkt>, val istApproximation: Boolean = true, val hinweise: List<String> = emptyList()) : VisualisierungsErgebnis
    data class Teilweise(val punkte: List<VisualisierungsPunkt>, val hinweise: List<String>) : VisualisierungsErgebnis
    data class NichtDarstellbar(val grund: String) : VisualisierungsErgebnis
}

/**
 * Plattformneutrales Sampling. Es baut keine UI- oder Meshobjekte auf, damit
 * der Aufrufer die Berechnung auf einem Hintergrunddispatcher ausführen kann.
 */
object VisualisierungsSampler {
    fun sample(menge: MengenAusdruck, konfiguration: VisualisierungsKonfiguration): VisualisierungsErgebnis = when (menge) {
        is EndlicheMenge -> sampleEndlich(menge, konfiguration)
        is DefinierteMenge -> sampleDefiniert(menge, konfiguration)
        is Abbild -> sampleAbbild(menge, konfiguration)
        else -> VisualisierungsErgebnis.NichtDarstellbar("Diese Mengenform kann noch nicht numerisch dargestellt werden.")
    }

    private fun sampleEndlich(menge: EndlicheMenge, konfiguration: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val dimension = if (konfiguration.dimension == RaumDimension.R3) 3 else 2
        val punkte = mutableListOf<VisualisierungsPunkt>()
        var übersprungen = 0
        menge.elemente.forEach { element ->
            val tupel = element as? Tupel
            val werte = tupel?.elemente?.map { numerisch(it as? ZahlAusdruck, emptyMap()) }
            if (werte == null || werte.size != dimension || werte.any { it == null }) übersprungen++ else {
                punkte += VisualisierungsPunkt(werte[0]!!, werte[1]!!, werte.getOrNull(2), null)
            }
        }
        return when {
            punkte.isEmpty() -> VisualisierungsErgebnis.NichtDarstellbar("Die endliche Menge enthält keine numerischen $dimension-dimensionalen Tupel.")
            übersprungen > 0 -> VisualisierungsErgebnis.Teilweise(punkte, listOf("$übersprungen nicht numerisch auswertbare Elemente wurden übersprungen."))
            else -> VisualisierungsErgebnis.Erfolgreich(punkte, istApproximation = false)
        }
    }

    private fun sampleAbbild(abbild: Abbild, konfiguration: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val parameter = abbild.methode.parameter.singleOrNull() as? Variable
            ?: return VisualisierungsErgebnis.NichtDarstellbar(
                "Die darzustellende Abbildung benötigt genau einen numerischen Parameter.",
            )
        val ausgabe = abbild.methode.ausgaben.values.singleOrNull()
            ?: return VisualisierungsErgebnis.NichtDarstellbar(
                "Die darzustellende Abbildung benötigt genau eine Ausgabe.",
            )
        val dimension = if (konfiguration.dimension == RaumDimension.R3) 3 else 2
        val parameterSampling = when (val ergebnis = sampleParameterMenge(abbild.menge, konfiguration)) {
            is ParameterSampling.Fehler -> return VisualisierungsErgebnis.NichtDarstellbar(ergebnis.grund)
            is ParameterSampling.Erfolgreich -> ergebnis
        }
        if (parameterSampling.werte.isEmpty()) {
            return VisualisierungsErgebnis.NichtDarstellbar("Die Definitionsmenge der Abbildung ist leer.")
        }

        val punkte = mutableListOf<VisualisierungsPunkt>()
        var übersprungen = 0
        parameterSampling.werte.forEach { parameterWert ->
            val parameterUmgebung = mapOf(parameter.name to parameterWert)
            val koordinaten = numerischeKoordinaten(ausgabe, parameterUmgebung)
            if (koordinaten == null || koordinaten.size != dimension) {
                übersprungen++
            } else {
                val farbUmgebung = buildMap {
                    putAll(parameterUmgebung)
                    put(konfiguration.achsen.x, koordinaten[0])
                    put(konfiguration.achsen.y, koordinaten[1])
                    if (dimension == 3) konfiguration.achsen.z?.let { put(it, koordinaten[2]) }
                }
                punkte += VisualisierungsPunkt(
                    x = koordinaten[0],
                    y = koordinaten[1],
                    z = koordinaten.getOrNull(2),
                    farbwert = farbwert(konfiguration, farbUmgebung),
                )
            }
        }

        return when {
            punkte.isEmpty() -> VisualisierungsErgebnis.NichtDarstellbar(
                "Die Abbildung erzeugt keine numerischen $dimension-dimensionalen Tupel oder Vektoren.",
            )
            übersprungen > 0 -> VisualisierungsErgebnis.Teilweise(
                punkte,
                parameterSampling.hinweise + "$übersprungen nicht numerisch auswertbare Bildpunkte wurden übersprungen.",
            )
            else -> VisualisierungsErgebnis.Erfolgreich(
                punkte,
                istApproximation = parameterSampling.istApproximation,
                hinweise = parameterSampling.hinweise,
            )
        }
    }

    private sealed interface ParameterSampling {
        data class Erfolgreich(
            val werte: List<Double>,
            val istApproximation: Boolean,
            val hinweise: List<String> = emptyList(),
        ) : ParameterSampling
        data class Fehler(val grund: String) : ParameterSampling
    }

    private fun sampleParameterMenge(
        menge: MengenAusdruck,
        konfiguration: VisualisierungsKonfiguration,
    ): ParameterSampling = when (menge) {
        LeereMenge -> ParameterSampling.Erfolgreich(emptyList(), istApproximation = false)

        is EndlicheMenge -> {
            val werte = menge.elemente.map { numerisch(it as? ZahlAusdruck, emptyMap()) }
            if (werte.any { it == null || !it.isFinite() }) {
                ParameterSampling.Fehler("Die endliche Definitionsmenge enthält nichtnumerische Elemente.")
            } else {
                ParameterSampling.Erfolgreich(werte.filterNotNull().distinct(), istApproximation = false)
            }
        }

        is ReellesIntervall -> {
            val minimum = numerisch(menge.untereGrenze, emptyMap())
            val maximum = numerisch(menge.obereGrenze, emptyMap())
            if (minimum == null || maximum == null || !minimum.isFinite() || !maximum.isFinite()) {
                ParameterSampling.Fehler("Die Grenzen des reellen Intervalls sind nicht numerisch auswertbar.")
            } else {
                val anzahl = (konfiguration.sampling.auflösung2D * 4).coerceIn(32, 2048)
                val werte = List(anzahl) { index ->
                    minimum + (maximum - minimum) * index.toDouble() / (anzahl - 1)
                }
                ParameterSampling.Erfolgreich(
                    werte,
                    istApproximation = true,
                    hinweise = listOf("Das kontinuierliche Abbild wird durch ${werte.size} Parameterwerte angenähert."),
                )
            }
        }

        is Vereinigung -> {
            val teile = menge.mengen.map { sampleParameterMenge(it, konfiguration) }
            teile.filterIsInstance<ParameterSampling.Fehler>().firstOrNull()
                ?: teile.filterIsInstance<ParameterSampling.Erfolgreich>().let { erfolge ->
                    ParameterSampling.Erfolgreich(
                        werte = erfolge.flatMap { it.werte }.distinct(),
                        istApproximation = erfolge.any { it.istApproximation },
                        hinweise = erfolge.flatMap { it.hinweise }.distinct(),
                    )
                }
        }

        else -> ParameterSampling.Fehler(
            "Die Definitionsmenge ${menge.zuLatex()} kann noch nicht als eindimensionaler Parameterbereich gesampelt werden.",
        )
    }

    private fun sampleDefiniert(menge: DefinierteMenge, konfiguration: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val vars = menge.variablen.map { it.variable.name }
        val achsen = listOfNotNull(konfiguration.achsen.x, konfiguration.achsen.y, if (konfiguration.dimension == RaumDimension.R3) konfiguration.achsen.z else null)
        if (achsen.distinct().size != achsen.size || achsen.any { it !in vars }) return VisualisierungsErgebnis.NichtDarstellbar("Die Achsenzuordnung enthält unbekannte oder doppelte Mengenvariablen.")
        return if (konfiguration.dimension == RaumDimension.R2) sample2D(menge.bedingung, konfiguration) else sample3D(menge.bedingung, konfiguration)
    }

    private fun sample2D(aussage: Aussage, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val n = c.sampling.auflösung2D
        val bx = c.bereiche.x; val by = c.bereiche.y
        val punkte = mutableListOf<VisualisierungsPunkt>()
        for (iy in 0 until n) for (ix in 0 until n) {
            val x = lerp(bx, ix.toDouble() / (n - 1)); val y = lerp(by, iy.toDouble() / (n - 1))
            val umgebung = mapOf(c.achsen.x to x, c.achsen.y to y)
            when (val wert = bewerteAussage(aussage, umgebung, c.sampling.toleranz)) {
                is NumerischeAussage.Gleichheit -> {
                    val dx = (bx.maximum - bx.minimum) / (n - 1); val dy = (by.maximum - by.minimum) / (n - 1)
                    val rechts = bewerteAussage(aussage, umgebung + (c.achsen.x to x + dx), c.sampling.toleranz)
                    val oben = bewerteAussage(aussage, umgebung + (c.achsen.y to y + dy), c.sampling.toleranz)
                    if (abs(wert.residuum) <= c.sampling.toleranz || (rechts as? NumerischeAussage.Gleichheit)?.residuum?.let { it * wert.residuum <= 0 } == true || (oben as? NumerischeAussage.Gleichheit)?.residuum?.let { it * wert.residuum <= 0 } == true) punkte += VisualisierungsPunkt(x, y, farbwert(c, umgebung))
                }
                NumerischeAussage.Wahr -> punkte += VisualisierungsPunkt(x, y, farbwert(c, umgebung))
                else -> Unit
            }
        }
        return if (punkte.isEmpty()) VisualisierungsErgebnis.NichtDarstellbar("Im gewählten Bereich wurden keine darstellbaren Punkte gefunden.") else VisualisierungsErgebnis.Erfolgreich(punkte)
    }

    private fun sample3D(aussage: Aussage, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val n = c.sampling.auflösung3D; val bz = c.bereiche.z ?: return VisualisierungsErgebnis.NichtDarstellbar("Für R³ fehlt ein Z-Achsenbereich.")
        val bx = c.bereiche.x; val by = c.bereiche.y; val schwelle = c.sampling.toleranz * maxOf(bx.maximum - bx.minimum, by.maximum - by.minimum, bz.maximum - bz.minimum)
        val punkte = mutableListOf<VisualisierungsPunkt>()
        for (iz in 0 until n) for (iy in 0 until n) for (ix in 0 until n) {
            val x = lerp(bx, ix.toDouble() / (n - 1)); val y = lerp(by, iy.toDouble() / (n - 1)); val z = lerp(bz, iz.toDouble() / (n - 1))
            val umgebung = mapOf(c.achsen.x to x, c.achsen.y to y, (c.achsen.z ?: return VisualisierungsErgebnis.NichtDarstellbar("Für R³ fehlt eine Z-Achse.")) to z)
            when (val wert = bewerteAussage(aussage, umgebung, c.sampling.toleranz)) {
                is NumerischeAussage.Gleichheit -> if (abs(wert.residuum) <= schwelle) punkte += VisualisierungsPunkt(x, y, z, farbwert(c, umgebung))
                NumerischeAussage.Wahr -> punkte += VisualisierungsPunkt(x, y, z, farbwert(c, umgebung))
                else -> Unit
            }
        }
        return if (punkte.isEmpty()) VisualisierungsErgebnis.NichtDarstellbar("Im gewählten R³-Bereich wurden keine Oberflächen- oder Bereichspunkte gefunden.") else VisualisierungsErgebnis.Erfolgreich(punkte, hinweise = listOf("R³ wird als numerische Oberflächen-Punktwolke angenähert."))
    }

    private fun farbwert(c: VisualisierungsKonfiguration, umgebung: Map<String, Double>) = if (c.farbe.modus == FarbModus.Spektrum) c.farbe.variable?.let(umgebung::get) else null
    private fun lerp(b: ZahlenBereich, t: Double) = b.minimum + (b.maximum - b.minimum) * t
}

private fun numerischeKoordinaten(
    objekt: MathematischesObjekt,
    werte: Map<String, Double>,
): List<Double>? {
    if (objekt is FallAusdruck) {
        return when (bewerteAussage(objekt.aussage, werte, 1e-9)) {
            NumerischeAussage.Wahr -> numerischeKoordinaten(objekt.wahr, werte)
            NumerischeAussage.Lüge -> numerischeKoordinaten(objekt.lüge, werte)
            else -> null
        }
    }
    val ausdrücke = when (objekt) {
        is Tupel -> objekt.elemente.map { it as? ZahlAusdruck ?: return null }
        is SpaltenVektor -> objekt.werte
        is ZeilenVektor -> objekt.werte
        else -> return null
    }
    return ausdrücke.map { numerisch(it, werte) ?: return null }.takeIf { koordinaten ->
        koordinaten.all { it.isFinite() }
    }
}

private sealed interface NumerischeAussage { data object Wahr : NumerischeAussage; data object Lüge : NumerischeAussage; data object Unbekannt : NumerischeAussage; data class Gleichheit(val residuum: Double) : NumerischeAussage }

private fun bewerteAussage(a: Aussage, werte: Map<String, Double>, toleranz: Double): NumerischeAussage = when (a) {
    is Gleichheit -> residuum(a.links, a.rechts, werte)?.let(NumerischeAussage::Gleichheit) ?: NumerischeAussage.Unbekannt
    is Ungleichheit -> residuum(a.links, a.rechts, werte)?.let { if (abs(it) > toleranz) NumerischeAussage.Wahr else NumerischeAussage.Lüge } ?: NumerischeAussage.Unbekannt
    is Vergleich -> { val l = numerisch(a.links, werte); val r = numerisch(a.rechts, werte); if (l == null || r == null) NumerischeAussage.Unbekannt else if (when (a.art) { VergleichsArt.Kleiner -> l < r; VergleichsArt.KleinerGleich -> l <= r + toleranz; VergleichsArt.Größer -> l > r; VergleichsArt.GrößerGleich -> l >= r - toleranz }) NumerischeAussage.Wahr else NumerischeAussage.Lüge }
    is Negation -> when (bewerteAussage(a.aussage, werte, toleranz)) { NumerischeAussage.Wahr -> NumerischeAussage.Lüge; NumerischeAussage.Lüge -> NumerischeAussage.Wahr; else -> NumerischeAussage.Unbekannt }
    is Konjunktion -> kombiniere(a.aussagen.map { bewerteAussage(it, werte, toleranz) }, true)
    is Disjunktion -> kombiniere(a.aussagen.map { bewerteAussage(it, werte, toleranz) }, false)
    is WahrheitsKonstante -> if (a.wert) NumerischeAussage.Wahr else NumerischeAussage.Lüge
    else -> NumerischeAussage.Unbekannt
}
private fun kombiniere(werte: List<NumerischeAussage>, und: Boolean) = when { und && werte.any { it == NumerischeAussage.Lüge } -> NumerischeAussage.Lüge; !und && werte.any { it == NumerischeAussage.Wahr } -> NumerischeAussage.Wahr; werte.all { it == NumerischeAussage.Wahr } && und -> NumerischeAussage.Wahr; werte.all { it == NumerischeAussage.Lüge } && !und -> NumerischeAussage.Lüge; else -> NumerischeAussage.Unbekannt }
private fun residuum(l: MathematischesObjekt, r: MathematischesObjekt, werte: Map<String, Double>) = numerisch(l as? ZahlAusdruck, werte)?.let { links -> numerisch(r as? ZahlAusdruck, werte)?.let { links - it } }
private fun numerisch(ausdruck: ZahlAusdruck?, werte: Map<String, Double>): Double? = when (ausdruck) {
    null -> null
    is RationaleZahl -> ausdruck.zuDezimal(16).toDouble()
    is Variable -> werte[ausdruck.name]
    is MathematischeKonstante -> when (ausdruck) {
        Pi -> Math.PI
        EulerscheZahl -> Math.E
        else -> null
    }
    is Addition -> ausdruck.summanden.map { numerisch(it, werte) }.takeUnless { it.any { n -> n == null } }?.sumOf { it!! }
    is Multiplikation -> ausdruck.faktoren.map { numerisch(it, werte) }.takeUnless { it.any { n -> n == null } }?.fold(1.0) { acc, n -> acc * n!! }
    is Division -> numerisch(ausdruck.dividend, werte)?.let { l -> numerisch(ausdruck.divisor, werte)?.takeIf { abs(it) > 1e-12 }?.let { l / it } }
    is Potenz -> numerisch(ausdruck.basis, werte)?.let { b -> numerisch(ausdruck.exponent, werte)?.let { b.pow(it) } }
    is Betrag -> numerisch(ausdruck.argument, werte)?.let(::abs)
    is Sinus -> numerisch(ausdruck.argument, werte)?.let(::sin)
    is Cosinus -> numerisch(ausdruck.argument, werte)?.let(::cos)
    is Exponentialfunktion -> numerisch(ausdruck.argument, werte)?.let(::exp)
    is NatürlicherLogarithmus -> numerisch(ausdruck.argument, werte)?.takeIf { it > 0 }?.let(::ln)
    is Wurzel -> numerisch(ausdruck.argument, werte)?.takeIf { it >= 0 }?.let(::sqrt)
    else -> null
}
