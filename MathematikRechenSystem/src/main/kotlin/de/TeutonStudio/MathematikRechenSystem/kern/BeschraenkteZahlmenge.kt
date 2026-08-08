package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Kanonische Relation einer Grenze.
 *
 * Rohes `<` wird nicht in erzeugtes LaTeX übernommen: strenge Grenzen werden
 * als `\lt`, eingeschlossene Grenzen unabhängig von `\le` oder `\leq` als
 * kanonisches `\leq` ausgegeben.
 */
enum class GrenzRelation(
    val latex: String,
    val grenzeEnthalten: Boolean,
) {
    KLEINER("\\lt", false),
    KLEINER_GLEICH("\\leq", true),
    ;

    companion object {
        fun ausEnthalten(enthalten: Boolean): GrenzRelation =
            if (enthalten) KLEINER_GLEICH else KLEINER

        fun ausLatex(operator: String): GrenzRelation = when (operator.trim()) {
            "<", "\\lt" -> KLEINER
            "<=", "≤", "\\le", "\\leq" -> KLEINER_GLEICH
            else -> throw IllegalArgumentException("Unbekannte Grenzrelation: $operator")
        }
    }
}

/**
 * Geordneter Zahlbereich mit optionaler unterer und oberer Schranke.
 * Die Darstellung folgt der Aussagenreihenfolge l R x R u.
 */
data class BeschraenkteZahlmenge(
    val traeger: FundamentalerZahlbereich,
    val untereGrenze: ZahlAusdruck? = null,
    val untereGrenzeEnthalten: Boolean = false,
    val obereGrenze: ZahlAusdruck? = null,
    val obereGrenzeEnthalten: Boolean = false,
) : MengenAusdruck {
    init {
        require(traeger.istGeordnet) {
            "Beschränkte Zahlmengen benötigen einen geordneten Zahlbereich; ${traeger.latex} ist nicht geordnet."
        }
        require(untereGrenze != null || !untereGrenzeEnthalten)
        require(obereGrenze != null || !obereGrenzeEnthalten)
    }

    val untereRelation: GrenzRelation
        get() = GrenzRelation.ausEnthalten(untereGrenzeEnthalten)

    val obereRelation: GrenzRelation
        get() = GrenzRelation.ausEnthalten(obereGrenzeEnthalten)

    override fun zuLatex(): String {
        val links = untereGrenze?.let { grenze ->
            "{}^{${grenze.alsGrenzeLatex()}${untereRelation.latex}}"
        }.orEmpty()
        val rechts = obereGrenze?.let { grenze ->
            "^{${obereRelation.latex}${grenze.alsGrenzeLatex()}}"
        }.orEmpty()
        return "$links${traeger.latex}$rechts"
    }
}

private fun ZahlAusdruck.alsGrenzeLatex(): String = when (this) {
    is Addition, is Multiplikation, is Division -> "\\left(${zuLatex()}\\right)"
    else -> zuLatex()
}

fun FundamentalerZahlbereich.alsMenge(): MengenAusdruck = when (this) {
    FundamentalerZahlbereich.NATUERLICH_POSITIV -> NatürlicheZahlen
    FundamentalerZahlbereich.NATUERLICH_MIT_NULL -> NichtnegativeGanzeZahlenSemantik.menge
    FundamentalerZahlbereich.GANZ -> GanzeZahlen
    FundamentalerZahlbereich.RATIONAL -> RationaleZahlen
    FundamentalerZahlbereich.REELL -> ReelleZahlen
    FundamentalerZahlbereich.KOMPLEX -> KomplexeZahlen
    FundamentalerZahlbereich.QUATERNION -> BenannteMenge("Quaternionen", "\\mathbb H")
}

fun MengenAusdruck.fundamentalerZahlbereichOderNull(): FundamentalerZahlbereich? = when (this) {
    NatürlicheZahlen -> FundamentalerZahlbereich.NATUERLICH_POSITIV
    GanzeZahlen -> FundamentalerZahlbereich.GANZ
    RationaleZahlen -> FundamentalerZahlbereich.RATIONAL
    ReelleZahlen -> FundamentalerZahlbereich.REELL
    KomplexeZahlen -> FundamentalerZahlbereich.KOMPLEX
    FundamentalerZahlbereich.QUATERNION.alsMenge() -> FundamentalerZahlbereich.QUATERNION
    NichtnegativeGanzeZahlenSemantik.menge -> FundamentalerZahlbereich.NATUERLICH_MIT_NULL
    else -> null
}

