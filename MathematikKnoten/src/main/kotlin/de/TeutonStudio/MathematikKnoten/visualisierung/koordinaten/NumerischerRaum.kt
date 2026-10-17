package de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten

import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Geordnete reelle Achsen (oder ausdrücklich endliche Argumentwerte) mit strukturtreuer Rückverpackung. */
internal sealed interface NumerischerRaum {
    val blätter: List<MengenAusdruck>
    fun packe(werte: Iterator<MathematischesObjekt>): MathematischesObjekt
    fun bedingung(element: MathematischesObjekt): Aussage

    data class Blatt(val menge: MengenAusdruck) : NumerischerRaum {
        override val blätter = listOf(menge)
        override fun packe(werte: Iterator<MathematischesObjekt>) = werte.next()
        override fun bedingung(element: MathematischesObjekt): Aussage = ElementBeziehung(element, menge)
    }

    class Produkt(
        val kinder: List<NumerischerRaum>,
        val baue: (List<MathematischesObjekt>) -> MathematischesObjekt,
        val zerlege: (MathematischesObjekt) -> List<MathematischesObjekt>?,
        val zusätzlicheBedingung: ((MathematischesObjekt) -> Aussage)? = null,
    ) : NumerischerRaum {
        override val blätter = kinder.flatMap { it.blätter }
        override fun packe(werte: Iterator<MathematischesObjekt>) = baue(kinder.map { it.packe(werte) })
        override fun bedingung(element: MathematischesObjekt): Aussage {
            val teile = zerlege(element)
            require(teile?.size == kinder.size) { "Der Wert besitzt nicht die Form seiner numerischen Grundmenge." }
            return Konjunktion(kinder.zip(teile!!).map { (k, e) -> k.bedingung(e) } + listOfNotNull(zusätzlicheBedingung?.invoke(element)))
        }
    }
}

/** Allocation is bounded across the entire tree, including nested powers and tensors. */
internal fun numerischerRaum(menge: MengenAusdruck, maximum: Int = 4096, endlicheObjekte: Boolean = false): NumerischerRaum? {
    var übrig = maximum
    fun baue(m: MengenAusdruck, tiefe: Int): NumerischerRaum? {
        if (tiefe > 64 || --übrig < 0) return null
        fun produkt(faktoren: List<MengenAusdruck>, bauen: (List<MathematischesObjekt>) -> MathematischesObjekt,
                    zerlegen: (MathematischesObjekt) -> List<MathematischesObjekt>?,
                    bedingung: ((MathematischesObjekt) -> Aussage)? = null): NumerischerRaum? {
            if (faktoren.size > übrig) return null
            val kinder = faktoren.map { baue(it, tiefe + 1) ?: return null }
            return NumerischerRaum.Produkt(kinder, bauen, zerlegen, bedingung)
        }
        fun tensor(dimensionen: List<Int>, basis: MengenAusdruck, matrix: Boolean): NumerischerRaum? {
            var anzahl = 1
            for (d in dimensionen) {
                if (d <= 0 || anzahl > übrig / d) return null
                anzahl *= d
            }
            return produkt(List(anzahl) { basis }, { werte ->
                val zahlen = werte.map { it as ZahlAusdruck }
                if (matrix) Matrix(zahlen.chunked(dimensionen[1])) else Tensor(dimensionen, zahlen)
            }, { wert -> when {
                (matrix || dimensionen.size == 2) && wert is Matrix && wert.zeilenAnzahl == dimensionen[0] && wert.spaltenAnzahl == dimensionen[1] -> wert.zeilen.flatten()
                !matrix && wert is Tensor && wert.dimensionen == dimensionen -> wert.werte
                else -> null
            } })
        }
        return when (m) {
            ReelleZahlen, RationaleZahlen, GanzeZahlen, NatürlicheZahlen,
            NichtnegativeGanzeZahlenSemantik.menge, Primzahlen, is ReellesIntervall -> NumerischerRaum.Blatt(m)
            is MengenDifferenz, is Vereinigung, is Schnitt, is SymmetrischeDifferenz ->
                if (istDiskreteZahlmenge(m)) NumerischerRaum.Blatt(m) else null
            is EndlicheMenge -> if (endlicheObjekte || m.elemente.all { it is RationaleZahl }) NumerischerRaum.Blatt(m) else null
            KomplexeZahlen, GaußscheGanzeZahlen, GaußschePrimzahlen -> {
                val basis = if (m == KomplexeZahlen) ReelleZahlen else GanzeZahlen
                produkt(listOf(basis, basis), { KomplexeZahl(it[0] as ZahlAusdruck, it[1] as ZahlAusdruck) },
                    { (it as? KomplexeZahl)?.let { z -> listOf(z.realteil, z.imaginärteil) } },
                    if (m == GaußschePrimzahlen) ({ e -> ElementBeziehung(e, m) }) else null)
            }
            is Tupelraum, is KartesischesProdukt, is KartesischerPotenzraum -> {
                val faktoren = m.kartesischeFaktorenOderNull(übrig) ?: return null
                produkt(faktoren, ::Tupel, { (it as? Tupel)?.elemente })
            }
            is Vektorraum -> {
                if (m.dimension > übrig) return null
                produkt(List(m.dimension) { m.skalarMenge }, { werte ->
                    val zahlen = werte.map { it as ZahlAusdruck }
                    if (m.orientierung == VektorOrientierung.Zeile) ZeilenVektor(zahlen) else SpaltenVektor(zahlen)
                }, { wert -> when {
                    m.orientierung == VektorOrientierung.Zeile && wert is ZeilenVektor -> wert.werte
                    m.orientierung == VektorOrientierung.Spalte && wert is SpaltenVektor -> wert.werte
                    else -> null
                } })
            }
            is Matrizenraum -> tensor(listOf(m.zeilen, m.spalten), m.skalarMenge, true)
            is Tensorraum -> {
                val dimensionen = m.dimensionen.map { (vereinfache(it) as? RationaleZahl)?.let { n -> if (n.nenner == java.math.BigInteger.ONE && n.zähler.signum() > 0 && n.zähler <= java.math.BigInteger.valueOf(übrig.toLong())) n.zähler.toInt() else null } ?: return null }
                tensor(dimensionen, m.elementMenge, m.matrixForm)
            }
            else -> null
        }
    }
    return baue(menge, 0)
}

