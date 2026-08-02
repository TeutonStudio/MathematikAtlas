package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.PI
import kotlin.math.abs

/** Geordnete Seiten- und Winkelangaben eines Dreiecks. Seite a liegt dem Winkel alpha gegenüber. */
data class DreiecksWerte(
    val a: ZahlAusdruck? = null,
    val b: ZahlAusdruck? = null,
    val c: ZahlAusdruck? = null,
    val alpha: ZahlAusdruck? = null,
    val beta: ZahlAusdruck? = null,
    val gamma: ZahlAusdruck? = null,
) {
    val seiten: List<ZahlAusdruck?> get() = listOf(a, b, c)
    val winkel: List<ZahlAusdruck?> get() = listOf(alpha, beta, gamma)
    val istVollständig: Boolean get() = seiten.all { it != null } && winkel.all { it != null }

    fun seite(index: Int): ZahlAusdruck? = seiten[index]
    fun winkel(index: Int): ZahlAusdruck? = winkel[index]

    fun mitSeiten(werte: List<ZahlAusdruck?>) = copy(a = werte[0], b = werte[1], c = werte[2])
    fun mitWinkeln(werte: List<ZahlAusdruck?>) = copy(alpha = werte[0], beta = werte[1], gamma = werte[2])

    fun gemeinsameWerte(andere: DreiecksWerte): DreiecksWerte = DreiecksWerte(
        a = gemeinsam(a, andere.a),
        b = gemeinsam(b, andere.b),
        c = gemeinsam(c, andere.c),
        alpha = gemeinsam(alpha, andere.alpha),
        beta = gemeinsam(beta, andere.beta),
        gamma = gemeinsam(gamma, andere.gamma),
    )
}

sealed interface DreiecksLösung {
    val werte: DreiecksWerte

    data class Partiell(
        override val werte: DreiecksWerte,
        val hinweis: String = "Nicht genügend Angaben für weitere Dreiecksgrößen.",
    ) : DreiecksLösung

    data class Vollständig(
        override val werte: DreiecksWerte,
        val dreieck: GeometrieDreieck,
        val annahmen: Set<Aussage> = emptySet(),
    ) : DreiecksLösung

    data class Mehrdeutig(
        override val werte: DreiecksWerte,
        val kandidaten: List<DreiecksWerte>,
    ) : DreiecksLösung

    data class Ungültig(
        val grund: String,
        override val werte: DreiecksWerte = DreiecksWerte(),
    ) : DreiecksLösung

    data class Unentscheidbar(
        override val werte: DreiecksWerte,
        val grund: String,
        val annahmen: Set<Aussage> = emptySet(),
    ) : DreiecksLösung
}

/**
 * Ein vollständig bestimmtes Dreieck mit reproduzierbarer lokaler Realisierung.
 * Die Punkte repräsentieren die Kongruenzklasse, keine globale Lage.
 */
data class GeometrieDreieck(
    val a: ZahlAusdruck,
    val b: ZahlAusdruck,
    val c: ZahlAusdruck,
    val alpha: ZahlAusdruck,
    val beta: ZahlAusdruck,
    val gamma: ZahlAusdruck,
    override val raum: EuklidischerRaum = EuklidischerRaum("Dreiecksebene", 2),
) : GeometrischerAusdruck {
    val punktA: GeometriePunkt = GeometriePunkt(
        "A",
        raum,
        Tupel(listOf(RationaleZahl.Null, RationaleZahl.Null)),
    )
    val punktB: GeometriePunkt = GeometriePunkt(
        "B",
        raum,
        Tupel(listOf(c, RationaleZahl.Null)),
    )

    // b*cos(alpha) = (b²+c²-a²)/(2c); die äquivalente Form vereinfacht rationale Beispiele besser.
    private val cX: ZahlAusdruck = vereinfache(
        Division(
            addition(
                Potenz(b, RationaleZahl.von(2)),
                Potenz(c, RationaleZahl.von(2)),
                negation(Potenz(a, RationaleZahl.von(2))),
            ),
            multiplikation(RationaleZahl.von(2), c),
        ),
    )
    private val cY: ZahlAusdruck = wurzel(
        subtraktion(Potenz(b, RationaleZahl.von(2)), Potenz(cX, RationaleZahl.von(2))),
    )

    val punktC: GeometriePunkt = GeometriePunkt("C", raum, Tupel(listOf(cX, cY)))
    val polygon: GeometriePolygon = GeometriePolygon(listOf(punktA, punktB, punktC))

    override fun zuLatex(): String = "\\triangle ABC"
}

