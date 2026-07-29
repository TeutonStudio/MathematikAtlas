package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.*

internal enum class DreieckStatus { Unzureichend, Eindeutig, Mehrdeutig, Ungültig }
internal data class DreieckLösung(
    val a: Double,
    val b: Double,
    val c: Double,
    val alpha: Double,
    val beta: Double,
    val gamma: Double,
)
internal data class DreieckErgebnis(val status: DreieckStatus, val lösungen: List<DreieckLösung>, val hinweis: String)
internal data class DreieckEingabe(
    val a: Double? = null,
    val b: Double? = null,
    val c: Double? = null,
    val alpha: Double? = null,
    val beta: Double? = null,
    val gamma: Double? = null,
) {
    val seiten get() = listOf(a, b, c)
    val winkel get() = listOf(alpha, beta, gamma)
}

internal fun MathematikAuswerterRegister.registriereDreieckRechner() {
    registriere("mathematik.geometrie.dreieckRechner") { k ->
        fun wert(name: String): Double? = numerischerMethodenWert(k.eingänge[name]?.objekt as? ZahlAusdruck, emptyMap())
        val ergebnis = löseDreieck(DreieckEingabe(
            a = wert("a"), b = wert("b"), c = wert("c"),
            alpha = wert("alpha"), beta = wert("beta"), gamma = wert("gamma"),
        ))
        val legitim = ergebnis.status in setOf(DreieckStatus.Eindeutig, DreieckStatus.Mehrdeutig)
        val ausgaben = linkedMapOf<String, BedingterWert>(
            "gültig" to BedingterWert(WahrheitsKonstante(legitim)),
            "bestimmt" to BedingterWert(WahrheitsKonstante(ergebnis.status == DreieckStatus.Eindeutig)),
            "status" to BedingterWert(AllgemeinerParameter(ergebnis.status.name, "\\mathrm{${ergebnis.status.name.lowercase()}}")),
        )
        ergebnis.lösungen.singleOrNull()?.let { lösung ->
            ausgaben["aWert"] = dreieckWert(lösung.a, "a")
            ausgaben["bWert"] = dreieckWert(lösung.b, "b")
            ausgaben["cWert"] = dreieckWert(lösung.c, "c")
            ausgaben["alphaWert"] = dreieckWert(lösung.alpha, "\\alpha", grad = true)
            ausgaben["betaWert"] = dreieckWert(lösung.beta, "\\beta", grad = true)
            ausgaben["gammaWert"] = dreieckWert(lösung.gamma, "\\gamma", grad = true)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = ausgaben,
            fehler = ergebnis.hinweis.takeIf { ergebnis.status == DreieckStatus.Ungültig },
        )
    }
}

internal fun löseDreieck(e: DreieckEingabe): DreieckErgebnis {
    if (e.seiten.filterNotNull().any { !it.isFinite() || it <= 0.0 }) return ungültig("Seitenlängen müssen positiv und endlich sein.")
    if (e.winkel.filterNotNull().any { !it.isFinite() || it <= 0.0 || it >= 180.0 }) return ungültig("Winkel müssen zwischen 0° und 180° liegen.")
    val bekannte = e.seiten.count { it != null } + e.winkel.count { it != null }
    if (bekannte < 3 || e.seiten.all { it == null }) {
        return DreieckErgebnis(DreieckStatus.Unzureichend, emptyList(), "Mindestens drei unabhängige Werte einschließlich einer Seitenlänge werden benötigt.")
    }

    val kandidaten = mutableListOf<DreieckLösung>()
    if (e.seiten.all { it != null }) sss(e.a!!, e.b!!, e.c!!)?.let(kandidaten::add)
    if (e.winkel.count { it != null } >= 2 && e.seiten.any { it != null }) winkelSeite(e)?.let(kandidaten::add)
    sas(e.a, e.b, e.gamma, fehlende = 2)?.let(kandidaten::add)
    sas(e.a, e.c, e.beta, fehlende = 1)?.let(kandidaten::add)
    sas(e.b, e.c, e.alpha, fehlende = 0)?.let(kandidaten::add)

    for (i in 0..2) {
        val seiteI = e.seiten[i] ?: continue
        val winkelI = e.winkel[i] ?: continue
        for (j in 0..2) if (j != i) {
            val seiteJ = e.seiten[j] ?: continue
            kandidaten += ssa(i, seiteI, winkelI, j, seiteJ)
        }
    }

    val gültige = kandidaten
        .filter(::istDreieck)
        .filter { passtZuEingabe(it, e) }
        .distinctBy { lösung ->
            listOf(lösung.a, lösung.b, lösung.c, lösung.alpha, lösung.beta, lösung.gamma)
                .joinToString("|") { String.format(java.util.Locale.ROOT, "%.7f", it) }
        }

    return when (gültige.size) {
        0 -> ungültig("Die verbundenen Werte ergeben kein konsistentes Dreieck.")
        1 -> DreieckErgebnis(DreieckStatus.Eindeutig, gültige, "Das Dreieck ist eindeutig bestimmt.")
        else -> DreieckErgebnis(DreieckStatus.Mehrdeutig, gültige, "Die SSW-Kombination beschreibt ${gültige.size} mögliche Dreiecke.")
    }
}

