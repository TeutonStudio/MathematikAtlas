package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.*

data class VisualisierungsPunkt(val x: Double, val y: Double = 0.0, val z: Double? = null, val farbwert: Double? = null)

sealed interface VisualisierungsErgebnis {
    data class Erfolgreich(val punkte: List<VisualisierungsPunkt>, val istApproximation: Boolean = true, val hinweise: List<String> = emptyList()) : VisualisierungsErgebnis
    data class Teilweise(val punkte: List<VisualisierungsPunkt>, val hinweise: List<String>) : VisualisierungsErgebnis
    data class NichtDarstellbar(val grund: String) : VisualisierungsErgebnis
}

/** Plattformneutrales Sampling für R¹, R² und R³. */
object VisualisierungsSampler {
    fun sample(menge: MengenAusdruck, konfiguration: VisualisierungsKonfiguration): VisualisierungsErgebnis = when (menge) {
        LeereMenge -> VisualisierungsErgebnis.Erfolgreich(emptyList(), istApproximation = false, hinweise = listOf("Die Menge ist leer."))
        is EndlicheMenge -> sampleEndlich(menge, konfiguration)
        is ReellesIntervall -> sampleKartesischesProdukt(listOf(menge), konfiguration)
        is KartesischesProdukt -> sampleKartesischesProdukt(menge.mengen, konfiguration)
        is DefinierteMenge -> sampleDefiniert(menge, konfiguration)
        is Abbild -> sampleAbbild(menge, konfiguration)
        is Vereinigung -> sampleVereinigung(menge, konfiguration)
        else -> VisualisierungsErgebnis.NichtDarstellbar("Diese Mengenform kann noch nicht numerisch dargestellt werden.")
    }

    private fun sampleVereinigung(menge: Vereinigung, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val teile = menge.mengen.map { sample(it, c) }
        val fehler = teile.filterIsInstance<VisualisierungsErgebnis.NichtDarstellbar>()
        val punkte = teile.flatMap {
            when (it) {
                is VisualisierungsErgebnis.Erfolgreich -> it.punkte
                is VisualisierungsErgebnis.Teilweise -> it.punkte
                is VisualisierungsErgebnis.NichtDarstellbar -> emptyList()
            }
        }.distinct()
        if (punkte.isEmpty()) return fehler.firstOrNull() ?: VisualisierungsErgebnis.NichtDarstellbar("Die Vereinigung enthält keine darstellbaren Punkte.")
        val hinweise = teile.flatMap {
            when (it) {
                is VisualisierungsErgebnis.Erfolgreich -> it.hinweise
                is VisualisierungsErgebnis.Teilweise -> it.hinweise
                is VisualisierungsErgebnis.NichtDarstellbar -> listOf(it.grund)
            }
        }.distinct()
        return if (fehler.isEmpty()) VisualisierungsErgebnis.Erfolgreich(punkte, teile.any { it !is VisualisierungsErgebnis.Erfolgreich || it.istApproximation }, hinweise)
        else VisualisierungsErgebnis.Teilweise(punkte, hinweise)
    }

    private fun sampleEndlich(menge: EndlicheMenge, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val dimension = c.dimensionsAnzahl
        val punkte = mutableListOf<VisualisierungsPunkt>()
        var übersprungen = 0
        menge.elemente.forEach { element ->
            val koordinaten = when {
                dimension == 1 && element is ZahlAusdruck -> listOfNotNull(numerisch(element, emptyMap()))
                else -> numerischeKoordinaten(element, emptyMap())
            }
            if (koordinaten == null || koordinaten.size != dimension || koordinaten.any { !it.isFinite() }) {
                übersprungen++
            } else {
                punkte += VisualisierungsPunkt(koordinaten[0], koordinaten.getOrElse(1) { 0.0 }, koordinaten.getOrNull(2))
            }
        }
        return when {
            punkte.isEmpty() && menge.elemente.isEmpty() -> VisualisierungsErgebnis.Erfolgreich(emptyList(), istApproximation = false)
            punkte.isEmpty() -> VisualisierungsErgebnis.NichtDarstellbar("Die endliche Menge enthält keine numerischen $dimension-dimensionalen Werte.")
            übersprungen > 0 -> VisualisierungsErgebnis.Teilweise(punkte, listOf("$übersprungen nicht numerische Elemente wurden übersprungen."))
            else -> VisualisierungsErgebnis.Erfolgreich(punkte, istApproximation = false)
        }
    }