/** Deterministischer, UI-unabhängiger Dreieckslöser. */
fun löseDreieck(angaben: DreiecksWerte): DreiecksLösung {
    val partiell = ergänzeWinkelsumme(angaben)
    prüfeDirekteWidersprüche(partiell)?.let { return DreiecksLösung.Ungültig(it, partiell) }

    val kandidaten = buildList {
        kandidatenAusSSS(partiell)?.let(::add)
        kandidatenAusSAS(partiell).forEach(::add)
        kandidatenAusWinkelnUndSeite(partiell).forEach(::add)
        kandidatenAusSSA(partiell).forEach(::add)
    }
        .mapNotNull { kandidat -> kandidat.takeIf(::istNumerischGültig) }
        .filter { passtZuAngaben(it, angaben) }
        .fold(mutableListOf<DreiecksWerte>()) { eindeutig, kandidat ->
            if (eindeutig.none { dreieckeGleich(it, kandidat) }) eindeutig += kandidat
            eindeutig
        }

    if (kandidaten.size > 1) {
        val gemeinsam = kandidaten.drop(1).fold(kandidaten.first()) { links, rechts -> links.gemeinsameWerte(rechts) }
        return DreiecksLösung.Mehrdeutig(überlagere(gemeinsam, partiell), kandidaten)
    }

    val kandidat = kandidaten.singleOrNull()
    if (kandidat != null) {
        val vollständig = überlagere(kandidat, angaben)
        if (!vollständig.istVollständig) {
            return DreiecksLösung.Unentscheidbar(partiell, "Die Angaben bestimmen das Dreieck nicht nachweisbar eindeutig.")
        }
        val annahmen = notwendigeAnnahmen(vollständig)
        return runCatching {
            DreiecksLösung.Vollständig(
                vollständig,
                GeometrieDreieck(
                    vollständig.a!!,
                    vollständig.b!!,
                    vollständig.c!!,
                    vollständig.alpha!!,
                    vollständig.beta!!,
                    vollständig.gamma!!,
                ),
                annahmen,
            )
        }.getOrElse {
            DreiecksLösung.Unentscheidbar(
                vollständig,
                it.message ?: "Die geometrische Realisierung ist symbolisch nicht nachweisbar.",
                annahmen,
            )
        }
    }

    val hatBestimmendeAngaben = partiell.seiten.count { it != null } >= 2 && partiell.winkel.any { it != null } ||
        partiell.winkel.count { it != null } >= 2 && partiell.seiten.any { it != null } ||
        partiell.seiten.all { it != null }
    return if (hatBestimmendeAngaben) {
        DreiecksLösung.Ungültig("Die verbundenen Angaben bestimmen kein gültiges Dreieck.", partiell)
    } else {
        val hinweis = if (partiell.winkel.all { it != null } && partiell.seiten.all { it == null }) {
            "Die Winkel bestimmen nur die Ähnlichkeitsklasse; für den Maßstab fehlt eine Seitenlänge."
        } else {
            "Nicht genügend Angaben für weitere Dreiecksgrößen."
        }
        DreiecksLösung.Partiell(partiell, hinweis)
    }
}

fun DreiecksWerte.notwendigeDreiecksAnnahmen(): Set<Aussage> = notwendigeAnnahmen(this)

private fun ergänzeWinkelsumme(werte: DreiecksWerte): DreiecksWerte {
    val winkel = werte.winkel.toMutableList()
    if (winkel.count { it == null } == 1) {
        val fehlend = winkel.indexOfFirst { it == null }
        val bekannte = winkel.filterNotNull()
        winkel[fehlend] = vereinfache(subtraktion(Pi, addition(bekannte)))
    }
    return werte.mitWinkeln(winkel)
}

private fun kandidatenAusSSS(werte: DreiecksWerte): DreiecksWerte? {
    val seiten = werte.seiten.map { it ?: return null }
    return runCatching { ausDreiSeiten(seiten) }.getOrNull()
}