fun beschraenkteZahlmenge(
    traeger: FundamentalerZahlbereich,
    untereGrenze: ZahlAusdruck? = null,
    untereGrenzeEnthalten: Boolean = false,
    obereGrenze: ZahlAusdruck? = null,
    obereGrenzeEnthalten: Boolean = false,
): MengenAusdruck {
    require(traeger.istGeordnet) {
        "Beschränkte Zahlmengen sind für ${traeger.latex} ohne registrierte lineare Ordnung nicht definiert."
    }
    val bereich = traegerGrenzeEinbeziehen(
        BeschraenkteZahlmenge(
            traeger,
            untereGrenze,
            untereGrenzeEnthalten,
            obereGrenze,
            obereGrenzeEnthalten,
        ),
    )
    return normalisiereKonkreteGrenzen(bereich)
}

/** Normalisiert Schnitte mit eingebetteten geordneten Zahlbereichen. */
fun normalisiereZahlmengenSchnitt(
    mengen: Iterable<MengenAusdruck>,
    grundMenge: MengenAusdruck? = null,
): MengenAusdruck {
    val flach = mengen.flatMap { if (it is Schnitt) it.mengen else listOf(it) }
    if (flach.isEmpty()) return grundMenge ?: error("Ein leerer Schnitt benötigt eine Grundmenge.")
    if (flach.any { it == LeereMenge }) return LeereMenge

    val relevante = flach.filter {
        it is BeschraenkteZahlmenge || it.fundamentalerZahlbereichOderNull() != null
    }
    if (relevante.isEmpty()) return schneide(flach, grundMenge)

    val rest = flach - relevante.toSet()
    val traeger = relevante.mapNotNull {
        when (it) {
            is BeschraenkteZahlmenge -> it.traeger
            else -> it.fundamentalerZahlbereichOderNull()
        }
    }
    val zielTraeger = waehleEngstenTraeger(traeger) ?: return Schnitt(flach.distinct(), grundMenge)
    if (relevante.none { it is BeschraenkteZahlmenge }) {
        val normalisiert = zielTraeger.alsMenge()
        return if (rest.isEmpty()) normalisiert else schneide(rest + normalisiert, grundMenge)
    }

    var bereich = BeschraenkteZahlmenge(zielTraeger)
    for (menge in relevante.filterIsInstance<BeschraenkteZahlmenge>()) {
        if (!FundamentaleZahlbereiche.istTeilbereich(zielTraeger, menge.traeger)) {
            return Schnitt(flach.distinct(), grundMenge)
        }
        bereich = vereinigeGrenzenFuerSchnitt(bereich, menge)
            ?: return Schnitt(flach.distinct(), grundMenge)
    }
    val normalisiert = normalisiereKonkreteGrenzen(traegerGrenzeEinbeziehen(bereich))
    if (normalisiert == LeereMenge) return LeereMenge
    return if (rest.isEmpty()) normalisiert else schneide(rest + normalisiert, grundMenge)
}

private fun waehleEngstenTraeger(
    traeger: List<FundamentalerZahlbereich>,
): FundamentalerZahlbereich? = traeger.firstOrNull { kandidat ->
    traeger.all { anderer ->
        FundamentaleZahlbereiche.istTeilbereich(kandidat, anderer) ||
            FundamentaleZahlbereiche.istTeilbereich(anderer, kandidat)
    }
}?.let {
    traeger.first { kandidat -> traeger.all { FundamentaleZahlbereiche.istTeilbereich(kandidat, it) } }
}

private fun vereinigeGrenzenFuerSchnitt(
    links: BeschraenkteZahlmenge,
    rechts: BeschraenkteZahlmenge,
): BeschraenkteZahlmenge? {
    val unten = strengereUntereGrenze(
        links.untereGrenze,
        links.untereGrenzeEnthalten,
        rechts.untereGrenze,
        rechts.untereGrenzeEnthalten,
    ) ?: return null
    val oben = strengereObereGrenze(
        links.obereGrenze,
        links.obereGrenzeEnthalten,
        rechts.obereGrenze,
        rechts.obereGrenzeEnthalten,
    ) ?: return null
    return BeschraenkteZahlmenge(
        links.traeger,
        unten.first,
        unten.second,
        oben.first,
        oben.second,
    )
}

