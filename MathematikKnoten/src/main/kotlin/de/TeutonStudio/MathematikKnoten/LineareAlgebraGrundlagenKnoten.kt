package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.BegriffsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.GaussZiel
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.gauss
import de.TeutonStudio.MathematikRechenSystem.kern.inverseMitGauss
import de.TeutonStudio.MathematikRechenSystem.kern.loeseErweiterteMatrix
import de.TeutonStudio.MathematikRechenSystem.kern.loeseLinearesSystem
import de.TeutonStudio.MathematikRechenSystem.kern.pruefeLineareAbbildung
import de.TeutonStudio.MathematikRechenSystem.kern.pruefeVektorraum

const val BEGRIFF_VEKTORRAUM_KNOTEN_ART = "mathematik.begriff.vektorraum"
const val BEGRIFF_LINEARE_ABBILDUNG_KNOTEN_ART = "mathematik.begriff.lineareAbbildung"
const val GAUSS_MODUS_PARAMETER = "gaussModus"

object LineareAlgebraGrundlagenKnotenVorlagen {
    private fun eingang(
        name: String,
        art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
        reihe: Int,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = reihe,
    )

    private fun ausgang(
        name: String,
        art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )

    val Vektorraum = KnotenVorlage(
        art = BEGRIFF_VEKTORRAUM_KNOTEN_ART,
        name = "Vektorraum überprüfen",
        kategorie = "Lineare Algebra: Begriffe",
        beschreibung = "Prüft Trägermenge, Addition und skalare Multiplikation gegen die Vektorraumaxiome.",
        standardGröße = GraphGröße(300f, 145f),
        anschlüsse = listOf(
            eingang("menge", MathematikAnschlussArten.Menge.id, 0),
            eingang("addition", MathematikAnschlussArten.Methode.id, 1),
            eingang("skalareMultiplikation", MathematikAnschlussArten.Methode.id, 2),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
    )

    val LineareAbbildung = KnotenVorlage(
        art = BEGRIFF_LINEARE_ABBILDUNG_KNOTEN_ART,
        name = "Lineare Abbildung überprüfen",
        kategorie = "Lineare Algebra: Begriffe",
        beschreibung = "Prüft eine Methode zwischen zwei nachgewiesenen Vektorräumen auf Additivität und Homogenität.",
        standardGröße = GraphGröße(315f, 145f),
        anschlüsse = listOf(
            eingang("definitionsraum", MathematikAnschlussArten.Aussage.id, 0),
            eingang("zielraum", MathematikAnschlussArten.Aussage.id, 1),
            eingang("methode", MathematikAnschlussArten.Methode.id, 2),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
    )

    val alle = listOf(Vektorraum, LineareAbbildung) +
        SkalarproduktKnotenVorlagen.alle +
        StrukturFormelRechnerVorlagen.alle
}

internal fun MathematikAuswerterRegister.registriereLineareAlgebraGrundlagen() {
    val basisAuswerten = requireNotNull(finde("mathematik.auswerten")) {
        "Der Standardauswerter für mathematik.auswerten muss vor den Lina-Erweiterungen registriert sein."
    }

    registriere(BEGRIFF_VEKTORRAUM_KNOTEN_ART) { kontext ->
        val traeger = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
            ?: error("Die Trägermenge fehlt.")
        val addition = kontext.eingänge["addition"]?.objekt as? Methode
            ?: error("Die Additionsmethode fehlt.")
        val skalareMultiplikation = kontext.eingänge["skalareMultiplikation"]?.objekt as? Methode
            ?: error("Die skalare Multiplikation fehlt.")
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "aussage" to BedingterWert(
                    pruefeVektorraum(traeger, addition, skalareMultiplikation),
                    kontext.gemeinsameAnnahmen(),
                ),
            ),
        )
    }

    registriere(BEGRIFF_LINEARE_ABBILDUNG_KNOTEN_ART) { kontext ->
        val definitionsraum = kontext.eingänge["definitionsraum"]?.objekt as? BegriffsAussage
            ?: error("Der Definitionsraum benötigt die Aussage eines Vektorraum-Begriffsknotens.")
        val zielraum = kontext.eingänge["zielraum"]?.objekt as? BegriffsAussage
            ?: error("Der Zielraum benötigt die Aussage eines Vektorraum-Begriffsknotens.")
        val methode = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Die zu prüfende Methode fehlt.")
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "aussage" to BedingterWert(
                    pruefeLineareAbbildung(definitionsraum, zielraum, methode),
                    kontext.gemeinsameAnnahmen(),
                ),
            ),
        )
    }

    registriere("mathematik.auswerten") { kontext ->
        if (kontext.eingänge["objekt"]?.objekt is Matrix) {
            kontext.gaussAuswerten()
        } else {
            basisAuswerten.auswerten(kontext)
        }
    }
}

private fun KnotenAuswertungsKontext.gaussAuswerten(): KnotenAuswertungsErgebnis {
    val matrix = eingänge["objekt"]?.objekt as? Matrix
        ?: error("Für die lineare Auswertung wird eine Matrix benötigt.")
    val rechteSeite = eingänge["rechteSeite"]?.objekt as? SpaltenVektor
    val variablen = knoten.parameter["variablen"].orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
    val modus = knoten.parameter[GAUSS_MODUS_PARAMETER]
        ?.trim()
        ?.lowercase()
        .orEmpty()
        .ifBlank { "automatisch" }

    if (rechteSeite != null || modus in setOf("linearessystem", "lineares system", "loesen", "lösen")) {
        val system = if (rechteSeite != null) {
            loeseLinearesSystem(matrix, rechteSeite, variablen)
        } else {
            loeseErweiterteMatrix(matrix, variablenNamen = variablen)
        }
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(system.loesung, gemeinsameAnnahmen())),
            schritte = system.schritte,
            warnungen = listOf(
                "Rang(A) = ${system.rangKoeffizienten}, Rang(A|b) = ${system.rangErweitert}",
                "Spalten: ${system.variablenNamen.joinToString()} | b",
            ),
        )
    }

    if (modus in setOf("inverse", "inverse durch gauss-jordan", "inverse durch gauß-jordan")) {
        val inverse = inverseMitGauss(matrix)
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(inverse.inverse, gemeinsameAnnahmen())),
            schritte = inverse.schritte,
            warnungen = listOf("Inverse über [A|I] mit Gauß-Jordan bestimmt."),
        )
    }

    val ziel = when (modus) {
        "stufenform", "zeilenstufenform" -> GaussZiel.ZEILENSTUFENFORM
        else -> GaussZiel.REDUZIERTE_ZEILENSTUFENFORM
    }
    val ergebnis = gauss(matrix, ziel)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("wert" to BedingterWert(ergebnis.matrix, gemeinsameAnnahmen())),
        schritte = ergebnis.schritte,
        warnungen = listOf("Rang = ${ergebnis.rang}"),
    )
}

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen() =
    eingänge.values.flatMap { it.annahmen }.toSet()
