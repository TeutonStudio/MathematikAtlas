package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixDiagonalArt
import de.TeutonStudio.MathematikRechenSystem.kern.Tensorartig
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val MATRIXDIAGONALE_ART = "mathematik.matrixdiagonale"
const val MATRIXDIAGONALE_ART_PARAMETER = "diagonalArt"
const val MATRIXDIAGONALE_MODUS = "diagonalModus"
const val MATRIXDIAGONALE_AUSGABE = "diagonalAusgabe"
const val MATRIXDIAGONALE_AUSLESEN = "auslesen"
const val MATRIXDIAGONALE_ERZEUGEN = "erzeugen"

fun matrixdiagonaleModus(knoten: KnotenDaten) =
    if (knoten.parameter[MATRIXDIAGONALE_MODUS] == MATRIXDIAGONALE_ERZEUGEN) MATRIXDIAGONALE_ERZEUGEN else MATRIXDIAGONALE_AUSLESEN

fun matrixdiagonaleAusgabe(knoten: KnotenDaten): String =
    knoten.parameter[MATRIXDIAGONALE_AUSGABE]?.takeIf { it in setOf("tupel", "zeile", "spalte") } ?: "tupel"

/** Port-IDs bleiben beim Moduswechsel erhalten; die Konfigurationsplanung prüft bestehende Kanten. */
fun konfiguriereMatrixdiagonale(knoten: KnotenDaten, modus: String = matrixdiagonaleModus(knoten),
    ausgabe: String = matrixdiagonaleAusgabe(knoten)): KnotenDaten {
    require(knoten.art == MATRIXDIAGONALE_ART && modus in setOf(MATRIXDIAGONALE_AUSLESEN, MATRIXDIAGONALE_ERZEUGEN))
    require(ausgabe in setOf("tupel", "zeile", "spalte"))
    val eingang = knoten.anschlüsse.first { it.richtung == AnschlussRichtung.Eingang }
    val ausgang = knoten.anschlüsse.first { it.richtung == AnschlussRichtung.Ausgang }
    val ausgabeArt = when (ausgabe) {
        "zeile" -> MathematikAnschlussArten.ZeilenVektor.id
        "spalte" -> MathematikAnschlussArten.SpaltenVektor.id
        else -> MathematikAnschlussArten.Tupel.id
    }
    val akzeptiert = setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id,
        MathematikAnschlussArten.SpaltenVektor.id)
    return knoten.copy(
        parameter = knoten.parameter + mapOf(MATRIXDIAGONALE_MODUS to modus, MATRIXDIAGONALE_AUSGABE to ausgabe),
        anschlüsse = if (modus == MATRIXDIAGONALE_ERZEUGEN) listOf(
            eingang.copy(name = "werte", art = MathematikAnschlussArten.Objekt.id, zulässigeArten = akzeptiert),
            ausgang.copy(name = "matrix", art = MathematikAnschlussArten.Matrix.id),
        ) else listOf(
            eingang.copy(name = "matrix", art = MathematikAnschlussArten.Matrix.id, zulässigeArten = emptySet()),
            ausgang.copy(name = "diagonale", art = ausgabeArt),
        ),
    )
}


object MatrixdiagonaleKnotenVorlagen {
    val Matrixdiagonale = KnotenVorlage(
        art = MATRIXDIAGONALE_ART,
        name = "Matrixdiagonale",
        kategorie = "Matrizen",
        beschreibung = "Liest die Haupt- oder Nebendiagonale als Tupel/Vektor oder erzeugt eine quadratische Diagonalmatrix aus Tupel/Vektor.",
        standardGröße = GraphGröße(270f, 115f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "matrix",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Matrix.id,
            ),
            AnschlussDaten(
                name = "diagonale",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Tupel.id,
            ),
        ),
        standardParameter = mapOf(
            MATRIXDIAGONALE_ART_PARAMETER to MatrixDiagonalArt.HAUPTDIAGONALE.parameterWert,
            MATRIXDIAGONALE_MODUS to MATRIXDIAGONALE_AUSLESEN,
            MATRIXDIAGONALE_AUSGABE to "tupel",
        ),
    )

    /**
     * Sichtbare Vorlagen des Erstellen-Dialogs. Vorkonfigurierte Varianten
     * bestehender Rechnerfamilien bleiben in ihren Definitionskatalogen und
     * werden nicht als zusätzliche Knoten angeboten.
     */
    val alle = listOf(Matrixdiagonale, SpurKnotenVorlagen.Spur) +
        TupelOperationKnotenVorlagen.alle +
        StrukturRechnerKnotenVorlagen.alle.filterNot { it.art in historischeSkalarproduktArten } +
        EinheitsvektorKnotenVorlagen.alle +
        VektorRechnerKnotenVorlagen.standard
}