private fun strengereUntereGrenze(
    a: ZahlAusdruck?,
    aEnthalten: Boolean,
    b: ZahlAusdruck?,
    bEnthalten: Boolean,
): Pair<ZahlAusdruck?, Boolean>? = when {
    a == null -> b to bEnthalten
    b == null -> a to aEnthalten
    a == b -> a to (aEnthalten && bEnthalten)
    a is RationaleZahl && b is RationaleZahl -> if (a > b) a to aEnthalten else b to bEnthalten
    else -> null
}

private fun strengereObereGrenze(
    a: ZahlAusdruck?,
    aEnthalten: Boolean,
    b: ZahlAusdruck?,
    bEnthalten: Boolean,
): Pair<ZahlAusdruck?, Boolean>? = when {
    a == null -> b to bEnthalten
    b == null -> a to aEnthalten
    a == b -> a to (aEnthalten && bEnthalten)
    a is RationaleZahl && b is RationaleZahl -> if (a < b) a to aEnthalten else b to bEnthalten
    else -> null
}

private fun traegerGrenzeEinbeziehen(bereich: BeschraenkteZahlmenge): BeschraenkteZahlmenge {
    val minimum = when (bereich.traeger) {
        FundamentalerZahlbereich.NATUERLICH_POSITIV -> RationaleZahl.Eins
        FundamentalerZahlbereich.NATUERLICH_MIT_NULL -> RationaleZahl.Null
        else -> null
    } ?: return bereich
    val unten = strengereUntereGrenze(
        bereich.untereGrenze,
        bereich.untereGrenzeEnthalten,
        minimum,
        true,
    ) ?: return bereich
    return bereich.copy(untereGrenze = unten.first, untereGrenzeEnthalten = unten.second)
}

private fun normalisiereKonkreteGrenzen(bereich: BeschraenkteZahlmenge): MengenAusdruck {
    val unten = bereich.untereGrenze as? RationaleZahl ?: return bereich
    val oben = bereich.obereGrenze as? RationaleZahl ?: return bereich
    if (unten > oben) return LeereMenge
    if (unten == oben) {
        if (!bereich.untereGrenzeEnthalten || !bereich.obereGrenzeEnthalten) return LeereMenge
        val enthalten = ElementBeziehung(unten, bereich.traeger.alsMenge()).entscheide().wahrheitswert
        return if (enthalten == Wahrheitswert.Wahr) EndlicheMenge(setOf(unten)) else LeereMenge
    }
    if (bereich.traeger in setOf(
            FundamentalerZahlbereich.NATUERLICH_POSITIV,
            FundamentalerZahlbereich.NATUERLICH_MIT_NULL,
            FundamentalerZahlbereich.GANZ,
        )
    ) {
        val erstes = erstesGanzzahligesElement(unten, bereich.untereGrenzeEnthalten)
        val letztes = letztesGanzzahligesElement(oben, bereich.obereGrenzeEnthalten)
        if (erstes > letztes) return LeereMenge
    }
    return bereich
}

private fun erstesGanzzahligesElement(grenze: RationaleZahl, enthalten: Boolean): java.math.BigInteger {
    val (ganz, rest) = grenze.zähler.divideAndRemainder(grenze.nenner)
    val aufgerundet = if (rest.signum() > 0) ganz + java.math.BigInteger.ONE else ganz
    return if (enthalten || rest.signum() != 0) aufgerundet else aufgerundet + java.math.BigInteger.ONE
}

private fun letztesGanzzahligesElement(grenze: RationaleZahl, enthalten: Boolean): java.math.BigInteger {
    val (ganz, rest) = grenze.zähler.divideAndRemainder(grenze.nenner)
    val abgerundet = if (rest.signum() < 0) ganz - java.math.BigInteger.ONE else ganz
    return if (enthalten || rest.signum() != 0) abgerundet else abgerundet - java.math.BigInteger.ONE
}
