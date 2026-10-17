package de.TeutonStudio.MathematikKnoten.visualisierung.modell

import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.numerischerRaum

/** Leitet nur dann einen Darstellungsraum ab, wenn die Mengenstruktur ihn eindeutig festlegt. */
fun empfohleneRaumDimension(menge: MengenAusdruck): RaumDimension? = when (menge) {
    is MethodenGraphMenge -> empfohleneRaumDimension(menge.methode.graphRaum())
    is Abbild -> empfohleneRaumDimension(menge.methode.zielMenge)
    KomplexeZahlen -> RaumDimension.C
    NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen,
    is ReellesIntervall -> RaumDimension.R1
    is KoordinatenBild -> menge.koordinatensystem.raum.dimension.alsReellerRaum()
    is OrbitBeschraenktheitsMenge -> empfohleneRaumDimension(menge.parameterRaum)
    NichtnegativeGanzeZahlenSemantik.menge, Primzahlen -> RaumDimension.R1
    GaußscheGanzeZahlen, GaußschePrimzahlen -> RaumDimension.C
    is Vektorraum -> if (menge.dimension == 1 && menge.skalarMenge == KomplexeZahlen) RaumDimension.C
        else numerischerRaum(menge)?.blätter?.size?.alsReellerRaum()
    is Tensorraum, is Matrizenraum, is KartesischerPotenzraum -> numerischerRaum(menge)?.blätter?.size?.alsReellerRaum()
    is Tupelraum -> menge.komponenten.mapNotNull(::reelleKoordinatenDimension)
        .takeIf { it.size == menge.komponenten.size }?.sum()?.alsReellerRaum()
    is KartesischesProdukt -> menge.mengen.mapNotNull(::reelleKoordinatenDimension)
        .takeIf { it.size == menge.mengen.size }?.sum()?.alsReellerRaum()
    is EndlicheMenge -> endlicheMengenDimension(menge)
    is DefinierteMenge -> menge.variablen.mapNotNull { reelleKoordinatenDimension(it.grundMenge) }
        .takeIf { it.size == menge.variablen.size }
        ?.sum()
        ?.alsReellerRaum()
    is GefilterteMenge -> empfohleneRaumDimension(menge.menge)
    is Vereinigung -> gemeinsameDimension(menge.mengen)
    is Schnitt -> gemeinsameDimension(menge.mengen.ifEmpty { listOfNotNull(menge.grundMenge) })
    is MengenDifferenz -> empfohleneRaumDimension(menge.links)
    is SymmetrischeDifferenz -> gemeinsameDimension(listOf(menge.links, menge.rechts))
    else -> null
}

fun VisualisierungsKonfiguration.mitWirksamerDimension(menge: MengenAusdruck?): VisualisierungsKonfiguration {
    if (dimensionsModus == DimensionsModus.Manuell || ausgabeKoordinaten.isNotEmpty() || menge == null) return this
    val abgeleitet = if (menge is Abbild && (methodenModus == MethodenDarstellungsModus.Funktionsgraph ||
        methodenModus == MethodenDarstellungsModus.Automatisch && menge.methode.vorschrift is ZahlAusdruck && menge.methode.zielMenge != KomplexeZahlen)) {
        (menge.methode.parameter.size + 1).alsReellerRaum()
    } else empfohleneRaumDimension(menge)
    if (abgeleitet == null) return this
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
    mengen.filterNot { it == LeereMenge || it is EndlicheMenge && it.elemente.isEmpty() }.let { nichtleer ->
        nichtleer.mapNotNull(::empfohleneRaumDimension).takeIf { it.size == nichtleer.size }
    }
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
        val dimensionen = menge.elemente.map { elementDimension(it) ?: return null }
        return if (dimensionen.any { it > 2 }) null else if (dimensionen.any { it == 2 }) RaumDimension.C else RaumDimension.R1
    }
    return menge.elemente.mapNotNull(::elementDimension)
        .takeIf { it.size == menge.elemente.size }
        ?.distinct()
        ?.singleOrNull()
        ?.alsReellerRaum()
}

private fun elementDimension(element: MathematischesObjekt): Int? = when (element) {
    is KomplexeZahl -> 2
    is ZahlAusdruck -> when ((DomaenenAuswerter.wert(element) as? DomaenenErgebnis.Wert)?.wert) {
        is DomaenenWert.Reell -> 1
        is DomaenenWert.Komplex -> 2
        is DomaenenWert.Quaternion -> 4
        null -> null
    }
    is ZeilenVektor -> element.werte.sumOf { elementDimension(it) ?: return null }
    is SpaltenVektor -> element.werte.sumOf { elementDimension(it) ?: return null }
    is Tensor -> if (element.dimensionen.count { it > 1 } <= 1) element.werte.sumOf { elementDimension(it) ?: return null } else null
    is Matrix -> element.vektorKomponenten()
        ?.mapNotNull(::elementDimension)
        ?.takeIf { it.size == element.zeilenAnzahl * element.spaltenAnzahl }
        ?.sum()
    is Tupel -> element.elemente.mapNotNull(::elementDimension)
        .takeIf { it.size == element.elemente.size }
        ?.sum()
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
