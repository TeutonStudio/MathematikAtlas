package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.KnotenKartenVerwalter.zustand.MINDESTEINGÄNGE_PARAMETER

const val TUPEL_EINZEL_EINGABEN = "einzelEingaben"
const val TUPEL_METHODE = "methode"
private const val TUPEL_ART = "mathematik.tupel"
private val TUPEL_ERZEUGUNGS_ARTEN = setOf(TUPEL_EINZEL_EINGABEN, TUPEL_METHODE)

data class TupelKonfiguration(val erzeugungsArt: String)

fun tupelKonfiguration(knoten: KnotenDaten): TupelKonfiguration = TupelKonfiguration(
    erzeugungsArt = knoten.parameter["erzeugungsArt"]?.takeIf(TUPEL_ERZEUGUNGS_ARTEN::contains)
        ?: TUPEL_EINZEL_EINGABEN,
)

fun konfiguriereTupel(knoten: KnotenDaten, erzeugungsArt: String): KnotenDaten {
    require(knoten.art == TUPEL_ART) { "Nur Tupel-Knoten können so konfiguriert werden." }
    val art = erzeugungsArt.takeIf(TUPEL_ERZEUGUNGS_ARTEN::contains) ?: TUPEL_EINZEL_EINGABEN
    val vorhandene = knoten.anschlüsse.associateBy { it.name }
    val eingänge = when (art) {
        TUPEL_METHODE -> listOf(
            tupelEingang(vorhandene["dimension"], "dimension", MathematikAnschlussArten.Zahl.id, 0),
            tupelEingang(vorhandene["methode"], "methode", MathematikAnschlussArten.Methode.id, 1),
        )
        else -> {
            val anzahl = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(1) ?: 2
            List(anzahl) { index ->
                val name = when (index) {
                    0 -> "a"
                    1 -> "b"
                    else -> "input${index + 1}"
                }
                tupelEingang(vorhandene[name], name, MathematikAnschlussArten.Zahl.id, index, erweiterbar = true)
            }
        }
    }
    val ausgang = vorhandene["tupel"]?.copy(
        name = "tupel",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Tupel.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(
        name = "tupel",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Tupel.id,
    )
    val parameter = when (art) {
        TUPEL_METHODE -> knoten.parameter - MINDESTEINGÄNGE_PARAMETER
        else -> knoten.parameter + mapOf(
            "festeEingänge" to (knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(1) ?: 2).toString(),
            "operatorAnzeige" to (knoten.parameter["operatorAnzeige"] ?: "wert"),
            MINDESTEINGÄNGE_PARAMETER to "1",
        )
    } + ("erzeugungsArt" to art)
    return knoten.copy(anschlüsse = eingänge + ausgang, parameter = parameter)
}

private fun tupelEingang(
    vorhanden: AnschlussDaten?,
    name: String,
    art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
    reihenfolge: Int,
    erweiterbar: Boolean = false,
): AnschlussDaten = vorhanden?.copy(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
    kannSichErweitern = erweiterbar,
    dynamischErzeugt = false,
) ?: AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
    kannSichErweitern = erweiterbar,
)

fun KartenEditorZustand.setzeTupelKonfiguration(knotenId: KnotenId, erzeugungsArt: String) {
    val knoten = karte.knoten.firstOrNull { it.id == knotenId } ?: return
    val konfiguriert = konfiguriereTupel(knoten, erzeugungsArt)
    führeAus(KartenAktion.KnotenKonfigurationErsetzen(knotenId, konfiguriert.parameter, konfiguriert.anschlüsse))
}
