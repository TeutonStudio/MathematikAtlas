package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand

const val MATRIX_EINZEL_EINGABEN = "einzelEingaben"
const val MATRIX_ZEILEN = "zeilen"
const val MATRIX_SPALTEN = "spalten"
const val MATRIX_METHODE = "methode"
private const val MATRIX_ART = "mathematik.matrix"
private val MATRIX_ERZEUGUNGS_ARTEN = setOf(MATRIX_EINZEL_EINGABEN, MATRIX_ZEILEN, MATRIX_SPALTEN, MATRIX_METHODE)

data class MatrixKonfiguration(
    val erzeugungsArt: String,
    val höhe: Int,
    val breite: Int,
)

fun matrixKonfiguration(knoten: KnotenDaten): MatrixKonfiguration = MatrixKonfiguration(
    erzeugungsArt = knoten.parameter["erzeugungsArt"]?.takeIf(MATRIX_ERZEUGUNGS_ARTEN::contains)
        ?: MATRIX_EINZEL_EINGABEN,
    höhe = knoten.parameter["höhe"]?.toIntOrNull()?.takeIf { it > 0 } ?: 2,
    breite = knoten.parameter["breite"]?.toIntOrNull()?.takeIf { it > 0 } ?: 2,
)

fun konfiguriereMatrix(
    knoten: KnotenDaten,
    erzeugungsArt: String,
    höhe: Int,
    breite: Int,
): KnotenDaten {
    require(knoten.art == MATRIX_ART) { "Nur Matrix-Knoten können so konfiguriert werden." }
    val art = erzeugungsArt.takeIf(MATRIX_ERZEUGUNGS_ARTEN::contains) ?: MATRIX_EINZEL_EINGABEN
    val gültigeHöhe = höhe.coerceAtLeast(1)
    val gültigeBreite = breite.coerceAtLeast(1)
    val vorhandene = knoten.anschlüsse.associateBy { it.name }
    val eingänge = when (art) {
        MATRIX_METHODE -> listOf(
            matrixEingang(
                vorhanden = vorhandene["methode"],
                name = "methode",
                art = MathematikAnschlussArten.ZahlFunktion.id,
                reihenfolge = 0,
            ),
        )
        MATRIX_ZEILEN -> List(gültigeHöhe) { zeile ->
            val name = matrixZeileName(zeile)
            matrixEingang(
                vorhanden = vorhandene[name],
                name = name,
                art = MathematikAnschlussArten.ZeilenVektor.id,
                reihenfolge = zeile,
            )
        }
        MATRIX_SPALTEN -> List(gültigeBreite) { spalte ->
            val name = matrixSpalteName(spalte)
            matrixEingang(
                vorhanden = vorhandene[name],
                name = name,
                art = MathematikAnschlussArten.SpaltenVektor.id,
                reihenfolge = spalte,
            )
        }
        else -> List(gültigeHöhe * gültigeBreite) { index ->
            val zeile = index / gültigeBreite
            val spalte = index % gültigeBreite
            val name = matrixEintragName(zeile, spalte)
            matrixEingang(
                vorhanden = vorhandene[name],
                name = name,
                art = MathematikAnschlussArten.Zahl.id,
                reihenfolge = index,
            )
        }
    }
    val ausgang = vorhandene["matrix"]?.copy(
        name = "matrix",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Matrix.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(
        name = "matrix",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Matrix.id,
    )
    return knoten.copy(
        anschlüsse = eingänge + ausgang,
        parameter = knoten.parameter - setOf("festeEingänge", "operatorAnzeige") + mapOf(
            "erzeugungsArt" to art,
            "höhe" to gültigeHöhe.toString(),
            "breite" to gültigeBreite.toString(),
        ),
    )
}

private fun matrixEingang(
    vorhanden: AnschlussDaten?,
    name: String,
    art: AnschlussArtId,
    reihenfolge: Int,
): AnschlussDaten = vorhanden?.copy(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
    kannSichErweitern = false,
    dynamischErzeugt = false,
) ?: AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
)

fun KartenEditorZustand.setzeMatrixKonfiguration(
    knotenId: KnotenId,
    erzeugungsArt: String,
    höhe: Int,
    breite: Int,
) {
    val knoten = karte.knoten.firstOrNull { it.id == knotenId } ?: return
    val konfiguriert = konfiguriereMatrix(knoten, erzeugungsArt, höhe, breite)
    führeAus(KartenAktion.KnotenKonfigurationErsetzen(knotenId, konfiguriert.parameter, konfiguriert.anschlüsse))
}

fun matrixEintragName(zeile: Int, spalte: Int) = "eintrag_${zeile}_${spalte}"
fun matrixZeileName(zeile: Int) = "zeile_$zeile"
fun matrixSpalteName(spalte: Int) = "spalte_$spalte"
