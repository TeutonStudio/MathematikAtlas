package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlenOperatorHebungsArt
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypInferenzRegel

private val variadischeZahlenOperatoren = setOf(
    UniversellerZahlenOperator.ADDITION,
    UniversellerZahlenOperator.MULTIPLIKATION,
    UniversellerZahlenOperator.MINIMUM,
    UniversellerZahlenOperator.MAXIMUM,
)

private val komplexKonstruktoren = setOf(
    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
    UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH,
)

private val angulusErzeuger = setOf(
    UniversellerZahlenOperator.KOMPLEXER_WINKEL,
    UniversellerZahlenOperator.ARCSINUS,
    UniversellerZahlenOperator.ARCCOSINUS,
)

private val angulusVerbraucher = setOf(
    UniversellerZahlenOperator.SINUS,
    UniversellerZahlenOperator.COSINUS,
)

private val binaereZahlenOperatoren = setOf(
    UniversellerZahlenOperator.SUBTRAKTION,
    UniversellerZahlenOperator.POTENZ,
    UniversellerZahlenOperator.WURZEL,
    UniversellerZahlenOperator.LOGARITHMUS,
    UniversellerZahlenOperator.MODULO,
)

private val zahlTyp = TypAusdruck.Atom(MathematischeTypen.Zahl)
private val angulusTyp = TypAusdruck.Atom(MathematischeTypen.Angulus)
private val methodenTyp = TypAusdruck.Atom(MathematischeTypen.Methode)
private val tupelTyp = TypAusdruck.Atom(MathematischeTypen.Tupel)

fun istVariadischerZahlenOperator(operator: UniversellerZahlenOperator): Boolean =
    operator in variadischeZahlenOperatoren

fun istKomplexKonstruktor(operator: UniversellerZahlenOperator): Boolean =
    operator in komplexKonstruktoren

/** Nur für die verlustfreie Migration alter Karten; neue Knoten verwenden Angulus. */
fun verwendetGradWinkel(operator: UniversellerZahlenOperator): Boolean = operator in setOf(
    UniversellerZahlenOperator.SINUS,
    UniversellerZahlenOperator.COSINUS,
    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
)

fun konfiguriereZahlenRechner(
    knoten: KnotenDaten,
    operator: UniversellerZahlenOperator = UniversellerZahlenOperator.vonId(knoten.parameter[ZAHLENRECHNER_OPERATOR]),
    komplexEingabe: String = knoten.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE]
        ?.takeIf { it == ZAHLENRECHNER_KOMPLEX_TUPEL || it == ZAHLENRECHNER_KOMPLEX_SEPARIERT }
        ?: ZAHLENRECHNER_KOMPLEX_SEPARIERT,
    festeEingänge: Int = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2,
): KnotenDaten {
    require(knoten.art == ZAHLENRECHNER_ART) { "Nur ein universeller Zahlenrechner kann so konfiguriert werden." }
    val vorhandene = knoten.anschlüsse.groupBy { it.richtung to it.name }
    val gewünschteEingänge = gewünschteZahlenRechnerEingänge(operator, komplexEingabe, festeEingänge)
    val eingänge = gewünschteEingänge.mapIndexed { index, vorgabe ->
        val vorhanden = vorhandene[AnschlussRichtung.Eingang to vorgabe.name]?.firstOrNull()
        (vorhanden ?: vorgabe).copy(
            name = vorgabe.name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = vorgabe.art,
            reihenfolge = index,
            kannSichErweitern = vorgabe.kannSichErweitern,
            dynamischErzeugt = index >= 2 && operator in variadischeZahlenOperatoren,
            zulässigeArten = vorgabe.zulässigeArten,
            vertrag = vorgabe.vertrag,
            typInferenz = vorgabe.typInferenz,
        )
    }
    val punktweiseEingangsNamen = eingänge
        .filter { MathematikAnschlussArten.Methode.id in it.zulässigeArten }
        .map { it.name }
    val vorhandenerAusgang = vorhandene[AnschlussRichtung.Ausgang to "wert"]?.firstOrNull()
        ?: knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    val skalareAusgangsArt = if (operator in angulusErzeuger) MathematikAnschlussArten.Angulus.id else MathematikAnschlussArten.Zahl.id
    val skalareAusgangsVertrag = when {
        operator in angulusErzeuger && punktweiseEingangsNamen.isNotEmpty() ->
            AnschlussVertrag(TypAusdruck.Vereinigung(listOf(angulusTyp, methodenTyp)))
        operator in angulusErzeuger -> AnschlussVertrag(angulusTyp)
        else -> vorhandenerAusgang?.vertrag ?: AnschlussVertrag()
    }
    val typInferenz = if (operator in angulusErzeuger && eingänge.any { it.name == "a" }) {
        TypInferenzRegel.AbbildungVonEingang(
            eingang = "a",
            abbildung = mapOf(zahlTyp to angulusTyp, methodenTyp to methodenTyp),
        )
    } else null
    val ausgang = (vorhandenerAusgang ?: AnschlussDaten(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = skalareAusgangsArt,
        vertrag = skalareAusgangsVertrag,
    )).copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = skalareAusgangsArt,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
        artFolgtEingang = null,
        artVereinigtEingänge = emptyList(),
        zulässigeArten = emptySet(),
        artAbbildungVonEingang = null,
        artPriorisiertEingänge = punktweiseEingangsNamen.takeIf { it.isNotEmpty() }?.let { namen ->
            AnschlussArtPriorisierung(eingänge = namen, prioritäten = listOf(MathematikAnschlussArten.Methode.id))
        },
        vertrag = skalareAusgangsVertrag,
        typInferenz = typInferenz,
    )
    return knoten.copy(
        name = zahlenRechnerNameFürWechsel(knoten, operator.titel),
        anschlüsse = eingänge + ausgang,
        parameter = knoten.parameter + mapOf(
            ZAHLENRECHNER_OPERATOR to operator.stabileId,
            ZAHLENRECHNER_KOMPLEX_EINGABE to komplexEingabe,
            "festeEingänge" to festeEingänge.toString(),
        ),
    )
}