/** No invented embedding for quotient spaces, abstract carriers or unresolved shapes. */
internal fun raumInterpretationsHinweis(menge: MengenAusdruck): String? = when (menge) {
    Gradmenge -> "Winkel sind Restklassen modulo 360°. Verwende ausdrücklich eine Vertreterabbildung (Bogenmaß) oder eine Kreisabbildung."
    is ModuloZahlenraum -> "Restklassen modulo ${menge.modul} sind keine reellen Koordinaten. Verwende ausdrücklich eine Abbildung auf Vertreter oder Kreispunkte."
    is Folgenraum -> "Der Folgenraum hat keine feste Koordinatenzahl. Wähle Glieder durch eine ausdrücklich numerische Abbildung aus."
    is Abbildungsmenge -> "Der Funktionenraum besitzt keine kanonischen Raumkoordinaten. Werte Funktionen ausdrücklich an gewählten Stellen aus."
    is Potenzmenge -> "Elemente einer Potenzmenge sind Mengen. Verwende eine ausdrückliche numerische Abbildung ihrer Elemente."
    is KartesischerPotenzraum -> if (numerischerRaum(menge) == null)
        "Die Potenzdimension ist noch nicht konkret oder überschreitet das Koordinatenbudget. Binde die Dimension vor der Darstellung." else null
    is Tensorraum -> if (numerischerRaum(menge) == null)
        "Die Tensor-/Matrixdimensionen sind noch nicht konkret oder überschreiten das Koordinatenbudget. Binde die Dimensionen vor der Darstellung." else null
    is BenannteMenge -> if (numerischerRaum(menge) == null)
        "Für die benannte Menge '${menge.name}' fehlt eine numerische Interpretation. Verwende eine ausdrückliche numerische Abbildung." else null
    else -> null
}

