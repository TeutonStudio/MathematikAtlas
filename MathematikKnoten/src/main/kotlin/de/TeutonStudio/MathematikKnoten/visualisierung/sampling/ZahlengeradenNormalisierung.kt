package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.VisualisierungsKonfiguration
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Exakte eindimensionale Normalisierung ohne Punktwolken für kontinuierliche Mengen. */
internal object ZahlengeradenNormalisierer {
    fun normalisiere(
        menge: MengenAusdruck,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsDefinition {
        val toleranz = konfiguration.sampling.toleranz
        return when (val ergebnis = lineareMenge(menge, konfiguration, toleranz)) {
            is LinearesErgebnis.Fehler -> VisualisierungsDefinition.NichtRäumlich(ergebnis.grund)
            is LinearesErgebnis.Erfolgreich -> {
                val sichtbar = ergebnis.menge.imFenster(konfiguration.bereiche.x.minimum, konfiguration.bereiche.x.maximum, toleranz)
                VisualisierungsDefinition.Zahlengerade(
                    punkte = sichtbar.punkte.sorted(),
                    intervalle = sichtbar.intervalle.map {
                        VisualisierungsIntervall(
                            von = it.von,
                            bis = it.bis,
                            linksGeschlossen = it.linksGeschlossen,
                            rechtsGeschlossen = it.rechtsGeschlossen,
                            linksAmFensterrand = it.linksAmFensterrand,
                            rechtsAmFensterrand = it.rechtsAmFensterrand,
                        )
                    },
                    hinweise = ergebnis.menge.hinweise,
                    mathematischLeer = ergebnis.menge.istMathematischLeer,
                )
            }
        }
    }

    private fun lineareMenge(
        menge: MengenAusdruck,
        c: VisualisierungsKonfiguration,
        toleranz: Double,
    ): LinearesErgebnis = when (menge) {
        LeereMenge -> erfolg(LineareMenge(istMathematischLeer = true))
        ReelleZahlen -> erfolg(LineareMenge(intervalle = listOf(LinearesIntervall(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, false))))
        GanzeZahlen -> ganzzahlen(c, natürlich = false)
        NatürlicheZahlen -> ganzzahlen(c, natürlich = true)
        RationaleZahlen -> fehler("ℚ ist dicht, aber nicht als endliche Punktmenge oder Intervallmenge exakt darstellbar.")
        KomplexeZahlen -> fehler("ℂ ist keine eindimensionale reelle Trägermenge. Verwende die komplexe Ebene.")
        is ReellesIntervall -> intervall(menge)
        is EndlicheMenge -> endlicheMenge(menge)
        is Vereinigung -> kombiniereListe(menge.mengen, c, toleranz, ::vereinigung)
        is Schnitt -> kombiniereListe(menge.mengen, c, toleranz, ::schnitt)
        is MengenDifferenz -> binär(menge.links, menge.rechts, c, toleranz, ::differenz)
        is SymmetrischeDifferenz -> binär(menge.links, menge.rechts, c, toleranz) { a, b ->
            vereinigung(differenz(a, b, toleranz), differenz(b, a, toleranz), toleranz)
        }
        is MengenFallAusdruck -> when (menge.aussage.entscheide(RechenKontext()).wahrheitswert) {
            Wahrheitswert.Wahr -> lineareMenge(menge.wahr, c, toleranz)
            Wahrheitswert.Lüge -> lineareMenge(menge.lüge, c, toleranz)
            null -> fehler("Die Fallbedingung der Menge ist nicht symbolisch entscheidbar.")
        }
        else -> fehler("${menge::class.simpleName} besitzt noch keine exakte R¹-Normalisierung.")
    }

    private fun intervall(menge: ReellesIntervall): LinearesErgebnis {
        val links = numerisch(menge.links) ?: return fehler("Die linke Intervallgrenze ist nicht numerisch auswertbar.")
        val rechts = numerisch(menge.rechts) ?: return fehler("Die rechte Intervallgrenze ist nicht numerisch auswertbar.")
        if (links > rechts || links == rechts && (menge.linksOffen || menge.rechtsOffen)) {
            return erfolg(LineareMenge(istMathematischLeer = true))
        }
        if (links == rechts) return erfolg(LineareMenge(punkte = setOf(links)))
        return erfolg(
            LineareMenge(
                intervalle = listOf(LinearesIntervall(links, rechts, !menge.linksOffen, !menge.rechtsOffen)),
            ),
        )
    }

    private fun endlicheMenge(menge: EndlicheMenge): LinearesErgebnis {
        val punkte = linkedSetOf<Double>()
        menge.elemente.forEachIndexed { index, element ->
            val ausdruck = when (element) {
                is ZahlAusdruck -> element
                is Tupel -> element.elemente.singleOrNull() as? ZahlAusdruck
                is ZeilenVektor -> element.werte.singleOrNull()
                is SpaltenVektor -> element.werte.singleOrNull()
                else -> null
            } ?: return fehler("Element ${index + 1} ist weder skalar noch ein eindimensionales Tupel oder ein Vektor der Länge eins.")
            val wert = numerisch(ausdruck) ?: return fehler("Element ${index + 1} ist nicht numerisch auswertbar.")
            if (!wert.isFinite()) return fehler("Element ${index + 1} ist nicht endlich.")
            punkte += wert
        }
        return erfolg(LineareMenge(punkte = punkte, istMathematischLeer = punkte.isEmpty()))
    }

    private fun ganzzahlen(c: VisualisierungsKonfiguration, natürlich: Boolean): LinearesErgebnis {
        val bereich = c.bereiche.x
        val start = ceil(bereich.minimum).toLong().coerceAtLeast(if (natürlich) 0L else Long.MIN_VALUE)
        val ende = floor(bereich.maximum).toLong()
        if (ende < start) return erfolg(LineareMenge())
        val anzahl = ende - start + 1
        if (anzahl > c.sampling.maximalesRasterBudget) {
            return fehler("Der sichtbare Bereich enthält $anzahl ganzzahlige Punkte und überschreitet das Rasterbudget.")
        }
        return erfolg(
            LineareMenge(
                punkte = (start..ende).mapTo(linkedSetOf(), Long::toDouble),
                hinweise = listOf(if (natürlich) "ℕ wird auf den sichtbaren Bereich begrenzt." else "ℤ wird auf den sichtbaren Bereich begrenzt."),
            ),
        )
    }

    private fun kombiniereListe(
        mengen: List<MengenAusdruck>,
        c: VisualisierungsKonfiguration,
        toleranz: Double,
        operation: (LineareMenge, LineareMenge, Double) -> LineareMenge,
    ): LinearesErgebnis {
        if (mengen.isEmpty()) return erfolg(LineareMenge(istMathematischLeer = true))
        var ergebnis = when (val erstes = lineareMenge(mengen.first(), c, toleranz)) {
            is LinearesErgebnis.Fehler -> return erstes
            is LinearesErgebnis.Erfolgreich -> erstes.menge
        }
        for (menge in mengen.drop(1)) {
            val rechts = when (val teil = lineareMenge(menge, c, toleranz)) {
                is LinearesErgebnis.Fehler -> return teil
                is LinearesErgebnis.Erfolgreich -> teil.menge
            }
            ergebnis = operation(ergebnis, rechts, toleranz)
        }
        return erfolg(ergebnis)
    }

    private fun binär(
        links: MengenAusdruck,
        rechts: MengenAusdruck,
        c: VisualisierungsKonfiguration,
        toleranz: Double,
        operation: (LineareMenge, LineareMenge, Double) -> LineareMenge,
    ): LinearesErgebnis {
        val a = when (val wert = lineareMenge(links, c, toleranz)) {
            is LinearesErgebnis.Fehler -> return wert
            is LinearesErgebnis.Erfolgreich -> wert.menge
        }
        val b = when (val wert = lineareMenge(rechts, c, toleranz)) {
            is LinearesErgebnis.Fehler -> return wert
            is LinearesErgebnis.Erfolgreich -> wert.menge
        }
        return erfolg(operation(a, b, toleranz))
    }

    private fun vereinigung(a: LineareMenge, b: LineareMenge, toleranz: Double): LineareMenge =
        kanonisch(a.punkte + b.punkte, a.intervalle + b.intervalle, a.hinweise + b.hinweise, toleranz)

    private fun schnitt(a: LineareMenge, b: LineareMenge, toleranz: Double): LineareMenge {
        val intervalle = buildList {
            a.intervalle.forEach { links -> b.intervalle.forEach { rechts -> schneide(links, rechts, toleranz)?.let(::add) } }
        }
        val punkte = (a.punkte.filter { b.enthält(it, toleranz) } + b.punkte.filter { a.enthält(it, toleranz) }).toSet()
        return kanonisch(punkte, intervalle, a.hinweise + b.hinweise, toleranz)
    }

    private fun differenz(a: LineareMenge, b: LineareMenge, toleranz: Double): LineareMenge {
        var intervalle = a.intervalle
        b.intervalle.forEach { abzuziehen -> intervalle = intervalle.flatMap { subtrahiere(it, abzuziehen, toleranz) } }
        b.punkte.forEach { punkt -> intervalle = intervalle.flatMap { subtrahierePunkt(it, punkt, toleranz) } }
        val punkte = a.punkte.filterNot { b.enthält(it, toleranz) }.toSet()
        return kanonisch(punkte, intervalle, a.hinweise + b.hinweise, toleranz)
    }

    private fun kanonisch(
        punkte: Collection<Double>,
        intervalle: Collection<LinearesIntervall>,
        hinweise: List<String>,
        toleranz: Double,
    ): LineareMenge {
        val einzelnePunkte = punkte.toMutableSet()
        val sortiert = intervalle.mapNotNull { intervall ->
            when {
                intervall.von < intervall.bis - toleranz -> intervall
                abs(intervall.von - intervall.bis) <= toleranz && intervall.linksGeschlossen && intervall.rechtsGeschlossen -> {
                    einzelnePunkte += (intervall.von + intervall.bis) / 2.0
                    null
                }
                else -> null
            }
        }.sortedBy(LinearesIntervall::von)
        val vereinigt = mutableListOf<LinearesIntervall>()
        sortiert.forEach { nächstes ->
            val aktuell = vereinigt.lastOrNull()
            if (aktuell == null || !verschmelzbar(aktuell, nächstes, toleranz)) {
                vereinigt += nächstes
            } else {
                vereinigt[vereinigt.lastIndex] = verschmelze(aktuell, nächstes, toleranz)
            }
        }
        einzelnePunkte.removeAll { punkt -> vereinigt.any { it.enthält(punkt, toleranz) } }
        return LineareMenge(
            punkte = einzelnePunkte,
            intervalle = vereinigt,
            hinweise = hinweise.distinct(),
            istMathematischLeer = einzelnePunkte.isEmpty() && vereinigt.isEmpty(),
        )
    }

    private fun schneide(a: LinearesIntervall, b: LinearesIntervall, toleranz: Double): LinearesIntervall? {
        val von = max(a.von, b.von)
        val bis = min(a.bis, b.bis)
        if (von > bis + toleranz) return null
        val linksGeschlossen = a.enthält(von, toleranz) && b.enthält(von, toleranz)
        val rechtsGeschlossen = a.enthält(bis, toleranz) && b.enthält(bis, toleranz)
        if (abs(von - bis) <= toleranz && !(linksGeschlossen && rechtsGeschlossen)) return null
        return LinearesIntervall(von, bis, linksGeschlossen, rechtsGeschlossen)
    }

    private fun subtrahiere(
        a: LinearesIntervall,
        b: LinearesIntervall,
        toleranz: Double,
    ): List<LinearesIntervall> {
        val überlappung = schneide(a, b, toleranz) ?: return listOf(a)
        if (überlappung.von <= a.von + toleranz && überlappung.bis >= a.bis - toleranz &&
            (!a.linksGeschlossen || b.enthält(a.von, toleranz)) && (!a.rechtsGeschlossen || b.enthält(a.bis, toleranz))) return emptyList()
        return buildList {
            if (a.von < überlappung.von - toleranz || abs(a.von - überlappung.von) <= toleranz && a.linksGeschlossen && !b.enthält(a.von, toleranz)) {
                add(LinearesIntervall(a.von, überlappung.von, a.linksGeschlossen, a.enthält(überlappung.von, toleranz) && !b.enthält(überlappung.von, toleranz)))
            }
            if (überlappung.bis < a.bis - toleranz || abs(überlappung.bis - a.bis) <= toleranz && a.rechtsGeschlossen && !b.enthält(a.bis, toleranz)) {
                add(LinearesIntervall(überlappung.bis, a.bis, a.enthält(überlappung.bis, toleranz) && !b.enthält(überlappung.bis, toleranz), a.rechtsGeschlossen))
            }
        }
    }

    private fun subtrahierePunkt(a: LinearesIntervall, punkt: Double, toleranz: Double): List<LinearesIntervall> {
        if (!a.enthält(punkt, toleranz)) return listOf(a)
        return buildList {
            if (punkt > a.von + toleranz) add(LinearesIntervall(a.von, punkt, a.linksGeschlossen, false))
            if (punkt < a.bis - toleranz) add(LinearesIntervall(punkt, a.bis, false, a.rechtsGeschlossen))
        }
    }

    private fun verschmelzbar(a: LinearesIntervall, b: LinearesIntervall, toleranz: Double): Boolean =
        b.von < a.bis - toleranz || abs(b.von - a.bis) <= toleranz && (a.rechtsGeschlossen || b.linksGeschlossen)

    private fun verschmelze(a: LinearesIntervall, b: LinearesIntervall, toleranz: Double): LinearesIntervall {
        val bis = max(a.bis, b.bis)
        val rechtsGeschlossen = when {
            b.bis > a.bis + toleranz -> b.rechtsGeschlossen
            a.bis > b.bis + toleranz -> a.rechtsGeschlossen
            else -> a.rechtsGeschlossen || b.rechtsGeschlossen
        }
        return LinearesIntervall(a.von, bis, a.linksGeschlossen, rechtsGeschlossen)
    }

    private fun LineareMenge.imFenster(minimum: Double, maximum: Double, toleranz: Double): LineareMenge {
        val sichtbarePunkte = punkte.filterTo(linkedSetOf()) { it >= minimum - toleranz && it <= maximum + toleranz }
        val sichtbareIntervalle = intervalle.mapNotNull { intervall ->
            if (intervall.bis < minimum - toleranz || intervall.von > maximum + toleranz) return@mapNotNull null
            val von = max(intervall.von, minimum)
            val bis = min(intervall.bis, maximum)
            val linksRand = intervall.von < minimum - toleranz
            val rechtsRand = intervall.bis > maximum + toleranz
            LinearesIntervall(
                von,
                bis,
                linksGeschlossen = if (linksRand) true else intervall.linksGeschlossen,
                rechtsGeschlossen = if (rechtsRand) true else intervall.rechtsGeschlossen,
                linksAmFensterrand = linksRand,
                rechtsAmFensterrand = rechtsRand,
            )
        }
        return copy(
            punkte = sichtbarePunkte,
            intervalle = sichtbareIntervalle,
            istMathematischLeer = istMathematischLeer,
        )
    }

    private fun LineareMenge.enthält(wert: Double, toleranz: Double): Boolean =
        punkte.any { abs(it - wert) <= toleranz } || intervalle.any { it.enthält(wert, toleranz) }

    private fun LinearesIntervall.enthält(wert: Double, toleranz: Double): Boolean {
        val links = wert > von + toleranz || abs(wert - von) <= toleranz && linksGeschlossen
        val rechts = wert < bis - toleranz || abs(wert - bis) <= toleranz && rechtsGeschlossen
        return links && rechts
    }

    private fun numerisch(ausdruck: ZahlAusdruck): Double? =
        (NumerischerAuswerter.wert(ausdruck) as? NumerischesErgebnis.Wert<Double>)?.wert

    private fun erfolg(menge: LineareMenge) = LinearesErgebnis.Erfolgreich(menge)
    private fun fehler(grund: String) = LinearesErgebnis.Fehler(grund)

    private sealed interface LinearesErgebnis {
        data class Erfolgreich(val menge: LineareMenge) : LinearesErgebnis
        data class Fehler(val grund: String) : LinearesErgebnis
    }

    private data class LineareMenge(
        val punkte: Set<Double> = emptySet(),
        val intervalle: List<LinearesIntervall> = emptyList(),
        val hinweise: List<String> = emptyList(),
        val istMathematischLeer: Boolean = false,
    )

    private data class LinearesIntervall(
        val von: Double,
        val bis: Double,
        val linksGeschlossen: Boolean,
        val rechtsGeschlossen: Boolean,
        val linksAmFensterrand: Boolean = false,
        val rechtsAmFensterrand: Boolean = false,
    )
}