    private fun sampleKartesischesProdukt(komponenten: List<MengenAusdruck>, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val dimension = c.dimensionsAnzahl
        if (komponenten.size != dimension) {
            return VisualisierungsErgebnis.NichtDarstellbar("Das kartesische Produkt besitzt ${komponenten.size} Komponenten, die Darstellung erwartet $dimension.")
        }
        val auflösung = when (c.dimension) {
            RaumDimension.R1 -> (c.sampling.auflösung2D * 4).coerceIn(32, 2048)
            RaumDimension.R2 -> c.sampling.auflösung2D.coerceIn(16, 180)
            RaumDimension.R3 -> c.sampling.auflösung3D.coerceIn(8, 48)
        }
        val samples = komponenten.map { sampleKomponente(it, auflösung) }
        val fehler = samples.filterIsInstance<KomponentenSampling.Fehler>().firstOrNull()
        if (fehler != null) return VisualisierungsErgebnis.NichtDarstellbar(fehler.grund)
        val erfolgreich = samples.filterIsInstance<KomponentenSampling.Erfolgreich>()
        if (erfolgreich.any { it.werte.isEmpty() }) return VisualisierungsErgebnis.Erfolgreich(emptyList(), istApproximation = false)
        val koordinaten = kartesisch(erfolgreich.map { it.werte })
        val punkte = koordinaten.map { werte ->
            VisualisierungsPunkt(werte[0], werte.getOrElse(1) { 0.0 }, werte.getOrNull(2))
        }
        return VisualisierungsErgebnis.Erfolgreich(
            punkte = punkte,
            istApproximation = erfolgreich.any { it.istApproximation },
            hinweise = if (erfolgreich.any { it.istApproximation }) listOf("Kontinuierliche Produktmengen werden durch ${punkte.size} Rasterpunkte angenähert.") else emptyList(),
        )
    }

    private sealed interface KomponentenSampling {
        data class Erfolgreich(val werte: List<Double>, val istApproximation: Boolean) : KomponentenSampling
        data class Fehler(val grund: String) : KomponentenSampling
    }

    private fun sampleKomponente(menge: MengenAusdruck, auflösung: Int): KomponentenSampling = when (menge) {
        LeereMenge -> KomponentenSampling.Erfolgreich(emptyList(), false)
        is EndlicheMenge -> {
            val werte = menge.elemente.map { numerisch(it as? ZahlAusdruck, emptyMap()) }
            if (werte.any { it == null || !it.isFinite() }) KomponentenSampling.Fehler("Eine Produktkomponente enthält nichtnumerische Elemente.")
            else KomponentenSampling.Erfolgreich(werte.filterNotNull().distinct(), false)
        }
        is ReellesIntervall -> {
            val minimum = numerisch(menge.untereGrenze, emptyMap())
            val maximum = numerisch(menge.obereGrenze, emptyMap())
            if (minimum == null || maximum == null || !minimum.isFinite() || !maximum.isFinite()) {
                KomponentenSampling.Fehler("Die Intervallgrenzen sind nicht numerisch auswertbar.")
            } else KomponentenSampling.Erfolgreich(
                List(auflösung) { index -> minimum + (maximum - minimum) * index.toDouble() / (auflösung - 1).coerceAtLeast(1) },
                true,
            )
        }
        is Vereinigung -> {
            val teile = menge.mengen.map { sampleKomponente(it, auflösung) }
            teile.filterIsInstance<KomponentenSampling.Fehler>().firstOrNull()
                ?: KomponentenSampling.Erfolgreich(
                    teile.filterIsInstance<KomponentenSampling.Erfolgreich>().flatMap { it.werte }.distinct(),
                    teile.filterIsInstance<KomponentenSampling.Erfolgreich>().any { it.istApproximation },
                )
        }
        else -> KomponentenSampling.Fehler("Die Produktkomponente ${menge.zuLatex()} ist nicht eindimensional numerisch samplbar.")
    }

    private fun kartesisch(komponenten: List<List<Double>>): List<List<Double>> =
        komponenten.fold(listOf(emptyList())) { aktuell, werte -> aktuell.flatMap { präfix -> werte.map { präfix + it } } }

