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
            MENGENDEFINITION_FORMMODUS to STRUKTURFORM_UNBEKANNT,
            MENGENDEFINITION_FORMEINGABE to STRUKTURFORM_EINZELN,
            MENGENDEFINITION_FORM to "",
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

fun konfiguriereMengenkonstruktor(
    knoten: KnotenDaten,
    elementArt: AnschlussArtId,
    formModus: String = knoten.parameter[MENGENDEFINITION_FORMMODUS] ?: STRUKTURFORM_UNBEKANNT,
    formEingabe: String = knoten.parameter[MENGENDEFINITION_FORMEINGABE] ?: STRUKTURFORM_EINZELN,
    inspektorForm: String = knoten.parameter[MENGENDEFINITION_FORM].orEmpty(),
): KnotenDaten {
    require(knoten.art == MENGENKONSTRUKTOR_ART)
    val istStruktur = elementArt in setOf(
        MathematikAnschlussArten.Tupel.id,
        MathematikAnschlussArten.SpaltenVektor.id,
        MathematikAnschlussArten.ZeilenVektor.id,
        MathematikAnschlussArten.Matrix.id,
        MathematikAnschlussArten.Tensor.id,
    )
    val modus = formModus.takeIf { istStruktur && it in setOf(
        STRUKTURFORM_UNBEKANNT, STRUKTURFORM_INSPEKTOR, STRUKTURFORM_EINGANG,
    ) } ?: STRUKTURFORM_UNBEKANNT
    val eingabe = formEingabe.takeIf { it in setOf(STRUKTURFORM_EINZELN, STRUKTURFORM_TUPEL) }
        ?: STRUKTURFORM_EINZELN
    val vorhandene = knoten.anschlüsse.associateBy(AnschlussDaten::name)
    val element = vorhandene["element"]?.takeIf { it.art == elementArt }?.copy(
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(
        name = "element",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = elementArt,
    )
    val dimensionen = if (modus != STRUKTURFORM_EINGANG) emptyList() else if (eingabe == STRUKTURFORM_TUPEL) {
        listOf(formEingang(vorhandene["dimensionen"], "dimensionen", MathematikAnschlussArten.Tupel.id, 0))
    } else {
        val anzahl = when (elementArt) {
            MathematikAnschlussArten.Matrix.id -> 2
            MathematikAnschlussArten.Tensor.id -> inspektorForm.split(',').count { it.isNotBlank() }.coerceAtLeast(1)
            else -> 1
        }
        List(anzahl) { index ->
            val name = "dimension.${index + 1}"
            formEingang(vorhandene[name], name, MathematikAnschlussArten.Zahl.id, index)
        }
    }
    return knoten.copy(
        parameter = (knoten.parameter - MENGENDEFINITION_ELEMENTMENGE) + mapOf(
            MENGENDEFINITION_ELEMENTART to elementArt.wert,
            MENGENDEFINITION_FORMMODUS to modus,
            MENGENDEFINITION_FORMEINGABE to eingabe,
            MENGENDEFINITION_FORM to inspektorForm,
        ),
        anschlüsse = dimensionen + element,
    )
}

private fun formEingang(
    vorhanden: AnschlussDaten?,
    name: String,
    art: AnschlussArtId,
    reihenfolge: Int,
): AnschlussDaten = vorhanden?.takeIf { it.art == art && it.richtung == AnschlussRichtung.Eingang }?.copy(
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
