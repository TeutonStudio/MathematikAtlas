package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Persistierbare, rekursive Beschreibung eines Wertebereichs allgemeiner Parameter. */
sealed interface WertebereichKonfiguration {
    data class Zahl(val grundmenge: String = "R") : WertebereichKonfiguration
    data object Winkel : WertebereichKonfiguration
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
        Winkel -> Gradmenge
        Aussage -> Wahrheitsmenge
        is Menge -> Potenzmenge(elementBereich.zuMenge())
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
        Winkel -> objekt("winkel")
        Aussage -> objekt("aussage")
        is Menge -> objekt("menge", "elementBereich" to elementBereich.zuEigenschaft())
        is Tupel -> objekt("tupel", "komponenten" to KnotenEigenschaft.Liste(komponenten.map { it.zuEigenschaft() }))
        is Vektor -> objekt("vektor", "orientierung" to KnotenEigenschaft.Text(orientierung.name), "dimension" to KnotenEigenschaft.Ganzzahl(dimension), "skalarMenge" to KnotenEigenschaft.Text(skalarMenge))
        is Matrix -> objekt("matrix", "zeilen" to KnotenEigenschaft.Ganzzahl(zeilen), "spalten" to KnotenEigenschaft.Ganzzahl(spalten), "skalarMenge" to KnotenEigenschaft.Text(skalarMenge))
    }

    companion object {
        const val EIGENSCHAFT = "wertebereich"
        val Standard = Zahl()

        /** Ganz fehlend bedeutet den dokumentierten historischen Standard; fehlerhafte Daten niemals. */
        fun vonEigenschaft(wert: KnotenEigenschaft?): WertebereichKonfiguration {
            if(wert==null) return Standard
            return liesObjekt(wert,0).also {it.zuMenge()}
        }

        private fun liesObjekt(wert:KnotenEigenschaft, tiefe:Int):WertebereichKonfiguration {
            require(tiefe<64) { "Der Wertebereich ist zu tief verschachtelt." }
            val felder=(wert as? KnotenEigenschaft.Objekt)?.felder ?: error("Ein Wertebereich muss ein Objekt sein.")
            val version=felder["version"]?.let {(it as? KnotenEigenschaft.Ganzzahl)?.wert ?: error("Ungültige Wertebereichsversion.")} ?: 0
            require(version in 0..1) {"Unbekannte Wertebereichsversion: $version"}
            val art=(felder["art"] as? KnotenEigenschaft.Text)?.wert ?: error("Die Art des Wertebereichs fehlt oder ist ungültig.")
            val erlaubt=when(art) {
                "zahl" -> setOf("grundmenge")
                "winkel","aussage" -> emptySet()
                "menge" -> setOf("elementBereich")
                "tupel" -> setOf("komponenten")
                "vektor" -> setOf("orientierung","dimension","skalarMenge")
                "matrix" -> setOf("zeilen","spalten","skalarMenge")
                else -> error("Unbekannte Wertebereichsart '$art'.")
            }+setOf("art","version")
            require(felder.keys.all {it in erlaubt}) {"Unbekannte Wertebereichsfelder: ${(felder.keys-erlaubt).sorted().joinToString()}"}
            fun unterbereich(name:String)=felder[name]?.let {liesObjekt(it,tiefe+1)} ?: Standard
            return when(art) {
                "zahl" -> Zahl(text(felder,"grundmenge","R"))
                "winkel" -> Winkel
                "aussage" -> Aussage
                "menge" -> Menge(unterbereich("elementBereich"))
                "tupel" -> {
                    val komponenten=felder["komponenten"]?.let {(it as? KnotenEigenschaft.Liste)?.werte ?: error("Tupelkomponenten müssen eine Liste sein.")}
                    require(version==1 || komponenten?.isEmpty()!=true) {"Der historische leere Tupelbereich wurde als zwei reelle Komponenten behandelt. Bitte den gewünschten Wertebereich ausdrücklich neu wählen."}
                    Tupel(komponenten?.map {liesObjekt(it,tiefe+1)} ?: listOf(Zahl(),Zahl()))
                }
                "vektor" -> Vektor(
                    runCatching {VektorOrientierung.valueOf(text(felder,"orientierung",VektorOrientierung.Spalte.name))}.getOrElse {error("Unbekannte Vektororientierung im Wertebereich.")},
                    ganzzahl(felder,"dimension",2),text(felder,"skalarMenge","R"))
                "matrix" -> Matrix(ganzzahl(felder,"zeilen",2),ganzzahl(felder,"spalten",2),text(felder,"skalarMenge","R"))
                else -> error("Unbekannte Wertebereichsart.")
            }
        }

        private fun objekt(art: String, vararg felder: Pair<String, KnotenEigenschaft>) =
            KnotenEigenschaft.Objekt(mapOf("art" to KnotenEigenschaft.Text(art), "version" to KnotenEigenschaft.Ganzzahl(1)) + felder)
        private fun text(felder: Map<String, KnotenEigenschaft>, schlüssel: String, standard: String) =
            felder[schlüssel]?.let {(it as? KnotenEigenschaft.Text)?.wert ?: error("Wertebereichsfeld '$schlüssel' muss Text sein.")} ?: standard
        private fun ganzzahl(felder: Map<String, KnotenEigenschaft>, schlüssel: String, standard: Int) =
            felder[schlüssel]?.let {(it as? KnotenEigenschaft.Ganzzahl)?.wert ?: error("Wertebereichsfeld '$schlüssel' muss eine ganze Zahl sein.")} ?: standard
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