    private fun sampleAbbild(abbild: Abbild, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val parameter = abbild.methode.parameter.singleOrNull() as? Variable
            ?: return VisualisierungsErgebnis.NichtDarstellbar("Die Abbildung benötigt genau einen numerischen Parameter.")
        val ausgabe = abbild.methode.ausgaben.values.singleOrNull()
            ?: return VisualisierungsErgebnis.NichtDarstellbar("Die Abbildung benötigt genau eine Ausgabe.")
        val parameterSampling = when (val ergebnis = sampleParameterMenge(abbild.menge, c)) {
            is ParameterSampling.Fehler -> return VisualisierungsErgebnis.NichtDarstellbar(ergebnis.grund)
            is ParameterSampling.Erfolgreich -> ergebnis
        }
        val punkte = mutableListOf<VisualisierungsPunkt>()
        var übersprungen = 0
        parameterSampling.werte.forEach { parameterWert ->
            val umgebung = mapOf(parameter.name to parameterWert)
            val koordinaten = numerischeKoordinaten(ausgabe, umgebung)
            if (koordinaten == null || koordinaten.size != c.dimensionsAnzahl) übersprungen++ else {
                val farbUmgebung = buildMap {
                    putAll(umgebung)
                    put(c.achsen.x, koordinaten[0])
                    if (c.dimensionsAnzahl >= 2) put(c.achsen.y, koordinaten[1])
                    if (c.dimensionsAnzahl == 3) c.achsen.z?.let { put(it, koordinaten[2]) }
                }
                punkte += VisualisierungsPunkt(koordinaten[0], koordinaten.getOrElse(1) { 0.0 }, koordinaten.getOrNull(2), farbwert(c, farbUmgebung))
            }
        }
        return when {
            punkte.isEmpty() -> VisualisierungsErgebnis.NichtDarstellbar("Die Abbildung erzeugt keine numerischen ${c.dimensionsAnzahl}-dimensionalen Werte.")
            übersprungen > 0 -> VisualisierungsErgebnis.Teilweise(punkte, parameterSampling.hinweise + "$übersprungen Bildpunkte wurden übersprungen.")
            else -> VisualisierungsErgebnis.Erfolgreich(punkte, parameterSampling.istApproximation, parameterSampling.hinweise)
        }
    }

    private sealed interface ParameterSampling {
        data class Erfolgreich(val werte: List<Double>, val istApproximation: Boolean, val hinweise: List<String> = emptyList()) : ParameterSampling
        data class Fehler(val grund: String) : ParameterSampling
    }

    private fun sampleParameterMenge(menge: MengenAusdruck, c: VisualisierungsKonfiguration): ParameterSampling = when (val ergebnis = sampleKomponente(menge, (c.sampling.auflösung2D * 4).coerceIn(32, 2048))) {
        is KomponentenSampling.Fehler -> ParameterSampling.Fehler(ergebnis.grund)
        is KomponentenSampling.Erfolgreich -> ParameterSampling.Erfolgreich(
            ergebnis.werte,
            ergebnis.istApproximation,
            if (ergebnis.istApproximation) listOf("Das kontinuierliche Abbild wird durch ${ergebnis.werte.size} Parameterwerte angenähert.") else emptyList(),
        )
    }

    private fun sampleDefiniert(menge: DefinierteMenge, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val variablen = menge.variablen.map { it.variable.name }
        val achsen = when (c.dimension) {
            RaumDimension.R1 -> listOf(c.achsen.x)
            RaumDimension.R2 -> listOf(c.achsen.x, c.achsen.y)
            RaumDimension.R3 -> listOfNotNull(c.achsen.x, c.achsen.y, c.achsen.z)
        }
        if (achsen.size != c.dimensionsAnzahl || achsen.distinct().size != achsen.size || achsen.any { it !in variablen }) {
            return VisualisierungsErgebnis.NichtDarstellbar("Die Achsenzuordnung enthält unbekannte oder doppelte Mengenvariablen.")
        }
        return when (c.dimension) {
            RaumDimension.R1 -> sample1D(menge.bedingung, c)
            RaumDimension.R2 -> sample2D(menge.bedingung, c)
            RaumDimension.R3 -> sample3D(menge.bedingung, c)
        }
    }