private fun sss(a: Double, b: Double, c: Double): DreieckLösung? {
    if (a + b <= c || a + c <= b || b + c <= a) return null
    val alpha = acos(((b * b + c * c - a * a) / (2 * b * c)).coerceIn(-1.0, 1.0)).grad()
    val beta = acos(((a * a + c * c - b * b) / (2 * a * c)).coerceIn(-1.0, 1.0)).grad()
    return DreieckLösung(a, b, c, alpha, beta, 180.0 - alpha - beta)
}

private fun sas(seite1: Double?, seite2: Double?, winkel: Double?, fehlende: Int): DreieckLösung? {
    if (seite1 == null || seite2 == null || winkel == null) return null
    val dritte = sqrt((seite1 * seite1 + seite2 * seite2 - 2 * seite1 * seite2 * cos(winkel.rad())).coerceAtLeast(0.0))
    return when (fehlende) {
        0 -> sss(dritte, seite1, seite2)
        1 -> sss(seite1, dritte, seite2)
        else -> sss(seite1, seite2, dritte)
    }
}

private fun winkelSeite(e: DreieckEingabe): DreieckLösung? {
    val winkel = e.winkel.toMutableList()
    if (winkel.count { it != null } == 2) {
        val fehlt = winkel.indexOfFirst { it == null }
        winkel[fehlt] = 180.0 - winkel.filterNotNull().sum()
    }
    if (winkel.any { it == null } || abs(winkel.filterNotNull().sum() - 180.0) > 1e-6) return null
    val seiten = e.seiten.toMutableList()
    val bekannteSeite = seiten.indexOfFirst { it != null }
    if (bekannteSeite < 0) return null
    val faktor = seiten[bekannteSeite]!! / sin(winkel[bekannteSeite]!!.rad())
    for (i in 0..2) if (seiten[i] == null) seiten[i] = faktor * sin(winkel[i]!!.rad())
    return DreieckLösung(seiten[0]!!, seiten[1]!!, seiten[2]!!, winkel[0]!!, winkel[1]!!, winkel[2]!!)
}

private fun ssa(i: Int, seiteI: Double, winkelI: Double, j: Int, seiteJ: Double): List<DreieckLösung> {
    val sinJ = seiteJ * sin(winkelI.rad()) / seiteI
    if (sinJ !in -1.0..1.0) return emptyList()
    val erster = asin(sinJ).grad()
    return listOf(erster, 180.0 - erster).distinct().filter { winkelI + it < 180.0 - 1e-7 }.mapNotNull { winkelJ ->
        val winkel = MutableList<Double?>(3) { null }
        winkel[i] = winkelI
        winkel[j] = winkelJ
        val k = (0..2).first { it != i && it != j }
        winkel[k] = 180.0 - winkelI - winkelJ
        val faktor = seiteI / sin(winkelI.rad())
        val seiten = MutableList<Double?>(3) { null }
        seiten[i] = seiteI
        seiten[j] = seiteJ
        seiten[k] = faktor * sin(winkel[k]!!.rad())
        DreieckLösung(seiten[0]!!, seiten[1]!!, seiten[2]!!, winkel[0]!!, winkel[1]!!, winkel[2]!!).takeIf(::istDreieck)
    }
}

private fun istDreieck(d: DreieckLösung) =
    d.a > 0 && d.b > 0 && d.c > 0 && d.a + d.b > d.c && d.a + d.c > d.b && d.b + d.c > d.a &&
        d.alpha > 0 && d.beta > 0 && d.gamma > 0 && abs(d.alpha + d.beta + d.gamma - 180.0) < 1e-4

private fun passtZuEingabe(d: DreieckLösung, e: DreieckEingabe): Boolean {
    val soll = listOf(e.a, e.b, e.c, e.alpha, e.beta, e.gamma)
    val ist = listOf(d.a, d.b, d.c, d.alpha, d.beta, d.gamma)
    return soll.indices.all { index ->
        soll[index]?.let { erwartet -> abs(ist[index] - erwartet) <= max(1e-5, abs(erwartet) * 1e-6) } ?: true
    }
}

private fun dreieckWert(wert: Double, symbol: String, grad: Boolean = false) = BedingterWert(
    objekt = RationaleZahl.von((wert * 1_000_000).roundToLong(), 1_000_000),
    zielMenge = ReelleZahlen,
    latexDarstellung = "$symbol = ${String.format(java.util.Locale.ROOT, "%.8g", wert)}${if (grad) "^{\\circ}" else ""}",
)

private fun ungültig(text: String) = DreieckErgebnis(DreieckStatus.Ungültig, emptyList(), text)
private fun Double.rad() = this * Math.PI / 180.0
private fun Double.grad() = this * 180.0 / Math.PI
