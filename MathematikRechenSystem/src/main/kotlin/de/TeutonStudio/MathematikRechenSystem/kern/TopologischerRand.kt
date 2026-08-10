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

/**
 * Exakte Spezialfälle des topologischen Randes; nicht geschlossene Fälle bleiben
 * als strukturierter [TopologischerRand] erhalten statt als Fehler zu enden.
 */
fun topologischerRand(
    menge: MengenAusdruck,
    kontext: TopologischerKontext,
): MengenAusdruck = when (kontext.topologie) {
    TopologieArt.DISKRET -> LeereMenge
    TopologieArt.INDISKRET -> when {
        menge == LeereMenge || menge == kontext.umgebungsraum -> LeereMenge
        else -> kontext.umgebungsraum
    }
    TopologieArt.KANONISCH_REELL -> kanonischReellerRand(menge, kontext)
    TopologieArt.SYMBOLISCH -> TopologischerRand(menge, kontext)
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
    else -> TopologischerRand(menge, kontext)
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