    private fun sample1D(aussage: Aussage, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val n = (c.sampling.auflösung2D * 4).coerceIn(32, 2048)
        val punkte = mutableListOf<VisualisierungsPunkt>()
        for (ix in 0 until n) {
            val x = lerp(c.bereiche.x, ix.toDouble() / (n - 1))
            val umgebung = mapOf(c.achsen.x to x)
            when (val wert = bewerteAussage(aussage, umgebung, c.sampling.toleranz)) {
                NumerischeAussage.Wahr -> punkte += VisualisierungsPunkt(x, farbwert = farbwert(c, umgebung))
                is NumerischeAussage.Gleichheit -> if (abs(wert.residuum) <= c.sampling.toleranz) punkte += VisualisierungsPunkt(x, farbwert = farbwert(c, umgebung))
                else -> Unit
            }
        }
        return if (punkte.isEmpty()) VisualisierungsErgebnis.NichtDarstellbar("Im gewählten R¹-Bereich wurden keine darstellbaren Werte gefunden.")
        else VisualisierungsErgebnis.Erfolgreich(punkte)
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
                    if (abs(wert.residuum) <= c.sampling.toleranz || (rechts as? NumerischeAussage.Gleichheit)?.residuum?.let { it * wert.residuum <= 0 } == true || (oben as? NumerischeAussage.Gleichheit)?.residuum?.let { it * wert.residuum <= 0 } == true) punkte += VisualisierungsPunkt(x, y, farbwert = farbwert(c, umgebung))
                }
                NumerischeAussage.Wahr -> punkte += VisualisierungsPunkt(x, y, farbwert = farbwert(c, umgebung))
                else -> Unit
            }
        }
        return if (punkte.isEmpty()) VisualisierungsErgebnis.NichtDarstellbar("Im gewählten Bereich wurden keine darstellbaren Punkte gefunden.") else VisualisierungsErgebnis.Erfolgreich(punkte)
    }

    private fun sample3D(aussage: Aussage, c: VisualisierungsKonfiguration): VisualisierungsErgebnis {
        val n = c.sampling.auflösung3D
        val bz = c.bereiche.z ?: return VisualisierungsErgebnis.NichtDarstellbar("Für R³ fehlt ein Z-Achsenbereich.")
        val bx = c.bereiche.x; val by = c.bereiche.y
        val schwelle = c.sampling.toleranz * maxOf(bx.maximum - bx.minimum, by.maximum - by.minimum, bz.maximum - bz.minimum)
        val punkte = mutableListOf<VisualisierungsPunkt>()
        for (iz in 0 until n) for (iy in 0 until n) for (ix in 0 until n) {
            val x = lerp(bx, ix.toDouble() / (n - 1)); val y = lerp(by, iy.toDouble() / (n - 1)); val z = lerp(bz, iz.toDouble() / (n - 1))
            val zName = c.achsen.z ?: return VisualisierungsErgebnis.NichtDarstellbar("Für R³ fehlt eine Z-Achse.")
            val umgebung = mapOf(c.achsen.x to x, c.achsen.y to y, zName to z)
            when (val wert = bewerteAussage(aussage, umgebung, c.sampling.toleranz)) {
                is NumerischeAussage.Gleichheit -> if (abs(wert.residuum) <= schwelle) punkte += VisualisierungsPunkt(x, y, z, farbwert(c, umgebung))
                NumerischeAussage.Wahr -> punkte += VisualisierungsPunkt(x, y, z, farbwert(c, umgebung))
                else -> Unit
            }
        }
        return if (punkte.isEmpty()) VisualisierungsErgebnis.NichtDarstellbar("Im gewählten R³-Bereich wurden keine Punkte gefunden.")
        else VisualisierungsErgebnis.Erfolgreich(punkte, hinweise = listOf("R³ wird als numerische Punktwolke angenähert."))
    }

    private fun farbwert(c: VisualisierungsKonfiguration, umgebung: Map<String, Double>) = if (c.farbe.modus == FarbModus.Spektrum) c.farbe.variable?.let(umgebung::get) else null
    private fun lerp(b: ZahlenBereich, t: Double) = b.minimum + (b.maximum - b.minimum) * t
}