internal fun istDiskreteZahlmenge(menge: MengenAusdruck): Boolean = when (menge) {
    GanzeZahlen, NatürlicheZahlen, NichtnegativeGanzeZahlenSemantik.menge, Primzahlen -> true
    is EndlicheMenge -> menge.elemente.all { it is RationaleZahl }
    is Vereinigung -> menge.mengen.isNotEmpty() && menge.mengen.all(::istDiskreteZahlmenge)
    is Schnitt -> menge.mengen.any(::istDiskreteZahlmenge) || menge.mengen.isEmpty() && menge.grundMenge?.let(::istDiskreteZahlmenge) == true
    is MengenDifferenz -> istDiskreteZahlmenge(menge.links)
    is SymmetrischeDifferenz -> istDiskreteZahlmenge(menge.links) && istDiskreteZahlmenge(menge.rechts)
    else -> false
}

/** Complete integer candidates, never a sparse floating point grid or an unbounded allocation. */
internal fun diskreteAchsenWerte(menge: MengenAusdruck, minimum: Double, maximum: Double, budget: Int): List<MathematischesObjekt>? {
    if (!istDiskreteZahlmenge(menge)) return null
    fun prüfeKandidaten(kandidaten: List<MathematischesObjekt>): List<MathematischesObjekt> = kandidaten.filter { zahl ->
        de.TeutonStudio.MathematikKnoten.visualisierung.sampling.SamplingSitzung.aktuelle()?.prüfe()
        when (ElementBeziehung(zahl, menge).entscheide().wahrheitswert) {
            Wahrheitswert.Wahr -> true
            Wahrheitswert.Lüge -> false
            null -> throw OffeneRaumBedingung("Die Mitgliedschaft eines diskreten Kandidaten ist noch nicht bewiesen.")
        }
    }
    if (menge is Vereinigung || menge is SymmetrischeDifferenz) {
        val teile = if (menge is Vereinigung) menge.mengen else (menge as SymmetrischeDifferenz).let { listOf(it.links, it.rechts) }
        var rest = budget
        val kandidaten = linkedSetOf<MathematischesObjekt>()
        for (teil in teile) {
            val werte = diskreteAchsenWerte(teil, minimum, maximum, rest)!!
            rest -= werte.size
            kandidaten += werte
        }
        return prüfeKandidaten(kandidaten.toList())
    }
    if (menge is Schnitt) {
        val basis = menge.mengen.firstOrNull { it is EndlicheMenge && istDiskreteZahlmenge(it) }
            ?: menge.mengen.firstOrNull(::istDiskreteZahlmenge) ?: menge.grundMenge!!
        return prüfeKandidaten(diskreteAchsenWerte(basis, minimum, maximum, budget)!!)
    }
    if (menge is MengenDifferenz) return prüfeKandidaten(diskreteAchsenWerte(menge.links, minimum, maximum, budget)!!)
    if (menge is EndlicheMenge) {
        require(menge.elemente.size <= budget) { "Die diskrete Achse überschreitet das Samplingbudget." }
        de.TeutonStudio.MathematikKnoten.visualisierung.sampling.samplingSpeicher(menge.elemente.size*64L)
        return menge.elemente.sortedBy {
            de.TeutonStudio.MathematikKnoten.visualisierung.sampling.SamplingSitzung.aktuelle()?.prüfe()
            it.zuLatex()
        }
    }
    val unten = java.math.BigDecimal.valueOf(minimum).setScale(0, java.math.RoundingMode.CEILING).toBigIntegerExact()
    val oben = java.math.BigDecimal.valueOf(maximum).setScale(0, java.math.RoundingMode.FLOOR).toBigIntegerExact()
    val start = when (menge) {
        NatürlicheZahlen -> maxOf(unten, java.math.BigInteger.ONE)
        NichtnegativeGanzeZahlenSemantik.menge -> maxOf(unten, java.math.BigInteger.ZERO)
        Primzahlen -> maxOf(unten, java.math.BigInteger.TWO)
        else -> unten
    }
    val anzahl = (oben - start + java.math.BigInteger.ONE).max(java.math.BigInteger.ZERO)
    require(anzahl <= java.math.BigInteger.valueOf(budget.toLong())) { "Die vollständige diskrete Achse benötigt $anzahl Kandidaten und überschreitet das Rasterbudget von $budget." }
    val sicher = java.math.BigInteger.ONE.shiftLeft(53)
    require(anzahl.signum() == 0 || start >= -sicher && oben <= sicher) { "Ganzzahlige Koordinaten außerhalb ±2⁵³ sind im Renderer nicht exakt unterscheidbar." }
    return prüfeKandidaten(buildList {
        repeat(anzahl.toInt()) {
            de.TeutonStudio.MathematikKnoten.visualisierung.sampling.SamplingSitzung.aktuelle()?.prüfe()
            de.TeutonStudio.MathematikKnoten.visualisierung.sampling.samplingSpeicher(256)
            add(RationaleZahl.von(start + java.math.BigInteger.valueOf(it.toLong())))
        }
    })
}

