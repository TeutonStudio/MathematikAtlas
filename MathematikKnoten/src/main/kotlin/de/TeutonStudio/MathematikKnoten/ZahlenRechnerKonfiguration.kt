package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlenOperatorHebungsArt

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

private val binaereZahlenOperatoren = setOf(
    UniversellerZahlenOperator.SUBTRAKTION,
    UniversellerZahlenOperator.POTENZ,
    UniversellerZahlenOperator.WURZEL,
    UniversellerZahlenOperator.LOGARITHMUS,
    UniversellerZahlenOperator.MODULO,
)

fun istVariadischerZahlenOperator(operator: UniversellerZahlenOperator): Boolean =
    operator in variadischeZahlenOperatoren

fun istKomplexKonstruktor(operator: UniversellerZahlenOperator): Boolean =
    operator in komplexKonstruktoren

fun verwendetGradWinkel(operator: UniversellerZahlenOperator): Boolean = operator in setOf(
    UniversellerZahlenOperator.SINUS,
    UniversellerZahlenOperator.COSINUS,
    UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
)

/**
 * Erzeugt die Anschlusskonfiguration eines Zahlenrechnerzustands und erhält
 * vorhandene Anschluss-IDs anhand ihrer semantischen Rolle. Dadurch bleiben
 * kompatible Edges bei Operatorwechseln bestehen.
 */
fun konfiguriereZahlenRechner(
    knoten: KnotenDaten,
    operator: UniversellerZahlenOperator = UniversellerZahlenOperator.vonId(
        knoten.parameter[ZAHLENRECHNER_OPERATOR],
    ),
    komplexEingabe: String = knoten.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE]
        ?.takeIf { it == ZAHLENRECHNER_KOMPLEX_TUPEL || it == ZAHLENRECHNER_KOMPLEX_SEPARIERT }
        ?: ZAHLENRECHNER_KOMPLEX_SEPARIERT,
    festeEingänge: Int = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2,
): KnotenDaten {
    require(knoten.art == ZAHLENRECHNER_ART) {
        "Nur ein universeller Zahlenrechner kann so konfiguriert werden."
    }
    val vorhandene = knoten.anschlüsse.groupBy { it.richtung to it.name }
    val gewünschteEingänge = gewünschteZahlenRechnerEingänge(
        operator = operator,
        komplexEingabe = komplexEingabe,
        festeEingänge = festeEingänge,
    )
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
        )
    }
    val punktweiseEingangsNamen = eingänge
        .filter { MathematikAnschlussArten.Methode.id in it.zulässigeArten }
        .map { it.name }
    val vorhandenerAusgang = vorhandene[AnschlussRichtung.Ausgang to "wert"]?.firstOrNull()
        ?: knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    val ausgang = (vorhandenerAusgang ?: AnschlussDaten(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id,
    )).copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
        artFolgtEingang = null,
        artVereinigtEingänge = emptyList(),
        zulässigeArten = emptySet(),
        artAbbildungVonEingang = null,
        artPriorisiertEingänge = punktweiseEingangsNamen.takeIf { it.isNotEmpty() }?.let { namen ->
            AnschlussArtPriorisierung(
                eingänge = namen,
                prioritäten = listOf(MathematikAnschlussArten.Methode.id),
            )
        },
    )
    val bisherigerOperator = knoten.parameter[ZAHLENRECHNER_OPERATOR]
        ?.let { operatorId -> UniversellerZahlenOperator.vonId(operatorId) }
    val bisherStandardName = bisherigerOperator?.let { knoten.name == it.titel } == true ||
        UniversellerZahlenOperator.entries.any { it.titel == knoten.name }
    val parameter = knoten.parameter + mapOf(
        ZAHLENRECHNER_OPERATOR to operator.stabileId,
        ZAHLENRECHNER_KOMPLEX_EINGABE to komplexEingabe,
        "festeEingänge" to festeEingänge.toString(),
    )
    return knoten.copy(
        name = if (bisherStandardName) operator.titel else knoten.name,
        anschlüsse = eingänge + ausgang,
        parameter = parameter,
    )
}

private fun gewünschteZahlenRechnerEingänge(
    operator: UniversellerZahlenOperator,
    komplexEingabe: String,
    festeEingänge: Int,
): List<AnschlussDaten> = when {
    operator in komplexKonstruktoren && komplexEingabe == ZAHLENRECHNER_KOMPLEX_TUPEL -> listOf(
        eingang("tupel", MathematikAnschlussArten.Tupel.id),
    )
    operator in komplexKonstruktoren -> listOf(
        zahlenOderMethodenEingang("a"),
        zahlenOderMethodenEingang("b"),
    )
    operator == UniversellerZahlenOperator.ITERIERTE_SUMME ||
        operator == UniversellerZahlenOperator.ITERIERTES_PRODUKT -> listOf(
        eingang("methode", MathematikAnschlussArten.ZahlMethode.id),
        eingang("indexmenge", MathematikAnschlussArten.Menge.id),
    )
    operator == UniversellerZahlenOperator.DIVISION -> listOf(
        zahlenOderMethodenEingang("a"),
        zahlenOderMethodenEingang("b"),
        eingang("c", MathematikAnschlussArten.Zahl.id),
    )
    operator in variadischeZahlenOperatoren -> List(festeEingänge) { index ->
        zahlenOderMethodenEingang(
            name = ('a'.code + index).toChar().toString(),
            erweiterbar = true,
        )
    }
    operator in binaereZahlenOperatoren -> listOf(
        zahlenOderMethodenEingang("a"),
        zahlenOderMethodenEingang("b"),
    )
    operator.hebungsArt == ZahlenOperatorHebungsArt.PUNKTWEISE ->
        listOf(zahlenOderMethodenEingang("a"))
    else -> listOf(eingang("a", MathematikAnschlussArten.Zahl.id))
}

private fun zahlenOderMethodenEingang(
    name: String,
    erweiterbar: Boolean = false,
): AnschlussDaten = eingang(
    name = name,
    art = MathematikAnschlussArten.Objekt.id,
    erweiterbar = erweiterbar,
    zulässigeArten = setOf(
        MathematikAnschlussArten.Zahl.id,
        MathematikAnschlussArten.Methode.id,
    ),
)

private fun eingang(
    name: String,
    art: AnschlussArtId,
    erweiterbar: Boolean = false,
    zulässigeArten: Set<AnschlussArtId> = emptySet(),
): AnschlussDaten = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    kannSichErweitern = erweiterbar,
    zulässigeArten = zulässigeArten,
)