private fun kandidatenAusSAS(werte: DreiecksWerte): List<DreiecksWerte> = buildList {
    for (winkelIndex in 0..2) {
        val winkel = werte.winkel(winkelIndex) ?: continue
        val links = (winkelIndex + 1) % 3
        val rechts = (winkelIndex + 2) % 3
        val seiteLinks = werte.seite(links) ?: continue
        val seiteRechts = werte.seite(rechts) ?: continue
        val gegenüber = runCatching {
            wurzel(
                subtraktion(
                    addition(
                        Potenz(seiteLinks, RationaleZahl.von(2)),
                        Potenz(seiteRechts, RationaleZahl.von(2)),
                    ),
                    multiplikation(
                        RationaleZahl.von(2),
                        seiteLinks,
                        seiteRechts,
                        Cosinus(winkel),
                    ),
                ),
            )
        }.getOrNull() ?: continue
        val seiten = werte.seiten.toMutableList().also { it[winkelIndex] = gegenüber }
        runCatching { ausDreiSeiten(seiten.map { it!! }) }.getOrNull()?.let(::add)
    }
}

private fun kandidatenAusWinkelnUndSeite(werte: DreiecksWerte): List<DreiecksWerte> {
    val mitSumme = ergänzeWinkelsumme(werte)
    if (mitSumme.winkel.any { it == null }) return emptyList()
    val referenz = mitSumme.seiten.indexOfFirst { it != null }
    if (referenz < 0) return emptyList()
    val winkel = mitSumme.winkel.map { it!! }
    val referenzSeite = mitSumme.seite(referenz)!!
    val seiten = List(3) { index ->
        if (index == referenz) referenzSeite
        else vereinfache(
            Division(
                multiplikation(referenzSeite, Sinus(winkel[index])),
                Sinus(winkel[referenz]),
            ),
        )
    }
    return listOf(DreiecksWerte(seiten[0], seiten[1], seiten[2], winkel[0], winkel[1], winkel[2]))
}

private fun kandidatenAusSSA(werte: DreiecksWerte): List<DreiecksWerte> = buildList {
    for (bekannterWinkel in 0..2) {
        val winkelI = werte.winkel(bekannterWinkel) ?: continue
        val seiteI = werte.seite(bekannterWinkel) ?: continue
        for (zweiteSeite in 0..2) {
            if (zweiteSeite == bekannterWinkel) continue
            val seiteJ = werte.seite(zweiteSeite) ?: continue
            if (werte.winkel(zweiteSeite) != null) continue
            val argument = vereinfache(Division(multiplikation(seiteJ, Sinus(winkelI)), seiteI))
            val erster = runCatching { arcSinus(argument) }.getOrNull() ?: continue
            val winkelKandidaten = listOf(erster, vereinfache(subtraktion(Pi, erster)))
            for (winkelJ in winkelKandidaten) {
                val dritter = 3 - bekannterWinkel - zweiteSeite
                val winkelK = vereinfache(subtraktion(Pi, addition(winkelI, winkelJ)))
                val winkel = werte.winkel.toMutableList().also {
                    it[bekannterWinkel] = winkelI
                    it[zweiteSeite] = winkelJ
                    it[dritter] = winkelK
                }
                val seiten = werte.seiten.toMutableList().also {
                    it[bekannterWinkel] = seiteI
                    it[zweiteSeite] = seiteJ
                    it[dritter] = vereinfache(
                        Division(multiplikation(seiteI, Sinus(winkelK)), Sinus(winkelI)),
                    )
                }
                add(DreiecksWerte(seiten[0], seiten[1], seiten[2], winkel[0], winkel[1], winkel[2]))
            }
        }
    }
}

private fun ausDreiSeiten(seiten: List<ZahlAusdruck>): DreiecksWerte {
    fun winkel(gegenüber: Int): ZahlAusdruck {
        val links = seiten[(gegenüber + 1) % 3]
        val rechts = seiten[(gegenüber + 2) % 3]
        val argument = vereinfache(
            Division(
                addition(
                    Potenz(links, RationaleZahl.von(2)),
                    Potenz(rechts, RationaleZahl.von(2)),
                    negation(Potenz(seiten[gegenüber], RationaleZahl.von(2))),
                ),
                multiplikation(RationaleZahl.von(2), links, rechts),
            ),
        )
        return arcCosinus(argument)
    }
    val winkel = List(3, ::winkel)
    return DreiecksWerte(seiten[0], seiten[1], seiten[2], winkel[0], winkel[1], winkel[2])
}