internal fun NumerischerRaum.hatZusatzBedingung(): Boolean = this is NumerischerRaum.Produkt &&
    (zusätzlicheBedingung != null || kinder.any { it.hatZusatzBedingung() })

internal class OffeneRaumBedingung(grund: String) : IllegalStateException(grund)

/** A safe product carrier for sampling set algebra; membership remains that of the original set. */
internal fun numerischeTrägerAchsen(menge: MengenAusdruck): List<MengenAusdruck>? {
    var rest = 4096
    fun ableiten(m: MengenAusdruck, tiefe: Int): List<MengenAusdruck>? {
        if (--rest < 0 || tiefe > 64) return null
        numerischerRaum(m, maximum = rest)?.let {
            rest -= it.blätter.size
            return it.blätter
        }
        val faktoren = m.kartesischeFaktorenOderNull(rest)
        if (faktoren != null) {
            if (faktoren.size > rest) return null
            return faktoren.flatMap { ableiten(it, tiefe + 1) ?: return null }
        }
        fun gemeinsam(teile: List<MengenAusdruck>, vereinige: Boolean): List<MengenAusdruck>? {
            if (teile.size > rest) return null
            val achsen = teile.mapNotNull { ableiten(it, tiefe + 1) }
            if (rest < 0 || achsen.isEmpty() || vereinige && achsen.size != teile.size || achsen.map { it.size }.distinct().size != 1) return null
            return achsen.first().indices.map { i ->
                val faktoren = achsen.map { it[i] }.distinct()
                if (faktoren.size == 1) faktoren.single() else if (vereinige) Vereinigung(faktoren) else Schnitt(faktoren)
            }
        }
        return when (m) {
            is GefilterteMenge -> ableiten(m.menge, tiefe + 1)
            is MengenDifferenz -> ableiten(m.links, tiefe + 1)
            is Vereinigung -> gemeinsam(m.mengen, true)
            is Schnitt -> if (m.mengen.isEmpty()) m.grundMenge?.let { ableiten(it, tiefe + 1) } else gemeinsam(m.mengen, false)
            is SymmetrischeDifferenz -> gemeinsam(listOf(m.links, m.rechts), true)
            else -> null
        }
    }
    return ableiten(menge, 0)
}

/** A restriction retains the element form of its carrier, not just its coordinate count. */
internal fun numerischeArgumentStruktur(menge: MengenAusdruck, endlicheObjekte: Boolean = false): NumerischerRaum? {
    var rest = 4096
    fun struktur(m: MengenAusdruck, tiefe: Int): NumerischerRaum? {
        if (--rest < 0 || tiefe > 64) return null
        numerischerRaum(m, maximum = rest, endlicheObjekte = endlicheObjekte)?.let { return it }
        val faktoren = m.kartesischeFaktorenOderNull(rest)
        if (faktoren != null) {
            if (faktoren.size > rest) return null
            val kinder = faktoren.map { struktur(it, tiefe + 1) ?: return null }
            return NumerischerRaum.Produkt(kinder, ::Tupel, { (it as? Tupel)?.elemente })
        }
        return when (m) {
            is MengenDifferenz -> struktur(m.links, tiefe + 1)
            is GefilterteMenge -> struktur(m.menge, tiefe + 1)
            is Schnitt -> if (m.mengen.size > rest) null else (m.mengen + listOfNotNull(m.grundMenge)).firstNotNullOfOrNull { struktur(it, tiefe + 1) }
            else -> null
        }
    }
    return struktur(menge, 0)
}
