package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import java.util.UUID

object MengendefinitionKnotenVorlagen {
    val Mengenkonstruktor = KnotenVorlage(
        art = MENGENKONSTRUKTOR_ART,
        name = "Mengenkonstruktor",
        kategorie = "Mengen",
        beschreibung = "Beginnt eine gebundene Mengendefinition und stellt ihr typisiertes Argument bereit.",
        standardGröße = GraphGröße(275f, 125f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "element",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        standardParameter = mapOf(
            MENGENDEFINITION_MENGENNAME to "M",
            MENGENDEFINITION_ELEMENTNAME to "x",
            MENGENDEFINITION_ELEMENTART to MathematikAnschlussArten.Zahl.id.wert,
        ),
    )

    val Mengendefinator = KnotenVorlage(
        art = MENGENDEFINATOR_ART,
        name = "Mengendefinator",
        kategorie = "Mengen",
        beschreibung = "Schließt eine gebundene Mengendefinition mit einem Prädikat ab.",
        standardGröße = GraphGröße(270f, 115f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "aussage",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Aussage.id,
            ),
            AnschlussDaten(
                name = "menge",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Menge.id,
            ),
        ),
    )
}

data class MengendefinitionsPaar(
    val konstruktor: KnotenDaten,
    val definator: KnotenDaten,
    val paarId: String,
)

/** Erzeugt beide Endpunkte ohne mathematisch falsche Direktverbindung. */
fun erzeugeMengendefinitionsPaar(position: GraphPunkt): MengendefinitionsPaar {
    val paarId = UUID.randomUUID().toString()
    val konstruktor = MengendefinitionKnotenVorlagen.Mengenkonstruktor.erzeuge(position).let { knoten ->
        knoten.copy(parameter = knoten.parameter + (MENGENDEFINITION_PAAR to paarId))
    }
    val definator = MengendefinitionKnotenVorlagen.Mengendefinator
        .erzeuge(position + GraphPunkt(410f, 0f))
        .let { knoten -> knoten.copy(parameter = knoten.parameter + (MENGENDEFINITION_PAAR to paarId)) }
    return MengendefinitionsPaar(konstruktor, definator, paarId)
}

fun KnotenDaten.istMengendefinitionsEndpunkt(): Boolean =
    art == MENGENKONSTRUKTOR_ART || art == MENGENDEFINATOR_ART

fun KnotenDaten.mengendefinitionsPaarId(): String? =
    parameter[MENGENDEFINITION_PAAR]?.trim()?.takeIf(String::isNotEmpty)

fun mengenkonstruktorFormel(knoten: KnotenDaten): String {
    val mengenName = knoten.parameter[MENGENDEFINITION_MENGENNAME]?.trim().orEmpty().ifBlank { "M" }
    val elementName = knoten.parameter[MENGENDEFINITION_ELEMENTNAME]?.trim().orEmpty().ifBlank { "x" }
    return "$mengenName=\\left\\{$elementName\\mid"
}
