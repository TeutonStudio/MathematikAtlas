package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigDecimal
import java.math.BigInteger

/** Der exakte rationale Wert der endlichen binären Gleitkommazahl, keine Rundung. */
fun rationaleKoordinate(wert: Double): RationaleZahl {
    require(wert.isFinite()) { "Eine Koordinate muss endlich sein." }
    val dezimal = BigDecimal(wert)
    return if (dezimal.scale() >= 0) RationaleZahl.von(dezimal.unscaledValue(), BigInteger.TEN.pow(dezimal.scale()))
    else RationaleZahl.von(dezimal.unscaledValue() * BigInteger.TEN.pow(-dezimal.scale()))
}

sealed class ReelleGrenze : Comparable<ReelleGrenze> {
    data object MinusUnendlich : ReelleGrenze()
    data class Endlich(val wert: RationaleZahl) : ReelleGrenze()
    data object PlusUnendlich : ReelleGrenze()
    final override fun compareTo(other: ReelleGrenze): Int = when {
        this == other -> 0
        this == MinusUnendlich || other == PlusUnendlich -> -1
        this == PlusUnendlich || other == MinusUnendlich -> 1
        else -> (this as Endlich).wert.compareTo((other as Endlich).wert)
    }
}

/** Entartete geschlossene Segmente sind Einzelpunkte, Unendlichkeiten keine Elemente. */
data class ExaktesSegment(
    val von: ReelleGrenze,
    val bis: ReelleGrenze,
    val linksGeschlossen: Boolean,
    val rechtsGeschlossen: Boolean,
) {
    init {
        require(von is ReelleGrenze.Endlich || !linksGeschlossen)
        require(bis is ReelleGrenze.Endlich || !rechtsGeschlossen)
    }
    val leer: Boolean get() = von > bis || von == bis && !(linksGeschlossen && rechtsGeschlossen)
    fun enthält(wert: ReelleGrenze): Boolean = wert is ReelleGrenze.Endlich &&
        (wert > von || wert == von && linksGeschlossen) && (wert < bis || wert == bis && rechtsGeschlossen)
    fun schnitt(b: ExaktesSegment): ExaktesSegment? {
        val links = maxOf(von, b.von)
        val rechts = minOf(bis, b.bis)
        return ExaktesSegment(links, rechts, enthält(links) && b.enthält(links), enthält(rechts) && b.enthält(rechts)).takeUnless { it.leer }
    }
    fun ohne(b: ExaktesSegment): List<ExaktesSegment> {
        val gemeinsam = schnitt(b) ?: return listOf(this)
        return listOf(
            ExaktesSegment(von, gemeinsam.von, linksGeschlossen, enthält(gemeinsam.von) && !b.enthält(gemeinsam.von)),
            ExaktesSegment(gemeinsam.bis, bis, enthält(gemeinsam.bis) && !b.enthält(gemeinsam.bis), rechtsGeschlossen),
        ).filterNot { it.leer }
    }
}

data class ExakteReelleMenge(val segmente: List<ExaktesSegment>, val vollständig: Boolean = true) {
    fun vereinigt(b: ExakteReelleMenge) = ExakteReelleMenge(kanonischeSegmente(segmente + b.segmente), vollständig && b.vollständig)
    fun geschnitten(b: ExakteReelleMenge) = ExakteReelleMenge(kanonischeSegmente(segmente.flatMap { a -> b.segmente.mapNotNull(a::schnitt) }), vollständig && b.vollständig)
    fun ohne(b: ExakteReelleMenge): ExakteReelleMenge {
        var rest = segmente
        b.segmente.forEach { abzug -> rest = rest.flatMap { it.ohne(abzug) } }
        return ExakteReelleMenge(kanonischeSegmente(rest), vollständig && b.vollständig)
    }
}

private fun kanonischeSegmente(segmente: List<ExaktesSegment>): List<ExaktesSegment> {
    val ergebnis = mutableListOf<ExaktesSegment>()
    for (b in segmente.filterNot { it.leer }.sortedWith(compareBy<ExaktesSegment> { it.von }.thenByDescending { it.linksGeschlossen })) {
        val a = ergebnis.lastOrNull()
        if (a == null || b.von > a.bis || b.von == a.bis && !(a.rechtsGeschlossen || b.linksGeschlossen)) {
            ergebnis += b
        } else {
            val rechtsGeschlossen = when {
                b.bis > a.bis -> b.rechtsGeschlossen
                a.bis > b.bis -> a.rechtsGeschlossen
                else -> a.rechtsGeschlossen || b.rechtsGeschlossen
            }
            ergebnis[ergebnis.lastIndex] = ExaktesSegment(a.von, maxOf(a.bis, b.bis), a.linksGeschlossen, rechtsGeschlossen)
        }
    }
    return ergebnis
}

sealed interface ExakteMengenNormalisierung {
    data class Normalisiert(val menge: ExakteReelleMenge) : ExakteMengenNormalisierung
    data class Offen(val grund: String) : ExakteMengenNormalisierung
}