private fun prüfeDirekteWidersprüche(werte: DreiecksWerte): String? {
    werte.seiten.forEach { seite ->
        val numerisch = seite?.numerisch() ?: return@forEach
        if (numerisch <= 0.0) return "Seitenlängen müssen positiv und reell sein."
    }
    werte.winkel.forEach { winkel ->
        val numerisch = winkel?.numerisch() ?: return@forEach
        if (numerisch <= 0.0 || numerisch >= PI) return "Winkel müssen zwischen 0 und π liegen."
    }
    val winkel = werte.winkel.map { it?.numerisch() }
    if (winkel.all { it != null } && abs(winkel.filterNotNull().sum() - PI) > TOLERANZ) {
        return "Die verbundenen Winkel verletzen die Winkelsumme α+β+γ=π."
    }
    val seiten = werte.seiten.map { it?.numerisch() }
    if (seiten.all { it != null }) {
        val s = seiten.filterNotNull()
        if (s[0] + s[1] <= s[2] + TOLERANZ || s[0] + s[2] <= s[1] + TOLERANZ || s[1] + s[2] <= s[0] + TOLERANZ) {
            return "Die Seiten verletzen die Dreiecksungleichung."
        }
    }
    return null
}

private fun istNumerischGültig(werte: DreiecksWerte): Boolean = prüfeDirekteWidersprüche(werte) == null

private fun passtZuAngaben(kandidat: DreiecksWerte, angaben: DreiecksWerte): Boolean {
    val kandidatWerte = kandidat.seiten + kandidat.winkel
    val vorgaben = angaben.seiten + angaben.winkel
    return vorgaben.indices.all { index ->
        val vorgabe = vorgaben[index] ?: return@all true
        äquivalent(kandidatWerte[index], vorgabe)
    }
}

private fun dreieckeGleich(links: DreiecksWerte, rechts: DreiecksWerte): Boolean =
    (links.seiten + links.winkel).zip(rechts.seiten + rechts.winkel).all { (a, b) -> äquivalent(a, b) }

private fun äquivalent(links: ZahlAusdruck?, rechts: ZahlAusdruck?): Boolean {
    if (links == null || rechts == null) return links == rechts
    if (links == rechts) return true
    val l = links.numerisch()
    val r = rechts.numerisch()
    return l == null || r == null || abs(l - r) <= TOLERANZ
}

private fun gemeinsam(links: ZahlAusdruck?, rechts: ZahlAusdruck?): ZahlAusdruck? =
    links?.takeIf { rechts != null && äquivalent(links, rechts) }

private fun überlagere(basis: DreiecksWerte, vorrang: DreiecksWerte): DreiecksWerte = DreiecksWerte(
    a = vorrang.a ?: basis.a,
    b = vorrang.b ?: basis.b,
    c = vorrang.c ?: basis.c,
    alpha = vorrang.alpha ?: basis.alpha,
    beta = vorrang.beta ?: basis.beta,
    gamma = vorrang.gamma ?: basis.gamma,
)

private fun notwendigeAnnahmen(werte: DreiecksWerte): Set<Aussage> = buildSet {
    werte.seiten.filterNotNull().forEach { seite ->
        if (seite.numerisch() == null) add(Vergleich(RationaleZahl.Null, VergleichsArt.Kleiner, seite))
    }
    werte.winkel.filterNotNull().forEach { winkel ->
        if (winkel.numerisch() == null) {
            add(Vergleich(RationaleZahl.Null, VergleichsArt.Kleiner, winkel))
            add(Vergleich(winkel, VergleichsArt.Kleiner, Pi))
        }
    }
    if (werte.winkel.all { it != null }) {
        add(Gleichheit(addition(werte.winkel.filterNotNull()), Pi))
    }
    if (werte.seiten.all { it != null }) {
        val s = werte.seiten.filterNotNull()
        add(Vergleich(s[0], VergleichsArt.Kleiner, addition(s[1], s[2])))
        add(Vergleich(s[1], VergleichsArt.Kleiner, addition(s[0], s[2])))
        add(Vergleich(s[2], VergleichsArt.Kleiner, addition(s[0], s[1])))
    }
}

private fun ZahlAusdruck.numerisch(): Double? = when (val ergebnis = NumerischerAuswerter.wert(this)) {
    is NumerischesErgebnis.Wert -> ergebnis.wert
    is NumerischesErgebnis.Fehler -> null
}

private const val TOLERANZ = 1e-7
