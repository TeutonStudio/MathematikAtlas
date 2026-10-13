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
    val signaturId = if (operator == UniversellerZahlenOperator.SINUS && knoten.parameter[SINUS_WINKELRAUM] == "grad") "grad"
        else if (operator in komplexKonstruktoren && komplexEingabe == ZAHLENRECHNER_KOMPLEX_TUPEL) "tupel" else "standard"
    val gewünschteEingänge = gewünschteZahlenRechnerEingänge(
        operator = operator,
        signaturId = signaturId,
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
    val parameter = knoten.parameter + mapOf(
        ZAHLENRECHNER_OPERATOR to operator.stabileId,
        ZAHLENRECHNER_KOMPLEX_EINGABE to komplexEingabe,
        "festeEingänge" to festeEingänge.toString(),
    )
    return knoten.copy(
        name = zahlenRechnerNameFürWechsel(knoten, operator.titel),
        anschlüsse = erhalteDynamischeOperanden(knoten,
            erhalteRechnerAnschlussIds(knoten.anschlüsse, eingänge + ausgang, RechnerFamilie.ZAHL, operator.stabileId,
                signaturId),
            knoten.parameter[ZAHLENRECHNER_OPERATOR] == operator.stabileId &&
                (knoten.parameter["festeEingänge"]?.toIntOrNull() ?: 2) == festeEingänge),
        parameter = parameter,
    )
}

private fun gewünschteZahlenRechnerEingänge(operator: UniversellerZahlenOperator, signaturId: String, festeEingänge: Int): List<AnschlussDaten> {
    val basis = rechnerAnschlüsse(RechnerFamilie.ZAHL, operator.stabileId, signaturId).filter { it.richtung == AnschlussRichtung.Eingang }
    if (operator !in variadischeZahlenOperatoren) return basis
    return List(festeEingänge) { index -> basis[minOf(index, basis.lastIndex)].copy(
        id = neueAnschlussId(), name = ('a'.code + index).toChar().toString(), reihenfolge = index, dynamischErzeugt = index >= 2,
    ) }
}

private fun zahlenOderMethodenEingang(
    name: String,
    erweiterbar: Boolean = false,
): AnschlussDaten = eingang(
    name = name,
    art = MathematikAnschlussArten.AtlasWert.id,
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

const val SINUS_WINKELRAUM = "sinus.winkelraum"
fun konfiguriereSinusWinkelraum(knoten: KnotenDaten, grad: Boolean): KnotenDaten = konfiguriereZahlenRechner(
    knoten.copy(parameter = knoten.parameter + (SINUS_WINKELRAUM to if (grad) "grad" else "reell")), UniversellerZahlenOperator.SINUS)