private fun gewünschteZahlenRechnerEingänge(
    operator: UniversellerZahlenOperator,
    komplexEingabe: String,
    festeEingänge: Int,
): List<AnschlussDaten> = when {
    operator in komplexKonstruktoren && komplexEingabe == ZAHLENRECHNER_KOMPLEX_TUPEL -> listOf(
        eingang("tupel", MathematikAnschlussArten.Tupel.id, vertrag = AnschlussVertrag(tupelTyp)),
    )
    operator == UniversellerZahlenOperator.KOMPLEX_AUS_POLAR -> listOf(
        zahlenOderMethodenEingang("a"),
        angulusOderMethodenEingang("b"),
    )
    operator == UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH -> listOf(
        zahlenOderMethodenEingang("a"),
        zahlenOderMethodenEingang("b"),
    )
    operator == UniversellerZahlenOperator.ITERIERTE_SUMME || operator == UniversellerZahlenOperator.ITERIERTES_PRODUKT -> listOf(
        eingang("methode", MathematikAnschlussArten.ZahlMethode.id),
        eingang("indexmenge", MathematikAnschlussArten.Menge.id),
    )
    operator == UniversellerZahlenOperator.DIVISION -> listOf(
        zahlenOderMethodenEingang("a"),
        zahlenOderMethodenEingang("b"),
        eingang("c", MathematikAnschlussArten.Zahl.id),
    )
    operator in variadischeZahlenOperatoren -> List(festeEingänge) { index ->
        zahlenOderMethodenEingang(('a'.code + index).toChar().toString(), erweiterbar = true)
    }
    operator in binaereZahlenOperatoren -> listOf(zahlenOderMethodenEingang("a"), zahlenOderMethodenEingang("b"))
    operator in angulusVerbraucher -> listOf(angulusOderMethodenEingang("a"))
    operator.hebungsArt == ZahlenOperatorHebungsArt.PUNKTWEISE -> listOf(zahlenOderMethodenEingang("a"))
    else -> listOf(eingang("a", MathematikAnschlussArten.Zahl.id))
}

private fun zahlenOderMethodenEingang(name: String, erweiterbar: Boolean = false): AnschlussDaten = eingang(
    name = name,
    art = MathematikAnschlussArten.Objekt.id,
    erweiterbar = erweiterbar,
    zulässigeArten = setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Methode.id),
)

private fun angulusOderMethodenEingang(name: String): AnschlussDaten = eingang(
    name = name,
    art = MathematikAnschlussArten.Objekt.id,
    zulässigeArten = setOf(MathematikAnschlussArten.Angulus.id, MathematikAnschlussArten.Methode.id),
    vertrag = AnschlussVertrag(TypAusdruck.Vereinigung(listOf(angulusTyp, methodenTyp))),
)

private fun eingang(
    name: String,
    art: AnschlussArtId,
    erweiterbar: Boolean = false,
    zulässigeArten: Set<AnschlussArtId> = emptySet(),
    vertrag: AnschlussVertrag = AnschlussVertrag(),
): AnschlussDaten = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    kannSichErweitern = erweiterbar,
    zulässigeArten = zulässigeArten,
    vertrag = vertrag,
)
