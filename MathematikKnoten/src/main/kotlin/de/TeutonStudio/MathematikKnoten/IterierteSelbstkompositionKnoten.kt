package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

const val SELBSTKOMPOSITION_KNOTEN_ART = "mathematik.iterierteSelbstkomposition"
const val SELBSTKOMPOSITION_ORDNUNG_PARAMETER = "selbstkomposition.ordnung"
const val SELBSTKOMPOSITION_EINGANGSMODUS_PARAMETER = "selbstkomposition.eingangsModus"
const val SELBSTKOMPOSITION_AUSGANGSMODUS_PARAMETER = "selbstkomposition.ausgangsModus"
const val SELBSTKOMPOSITION_BEREICHSMODUS_PARAMETER = "selbstkomposition.bereichsModus"
const val SELBSTKOMPOSITION_AUSWERTUNGSBUDGET_PARAMETER = "selbstkomposition.auswertungsBudget"
const val SELBSTKOMPOSITION_MIGRATIONSFEHLER_PARAMETER = "selbstkomposition.migrationsFehler"

private const val HISTORISCHE_SELBSTKOMPOSITION_ART = "mathematik.selbstkompositionIteriert"

object IterierteSelbstkompositionKnotenVorlagen {
    val Selbstkomposition = KnotenVorlage(
        art = SELBSTKOMPOSITION_KNOTEN_ART,
        name = "Selbstkomposition",
        kategorie = "Grundlagen: Methoden",
        beschreibung = "Bildet f^{⟨n⟩}, prüft den erneuten Aufrufvertrag und bestimmt soweit möglich den maximal zulässigen Wertevorrat.",
        standardGröße = GraphGröße(305f, 145f),
        anschlüsse = listOf(
            selbstkompositionsEingang("methode", MathematikAnschlussArten.Methode.id, 0),
            selbstkompositionsEingang("ordnung", MathematikAnschlussArten.Zahl.id, 1),
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Methode.id,
            ),
        ),
        standardParameter = mapOf(
            SELBSTKOMPOSITION_ORDNUNG_PARAMETER to "2",
            SELBSTKOMPOSITION_EINGANGSMODUS_PARAMETER to KompositionsEingangsModus.GETRENNTE_ARGUMENTE.name,
            SELBSTKOMPOSITION_AUSGANGSMODUS_PARAMETER to KompositionsAusgangsModus.GEPACKT.name,
            SELBSTKOMPOSITION_BEREICHSMODUS_PARAMETER to KompositionsBereichsModus.MAXIMAL_ZULAESSIG.name,
            SELBSTKOMPOSITION_AUSWERTUNGSBUDGET_PARAMETER to "12",
        ),
    )

    val alle = listOf(Selbstkomposition)
}

internal fun MathematikAuswerterRegister.registriereIterierteSelbstkomposition() {
    registriere(SELBSTKOMPOSITION_KNOTEN_ART) { kontext ->
        kontext.werteIterierteSelbstkompositionAus()
    }
}