private fun numerischeKoordinaten(objekt: MathematischesObjekt, werte: Map<String, Double>): List<Double>? {
    if (objekt is FallAusdruck) return when (bewerteAussage(objekt.aussage, werte, 1e-9)) {
        NumerischeAussage.Wahr -> numerischeKoordinaten(objekt.wahr, werte)
        NumerischeAussage.Lüge -> numerischeKoordinaten(objekt.lüge, werte)
        else -> null
    }
    val ausdrücke = when (objekt) {
        is ZahlAusdruck -> listOf(objekt)
        is Tupel -> objekt.elemente.map { it as? ZahlAusdruck ?: return null }
        is SpaltenVektor -> objekt.werte
        is ZeilenVektor -> objekt.werte
        else -> return null
    }
    return ausdrücke.map { numerisch(it, werte) ?: return null }.takeIf { koordinaten -> koordinaten.all(Double::isFinite) }
}

private sealed interface NumerischeAussage {
    data object Wahr : NumerischeAussage
    data object Lüge : NumerischeAussage
    data object Unbekannt : NumerischeAussage
    data class Gleichheit(val residuum: Double) : NumerischeAussage
}

private fun bewerteAussage(a: Aussage, werte: Map<String, Double>, toleranz: Double): NumerischeAussage = when (a) {
    is AussagenFallAusdruck -> when (bewerteAussage(a.aussage, werte, toleranz)) {
        NumerischeAussage.Wahr -> bewerteAussage(a.wahr, werte, toleranz)
        NumerischeAussage.Lüge -> bewerteAussage(a.lüge, werte, toleranz)
        else -> NumerischeAussage.Unbekannt
    }
    is Gleichheit -> residuum(a.links, a.rechts, werte)?.let(NumerischeAussage::Gleichheit) ?: NumerischeAussage.Unbekannt
    is Ungleichheit -> residuum(a.links, a.rechts, werte)?.let { if (abs(it) > toleranz) NumerischeAussage.Wahr else NumerischeAussage.Lüge } ?: NumerischeAussage.Unbekannt
    is Vergleich -> {
        val l = numerisch(a.links, werte); val r = numerisch(a.rechts, werte)
        if (l == null || r == null) NumerischeAussage.Unbekannt else if (when (a.art) {
            VergleichsArt.Kleiner -> l < r
            VergleichsArt.KleinerGleich -> l <= r + toleranz
            VergleichsArt.Größer -> l > r
            VergleichsArt.GrößerGleich -> l >= r - toleranz
        }) NumerischeAussage.Wahr else NumerischeAussage.Lüge
    }
    is Negation -> when (bewerteAussage(a.aussage, werte, toleranz)) { NumerischeAussage.Wahr -> NumerischeAussage.Lüge; NumerischeAussage.Lüge -> NumerischeAussage.Wahr; else -> NumerischeAussage.Unbekannt }
    is Konjunktion -> kombiniere(a.aussagen.map { bewerteAussage(it, werte, toleranz) }, true)
    is Disjunktion -> kombiniere(a.aussagen.map { bewerteAussage(it, werte, toleranz) }, false)
    is WahrheitsKonstante -> if (a.wert) NumerischeAussage.Wahr else NumerischeAussage.Lüge
    else -> NumerischeAussage.Unbekannt
}

private fun kombiniere(werte: List<NumerischeAussage>, und: Boolean) = when {
    und && werte.any { it == NumerischeAussage.Lüge } -> NumerischeAussage.Lüge
    !und && werte.any { it == NumerischeAussage.Wahr } -> NumerischeAussage.Wahr
    werte.all { it == NumerischeAussage.Wahr } && und -> NumerischeAussage.Wahr
    werte.all { it == NumerischeAussage.Lüge } && !und -> NumerischeAussage.Lüge
    else -> NumerischeAussage.Unbekannt
}
private fun residuum(l: MathematischesObjekt, r: MathematischesObjekt, werte: Map<String, Double>) = numerisch(l as? ZahlAusdruck, werte)?.let { links -> numerisch(r as? ZahlAusdruck, werte)?.let { links - it } }
private fun numerisch(ausdruck: ZahlAusdruck?, werte: Map<String, Double>): Double? = when (ausdruck) {
    null -> null
    is ZahlFallAusdruck -> when (bewerteAussage(ausdruck.aussage, werte, 1e-9)) {
        NumerischeAussage.Wahr -> numerisch(ausdruck.wahr, werte)
        NumerischeAussage.Lüge -> numerisch(ausdruck.lüge, werte)
        else -> null
    }
    is RationaleZahl -> ausdruck.zuDezimal(16).toDouble()
    is Variable -> werte[ausdruck.name]
    is MathematischeKonstante -> when (ausdruck) { Pi -> Math.PI; EulerscheZahl -> Math.E; else -> null }
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
