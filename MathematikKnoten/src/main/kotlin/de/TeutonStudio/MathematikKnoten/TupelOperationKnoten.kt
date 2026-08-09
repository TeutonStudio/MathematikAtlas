package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val TUPEL_ERGÄNZEN_ART = "mathematik.tupelErgänzen"
const val TUPEL_AUFLÖSEN_ART = "mathematik.tupelAuflösen"
const val TUPEL_ERGÄNZEN_MODUS_PARAMETER = "modus"

enum class TupelErgänzenModus(val id: String) {
    Tupel("tupelErgänzen"),
    Elemente("elementeErgänzen"),
    ;

    companion object {
        fun von(knoten: KnotenDaten): TupelErgänzenModus =
            entries.firstOrNull { it.id == knoten.parameter[TUPEL_ERGÄNZEN_MODUS_PARAMETER] } ?: Tupel
    }
}

object TupelOperationKnotenVorlagen {
    private fun eingang(
        name: String,
        art: AnschlussArtId,
        reihenfolge: Int,
        erweiterbar: Boolean = false,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = reihenfolge,
        kannSichErweitern = erweiterbar,
    )

    private fun ausgang(name: String, art: AnschlussArtId) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )

    val Ergänzen = KnotenVorlage(
        art = TUPEL_ERGÄNZEN_ART,
        name = "Tupel ergänzen",
        kategorie = "Zahlen",
        beschreibung = "Verkettet Tupel oder ergänzt ein Basistupel um einzelne mathematische Objekte.",
        standardGröße = GraphGröße(245f, 130f),
        anschlüsse = listOf(
            eingang("tupel1", MathematikAnschlussArten.Tupel.id, 0, true),
            eingang("tupel2", MathematikAnschlussArten.Tupel.id, 1, true),
            ausgang("tupel", MathematikAnschlussArten.Tupel.id),
        ),
        standardParameter = mapOf(
            TUPEL_ERGÄNZEN_MODUS_PARAMETER to TupelErgänzenModus.Tupel.id,
            "festeEingänge" to "2",
            "operatorAnzeige" to "wert",
        ),
    )

    val Auflösen = KnotenVorlage(
        art = TUPEL_AUFLÖSEN_ART,
        name = "Tupel auflösen",
        kategorie = "Zahlen",
        beschreibung = "Stellt jedes direkte Element eines Tupels in derselben Reihenfolge als typisierten Ausgang bereit.",
        standardGröße = GraphGröße(235f, 115f),
        anschlüsse = listOf(
            eingang("tupel", MathematikAnschlussArten.Tupel.id, 0),
        ),
    )

    val alle = listOf(Ergänzen, Auflösen)
}

/**
 * Wechselt den Ergänzungsmodus. Nur der erste Tupelanschluss behält seine ID;
 * weitere Anschlüsse werden absichtlich nicht in die andere Semantik umgedeutet.
 */
fun konfiguriereTupelErgänzen(
    knoten: KnotenDaten,
    modus: TupelErgänzenModus,
): KnotenDaten {
    if (TupelErgänzenModus.von(knoten) == modus) return knoten

    val bisherigeEingänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val erster = bisherigeEingänge.firstOrNull()
    val ausgang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "tupel"
    } ?: AnschlussDaten(
        name = "tupel",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Tupel.id,
    )

    val basis = (erster ?: AnschlussDaten(
        name = "tupel1",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Tupel.id,
    )).copy(
        name = if (modus == TupelErgänzenModus.Tupel) "tupel1" else "basistupel",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Tupel.id,
        reihenfolge = 0,
        kannSichErweitern = modus == TupelErgänzenModus.Tupel,
        dynamischErzeugt = false,
    )

    val zweiter = when (modus) {
        TupelErgänzenModus.Tupel -> AnschlussDaten(
            name = "tupel2",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Tupel.id,
            reihenfolge = 1,
            kannSichErweitern = true,
        )
        TupelErgänzenModus.Elemente -> AnschlussDaten(
            name = "element1",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Objekt.id,
            reihenfolge = 1,
            kannSichErweitern = true,
        )
    }

    return knoten.copy(
        anschlüsse = listOf(basis, zweiter, ausgang),
        parameter = knoten.parameter + mapOf(
            TUPEL_ERGÄNZEN_MODUS_PARAMETER to modus.id,
            "festeEingänge" to "2",
        ),
    )
}

internal fun MathematikAuswerterRegister.registriereTupelOperationKnoten() {
    registriere(TUPEL_ERGÄNZEN_ART) { kontext ->
        val eingangsAnschlüsse = kontext.knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        require(eingangsAnschlüsse.size >= 2) { "Tupel ergänzen benötigt mindestens zwei Eingänge." }

        val verwendeteWerte: List<BedingterWert>
        val ergebnis = when (TupelErgänzenModus.von(kontext.knoten)) {
            TupelErgänzenModus.Tupel -> {
                verwendeteWerte = eingangsAnschlüsse.mapIndexed { index, anschluss ->
                    kontext.eingänge[anschluss.name]
                        ?: error("Tupel ${index + 1} ist nicht verbunden.")
                }
                val tupel = verwendeteWerte.map { wert ->
                    wert.objekt as? Tupel ?: error("Der Eingang erwartet ein Tupel.")
                }
                ergänzeTupel(tupel)
            }
            TupelErgänzenModus.Elemente -> {
                val basisWert = kontext.eingänge[eingangsAnschlüsse.first().name]
                    ?: error("Basistupel ist nicht verbunden.")
                val basis = basisWert.objekt as? Tupel ?: error("Der Eingang erwartet ein Tupel.")
                val elementWerte = eingangsAnschlüsse.drop(1).mapIndexed { index, anschluss ->
                    kontext.eingänge[anschluss.name]
                        ?: error("Element ${index + 1} ist nicht verbunden.")
                }
                verwendeteWerte = listOf(basisWert) + elementWerte
                ergänzeTupelUmElemente(basis, elementWerte.map(BedingterWert::objekt))
            }
        }

        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "tupel" to BedingterWert(
                    objekt = ergebnis,
                    annahmen = verwendeteWerte.flatMap { it.annahmen }.toSet(),
                    reelleVariablen = reelleVariablen(verwendeteWerte),
                    variablenQuellen = verwendeteWerte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
                ),
            ),
        )
    }

    registriere(TUPEL_AUFLÖSEN_ART) { kontext ->
        val eingang = kontext.eingänge["tupel"] ?: error("Tupel ist nicht verbunden.")
        val tupel = eingang.objekt as? Tupel ?: error("Der Eingang erwartet ein Tupel.")
        KnotenAuswertungsErgebnis(
            ausgaben = tupel.elemente.mapIndexed { index, element ->
                "element-${index + 1}" to BedingterWert(
                    objekt = element,
                    annahmen = eingang.annahmen,
                    reelleVariablen = eingang.reelleVariablen,
                    variablenQuellen = eingang.variablenQuellen,
                )
            }.toMap(),
        )
    }
}
