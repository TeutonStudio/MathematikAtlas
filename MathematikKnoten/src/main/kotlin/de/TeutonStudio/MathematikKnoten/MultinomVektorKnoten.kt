package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

object MultinomVektorKnotenVorlagen {
    val standard = KnotenVorlage(
        art = MULTINOMVEKTOR_ART,
        name = "Multinomvektor",
        kategorie = "Vektoren",
        beschreibung = "Erzeugt (x^k) für k=0,…,dim als Spaltenvektor, Zeilenvektor oder Tupel.",
        standardGröße = GraphGröße(245f, 125f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "x",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Zahl.id,
                reihenfolge = 0,
            ),
            AnschlussDaten(
                name = "dim",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Zahl.id,
                reihenfolge = 1,
            ),
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.SpaltenVektor.id,
            ),
        ),
        standardParameter = mapOf(
            MULTINOM_AUSGABEFORM_PARAMETER to MULTINOM_AUSGABE_VEKTOR,
            VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_SPALTE,
        ),
    )
}

fun MathematikAuswerterRegister.registriereMultinomVektor() {
    registriere(MULTINOMVEKTOR_ART) { kontext ->
        val xWert = kontext.eingänge["x"] ?: error("Der Multinomvektor benötigt den Eingang x.")
        val dimEingang = kontext.eingänge["dim"] ?: error("Der Multinomvektor benötigt den Eingang dim.")
        val x = xWert.objekt as? ZahlAusdruck ?: error("Der Eingang x muss ein Zahlterm sein.")
        val dimWert = dimEingang.objekt as? RationaleZahl
            ?: error("Der Multinomvektor benötigt eine konkrete ganze Dimension.")
        require(dimWert.nenner == BigInteger.ONE && dimWert.zähler.signum() >= 0 && dimWert.zähler.bitLength() < 31) {
            "dim muss eine konkrete nichtnegative ganze Zahl sein."
        }
        val dim = dimWert.zähler.toInt()
        val komponenten = multinomFolge(x, dim)
        val form = kontext.knoten.parameter[MULTINOM_AUSGABEFORM_PARAMETER] ?: MULTINOM_AUSGABE_VEKTOR
        val orient = kontext.knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: VEKTOR_ORIENTIERUNG_SPALTE
        val objekt: MathematischesObjekt = when {
            form == MULTINOM_AUSGABE_TUPEL -> Tupel(komponenten)
            orient == VEKTOR_ORIENTIERUNG_ZEILE -> ZeilenVektor(komponenten)
            else -> SpaltenVektor(komponenten)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "wert" to BedingterWert(
                    objekt = objekt,
                    annahmen = xWert.annahmen + dimEingang.annahmen,
                    reelleVariablen = xWert.reelleVariablen + dimEingang.reelleVariablen,
                    variablenQuellen = xWert.variablenQuellen + dimEingang.variablenQuellen,
                    latexDarstellung = when (objekt) {
                        is Tupel -> "(x^k)_{0\\le k\\le $dim}"
                        is ZeilenVektor -> "(x^k)_{0\\le k\\le $dim}"
                        is SpaltenVektor -> "\\left(x^k\\right)_{0\\le k\\le $dim}^{\\mathsf T}"
                        else -> null
                    },
                ),
            ),
            eingänge = kontext.eingänge,
        )
    }
}
