package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.TUPEL_AUFLÖSEN_ART
import de.TeutonStudio.MathematikKnoten.VEKTOR_RECHNER_OPERATOR
import de.TeutonStudio.MathematikKnoten.anschlussArtFürMathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.OrientierterVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator

internal const val TUPEL_AUFLÖSEN_ANZAHL = "tupelAuflösen.anzahl"
internal const val VEKTOR_ZERLEGEN_ANZAHL = "vektorZerlegen.anzahl"

/**
 * Synchronisiert den letzten erfolgreich bekannten Tupel-/Vektorvertrag mit den
 * dynamischen Ausgängen. Bei fehlgeschlagener Auswertung bleibt der bestehende
 * Vertrag unverändert; dadurch flackert die Graphstruktur nicht zwischen zwei
 * Auswertungszuständen.
 */
internal fun synchronisiereTupelAuflöser(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
    prüfung: GraphPrüfung,
): KartenDaten {
    fun istDynamischerAuflöser(knoten: KnotenDaten): Boolean =
        knoten.art == TUPEL_AUFLÖSEN_ART ||
            (
                knoten.art == VektorRechner.KNOTEN_ART &&
                    VektorRechnerOperator.vonIdOderNull(knoten.parameter[VEKTOR_RECHNER_OPERATOR]) ==
                    VektorRechnerOperator.ZERLEGEN
                )

    val idErsetzungen = buildMap {
        karte.knoten.filter(::istDynamischerAuflöser).forEach { knoten ->
            knoten.anschlüsse
                .filter { it.richtung == AnschlussRichtung.Ausgang }
                .sortedBy { it.reihenfolge }
                .forEachIndexed { index, anschluss ->
                    val alt = AnschlussVerweis(knoten.id, anschluss.id)
                    val neu = AnschlussVerweis(knoten.id, elementId(knoten.id, index, knoten.art))
                    if (alt != neu) put(alt, neu)
                }
        }
    }

    val synchronisierteKnoten = karte.knoten.map { knoten ->
        when {
            knoten.art == TUPEL_AUFLÖSEN_ART -> {
                val tupel = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("tupel")
                    ?.objekt as? Tupel
                    ?: return@map knoten
                synchronisiereAuflöserKnoten(knoten, tupel.elemente, "tupel", TUPEL_AUFLÖSEN_ANZAHL)
            }
            knoten.art == VektorRechner.KNOTEN_ART &&
                VektorRechnerOperator.vonIdOderNull(knoten.parameter[VEKTOR_RECHNER_OPERATOR]) ==
                VektorRechnerOperator.ZERLEGEN -> {
                val struktur = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("struktur")
                    ?.objekt
                    ?: return@map knoten
                val elemente = when (struktur) {
                    is Tupel -> struktur.elemente
                    is OrientierterVektor -> struktur.werte
                    else -> return@map knoten
                }
                synchronisiereAuflöserKnoten(knoten, elemente, "struktur", VEKTOR_ZERLEGEN_ANZAHL)
            }
            else -> knoten
        }
    }

    var ergebnis = karte.copy(
        knoten = synchronisierteKnoten,
        verbindungen = karte.verbindungen.map { verbindung ->
            verbindung.copy(
                von = idErsetzungen[verbindung.von] ?: verbindung.von,
                zu = idErsetzungen[verbindung.zu] ?: verbindung.zu,
            )
        },
    )

    val vorhandeneAnschlüsse = ergebnis.knoten.flatMap { knoten ->
        knoten.anschlüsse.map { AnschlussVerweis(knoten.id, it.id) }
    }.toSet()
    ergebnis = ergebnis.copy(verbindungen = ergebnis.verbindungen.filter {
        it.von in vorhandeneAnschlüsse && it.zu in vorhandeneAnschlüsse
    })

    val gültigeVerbindungen = ergebnis.verbindungen.filter { verbindung ->
        val ohneAktuelle = ergebnis.copy(
            verbindungen = ergebnis.verbindungen.filterNot { it.id == verbindung.id },
        )
        prüfung.prüfe(ohneAktuelle, verbindung.von, verbindung.zu) is VerbindungsPrüfung.Erlaubt
    }
    return ergebnis.copy(verbindungen = gültigeVerbindungen)
}

private fun synchronisiereAuflöserKnoten(
    knoten: KnotenDaten,
    elemente: List<de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt>,
    eingangsName: String,
    anzahlParameter: String,
): KnotenDaten {
    val eingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == eingangsName
    } ?: return knoten
    val bisherigeAusgänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }

    val ausgänge = elemente.mapIndexed { index, element ->
        (bisherigeAusgänge.getOrNull(index) ?: AnschlussDaten(
            id = elementId(knoten.id, index, knoten.art),
            name = "element-${index + 1}",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = anschlussArtFürMathematischesObjekt(element),
        )).copy(
            id = elementId(knoten.id, index, knoten.art),
            name = "element-${index + 1}",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = anschlussArtFürMathematischesObjekt(element),
            zulässigeArten = emptySet(),
            reihenfolge = index,
            kannSichErweitern = false,
            dynamischErzeugt = true,
        )
    }

    val mindestHöhe = maxOf(115f, 78f + 28f * ausgänge.size)
    return knoten.copy(
        anschlüsse = listOf(eingang.copy(reihenfolge = 0)) + ausgänge,
        größe = knoten.größe.copy(höhe = maxOf(knoten.größe.höhe, mindestHöhe)),
        parameter = knoten.parameter + (anzahlParameter to ausgänge.size.toString()),
    )
}

private fun elementId(knotenId: KnotenId, index: Int, knotenArt: String) =
    AnschlussId(
        if (knotenArt == TUPEL_AUFLÖSEN_ART) {
            "${knotenId.wert}:tupelAuflösen:element:${index + 1}"
        } else {
            "${knotenId.wert}:vektorZerlegen:element:${index + 1}"
        },
    )
