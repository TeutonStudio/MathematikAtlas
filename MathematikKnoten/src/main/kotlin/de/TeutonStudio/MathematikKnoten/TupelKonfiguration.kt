package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtAbbildung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.KnotenKartenVerwalter.zustand.MINDESTEINGÄNGE_PARAMETER
import de.TeutonStudio.TypSystem.TypInferenzRegel

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
                // Ein allgemeines Tupel darf nicht heimlich auf Zahlen beschränkt sein.
                tupelEingang(vorhandene[name], name, MathematikAnschlussArten.Objekt.id, index, erweiterbar = true)
            }
        }
    }
    val elementEingänge = eingänge.takeIf { art == TUPEL_EINZEL_EINGABEN }.orEmpty()
    val klassifikationsEingang = when {
        elementEingänge.size >= 2 -> elementEingänge[1].name
        elementEingänge.size == 1 -> elementEingänge[0].name
        else -> null
    }
    val artAbbildung = klassifikationsEingang?.let { name ->
        AnschlussArtAbbildung(
            eingang = name,
            abbildung = mapOf(
                MathematikAnschlussArten.Zahl.id to MathematikAnschlussArten.KartesischesTupel.id,
                MathematikAnschlussArten.Angulus.id to MathematikAnschlussArten.PolarTupel.id,
            ),
        )
    }
    val typInferenz = elementEingänge.takeIf { it.isNotEmpty() }?.let { elemente ->
        TypInferenzRegel.TupelAus(elemente.map { it.name })
    }
    val ausgang = vorhandene["tupel"]?.copy(
        name = "tupel",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Tupel.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
        artAbbildungVonEingang = artAbbildung,
        typInferenz = typInferenz,
    ) ?: AnschlussDaten(
        name = "tupel",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Tupel.id,
        artAbbildungVonEingang = artAbbildung,
        typInferenz = typInferenz,
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
    art: AnschlussArtId,
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
    zulässigeArten = emptySet(),
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

/** Setzt ausschließlich für den Elementmodus die feste Tupellänge; Einertupel sind gültig. */
fun KartenEditorZustand.setzeTupelEingangAnzahl(knotenId: KnotenId, anzahl: Int) {
    val knoten = karte.knoten.firstOrNull { it.id == knotenId && it.art == TUPEL_ART } ?: return
    val vorbereitet = knoten.copy(
        parameter = knoten.parameter + mapOf(
            "festeEingänge" to anzahl.coerceAtLeast(1).toString(),
            "erzeugungsArt" to TUPEL_EINZEL_EINGABEN,
        ),
    )
    val konfiguriert = konfiguriereTupel(vorbereitet, TUPEL_EINZEL_EINGABEN)
    führeAus(
        KartenAktion.KnotenKonfigurationErsetzen(
            knotenId,
            konfiguriert.parameter,
            konfiguriert.anschlüsse,
        ),
    )
}
