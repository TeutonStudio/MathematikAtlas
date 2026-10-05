package de.TeutonStudio.MathematikKnoten.visualisierung.modell

import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Leitet nur dann einen Darstellungsraum ab, wenn die Mengenstruktur ihn eindeutig festlegt. */
fun empfohleneRaumDimension(menge: MengenAusdruck): RaumDimension? = when (menge) {
    is Abbild -> empfohleneRaumDimension(menge.methode.zielMenge)
    KomplexeZahlen -> RaumDimension.C
    NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen,
    is ReellesIntervall -> RaumDimension.R1
    is KoordinatenBild -> menge.koordinatensystem.raum.dimension.alsReellerRaum()
    is OrbitBeschraenktheitsMenge -> empfohleneRaumDimension(menge.parameterRaum)
    is Vektorraum -> when (empfohleneRaumDimension(menge.skalarMenge)) {
        RaumDimension.R1 -> menge.dimension.alsReellerRaum()
        RaumDimension.C -> if (menge.dimension == 1) RaumDimension.C else (menge.dimension * 2).alsReellerRaum()
        else -> null
    }
    is Matrizenraum -> menge.vektorDimension()?.let { dimension ->
        when (empfohleneRaumDimension(menge.skalarMenge)) {
            RaumDimension.R1 -> dimension.alsReellerRaum()
            RaumDimension.C -> if (dimension == 1) RaumDimension.C else (dimension * 2).alsReellerRaum()
            else -> null
        }
    }
    is Tupelraum -> menge.komponenten.mapNotNull(::reelleKoordinatenDimension)
        .takeIf { it.size == menge.komponenten.size }
        ?.sum()
        ?.alsReellerRaum()
    is KartesischesProdukt -> menge.mengen.mapNotNull(::reelleKoordinatenDimension)
        .takeIf { it.size == menge.mengen.size }
        ?.sum()
        ?.alsReellerRaum()
    is EndlicheMenge -> endlicheMengenDimension(menge)
    is DefinierteMenge -> menge.variablen.mapNotNull { reelleKoordinatenDimension(it.grundMenge) }
        .takeIf { it.size == menge.variablen.size }
        ?.sum()
        ?.alsReellerRaum()
    is GefilterteMenge -> empfohleneRaumDimension(menge.menge)
    is Vereinigung -> gemeinsameDimension(menge.mengen)
    is Schnitt -> gemeinsameDimension(menge.mengen.ifEmpty { listOfNotNull(menge.grundMenge) })
    is MengenDifferenz -> gemeinsameDimension(listOf(menge.links, menge.rechts))
    is SymmetrischeDifferenz -> gemeinsameDimension(listOf(menge.links, menge.rechts))
    else -> null
}

fun VisualisierungsKonfiguration.mitWirksamerDimension(menge: MengenAusdruck?): VisualisierungsKonfiguration {
    if (dimensionsModus == DimensionsModus.Manuell || menge == null) return this
    val abgeleitet = empfohleneRaumDimension(menge) ?: return this
    if (abgeleitet == dimension) return this
    val neueAchsen = when (abgeleitet) {
        RaumDimension.C -> AchsenZuordnung("re", "im", null)
        RaumDimension.R1 -> achsen.copy(y = "", z = null)
        RaumDimension.R2 -> AchsenZuordnung(
            achsen.x.ifBlank { "x" },
            achsen.y.ifBlank { "y" },
            null,
        )
        RaumDimension.R3 -> AchsenZuordnung(
            achsen.x.ifBlank { "x" },
            achsen.y.ifBlank { "y" },
            achsen.z?.ifBlank { "z" } ?: "z",
        )
    }
    return copy(dimension = abgeleitet, achsen = neueAchsen)
}

private fun gemeinsameDimension(mengen: List<MengenAusdruck>): RaumDimension? =
    mengen.mapNotNull(::empfohleneRaumDimension)
        .takeIf { it.size == mengen.size }
        ?.distinct()
        ?.singleOrNull()

private fun reelleKoordinatenDimension(menge: MengenAusdruck): Int? = when (empfohleneRaumDimension(menge)) {
    RaumDimension.R1 -> 1
    RaumDimension.R2, RaumDimension.C -> 2
    RaumDimension.R3 -> 3
    null -> null
}

private fun endlicheMengenDimension(menge: EndlicheMenge): RaumDimension? {
    if (menge.elemente.isEmpty()) return null
    if (menge.elemente.all { it is ZahlAusdruck }) {
        return if (menge.elemente.any { it is KomplexeZahl }) RaumDimension.C else RaumDimension.R1
    }
    return menge.elemente.mapNotNull(::elementDimension)
        .takeIf { it.size == menge.elemente.size }
        ?.distinct()
        ?.singleOrNull()
        ?.alsReellerRaum()
}

private fun elementDimension(element: MathematischesObjekt): Int? = when (element) {
    is KomplexeZahl -> 2
    is ZahlAusdruck -> 1
    is Matrix -> element.vektorKomponenten()
        ?.mapNotNull(::elementDimension)
        ?.takeIf { it.size == element.zeilenAnzahl * element.spaltenAnzahl }
        ?.sum()
    is Tupel -> element.elemente.mapNotNull(::elementDimension)
        .takeIf { it.size == element.elemente.size }
        ?.sum()
    else -> null
}

private fun Matrizenraum.vektorDimension(): Int? = when {
    zeilen == 1 -> spalten
    spalten == 1 -> zeilen
    else -> null
}

private fun Matrix.vektorKomponenten(): List<ZahlAusdruck>? = when {
    zeilenAnzahl == 1 -> zeilen.single()
    spaltenAnzahl == 1 -> zeilen.map(List<ZahlAusdruck>::single)
    else -> null
}

private fun Int.alsReellerRaum(): RaumDimension? = when (this) {
    1 -> RaumDimension.R1
    2 -> RaumDimension.R2
    3 -> RaumDimension.R3
    else -> null
}
