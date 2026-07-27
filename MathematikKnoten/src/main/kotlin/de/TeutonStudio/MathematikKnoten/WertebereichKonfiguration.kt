package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Persistierbare, rekursive Beschreibung eines Wertebereichs allgemeiner Parameter. */
sealed interface WertebereichKonfiguration {
    data class Zahl(val grundmenge: String = "R") : WertebereichKonfiguration
    data object Aussage : WertebereichKonfiguration
    data class Menge(val elementBereich: WertebereichKonfiguration = Zahl()) : WertebereichKonfiguration
    data class Tupel(val komponenten: List<WertebereichKonfiguration> = listOf(Zahl(), Zahl())) : WertebereichKonfiguration
    data class Vektor(
        val orientierung: VektorOrientierung = VektorOrientierung.Spalte,
        val dimension: Int = 2,
        val skalarMenge: String = "R",
    ) : WertebereichKonfiguration
    data class Matrix(val zeilen: Int = 2, val spalten: Int = 2, val skalarMenge: String = "R") : WertebereichKonfiguration

    fun zuMenge(): MengenAusdruck = when (this) {
        is Zahl -> zahlGrundmenge(grundmenge)
        Aussage -> Wahrheitsmenge
        is Menge -> elementBereich.zuMenge()
        is Tupel -> Tupelraum(komponenten.map { it.zuMenge() })
        is Vektor -> Vektorraum(orientierung, dimension.takeIf { it > 0 } ?: error("Die Vektordimension muss positiv sein."), zahlGrundmenge(skalarMenge))
        is Matrix -> Matrizenraum(
            zeilen.takeIf { it > 0 } ?: error("Die Zeilenzahl muss positiv sein."),
            spalten.takeIf { it > 0 } ?: error("Die Spaltenzahl muss positiv sein."),
            zahlGrundmenge(skalarMenge),
        )
    }

    fun zuEigenschaft(): KnotenEigenschaft.Objekt = when (this) {
        is Zahl -> objekt("zahl", "grundmenge" to KnotenEigenschaft.Text(grundmenge))
        Aussage -> objekt("aussage")
        is Menge -> objekt("menge", "elementBereich" to elementBereich.zuEigenschaft())
        is Tupel -> objekt("tupel", "komponenten" to KnotenEigenschaft.Liste(komponenten.map { it.zuEigenschaft() }))
        is Vektor -> objekt("vektor", "orientierung" to KnotenEigenschaft.Text(orientierung.name), "dimension" to KnotenEigenschaft.Ganzzahl(dimension), "skalarMenge" to KnotenEigenschaft.Text(skalarMenge))
        is Matrix -> objekt("matrix", "zeilen" to KnotenEigenschaft.Ganzzahl(zeilen), "spalten" to KnotenEigenschaft.Ganzzahl(spalten), "skalarMenge" to KnotenEigenschaft.Text(skalarMenge))
    }

    companion object {
        const val EIGENSCHAFT = "wertebereich"
        val Standard = Zahl()

        fun vonEigenschaft(wert: KnotenEigenschaft?): WertebereichKonfiguration = liesObjekt(wert as? KnotenEigenschaft.Objekt)

        private fun liesObjekt(objekt: KnotenEigenschaft.Objekt?): WertebereichKonfiguration {
            val felder = objekt?.felder.orEmpty()
            val art = (felder["art"] as? KnotenEigenschaft.Text)?.wert
            return when (art) {
                "zahl" -> Zahl(text(felder, "grundmenge", "R"))
                "aussage" -> Aussage
                "menge" -> Menge(liesObjekt(felder["elementBereich"] as? KnotenEigenschaft.Objekt))
                "tupel" -> Tupel((felder["komponenten"] as? KnotenEigenschaft.Liste)?.werte
                    ?.map { liesObjekt(it as? KnotenEigenschaft.Objekt) }
                    ?.takeIf { it.isNotEmpty() } ?: listOf(Zahl(), Zahl()))
                "vektor" -> Vektor(
                    runCatching { VektorOrientierung.valueOf(text(felder, "orientierung", VektorOrientierung.Spalte.name)) }.getOrDefault(VektorOrientierung.Spalte),
                    ganzzahl(felder, "dimension", 2),
                    text(felder, "skalarMenge", "R"),
                )
                "matrix" -> Matrix(ganzzahl(felder, "zeilen", 2), ganzzahl(felder, "spalten", 2), text(felder, "skalarMenge", "R"))
                else -> Standard
            }
        }

        private fun objekt(art: String, vararg felder: Pair<String, KnotenEigenschaft>) =
            KnotenEigenschaft.Objekt(mapOf("art" to KnotenEigenschaft.Text(art)) + felder)
        private fun text(felder: Map<String, KnotenEigenschaft>, schlüssel: String, standard: String) =
            (felder[schlüssel] as? KnotenEigenschaft.Text)?.wert ?: standard
        private fun ganzzahl(felder: Map<String, KnotenEigenschaft>, schlüssel: String, standard: Int) =
            (felder[schlüssel] as? KnotenEigenschaft.Ganzzahl)?.wert ?: standard
    }
}

private val Wahrheitsmenge = EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))

private fun zahlGrundmenge(name: String): MengenAusdruck = when (name.trim().uppercase()) {
    "N" -> NatürlicheZahlen
    "Z" -> GanzeZahlen
    "Q" -> RationaleZahlen
    "R" -> ReelleZahlen
    "C" -> KomplexeZahlen
    else -> error("Unbekannte Zahlgrundmenge '$name'. Erlaubt sind N, Z, Q, R und C.")
}