private fun KnotenAuswertungsKontext.werteIterierteSelbstkompositionAus(): KnotenAuswertungsErgebnis {
    knoten.parameter[SELBSTKOMPOSITION_MIGRATIONSFEHLER_PARAMETER]?.let { fehler ->
        return fehlerErgebnis(fehler)
    }
    val methode = eingänge["methode"]?.objekt as? Methode
        ?: return fehlerErgebnis("Die Selbstkomposition benötigt eine Methode.")
    val ordnung = bestimmeSelbstkompositionsOrdnung()
        ?: return fehlerErgebnis("Die Kompositionsordnung muss konkret oder symbolisch in ℕ₀ liegen.")
    val eingangsModus = enumParameter(
        SELBSTKOMPOSITION_EINGANGSMODUS_PARAMETER,
        KompositionsEingangsModus.GETRENNTE_ARGUMENTE,
    )
    val ausgangsModus = enumParameter(
        SELBSTKOMPOSITION_AUSGANGSMODUS_PARAMETER,
        KompositionsAusgangsModus.GEPACKT,
    )
    val bereichsModus = enumParameter(
        SELBSTKOMPOSITION_BEREICHSMODUS_PARAMETER,
        KompositionsBereichsModus.MAXIMAL_ZULAESSIG,
    )
    val budget = knoten.parameter[SELBSTKOMPOSITION_AUSWERTUNGSBUDGET_PARAMETER]
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?: 12
    val kontextMitAnnahmen = rechenKontext.copy(
        annahmen = rechenKontext.annahmen + gemeinsameAnnahmen(),
    )
    val ergebnis = runCatching {
        werteSelbstkompositionAus(
            methode = methode,
            ordnung = ordnung,
            eingangsModus = eingangsModus,
            ausgangsModus = ausgangsModus,
            bereichsModus = bereichsModus,
            kontext = kontextMitAnnahmen,
            auswertungsBudget = budget,
        )
    }.getOrElse { fehler ->
        return fehlerErgebnis(
            fehler.message ?: "Die Selbstkomposition konnte nicht ausgewertet werden.",
        )
    }

    val ausgabeMethode = ergebnis.methode ?: return fehlerErgebnis(
        "${ergebnis.status.name}: ${ergebnis.begruendung}",
    )
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "methode" to BedingterWert(
                objekt = ausgabeMethode,
                annahmen = gemeinsameAnnahmen() + ergebnis.voraussetzungen,
                zielMenge = ausgabeMethode.zielMenge,
            ),
        ),
        warnungen = buildList {
            add("Status: ${ergebnis.status.name}")
            add("Wertevorrat: ${ergebnis.maximalerWertevorrat.zuLatex()}")
            add("Zielmenge: ${ergebnis.zielMenge.zuLatex()}")
            add("Eingang: ${eingangsModus.name}")
            add("Ausgang: ${ausgangsModus.name}")
            add("Bereich: ${bereichsModus.name}")
            add(ergebnis.begruendung)
        },
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.bestimmeSelbstkompositionsOrdnung(): IterationsOrdnung? {
    val verbunden = eingänge["ordnung"]
    val objekt = verbunden?.objekt
    if (objekt != null) {
        return when (objekt) {
            is RationaleZahl -> when {
                objekt.nenner != BigInteger.ONE -> null
                objekt.zähler.signum() < 0 -> null
                else -> IterationsOrdnung.Konkret(objekt.zähler)
            }
            is ZahlAusdruck -> IterationsOrdnung.Symbolisch(
                ausdruck = objekt,
                annahmen = verbunden.annahmen + UnentscheidbareAussage(
                    "${objekt.zuLatex()}\\in\\mathbb N_0",
                    "Selbstkompositionsordnung",
                ),
            )
            else -> null
        }
    }
    val fallback = knoten.parameter[SELBSTKOMPOSITION_ORDNUNG_PARAMETER]
        ?.trim()
        ?.toBigIntegerOrNull()
        ?: return null
    return fallback.takeIf { it.signum() >= 0 }?.let(IterationsOrdnung::Konkret)
}

private inline fun <reified T : Enum<T>> KnotenAuswertungsKontext.enumParameter(
    parameter: String,
    fallback: T,
): T = enumValues<T>().firstOrNull { it.name == knoten.parameter[parameter] } ?: fallback

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

private fun KnotenAuswertungsKontext.fehlerErgebnis(nachricht: String): KnotenAuswertungsErgebnis =
    KnotenAuswertungsErgebnis(
        ausgaben = emptyMap(),
        fehler = nachricht,
        eingänge = eingänge,
    )

fun KartenDaten.migriereIterierteSelbstkompositionKnoten(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        when (knoten.art) {
            SELBSTKOMPOSITION_KNOTEN_ART -> knoten.normalisiereSelbstkompositionsParameter()
            HISTORISCHE_SELBSTKOMPOSITION_ART -> knoten.copy(
                art = SELBSTKOMPOSITION_KNOTEN_ART,
                name = "Selbstkomposition",
                parameter = knoten.parameter + mapOf(
                    SELBSTKOMPOSITION_ORDNUNG_PARAMETER to (knoten.parameter["ordnung"] ?: "2"),
                    SELBSTKOMPOSITION_EINGANGSMODUS_PARAMETER to KompositionsEingangsModus.GETRENNTE_ARGUMENTE.name,
                    SELBSTKOMPOSITION_AUSGANGSMODUS_PARAMETER to KompositionsAusgangsModus.GEPACKT.name,
                    SELBSTKOMPOSITION_BEREICHSMODUS_PARAMETER to KompositionsBereichsModus.MAXIMAL_ZULAESSIG.name,
                    SELBSTKOMPOSITION_AUSWERTUNGSBUDGET_PARAMETER to "12",
                ),
            ).normalisiereSelbstkompositionsParameter()
            else -> knoten
        }
    },
)

private fun KnotenDaten.normalisiereSelbstkompositionsParameter(): KnotenDaten = copy(
    parameter = IterierteSelbstkompositionKnotenVorlagen.Selbstkomposition.standardParameter + parameter,
)

private fun selbstkompositionsEingang(
    name: String,
    art: AnschlussArtId,
    reihenfolge: Int,
): AnschlussDaten = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
)