/** Nur reelle skalare Träger; eine Koordinatenabbildung darf vor Mengenalgebra nichts identifizieren. */
fun normalisiereExakteReelleMenge(
    menge: MengenAusdruck,
    fensterLinks: RationaleZahl,
    fensterRechts: RationaleZahl,
    budget: Int,
): ExakteMengenNormalisierung {
    require(fensterLinks < fensterRechts && budget > 0)
    var verbleibend = budget
    fun punkt(x: RationaleZahl) = ExaktesSegment(ReelleGrenze.Endlich(x), ReelleGrenze.Endlich(x), true, true)
    fun ganz(zahl: RationaleZahl, oben: Boolean): BigInteger {
        val (q, rest) = zahl.zähler.divideAndRemainder(zahl.nenner)
        return q + if (oben && rest.signum() > 0) BigInteger.ONE else if (!oben && rest.signum() < 0) -BigInteger.ONE else BigInteger.ZERO
    }
    fun normalisiere(m: MengenAusdruck): ExakteReelleMenge? {
        if (--verbleibend < 0) return null
        return when (m) {
            LeereMenge -> ExakteReelleMenge(emptyList())
            ReelleZahlen -> ExakteReelleMenge(listOf(ExaktesSegment(ReelleGrenze.MinusUnendlich, ReelleGrenze.PlusUnendlich, false, false)))
            GanzeZahlen, NatürlicheZahlen -> {
                val start = ganz(fensterLinks, true).let { if (m == NatürlicheZahlen) it.max(BigInteger.ONE) else it }
                val ende = ganz(fensterRechts, false)
                val anzahl = (ende - start + BigInteger.ONE).max(BigInteger.ZERO)
                if (anzahl > BigInteger.valueOf(verbleibend.toLong())) return null
                verbleibend -= anzahl.toInt()
                ExakteReelleMenge(List(anzahl.toInt()) { punkt(RationaleZahl.von(start + BigInteger.valueOf(it.toLong()))) }, vollständig = false)
            }
            is EndlicheMenge -> {
                if (m.elemente.size > verbleibend) return null
                verbleibend -= m.elemente.size
                val werte = m.elemente.map { (it as? ZahlAusdruck)?.let { z -> vereinfache(z) } as? RationaleZahl ?: return null }
                ExakteReelleMenge(kanonischeSegmente(werte.map(::punkt)))
            }
            is ReellesIntervall -> {
                val a = vereinfache(m.links) as? RationaleZahl ?: return null
                val b = vereinfache(m.rechts) as? RationaleZahl ?: return null
                ExakteReelleMenge(listOf(ExaktesSegment(ReelleGrenze.Endlich(a), ReelleGrenze.Endlich(b), !m.linksOffen, !m.rechtsOffen)).filterNot { it.leer })
            }
            is Vereinigung, is Schnitt -> {
                val mengen = if (m is Vereinigung) m.mengen else (m as Schnitt).mengen
                if (mengen.isEmpty()) return if (m is Vereinigung) ExakteReelleMenge(emptyList()) else (m as Schnitt).grundMenge?.let { normalisiere(it) }
                var result = normalisiere(mengen.first()) ?: return null
                for (teil in mengen.drop(1)) {
                    val b = normalisiere(teil) ?: return null
                    val kosten = result.segmente.size.toLong() * b.segmente.size.toLong()
                    if (kosten > verbleibend) return null
                    verbleibend -= kosten.toInt()
                    result = if (m is Vereinigung) result.vereinigt(b) else result.geschnitten(b)
                }
                result
            }
            is MengenDifferenz, is SymmetrischeDifferenz -> {
                val links = if (m is MengenDifferenz) m.links else (m as SymmetrischeDifferenz).links
                val rechts = if (m is MengenDifferenz) m.rechts else (m as SymmetrischeDifferenz).rechts
                val a = normalisiere(links) ?: return null
                val b = normalisiere(rechts) ?: return null
                val kosten = 2L * (a.segmente.size + b.segmente.size + 1L) * (b.segmente.size + a.segmente.size + 1L)
                if (kosten > verbleibend) return null
                verbleibend -= kosten.toInt()
                if (m is MengenDifferenz) a.ohne(b) else a.ohne(b).vereinigt(b.ohne(a))
            }
            is MengenFallAusdruck -> when (m.aussage.entscheide().wahrheitswert) {
                Wahrheitswert.Wahr -> normalisiere(m.wahr)
                Wahrheitswert.Lüge -> normalisiere(m.lüge)
                null -> null
            }
            else -> null
        }
    }
    val result = normalisiere(menge)
    return result?.let { ExakteMengenNormalisierung.Normalisiert(it) }
        ?: ExakteMengenNormalisierung.Offen(if (verbleibend <= 0) "Budget der exakten Normalisierung ausgeschöpft." else "Keine exakte rationale Normalisierung im verfügbaren Budget; Mitgliedschaft bleibt zu prüfen.")
}
