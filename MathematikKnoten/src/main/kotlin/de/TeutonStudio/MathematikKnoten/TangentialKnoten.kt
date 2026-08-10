package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialBegriff
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.TangentialAusgabeForm
import de.TeutonStudio.MathematikRechenSystem.kern.TangentialErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.tangentialObjekt

const val TANGENTIAL_KNOTEN_ART = "mathematik.tangentialObjekt"
const val TANGENTIAL_AUSGABEFORM_PARAMETER = "tangential.ausgabeForm"
const val TANGENTIAL_DIFFERENTIALBEGRIFF_PARAMETER = "tangential.differentialBegriff"

object TangentialKnotenVorlagen {
    val TangentialObjekt = KnotenVorlage(
        art = TANGENTIAL_KNOTEN_ART,
        name = "Tangentialobjekt",
        kategorie = "Analysis: Differentialrechnung",
        beschreibung = "Erzeugt am Argument einer Methode wahlweise die Tangentialmethode oder die geometrische Tangentialmenge.",
        standardGröße = GraphGröße(285f, 130f),
        anschlüsse = tangentialAnschluesse(TangentialAusgabeForm.METHODE),
        standardParameter = mapOf(
            TANGENTIAL_AUSGABEFORM_PARAMETER to TangentialAusgabeForm.METHODE.name,
            TANGENTIAL_DIFFERENTIALBEGRIFF_PARAMETER to DifferentialBegriff.REELL_FRECHET.name,
        ),
    )

    val alle = listOf(TangentialObjekt)
}

fun aktuelleTangentialAusgabeform(knoten: KnotenDaten): TangentialAusgabeForm =
    TangentialAusgabeForm.entries.firstOrNull {
        it.name == knoten.parameter[TANGENTIAL_AUSGABEFORM_PARAMETER]
    } ?: TangentialAusgabeForm.METHODE

fun konfiguriereTangentialKnoten(
    knoten: KnotenDaten,
    ausgabeForm: TangentialAusgabeForm,
): KnotenDaten {
    require(knoten.art == TANGENTIAL_KNOTEN_ART)
    val bisher = knoten.anschlüsse.associateBy { it.richtung to it.name }
    val anschluesse = tangentialAnschluesse(ausgabeForm).map { neu ->
        bisher[neu.richtung to neu.name]?.let { alt -> neu.copy(id = alt.id) } ?: neu
    }
    return knoten.copy(
        name = when (ausgabeForm) {
            TangentialAusgabeForm.METHODE -> "Tangentialmethode"
            TangentialAusgabeForm.MENGE -> "Tangentialmenge"
        },
        anschlüsse = anschluesse,
        parameter = knoten.parameter + (TANGENTIAL_AUSGABEFORM_PARAMETER to ausgabeForm.name),
    )
}

internal fun MathematikAuswerterRegister.registriereTangentialKnoten() {
    registriere(TANGENTIAL_KNOTEN_ART) { kontext ->
        val methode = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Für das Tangentialobjekt fehlt die Methode.")
        val argument = kontext.eingänge["argument"]?.objekt as? MathematischesObjekt
            ?: error("Für das Tangentialobjekt fehlt das Argument.")
        val begriff = DifferentialBegriff.entries.firstOrNull {
            it.name == kontext.knoten.parameter[TANGENTIAL_DIFFERENTIALBEGRIFF_PARAMETER]
        } ?: DifferentialBegriff.REELL_FRECHET
        val ergebnis = tangentialObjekt(
            methode = methode,
            argument = argument,
            ausgabeForm = aktuelleTangentialAusgabeform(kontext.knoten),
            begriff = begriff,
        )
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
        when (ergebnis) {
            is TangentialErgebnis.MethodeWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    "tangente" to BedingterWert(
                        objekt = ergebnis.methode,
                        annahmen = annahmen,
                        latexDarstellung = ergebnis.methode.zuLatex(),
                    ),
                ),
                warnungen = listOf(ergebnis.verwendeteRegel),
            )
            is TangentialErgebnis.MengeWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    "tangente" to BedingterWert(
                        objekt = ergebnis.menge,
                        annahmen = annahmen,
                        latexDarstellung = ergebnis.menge.zuLatex(),
                    ),
                ),
                warnungen = listOf(ergebnis.verwendeteRegel),
            )
            is TangentialErgebnis.NichtDarstellbarAlsMethode -> KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                fehler = ergebnis.grund,
                warnungen = listOf("Als Menge verfügbar: ${ergebnis.menge.zuLatex()}"),
            )
        }
    }
}

private fun tangentialAnschluesse(form: TangentialAusgabeForm): List<AnschlussDaten> = listOf(
    AnschlussDaten(
        name = "methode",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Methode.id,
        reihenfolge = 0,
    ),
    AnschlussDaten(
        name = "argument",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Objekt.id,
        reihenfolge = 1,
    ),
    AnschlussDaten(
        name = "tangente",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = when (form) {
            TangentialAusgabeForm.METHODE -> MathematikAnschlussArten.Methode.id
            TangentialAusgabeForm.MENGE -> MathematikAnschlussArten.Menge.id
        },
    ),
)
