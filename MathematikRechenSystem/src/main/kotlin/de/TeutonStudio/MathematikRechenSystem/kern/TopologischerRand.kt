package de.TeutonStudio.MathematikRechenSystem.kern

enum class TopologieArt(val persistenzWert: String) {
    KANONISCH_REELL("kanonisch"),
    DISKRET("diskret"),
    INDISKRET("indiskret"),
    SYMBOLISCH("symbolisch"),
    ;

    companion object {
        fun ausPersistenz(wert: String?): TopologieArt = when (wert?.trim()?.lowercase()) {
            null, "", "kanonisch", "kanonisch:r", "r", "reell", "automatisch" -> KANONISCH_REELL
            "diskret" -> DISKRET
            "indiskret", "trivial" -> INDISKRET
            else -> SYMBOLISCH
        }
    }
}

data class TopologischerKontext(
    val umgebungsraum: MengenAusdruck,
    val topologie: TopologieArt,
    val relativ: Boolean = false,
)

data class TopologischerAbschluss(
    val menge: MengenAusdruck,
    val kontext: TopologischerKontext,
) : MengenAusdruck {
    override fun zuLatex(): String = "\\operatorname{cl}_{${kontext.umgebungsraum.zuLatex()}}\\left(${menge.zuLatex()}\\right)"
}

data class TopologischesInneres(
    val menge: MengenAusdruck,
    val kontext: TopologischerKontext,
) : MengenAusdruck {
    override fun zuLatex(): String = "\\operatorname{int}_{${kontext.umgebungsraum.zuLatex()}}\\left(${menge.zuLatex()}\\right)"
}

data class TopologischerRand(
    val menge: MengenAusdruck,
    val kontext: TopologischerKontext,
) : MengenAusdruck {
    override fun zuLatex(): String = if (kontext.umgebungsraum == ReelleZahlen && !kontext.relativ) {
        "\\partial ${menge.zuLatex()}"
    } else {
        "\\partial_{${kontext.umgebungsraum.zuLatex()}} ${menge.zuLatex()}"
    }
}

/** Relatives Komplement im explizit gewählten topologischen Umgebungsraum. */
fun topologischesKomplement(
    menge: MengenAusdruck,
    kontext: TopologischerKontext,
): MengenAusdruck = mengenDifferenz(kontext.umgebungsraum, menge)

/** Wiederverwendbarer Abschlusskern für Rand- und spätere Topologieknoten. */
fun topologischerAbschluss(
    menge: MengenAusdruck,
    kontext: TopologischerKontext,
): MengenAusdruck = when (kontext.topologie) {
    TopologieArt.DISKRET -> menge
    TopologieArt.INDISKRET -> if (menge == LeereMenge) LeereMenge else kontext.umgebungsraum
    TopologieArt.KANONISCH_REELL -> when {
        menge == LeereMenge -> LeereMenge
        menge == kontext.umgebungsraum -> kontext.umgebungsraum
        menge == RationaleZahlen && kontext.umgebungsraum == ReelleZahlen -> ReelleZahlen
        menge is EndlicheMenge -> menge
        menge is ReellesIntervall -> reellesIntervall(
            menge.links,
            linksOffen = false,
            menge.rechts,
            rechtsOffen = false,
        )
        else -> TopologischerAbschluss(menge, kontext)
    }
    TopologieArt.SYMBOLISCH -> TopologischerAbschluss(menge, kontext)
}

/** Wiederverwendbarer Inneres-Kern für Rand- und spätere Topologieknoten. */
fun topologischesInneres(
    menge: MengenAusdruck,
    kontext: TopologischerKontext,
): MengenAusdruck = when (kontext.topologie) {
    TopologieArt.DISKRET -> menge
    TopologieArt.INDISKRET -> if (menge == kontext.umgebungsraum) kontext.umgebungsraum else LeereMenge
    TopologieArt.KANONISCH_REELL -> when {
        menge == LeereMenge -> LeereMenge
        menge == kontext.umgebungsraum -> kontext.umgebungsraum
        menge == RationaleZahlen && kontext.umgebungsraum == ReelleZahlen -> LeereMenge
        menge is EndlicheMenge -> LeereMenge
        menge is ReellesIntervall -> reellesIntervall(
            menge.links,
            linksOffen = true,
            menge.rechts,
            rechtsOffen = true,
        )
        else -> TopologischesInneres(menge, kontext)
    }
    TopologieArt.SYMBOLISCH -> TopologischesInneres(menge, kontext)
}

/**
 * Rand als gemeinsame Topologieoperation. Exakte Spezialfälle werden reduziert;
 * nicht entscheidbare Fälle bleiben strukturiert statt als Fehler zu enden.
 * Abschluss, Inneres und Komplement sind separat verfügbar, damit spätere Knoten
 * denselben mathematischen Kern wiederverwenden.
 */
fun topologischerRand(
    menge: MengenAusdruck,
    kontext: TopologischerKontext,
): MengenAusdruck = when (kontext.topologie) {
    TopologieArt.SYMBOLISCH -> TopologischerRand(menge, kontext)
    TopologieArt.DISKRET -> LeereMenge
    TopologieArt.INDISKRET -> when {
        menge == LeereMenge || menge == kontext.umgebungsraum -> LeereMenge
        else -> kontext.umgebungsraum
    }
    TopologieArt.KANONISCH_REELL -> kanonischReellerRand(menge, kontext)
}

private fun kanonischReellerRand(
    menge: MengenAusdruck,
    kontext: TopologischerKontext,
): MengenAusdruck = when {
    menge == LeereMenge -> LeereMenge
    menge == kontext.umgebungsraum -> LeereMenge
    menge == RationaleZahlen && kontext.umgebungsraum == ReelleZahlen -> ReelleZahlen
    menge is EndlicheMenge -> menge
    menge is ReellesIntervall -> intervallRand(menge, kontext.umgebungsraum)
    else -> {
        val abschluss = topologischerAbschluss(menge, kontext)
        val inneres = topologischesInneres(menge, kontext)
        if (abschluss == inneres) LeereMenge
        else if (abschluss !is TopologischerAbschluss && inneres !is TopologischesInneres) {
            mengenDifferenz(abschluss, inneres)
        } else {
            TopologischerRand(menge, kontext)
        }
    }
}

/** Endpunkte eines Intervalls, mit korrekter relativer Behandlung an Umgebungsraumgrenzen. */
private fun intervallRand(
    menge: ReellesIntervall,
    umgebungsraum: MengenAusdruck,
): MengenAusdruck {
    val grenzen = linkedSetOf<MathematischesObjekt>()
    val raumIntervall = umgebungsraum as? ReellesIntervall

    val linksIstRaumRandUndInnen = raumIntervall != null &&
        menge.links == raumIntervall.links &&
        !menge.linksOffen &&
        !raumIntervall.linksOffen
    val rechtsIstRaumRandUndInnen = raumIntervall != null &&
        menge.rechts == raumIntervall.rechts &&
        !menge.rechtsOffen &&
        !raumIntervall.rechtsOffen

    if (!linksIstRaumRandUndInnen) grenzen += menge.links
    if (!rechtsIstRaumRandUndInnen) grenzen += menge.rechts
    return if (grenzen.isEmpty()) LeereMenge else EndlicheMenge(grenzen)
}
